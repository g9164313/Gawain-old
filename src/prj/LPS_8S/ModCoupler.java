package prj.LPS_8S;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import narl.itrc.DevModbus;

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
 * 3.1 IN5   4.1 IN6
 * 3.2 Um    4.1 Um
 * 3.3 GND   4.2 GND
 * 3.4 IN7   4.3 IN8
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


public class ModCoupler extends DevModbus {

	private final IntegerProperty din;
	private final IntegerProperty[] ain= {null,null,null,null};
	
	private final int DINN_ADDR = 8000;
	private final int DOUT_ADDR1= 8007;
	private final int DOUT_ADDR2= 8008;
	
	private final int AIN1_VAL1 = 8001;
	private final int AIN1_VAL2 = 8002;
	private final int AIN1_PRG1 = 8009;
	private final int AIN1_PRG2 = 8010;
	
	private final int AIN2_VAL1 = 8003;
	private final int AIN2_VAL2 = 8004;
	private final int AIN2_PRG1 = 8011;
	private final int AIN2_PRG2 = 8012;
	
	//private final int AOUT_VAL1 = 8005;
	//private final int AOUT_VAL2 = 8006;
	private final int AOUT_ARM_DW = 8013;//下壓
	private final int AOUT_ARM_UP = 8014;//上推
	
	public final BooleanProperty pumperUnclog = new SimpleBooleanProperty(false);
	public final BooleanProperty majorUnlock  = new SimpleBooleanProperty(false);
	public final BooleanProperty majorLockPing= new SimpleBooleanProperty(false);

	public final IntegerProperty ARM_MVOLT_UP;
	public final IntegerProperty ARM_MVOLT_DW;
	public final IntegerProperty FD_Q20C_AOUT;
	
	public final FloatProperty ARM_PRESS_UP = new SimpleFloatProperty();//unit is MPa
	public final FloatProperty ARM_PRESS_DW = new SimpleFloatProperty();//unit is MPa

	public Runnable working_press = null;
	public Runnable working_release = null;
	public Runnable emerged_press = null;
	public Runnable emerged_release = null;
	
	public ModCoupler() {
		
		mapAddress("h8000~8006");
		
		din = mapInteger(DINN_ADDR);
		
		ARM_MVOLT_DW = ain[0] = mapInteger(AIN1_VAL1);
		ARM_MVOLT_UP = ain[1] = mapInteger(AIN1_VAL2);
		FD_Q20C_AOUT = ain[2] = mapInteger(AIN2_VAL1);
		ain[3] = mapInteger(AIN2_VAL2);
		
		ARM_PRESS_DW.bind(ARM_MVOLT_DW.divide(1000f).multiply(0.1992).subtract(0.1694f));
		ARM_PRESS_UP.bind(ARM_MVOLT_DW.divide(1000f).multiply(0.1726).subtract(0.1239f)); 
	}
	
	public ModInsider ibus = null;
	
	public JFXToggleButton tglAlarm;
	public JFXToggleButton tglSlurryHeat;
	public JFXToggleButton tglSlurryPump;
	
