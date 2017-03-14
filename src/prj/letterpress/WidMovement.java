package prj.letterpress;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import narl.itrc.DevMotion;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;

public class WidMovement extends PanDecorate {

	public WidMovement(){
		super("Satge Movement");
	}
	
	@Override
	public Node eventLayout() {
		final GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium");
		
		Label[] txt ={			
			new Label("Up"),
			new Label("Right"),			
			new Label("Down"),
			new Label("Left")
		};
		
		Button[] btn = {
			PanBase.genButton0("","dir-up.png"),			
			PanBase.genButton0("","dir-right.png"),
			PanBase.genButton0("","dir-down.png"),
			PanBase.genButton0("","dir-left.png"),			
			PanBase.genButton0("Auto\nScan","")
		};
		
		for(int i=0; i<btn.length; i++){
			GridPane.setHgrow(btn[i], Priority.ALWAYS);
		}

		final double speed = 2000.;
		btn[0].addEventFilter(MouseEvent.MOUSE_PRESSED,event->{
			Entry.stg0.joggingTo(true,DevMotion.PULSE_UNIT, null, -speed);
		});
		btn[0].addEventFilter(MouseEvent.MOUSE_RELEASED,event->{
			Entry.stg0.joggingTo(false,DevMotion.PULSE_UNIT, null, -speed);
		});
		btn[2].addEventFilter(MouseEvent.MOUSE_PRESSED,event->{
			Entry.stg0.joggingTo(true,DevMotion.PULSE_UNIT, null, speed);
		});
		btn[2].addEventFilter(MouseEvent.MOUSE_RELEASED,event->{
			Entry.stg0.joggingTo(false,DevMotion.PULSE_UNIT, null, speed);
		});
		
		btn[1].addEventFilter(MouseEvent.MOUSE_PRESSED,event->{
			Entry.stg0.joggingTo(true,DevMotion.PULSE_UNIT, -speed);
		});
		btn[1].addEventFilter(MouseEvent.MOUSE_RELEASED,event->{
			Entry.stg0.joggingTo(false,DevMotion.PULSE_UNIT, -speed);
		});
		btn[3].addEventFilter(MouseEvent.MOUSE_PRESSED,event->{
			Entry.stg0.joggingTo(true,DevMotion.PULSE_UNIT, speed);
		});
		btn[3].addEventFilter(MouseEvent.MOUSE_RELEASED,event->{
			Entry.stg0.joggingTo(false,DevMotion.PULSE_UNIT, speed);
		});
		
		GridPane.setHalignment(txt[0], HPos.CENTER);
		
		GridPane.setHalignment(txt[1], HPos.CENTER);
		GridPane.setValignment(txt[1], VPos.BOTTOM);
		
		GridPane.setHalignment(txt[2], HPos.CENTER);
		
		GridPane.setHalignment(txt[3], HPos.CENTER);
		GridPane.setValignment(txt[3], VPos.BOTTOM);
		
		lay.add(txt[0], 1, 0);
		lay.add(txt[1], 2, 1);
		lay.add(txt[2], 1, 4);
		lay.add(txt[3], 0, 1);
		
		lay.add(btn[0], 1, 1);
		lay.add(btn[1], 2, 2);
		lay.add(btn[2], 1, 3);
		lay.add(btn[3], 0, 2);
		lay.add(btn[4], 1, 2);
		
		return lay;
	}
}
