package narl.itrc;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.glass.ui.Application;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@SuppressWarnings("restriction")
public abstract class DevBase implements Runnable {
	
	protected String TAG = "DevBase";
	
	public static final AtomicBoolean shutdown = new AtomicBoolean(false); 
	
	abstract public void open();	
	abstract public void close();
	abstract public boolean isLive();
	
	private final StringProperty prop_state_name = new SimpleStringProperty();
	public final ReadOnlyStringProperty PropStateName = prop_state_name;
	
	private boolean is_flowing() {
		if(blockage.get()==true || shutdown.get()==true){
			return false;
		}
		return true;
	}

	protected final AtomicBoolean blockage = new AtomicBoolean(true);
	
	private final ConcurrentHashMap<String,Runnable> state_task = new ConcurrentHashMap<String,Runnable>();
	
	private final AtomicReference<String> next_state_name = new AtomicReference<String>("");
	
	private static final String NAME_BREAK_IN = "__breakIn__";//special state

	@Override
	public void run() {
		//main looper
		while(is_flowing()==true) {
			//first, check whether we had break-in event.
			if(state_task.containsKey(NAME_BREAK_IN)==true) {
				Application.invokeLater(()->prop_state_name.set(NAME_BREAK_IN));
				state_task.get(NAME_BREAK_IN).run();
				state_task.remove(NAME_BREAK_IN);
				continue;
			}
			//go through state-flow
			final String name = next_state_name.get();
			if(name.length()==0) {
				//if name is empty, it means idle state....
				Application.invokeLater(()->prop_state_name.set("--idle--"));
				synchronized(taskFlow) {
					try {
						taskFlow.wait();
					} catch (InterruptedException e) {
						Misc.logv("%s is interrupted!!", TAG);
					}
				}
				continue;
			}
			Application.invokeLater(()->prop_state_name.set(name));
			//device launch a GUI event, let GUI event decide the end time of emergence.<p>
			//device only launch the emergence once!!
			//it will not enter again, if the flag were not set again.!! 
			final Runnable work = state_task.get(name);
			if(work==null) {
				Misc.loge("[%s] invalid state - %s", TAG, name);
				next_state_name.set("");//edge case, no working, just goto idle.
			}else {
				work.run();
			}
		};
		Misc.logv("%s --> close", TAG);
	}

	private Thread taskFlow = null;
	
	public boolean isFlowing() {
		if(taskFlow==null){
			return false;
		}
		return taskFlow.isAlive();
	}
	public DevBase addState(
		final String name,
		final Runnable work
	) {
		state_task.put(name, work);
		return this;
	}
	
	public void playFlow(final String init_state) {
		//invoked by application thread
		if(taskFlow!=null) {
			return;
		}
		next_state_name.set(init_state);
		blockage.set(false);
		taskFlow = new Thread(this,TAG);
		taskFlow.setDaemon(true);
		taskFlow.start();
	}
	public void stopFlow() {
		//invoked by application thread
		blockage.set(true);
		if(Application.isEventThread()!=true){
			try {
				taskFlow.join();
			} catch (InterruptedException e) {
			}
		}
		taskFlow = null;//reset, because thread is 'dead'
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
		if(state_task.containsKey(NAME_BREAK_IN)==true){
			return this;
		}
		state_task.put(NAME_BREAK_IN, work);
		return this;
	}
	public boolean isAsyncDone(){
		if(orph_break_in!=null){
			return !orph_break_in.isAlive();
		}
		if(taskFlow!=null){
			return !state_task.containsKey(NAME_BREAK_IN);
		}
		return true;
	}
	public void blockWaiting(){
		while(isAsyncDone()==false) {
			try {
				TimeUnit.MILLISECONDS.sleep(25L);
			} catch (InterruptedException e1) {
			}
		}
	}
	
	/**
	 * Device will wait for GUI thread.
	 * Important!! the caller(GUI thread) will be blocking by I/O operation.
	 * @param work - runnable code
	 * @return self
	 */
	/*public DevBase syncBreakIn(final Runnable work) {
		if(taskFlow==null) {
			work.run();
			return this;
		}
		nextState("");
		do{
			try {
				TimeUnit.MILLISECONDS.sleep(25L);
			} catch (InterruptedException e1) {
			}
		}while(taskFlow.getState()!=Thread.State.WAITING);
		work.run();
		//nextState(prev_state_name);		
		return this;
	}*/
	
	//TODO: how to check emergence???
	private static final AtomicBoolean is_emergent = new AtomicBoolean(false);
	private static final StringProperty emergency_tag = new SimpleStringProperty();
	public static final ReadOnlyStringProperty EmergencyTag = emergency_tag;	
	public static Optional<Runnable> emergency = Optional.empty();
	
	protected static synchronized void emergency(final String tag) {
		if(is_emergent.get()==true) {
			return;
		}
		is_emergent.set(true);
		Misc.logw("[%s] !!EMERGENCY!!", tag);
		Application.invokeAndWait(()->{			
			emergency_tag.set(tag);
			if(emergency.isPresent()==true) {
				emergency.get().run();//Let GUI thread set flag again~~~
			}else {
				is_emergent.set(false);
			}
		});
	}
	
	public static void ignore_emergency() {
		if(Application.isEventThread()==false) {
			//only GUI-event can decide to exit in an emergency.
			return;
		}
		Misc.logw("~~ Ignore emergency~~");
		is_emergent.set(false);
	}
	
	protected void sleep(final long millisec) {
		try {
			TimeUnit.MILLISECONDS.sleep(millisec);
		} catch (InterruptedException e1) {
		}
	}
}

