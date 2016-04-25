package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

public abstract class DlgTask extends Task<Void> implements 
	EventHandler<WindowEvent>	
{
	public DlgTask(){
		scene = new PanTask("::DlgTask::",this);
	}
	
	public DlgTask(String title){
		scene = new PanTask(title,this);
	}
	
	private Stage stage;
	private PanTask scene;
	
	private void init(Window win){		
		stage = new Stage(StageStyle.UNIFIED);		
		stage.initModality(Modality.WINDOW_MODAL); 
		stage.initOwner(win);
		stage.setOnShowing(this);
		stage.setOnShown(this);
		stage.setResizable(false);	
	}	

	public DlgTask show(Window win){
		init(win);
		stage.centerOnScreen();
		scene.appear(stage);
		return this;
	}
	
	public DlgTask popup(Window win){
		init(win);
		stage.centerOnScreen();
		scene.standby(stage);
		return this;
	}

	public DlgTask show(){
		return show(null);
	}
	
	public DlgTask popup(){
		return popup(null);
	}
	
	//private Thread core = null;
	@Override
	public void handle(WindowEvent event) {
		EventType<?> type = event.getEventType();
		if(type.equals(WindowEvent.WINDOW_SHOWING)==true){
			//reset data and logger~~~
			logger = "";
			scene.logUpdate("");
		}else if(type.equals(WindowEvent.WINDOW_SHOWN)==true){
			if(isReady(DlgTask.this)==true){
				//TODO: how to reset it!!!
				new Thread(this,stage.getTitle()).start();
			}
		}
	}
		
	protected String logger = "";
	public void updateMessage(String msg){
		scene.logEvent(msg);
	}
	//------------------------------//
	
	protected boolean propOneShoot = true;
	protected boolean isReady(DlgTask dlg){ return true; }//user can override this, GUI thread
	protected boolean prepare(DlgTask dlg){ return true; }//Working thread, user can override this
	public abstract boolean execute(DlgTask dlg);// Working thread, user must implements this	
	protected boolean isEnded(DlgTask dlg){ return true; }//user can override this, GUI thread
	
	@Override
	protected Void call() throws Exception {
		final Runnable event = new Runnable(){
			@Override
			public void run() {
				if(isEnded(DlgTask.this)==true){
					stage.close();
				}
			}
		};
		boolean ans = prepare(this);
		if(ans==false){
			return null;
		}
		if(propOneShoot==true){
			//just jump-in, then check result~~~
			ans = execute(this);
			if(ans==false){
				Application.invokeLater(event);
				return null;
			}
		}else{
			//continue to execute the function~~~
			do{
				ans = execute(this);
				if(isCancelled()==true){
					updateMessage("停止程序");
					Application.invokeLater(event);
					return null;
				}
			}while(ans==false);
		}
		Application.invokeLater(event);
		return null;
	}
}

