package prj.LPS_8S;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import narl.itrc.DevModbus;


public class ModCoupler extends DevModbus {

	/**
	 * i8000       - digital input
	 * i8001~i8004 - analog  input
	 * i8005       - digital output
	 * i8006~i8009 - analog  output
	 */
	public final IntegerProperty dint,dout;
	
	public final BooleanProperty flg = new SimpleBooleanProperty();
	
	public ModCoupler() {
		
		looperDelay = 0;
		
		mapAddress("i8000","i8005");//8005-->output
		
		dint= inputRegister(8000);
		dout= inputRegister(8005);
		
		//flg.bind(pin_in.isEqualTo(3));
		
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

