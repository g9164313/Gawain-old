package narl.itrc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.function.Predicate;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.jfoenix.controls.JFXButton;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.ImageView;


/**
 * Extra feature Button, features include 'bounce' and 'toggle'.<p>
 * 'Bounce' means that event will be triggered periodically after pressing.<p>
 * 'Toggle' means that button will change caption and icon image.<p>
 * @author qq
 *
 */
public class ButtonEx extends JFXButton {

	private int indx = 0;
	private String[]    text = null;
	private ImageView[] icon = null;
	
	public ButtonEx(){
		
	}
		
	public ButtonEx(String... args){
		
		getStyleClass().add("button");		
		setContentDisplay(ContentDisplay.TOP);
		
		int cnt = args.length/2;
		if(cnt==0){
			return;
		}
		text = new String[cnt];
		icon = new ImageView[cnt];
		for(int i=0; i<cnt; i++){
			text[i] = args[i*2+0];
			icon[i] = Misc.getIconView(args[i*2+1]);
		}
		setFace(0);
	}
	
	public ButtonEx setStyleBy(String name){
		final Predicate<String> filter = new Predicate<String>(){
			@Override
			public boolean test(String t) {
				return t.startsWith("btn-raised");
			}
		};
		getStyleClass().removeIf(filter);
		getStyleClass().add(name);
		return this;
	}

	public void setFace(int i){
		if(i>=text.length){
			i = 0;
		}
		setText(text[i]);
		setGraphic(icon[i]);
		indx = i;
	}
	public void nextFace(){
		setFace(++indx);
	}
	
	/**
	 * just a warp for 'setOnAction' function
	 * @param event - same as function 'setOnAction'
	 * @return
	 */
	public ButtonEx setOnClick(EventHandler<ActionEvent> event){
		setOnAction(event);
		return this;
	}
	
	/**
	 * The default is always jumping to next face.<p>
	 * It means always changing face between the first and second.<p>
	 */
	public boolean nextToggle = true;
	
	public ButtonEx setOnToggle(
		EventHandler<ActionEvent> event1,
		EventHandler<ActionEvent> event2
	){
		setOnAction(e->{
			switch(indx){
			case 0: 
				if(event1!=null){
					event1.handle(e);
					if(nextToggle==true && event2!=null){
						setFace(1);
					}
				}
				break;
			case 1:
				if(event2!=null){ 
					event2.handle(e);
					if(nextToggle==true && event1!=null){
						setFace(0);
					}					
				}
				break;
			}			
		});
		return this;
	}

	
	public Object arg = null;
	
	private Task<Integer> task = null;
	
	public void setOnTask(
		EventHandler<ActionEvent> looper,
		EventHandler<ActionEvent> finish
	){
		setOnAction(event->{
			
			if(task!=null){
				if(task.isDone()==false){				
					return;
				}
			}
			//setText(text2);
			//setGraphic(icon2);
			
			final ActionEvent eventRef = new ActionEvent(this,event.getTarget());
			
			task = new Task<Integer>(){
				@Override
				protected Integer call() throws Exception {					
					looper.handle(eventRef);
					return 0;
				}
			};
			task.setOnSucceeded(e1->{
				//setText(text1);
				//setGraphic(icon1);
				if(finish!=null){
					finish.handle(eventRef);
				}
			});
			task.setOnCancelled(e2->{
				///setText(text1);
				//setGraphic(icon1);
			});
			task.setOnFailed(e3->{
				//setText(text1);
				//setGraphic(icon1);
			});			
			new Thread(task,"ButtonExtra-task").start();
		});
	}
	
	
	public void setOnTaskScript(
		final String text,
		final String bindingKey,
		final Object bindingVal
	){
		setOnTask(eventLooper->{			
			try {
				ScriptEngine core = new ScriptEngineManager().getEngineByName("nashorn");
				core.put(bindingKey, bindingVal);
				core.eval(text);
			} catch (Exception e) {
				e.printStackTrace();
			}			
		},null);
	}
	
	public void setOnTaskScriptFile(
		final String fileName,
		final String bindingKey,
		final Object bindingVal
	){		
		setOnTask(eventLooper->{
			try {
				BufferedReader stm = new BufferedReader(new FileReader(fileName));
				ScriptEngine core = new ScriptEngineManager().getEngineByName("nashorn");
				core.put(bindingKey, bindingVal);
				core.eval(stm);
			} catch (Exception e) {
				e.printStackTrace();
			}			
		},null);		
	}
	
	/*private EventHandler<ActionEvent> eventStart = null;
	
	private EventHandler<ActionEvent> eventFinish= null;
	
	public ButtonExtra setOnActionStart(EventHandler<ActionEvent> val){
		eventStart = val;
		return this;
	}
	
	public ButtonExtra setOnActionFinish(EventHandler<ActionEvent> val){
		eventFinish = val;
		return this;
	}*/
}
