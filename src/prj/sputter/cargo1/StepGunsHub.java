package prj.sputter.cargo1;

import com.jfoenix.controls.JFXRadioButton;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

public class StepGunsHub extends StepCommon {

	public static final String action_name = "電極/檔板";
	
	final JFXRadioButton chk_gun1 = new JFXRadioButton("Gun-1");
	final JFXRadioButton chk_gun2 = new JFXRadioButton("Gun-2");
	final JFXRadioButton chk_gun3 = new JFXRadioButton("Gun-3");
	
	final ComboBox<String> cmb_polar = new ComboBox<String>();
	
	public StepGunsHub() {
		
		ToggleGroup grp = new ToggleGroup();
		chk_gun1.setToggleGroup(grp);
		chk_gun2.setToggleGroup(grp);
		chk_gun3.setToggleGroup(grp);
				
		cmb_polar.getItems().addAll(
			"不改變",
			"Bipolar",
			"Unipolar -",
			"Unipolar +",
			"DC- Mode",
			"DC+ Mode"
		);
		cmb_polar.getSelectionModel().select(0);
		
		set(op1,run_waiting(1000,null),op2);
	}
	
	final Runnable op1 = ()->{
		msg[0].setText("GUN-x");
		msg[1].setText("Polar");
		adam2.asyncSetAllRelay(
			null,
			chk_gun1.isSelected(),
			chk_gun2.isSelected(),
			chk_gun3.isSelected()
		);
		spik.setMode(
			cmb_polar.getSelectionModel().getSelectedIndex()
		);
		next_step();
	};
	final Runnable op2 = ()->{
		msg[0].setText(action_name);
		msg[1].setText("done");
		next_step();
	};
	
	@Override
	public Node getContent() {
		msg[0].setText(action_name);
		msg[0].setMinWidth(100.);
		msg[1].setMinWidth(100.);
		cmb_polar.setMaxWidth(Double.MAX_VALUE);
			
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,msg);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.addRow(0, chk_gun1, chk_gun2, chk_gun3);
		lay.add(new Label("電極:"), 2, 1);
		lay.add(cmb_polar, 3, 1, 2, 1);
		//lay.addRow(1, chk_sht1, chk_sht2, chk_sht3);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return control2text(chk_gun1,chk_gun2,chk_gun3,cmb_polar);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,chk_gun1,chk_gun2,chk_gun3,cmb_polar);
	}
}
