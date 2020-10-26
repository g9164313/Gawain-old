package prj.LPS_8S;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import narl.itrc.DevModbus;
import narl.itrc.Misc;
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
	public static final int ID_FC4310= 5;//電導度
	
	//flatten information:
	//主軸（main）的，RPM，TOR，ALM
	//加壓軸（press）的，RPM，TOR，ALM
	//擺動軸（swing）的，RPM，TOR，ALM

	public final IntegerProperty PV_MAJOR_RPM;
	public final IntegerProperty MAJOR_TOR;
	public final IntegerProperty MAJOR_ALM;
	
	public final IntegerProperty PV_PRESS_RPM;
	public final IntegerProperty PRESS_TOR;
	public final IntegerProperty PRESS_ALM;

	public final IntegerProperty PV_SWING_RPM;
	public final IntegerProperty SWING_TOR;
	public final IntegerProperty SWING_ALM;

	public final IntegerProperty PV_FA231;
	public final IntegerProperty PV_COND;
	
	//欄位依序是：主軸（main），加壓軸（press），擺動軸（swing）
	public final IntegerProperty[] RPM = {null, null, null};
	public final IntegerProperty[] TOR = {null, null, null};
	public final IntegerProperty[] ALM = {null, null, null};
	
	public final StringProperty[] ALM_TXT = {
		new SimpleStringProperty("--ERROR--"),
		new SimpleStringProperty("--ERROR--"),
		new SimpleStringProperty("--ERROR--"),
	};
	
	public ModInsider() {
		
		//looperDelay = 100;
		
		mapAddress16(ID_FA231,"i08A");
		PV_FA231 = inputRegister(ID_FA231, 0x08A);
		
		//mapAddress16(ID_FC4310,"h035");
		PV_COND = holdingRegister(ID_FC4310, 0x035);
		
		//SDA系列
		//內部  - 註解        - Modbus地址
		//     - 瞬時轉速(RPM)- 0x0006
		//	   - 瞬時轉矩(%)  - 0x000F
		//	   - 異常警報     - 0x0100
		//mapAddress16(ID_PRESS,"i0006","i000F","i0100");
		RPM[1] = PV_PRESS_RPM = inputRegister(ID_PRESS, 0x0006);
		TOR[1] = PRESS_TOR = inputRegister(ID_PRESS, 0x000F);
		ALM[1] = PRESS_ALM = inputRegister(ID_PRESS, 0x0100);
		//mapAddress16(ID_SWING,"i0006","i000F","i0100");
		RPM[2] = PV_SWING_RPM = inputRegister(ID_SWING, 0x0006);
		TOR[2] = SWING_TOR = inputRegister(ID_SWING, 0x000F);
		ALM[2] = SWING_ALM = inputRegister(ID_SWING, 0x0100);
		
		//SDE系列
		//內部  - 註解        - Modbus地址
		//     - 瞬時轉速(RPM)- 0x0008 (2word)
		//	   - 瞬時轉矩(%)  - 0x001A (2word) 
		//	   - 異常警報     - 0x0100
		//mapAddress16(ID_MAJOR,"h008","h01A","h100");
		/*= RPM[0]*/ PV_MAJOR_RPM = holdingRegister(ID_MAJOR, 0x0008);
		TOR[0] = MAJOR_TOR = holdingRegister(ID_MAJOR, 0x001A);
		ALM[0] = MAJOR_ALM = holdingRegister(ID_MAJOR, 0x0100);
		
		RPM[0] = new SimpleIntegerProperty(); 
		RPM[0].bind(PV_MAJOR_RPM.multiply(-1));
		
		//change Alarm ID to readable text
		if(ALM[0]!=null) {
			ALM[0].addListener((obv,oldVal,newVal)->alarm_text(newVal.intValue(),ALM_TXT[0]));
		}
		if(ALM[1]!=null) {
			ALM[1].addListener((obv,oldVal,newVal)->alarm_text(newVal.intValue(),ALM_TXT[1]));
		}
		if(ALM[2]!=null) {
			ALM[2].addListener((obv,oldVal,newVal)->alarm_text(newVal.intValue(),ALM_TXT[2]));
		}
	}
	
	public final IntegerProperty SV_MAJOR_RPM1 = new SimpleIntegerProperty();
	public final IntegerProperty SV_MAJOR_RPM2 = new SimpleIntegerProperty();
	public final IntegerProperty SV_MAJOR_MOV1 = new SimpleIntegerProperty();
	public final IntegerProperty SV_MAJOR_MOV2 = new SimpleIntegerProperty();
	
	public final IntegerProperty SV_PRESS_RPM1 = new SimpleIntegerProperty();
	public final IntegerProperty SV_PRESS_RPM2 = new SimpleIntegerProperty();
	public final IntegerProperty SV_PRESS_MOV1 = new SimpleIntegerProperty();
	public final IntegerProperty SV_PRESS_MOV2 = new SimpleIntegerProperty();
	
	public final IntegerProperty SV_SWING_RPM1 = new SimpleIntegerProperty();
	public final IntegerProperty SV_SWING_RPM2 = new SimpleIntegerProperty();
	public final IntegerProperty SV_SWING_MOV1 = new SimpleIntegerProperty();
	public final IntegerProperty SV_SWING_MOV2 = new SimpleIntegerProperty();
	
	@Override
	protected void ignite() {
		looperDelay = 100;
		//before looping, insure setting~~~	
		//PA15 -0x030E- 內部位置命令1之位置旋轉圈數設定(rev)
		//PA17 -0x0310- 內部位置命令2之位置旋轉圈數設定(rev)
		final int PRESS_REV1 = readReg(ID_PRESS,'I',0x030E);
		final int PRESS_REV2 = readReg(ID_PRESS,'I',0x0310);
		final int SWING_REV1 = readReg(ID_SWING,'I',0x030E);
		final int SWING_REV2 = readReg(ID_SWING,'I',0x0310);
		
		//PC05 - 速度1(RPM)
		final int MAJOR_RPM1 = readReg(ID_MAJOR, 'I', 0x0508);
		final int PRESS_RPM1 = readReg(ID_PRESS, 'I', 0x034F);
		final int SWING_RPM1 = readReg(ID_SWING, 'I', 0x034F);
		
		//內部接點開關（速度模式）- PD16
		//B11-B10-B09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//CDP- TL- ? - ? -LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		//0:外部配線，1:內部暫存器
		writeVal(ID_MAJOR, 0x061E, 0x0FFF);
		//內部接點狀態（速度模式）- PD25
		//B11-B10-B09-B08-B07-B06-B05-B04-B03-B02-B01-B00
		//CDP- TL- ? - ? -LOP-EMG-SP1-RES-ST2-ST1-SP2-SON
		//0:on，1:off
		writeVal(ID_MAJOR, 0x0630, 0x0);

		Application.invokeLater(()->{
			
			SV_MAJOR_RPM1.set(MAJOR_RPM1);
			
			SV_PRESS_RPM1.set(PRESS_RPM1);
			SV_PRESS_MOV1.set(PRESS_REV1);
			SV_PRESS_MOV2.set(PRESS_REV2);
			
			SV_SWING_RPM1.set(SWING_RPM1);
			SV_SWING_MOV1.set(SWING_REV1);
			SV_SWING_MOV2.set(SWING_REV2);
		});
		super.ignite();//goto next stage~~~~
	}
	private void SDA_speed(final int id,final int val) {
		//PC05 - 速度1(RPM)
		writeVal(id, 0x034F, 40*val);
		Application.invokeLater(()->{
			switch(id) {
			case ID_SWING:
				SV_SWING_RPM1.set(val);
				break;
			case ID_PRESS:
				SV_PRESS_RPM1.set(val);
				break;
			}
		});
	}
	private void SDA_kickoff(
		final int id,
		final boolean spin, 
		final boolean clockwise
	) {
		//LOP is on : speed mode
		//-LOP-xxx-xxx-xxx -xxx-SP1-ST2-ST1-
		if(spin==true) {//PD
			if(clockwise==true) {
				writeVal(id, 0x0201, 0x085);
			}else {
				writeVal(id, 0x0201, 0x086);
			}
		}else {
			writeVal(id, 0x0201, 0x080);
		}
	}
	private void SDA_move(
		final int id,
		final int pos
	) {
		//LOP is off : position mode
		//-LOP-POS2-POS1-CTRG-xxx-xxx-xxx-xxx-
		
		//clear flag
		writeVal(id, 0x0201, 0x000);
		block_delay(50);
		//rise flag
		switch(pos) {
		case 1: writeVal(id, 0x0201, 0x010); break;
		case 2: writeVal(id, 0x0201, 0x030); break;
		}
	}	
	//PA16 -0x030F- 內部位置命令1之位置脈波數設定(pulse)
	//PA18 -0x0311- 內部位置命令2之位置脈波數設定(pulse)
	private void SDA_revolution(
		final int id,
		final int idx,
		final int val
	) {
		//PA15 -0x030E- 內部位置命令1之位置旋轉圈數設定(rev)
		//PA17 -0x0310- 內部位置命令2之位置旋轉圈數設定(rev)
		writeVal(id, 0x030E+(idx-1)*2, val);
		Application.invokeLater(()->{
			switch(id) {
			case ID_SWING:
				switch(idx) {
				case 1: SV_SWING_MOV1.set(val); break;
				case 2: SV_SWING_MOV2.set(val); break;
				} 
				break;
			case ID_PRESS:
				switch(idx) {
				case 1: SV_PRESS_MOV1.set(val); break;
				case 2: SV_PRESS_MOV2.set(val); break;
				}
				break;
			}
		});
	}
	
	//-------------------------------------------//
	private void SDE_speed(final int val) {
		//內部  - 註解        - Modbus 地址
		//PC05 - 速度1(RPM)  - 0x0508_0509 (-6000~+6000)
		writeVal(ID_MAJOR, 0x0508, (40*val>> 0));
		writeVal(ID_MAJOR, 0x0509, (40*val>>16));
		Application.invokeLater(()->{
			SV_MAJOR_RPM1.set(val);
		});
	}
	private void SDE_kickoff(
		final boolean spin, 
		final boolean clockwise
	) {
		//LOP is on : speed mode
		//-B07-B06-B05-B04-B03-B02-B01-B00-
		//-LOP-xxx-xxx-xxx-xxx-SP1-ST2-ST1-
		if(spin==true) {
			if(clockwise==true) {
				writeVal(ID_MAJOR, 0x0630, 0x085);
			}else {
				writeVal(ID_MAJOR, 0x0630, 0x086);
			}
		}else {
			writeVal(ID_MAJOR, 0x0630, 0x080);
		}
	}
	private void SDE_move(final int pos) {
		//LOP is off : position mode
		//-B07-B06 -B05 -B04 -B03-B02-B01-B00-
		//-LOP-POS2-POS1-CTRG-STP-xxx-xxx-xxx-
		writeVal(ID_MAJOR, 0x0630, 0x000);
		block_delay(100);
		//rise flag
		writeVal(ID_MAJOR, 0x0630, 0x030);
	}
	private void SDE_set_pulse(final int pos) {

		writeVal(ID_MAJOR, 0x030E, pos);
	}
	//-------------------------------------------//
	
	public IntegerProperty getSpeed(final int ID) {
		switch(ID) {
		case ID_MAJOR:return RPM[0];//return MAJOR_RPM;
		case ID_PRESS:return PV_PRESS_RPM;
		case ID_SWING:return PV_SWING_RPM;
		}
		return null;
	}
	public IntegerProperty getTorr(final int ID) {
		switch(ID) {
		case ID_MAJOR: return MAJOR_TOR;
		case ID_PRESS:return PRESS_TOR;
		case ID_SWING:return SWING_TOR;
		}
		return null;
	}
	public IntegerProperty getAlarm(final int ID) {
		switch(ID) {
		case ID_MAJOR: return MAJOR_ALM;
		case ID_PRESS:return PRESS_ALM;
		case ID_SWING:return SWING_ALM;
		}
		return null;
	}
	public StringProperty getAlarmText(final int ID) {
		switch(ID) {
		case ID_MAJOR: return ALM_TXT[0];
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
		case ID_MAJOR:
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
		case ID_MAJOR:
			//major axis always counter-clockwise!!!
			asyncBreakIn(()->SDE_kickoff(SPIN,false));
			break;
		case ID_PRESS:
		case ID_SWING:
			asyncBreakIn(()->SDA_kickoff(ID,SPIN,CLOCKWISE));
			break;
		case ID_OTHER:
			asyncBreakIn(()->{
				SDA_kickoff(ID_PRESS,SPIN,CLOCKWISE);
				//block_delay(100);//???why???
				SDA_kickoff(ID_SWING,SPIN,CLOCKWISE);
			});
			break;
		}
	}
	public void kickoff(
		final int ID,
		final boolean SPIN
	) {
		kickoff(ID,SPIN,true);
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
	
	public void set_FA231(final float val) {asyncBreakIn(()->{
		writeVal(ID_FA231, 0, (int)(val*10.f));
	});}
	
	
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
		}
	}
	
	public Node gen_console() {
		
		//速度 PV, 速度 SV, 寸進 SV, 迴圈 SV
		final Label[] txt = new Label[15];
		for(int i=0; i<txt.length; i++) {
			Label obj = new Label();
			obj.getStyleClass().addAll("font-size5");
			obj.setMinWidth(100.);
			txt[i] = obj;
		}
		
		txt[0].textProperty().bind(PV_MAJOR_RPM.asString("%4d"));
		txt[1].textProperty().bind(SV_MAJOR_RPM1.asString("%4d"));
		txt[2].textProperty().bind(SV_MAJOR_MOV1.asString("%4d"));
		txt[3].textProperty().bind(SV_MAJOR_MOV2.asString("%4d"));
		
		txt[1].setOnMouseClicked(e->set_rpm_value(ID_MAJOR,txt[1]));
		txt[2].setOnMouseClicked(e->set_pos1_value(ID_MAJOR,txt[2]));
		txt[3].setOnMouseClicked(e->set_pos2_value(ID_MAJOR,txt[3]));
		
		txt[4].textProperty().bind(PV_PRESS_RPM.asString("%4d"));
		txt[5].textProperty().bind(SV_PRESS_RPM1.asString("%4d"));
		txt[6].textProperty().bind(SV_PRESS_MOV1.asString("%4d"));
		txt[7].textProperty().bind(SV_PRESS_MOV2.asString("%4d"));

		txt[5].setOnMouseClicked(e->set_rpm_value(ID_PRESS,txt[5]));
		txt[6].setOnMouseClicked(e->set_pos1_value(ID_PRESS,txt[6]));
		txt[7].setOnMouseClicked(e->set_pos2_value(ID_PRESS,txt[7]));
		
		txt[8].textProperty().bind(PV_SWING_RPM.asString("%4d"));
		txt[9].textProperty().bind(SV_SWING_RPM1.asString("%4d"));
		txt[10].textProperty().bind(SV_SWING_MOV1.asString("%4d"));
		txt[11].textProperty().bind(SV_SWING_MOV2.asString("%4d"));

		txt[9].setOnMouseClicked(e->set_rpm_value(ID_SWING,txt[9]));
		txt[10].setOnMouseClicked(e->set_pos1_value(ID_SWING,txt[10]));
		txt[11].setOnMouseClicked(e->set_pos2_value(ID_SWING,txt[11]));
		
		//主軸,加壓,擺動
		final JFXButton[] btn = {
			new JFXButton("旋轉"), new JFXButton("停止"), new JFXButton("寸進"), new JFXButton("迴圈"),
			new JFXButton("旋轉"), new JFXButton("停止"), new JFXButton("寸進"), new JFXButton("迴圈"),
			new JFXButton("旋轉"), new JFXButton("停止"), new JFXButton("寸進"), new JFXButton("迴圈"),
			
			new JFXButton("同步迴圈"),
			new JFXButton("同步旋轉"),
			new JFXButton("同步停止"),
		};
		for(int i=0; i<btn.length; i++) {
			btn[i].setMaxWidth(Double.MAX_VALUE);
		}
		
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setOnAction(e->kickoff(ID_MAJOR,true));
		btn[1].getStyleClass().add("btn-raised-0");
		btn[1].setOnAction(e->kickoff(ID_MAJOR,false));
		btn[2].getStyleClass().add("btn-raised-2");
		//btn[3].setDisable(true);
		btn[3].setOnAction(e->asyncBreakIn(()->SDE_move(1)));
		btn[3].getStyleClass().add("btn-raised-2");
		//btn[3].setDisable(false);
		btn[3].setOnAction(e->asyncBreakIn(()->SDE_move(2)));

		
		btn[4].getStyleClass().add("btn-raised-1");
		btn[4].setOnAction(e->kickoff(ID_PRESS,true));
		btn[5].getStyleClass().add("btn-raised-0");
		btn[5].setOnAction(e->kickoff(ID_PRESS,false));
		btn[6].getStyleClass().add("btn-raised-2");
		btn[6].setOnAction(e->asyncBreakIn(()->SDA_move(ID_PRESS,1)));
		btn[7].getStyleClass().add("btn-raised-2");
		btn[7].setOnAction(e->asyncBreakIn(()->SDA_move(ID_PRESS,2)));
		
		btn[8].getStyleClass().add("btn-raised-1");
		btn[8].setOnAction(e->kickoff(ID_SWING,true));
		btn[9].getStyleClass().add("btn-raised-0");
		btn[9].setOnAction(e->kickoff(ID_SWING,false));
		btn[10].getStyleClass().add("btn-raised-2");
		btn[10].setOnAction(e->asyncBreakIn(()->SDA_move(ID_SWING,1)));
		btn[11].getStyleClass().add("btn-raised-2");
		btn[11].setOnAction(e->asyncBreakIn(()->SDA_move(ID_SWING,2)));
		
		btn[12].getStyleClass().add("btn-raised-3");
		btn[12].setOnAction(e->{asyncBreakIn(()->{
			SDA_move(ID_SWING,2);
			SDA_move(ID_PRESS,2);
		});});
		
		btn[13].getStyleClass().add("btn-raised-1");
		btn[13].setOnAction(e->kickoff(ID_OTHER,true));
		btn[14].getStyleClass().add("btn-raised-0");
		btn[14].setOnAction(e->kickoff(ID_OTHER,false));
		
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad","box-border");
		lay0.addColumn(0, 
			new Label("名稱"),
			new Label("速度 PV"),
			new Label("速度 SV")
		);
		lay0.addColumn(1, 
			new Label("主軸"),
			txt[0],txt[1],btn[0],btn[1]
		);
		lay0.addColumn(2, 
			new Label("加壓"),
			txt[4],txt[5],btn[4],btn[5]
		);
		lay0.addColumn(3, 
			new Label("擺動"),
			txt[8],txt[9],btn[8],btn[9]
		);
		lay0.add(new Separator(), 0, 5, 4, 1);
		lay0.addColumn(0, 
			new Label("寸進 SV"),
			new Label("迴圈 SV")
		);
		lay0.addColumn(1,txt[2],txt[3],btn[2],btn[3]);//主軸
		lay0.addColumn(2,txt[6],txt[7],btn[6],btn[7]);//擺動
		lay0.addColumn(3,txt[10],txt[11],btn[10],btn[11]);//加壓
		lay0.add(new Separator(), 0, 10, 4, 1);
		lay0.add(btn[12], 2, 11, 2, 1);
		lay0.add(btn[13], 2, 12, 2, 1);
		lay0.add(btn[14], 2, 13, 2, 1);
		return lay0;
	}
	
	private String id_to_name(final int ID) {
		switch(ID) {
		case ID_MAJOR: return "主軸";	
		case ID_PRESS: return "加壓";
		case ID_SWING: return "擺動";
		default: return "???";
		}
	}
	
	private void set_pos_value(
		final int pos,
		final int ID,
		final Label txt_val
	) {
		String appx;
		switch(pos) {
		case 1: appx = "寸進脈波數"; break;
		case 2: appx = "迴圈脈波數"; break;
		default: return;
		}
		PadTouch pad = new PadTouch(
			'I',
			id_to_name(ID)+appx
		);
		Optional<String> opt = pad.showAndWait();			
		if(opt.isPresent()==false) {
			return;
		}
		int val = Integer.valueOf(opt.get());
		switch(ID) {
		case ID_MAJOR:
			break;
		case ID_PRESS:
		case ID_SWING:
			asyncBreakIn(()->SDA_revolution(ID,pos,val));
			break;
		}
	}
	private void set_pos1_value(
		final int ID,
		final Label txt_val
	) {
		set_pos_value(1,ID,txt_val);
	}
	private void set_pos2_value(
		final int ID,
		final Label txt_val
	) {
		set_pos_value(2,ID,txt_val);
	}
	
	private void set_rpm_value(
		final int ID,
		final Label txt_val
	) {
		PadTouch pad = new PadTouch(
			'I',
			id_to_name(ID)+"RPM"
		);
		Optional<String> opt = pad.showAndWait();			
		if(opt.isPresent()==false) {
			return;
		}
		int val = Integer.valueOf(opt.get());
		if(val>=41) {
			return;
		}
		switch(ID) {
		case ID_MAJOR: 
			asyncBreakIn(()->SDE_speed(val));
			break;
		case ID_PRESS:
		case ID_SWING:
			asyncBreakIn(()->SDA_speed(ID,val));
			break;
		}
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







