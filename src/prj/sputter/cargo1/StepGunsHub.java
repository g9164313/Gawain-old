package prj.sputter.cargo1;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

public class StepGunsHub extends StepCommon {

	public static final String action_name = "GunHub";
	
	final JFXRadioButton chk_gun1 = new JFXRadioButton("Gun-1");
	final JFXRadioButton chk_gun2 = new JFXRadioButton("Gun-2");
	final JFXRadioButton chk_gun3 = new JFXRadioButton("Gun-3");
	final JFXCheckBox chk_sht1 = new JFXCheckBox("擋板-1");
	final JFXCheckBox chk_sht2 = new JFXCheckBox("擋板-2");
	final JFXCheckBox chk_sht3 = new JFXCheckBox("擋板-3");
	
	public StepGunsHub() {
		ToggleGroup grp = new ToggleGroup();
		chk_gun1.setToggleGroup(grp);
		chk_gun2.setToggleGroup(grp);
		chk_gun3.setToggleGroup(grp);
		
		set(op1,
			run_waiting(500,null)
		);
	}
	
	final Runnable op1 = ()->{
		adam2.asyncSetRelayAll(
			null,
			chk_gun1.isSelected(),
			chk_gun2.isSelected(),
			chk_gun3.isSelected()
		);
		/*adam1.asyncSetLevelAll(
			chk_sht1.isSelected(),
			chk_sht2.isSelected(),
			chk_sht3.isSelected()
		);*/
		next_step();
	};

	@Override
	public Node getContent() {
		msg[0].setText(action_name);
		msg[0].setMinWidth(100.);
		msg[1].setMinWidth(100.);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,msg);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.addRow(0, chk_gun1, chk_gun2, chk_gun3);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return control2text(chk_gun1,chk_gun2,chk_gun3);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,chk_gun1,chk_gun2,chk_gun3);
	}
}
