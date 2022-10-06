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
		set(
			op1, 
			run_waiting(1000,null),
			op2,
			run_waiting(500,null),
			op3
		);
	}
	
	private Float[] val = {null, null, null};
	
	final Runnable op1 = ()->{
		msg[1].setText("前閥");
		
		val[0] = clip_float(box2float(mfc1_sv,null),null,PanMain.MFC1_MAX_SCCM);
		val[1] = clip_float(box2float(mfc2_sv,null),null,PanMain.MFC2_MAX_SCCM);
		val[2] = clip_float(box2float(mfc3_sv,null),null,PanMain.MFC3_MAX_SCCM);
		
		adam1.asyncSetAllLevel(
			null, null,	null,
			(val[0]!=null)?((val[0]>0.1f)?(true):(false)):(false), 
			(val[1]!=null)?((val[1]>0.1f)?(true):(false)):(false), 
			(val[2]!=null)?((val[2]>0.1f)?(true):(false)):(false)
		);
	};
	
	final Runnable op2 = ()->{
		msg[1].setText("Apply!");
		adam4.asyncDirectOuput(
			PanMain.sccm2volt(val[0],PanMain.MFC1_MAX_SCCM), 
			PanMain.sccm2volt(val[1],PanMain.MFC2_MAX_SCCM), 
			PanMain.sccm2volt(val[2],PanMain.MFC3_MAX_SCCM)
		);
	};
	
	final Runnable op3 = ()->{
		msg[1].setText("");
		Float[] dst = {
			PanMain.mfc1_pv.get(),
			PanMain.mfc2_pv.get(),
			PanMain.mfc3_pv.get(),
		};
		next_step();
		for(int i=0; i<val.length; i++) {
			if(val[i]==null) {
				continue;
			}
			final float dv = val[i]-dst[i];
			if(Math.abs(dv)>0.5 && dv>0.f) {
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
