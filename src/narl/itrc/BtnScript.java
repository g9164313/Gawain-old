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
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;

/**
 * A convenient button to evaluate JavaScript file...
 * User must call eval() in action event by self.
 * @author qq
 *
 */
public class BtnScript extends JFXButton {

	private ImageView[] icon = {
		Misc.getIcon("play.png"),
		Misc.getIcon("pause.png")
	};
	
	public BtnScript(String title){
		this(title,"btn-raised-2",null);
	}
	
	public BtnScript(String title,Object obj){
		this(title,"btn-raised-2",obj);
	}
	
	public BtnScript(String title,String style,Object obj){		
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
			System.out.println(e.getMessage());
			//e.printStackTrace();
			return result;
		}
	}
	
	private Task<Object> task = null;
	
	private EventHandler<WorkerStateEvent> hook1, hook2, hook3;//ugly, because this type has no generic array.....
	
	public void setOnCancelled(EventHandler<WorkerStateEvent> value){
		hook1 = value;
	}
	
	public void setOnFailed(EventHandler<WorkerStateEvent> value){
		hook2 = value;
	}
	
	public void setOnSucceeded(EventHandler<WorkerStateEvent> value){
		hook3 = value;
	}
	
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

				ScriptEngine par = new ScriptEngineManager().getEngineByName("nashorn");
				
				result = null;//reset the previous result~~~
				
				if(thiz!=null){
					par.put("thiz",thiz);
				}
				return tsk_core(fs,par);
			}
		};
		task.setOnCancelled(event->{
			setGraphic(icon[0]);	
			if(hook1!=null){
				hook1.handle(event);
			}
		});
		task.setOnFailed(event->{
			setGraphic(icon[0]);	
			if(hook2!=null){
				hook2.handle(event);
			}
		});
		task.setOnSucceeded(event->{
			setGraphic(icon[0]);			
			if(hook3!=null){
				hook3.handle(event);
			}
		});
		
		new Thread(task,"==Eval-Script==").start();		
		return true;
	}
	
	public boolean eval(String name){
		return eval(new File(name));
	}
}
