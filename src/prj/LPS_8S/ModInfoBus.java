package prj.LPS_8S;

import javafx.beans.property.IntegerProperty;
import narl.itrc.DevModbus;

/**
 * access motor servo by Modbus/RTU.<p>
 * This object access 3 servo, 1 SDE and 2 SDA series.<p> 
 * @author qq
 *
 */
public class ModInfoBus extends DevModbus {
	
	public final int DEFAULT_SPEED = 100;
	
	private static final int ID_MAIN = 3;//主軸
	private static final int ID_PRESS = 1;//加壓軸
	private static final int ID_SWING = 2;//擺動軸
		
	public final IntegerProperty MAIN_RPM;
	public final IntegerProperty MAIN_TOR;
	public final IntegerProperty MAIN_ALM;
	
	public final IntegerProperty PRESS_RPM;
	public final IntegerProperty PRESS_TOR;
	public final IntegerProperty PRESS_ALM;

	public final IntegerProperty SWING_RPM;
	public final IntegerProperty SWING_TOR;
	public final IntegerProperty SWING_ALM;

	//欄位依序是：主軸（main），加壓軸（press），擺動軸（swing）
	public final IntegerProperty[] RPM = {null, null, null};
	public final IntegerProperty[] TOR = {null, null, null};
	public final IntegerProperty[] ALM = {null, null, null};
	
	public ModInfoBus() {
		looperDelay = 0;
		
		//SDE系列
		//內部  - 註解        - Modbus地址
		//     - 瞬時轉速(RPM)- 0x0008 (2word)
		//	   - 瞬時轉矩(%)  - 0x001A (2word) 
		//	   - 異常警報     - 0x0100
		mapAddress16(ID_MAIN, "r0009","r001B","r0100");
		MAIN_RPM = inputRegister(ID_MAIN, 0x0009);
		MAIN_TOR = inputRegister(ID_MAIN, 0x000B);
		MAIN_ALM = inputRegister(ID_MAIN, 0x0100);

		//SDA系列
		//內部  - 註解        - Modbus地址
		//     - 瞬時轉速(RPM)- 0x0006
		//	   - 瞬時轉矩(%)  - 0x000F
		//	   - 異常警報     - 0x0100
		mapAddress16(ID_PRESS,"r0006","r000F","r0100");
		PRESS_RPM = inputRegister(ID_PRESS, 0x0006);
		PRESS_TOR = inputRegister(ID_PRESS, 0x000F);
		PRESS_ALM = inputRegister(ID_PRESS, 0x0100);
		mapAddress16(ID_SWING,"r0006","r000F","r0100");
		SWING_RPM = inputRegister(ID_SWING, 0x0006);
		SWING_TOR = inputRegister(ID_SWING, 0x000F);
		SWING_ALM = inputRegister(ID_SWING, 0x0100);
	}
	
	@Override
	protected void ignite() {
		//before looping, insure setting~~~
		SDE_speed_mode(ID_MAIN);
		SDA_speed_mode(ID_PRESS);
		SDA_speed_mode(ID_SWING);
	}
	
	private void SDA_speed_mode(final int sid) {
		//SDA系列
		//內部  - 註解        - Modbus 地址
		//PA01 - 控制模式     - 0x0300
		//PC01 - 加速度時間(ms)- 0x034B (0~20000)
		//PC02 - 減速度時間(ms)- 0x034C (0~20000)
		//PC03 - S型時間常數(ms)-0x034D (0~10000)
		//PC05 - 速度1(RPM)   - 0x034F (-4500~+4500)
		//PC24 - 螢幕狀態顯示  - 
		//PD01 - 輸入自動選擇  - 
		writeVal(sid, 0x034B, 1000);
		writeVal(sid, 0x034C, 1000);
		writeVal(sid, 0x034F, DEFAULT_SPEED);
	
		//Modbus:0x0387 DI mode --> 1: inside bus, 0:outside wire
		//After setting, servo will off~~~~
		writeVal(sid, 0x0387, 1);
		super.ignite();//goto next stage~~~~
	}
	public void SDA_motor(final boolean son) {asyncBreakIn(()->{
		//SDA系列
		//Modbus:0x0201 DI status
		//[B15~10]: reverse
		//           B7, B6, B5, B4, B3, B2, B1, B0
		//LSN, LSP, LOP,EMG,SP1,RES,ST2,ST1,SP2,SON
		//  1,   1,   0,  1,  1,  0,  0,  1,  0,  1 (x365--> on, ccw, 速度1 
		//  1,   1,   0,  1,  1,  0,  0,  1,  0,  0 (x364-->off, ccw, 速度1
		//  1,   1,   0,  1,  1,  0,  1,  0,  0,  1 (x369--> on,  cw, 速度1
		//  1,   1,   0,  1,  1,  0,  1,  0,  0,  0 (x368-->off,  cw, 速度1
		//  1,   1,   0,  1,  1,  0,  0,  0,  0,  1 (x361--> on,halt, 速度1
		//  1,   1,   0,  1,  1,  0,  0,  0,  0,  0 (x360-->off,halt, 速度1
		if(son==true) {
			writeVal(ID_PRESS, 0x0201, 0x365);
		}else {
			writeVal(ID_PRESS, 0x0201, 0x361);
		}
	});}
	public void setStart_press(final boolean spin, final boolean clockwise) {asyncBreakIn(()->{
		set_start(ID_PRESS,spin,clockwise);
	});}
	public void setStart_swing(final boolean spin, final boolean clockwise) {asyncBreakIn(()->{
		set_start(ID_SWING,spin,clockwise);
	});}
	private void set_start(
		final int id,
		final boolean spin, 
		final boolean clockwise
	) {
		
	}
	public void setRPM_press(final int RPM) {asyncBreakIn(()->{
		set_speed_sda(ID_PRESS,RPM);
	});}
	public void setRPM_swing(final int RPM) {asyncBreakIn(()->{
		set_speed_sda(ID_SWING,RPM);
	});}
	private void set_speed_sda(final int id,final int RPM) {
		//內部  - 註解       - Modbus地址
		//PC05 - 速度1(RPM) - 0x034F
		writeVal(id, 0x034F, RPM);
	}
	
