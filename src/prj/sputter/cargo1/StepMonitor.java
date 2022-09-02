package prj.sputter.cargo1;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import narl.itrc.Stepper;
import prj.sputter.DevSQM160;

public class StepMonitor extends Stepper {

	final DevSQM160 sqm1;
	
	public StepMonitor() {
		sqm1 = PanMain.sqm1;
	}
	
	public static final String action_name = "薄膜監控";
	
	final Label[] msg = {
		new Label(action_name), 
		new Label(),
	};
	
	
	
	@Override
	public Node getContent() {
		msg[0].setMaxWidth(80);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,msg);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		return null;
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return null;
	}
	@Override
	public void expand(String txt) {
	}
}
