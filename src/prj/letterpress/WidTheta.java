package prj.letterpress;

import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import narl.itrc.DevMotion;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;

public class WidTheta extends PanDecorate {

	public WidTheta(){
		super("Theta Moitor");
	}
	
	@Override
	public Node layoutBody() {
		final GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium");
		
		Label txt_ccw = new Label("CCW");
		Label txt_cw = new Label("CW");

		Button btn_ccw = PanBase.genButton0("", "clock-ccw.png");
		Button btn_cw = PanBase.genButton0("", "clock-cw.png");
		
		final double speed = 20000.;
		btn_ccw.addEventFilter(MouseEvent.MOUSE_PRESSED,event->{
			Entry.stg0.joggingTo(true,DevMotion.PULSE_UNIT, null, null, null, speed);
		});
		btn_ccw.addEventFilter(MouseEvent.MOUSE_RELEASED,event->{
			Entry.stg0.joggingTo(false,DevMotion.PULSE_UNIT, null, null, null, speed);
		});
		
		btn_cw.addEventFilter(MouseEvent.MOUSE_PRESSED,event->{
			Entry.stg0.joggingTo(true,DevMotion.PULSE_UNIT, null, null, null, -speed);
		});
		btn_cw.addEventFilter(MouseEvent.MOUSE_RELEASED,event->{
			Entry.stg0.joggingTo(false,DevMotion.PULSE_UNIT, null, null, null, -speed);
		});
		
		GridPane.setHalignment(txt_ccw, HPos.CENTER);
		GridPane.setHalignment(txt_cw,HPos.CENTER);
		
		GridPane.setHgrow(btn_ccw, Priority.ALWAYS);
		GridPane.setHgrow(btn_cw, Priority.ALWAYS);
		
		lay.add(txt_ccw, 0, 0);
		lay.add(txt_cw , 2, 0);
		lay.add(btn_ccw, 0, 1);
		lay.add(btn_cw , 2, 1);
		
		return lay;
	}

}
