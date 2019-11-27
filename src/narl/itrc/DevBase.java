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
		
	public final AtomicReference<String> nextState = new AtomicReference<String>("");
	
	private ConcurrentHashMap<String,Runnable> state = new ConcurrentHashMap<String,Runnable>();
	
	private AtomicReference<Runnable> interrupt = new AtomicReference<Runnable>(null);
	
	private String init_s_name = "";
	
	private long tick = -1;
	
	public boolean waiting(final long period) {
		if(tick<=0) {
			//start timer!!
			tick = System.currentTimeMillis();
			return true;
		}
		if((System.currentTimeMillis() - tick)<period) {
			return true;
		}
		//restart period~~~
		tick = -1L;
		return false;
	}
	
	private class StateFlow extends Task<String> {
		
		void looper() {			
			//check whether we have interrupt~~
			Runnable work = interrupt.get();			
			if(work!=null) {
				updateTitle("interrupt!!");
				updateValue("***");
				work.run();
				interrupt.set(null);
				return;
			}
			//go through state-flow
			String name = nextState.get();
			if(name==null) {
				//special case, do nothing~~~
				return;
			}
			if(name.length()==0) {
				//special case, do nothing~~~
				return;
			}
			work = state.get(name);
			if(work==null) {
				updateTitle("No state:"+name);
				updateValue("???");
				nextState.set(init_s_name);
				return;
			}
			updateTitle("state:"+name);
			updateValue(name);			
			work.run();
		}
		
		@Override
		protected String call() throws Exception {
			do {
				if(Gawain.isExit()==true) {
					close();
					return "!exist";
				}
				if(state.isEmpty()==true) {
					continue;
				}
				looper();
			}while(isCancelled()==false);
			return "";
		}
	};
	private StateFlow tsk = null;
	
	public DevBase interrupt(final Runnable work) {
		if(interrupt.get()!=null) {
			Misc.logw("%s is busy!!", TAG);
			return this;
		}
		interrupt.set(work);
		return this;
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
		Thread th = new Thread(tsk,TAG);
		th.setDaemon(true);
		th.start();
	}
	
	public void stopFlow() {
		if(tsk==null) {
			return;
		}else if(tsk.isRunning()==true) {
			while(tsk.cancel()==true);
		}
	}	
}
