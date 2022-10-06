package prj.sputter.cargo1;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
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
			op3, run_holding,
			op4, run_waiting(1000,null),
			op5, run_waiting(1000,null)
		);
	}
	
	final Runnable op1 = ()->{
		msg[1].setText("開啟檔板");
		adam1.asyncSetAllLevel(true, false, false);//open top and close other~~
		next_step();
	};
	final Runnable op2 = ()->{
		msg[1].setText("fire!!");
		next_step();		
		sar1.set_onoff(true);		
	};
	final Runnable op3 = ()->{
		msg[1].setText("apply");
		next_step();		
		sar1.apply_setpoint(
			Misc.txt2Float(txt_rf.getText()),
			Misc.txt2Float(txt_dc.getText())
		);		
	};
	protected final Runnable op4 = ()->{
		next_step();			
		if(chk_cont.isSelected()==false) {
			msg[1].setText("關檔板");	
			sar1.set_onoff(false);
		}else {
			msg[1].setText("");
		}
	};
	protected final Runnable op5 = ()->{
		next_step();			
		if(chk_cont.isSelected()==false) {
			adam1.asyncSetAllLevel(true, true, true);			
		}
	};
	
	@Override
	public Node getContent() {
		
		final Label inf1 = new Label();
		inf1.textProperty().bind(sar1.txt_forward.textProperty());
		
		final Label inf2 = new Label();
		inf2.textProperty().bind(sar1.txt_reflect.textProperty());
		
		return gen_grid_pane(
			action_name,"5:00",false,
			new HBox(new Label("RF輸出(W):"), txt_rf), new HBox(new Label("輸出(W):"), inf1),
			new HBox(new Label("DC偏壓(W):"), txt_dc), new HBox(new Label("反射(W):"), inf2)
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
