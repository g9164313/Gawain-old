package prj.shelter;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;

public class StepCalibrate extends StepMeasure {

	public StepCalibrate(
		final DevHustIO dev1, 
		final DevAT5350 dev2, 
		final DevCDR06 dev3
	) {
		super(dev1, dev2, dev3);
		addRun(op_shoot);
		title.setText("劑量校正");
		inform.setText("μSv/hr");
	}

	/**
	 * 衰退劑量公式：= radiation * 0.977
	 * unit is 'μSv/hr'
	 */
	public static float S_RADI = 0.977f;
	/**
	 * 衰退距離公式：=((distance+90)*0.988)-90
	 * unit is 'cm'
	 */
	public static float S_DIST1 = 90f;
	public static float S_DIST2 = 0.988f;
	public static float S_DIST3 = 90f;
	
	private final Label[] info = {
		new Label(),
		new Label(),
		new Label(),
		new Label()
	};
	
	private final Label txt_cur_val = info[0];
	private final Label txt_decay0= info[1];
	private final Label txt_decay = info[2];
	private final Label txt_decay1= info[3];
	
	//unit is 'μSv/hr'
	private final TextField txt_dose0 = new TextField();
	private final TextField txt_dose1 = new TextField();

	Runnable op_shoot = ()->{
		Misc.logv("op-shoot");
		step_jump(-6);
		//next_step();
	};
	
	private void decay_value(final TextField box,final Label txt) {
		String val = box.getText().trim();
		try {
			float _v = Float.valueOf(val);
			txt.setText(String.format("%.3f",_v*S_RADI));
		}catch(NumberFormatException e) {
			Misc.loge("wrong fmt-->%s", val);
		}
	}
	
	@Override
	public Node getContent() {
		
		txt_cur_val.textProperty().bind(txt_avg.textProperty());
		txt_dose0.setPrefWidth(53);
		txt_dose1.setPrefWidth(53);
		txt_dose0.setOnAction(e->decay_value(txt_dose0,txt_decay0));
		txt_dose1.setOnAction(e->decay_value(txt_dose1,txt_decay1));
		
		GridPane lay = (GridPane)super.getContent();		
		lay.addColumn(8, txt_dose0, txt_cur_val, txt_dose1);
		lay.add(new Label("-->"), 9, 1, 1, 1);
		lay.addColumn(10, txt_decay0, txt_decay, txt_decay1);
		return lay;
	}
}
