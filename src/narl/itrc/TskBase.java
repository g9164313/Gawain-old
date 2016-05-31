package narl.itrc;

import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

abstract class TskBase {

	private Task<Integer> tsk;
	
	private boolean reentry = false;
	
	/**
	 * no special mean, user can arbitrarily use this variable
	 */
	public AtomicInteger arg1 = new AtomicInteger(0);
	
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
	abstract int looper(Task<Integer> task);
	
	/**
	 * This happened when user cancel task, use can also override routine<p>
	 * It is invoked by GUI thread.
	 */
	protected void eventCancel(){
	}
	
	public void stop(){
		if(tsk!=null){
			if(tsk.isRunning()==false){
				tsk.cancel();
			}
		}
	}
	
	private int result = 0;
	
	public boolean start(){
		return start("unknow-task");
	}
	
	public boolean start(String name){
		
		if(tsk!=null){
			if(tsk.isRunning()==true){
				if(reentry==true){
					do{
						tsk.cancel();
					}while(tsk.isDone()==false);
				}else{
					return false;
				}				
			}			
		}
		
		tsk = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				result = 0;
				Application.invokeAndWait(new Runnable(){
					@Override
					public void run() {
						if(eventInit()==false){
							result = -1;
						}
					}					
				});
				while(tsk.isCancelled()==false && result==0){
					result = looper(tsk);
					if(Application.GetApplication()==null){						
						//Platform is shutdown
						result = -2;
						break;
					}
				}
				return result;
			}
		};
		
		tsk.setOnCancelled(new EventHandler<WorkerStateEvent>(){
			@Override
			public void handle(WorkerStateEvent event) {
				eventCancel();
			}
		});
		
		new Thread(tsk,name).start();
		
		return true;
	}	
}