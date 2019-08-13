package narl.itrc;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DevBase {
	
	public class Act implements Delayed {
		
		public final AtomicBoolean loop = new AtomicBoolean(false);
		
		public long stamp, delay;
		
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
		
		public Act setLoop(final boolean flag) {
			loop.set(flag);
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

	protected void check_loop(final Act act) {
		if(act.loop.get()==false) {
			return;
		}
		//reset stamp again
		act.stamp = System.currentTimeMillis();
		//put this action again
		action.put(act);
	}
	
	protected void abort(final Act act) {
		act.loop.set(false);
		action.remove(act);
	}
	
	protected void take(final Act act) {
		action.put(act);
	}
}
