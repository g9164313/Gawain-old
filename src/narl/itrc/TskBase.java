package narl.itrc;

import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;

abstract class TskBase {

	private Task<Integer> task;
	
	private boolean reentry = false;
	
	/**
	 * no special mean, user can arbitrarily use this variable
	 */
	public AtomicInteger arg1 = new AtomicInteger(0);
	
	protected int result = 0;
	
	public TskBase(){		
	}
	
	public TskBase(boolean flag){
		reentry = flag;
	}
	
	/**
	 * user can override this function<p>
	 * This happened before looper.<p> 
	 * And It is invoked by GUI thread
	 */
	protected boolean eventInit(){
		return true;
	}
	
	/**
	 * user must implements this function, it is a infinite loop!!!
	 * @param task - Task<Integer>
	 * @return 0 - working, !0 - exist
	 */
	public abstract int looper(Task<Integer> task);
	
	/**
	 * This happened when user cancel task, use can also override routine<p>
	 * It is invoked by GUI thread.
	 */
	protected void eventCancel(){
	}
	
	public void stop(){
		if(task!=null){
			if(task.isDone()==false){
				task.cancel();
			}
		}
	}

	public boolean start(){
		return start("unknow-task");
	}
	
	public boolean start(String name){
		
		if(task!=null){
			if(task.isRunning()==true){
				if(reentry==true){
					do{
						task.cancel();
					}while(task.isDone()==false);
				}else{
					return false;
				}				
			}			
		}
		
		if(eventInit()==false){
			result = -1;
			return false;
		}
		
		task = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				result = 0;
				while(task.isCancelled()==false && result==0){
					result = looper(task);
					if(Application.GetApplication()==null){						
						result = -2;//Platform is shutdown
						break;
					}
				}
				return result;
			}
		};
		
		task.setOnCancelled(EVENT->eventCancel());
		
		new Thread(task,name).start();
		
		return true;
	}	
}
