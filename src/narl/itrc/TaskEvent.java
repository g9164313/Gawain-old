package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Control;

public class TaskEvent extends ActionEvent {

	private static final long serialVersionUID = -2905826094112990850L;
	
	public Control node = null;
	
	public EventHandler<ActionEvent> prologue, epilogue;

	public TaskEvent(){
		this(null,null,null);
	}

	public TaskEvent(
		Control nod,
		EventHandler<ActionEvent> pro,
		EventHandler<ActionEvent> epi
	){
		source = this;
		node = nod;
		setPrologue(pro);
		setEpilogue(epi);
	}
	
	
	public TaskEvent set(EventHandler<ActionEvent> event){	
		return setPrologue(event);
	}
	
	public TaskEvent setPrologue(EventHandler<ActionEvent> event){
		prologue = event;		
		return this;
	}
	
	public TaskEvent setEpilogue(EventHandler<ActionEvent> event){
		epilogue = event;		
		return this;
	}

	public void fireEvent(){
		if(prologue!=null){
			prologue.handle(this);
		}
		if(epilogue!=null){
			Application.invokeAndWait(eventHook2);
		}
	}
	
	private Runnable eventHook2 = new Runnable(){
		@Override
		public void run() {			
			epilogue.handle(TaskEvent.this);
			if(node!=null){
				node.setDisable(false);
			}
		}
	};
}
