package prj.LPS_8S;

import javafx.beans.property.IntegerProperty;
import narl.itrc.DevModbus;

/**
 * access motor servo by Modbus/RTU.<p>
 * This object access 3 servo, 1 SDE and 2 SDA series.<p> 
 * @author qq
 *
 */
public class ModServo extends DevModbus {
	
	private static final int ID1 = 1;//SDA系列
	//private static final int ID2 = 2;//SDA系列
	//private static final int ID3 = 3;//SDE系列
	
	public final IntegerProperty RPM_1;
	public final IntegerProperty TOR_1;
	public final IntegerProperty ALM_1;

	public ModServo() {
		looperDelay = 0;
		//SDA系列
		//內部  - 註解        - Modbus地址
		//     - 瞬時轉速(RPM)- 0x0006
		//	   - 瞬時轉矩(%)  - 0x000F
		//	   - 異常警報     - 0x0100
		mapAddress16(1, "r0000-000F","r0100");
		RPM_1 = inputRegister(ID1, 0x0006);
		TOR_1 = inputRegister(ID1, 0x000F);
		ALM_1 = inputRegister(ID1, 0x0100);
		
		//SDE系列
		//mapAddress(3, "r300");
		//inputRegister(slaveId, address)
		//speed[0].bind();
	}	
	@Override
	protected void ignite() {
		//before looping, insure setting~~~
		SDA_init(ID1);
	}
		
	private void SDA_init(final int sid) {
		//SDA系列
		//內部  - 註解 - Modbus地址
		//PA01 - 控制模式     - 0x0300
		//PC01 - 加速度時間(ms)- 0x034B
		//PC02 - 減速度時間(ms)- 0x034C
		//PC05 - 速度1(RPM)   - 0x034F 
		//PC06 - 速度2(RPM)   - 0x0350
		//PC07 - 速度3(RPM)   - 0x0351
		//PD01 - SON(XXX1) - 0x0351
		//PD01 - EMG(1XXX) - 0x0351
		writeVal(sid, 0x034B, 1000);
		writeVal(sid, 0x034C, 1000);
		writeVal(sid, 0x034F, 50);
		writeVal(sid, 0x0350, 50);
		writeVal(sid, 0x0351, 50);
		
		//SP2, SP1 --> 類比，速度1，速度2，速度3
		//ST2, ST1 --> 停止，反轉，正轉，停止
		//*DO:??,RD,ALM,INP/SA,HOME,TLC/VLC,MBR,WNG,ZSP,CMDOK
		//*DI:?? ,SON    ,RES    ,PC  ,TL  ,TL1 ,SP1 ,SP2,
		//    SP3,ST1/RS2,ST2/RS1,ORGP,SHOM,CM1 ,CM2 ,CR ,
		//    CDP,LOP    ,EMG    ,POS1,POS2,POS3,CTRG,HOLD 
		//Modbus:0x0204 規劃
		//[B15~12]: CN1-45 --> *DO --> RD ( 1)
		//[B11~08]: CN1-44 --> *DO --> TLC( 5)
		//[B07~04]: CN1-43 --> *DO --> 
		//[B03~00]: CN1-42 --> *DO --> ZSP( 8)
		//Modbus:0x0205 規劃
		//[B15~10]: CN1-41 --> *DO --> SA ( 3)
		//[B09~05]: CN1-21 --> *DI --> LOP(17)
		//[B04~00]: CN1-20 --> *DI --> EMG(18)
		//Modbus:0x0206 規劃
		//[B15~10]: CN1-19 --> *DI --> SP1( 6)
		//[B09~05]: CN1-18 --> *DI --> RES( 2)
		//[B04~00]: CN1-17 --> *DI --> ST2(10)
		//Modbus:0x0207 規劃
		//[B15~10]: CN1-16 --> *DI --> ST1( 9)
		//[B09~05]: CN1-15 --> *DI --> SP2( 7)
		//[B04~00]: CN1-14 --> *DI --> SON( 1) 
		final int[] val = {
			( 1<<12) | ( 5<<8) | ( 0<<4) | ( 8<<0),
			( 3<<10) | (17<<5) | (18<<0),			
			( 6<<10) | ( 2<<5) | (10<<0), 
			( 9<<10) | ( 7<<5) | ( 1<<0), 
		};
		writeVal(sid, 0x0204, val);
		
		//Modbus:0x0387 DI mode --> 1: inside bus, 0:outside wire
		//After setting, servo will off~~~~
		writeVal(sid, 0x0387, 1);
	}
	
	public void SDA_motor(final boolean son) {asyncBreakIn(()->{
		//SDA系列
		//Modbus:0x0201 DI status
		//[B15~10]: reverse
		// B7, B6, B5, B4, B3, B2, B1, B0
		//LOP,EMG,SP1,RES,ST2,ST1,SP2,SON
		//  0,  0,  1,  0,  0,  1,  0,  1 (x25--> on, ccw, 速度1 
		//  0,  0,  1,  0,  0,  1,  0,  0 (x24-->off, ccw, 速度1
		//  0,  0,  1,  0,  1,  0,  0,  1 (x29--> on, ccw, 速度1
		//  0,  0,  1,  0,  1,  0,  0,  0 (x28-->0ff, ccw, 速度1 
		if(son==true) {
			writeVal(ID1, 0x0201, 0x25);
		}else {
			writeVal(ID1, 0x0201, 0x24);
		}
	});}
	public void SDA_speed(final int RPM) {asyncBreakIn(()->{
		//內部  - 註解 - Modbus地址
		//PC05 - 速度1(RPM)   - 0x034F
		writeVal(ID1, 0x034F, RPM);
	});}
	public void working(final int sid,final boolean flag) {
		
	}
	public void workingAll(final boolean flag) {
		
	}
}
