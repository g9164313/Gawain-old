package prj.LPS_8S;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import narl.itrc.DevModbus;
import narl.itrc.PadTouch;

/**
 * access motor servo by Modbus/RTU.<p>
 * This object access 3 servo, 1 SDE and 2 SDA series.<p> 
 * @author qq
 *
 */
public class ModInsider extends DevModbus {
	
	//SDA - A綠，B棕白
	//SDE - A藍白 ，B藍
	//平板電腦: A白，B黑
	
	public final int DEFAULT_SPEED = 1000;
	
	
	public static final int ID_MAJOR = 3;//主軸
	public static final int ID_PRESS = 2;//加壓軸
	public static final int ID_SWING = 1;//擺動軸
	public static final int ID_OTHER = (ID_PRESS<<8)+ID_SWING;//加壓軸+擺動軸
	
	public static final int ID_FA231 = 4;//溫度計
	public static final int ID_EC4310= 5;//電導度
	
	//flatten information:
	//主軸（main）的，RPM，TOR，ALM
	//加壓軸（press）的，RPM，TOR，ALM
	//擺動軸（swing）的，RPM，TOR，ALM

	public final IntegerProperty MAJOR_RPM;
	public final IntegerProperty MAJOR_TOR;
	public final IntegerProperty MAJOR_ALM;

	/*
	 * PIN mapping in SDA servo:
	 * B15-B14-B13-B12-B11-B10-B09-B08-
	 * ALM-DO5-DO4-DO3-DO2-DO1-LSN-LSP-
	 * 
	 * B07-B06-B05-B04-B03-B02-B01-B00
	 * DI8-DI7-DI6-DI5-DI4-DI3-DI2-DI1
	 */
	public final IntegerProperty PRESS_RPM;
	public final IntegerProperty PRESS_TOR;
	public final IntegerProperty PRESS_ALM;
	public final IntegerProperty PRESS_PIN;	
	public final BooleanProperty PRESS_ZSP  = new SimpleBooleanProperty();
	public final BooleanProperty PRESS_CMDOK= new SimpleBooleanProperty();
	
	public final IntegerProperty SWING_RPM;
	public final IntegerProperty SWING_TOR;
	public final IntegerProperty SWING_ALM;
	public final IntegerProperty SWING_PIN;
	public final BooleanProperty SWING_ZSP  = new SimpleBooleanProperty();
	public final BooleanProperty SWING_CMDOK= new SimpleBooleanProperty();

	public final BooleanProperty OTHER_ZSP  = new SimpleBooleanProperty();//零速度檢出
	public final BooleanProperty OTHER_CMDOK= new SimpleBooleanProperty();//內部位置命令完成輸出

	public final IntegerProperty PV_FA231;
	public final FloatProperty   PV_COND,PV_TEMP;	
		
	public ModInsider() {
		
		looperDelay = 250;//good delay~~
		
		mapAddress16(ID_EC4310,"h031-038");
		PV_COND = mapFloat(ID_EC4310, 0x035);
		PV_TEMP = mapFloat(ID_EC4310, 0x037);
		
		mapAddress16(ID_FA231,"h08A");
		PV_FA231 = mapInteger(ID_FA231, 0x08A);
		
		//SDA系列
		//內部  - 註解        - Modbus地址
		//    - 瞬時轉速(RPM)- 0x0006
		//	  - 瞬時轉矩(%)  - 0x000F
		//	  - 異常警報     - 0x0100
		//    - PIN port - 0x0203
		mapAddress16(ID_PRESS,"h0006","h000F","h0100","h0203");
		PRESS_RPM = mapInteger(ID_PRESS, 0x0006);		
		PRESS_TOR = mapInteger(ID_PRESS, 0x000F);
		PRESS_ALM = mapInteger(ID_PRESS, 0x0100);
		PRESS_PIN = mapInteger(ID_PRESS, 0x0203);
		
		mapAddress16(ID_SWING,"h0006","h000F","h0100","h0203");
		SWING_RPM = mapInteger(ID_SWING, 0x0006);
		SWING_TOR = mapInteger(ID_SWING, 0x000F);
		SWING_ALM = mapInteger(ID_SWING, 0x0100);
		SWING_PIN = mapInteger(ID_SWING, 0x0203);
		
		//SDE系列
		//內部  - 註解        - Modbus地址
		//     - 瞬時轉速(RPM)- 0x0008 (2word)
		//	   - 瞬時轉矩(%)  - 0x001A (2word) 
		//	   - 異常警報     - 0x0100
		mapAddress16(ID_MAJOR,"h008","h01A","h100");
		MAJOR_RPM = mapInteger(ID_MAJOR, 0x0008);
		MAJOR_TOR = mapInteger(ID_MAJOR, 0x001A);
		MAJOR_ALM = mapInteger(ID_MAJOR, 0x0100);
	}
	
