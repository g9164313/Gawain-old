package narl.itrc;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;

public class ButtonBounce extends Button {

	public ButtonBounce(){
		this("","");
	}
	
	public ButtonBounce(String title){
		this(title,"");
	}
	
	public ButtonBounce(String title,String iconName){		
		if(title.length()!=0){
			setText(title);
		}
		if(iconName.length()!=0){
			setGraphic(Misc.getIconView(iconName));
		}
	}

	public ButtonBounce setTitle(String title){
		setText(title);
		return this;
	}
	
	public ButtonBounce setIcon(String iconName){
		setGraphic(Misc.getIconView(iconName));
		return this;
	}
	
	public ButtonBounce setOnAction(
		EventHandler<ActionEvent> eventPress,
		EventHandler<ActionEvent> eventRelease
	){
		setOnMousePressed(e->{
			MouseButton btn = e.getButton();				
			if(btn==MouseButton.PRIMARY){
				eventPress.handle(null);
			}
		});
		setOnMouseReleased(e->{
			MouseButton btn = e.getButton();				
			if(btn==MouseButton.PRIMARY){
				eventRelease.handle(null);
			}
		});
		return this;
	}	
}
