package prj.sputter;

import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import narl.itrc.DevModbus;

public class ModCouple extends DevModbus {

	/**
	 * h8000       - digital input
	 * h8001~h8004 - analog  input  (1000-->1V)
	 * i8005       - digital output
	 * i8006~i8009 - analog  output (1000-->1V)
	 */
	public ModCouple(){
		mapAddress("h8000-8004");
	}
	
	public void select_gun1(final ActionEvent e){
		select_gun(e,2);
	}
	public void select_gun2(final ActionEvent e){
		select_gun(e,3);
	}
	
	private void select_gun(
		final ActionEvent e,
		final int bit
	){
		final CheckBox chk = (CheckBox)e.getSource();
		if(chk.isSelected()==true){
			asyncWriteBit1(8005, bit);
		}else{
			asyncWriteBit0(8005, bit);
		}
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

