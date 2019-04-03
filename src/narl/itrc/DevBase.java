package narl.itrc;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;


abstract public class DevBase {
	
	protected String TAG = "Abstract Device Layer";
	
	protected final Alert alert = new Alert(AlertType.INFORMATION);
	
	public DevBase(String tag){
		TAG = tag;
		alert.setHeaderText("");
	}
	
	protected abstract boolean eventLink();
	
	protected abstract boolean afterLink();
	
	protected abstract void beforeUnlink();
	
	protected abstract void eventUnlink(); 
	
	protected static abstract class Work implements Delayed {
		
		/**
		 * initial time tick for counting delay.<p>
		 * unit is MILLISECONDS 
		 */
		private long cntBegin = 0L;
		
		/**
		 * indicate how long time passes.<p>
		 * unit is MILLISECONDS
		 */
		private long cntDelay = 0L;
		
		/**
		 * Whether Looper queues token again.<p> 
		 */
		public boolean isPermanent = false;
		
		public Work(){
		}
		public Work(int delay_ms){
			cntDelay = delay_ms;
		}
		public Work(int delay_ms, boolean permanent){
			cntDelay = delay_ms;
			isPermanent = permanent;
		}
		
		public abstract int looper(Work obj, final int pass);
		
		public abstract int event(Work obj, final int pass);
		
		@Override
		public int compareTo(Delayed o) {
			long t1 = getDelay(null);
			long t2 = o.getDelay(null);
			return (int)(t1-t2);
		}
		@Override
		public long getDelay(TimeUnit unit) {
			long past = System.currentTimeMillis()-cntBegin;
			if(past>=cntDelay){
				return 0;
			}
			return cntDelay;
		}
	}
	
	private Task<?> looper = null;
	
	private DelayQueue<Work> queuer = new DelayQueue<Work>();
	
	public void link(){
		
		//check whether looper is running
		if(looper!=null){
			if(looper.isDone()==false){
				return;				
			}
		}

		//let user prepare something
		if(eventLink()==false){
			return;
		}
		
		//go, working and working~~~~
		looper = new Task<Void>(){
			int pass = 0;
			@Override
			protected Void call() throws Exception {
				//initialize everything or setup
				if(afterLink()==false){
					return null;
				}								
				//this is main looper and device core.
				while(looper.isCancelled()==false){
					
					Work obj = queuer.take();					
					if(obj.cntBegin<0){
						//special case!!!
						//user want to escape looper~~~
						break;
					}
					
					pass = 0;
					do{
						pass = obj.looper(obj, pass);
						if(pass>0){
							Application.invokeAndWait(()->{
								pass = obj.event(obj, pass);						
							});
						}	
					}while(pass!=0 && looper.isCancelled()==false);

					if(obj.isPermanent==true){
						//push-back and count again~~~
						obj.cntBegin = System.currentTimeMillis();
						queuer.offer(obj);
					}
				}				
				//Before ending, release resource.
				beforeUnlink();				
				return null;
			}
		};
		
		Thread th = new Thread(looper,TAG);
		th.setDaemon(true);
		th.start();
	}
	
	private static final Work work_done = new Work(-1){
		@Override
		public int looper(Work obj, int pass) {
			return 0;
		}
		@Override
		public int event(Work obj,int pass) {
			return 0;
		}
	};
	
	public void unlink(){
		queuer.add(work_done);
		if(looper!=null){
			if(looper.isDone()==false){				
				looper.cancel();				
			}
			looper = null;
		}		
		eventUnlink();
		queuer.clear();//clear token for next turn~~~
	}
	
	public DevBase offer(Work token){
		queuer.offer(token);
		return this;
	}
	public DevBase offer(
		int delay_ms,
		Work token		
	){	
		return offer(			
			delay_ms,
			TimeUnit.MILLISECONDS,
			false,
			token
		);
	}
	public DevBase offer(		
		int delay_ms,
		boolean permanent,
		Work token
	){	
		return offer(			
			delay_ms,
			TimeUnit.MILLISECONDS,
			permanent,
			token
		);
	}
	
	public DevBase offer(		
		int delay,
		TimeUnit unit,
		boolean permanent,
		Work tkn
	){
		tkn.cntBegin= System.currentTimeMillis();
		tkn.cntDelay= TimeUnit.MILLISECONDS.convert(delay, unit);
		tkn.isPermanent = permanent;
		queuer.offer(tkn);
		return this;
	}
	
	public DevBase remove(Work tkn){
		tkn.isPermanent = false;
		queuer.remove(tkn);
		return this;
	}
}
