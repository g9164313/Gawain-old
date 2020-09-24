package prj.LPS_8S;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import narl.itrc.DevModbus;

/**
 * access motor servo by Modbus/RTU.<p>
 * This object access 3 servo, 1 SDE and 2 SDA series.<p> 
 * @author qq
 *
 */
public class ModInfoBus extends DevModbus {
	
	public final int DEFAULT_SPEED = 100;
	
	public static final int ID_MAIN = 3;//主軸
	public static final int ID_PRESS = 1;//加壓軸
	public static final int ID_SWING = 2;//擺動軸
	public static final int ID_AUXIT = (ID_PRESS<<8)+ID_SWING;//加壓軸+擺動軸
	
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
	public final StringProperty[] ALM_TXT = {
		new SimpleStringProperty(""),
		new SimpleStringProperty(""),
		new SimpleStringProperty(""),
	};
	
	public ModInfoBus() {
		looperDelay = 0;
		
		//SDE系列
		//內部  - 註解        - Modbus地址
		//     - 瞬時轉速(RPM)- 0x0008 (2word)
		//	   - 瞬時轉矩(%)  - 0x001A (2word) 
		//	   - 異常警報     - 0x0100
		mapAddress16(ID_MAIN, "r0009","r001B","r0100");
		MAIN_RPM = inputRegister(ID_MAIN, 0x0009);
		MAIN_TOR = inputRegister(ID_MAIN, 0x001B);
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
		
		//keep the main parameter~~~
		RPM[0] = MAIN_RPM;
		RPM[1] = PRESS_RPM;
		RPM[2] = SWING_RPM;
		
		TOR[0] = MAIN_TOR;
		TOR[1] = PRESS_TOR;
		TOR[2] = SWING_TOR;
		
		ALM[0] = MAIN_ALM;
		ALM[1] = PRESS_ALM;
		ALM[2] = SWING_ALM;
		
		//change Alarm ID to readable text
		ALM[0].addListener((obv,oldVal,newVal)->alarm_text(newVal.intValue(),ALM_TXT[0]));
		ALM[1].addListener((obv,oldVal,newVal)->alarm_text(newVal.intValue(),ALM_TXT[1]));
		ALM[2].addListener((obv,oldVal,newVal)->alarm_text(newVal.intValue(),ALM_TXT[2]));
	}
	
	@Override
	protected void ignite() {
		//before looping, insure setting~~~
		SDE_speed_mode(ID_MAIN);
		SDA_speed_mode(ID_PRESS);
		SDA_speed_mode(ID_SWING);
		super.ignite();//goto next stage~~~~
	}
	