	public final IntegerProperty MAJOR_RPM_SV = new SimpleIntegerProperty();
	public final IntegerProperty MAJOR_PLS_SV = new SimpleIntegerProperty();
	
	public final IntegerProperty PRESS_RPM_SV = new SimpleIntegerProperty();
	public final IntegerProperty PRESS_PLS_SV = new SimpleIntegerProperty();	
	
	public final IntegerProperty SWING_RPM_SV = new SimpleIntegerProperty();
	public final IntegerProperty SWING_PLS_SV = new SimpleIntegerProperty();
	
	private static final int SPD_DECAY = 40;
	private static final int SDA_ADDR_RPM = 0x34F;//PC05 - 內部速度命令
	//private static final int SDA_ADDR_POS = 0x310;//PA17 - 位置圈數(REV)
	private static final int SDA_ADDR_POS = 0x311;//PA18 - 位置脈波數(PULSE)
	
	@Override
	protected void ignite() {
		
		final int V_PRESS_RPM1 = readReg(ID_PRESS,'I',SDA_ADDR_RPM);
		final int V_PRESS_POS1 = readReg(ID_PRESS,'I',SDA_ADDR_POS);
				
		final int V_SWING_RPM1 = readReg(ID_SWING,'I',SDA_ADDR_RPM);
		final int V_SWING_POS1 = readReg(ID_SWING,'I',SDA_ADDR_POS);
		
		//PC05 - 速度1(RPM)
		final int V_MAJOR_RPM1 = readReg(ID_MAJOR, 'I', 0x0508);
		
		OTHER_ZSP.bind(PRESS_ZSP.and(SWING_ZSP));
		OTHER_CMDOK.bind(PRESS_CMDOK.and(SWING_CMDOK));
		
		PRESS_PIN.addListener((obv,oldVal,newVal)->{
			PRESS_ZSP.set  ( (newVal.intValue()&0x0400)!=0 );
			PRESS_CMDOK.set( (newVal.intValue()&0x0800)!=0 );
		});
		SWING_PIN.addListener((obv,oldVal,newVal)->{
			SWING_ZSP.set  ( (newVal.intValue()&0x0400)!=0 );
			SWING_CMDOK.set( (newVal.intValue()&0x0800)!=0 );
		});
		
		//SDE 內部接點開關（速度模式）- PD16
		//B11-B10-B09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//CDP- TL- ? - ? -LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		//0:外部配線，1:內部暫存器
		writeCont_sid(ID_MAJOR, 0x061E, 0x0FFF);
		//SDE 內部接點狀態（速度模式）- PD25
		//B11-B10-B09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//CDP- TL- ? - ? -LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		//0:on，1:off
		writeCont_sid(ID_MAJOR, 0x0630, 0x0000);

		Application.invokeLater(()->{
			
			MAJOR_RPM_SV.set(V_MAJOR_RPM1/SPD_DECAY);
			MAJOR_PLS_SV.set(0);
			
			PRESS_RPM_SV.set(V_PRESS_RPM1/SPD_DECAY);
			PRESS_PLS_SV.set(V_PRESS_POS1);
			
			SWING_RPM_SV.set(V_SWING_RPM1/SPD_DECAY);
			SWING_PLS_SV.set(V_SWING_POS1);
		});
		super.ignite();//goto next stage~~~~
	}
	//-------------------------------------------//
	
	private final AtomicBoolean flag_major_move = new AtomicBoolean(false);
	
