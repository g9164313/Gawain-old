package prj.sputter.cargo1;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class StepMassFlow extends StepCommon {
	
	public static final String action_name = "微量氣體";
	
	final TextField mfc1_sv = new TextField();
	final TextField mfc2_sv = new TextField();
	final TextField mfc3_sv = new TextField();
	
	public StepMassFlow() {
		mfc1_sv.setPrefWidth(100.);
		mfc2_sv.setPrefWidth(100.);
		mfc3_sv.setPrefWidth(100.);
	}
	
	@Override
	public Node getContent() {
		final Label[] pv = {
			new Label(), new Label(), new Label(),	
		};
		for(Label obj:pv) {
			obj.setPrefWidth(100.);
		}
		pv[0].textProperty().bind(PanMain.mfc1_pv.asString("%.1f"));
		pv[1].textProperty().bind(PanMain.mfc2_pv.asString("%.1f"));
		pv[2].textProperty().bind(PanMain.mfc3_pv.asString("%.1f"));
		
		return gen_grid_pane(
			action_name, null, false,
			new Label("PV"), new Label("SV"),
			pv[0], mfc1_sv,
			pv[1], mfc2_sv,
			pv[2], mfc3_sv
		);
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