	private void SDA_speed_mode(final int sid) {
		//SDA系列
		//內部  - 註解        - Modbus 地址
		//PA01 - 控制模式     - 0x0300
		//PC01 - 加速度時間(ms)-0x034B
		//PC02 - 減速度時間(ms)- 0x034C
		//PC03 - S型時間常數(ms)-0x034D
		//PC05 - 速度1(RPM)   - 0x034F
		//PC24 - 螢幕狀態顯示  - 0x0362
		//PD01 - 輸入接點導向  - 0x0378
		//PD16 - 內部接點開關  - 0x0387
		//???? - 內部接點狀態  - 0x0201

		//速度1(RPM)
		writeVal(sid, 0x034F, DEFAULT_SPEED);
		
		//螢幕狀態顯示 
		//B07~04] 0:根據模式顯示，1:根據Bit0~3設定
		//B03~00] 6:轉速，16:轉矩
		writeVal(sid, 0x0362, 0x001F);
		
		//輸入接點導向
		//a接點：SON
		//b接點：EMG,LSP,LSN		
		//B15~12] EMG - 0:外部配線，1:導通b接點
		//B11~08] LSN - 0:外部配線，1:導通b接點
		//B07~04] LSP - 0:外部配線，1:導通b接點
		//B03~00] SON - 0:外部配線，1:導通a接點
		writeVal(sid, 0x0378, 0x1111);
		
		//內部接點開關（速度模式）
		//B07-B06-B05-B04-B03-B02-B01-B00
		//LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		//0:外部配線，1:內部暫存器
		writeVal(sid, 0x0387, 0x00FF);
		
		//內部接點狀態（速度模式）
		//F09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//LSN-LSP-LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		//0:on，1:off
		writeVal(sid, 0x0201, 0x0300);
				
		//極限開關模式
		//B03~00] 0:急停，1:依據減速時間
		
		//控制模式
		//B15~12] 0:端子可規劃，1:端子不可規劃(照連接圖)， 當模式切換時
		//B11~08] 0:無剎車，1:有剎車
		//B07~04] 0:端子輸入位置，1:內部輸入絕對位置，2:內部輸入相對位置
		//B03~00] 0:位置模式，2:速度模式，4:轉矩模式
		writeVal(sid, 0x0300, 0x1012);
	}
	private void SDA_kickoff(
		final int id,
		final boolean spin, 
		final boolean clockwise
	) {
		//內部接點狀態（速度模式）
		//F09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//LSN-LSP-LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		// 1 - 1 | 0 - 1 - 1 - 0 | 0 - 0 - 0 - 1 --> 0x361,速度1,停止,
		// 1 - 1 | 0 - 1 - 1 - 0 | 0 - 1 - 0 - 1 --> 0x365,速度1,正轉, 
		// 1 - 1 | 0 - 1 - 1 - 0 | 1 - 0 - 0 - 1 --> 0x369,速度1,反轉, 
		// 1 - 1 | 0 - 1 - 1 - 0 | 1 - 1 - 0 - 1 --> 0x36D,速度1,停止, 
		//writeVal(sid, 0x0201, 0x0300);
		if(spin==true) {
			if(clockwise==true) {
				writeVal(id, 0x0201, 0x365);
			}else {
				writeVal(id, 0x0201, 0x369);
			}
		}else {
			writeVal(id, 0x0201, 0x361);
		}
	}
	private void SDA_speed(final int id,final int RPM) {
		//速度1(RPM)
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
		
		//速度1(RPM)
		writeVal(sid, 0x0508, DEFAULT_SPEED);
		
		//螢幕狀態顯示 
		//B11~08] 0:根據模式顯示，1:根據Bit0~4設定
		//B07~00] 6:轉速，16:轉矩
		writeVal(sid, 0x052E, 0x010F);
		
		//輸入接點導向
		//a接點：SON
		//b接點：EMG,LSP,LSN		
		//B15~12] EMG - 0:外部配線，1:導通b接點
		//B11~08] LSN - 0:外部配線，1:導通b接點
		//B07~04] LSP - 0:外部配線，1:導通b接點
		//B00~03] SON - 0:外部配線，1:導通a接點
		writeVal(sid, 0x0600, 0x1111);
		
		//內部接點開關（速度模式）
		//B11-B10-B09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//CDP- TL-   -   -LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		//0:外部配線，1:內部暫存器
		writeVal(sid, 0x061E, 0x0FFF);

		//內部接點狀態（速度模式）
		//B11-B10-B09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//CDP- TL-   -   -LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		//0:on，1:off
		writeVal(sid, 0x0630, 0x0000);

		//控制模式
		//B15~12] 0:端子可規劃，1:端子不可規劃(照連接圖)， 當模式切換時
		//B11~08] 0:無剎車，1:有剎車
		//B07~04] 0:端子輸入位置，1:內部輸入位置
		//B03~00] 0:位置模式，2:速度模式，4:轉矩模式，8:刀具模式,
		writeVal(sid, 0x0300, 0x1012);
	}
	private void SDE_kickoff(
		final boolean spin, 
		final boolean clockwise
	) {
		//內部接點狀態（速度模式）
		//B11-B10-B09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//CDP- TL-   -   -LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		// 0 - 0 - 0 - 0 | 0 - 1 - 1 - 0 | 0 - 0 - 0 - 1 --> 0x061,速度1,停止, 
		// 0 - 0 - 0 - 0 | 0 - 1 - 1 - 0 | 0 - 1 - 0 - 1 --> 0x065,速度1,正轉,
		// 0 - 0 - 0 - 0 | 0 - 1 - 1 - 0 | 1 - 0 - 0 - 1 --> 0x069,速度1,反轉,
		// 0 - 0 - 0 - 0 | 0 - 1 - 1 - 0 | 1 - 1 - 0 - 1 --> 0x06D,速度1,停止, 
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
	private void SDE_speed(final int RPM) {
		//內部  - 註解        - Modbus 地址
		//PC05 - 速度1(RPM)  - 0x0508_0509 (-6000~+6000)
		writeVal(ID_MAIN, 0x0508, (RPM>> 0));
		writeVal(ID_MAIN, 0x0509, (RPM>>16));
	}
	
	//-------------------------------------------//
	
	public IntegerProperty getSpeed(final int ID) {
		switch(ID) {
		case ID_MAIN: return MAIN_RPM;
		case ID_PRESS:return PRESS_RPM;
		case ID_SWING:return SWING_RPM;
		}
		return null;
	}
	public IntegerProperty getTorr(final int ID) {
		switch(ID) {
		case ID_MAIN: return MAIN_TOR;
		case ID_PRESS:return PRESS_TOR;
		case ID_SWING:return SWING_TOR;
		}
		return null;
	}
	public IntegerProperty getAlarm(final int ID) {
		switch(ID) {
		case ID_MAIN: return MAIN_ALM;
		case ID_PRESS:return PRESS_ALM;
		case ID_SWING:return SWING_ALM;
		}
		return null;
	}
	public StringProperty getAlarmText(final int ID) {
		switch(ID) {
		case ID_MAIN: return ALM_TXT[0];
		case ID_PRESS:return ALM_TXT[1];
		case ID_SWING:return ALM_TXT[2];
		}
		return null;
	}
	
	public void setSpeed(
		final int ID,
		final int RPM
	) {
		switch(ID) {
		case ID_MAIN:
			asyncBreakIn(()->SDE_speed(RPM));
			break;
		case ID_PRESS:
		case ID_SWING:
			asyncBreakIn(()->SDA_speed(ID,RPM));
			break;
		}
	}
	
	public void kickoff(
		final int ID,
		final boolean SPIN,
		final boolean CLOCKWISE
	) {
		switch(ID) {
		case ID_MAIN:
			asyncBreakIn(()->SDE_kickoff(SPIN,CLOCKWISE));
			break;
		case ID_PRESS:
		case ID_SWING:
			asyncBreakIn(()->SDA_kickoff(ID,SPIN,CLOCKWISE));
			break;
		case ID_AUXIT:
			asyncBreakIn(()->{
				SDA_kickoff(ID_PRESS,SPIN,CLOCKWISE);
				SDA_kickoff(ID_SWING,SPIN,CLOCKWISE);
			});
			break;
		}
	}
	public void kickoff_all(
		final boolean SPIN,
		final boolean CW_MAIN,
		final boolean CW_PRESS,
		final boolean CW_SWING
	) {asyncBreakIn(()->{
		SDE_kickoff(SPIN,CW_MAIN);
		SDA_kickoff(ID_PRESS,SPIN,CW_PRESS);
		SDA_kickoff(ID_SWING,SPIN,CW_SWING);
	});}	
	public void kickoff_all(
		final boolean SPIN		
	) {
		kickoff_all(SPIN,true,true,true);
	}
	
	private final void alarm_text(final int val,final StringProperty txt) {
		switch(val) {
		case  1: txt.set("過電壓"); break;
		case  2: txt.set("低電壓"); break;
		case  3: txt.set("過電流"); break;
		case  4: txt.set("回生異常"); break;
		case  5: txt.set("過負載"); break;
		case  6: txt.set("過速度"); break;
		case  7: txt.set("異常脈波控制命令"); break;
		case  8: txt.set("位置控制誤差過大"); break;
		case  9: txt.set("串列通訊異常"); break;
		case 10: txt.set("串列通訊逾時"); break;
		case 11: txt.set("位置檢出器異常 1"); break;
		case 12: txt.set("位置檢出器異常 2"); break;
		case 13: txt.set("風扇異常"); break;
		case 14: txt.set("IGBT 過溫"); break;
		case 15: txt.set("記憶體異常"); break;
		
		case 16: txt.set("過負載 2"); break;
		case 17: txt.set("馬達匹配異常");  break;
		case 18: txt.set("緊急停止"); break;
		case 19: txt.set("正反轉極限異常"); break;
		
		case 0x20: txt.set("馬達碰撞錯誤"); break;
		case 0x21: txt.set("馬達 UVW 斷線"); break;
		case 0x22: txt.set("編碼器通訊異常"); break;
		case 0x24: txt.set("馬達編碼器種類錯誤"); break;
		case 0x26: txt.set("位置檢出器異常 3"); break;
		case 0x27: txt.set("位置檢出器異常 4"); break;
		case 0x28: txt.set("位置檢出器過熱"); break;
		case 0x29: txt.set("位置檢出器溢位 5"); break;
		case 0x2A: txt.set("絕對型編碼器異常 1"); break;
		case 0x2B: txt.set("絕對型編碼器異常 2"); break;
		case 0x2E: txt.set("控制迴路異常"); break;
		case 0x2F: txt.set("回生能量異常"); break;
		
		case 0x30: txt.set("脈波輸出檢出器頻率過高"); break;
		case 0x31: txt.set("過電流 2"); break;
		case 0x32: txt.set("控制迴路異常 2"); break;
		case 0x33: txt.set("記憶體異常 2"); break;
		case 0x34: txt.set("過負載 4"); break;
		default: txt.set("？？？"); break;
		}
	}
}





