package narl.itrc;

import java.util.LinkedList;

import com.jfoenix.controls.JFXTextField;
import com.sun.glass.ui.Application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;


/**
 * Let user only typing integer number...
 * @author qq
 *
 */
public class JBoxValInteger extends JFXTextField {

	public IntegerProperty val = new SimpleIntegerProperty();//current value
	
	private LinkedList<Integer> lst = new LinkedList<Integer>();//history
	
	private void prv_value(){
		if(lst.isEmpty()==true){ 
			return;
		}
		int _v = lst.removeLast();
		lst.addFirst(_v);
		setText(""+_v);
	}
	
	private void nxt_value(){
		if(lst.isEmpty()==true){ 
			return;
		}
		int _v = lst.removeFirst();
		lst.addLast(_v);
		setText(""+_v);
	}
	
	private void set_value(boolean append){
		int _v = 0;
		try{
			_v = Integer.valueOf(getText());
			val.set(_v);			
			if(append==true){
				lst.addLast(_v);
			}
			if(eventHook!=null){
				eventHook.handle(hook);
			}
		}catch(NumberFormatException e){
		}		
	}

	public JBoxValInteger(){
		this(0);
	}
	
	public JBoxValInteger(int value){
		
		setPrefWidth(60);
		
		val.set(value);
		
		setOnKeyPressed(event->{
			KeyCode code = event.getCode();
			if(
				code==KeyCode.INSERT ||
				code==KeyCode.DELETE || code==KeyCode.BACK_SPACE ||
				code==KeyCode.LEFT || code==KeyCode.RIGHT ||
				code==KeyCode.HOME || code==KeyCode.END
			){
				return;//we need these key~~~
			}else if(code==KeyCode.UP || code==KeyCode.PAGE_UP){
				prv_value();
			}else if(code==KeyCode.DOWN || code==KeyCode.PAGE_DOWN){
				nxt_value();
			}else if(code==KeyCode.ENTER){
				set_value(true);
			}
			event.consume();
		});
		
		textProperty().addListener(event->{
			set_value(false);
		});
		
		setText(""+value);
	}
	
	public int get(){
		return val.get();
	}
	
	public void set(int value){
		if(Application.isEventThread()==true){
			set_value(true);
		}else{
			Misc.invoke(tsk->set_value(true));
		}
	}
	
	private ActionEvent hook = new ActionEvent(this,null);
	
	private EventHandler<ActionEvent> eventHook = null;
	
	public JBoxValInteger setOnHook(EventHandler<ActionEvent> event){
		eventHook = event;
		return this;
	}
}
