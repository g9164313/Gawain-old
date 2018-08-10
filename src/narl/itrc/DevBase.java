package narl.itrc;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import javafx.concurrent.Task;

abstract public class DevBase {
	
	protected String TAG = "Abstract Device Layer";
	
	public DevBase(String tag){
		TAG = tag;
	}
	
	protected abstract void looper(Object tkn);
	
	protected abstract void event_link(); 
	
	protected abstract void event_unlink(); 
	
	protected static class Token implements Delayed {

		//count for delay,
		//if it is less than zero, this means skipping from looper. 
		private long cntVal;
		
		private TimeUnit cntUnit;
		
		private boolean repeat;
		
		public Token(){
			set(false, 0, TimeUnit.MILLISECONDS);
		}
		public Token(int val){
			set(false, val, TimeUnit.MILLISECONDS);
		}
		public Token(int val, TimeUnit unit){
			set(false, val, unit);
		}
		public Token(boolean flag, int val, TimeUnit unit){
			set(flag, val, unit);
		}
		
		public void set(int val){
			cntVal = val;
		}
		public void set(TimeUnit unit){
			cntUnit= unit;
		}
		public void set(boolean flag){
			repeat = flag;
		}
		public void set(int val, TimeUnit unit){
			cntVal = val;
			cntUnit= unit;
		}
		public void set(boolean flag, int val, TimeUnit unit){
			repeat = flag;
			cntVal = val;
			cntUnit= unit;			
		}
		
		@Override
		public int compareTo(Delayed o) {
			return (int)(cntVal-o.getDelay(cntUnit));
		}
		@Override
		public long getDelay(TimeUnit unit) {
			if(cntVal<0){
				return 0;
			}
			return unit.convert(cntVal,cntUnit);
		}
	}
	
	private Task<?> looper = null;
	
	private DelayQueue<Token> queuer = new DelayQueue<Token>();
	
	public void link(){
		
		event_link();
		
		if(looper!=null){
			if(looper.isDone()==false){	
				Misc.logw("SQM160 is linked.");
				return;				
			}
		}
		
		queuer.clear();//clear previous token...
		
		looper = new Task<Void>(){
			@Override
			protected Void call() throws Exception {				
				while(looper.isCancelled()==false){
					Token obj = queuer.take();
					if(obj.cntVal<0){						
						break;
					}
					looper(obj);
					if(obj.repeat==true){
						queuer.offer(obj);
					}
				}
				Misc.logv("%s exist!!!", TAG);
				return null;
			}
		};
		
		Thread th = new Thread(looper,TAG);
		th.setDaemon(true);
		th.start();
	}	
	
	public void unlink(){
		queuer.add(new Token(-1));
		if(looper!=null){
			if(looper.isDone()==false){				
				looper.cancel();				
			}
			looper = null;
		}
		event_unlink();
	}
	
	/**
	 * This is special method, it will remove all repeatedly token.
	 */
	protected void remove_remainder(){
		for(Token tkn:queuer){
			if(tkn.repeat==true){
				queuer.remove(tkn);
			}
		}
	}
}
