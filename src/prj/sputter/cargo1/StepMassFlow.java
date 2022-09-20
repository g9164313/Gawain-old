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
		set(op1,
			run_waiting(1000,null),
			op2
		);
	}
	
	private static final Float[] MAX_SCCM = {
		PanMain.MFC1_MAX_SCCM,
		PanMain.MFC2_MAX_SCCM,
		PanMain.MFC3_MAX_SCCM,	
	};
	
	final Runnable op1 = ()->{
		msg[1].setText("apply");
		Float[] vals = {
			box2float(mfc1_sv,null),
			box2float(mfc2_sv,null),
			box2float(mfc3_sv,null),
		};
		for(int i=0; i<vals.length; i++) {
			if(vals[i]==null) {
				continue;
			}
			vals[i] = (vals[i] * MAX_SCCM[i]) / 5f;
		}
		adam4.asyncDirectOuput(vals);
	};
	
	final Runnable op2 = ()->{
		msg[1].setText("waiting");
		Float[] src = {
			box2float(mfc1_sv,null),
			box2float(mfc2_sv,null),
			box2float(mfc3_sv,null),
		};
		Float[] dst = {
			PanMain.mfc1_pv.get(),
			PanMain.mfc2_pv.get(),
			PanMain.mfc3_pv.get(),
		};
		next_step();
		for(int i=0; i<src.length; i++) {
			if(src[i]==null) {
				continue;
			}
			if(Math.abs(src[i]-dst[i])>1f) {
				//no stable, just waiting~~~~
				msg[1].setText(String.format("wait MFC-%d",i+1));
				hold_step(); 
				return;
			}
		}
	};
	
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
		return control2text(
			mfc1_sv, mfc2_sv, mfc3_sv
		);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,
			mfc1_sv, mfc2_sv, mfc3_sv
		);
	}
}
