package narl.itrc;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.sun.glass.ui.Application;

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
	
	protected abstract boolean looper(TokenBase token);
	
	protected abstract boolean eventReply(TokenBase token);
	
	protected abstract void eventLink(); 
	
	protected abstract void eventUnlink(); 
	
	protected static class TokenBase implements Delayed {
		
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
		 * Whether Looper decides to invoke GUI event.<p> 
		 */
		private boolean isEvent = true;
		
		/**
		 * Whether Looper queues token again.<p> 
		 */
		private boolean isRerun = false;
		
		public TokenBase(){
		}
		public TokenBase(int delay){
			cntDelay = delay;
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
	
	protected static class EventRun implements Runnable{
		
		private DevBase dev = null;
		
		private TokenBase tkn = null;		
		
		private boolean isExit = false;
		
		EventRun(DevBase self){
			dev= self;			
		}		
		public boolean invoke(TokenBase obj){
			if(obj.isEvent==false){
				return true;
			}
			tkn = obj;
			Application.invokeAndWait(this);
			return isExit;
		} 		
		@Override
		public void run() {
			isExit = dev.eventReply(tkn);
		}
	}
	
	private Task<?> looper = null;
	
	private DelayQueue<TokenBase> queuer = new DelayQueue<TokenBase>();
	
	public void link(){
		//check whether looper is running
		if(looper!=null){
			if(looper.isDone()==false){	
				Misc.logw("SQM160 is linked.");
				return;				
			}
		}
		
		//clear previous tokens...
		queuer.clear();
		
		//let user prepare something
		eventLink();
		
		//go, working and working~~~~
		looper = new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				
				EventRun event = new EventRun(DevBase.this);
				
				//this is main looper and device core.
				while(looper.isCancelled()==false){
					
					TokenBase obj = queuer.take();
					
					if(obj.cntBegin<0){						
						break;//special token~~~
					}
					if(looper(obj)==false){
						break;
					}
					if(obj.isEvent==true){
						if(event.invoke(obj)==false){
							break;
						}
					}
					if(obj.isRerun==true){
						obj.cntBegin = System.currentTimeMillis();//count again~~~
						queuer.offer(obj);
					}
				}
				Misc.logv("%s looper is done!!!", TAG);
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
		int delay
	){	
		return offer(
			tkn,
			delay,
			TimeUnit.MILLISECONDS,
			false
		);
	}
	protected DevBase offer(
		TokenBase tkn, 
		int delay,
		boolean repeat
	){	
		return offer(
			tkn,
			delay,
			TimeUnit.MILLISECONDS,
			repeat
		);
	}
	protected DevBase offer(
		TokenBase tkn, 
		int delay,
		TimeUnit unit,
		boolean repeat
	){
		tkn.cntBegin= System.currentTimeMillis();
		tkn.cntDelay= TimeUnit.MILLISECONDS.convert(delay, unit);
		tkn.isRerun = repeat;
		queuer.offer(tkn);
		return this;
	}
	
	/**
	 * clear all token
	 */
	protected void clear_token(){
		queuer.clear();
	}
	
	/**
	 * This is special method, it will remove all 'rerun' token.
	 */
	protected void remove_remainder(){
		for(TokenBase tkn:queuer){
			if(tkn.isRerun==true){
				queuer.remove(tkn);
			}
		}
	}
}
