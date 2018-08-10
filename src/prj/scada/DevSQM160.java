package prj.scada;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import javafx.concurrent.Task;
import narl.itrc.DevTTY;
import narl.itrc.Misc;

public class DevSQM160 extends DevTTY {

	public DevSQM160(){
		this("");
	}
	
	public DevSQM160(String path){
		super(path);
	}
	
	private static class Token implements Delayed {

		private int cnt = 3;//count for delay time~~~
		
		public char cmd = ' ';
		
		public byte[] msg = null;
		
		public Token(char command){	
			cmd = command;
		}
		@Override
		public int compareTo(Delayed o) {
			int t1 = (int)o.getDelay(TimeUnit.SECONDS);
			Misc.logv("compare(%C) -- %d",cmd,t1);
			return -1;
		}
		@Override
		public long getDelay(TimeUnit unit) {
			if(cmd=='@'){
				return unit.convert(cnt--,TimeUnit.SECONDS);
			}
			return 0;
		}
	};
	
	private Task<?> looper = null;
	
	private DelayQueue<Token> queuer = new DelayQueue<Token>();
	
	public void link(){
		link(getPathName());
	}
	
	public void link(String path){
		
		//if(open(path)==false){
		//	return;
		//}	
		if(looper!=null){
			if(looper.isDone()==false){	
				Misc.logw("SQM160 is linked.");
				return;				
			}
		}
		queuer.clear();
		looper = new Task<Void>(){
			@Override
			protected Void call() throws Exception {				
				while(looper.isCancelled()==false){
					Token tkn = (Token)queuer.take();
					Misc.logv("token=%C", tkn.cmd);
				}
				return null;
			}
		};
		
		Thread th = new Thread(looper,"Dev-SQM160");
		th.setDaemon(true);
		th.start();
	}
	
	public void unlink(){
		
		queuer.put(new Token('E'));
		
		if(looper!=null){
			if(looper.isDone()==false){				
				looper.cancel();				
			}
			looper = null;
		}
		close();
	}
	
	//---------------------------------//
	
	public void test_event1(){
		queuer.offer(new Token('@'), 3L, TimeUnit.SECONDS);
	}
	
	public void test_event2(){
		queuer.offer(new Token('A'));
	}
}
