package narl.itrc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javafx.concurrent.Task;

public abstract class DevBase {
	
	protected String TAG = "DevBase";
	
	abstract public void open();	
	abstract public void close();
	abstract public boolean isLive();
	
	//public StringProperty state = new SimpleStringProperty();
	
	private AtomicReference<Runnable> breakIn = new AtomicReference<Runnable>(null);
	
	public final AtomicReference<String> nextState = new AtomicReference<String>("");
	
	private ConcurrentHashMap<String,Runnable> state = new ConcurrentHashMap<String,Runnable>();
	
	private String init_s_name = "";
	
	private class StateFlow extends Task<Integer> {
		
		void looper() {			
			//check whether we have interrupt~~
			Runnable work = breakIn.get();			
			if(work!=null) {
				updateTitle("!!interrupt!!");
				updateValue(0);
				work.run();
				breakIn.set(null);
				return;
			}
			//go through state-flow
			String name = nextState.get();
			if(isIdle(name)==true) {				
				waiting();//special case, do nothing~~~
				return;
			}
			work = state.get(name);
			if(work==null) {
				updateTitle("[No state]:"+name);
				updateValue(-1);
				nextState.set(init_s_name);
				return;
			}
			updateTitle("state:"+name);
			updateValue(name.hashCode());			
			work.run();
		}		
		private boolean isIdle(final String name) {
			if(name==null) {
				return true;
			}
			if(name.length()==0) {
				return true;
			}
			return false;
		}
		private void waiting() {
			try {synchronized(breakIn) {
				breakIn.wait();
			}} catch (InterruptedException e) {
				//Misc.logv("%s: looper wake up!!",TAG);
			} catch (final Throwable th) {
				//Misc.loge("%s:%s",TAG,th.getMessage());
			}
		}
		@Override
		protected Integer call() throws Exception {
			do {
				if(Gawain.isExit()==true) {
					close();
					return 0;
				}
				if(state.isEmpty()==true) {
					waiting();
					continue;
				}
				looper();
			}while(isCancelled()==false);
			return 0;
		}
	};
	private StateFlow tsk = null;
	private Thread thd = null;
	
	public DevBase breakIn(final Runnable work) {
		if(thd==null) {
			return this;
		}
		if(breakIn.get()!=null) {
			Misc.logw("%s is busy!!", TAG);
			return this;
		}
		breakIn.set(work);
		thd.interrupt();
		return this;
	}
	public void waiting() {
		do {
			if(breakIn.get()==null) {
				break;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}while(tsk.isDone()==false);
	}
	public DevBase syncBreakIn(final Runnable work) {
		breakIn(work);
		waiting();
		return this;
	}	

	public boolean isFlowing() {
		if(tsk==null) {
			return false;
		}
		if(tsk.isDone()==true) {
			return false;
		}
		if(tsk.valueProperty().get()==0) {
			return true;
		}
		return false;
	}
		
	public DevBase setupState0(
		final String name,
		final Runnable work
	) {
		return setup_state(true, name, work);
	}
	public DevBase setupStateX(
		final String name,
		final Runnable work
	) {
		return setup_state(false, name, work);
	}
	private DevBase setup_state(
		final boolean is_init,
		final String name,
		final Runnable work
	) {
		if(is_init==true) {
			init_s_name = name;
			nextState.set(name);
		}
		state.put(name, work);
		return this;
	}
	
	public void playFlow() {
		if(tsk==null) {
			tsk = new StateFlow();
		}else if(tsk.isRunning()==true) {
			return;
		}
		thd = new Thread(tsk,TAG);
		thd.setDaemon(true);
		thd.start();
	}
	
	public void stopFlow() {
		if(tsk==null) {
			return;
		}else if(tsk.isRunning()==true) {
			while(tsk.cancel()==true);
		}
	}	
}
