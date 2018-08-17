package narl.itrc;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

abstract public class DevBase {
	
	protected String TAG = "Abstract Device Layer";
	
	protected final Alert alert = new Alert(AlertType.INFORMATION);
	
	public DevBase(String tag){
		TAG = tag;
		alert.setHeaderText("");
	}
	
	protected abstract boolean looper(Object token);
	
	protected abstract void eventLink(); 
	
	protected abstract void eventUnlink(); 
	
	protected static class TokenBase implements Delayed {
		
		//count for delay,
		//base value for time count.
		//unit is MILLISECONDS
		private long cntVal = 0L;
		
		private long cntMax = 0L;
		
		private boolean repeat = false;
		
		public TokenBase(){
		}
		public TokenBase(int val){
			cntMax = val;
		}
		
		/**
		 * This event must be executed by GUI-thread.
		 */
		private EventHandler<ActionEvent> eventHandler = null;
		/**
		 * User can get 'Token' from action source.
		 */
		private ActionEvent eventSource = new ActionEvent(this, null);
		/**
		 * Hook a method for UI response in looper.
		 * @param event
		 * @return
		 */
		public TokenBase setOnAction(EventHandler<ActionEvent> event){
			eventHandler = event;
			return this;
		}
		/**
		 * This event must be executed by GUI-thread.
		 */
		public void action(){
			if(eventHandler==null){
				return;
			}
			
			eventHandler.handle(eventSource);
		}
		
		@Override
		public int compareTo(Delayed o) {
			return (int)(cntMax-o.getDelay(null));
		}
		@Override
		public long getDelay(TimeUnit unit) {
			if(cntMax<0){
				return 0;
			}
			long past = System.currentTimeMillis()-cntVal;
			if(past>=cntMax){
				return 0;
			}
			return cntMax-past;
		}
	}
	
	private Task<?> looper = null;
	
	private DelayQueue<TokenBase> queuer = new DelayQueue<TokenBase>();
	
	public void link(){
		
		if(looper!=null){
			if(looper.isDone()==false){	
				Misc.logw("SQM160 is linked.");
				return;				
			}
		}
				
		queuer.clear();//clear previous token...
		
		eventLink();
		
		looper = new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				
				while(looper.isCancelled()==false){
					
					TokenBase obj = queuer.take();
					
					if(obj.cntVal<0){						
						break;//special token~~~
					}
					if(looper(obj)==false){
						break;
					}	
					if(obj.repeat==true){
						obj.cntVal = System.currentTimeMillis();
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
		queuer.add(new TokenBase(-1));
		if(looper!=null){
			if(looper.isDone()==false){				
				looper.cancel();				
			}
			looper = null;
		}
		eventUnlink();
	}
	
	protected DevBase offer(TokenBase tkn){
		return offer(
			tkn,
			0,
			TimeUnit.MILLISECONDS,
			false
		);
	}
	protected DevBase offer(
		TokenBase tkn, 
		int value
	){	
		return offer(
			tkn,
			value,
			TimeUnit.MILLISECONDS,
			false
		);
	}
	protected DevBase offer(
		TokenBase tkn, 
		int value,
		TimeUnit unit
	){
		return offer(
			tkn,
			value,
			unit,
			false
		);
	}
	protected DevBase offer(
		TokenBase tkn, 
		int value,
		TimeUnit unit,
		boolean flag
	){
		tkn.cntVal = System.currentTimeMillis();
		tkn.cntMax = TimeUnit.MILLISECONDS.convert(value, unit);
		tkn.repeat = flag;
		queuer.offer(tkn);
		return this;
	}
	
	/**
	 * This is special method, it will remove all repeatedly token.
	 */
	protected void remove_remainder(){
		for(TokenBase tkn:queuer){
			if(tkn.repeat==true){
				queuer.remove(tkn);
			}
		}
	}
}