	@Override
	protected void ignite() {
		//before looping, insure setting~~~
		lockclub(false);
		
		//program AI2 SF/PAC
		//bit 5~4:
		// 00 --> IB IL format
		// 01 --> IB ST format
		// 10 --> IB RT format
		// 11 --> standardized representation
		//bit 3~0:
		// 0000 -->  0~10V
		// 0001 -->-10~10V 
		// 1000 -->  0~20mA
		// 1001 -->-20~20mA
		// 1010 -->  4~20mA
		writeVals(AIN1_PRG1,0x8030);
		writeVals(AIN1_PRG2,0x8030);
		writeVals(AIN2_PRG1,0x8038);
		writeVals(AIN2_PRG2,0x8038);
		
		//program AO2 SF/PAC
		//writeVals(AOUT_PRG1,0x8030);//init-code
		//writeVals(AOUT_PRG1,0x8059);//parameter, non-volatile, IB IL format, Reset		
		//final int aout_val1 = readReg('H',AOUT_VAL1);
		
		//writeVals(AOUT_PRG2,0x8030);//init-code
		//writeVals(AOUT_PRG2,0x8059);//parameter, non-volatile, IB IL format, Reset		
		//final int aout_val2 = readReg('H',AOUT_VAL2);
		
		//writeSet(DOUT_ADDR2,0);//卡榫退出
		
		//implReadI(DOUT_ADDR1,dv1);
		final int dinn = readReg('H',DINN_ADDR);
		final int dout1= readReg('I',DOUT_ADDR1);
		final int dout2= readReg('I',DOUT_ADDR2);
		final boolean[] dout = {
			(dout1&0x1)!=0,//抽水幫浦和止水汽缸
			(dout1&0x2)!=0,//加熱器
			(dout1&0x4)!=0,//警示燈
			(dout1&0x8)!=0,//~~~~
			(dout2&0x1)!=0,//擺動軸 CTRG 
			(dout2&0x2)!=0,//壓力軸 CTRG
			(dout2&0x4)!=0,//擺動和壓力軸 LOP
			(dout2&0x8)!=0,//擺動和壓力軸 ST1
		};
		
		//when system boost, arm is always up~~~~
		act_arm_up.run();
	
		//setting the first property value~~~
		Application.invokeAndWait(()->{
			din.setValue(dinn);
			refresh_flag(dinn);
			din.addListener((obv,oldVal,newVal)->{
				/**
				 * Digital Input node:
				 * bit 0-> 卡榫退出
				 * bit 1-> 上提壓扣
				 * bit 2-> 卡榫定點
				 * bit 3-> 止水磁簧
				 * bit 4-> 急停壓扣
				 * bit 5-> 加工壓扣
				 * bit 6-> 
				 * bit 7-> 
				 */
				int prv = oldVal.intValue();
				int cur = newVal.intValue();
				refresh_flag(cur);
				detect_edge(prv,cur,1,act_arm_up,act_arm_dw);
				detect_edge(prv,cur,4,emerged_press,emerged_release);
				detect_edge(prv,cur,5,working_press,working_release);
			});
			
			toggle(tglAlarm,dout[2]);
			toggle(tglSlurryHeat,dout[1]);
			toggle(tglSlurryPump,dout[0]);
		});		
		super.ignite();//goto next stage~~~~
	}
	private void toggle(final JFXToggleButton obj, final boolean flg) {
		if(obj==null) {
			return;
		}
		obj.setSelected(flg);
	}
	private void refresh_flag(final int value) {
		majorUnlock.set((value&(1<<0))!=0);
		majorLockPing.set((value&(1<<2))!=0);
		pumperUnclog .set((value&(1<<3))!=0);
	}
	
	
	private void detect_edge(
		final int oldVal,
		final int newVal,
		final int bit_mask,
		final Runnable act_press,
		final Runnable act_float
	) {
		int ov = (oldVal & (1<<bit_mask)) >> bit_mask;
		int nv = (newVal & (1<<bit_mask)) >> bit_mask;
		// a接點 或 b接點
		if(ov==0 && nv==1) {
			//Misc.logv("signal rise!!");
			if(act_press!=null) { act_press.run(); }
		}else if(ov==1 && nv==0) {
			//Misc.logv("signal fall!!");
			if(act_float!=null) { act_float.run(); }
		}
	}
	
	public final AtomicInteger ArmDownDelay = new AtomicInteger(500);
	