	public void majorMove(final boolean flag) {
		flag_major_move.set(flag);
		if(flag==false) {
			return;
		}
		asyncBreakIn(()->{
			while(flag_major_move.get()==true) {
				//LOP is off : position mode
				//-B07-B06 -B05 -B04 -B03-B02-B01-B00-
				//-LOP-POS2-POS1-CTRG-STP-xxx-xxx-xxx-
				writeCont_sid(ID_MAJOR, 0x0630, 0x030);
				block_delay(50);
				writeCont_sid(ID_MAJOR, 0x0630, 0x000);
				block_delay(50);
			}
		});
	}
	
	public void majorKickoff(final JFXToggleButton tgl) {
		final boolean SPIN = tgl.isSelected();
		asyncBreakIn(()->{
			//LOP is on : speed mode
			//-B07-B06-B05-B04-B03-B02-B01-B00-
			//-LOP-xxx-xxx-xxx-xxx-SP1-ST2-ST1-
			if(SPIN==true) {
				//if(clockwise==true) {
				//	writeCont_sid(ID_MAJOR, 0x0630, 0x085);
				//}else {
					writeCont_sid(ID_MAJOR, 0x0630, 0x086);
				//}
			}else {
				writeCont_sid(ID_MAJOR, 0x0630, 0x080);
			}
		});
	}
	
	//-------------------------------------------//
	
	public void setLocatePulse(final int ID,final int val) {		
		switch(ID) {
		case ID_MAJOR: MAJOR_PLS_SV.set(val); break;
		case ID_PRESS: PRESS_PLS_SV.set(val); break;
		case ID_SWING: SWING_PLS_SV.set(val); break;
		default: return;
		}
		asyncBreakIn(()->{
			if(ID==ID_MAJOR) {
				writeCont_sid(ID, 0x030E, 5000);
			}else {
				writeCont_sid(ID, SDA_ADDR_POS, val);
			}
		});
	}
	
	public void setSpeed(final int ID,final int RPM) {		
		switch(ID) {
		case ID_MAJOR:
			MAJOR_RPM_SV.set(RPM);
			asyncBreakIn(()->{
				//內部  - 註解        - Modbus 地址
				//PC05 - 速度1(RPM)  - 0x0508_0509 (-6000~+6000)
				final int _v = SPD_DECAY * RPM;
				writeCont_sid(ID_MAJOR, 0x0508, (_v>> 0));
				writeCont_sid(ID_MAJOR, 0x0509, (_v>>16));
			});
			break;
		case ID_PRESS:
			PRESS_RPM_SV.set(RPM);
			asyncBreakIn(()->writeCont_sid(ID, SDA_ADDR_RPM, SPD_DECAY*RPM));
			break;
		case ID_SWING:
			SWING_RPM_SV.set(RPM);
			asyncBreakIn(()->writeCont_sid(ID, SDA_ADDR_RPM, SPD_DECAY*RPM));
			break;
		}
	}

	public void set_FA231(final float val) {asyncBreakIn(()->{
		writeCont_sid(ID_FA231, 0, (int)(val*10.f));
	});}
	
