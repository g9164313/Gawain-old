package prj.sputter;

import javafx.beans.property.IntegerProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class StepSetSPIK extends Stepper {
	
	public static final String action_name = "SPIK脈衝";
	
	DevSPIK2k dev;
	final Label[] msg = {new Label(), new Label() };
	
	final TextField t_on_p = new TextField();
	final TextField t_on_n = new TextField();
	final TextField t_of_p = new TextField();
	final TextField t_of_n = new TextField();
	
	public StepSetSPIK(final DevSPIK2k device) {
		dev = device;
		set(op1,run_waiting(3000,null),op2);
	}
	
	final Runnable op1 = ()->{
		msg[0].setText("APPLY~");
		msg[1].setText("PULSE!!");
		next_step();		
		set_register(4, dev.Ton_pos, t_on_p);
		set_register(5, dev.Tof_pos, t_of_p);
		set_register(6, dev.Ton_neg, t_on_n);		
		set_register(7, dev.Tof_neg, t_of_n);
	};
	private void set_register(
		final int addr,
		final IntegerProperty prop,
		final TextField box		
	) {
		final Integer val = Misc.txt2Int(box.getText());
		if(val==null) {
			return;
		}
		dev.asyncSetRegister(tkn->{
			prop.set(val.intValue());
		}, addr, val.intValue());
	}
	
	final Runnable op2 = ()->{
		msg[0].setText(action_name);
		msg[1].setText("");
		next_step();
	};
	
	@Override
	public Node getContent() {
		msg[0].setText(action_name);
		msg[0].setMinWidth(100.);		
		msg[1].setMinWidth(100.);
		
		t_on_p.setPrefWidth(87.);
		t_on_n.setPrefWidth(87.);
		t_of_p.setPrefWidth(87.);
		t_of_n.setPrefWidth(87.);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,msg);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.addRow(0, new Label("Ton +"), t_on_p, new Label("Toff+"), t_of_p);
		lay.addRow(1, new Label("Ton -"), t_on_n, new Label("Toff-"), t_of_n);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	
	@Override
	public String flatten() {
		return control2text(t_on_p, t_on_n, t_of_p, t_of_n);
	}
	@Override
	public void expand(String txt) {
		text2control(txt, t_on_p, t_on_n, t_of_p, t_of_n);
	}
}
