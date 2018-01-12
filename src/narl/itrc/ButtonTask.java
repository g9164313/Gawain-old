package narl.itrc;

import com.jfoenix.controls.JFXButton;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;

public class ButtonTask extends JFXButton {

	private String text = Misc.TXT_UNKNOW;
	
	private ImageView icon = null;
	
	public ButtonTask(){
		this(Misc.TXT_UNKNOW,null,"btn-raised-1");
	}
	
	public ButtonTask(String title){
		this(title,null,"btn-raised-2");
	}
	
	public ButtonTask(String title, String iconName){
		this(title,iconName,"btn-raised-2");
	}
	
	public ButtonTask(String title, String iconName, String styleName){
		
		getStyleClass().add(styleName);
		setOnAction(pre_handler);
		
		if(title!=null){
			text = title;
		}
		if(iconName!=null){
			icon = Misc.getIcon(iconName);
		}

		setText(text);
		setGraphic(icon);
	}
	
	public static class Action extends Task<Integer>{		
		private EventHandler<ActionEvent> hand = null;		
		@Override
		protected Integer call() throws Exception {
			hand.handle(new ActionEvent(this,this));
			return null;
		}		
		public void update_msg(String msg){
			updateMessage(msg);
		}		
		public void update_val(int val){
			updateValue(val);
		}		
		public void update_txt(String title){
			updateTitle(title);
		}
	};
	
	private Action task = null;
	
	public boolean isDone(){
		if(task!=null){
			return task.isDone();
		}
		return true;
	}
	
	private EventHandler<ActionEvent> pre_handler = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			
			if(handTask==null){
				return;
			}
			if(reentry==false){
				//Don't let user-event re-entry, so disable this button
				setDisable(true);
			}else{
				//If user-event re-entry, try to cancel this task 
				if(task!=null){
					if(task.isDone()==false){
						task.cancel();
						return;
					}
				}				
			}
			setText("[ "+text+" ]");//indicate we have action~~
			
			task = new Action(){
				@Override
				protected Integer call() throws Exception {
					//updateMessage("start!!");
					handTask.handle(new ActionEvent(task,task));
					return 0;
				}
			};
			task.setOnSucceeded(eventDone);
			task.setOnFailed(eventDone);
			task.setOnCancelled(eventDone);
			new Thread(task,"Button-Task").start();
		}
	};
	
	private EventHandler<WorkerStateEvent> eventDone = new EventHandler<WorkerStateEvent>(){
		@Override
		public void handle(WorkerStateEvent event) {
			if(reentry==false){
				setDisable(false);
			}
			task = null;//reset it~~~
			setText(text);//restore the origin caption~~~
		}
	};
	
	private boolean reentry = false;
	
	//private EventHandler<ActionEvent> handPrep = null;
				
	//public ButtonTask setPrepare(EventHandler<ActionEvent> action){
	//	handPrep = action;
	//	return this;
	//}
	
	private EventHandler<ActionEvent> handTask = null;
	
	public ButtonTask setAction(EventHandler<ActionEvent> action){
		return setAction(reentry,action);
	}
	
	public ButtonTask setAction(boolean isReentry, EventHandler<ActionEvent> action){
		reentry = isReentry;
		handTask= action;
		return this;
	}	
}
