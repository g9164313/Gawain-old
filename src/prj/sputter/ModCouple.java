package prj.sputter;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import narl.itrc.DevModbus;
import narl.itrc.Misc;

public class ModCouple extends DevModbus {

	/**
	 * h8000       - digital input
	 * i8007       - digital output
	 * 
	 * h8001-8002  - analog 2 input channel
	 * i8008-8009  - analog 2 setting, B15:parameterization, Bit5-4: 11-standardized 
	 * 
	 * h8003-8004  - analog 2 input channel
	 * i8010-8011  - analog 2 setting
	 * 
	 * h8005-8006  - analog 2 setting/mirror 
	 * i8012-8013  - analog 2 output channel
	 * 
	 * IB IL (SF-)PAC format: 
	 *     1 --> 0.333mv
	 * 30000 --> 10v
	 * 32512 --> 10.837
	 * 
	 */
	
	public final IntegerProperty ain1, ain2, ain3, ain4;
	public final IntegerProperty aout1, aout2;
	
	public ModCouple(){
		
		mapAddress("h8000-8006");
		
		ain1 = mapInteger(8001);
		ain2 = mapInteger(8002);
		ain3 = mapInteger(8003);
		ain4 = mapInteger(8004);
		
		aout1 = mapInteger(8005);
		aout2 = mapInteger(8006);
	}

	@Override
	protected void ignite() {
		
		//writeVals(8008,	0x8030,0x8030,0x8030,0x8030);//input channel setting
		
		/*int v1 = readReg('I',8008);//ain.1
		Misc.logv("#8008=0x%04X",v1);
		int v2 = readReg('I',8009);//ain.2
		Misc.logv("#8009=0x%04X",v2);
		int v3 = readReg('I',8010);//ain.1
		Misc.logv("#8010=0x%04X",v3);
		int v4 = readReg('I',8011);//ain.2
		Misc.logv("#8011=0x%04X",v4);*/
		
		//writeVals(8012,3000);
		//int v1 = readReg('H',8005);		
		//Misc.logv("#8005=0x%04X",v1);
		//int v2 = readReg('I',8012);
		//Misc.logv("#8012=0x%04X",v2);
		
		super.ignite();//goto next stage~~~~
	}
	
	/**
	 * get input channel property, unit is Volt.<p>
	 * @param cid - channel identify
	 * @return float property, unit is Volt
	 */
	public FloatProperty getChannelIn(final int cid) {
		FloatProperty prop = new SimpleFloatProperty();
		IntegerProperty val;
		switch(cid){
		case 1: val = ain1; break;
		case 2: val = ain2; break;
		case 3: val = ain3; break;
		case 4: val = ain4; break;
		default:
			Misc.logw("invalid input channel:%d",cid);
			return prop;
		}
		prop.bind(val.divide(1000f));		
		return prop;
	}
	
	/**
	 * get current output channel setting value.unit is volt.<p>
	 * @param cid - channel identify
	 * @return float property, unit is Volt
	 */
	public FloatProperty getChannelOut(final int cid) {
		FloatProperty prop = new SimpleFloatProperty();
		IntegerProperty val;
		switch(cid){
		case 1: val = aout1; break;
		case 2: val = aout2; break;
		default:
			Misc.logw("invalid output channel:%d",cid);
			return prop;
		}
		prop.bind(val.divide(3000f));		
		return prop;
	}
	
	public void asyncAanlogOut(final int cid, final float mvolt){
		final int val = (int)(mvolt * 3000f); //IB IL format
		switch(cid){
		case 1: asyncBreakIn(()->writeVals(8012,val)); break;
		case 2: asyncBreakIn(()->writeVals(8013,val)); break;
		}
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
			asyncWriteSet(8007, bit);
		}else{
			asyncWriteCls(8007, bit);
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
 * 1.2 Um    2.2 Um
 * 1.3 GND   2.3 GND
 * 1.4 IN3   2.4 IN4
 * 
 * 1.1 IN5   2.1 IN6
 * 1.2 Um    2.2 Um
 * 1.3 GND   2.3 GND
 * 1.4 IN7   2.4 IN8
 * -----------------
 * IB IL AI 2/SF-PAC
 * 1.1 +U1   2.1 +U2
 * 1.2 +I1   2.2 +I2
 * 1.3 -1    2.3 -1(GND??)
 * 1.4 Sh    2.4 Sh(ield) 
 * -----------------
 * IB IL AO 2/SF-PAC
 * Connector-1     |Connector-2      |Connector-3     |Connector-4      
 * 1.1 +U1  2.1 +U1|1.1 +Ia1 2.1 +Ib1|1.1 +U2  2.1 +U2|1.1 +Ia2 2.1 +Ib2       
 * 1.2 B    2.2 B  |1.2 B    2.2 B   |1.2 B    2.2 B  |1.2 B    2.2 B(ridge)
 * 1.3 GND  2.3 GND|1.3 GND  2.3 GND |1.3 GND  2.3 GND|1.3 GND  2.3 GND
 * 1.4 Sh   2.4 Sh |1.4 Sh   2.4 Sh  |1.4 Sh   2.4 Sh |1.4 Sh   2.4 Sh(ield)
 * Ia2 --> 0mA~20mA
 * Ib2 --> 4mA~20mA
 * -----------------
 * Um - 24V
 * FE - Function Earth
 */

