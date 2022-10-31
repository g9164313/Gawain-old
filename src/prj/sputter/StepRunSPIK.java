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
	
	//final TextField ramp = new TextField("30");
	final TextField hold = new TextField("30");
	final CheckBox  cont = new CheckBox("連續");
	
	public StepRunSPIK(final DevSPIK2k device) {
		dev = device;
		set(
			op1, run_waiting(3000,null),
			op2_1, run_waiting(3000,null), op2_2,
			op3_1, run_waiting(3000,null), 
			op3_2, run_waiting(3000,null),
			op4_1, run_waiting(3000,null), op4_2,
			run_hold, op_end
		);
	}
	
	final Runnable op1 = ()->{
		msg[0].setText("APPLY~");
		msg[1].setText("V.I.P!!");
		next_step();
		
		dev.set_DC_value(null, '1', 'V', Misc.txt2Float(vol_1.getText()));
		dev.set_DC_value(null, '1', 'I', Misc.txt2Float(amp_1.getText()));
		dev.set_DC_value(null, '1', 'P', Misc.txt2Float(pow_1.getText()));
		
		dev.set_DC_value(null, '2', 'V', Misc.txt2Float(vol_2.getText()));
		dev.set_DC_value(null, '2', 'I', Misc.txt2Float(amp_2.getText()));
		dev.set_DC_value(null, '2', 'P', Misc.txt2Float(pow_2.getText()));
	};
	
	final Runnable op2_1 = ()->{
		msg[0].setText("-RUN-");
		msg[1].setText("");
		next_step();
		if(dev.Run.get()==false) {
			dev.toggleRun(true);
		}		
	};
	final Runnable op2_2 = ()->{
		if(dev.Run.get()==true) {
			msg[1].setText("On!!");
			next_step();
		}else {
			msg[1].setText("wait");
			hold_step();
		}
	};
	
	long tick_diff;
	int  power_set;
	final Runnable op3_1 = ()->{
		tick_diff = System.currentTimeMillis();
		power_set = dev.DC1_P_Set.get();
		msg[0].setText("-DC1-");
		msg[1].setText("");
		next_step();
		dev.toggleDC1(dc_1.isSelected());
	};
	final Runnable op3_2 = ()->{
		if(dev.DC1.get()==dc_1.isSelected()) {
			next_step();//TODO: DC1_P_Act no update!!!!
			/*final int power_cur = dev.DC1_P_Act.get();
			final int power_dff = Math.abs(power_set-power_cur);			
			Misc.logv("[StepRunSPIK] pow:%d-->%d", power_set, power_cur);
			if(power_dff<10) {
				msg[1].setText("check!!");
				tick_diff = System.currentTimeMillis() - tick_diff;
				Misc.logv("[StepRunSPIK] ramp-time:%s", Misc.tick2text(tick_diff,true));
				next_step();
			}else {
				msg[1].setText("W:"+power_dff);
				hold_step();
			}*/			
		}else {
			msg[1].setText("wait~~");
			hold_step();
		}
	};
	
	final Runnable op4_1 = ()->{
		msg[0].setText("DC2");
		msg[1].setText("");
		next_step();
		dev.toggleDC2(dc_2.isSelected());	
	};
	final Runnable op4_2 = ()->{
		if(dev.DC2.get()==dc_2.isSelected()) {
			msg[1].setText("check!!");
			next_step();
		}else {
			msg[1].setText("wait~~");
			hold_step();
		}		
	};
	
	final Runnable run_hold = ()->{
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
		if(cont.isSelected()==false) {
			msg[1].setText("OFF");
			if(dev.DC1.get()==true) {
				dev.toggleDC1(false);
			}
			if(dev.DC2.get()==true) {
				dev.toggleDC2(false);
			}
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
			hold
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
		lay.addRow(0, new Label("維持時間:"), hold, cont);
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
			hold,cont
		);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,
			dc_1,vol_1,amp_1,pow_1,
			dc_2,vol_2,amp_2,pow_2,
			hold,cont
		);
	}
}
