package narl.itrc;

import java.util.concurrent.atomic.AtomicBoolean;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.ImageView;

import com.jfoenix.controls.JFXButton;

public class BtnToggle extends JFXButton implements EventHandler<ActionEvent> {

	private String[] name={"",""};
	private ImageView[] icon={null,null};

	/**
	 * create a toggle button with two title name and icon picture<p>
	 * When user trigger action, button will change title(and icon) to the second one  
	 * @param txt - tile and icon name, etc...
	 */
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
		refreshFace();
		eventInit(state.get());
		setOnAction(this);
	}
	
	private void refreshFace(){
		int i=(state.get()==false)?(0):(1);
		setText(name[i]);
		if(icon[i]!=null){
			setGraphic(icon[i]);
		}
	}
	
	private AtomicBoolean state = new AtomicBoolean(false);

	public void setState(boolean flag){
		if(flag==state.get()){
			return;
		}
		fire();
	}
	
	public boolean getState(){
		return state.get();
	}

	protected void eventInit(boolean state){ }
	
	protected void eventSwitch(boolean state){ }
	
	protected void eventSelect(){ }
	
	protected void eventDeselect(){ }
	
	@Override
	public void handle(ActionEvent event) {
		boolean ss = state.get();
		eventSwitch(ss);
		ss = !ss;
		if(ss==true){
			eventSelect();
		}else{
			eventDeselect();
		}		
		state.set(ss);
		refreshFace();
	}
}
