package prj.sputter;

import com.sun.glass.ui.Application;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import narl.itrc.DevModbus;
import narl.itrc.Misc;

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

public class ModCouple extends DevModbus {

	/**
	 * h8000       - digital input
	 * i8005       - digital output
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
	public final IntegerProperty aout1, aout2, aout3, aout4;
	
	public ModCouple(){
		
		mapAddress("h8000-8010");
		
		ain1 = mapInteger(8001);
		ain2 = mapInteger(8002);
		ain3 = mapInteger(8003);
		ain4 = mapInteger(8004);
		
		aout1 = mapInteger(8006);
		aout2 = mapInteger(8007);
		aout3 = mapInteger(8008);
		aout4 = mapInteger(8009);
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
		
		init_flow_prop();
		
		super.ignite();//goto next stage~~~~
	}
	
	//-------------------------------//
	
	/**
	 * get input channel property, unit is Volt.<p>
	 * @param cid - channel identify
	 * @return float property, unit is Volt
	 */
	public FloatProperty getAnalogIn(final int cid) {
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
	public FloatProperty getAnalogOut(final int cid) {
		FloatProperty prop = new SimpleFloatProperty();
		IntegerProperty val;
		switch(cid){
		case 1: val = aout1; break;
		case 2: val = aout2; break;
		case 3: val = aout3; break;
		case 4: val = aout4; break;
		default:
			Misc.logw("invalid output channel:%d",cid);
			return prop;
		}
		prop.bind(val.divide(1000f));		
		return prop;
	}
	
	public void asyncAanlogOut(final int cid, final float mvolt){
		final int val = (int)(mvolt * 1000f);
		switch(cid){
		case 1: asyncBreakIn(()->writeVals(8006,val)); break;
		case 2: asyncBreakIn(()->writeVals(8007,val)); break;
		case 3: asyncBreakIn(()->writeVals(8008,val)); break;
		case 4: asyncBreakIn(()->writeVals(8009,val)); break;
		}
	}
	
	//-------------------------------//
	
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
		
	public final FloatProperty[] PV_Flow = {
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
	};//unit is sccm!!!
	
	public final FloatProperty PV_FlowAr = PV_Flow[0];	
	public final FloatProperty PV_FlowN2 = PV_Flow[1];
	public final FloatProperty PV_FlowO2 = PV_Flow[2];
	
	private final float ar_max_sccm = 100f;
	private final float n2_max_sccm =  30f;
	private final float o2_max_sccm =  10f;
	
	private void set_mass_flow(
		final int aout_addr,
		String value,
		final float max_sccm
	){
		try{
			value = value.trim();
			if(value.length()==0){
				return;
			}
			float volt = (Float.valueOf(value) * 5f) / max_sccm;
			//boundary!!
			if(volt>5f) { 
				volt = 5f;
			}else if(volt<=0f){
				volt = 0f;
			}
			int mvolt = (int)(volt * 1000f);//IB IL format
			writeVals(aout_addr,mvolt);
		}catch(NumberFormatException e){
			Misc.loge("Wrong Format: %s", value);
		}
	}
	
	/**
	 * set mass flow control, unit is SCCM
	 * @param val_ar
	 * @param val_n2
	 * @param val_o2
	 */
	public void asynSetMassFlow(
		final String val_ar,
		final String val_n2,
		final String val_o2
	){asyncBreakIn(()->{
		set_mass_flow(8006, val_ar, ar_max_sccm);
		set_mass_flow(8007, val_n2, n2_max_sccm);
		set_mass_flow(8008, val_o2, o2_max_sccm);
	});}
	
	private void init_flow_prop() {
		Application.invokeLater(()->{
			PV_Flow[0].bind(ain1.divide(1000f).multiply(ar_max_sccm/5f));
			PV_Flow[1].bind(ain2.divide(1000f).multiply(n2_max_sccm/5f));
			PV_Flow[2].bind(ain3.divide(1000f).multiply(o2_max_sccm/5f));			
		});
	}
	
	//-------------------------------//
	
	public void asyncSelectGunHub(
		final boolean bipolar,
		final boolean unipolar,
		final boolean gun1,
		final boolean gun2
	) {	asyncBreakIn(()->{
		if(bipolar==true) {
			Misc.logv("電極切換:bipolar");
			writeVals(8005, 1);
		}else if(unipolar==true) {
			Misc.logv("電極切換:unipolar, %s",(gun1==true)?("gun-1"):("gun-2"));
			int val = 2;
			if(gun1==true) {
				val = val | 4;
			}
			if(gun2==true) {
				val = val | 8;
			}
			writeVals(8005, val);
		}else {
			Misc.logv("電極切換:???");
		}
	});}
	
	public void asyncMotorPump(final int dir) {	asyncBreakIn(()->{
		int val = readReg('H',8010);
		val = val & 0xFC0;
		if(dir<0) {
			val = val | 0x01;			
		}else if(dir>0) {
			val = val | 0x02;
		}
		writeVals(8010, val);
	});}
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
 * Um - 24V
 * FE - Function Earth
 * -----------------
 * IB IL AO 4-ECO
 * 1.1 OUT1  2.1 OUT2
 * 1.2 GND   2.1 GND
 * 1.3 OUT3  2.2 OUT4
 * 1.4 GND   2.3 GND
 * -----------------
 * IB IL AI 4-ECO
 * 1.1 IN1   2.1 GND
 * 1.2 IN2   2.1 GND
 * 1.3 IN3   2.2 GND
 * 1.4 IN4   2.3 GND 
 * -----------------
 *  
 */

