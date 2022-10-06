package prj.sputter;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class StepRunSPIK extends Stepper {
	
	public static final String action_name = "SPIK點火";
	
	DevSPIK2k dev;
	final Label[] msg = {new Label(), new Label() };
	
	final CheckBox  dc_1 = new CheckBox("DC1");
	final TextField vol_1 = new TextField();
	final TextField amp_1 = new TextField();
	final TextField pow_1 = new TextField();
	
	final CheckBox  dc_2 = new CheckBox("DC2");
	final TextField vol_2 = new TextField();
	final TextField amp_2 = new TextField();
	final TextField pow_2 = new TextField();
	
	final TextField ramp = new TextField("30");
	final TextField hold = new TextField("30");
	final CheckBox  cont = new CheckBox("保留");
	
	public StepRunSPIK(final DevSPIK2k device) {
		dev = device;
		set(
			op1, run_waiting(1500,null),
			op2, op2_1,
			op3, op3_1,
			op4, run_holding,
			op_end
		);
	}
	
	final Runnable op1 = ()->{
		msg[0].setText("APPLY~");
		msg[1].setText("V.I.P!!");
		next_step();
		
		dev.set_DC_value(
			null, '1', 'V', 
			Misc.txt2Float(vol_1.getText())
		);
		dev.set_DC_value(null, '1', 'I', Misc.txt2Float(amp_1.getText()));
		dev.set_DC_value(null, '1', 'P', Misc.txt2Float(pow_1.getText()));
		
		dev.set_DC_value(null, '2', 'V', Misc.txt2Float(vol_2.getText()));
		dev.set_DC_value(null, '2', 'I', Misc.txt2Float(amp_2.getText()));
		dev.set_DC_value(null, '2', 'P', Misc.txt2Float(pow_2.getText()));
	};
	
	final Runnable op2 = ()->{
		msg[0].setText("DC1");
		msg[1].setText("check~");
		next_step();
		if(dc_1.isSelected()==true) {
			dev.toggleDC1(true);
		}		
	};
	final Runnable op2_1 = ()->{
		next_step();
		if(dc_1.isSelected()==true && dev.DC1.get()==false) {
			msg[1].setText("wait~");
			hold_step();
		}		
	};
	
	final Runnable op3 = ()->{
		msg[0].setText("DC2");
		msg[1].setText("check~");
		next_step();
		if(dc_2.isSelected()==true) {
			dev.toggleDC2(true);
		}		
	};
	final Runnable op3_1 = ()->{
		next_step();
		if(dc_2.isSelected()==true && dev.DC2.get()==false) {
			msg[1].setText("wait~");
			hold_step();
		}		
	};
	
	final Runnable op4 = ()->{
		msg[0].setText("RUN!!");
		msg[1].setText("");
		next_step();
		dev.toggleRun(true);		
	};
	final Runnable run_holding = ()->{
		msg[0].setText("HOLD~~");
		final long rem = waiting_time(hold.getText());		
		if(rem>0) {
			msg[1].setText("倒數"+Misc.tick2text(rem, true));
		}else {
			msg[1].setText("");
		}
	};
	
	final Runnable op_end = ()->{
		msg[0].setText(action_name);
		if(cont.isSelected()==true) {
			msg[1].setText("Run");
		}else {
			msg[1].setText("OFF");
			dev.toggleRun(false);
		}		
		next_step();
	};
	
	@Override
	public Node getContent() {
		msg[0].setText(action_name);
		msg[0].setMinWidth(100.);		
		msg[1].setMinWidth(100.);
		
		cont.setSelected(true);
		
		for(Control obj:new Control[] {
			dc_1,vol_1,amp_1,pow_1,
			dc_2,vol_2,amp_2,pow_2,
			ramp,hold
		}) {
			obj.setPrefWidth(87.);
		}
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,msg);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.addRow(0, 
			dc_1, new Label("V:"), vol_1, new Label("I:"), amp_1, new Label("P:"), pow_1
		);
		lay.addRow(1, 
			dc_2, new Label("V:"), vol_2, new Label("I:"), amp_2, new Label("P:"), pow_2
		);
		lay.add(new Separator(Orientation.VERTICAL), 9, 0, 1, 2);
		lay.addColumn(10, new Label("爬升時間:"), new Label("維持時間:"));
		lay.addColumn(11, ramp, hold);
		lay.add(cont, 12, 1);
		return lay;
	}
	@Override
	public void eventEdit() {
	}

	@Override
	public String flatten() {
		return control2text(
			dc_1,vol_1,amp_1,pow_1,
			dc_2,vol_2,amp_2,pow_2,
			ramp,hold,cont
		);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,
			dc_1,vol_1,amp_1,pow_1,
			dc_2,vol_2,amp_2,pow_2,
			ramp,hold,cont
		);
	}
}
