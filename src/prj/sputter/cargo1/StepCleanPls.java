package prj.sputter.cargo1;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import narl.itrc.Misc;

/**
 * plasma clean procedure~~~~
 * @author qq
 *
 */
public class StepCleanPls extends StepCommon {

	public static final String action_name = "清洗電漿";
	
	final TextField txt_rf = new TextField();
	final TextField txt_dc = new TextField();
	
	public StepCleanPls() {
		txt_rf.setPrefWidth(100.);
		txt_dc.setPrefWidth(100.);
		set(
			op1, run_waiting(1000,null),
			op2, run_waiting(1000,null), 
			op3, run_waiting(1000,null), 
			op4, run_holding, op5
		);
	}
	
	final Runnable op1 = ()->{
		msg[1].setText("開啟檔板");		
		next_step();		
		adam1.asyncSetAllLevel(true);
	};
	final Runnable op2 = ()->{
		msg[1].setText("apply~~");
		next_step();		
		sar1.apply_setpoint(
			Misc.txt2Float(txt_rf.getText()),
			Misc.txt2Float(txt_dc.getText())
		);		
	};
	final Runnable op3 = ()->{
		msg[1].setText("Remote!!");
		next_step();		
		sar1.set_onoff(true);		
	};
	final Runnable op4 = ()->{
		msg[1].setText("Fire!!");
		next_step();		
		sar1.set_RF_fire(true);		
	};
	protected final Runnable op5 = ()->{
		msg[1].setText("關閉檔板");
		next_step();
		adam1.asyncSetAllLevel(false);
	};
	
	
	@Override
	public Node getContent() {
		
		final Label inf1 = new Label();
		inf1.textProperty().bind(sar1.txt_forward.textProperty());
		
		final Label inf2 = new Label();
		inf2.textProperty().bind(sar1.txt_reflect.textProperty());
		
		chk_cont.setDisable(true);
		
		return gen_grid_pane(
			action_name,"5:00",false,
			new Label("RF輸出(W):"), new Label("DC偏壓(V):"), txt_rf, txt_dc,
			new Label("輸出(W):"), new Label("反射(W):"), inf1, inf2
		);
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return control2text(
			box_hold, 
			txt_rf, txt_dc
		);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,
			box_hold,
			txt_rf, txt_dc
		);
	}
}
