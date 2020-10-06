package prj.sputter;

import java.util.Optional;

import com.jfoenix.controls.JFXRadioButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.IntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import narl.itrc.DevModbus;
import narl.itrc.PadTouch;

public class LayFlower {
	
	/**
	 * PHOENIX CONTACT coupler:
	 * 
	 * ETH BK DIO8 DO4 2TX-PAC:
	 * 1.1 OUT1  2.1 OUT2
	 * 1.2 GND   2.2 GND
	 * 1.3 FE    2.3 FE
	 * 1.4 OUT3  2.4 OUT4
	 * 
	 * 1.1 IN1   2.1 IN2
	 * 1.2 Um    2.1 Um
	 * 1.3 GND   2.2 GND
	 * 1.4 IN3   2.3 IN4
	 * 
	 * 1.1 IN5   2.1 IN6
	 * 1.2 Um    2.1 Um
	 * 1.3 GND   2.2 GND
	 * 1.4 IN7   2.3 IN8
	 * -----------------
	 * IB IL AI 4-ECO
	 * 1.1 IN1   2.1 GND
	 * 1.2 IN2   2.1 GND
	 * 1.3 IN3   2.2 GND
	 * 1.4 IN4   2.3 GND 
	 * -----------------
	 * IB IL AO 4-ECO
	 * 1.1 OUT1  2.1 OUT2
	 * 1.2 GND   2.1 GND
	 * 1.3 OUT3  2.2 OUT4
	 * 1.4 GND   2.3 GND
	 * -----------------
	 * Um - 24V
	 * FE - Function Earth
	 */
	/**
	 * i8000       - digital input
	 * i8001~i8004 - analog  input
	 * i8005       - digital output
	 * i8006~i8009 - analog  output
	 */
	private IntegerProperty[] sig = new IntegerProperty[5];
	
	private final DevModbus dev;
	
	public LayFlower(final DevModbus dev) {
		this.dev = dev;
		sig[1] = dev.inputRegister(8001);
		sig[2] = dev.inputRegister(8002);
		sig[3] = dev.inputRegister(8003);
		sig[4] = dev.inputRegister(8004);
	}
	
	/**
	 * Brooks 5850E, card edge to D-sub 9-Pin
	 * Edge  9Pin
	 *   F --  1 -- -15V 
	 *   D --  2 -- Test point
	 *   B --  3 -- CMD ground
	 *   C --  4 -- supply ground
	 *   A --  5 -- CMD
	 *   4 --  6 -- +15V
	 *   3 --  7 -- SIG
	 *   2 --  8 -- SIG ground
	 *   1 --  9 -- chassis
	 */
	public Node genMassFlowCtrl(
		final String name,
		final int cid
	) {
		//for Brooks 5850E
		//0.003 lpm~30 lpm
		//1 sccm = 0.001 lpm
		final Label[] txt = {
			new Label(name),
			new Label("PV"), new Label(),
			new Label("SV"), new Label(),
		};
		for(Label obj:txt) {
			obj.getStyleClass().add("font-size5");
		}
		txt[2].setPrefWidth(73);
		txt[4].setPrefWidth(73);
		
		final ToggleGroup grp = new ToggleGroup();
		final JFXRadioButton[] rad = {
			new JFXRadioButton("Volt"),
			new JFXRadioButton("SCCM"),
			new JFXRadioButton("SLPM"),
		};
		rad[0].setSelected(true);
		rad[0].setToggleGroup(grp);
		rad[1].setToggleGroup(grp);
		rad[2].setToggleGroup(grp);
		
		rad[0].setOnAction(e->{
			txt[2].textProperty().unbind();
			txt[2].textProperty().bind(sig[cid].asString("%d"));
			get_5850E(cid,txt[4],grp);
		});
		rad[1].setOnAction(e->{
			txt[2].textProperty().unbind();
			txt[2].textProperty().bind(sig[cid].asString("%d"));
			get_5850E(cid,txt[4],grp);
		});
		rad[2].setOnAction(e->{
			txt[2].textProperty().unbind();
			txt[2].textProperty().bind(sig[cid].asString("%d"));
			get_5850E(cid,txt[4],grp);
		});
		
		txt[3].setOnMouseClicked(e->set_5850E(cid,txt[3],grp));
		txt[4].setOnMouseClicked(txt[3].getOnMouseClicked());
		get_5850E(cid,txt[4],grp);
		
		final GridPane root = new GridPane();
		root.getStyleClass().add("box-pad");
		root.add(txt[0], 0, 0, 3, 1);
		root.addColumn(0, txt[1], txt[3]);
		root.addColumn(1, txt[2], txt[4]);
		root.addColumn(2, rad);
		return root;
	}
	
	private void set_5850E(
		final int cid, 
		final Label txt,
		final ToggleGroup grp
	) {
		String unit = ((RadioButton)grp.getSelectedToggle()).getText();
		
		PadTouch pad = new PadTouch('n',unit);
		Optional<String> opt = pad.showAndWait();			
		if(opt.isPresent()==false) {
			return;
		}
		
		String val = opt.get();
		
		int addr = 8005 + cid;
		
		if(unit.equals("Volt")==true) {
			int _val = Integer.valueOf(val);
			if(_val<0 || 5<_val) {
				return;
			}
			dev.asyncWriteVal(addr,_val);
		}else if(unit.equals("SCCM")==true) {

		}else if(unit.equals("SLPM")==true) {
			
		}
		txt.setText(val);
	}
	private void get_5850E(
		final int cid, 
		final Label txt, 
		final ToggleGroup grp
	) {dev.asyncBreakIn(()->{
		final int val = dev.read(8005+cid, 'I');
		Application.invokeAndWait(()->{
			String unit = ((RadioButton)grp.getSelectedToggle()).getText();
			if(unit.equals("Volt")==true) {
				txt.setText(String.format("%d", val));
			}else if(unit.equals("SCCM")==true) {
				txt.setText(String.format("%d", val));
			}else if(unit.equals("SLPM")==true) {
				txt.setText(String.format("%d", val));
			}
		});
	});}
}
