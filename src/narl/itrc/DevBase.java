package narl.itrc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public abstract class DevBase implements Runnable {
	
	protected String TAG = "DevBase";
	
	abstract public void open();	
	abstract public void close();
	abstract public boolean isLive();
	
	//public StringProperty state = new SimpleStringProperty();
	
	public final StringProperty propStateName = new SimpleStringProperty();
	
	private String get_next_state_name() {
		final String name = next_state_name.get();
		if(name.length()==0) {
			Application.invokeLater(()->propStateName.set("--idle--"));
		}else {
			Application.invokeLater(()->propStateName.set(name));
		}
		return name;
	}
	
	protected final AtomicBoolean isExist = new AtomicBoolean(true);
	
	private final ConcurrentHashMap<String,Runnable> state = new ConcurrentHashMap<String,Runnable>();
	
	private final AtomicReference<String> next_state_name = new AtomicReference<String>("");
	
	private String prev_state_name = "";
	
	private static final String STA_BREAK_IN = "__breakIn__";//special state
	
	@Override
	public void run() {
		//main looper
		do {
			if(
				isExist.get()==true ||
				Gawain.isKill.get()==true
			){
				break;
			}
			//go through state-flow
			String name = get_next_state_name();
			if(name.length()==0) {
				//if name is empty, it means idle....
				synchronized(taskFlow) {
					try {
						taskFlow.wait();
					} catch (InterruptedException e) {
					}
				}
				continue;
			}
			Runnable work = state.get(name);
			if(work==null) {
				Misc.loge("[%s] invalid state - %s", TAG,name);
				next_state_name.set("");
			}else {
				work.run();
				if(name.equals(STA_BREAK_IN)==true) {
					state.remove(STA_BREAK_IN);
					//next state no changed, go back to previous state.
					name = next_state_name.get();
					if(name.equals(STA_BREAK_IN)==true) {
						next_state_name.set(prev_state_name);
					}
				}else {
					//GUI-thread may change state during work.run() function.
					name = next_state_name.get();					
					if(name.equals(STA_BREAK_IN)==false) {
						//update previous state, but ignore special state.
						prev_state_name = name;
					}
				}
			}
		}while(true);
		Misc.logv("%s --> close", TAG);
	}

	private Thread taskFlow = null;
	
	public boolean isFlowing() {
		if(taskFlow==null){
			return false;
		}
		return taskFlow.isAlive();
	}
	
	public boolean isBreaking(){
		return state.contains(STA_BREAK_IN);
	}
	
	public DevBase addState(
		final String name,
		final Runnable work
	) {
		state.put(name, work);
		return this;
	}
	
	public void playFlow(final String init_state) {
		if(taskFlow!=null) {
			return;
		}
		next_state_name.set(init_state);
		isExist.set(false);
		taskFlow = new Thread(this,TAG);
		taskFlow.setDaemon(true);
		taskFlow.start();
	}
	
	public void stopFlow() {
		isExist.set(true);
		if(Application.isEventThread()!=true){
			try {
				taskFlow.join();
			} catch (InterruptedException e) {
			}
		}
		taskFlow = null;//reset this flag~~~
	}
	
	public void nextState(final String name) {
		next_state_name.set(name);
		if(taskFlow.getState()==Thread.State.WAITING) {
			synchronized(taskFlow) {
				taskFlow.notify();
			}
		}else if(taskFlow.getState()==Thread.State.TIMED_WAITING) {
			taskFlow.interrupt();
		}
	}
	
	private Thread orph_break_in = null;//independent break-in task
	/**
	 * Caller won't be blocked.
	 * @param work - runnable code
	 * @return self
	 */
	public DevBase asyncBreakIn(final Runnable work) {
		if(taskFlow==null) {
			if(orph_break_in!=null) {
				if(orph_break_in.isAlive()==true) {
					Misc.logw("%s is busy!!",TAG);
					return this;
				}
			}
			orph_break_in = new Thread(work,TAG+"-breakin");
			orph_break_in.start();
			return this;
		}
		if(state.contains(STA_BREAK_IN)==true){
			return this;
		}
		state.put(STA_BREAK_IN, work);
		nextState(STA_BREAK_IN);
		return this;
	}
	public boolean isAsyncDone(){
		if(orph_break_in!=null){
			return !orph_break_in.isAlive();
		}
		if(taskFlow!=null){
			return !state.contains(STA_BREAK_IN);
		}
		return true;
	}
	public void asyncBlocking(){
		while(isAsyncDone()==false) {
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Device will wait for caller.
	 * @param work - runnable code
	 * @return self
	 */
	public DevBase syncBreakIn(final Runnable work) {
		if(taskFlow==null) {
			work.run();
			return this;
		}
		idleState();
		work.run();
		nextState(prev_state_name);		
		return this;
	}
	private void idleState() {
		nextState("");		
		do{
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		}while(taskFlow.getState()!=Thread.State.WAITING);
	}
	
	protected void blocking_delay(final long millisec) {
		try {
			Thread.sleep(millisec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

