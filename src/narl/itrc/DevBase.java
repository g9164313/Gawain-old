package narl.itrc;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import javafx.concurrent.Task;

public class DevBase {
	
	public interface Work {
		public void doWork(DevBase.Act act);
	}
	
	public class Act implements Delayed {
		
		/**
		 * indicate this action should be pushed again.<p>
		 * >=1 means counting.<p>
		 * ==0 means skipping from looper.<p>
		 * =-1 means infinite running.<p>
		 */
		public int repeat;
		
		public long stamp, delay;
		
		public Work hook;
		
		public Act() {
			this(-1);
		}
		public Act(long millis) {
			stamp = System.currentTimeMillis();
			delay = millis;
		}		
		public Act setDelay(final long value) {
			delay = value;
			return this;
		}
		public Act setRepeat(final int value) {
			repeat = value;
			return this;
		}
		public Act setWork(final Work value) {
			hook = value;
			return this;
		}
		@Override
		public int compareTo(Delayed obj) {
			return (int)(delay-obj.getDelay(TimeUnit.MILLISECONDS));
		}
		@Override
		public long getDelay(TimeUnit unit) {
			if(delay<=0) {
				return 0L;
			}
			long past = delay-(System.currentTimeMillis()-stamp);
			if(past<=0L) {
				return 0L;
			}
			return unit.convert(
				past, 
				TimeUnit.MILLISECONDS
			);
		}
	};
	
	protected String TAG = "dev-base";
	
	public DevBase(){		
	}
	
	protected final DelayQueue<Act> action = new DelayQueue<Act>();

	protected void abort(final Act act) {
		act.repeat = 0;
		action.remove(act);
	}
	
	protected void take(final Act act) {
		action.put(act);
	}
	
	protected void take(
		final int repeat,
		final int delay_ms,
		final DevBase.Work work
	) {
		Act obj = new Act();
		obj.setRepeat(repeat);
		obj.setDelay(delay_ms);
		obj.setWork(work);
		action.put(obj);
		if(l_core!=null) {
			l_core.interrupt();
		}
	}
	
	private Task<?> looper = null;
	private Thread  l_core = null;
		
	protected void check_repeat(Act act) {
		if(act.repeat>0) {
			act.repeat-=1;
		}
		if(act.repeat==0) {
			return;
		}
		act.stamp = System.currentTimeMillis();
		action.put(act);
	}
	
	protected void looper_start() {
		if(looper!=null) {
			if(looper.isDone()==false) {
				return;
			}
		}
		looper = new Task<Integer>() {
			
			private boolean check_exit() {
				return looper.isCancelled() | Gawain.isExit();
			}
			
			@Override
			protected Integer call() throws Exception {
				do {
					Act act = action.poll();
					if(act==null) {
						try {
							//wait for action
							updateMessage("Idle");
							Thread.sleep(100);
						} catch (InterruptedException e) {
						}
						continue;
					}else {
						if(act.hook!=null) {
							//updateMessage("");
							act.hook.doWork(act);
						}
						check_repeat(act);
					}					
				}while(check_exit()==false);				
				return 0;
			}
		};
		
		l_core = new Thread(
			looper,
			String.format("%s-looper", TAG)
		);
		l_core.setDaemon(true);
		l_core.start();
	} 
	
	protected void looper_stop() {
		if(looper==null) {
			return;
		}
		if(looper.isDone()==true) {
			return;
		}
		while(looper.cancel()==false) {
			try {
				l_core.interrupt();
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}
	}
}
