package narl.itrc;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;

import com.jfoenix.controls.JFXButton;

public class BtnToggle extends JFXButton implements EventHandler<ActionEvent> {

	private String[] name={"",""};
	private ImageView[] icon={null,null};

	public BtnToggle(String... txt){
		switch(txt.length){
		case 1:
			name[0] = name[1] = txt[0];
			break;
		case 2:
			name[0] = name[1] = txt[0];
			icon[0] = icon[1] = Misc.getIcon(txt[1]);
			break;
		case 4:
			name[0] = txt[0];
			name[1] = txt[2];
			icon[0] = Misc.getIcon(txt[1]);
			icon[1] = Misc.getIcon(txt[3]);
			break;
		default:
			name[0]="off";
			name[1]="on";
			break;
		}
		set(null);
		setOnAction(this);
	}
	
	protected void eventStart(){		
	}
	protected void eventFinal(){		
	}
	
	private AtomicBoolean state = new AtomicBoolean(false);
	
	protected void change(boolean flag){
		state.set(flag);//if we don't switch to next state, just set this variable 
	}
	
	private void set(Boolean flag){
		if(flag==null){
			//default~~~~
			setText(name[0]);
			if(icon[0]!=null){
				setGraphic(icon[0]);
			}
			return;
		}
		if(flag==false){
			setText(name[0]);
			if(icon[0]!=null){ setGraphic(icon[0]); }
			eventFinal();
		}else{
			setText(name[1]);
			if(icon[1]!=null){ setGraphic(icon[1]); }
			eventStart();
		}
	}
	
	public boolean get(){
		return state.get();
	}

	@Override
	public void handle(ActionEvent event) {
		boolean ss = !state.get();
		set(ss);
		state.set(ss);
	}
}
