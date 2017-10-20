package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

public abstract class TskAction implements EventHandler<ActionEvent> {

	private static final String DEF_NAME = "TskAction";
	
	protected String name = DEF_NAME;
	
	protected PanBase root = null;
	
	private Task<Integer> task = null;
		
	public int result = 0;

	/**
	 * Special variable. When user create key-frames，we don't know whether action is starting.<p>
	 * So, set this variable TRUE to indicate this action is starting!!!.<p> 
	 */
	public boolean isTrigger = false;
	
	public TskAction(){		
	}
	
	public TskAction(String name){
		this(name,null);
	}
	
	public TskAction(PanBase root){
		this(DEF_NAME,root);
	}
	
	public TskAction(String title,PanBase panel){
		setName(title);
		setRoot(panel);
	}
	
	public void setName(String title){
		name = title;
	}
	
	public void setRoot(PanBase panel){
		root = panel;
	}
	
	/**
	 * user can override this function<p>
	 * This happened before looper.<p> 
	 * And It is invoked by GUI thread
	 */
	protected boolean eventBegin(){
		return true;
	}
	
	/**
	 * user must implements this function, it is a infinite loop!!!
	 * @param task - Task<Integer>
	 * @return 0 - working, !0 - exist
	 */
	public abstract int looper(Task<Integer> tsk);
	
	/**
	 * When task is finished, <p>
	 * And It is invoked by GUI thread
	 */
	protected void eventFinish(){
	}

	private void eventDone(){
		if(root!=null){
			//Task have already been stop,so we don't pass second argument~~~
			root.spinning(false,null);
		}
		eventFinish();
	}
	
	public boolean isRunning(){
		if(task==null){
			return false;
		}
		return task.isRunning();
	}

	public void stop(){
		if(task!=null){
			if(task.isDone()==false){
				task.cancel();
			}
		}
	}

	public boolean start(){
		result = -1;
		if(eventBegin()==false){
			return false;
		}
		if(task!=null){
			if(task.isDone()==false){
				//we are already running, just skip this calling!!!
				return false;				
			}			
		}
		if(root!=null){
			root.spinning(true,this);
		}
		
		task = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				result = 0;//always reset this variable~~~
				while(task.isCancelled()==false && result==0){
					result = looper(task);
					if(Application.GetApplication()==null){						
						result = -2;//Platform is shutdown						
						break;
					}
				}
				Application.invokeAndWait(()->eventDone());
				return result;
			}
		};
		task.setOnCancelled(event->eventDone());
		
		new Thread(task,name).start();
		return true;
	}
	
	@Override
	public void handle(ActionEvent event) {
		if(task!=null){
			if(task.isDone()==false){
				task.cancel();
				Misc.logv("取消任務中...");
				return;
			}			
		}
		start();
	}
	//---------------------------------//
	
	/**
	 * just create a JavaFx-Task object
	 * @param title - task name
	 * @param core - just reflect Thread class
	 * @param task - user must provide context, and This will ignore internal task 
	 * @return
	 */
	public static Thread create(
		String title,
		Thread core,
		Task<Void> task
	){
		if(core!=null){
			if(core.isAlive()==true){				
				return core;
			}
		}
		core = new Thread(task,title);
		core.start();
		return core;
	}
	
	@SuppressWarnings("unchecked") 
	public static EventHandler<ActionEvent> createKeyframe(
		final EventHandler<ActionEvent>... lst
	){
		return createKeyframe(false,lst);
	}
	
	@SuppressWarnings("unchecked") 
	public static EventHandler<ActionEvent> createKeyframe(
		final boolean isRepeat, 
		final EventHandler<ActionEvent>... lst
	){		
		final EventHandler<ActionEvent> act_keyframe = 
			new EventHandler<ActionEvent>()
		{
			private int idx = 0;
			
			private Timeline timer = null;
			
			private void reset(){
				timer.stop();
				timer = null;//reset this				
				for(Object obj:lst){
					if(obj instanceof TskAction){
						((TskAction)obj).isTrigger = false;//reset it for next turn~~~~
					}
				}
			}
			
			private KeyFrame looper = new KeyFrame(Duration.millis(250), event->{
				if(idx>=lst.length){
					if(isRepeat==true){
						idx = 0;//reset index~~~~
					}else{
						reset();//we done!!!
						Misc.logv("任務完成...");
						return;
					}					
				}
				Object obj = lst[idx];
				//how do we trigger???
				if(obj instanceof TskAction){
					
					TskAction act = (TskAction)obj;
					if(act.isTrigger==false){
						act.isTrigger=true;
						act.handle(event);
						return;
					}					
					if(act.isRunning()==true){
						return;//we are still waiting~~~~
					}
					act.isTrigger = false;
					
				}else if(obj instanceof EventHandler<?>){
					((EventHandler<ActionEvent>)obj).handle(event);
				}
				idx++;
			});
			
			@Override
			public void handle(ActionEvent e) {
				idx = 0; //reset index~~~
				if(timer!=null){					
					reset();
					Misc.logv("取消任務中...");
					return;
				}
				timer = new Timeline();
				timer.setCycleCount(Timeline.INDEFINITE);
				timer.getKeyFrames().add(looper);
				timer.play();
			}			
		};
		return act_keyframe;		
	}	
}
