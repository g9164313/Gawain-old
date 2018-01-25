package narl.itrc;

import java.util.function.Predicate;

import com.jfoenix.controls.JFXButton;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;


/**
 * Extra feature Button, features include 'bounce' and 'toggle'.<p>
 * 'Bounce' means that event will be triggered periodically after pressing.<p>
 * 'Toggle' means that button will change caption and icon image.<p>
 * @author qq
 *
 */
public class ButtonExtra extends JFXButton {

	private String text1 = "";
	private String text2 = "";
	private ImageView icon1 = null;
	private ImageView icon2 = null;
	
	public ButtonExtra(){
		this("",null,"",null);
	}
	
	public ButtonExtra(
		final String caption
	){
		this(caption,null,caption,null);
	}
	
	public ButtonExtra(
		final String caption,
		final String symbol
	){
		this(caption,symbol,caption,symbol);
	}
	
	public ButtonExtra(
		final String caption1,
		final String symbol1,
		final String caption2,
		final String symbol2
	){
		getStyleClass().add("btn-raised-2");
		if(caption1!=null){
			text1 = caption1;
		}else{
			text1 = "";
		}
		if(caption2!=null){
			text2 = caption2;
		}else{
			text2 = text1;
		}
		if(symbol1!=null){
			icon1 = Misc.getIcon(symbol1);
		}else{
			icon1 = null;
		}
		if(symbol2!=null){
			icon2 = Misc.getIcon(symbol2);
		}else{
			icon2 = icon1;
		}
		setText(text1);
		setGraphic(icon1);
		//setOnAction(eventHook);
	}
	
	public ButtonExtra setStyleBy(String name){
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
	
	private boolean isToggle = false;
	
	public ButtonExtra setOnToggle(
		EventHandler<ActionEvent> value_on,
		EventHandler<ActionEvent> value_off
	){
		setOnAction(event->{
			if(isToggle==false){
				setText(text2);
				setGraphic(icon2);
				if(value_on!=null){
					value_on.handle(event);
				}
			}else{
				setText(text1);
				setGraphic(icon1);
				if(value_off!=null){
					value_off.handle(event);
				}
			}
			isToggle = !isToggle;
		});
		return this;
	}	

	
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
			setText(text2);
			setGraphic(icon2);
			task = new Task<Integer>(){
				@Override
				protected Integer call() throws Exception {
					final ActionEvent e0 = new ActionEvent(task,event.getTarget());
					looper.handle(e0);
					return 0;
				}
			};
			task.setOnSucceeded(e1->{
				setText(text1);
				setGraphic(icon1);
				if(finish!=null){
					finish.handle(event);
				}
			});
			task.setOnCancelled(e2->{
				setText(text1);
				setGraphic(icon1);
			});
			task.setOnFailed(e3->{
				setText(text1);
				setGraphic(icon1);
			});			
			new Thread(task,"ButtonExtra-task").start();
		});
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
