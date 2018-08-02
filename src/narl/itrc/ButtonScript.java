package narl.itrc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.jfoenix.controls.JFXButton;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;

/**
 * A convenient button to evaluate JavaScript file...
 * User must call eval() in action event by self.
 * @author qq
 *
 */
public class ButtonScript extends JFXButton {

	private ImageView[] icon = {
		Misc.getResIcon("play.png"),
		Misc.getResIcon("pause.png")
	};
	
	public ButtonScript(String title){
		this(title,"btn-raised-2",null);
	}
	
	public ButtonScript(String title,Object obj){
		this(title,"btn-raised-2",obj);
	}
	
	public ButtonScript(String title,String style,Object obj){		
		getStyleClass().add(style);
		setText(title);
		setMinWidth(110);
		setMaxWidth(Double.MAX_VALUE);
		setGraphic(icon[0]);
		thiz = obj;
	}
	
	/**
	 * let this object provide all functions
	 */
	public Object thiz = null;
	
	/**
	 * this object will keep the result~~~
	 */
	public Object result = null;
	
	private Object tsk_core(
		final File file,
		final ScriptEngine parser
	) throws FileNotFoundException {		
		try {
			BufferedReader stream = new BufferedReader(new FileReader(file));
			result = parser.eval(stream);
			return result;
		} catch (ScriptException e) {			
			Misc.loge(e.getMessage());
			//System.out.println(e.getMessage());
			//e.printStackTrace();
			return result;
		}
	}
	
	private Task<Object> task = null;
	
	private EventHandler<WorkerStateEvent> hook = null;//ugly, because this type has no generic array.....
	
	/*public void setOnCancelled(EventHandler<WorkerStateEvent> value){
		hook1 = value;
	}
	public void setOnFailed(EventHandler<WorkerStateEvent> value){
		hook2 = value;
	}
	public void setOnSucceeded(EventHandler<WorkerStateEvent> value){
		hook3 = value;
	}*/
	
	public boolean eval(final File fs){
		
		if(fs.exists()==false || fs.isFile()==false){
			return false;
		}
		
		if(task!=null){
			if(task.isDone()==false){				
				task.cancel();//cancel task???
				return false;
			}
		}
		
		setGraphic(icon[1]);
		
		task = new Task<Object>(){			
			@Override
			protected Object call() throws Exception {

				ScriptEngine parser = new ScriptEngineManager().getEngineByName("nashorn");
				
				result = null;//reset the previous result~~~
				
				parser.put("sys", ButtonScript.this);//provide the basic or convenience functions 
				if(thiz!=null){
					parser.put("dev",thiz);
				}
				return tsk_core(fs,parser);
			}
		};
		task.setOnCancelled(event->{
			setGraphic(icon[0]);	
			if(hook!=null){
				hook.handle(event);
			}
		});
		task.setOnFailed(event->{
			setGraphic(icon[0]);	
			if(hook!=null){
				hook.handle(event);
			}
		});
		task.setOnSucceeded(event->{
			setGraphic(icon[0]);
			if(hook!=null){
				hook.handle(event);
			}
		});
		
		new Thread(task,"nashorn-core").start();		
		return true;
	}
	
	public boolean eval(String name){
		return eval(new File(name));
	}

	public void setOnAction(
		EventHandler<ActionEvent> beg_event,
		EventHandler<WorkerStateEvent> end_event
	) { 
		onActionProperty().set(beg_event);
		hook = end_event;
	}
	//----------------------------------
	
	//Basic API
	public void delay(long millisec){
		Misc.delay(millisec);
	}
}