	final float VOLT_SCALE = 3000f;//IBIL format
	private Runnable act_arm_up = ()->{
		writeVals(AOUT_ARM_DW, (int)(0.0f*VOLT_SCALE));//exhaust~~~
		writeVals(AOUT_ARM_UP, (int)(0.9f*VOLT_SCALE));//up up~~~
	};
	private Runnable act_arm_dw = ()->{			
		writeVals(AOUT_ARM_UP, (int)(0.0f*VOLT_SCALE));//exhaust~~~
		writeVals(AOUT_ARM_DW, (int)(1.4f*VOLT_SCALE));//down, down~~~
		//blocking_delay(500);//angle-1
		//blocking_delay(700);//angle-2
		blocking_delay(ArmDownDelay.get());//angle-2
		//finally~~~
		writeVals(AOUT_ARM_DW, (int)(3.0f*VOLT_SCALE));
		writeVals(AOUT_ARM_UP, (int)(3.4f*VOLT_SCALE));
		blocking_delay(50);
	};	
	public void armForce(
		final float volt1,
		final float volt2
	) {
		final int v1 =  (int)(volt1*VOLT_SCALE);
		final int v2 =  (int)(volt2*VOLT_SCALE);
		asyncBreakIn(()->{
			writeVals(AOUT_ARM_DW, 0, 0);//exhaust~~~
			blocking_delay(150);//angle-2
			writeVals(AOUT_ARM_DW,v1,v2);//supply~~~
			blocking_delay(150);//angle-2
		});
	}
	
	public void pumpSlurry(final boolean flg) {asyncBreakIn(()->{
		if(flg) {
			writeSet(DOUT_ADDR1,0);// 開汞  and 開水
		}else {
			writeCls(DOUT_ADDR1,0);// 關汞 and 關水
		}
	});}
	public void pumpSlurry() {
		if(tglSlurryPump==null) { return; }
		pumpSlurry(tglSlurryPump.isSelected());
	}
	
	public void heatSlurry(final boolean flg) {asyncBreakIn(()->{
		if(flg) {
			writeSet(DOUT_ADDR1,1);
		}else {
			writeCls(DOUT_ADDR1,1);
		}
	});}
	public void heatSlurry() {
		if(tglSlurryHeat==null) { return; }
		heatSlurry(tglSlurryHeat.isSelected());
	}

	public void giveAlarm(final boolean flg) {asyncBreakIn(()->{
		if(flg) {
			writeSet(DOUT_ADDR1,2);
		}else {
			writeCls(DOUT_ADDR1,2);
		}
	});}
	public void giveAlarm() {
		if(tglAlarm==null) { return; }
		giveAlarm(tglAlarm.isSelected());
	}
	

	
	public void LockMasterMotor(final boolean lock) {
		asyncBreakIn(()->lockclub(lock));
	}
	private void lockclub(final boolean lock) {
		if(lock==true) {
			writeCls(DOUT_ADDR1, 3);
		}else {
			writeSet(DOUT_ADDR1, 3);
		}
	}
	
	final AtomicBoolean flag_move = new AtomicBoolean(false);
		
	public void servoMove(
		final int ID,
		final boolean flag
	) {
		if(ID==ModInsider.ID_MAJOR) {
			//no support~~~
			return;
		}		
		final int bit = (ID==ModInsider.ID_PRESS)?(1):(0);
		flag_move.set(flag);
		if(flag==false) {
			return;
		}
		asyncBreakIn(()->{	
			while(flag_move.get()==true) {
				writeSet(DOUT_ADDR2,bit);
				blocking_delay(25);
				writeCls(DOUT_ADDR2,bit);
				blocking_delay(25);
			}
		});
	};
	public void kickoff_other(final JFXToggleButton tgl) {
		if(tgl.isSelected()==true) {
			asyncBreakIn(()->{
				writeSet(DOUT_ADDR2,2);//LOP
				blocking_delay(200);
				writeSet(DOUT_ADDR2,3);//ST1
			});	
		}else {
			asyncBreakIn(()->{
				writeCls(DOUT_ADDR2,3);//ST1
				blocking_delay(4000);
				writeCls(DOUT_ADDR2,2);//LOP
			});	
		}
	}
	
	public void dout_pin(
		final JFXToggleButton tgl,
		int bit
	) {
		int addr = -1;
		if(bit>=4) {
			//bit4-7
			addr = DOUT_ADDR2;
			bit-=4;
		}else {
			//bit0~3
			addr = DOUT_ADDR1;
		}
		if(tgl.isSelected()==true) {
			asyncWriteSet(addr, bit);
		}else {
			asyncWriteCls(addr, bit);
		}		
	}

	private void blocking_delay(final int msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