	//-------------------------------------------//
	
	private void SDE_speed_mode(final int sid) {
		//SDE系列 *one register occupy 2 word!!
		//內部  - 註解        - Modbus 地址
		//PA01 - 控制模式     - 0x0300_0301
		//PC01 - 加速度時間(ms)- 0x0500_0501
		//PC02 - 減速度時間(ms)- 0x0502_0503
		//PC03 - S型時間常數(ms)- 0x0504_0505
		//PC05 - 速度1(RPM)   - 0x0508_0509 (-6000~+6000)
		//PC24 - 螢幕狀態顯示  - 0x052E_052F
		//PD01 - 輸入接點導向  - 0x0600_0x0601
		//PD16 - 內部接點開關  - 0x061E_0x061F
		//PD25 - 內部接點狀態  - 0x0630_0x0631
		
		writeVal(sid, 0x0508, DEFAULT_SPEED);
		
		//B11~08] 0:根據模式顯示，1:根據Bit0~4設定
		//B07~00] 6:轉速，16:轉矩
		writeVal(sid, 0x052E, 0x010F);
		
		//B15~12] EMG - 0:外部配線，1:導通b接點
		//B11~08] LSN - 0:外部配線，1:導通b接點
		//B07~04] LSP - 0:外部配線，1:導通b接點
		//B00~03] SON - 0:外部配線，1:導通b接點
		writeVal(sid, 0x0600, 0x1111);
		
		//one bit mapping one input-wire
		//a接點：SON
		//b接點：EMG,LSP,LSN
		//速度模式:
		//B11-B10-B09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//CDP- TL-   -   -LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		//0:外部配線，1:內部暫存器
		writeVal(sid, 0x061E, 0xFFFF);

		//one bit mapping one input-wire
		//a接點：SON
		//b接點：EMG,LSP,LSN
		//速度模式:
		//B11-B10-B09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//CDP- TL-   -   -LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		//0:on，1:off
		writeVal(sid, 0x0630, 0x0000);

		//B15~12] 0:端子可規劃，1:端子不可規劃(照連接圖)， 當模式切換時
		//B11~08] 0:無剎車，1:有剎車
		//B07~04] 0:端子輸入位置，1:內部輸入位置
		//B03~00] 0:位置模式，2:速度模式，4:轉矩模式，8:刀具模式,
		writeVal(sid, 0x0300, 0x1012);
		
		super.ignite();//goto next stage~~~~
	}
	public void setStart_main(final boolean spin, final boolean clockwise) {asyncBreakIn(()->{
		set_start_main(spin,clockwise);
	});}
	private void set_start_main(
		final boolean spin, 
		final boolean clockwise
	) {
		//one bit mapping one input-wire
		//速度模式:
		//a接點：SON
		//b接點：EMG,LSP,LSN
		//B11-B10-B09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//CDP- TL-   -   -LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		// 0 - 0 - 0 - 0 | 0 - 1 - 1 - 0 | 0 - 0 - 0 - 1 --> 0x061, 停止, 速度1 
		// 0 - 0 - 0 - 0 | 0 - 1 - 1 - 0 | 0 - 1 - 0 - 1 --> 0x065, 正轉, 速度1
		// 0 - 0 - 0 - 0 | 0 - 1 - 1 - 0 | 1 - 0 - 0 - 1 --> 0x069, 反轉, 速度1
		// 0 - 0 - 0 - 0 | 0 - 1 - 1 - 0 | 1 - 1 - 0 - 1 --> 0x06D, 停止, 速度1
		if(spin==true) {
			if(clockwise==true) {
				writeVal(ID_MAIN, 0x0630, 0x065);
			}else {
				writeVal(ID_MAIN, 0x0630, 0x069);
			}
		}else {
			writeVal(ID_MAIN, 0x0630, 0x061);
		}
	}
	public void setSpeed_main(final int RPM) {asyncBreakIn(()->{
		set_speed_main(RPM);
	});}
	private void set_speed_main(final int RPM) {
		//內部  - 註解        - Modbus 地址
		//PC05 - 速度1(RPM)  - 0x0508_0509 (-6000~+6000)
		writeVal(ID_MAIN, 0x0508, (RPM>> 0));
		writeVal(ID_MAIN, 0x0509, (RPM>>16));
	}
}


/*//SP2, SP1 --> 類比，速度1，速度2，速度3
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
writeVal(sid, 0x0204, val);*/



