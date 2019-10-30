package narl.itrc;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DevBase implements Runnable {
	
	protected String TAG = "DevBase";
	
	public DevBase(){
	}

	abstract public void open();	
	abstract public void close();
	abstract public boolean isLive();
	
	public interface Work {
		public void doWork(Action act);
	}
	protected class Action {
		
		protected short stgx;
		protected short next;		
		protected Work hook;
		
		public Action() {
			this(nxt_stage,DEATH_STAGE,null);
		}
		public Action(
			final int stage,
			final Work work
		) {
			this(stage,stage+1,work);
		}
		public Action(
			final int stage,
			final int next_stage,
			final Work work
		) {
			stgx = (short)stage;
			next = (short)next_stage;
			hook = work;
		}
		private void do_work() {
			if(hook==null) {
				return;
			}
			hook.doWork(this);
		}
	};
	protected final ConcurrentLinkedQueue<Action> action = new ConcurrentLinkedQueue<Action>();
		
	public DevBase doing(final Action act) {
		action.offer(act);
		if(looper!=null) {
			looper.interrupt();
		}
		return this;
	}		
	public DevBase doing(
		final int stgx,
		final DevBase.Work work
	) {
		doing(new Action(stgx,work));
		return this;
	}
	public DevBase doing(
		final int stgx,
		final int next,
		final DevBase.Work work
	) {
		doing(new Action(stgx,next,work));
		return this;
	}
	public DevBase doingNow(final DevBase.Work work) {
		doing(new Action(0,0,work));
		return this;
	}
	
	
	public final AtomicBoolean exitLoop = new AtomicBoolean();
	
	private short nxt_stage;
	private final short SELF_STAGE =-1;//goto self
	private final short DEATH_STAGE= 0;//poll action
	private final short FIRST_STAGE= 1;//first action
		
	private final int MAX_STGX_FAIL = 5;
	private int stgx_fail_cnt = MAX_STGX_FAIL;
	
	private void doAction() {
		
		if(action.isEmpty()==true) {
			return;
		}
		
		Action act = action.poll();		
		final short head = act.stgx;
		do{
			short act_stgx = (short)Math.abs(act.stgx);
			if(act_stgx==nxt_stage || act_stgx==DEATH_STAGE) {
				
				act.do_work();
				
				if(act.next==SELF_STAGE){
					
					nxt_stage = act_stgx;
					action.offer(act);
					
				}else if(act.next==DEATH_STAGE) {
					
					if(act_stgx!=DEATH_STAGE) {
						nxt_stage = (short)(act.stgx + 1);
					}
					
				}else if(act.next>=FIRST_STAGE) {
					
					nxt_stage = act.next;
					if(act.stgx<SELF_STAGE) {
						action.offer(act);
					}
				}
				return;
			}else {
				action.offer(act);
				act = action.poll();
			}
		}while(head!=act.stgx);	
		
		if(stgx_fail_cnt<=0) {
			//not match stage, reset stage number!!!
			nxt_stage = (short)Math.abs(act.stgx);
			stgx_fail_cnt = MAX_STGX_FAIL;//reset it again~~~
		}else {
			stgx_fail_cnt -=1;
		}
		action.offer(act);
	}
	
	//user must override this event
	protected void doLoop(DevBase dev) {
	}
	
	@Override
	public void run() {
		nxt_stage = FIRST_STAGE;
		do {
			doAction();
			doLoop(this);
		}while(exitLoop.get()==false);
		Misc.logv("%s is deadth", TAG);
	}

	
	private Thread looper = null;
	
	protected void beforeLoop() {}
	protected void afterLoop() {}
	
	public void startLoop() {		
		if(looper!=null) {
			if(looper.isAlive()==true) {
				return;
			}
		}
		action.clear();		
		exitLoop.set(false);
		looper = new Thread(this,TAG);
		looper.setDaemon(true);
		beforeLoop();
		looper.start();
		afterLoop();
	} 
		
	public void stopLoop() {		
		if(looper==null) {
			return;
		}
		if(looper.isAlive()==false) {
			return;
		}		
		try {
			exitLoop.set(true);
			looper.interrupt();
			looper.join();
			looper = null;
		} catch (InterruptedException e) {
		}
	}
}
