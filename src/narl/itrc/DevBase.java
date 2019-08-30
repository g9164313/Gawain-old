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
			this(0);
		}
		public Act(long millis) {
			setStamp(System.currentTimeMillis());
			setDelay(millis);
			Misc.delay(1);//trick, let all stamp different~~
		}
		public Act setStamp(final long value) {
			stamp = value;
			return this;
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
		public void do_work() {
			if(hook==null) {
			}
			hook.doWork(this);
		}
		
		@Override
		public int compareTo(Delayed obj) {
			long t1 = this.getDelay(TimeUnit.MILLISECONDS);
			long t2 = obj.getDelay(TimeUnit.MILLISECONDS);			
			return (int)(t1-t2);
		}
		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(
				delay - (System.currentTimeMillis() - stamp), 
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
		if(thr_l!=null) {
			thr_l.interrupt();
		}
	}
	protected void take(
		final int repeat,
		final int delay_ms,
		final Act obj_act
	) {
		obj_act.setStamp(System.currentTimeMillis());//reset stamp~~~
		obj_act.setRepeat(repeat);
		obj_act.setDelay(delay_ms);
		action.put(obj_act);
		if(thr_l!=null) {
			thr_l.interrupt();
		}
	}
			
	private Task<?> looper = null;
	private Thread  thr_l = null;
		
	protected void check_repeat(Act act) {
		if(act.repeat>0) {
			act.repeat-=1;
		}
		if(act.repeat==0) {
			return;
		}
		act.setStamp(System.currentTimeMillis());
		action.remove(act);//why do we remove object in queue???
		action.put(act);
	}
	
	//user can override this event
	protected void wait_act(Task<?> looper) {
		try {
			//wait for action
			Thread.sleep(10);
		} catch (InterruptedException e) {
		}
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
						wait_act(looper);
						continue;
					}else {						
						act.do_work();
						check_repeat(act);
					}
					//Misc.logv("act=%d", action.size());
				}while(check_exit()==false);
				Misc.logv("[%s-looper] is done...", TAG);
				return 0;
			}
		};
		
		thr_l = new Thread(
			looper,
			String.format("%s-looper", TAG)
		);
		thr_l.setDaemon(true);
		thr_l.start();
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
				thr_l.interrupt();
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}
	}
}