	private void init_text_box(
		final Label txt,
		final IntegerProperty prop,
		final String title,
		final int ID,
		final int ADDR,
		final int MAXIUM,
		final int SCALE
	) {
		txt.getStyleClass().addAll("font-size5");
		txt.setMinWidth(90.);
		txt.textProperty().bind(prop.asString("%4d"));
		txt.setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('I',title);
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			int val = Integer.valueOf(opt.get());
			if(val>MAXIUM && MAXIUM>0) {
				return;
			}
			prop.set(val);
			asyncBreakIn(()->writeCont_sid(ID, ADDR, val*SCALE));
		});
	}
	
	public Node gen_console() {
		
		final JFXCheckBox[] chk = {
			new JFXCheckBox(),//PRESS - ZSP
			new JFXCheckBox(),//PRESS - CMDOK
			new JFXCheckBox(),//SWING - ZSP
			new JFXCheckBox(),//SWING - CMDOK
		};
		for(int i=0; i<chk.length; i++) {
			chk[i].setDisable(true);
			chk[i].setStyle("-fx-opacity: 1.0;");
		}
		chk[0].selectedProperty().bind(PRESS_ZSP);		
		chk[1].selectedProperty().bind(PRESS_ZSP);
		chk[2].selectedProperty().bind(SWING_ZSP);		
		chk[3].selectedProperty().bind(SWING_ZSP);
		
		//RPM,POS
		final Label[] txt = new Label[6];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
		}
		init_text_box(txt[1],PRESS_RPM_SV,"加壓軸(RPM)",ID_PRESS,0x034F,40,40);
		init_text_box(txt[2],SWING_RPM_SV,"擺動軸(RPM)",ID_SWING,0x034F,40,40);
		
		init_text_box(txt[4],PRESS_PLS_SV,"加壓軸(PULSE)",ID_PRESS,SDA_ADDR_POS,-1,1);
		init_text_box(txt[5],SWING_PLS_SV,"擺動軸(PULSE)",ID_SWING,SDA_ADDR_POS,-1,1);
		
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad","box-border");
		lay0.addColumn(0, 
			new Label("名稱"),
			new Label("ZSP"),
			new Label("CMDOK"),
			new Label("轉速"),
			new Label("定位")
		);
		lay0.addColumn(1,
			new Label("主軸"),
			new Label(),
			new Label(),
			txt[0],
			txt[3]
		);
		lay0.addColumn(2,
			new Label("加壓軸"),
			chk[0],
			chk[1],
			txt[1],
			txt[4]
		);
		lay0.addColumn(3,
			new Label("擺動軸"),
			chk[2],
			chk[3],
			txt[2],
			txt[5]
		);
		return lay0;
	}
	private void block_delay(final int msec) {
		try {
			Thread.sleep(msec);//????
		} catch (InterruptedException e) {
		}
	}
}


/*private void SDA_init(final int sid) {
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
//writeVal(sid, 0x034F, DEFAULT_SPEED);

//螢幕狀態顯示  - PC24
//B07~04] 0:根據模式顯示，1:根據Bit0~3設定
//B03~00] 6:轉速，16:轉矩
//writeVal(sid, 0x0362, 0x001F);
//writeVal(sid, 0x0362, 0x0000);

//輸入接點導向 - PD01
//a接點：SON
//b接點：EMG,LSP,LSN		
//B15~12] EMG - 0:外部配線，1:導通 (b接點
//B11~08] LSN - 0:外部配線，1:導通 (b接點
//B07~04] LSP - 0:外部配線，1:導通 (b接點
//B03~00] SON - 0:外部配線，1:導通 (a接點
//these will take effect after reseting servo
//writeVal(sid, 0x0378, 0x1111);

//內部接點開關- PD16
//0:外部配線，1:內部暫存器
//writeVal(sid, 0x0387, 0x0001);

//控制模式 - PA01
//B15~12] 0:端子可規劃，1:端子不可規劃(照連接圖)， 當模式切換時
//B11~08] 0:無剎車，1:有剎車
//B07~04] 0:端子輸入位置，1:內部輸入絕對位置，2:內部輸入相對位置
//B03~00] 0:位置模式，2:速度模式，4:轉矩模式
//writeVal(sid, 0x0300, 0x1012);
}*/

/*
private void SDE_init(final int sid) {
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
	//writeVal(sid, 0x0508, DEFAULT_SPEED);
	
	//螢幕狀態顯示 
	//B11~08] 0:根據模式顯示，1:根據Bit0~4設定
	//B07~00] 6:轉速，16:轉矩
	//writeVal(sid, 0x052E, 0x010F);
	
	//輸入接點導向 - PD01
	//a接點：SON
	//b接點：EMG,LSP,LSN		
	//B15~12] EMG - 0:外部配線，1:導通 (b接點
	//B11~08] LSN - 0:外部配線，1:導通 (b接點
	//B07~04] LSP - 0:外部配線，1:導通 (b接點
	//B00~03] SON - 0:外部配線，1:導通 (a接點
	//writeVal(sid, 0x0600, 0x1111);

	//控制模式
	//B15~12] 0:端子可規劃，1:端子不可規劃(照連接圖)， 當模式切換時
	//B11~08] 0:無剎車，1:有剎車
	//B07~04] 0:端子輸入位置，1:內部輸入位置
	//B03~00] 0:位置模式，2:速度模式，4:轉矩模式，8:刀具模式,
	//writeVal(sid, 0x0300, 0x1012);
}
*/







