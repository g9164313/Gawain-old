package narl.itrc;

import java.util.NoSuchElementException;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;


abstract public class DevBase1 {
	
	protected String TAG = "Abstract Device Layer";
	
	protected final Alert alert = new Alert(AlertType.INFORMATION);
	
	public DevBase1(String tag){
		TAG = tag;
		alert.setHeaderText("");
	}
	
	protected abstract boolean eventLink();
	
	protected abstract boolean afterLink();

	protected abstract void beforeUnlink();
	
	protected abstract void eventUnlink(); 	
	
	protected static interface WorkLooper {
		abstract void looper(Work obj);
	};
	
	protected static abstract class Work implements Delayed {
		
		/**
		 * initial time tick for counting delay.<p>
		 * unit is MILLISECONDS.<p>
		 */
		private long cntBegin = 0L;
		
		/**
		 * indicate how long time passes.<p>
		 * unit is MILLISECONDS.<p>
		 */
		protected long cntDelay = 0L;
		
		/**
		 * Whether Looper queues token again.<p> 
		 */
		public boolean durable = false;
		
		public Work(){
		}
		public Work(int delay_ms){
			cntDelay= delay_ms;
		}
		public Work(int delay_ms, boolean isPermanent){
			cntDelay= delay_ms;
			durable = isPermanent;
		}
		
		public void setDelay(long millisecond){
			cntBegin = System.currentTimeMillis();
			cntDelay = millisecond;
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
	
	protected boolean isCanceled(){
		if(looper==null){
			return true;
		}
		return looper.isCancelled();
	}
	
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
					//start!!, try to get work~~~~
					Work obj = queuer.take();					
					if(obj.cntBegin<0){
						//special case!!!
						//user want to escape looper~~~
						break;
					}
					//main looper
					pass = 0;
					do{
						pass = obj.looper(obj, pass);
						if(pass!=0){
							Application.invokeAndWait(()->{
								pass = obj.event(obj, pass);						
							});
						}	
					}while(pass!=0 && looper.isCancelled()==false);
					//check if we need this work again~~~
					if(obj.durable==true){
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
		if(looper!=null){
			queuer.add(work_done);
			do{				
				looper.cancel();
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}while(looper.isDone()==false);
			looper = null;
		}		
		eventUnlink();
		queuer.clear();//clear token for next turn~~~
	}
	
	public DevBase1 offer(WorkLooper task){
		queuer.offer(new Work(){
			@Override
			public int looper(Work obj, int pass) {
				task.looper(obj);
				return 0;
			}
			@Override
			public int event(Work obj, int pass) {
				return 0;
			}
		});
		return this;
	}
	
	public DevBase1 offer(Work token){
		if(queuer.contains(token)==true) {
			return this;
		}
		queuer.offer(token);
		return this;
	}
	public DevBase1 offer(
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
	public DevBase1 offer(		
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
	
	public DevBase1 offer(		
		int delay,
		TimeUnit unit,
		boolean permanent,
		Work tkn
	){
		if(queuer.contains(tkn)==true){
			return this;
		}
		tkn.setDelay(TimeUnit.MILLISECONDS.convert(delay, unit));
		tkn.durable = permanent;
		queuer.offer(tkn);
		return this;
	}
	
	private Work core_inner;
	protected int core_looper(Work obj, int pass){ return 0; }
	protected int core_event (Work obj, int pass){ return 0; }	
	/**
	 * provide a convenience method for infinite looper.<p>
	 * User can override 'core_looper' and 'core_event' method.<p>
	 * @param ms - time to repeat action
	 * @return self
	 */
	protected DevBase1 createLooper(int ms){
		if(core_inner==null){
			core_inner = new Work(){
				@Override
				public int looper(Work obj, int pass) {
					return DevBase1.this.core_looper(obj, pass);
				}
				@Override
				public int event(Work obj, int pass) {
					return DevBase1.this.core_event(obj, pass);
				}
			};
			core_inner.setDelay(ms);
		}else{
			core_inner.setDelay(ms);
			return this;
		}
		core_inner.durable = true;
		return offer(core_inner);
	}
	
	public int countWork(){
		return queuer.size();
	}
	
	public DevBase1 remove(Work tkn){
		tkn.durable = false;
		queuer.remove(tkn);
		return this;
	}
	
	//TODO:how to purge???
	//public DevBase purge(){
		//queuer.iterator()
		//queuer.removeAll();
		//return this;
	//}
	
	protected void waitForEmpty(){
		do{
			try{
				queuer.remove();
				Thread.sleep(50);
			}catch(NoSuchElementException | InterruptedException e){
				return;
			}
		}while(queuer.isEmpty()==false);
	}	
}
