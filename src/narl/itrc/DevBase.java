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
			//go through state-flow
			String name = get_next_state_name();
			if(name.length()==0) {
				//if name is empty, it means idle....
				synchronized(task) {
					try {
						task.wait();
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
		}while(Gawain.isExit()==false && isExist.get()==false);
		close();
	}

	private Thread task = null;
	
	public boolean isFlowing() {
		if(task==null){
			return false;
		}
		return task.isAlive();
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
		if(task!=null) {
			return;
		}
		next_state_name.set(init_state);
		isExist.set(false);
		task = new Thread(this,TAG);
		task.setDaemon(true);
		task.start();
	}
	
	public void stopFlow() {
		isExist.set(true);
		try {
			task.join();
			task = null;//reset this flag~~~
		} catch (InterruptedException e) {
		}
	}
	
	public void nextState(final String name) {
		next_state_name.set(name);
		if(task.getState()==Thread.State.WAITING) {
			synchronized(task) {
				task.notify();
			}
		}else if(task.getState()==Thread.State.TIMED_WAITING) {
			task.interrupt();
		}
	}
	
	/**
	 * Caller won't be blocked.
	 * @param work - runnable code
	 * @return self
	 */
	public DevBase asyncBreakIn(final Runnable work) {
		if(task==null) {
			work.run();
			return this;
		}
		if(state.contains(STA_BREAK_IN)==true){
			return this;
		}
		state.put(STA_BREAK_IN, work);
		nextState(STA_BREAK_IN);
		return this;
	}
	
	/**
	 * Device will wait for caller.
	 * @param work - runnable code
	 * @return self
	 */
	public DevBase syncBreakIn(final Runnable work) {
		if(task==null) {
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
		}while(task.getState()!=Thread.State.WAITING);
	}
}

