package prj.sputter;

import java.util.Optional;

import com.jfoenix.controls.JFXRadioButton;

import javafx.beans.property.FloatProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import narl.itrc.DevModbus;
import narl.itrc.PadTouch;

public class LayFlower {
	
	private final ModCouple dev;
	
	private static final String UNIT_VOLT = "Volt";
	private static final String UNIT_SCCM = "SCCM";
	//private static final String UNIT_SLPM = "SLPM";//1SLPM=1000SCCM
	
	private static final String FMT_FLOAT = "%5.3f";
	
	public LayFlower(final DevModbus dev) {
		this.dev = (ModCouple)dev;
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
	public Node genPanel_5850E(
		final String name,
		final int cid,
		final int max_sccm
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
		txt[2].setPrefWidth(100);
		txt[4].setPrefWidth(100);
		
		final ToggleGroup grp = new ToggleGroup();
		final JFXRadioButton[] rad = {
			new JFXRadioButton(UNIT_VOLT),
			new JFXRadioButton(UNIT_SCCM)
		};		
		rad[0].setToggleGroup(grp);
		rad[1].setToggleGroup(grp);	
		rad[0].setOnAction(e->bind_5850E(cid,grp,max_sccm,txt[2],txt[4]));
		rad[1].setOnAction(e->bind_5850E(cid,grp,max_sccm,txt[2],txt[4]));

		//default PV(Practice Value)
		grp.selectToggle(rad[1]);
		((JFXRadioButton)grp.getSelectedToggle()).getOnAction().handle(null);
		
		//label for SV(Setting Value)
		txt[3].setOnMouseClicked(e->set_5850E(cid,grp,max_sccm));
		txt[4].setOnMouseClicked(txt[3].getOnMouseClicked());
		
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
		final ToggleGroup grp,
		final float max_sccm
	) {
		String unit = ((RadioButton)grp.getSelectedToggle()).getText();
		
		PadTouch pad = new PadTouch('f',unit);
		Optional<String> opt = pad.showAndWait();			
		if(opt.isPresent()==false) {
			return;
		}
		float val = Float.valueOf(opt.get());
		if(unit.equals(UNIT_VOLT)==true) {
			if(val>5.f){ 
				return; 
			}
		}else if(unit.equals(UNIT_SCCM)==true) {
			if(val>max_sccm){ 
				return; 
			}
			val = (val * 5f) / max_sccm;
		}
		dev.asyncAanlogOut(cid,val);
	}
	private void bind_5850E(
		final int cid,
		final ToggleGroup grp,
		final float max_sccm,
		final Label txt_pv,
		final Label txt_sv
	) {
		String unit = ((RadioButton)grp.getSelectedToggle()).getText();	
		
		FloatProperty pv_v = dev.getChannelIn(cid);
		FloatProperty sv_v = dev.getChannelOut(cid);
		txt_pv.textProperty().unbind();
		txt_sv.textProperty().unbind();
		float fac = 1.f;
		if(unit.equals(UNIT_VOLT)==true) {
			fac = 1f;
		}else if(unit.equals(UNIT_SCCM)==true) {
			fac = max_sccm / 5f;
		}
		txt_pv.textProperty().bind(pv_v.multiply(fac).asString(FMT_FLOAT));
		txt_sv.textProperty().bind(sv_v.multiply(fac).asString(FMT_FLOAT));
	}
}

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


