package prj.sputter;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PadTouch;
import narl.itrc.PanBase;

/**
 * SPIK2000 is a high voltage pulse generator.<p>
 * This code is for device communication.<p>
 * Although manual say RS-232 protocol is RK512, it not real.<p>
 * I guess someone change the internal controller.<p>
 * Reference Document is "s7300_cp341_manual.pdf".<p>
 * @author qq
 *
 */
public class DevSPIK2k extends DevTTY {

	public DevSPIK2k() {
		TAG = "SPIK2k";
	}	
	@Override
	public void afterOpen() {
		//load default values
		asyncGetRegister(tkn->{			
			if(is_valid(tkn)==false) { return; }
			Ton_pos.set(tkn.values[0]);
			Tof_pos.set(tkn.values[1]);
			Ton_neg.set(tkn.values[2]);
			Tof_neg.set(tkn.values[3]);
		}, 4 ,4);
		//load initial state
		asyncGetRegister(tkn->{			
			if(is_valid(tkn)==false) {	return; }
			final int sta = tkn.values[0];
			Run.set((sta & 0x0002)!=0);
			DC1.set((sta & 0x0040)!=0);
			DC2.set((sta & 0x0080)!=0);
		}, 1, 1);
		//load initial DC1 & DC2 setting
		asyncGetRegister(tkn->{			
			if(is_valid(tkn)==false) {	return; }
			DC1_V_Set.set(tkn.values[0]);
			DC1_I_Set.set(tkn.values[1]);
			DC1_P_Set.set(tkn.values[2]);
			DC2_V_Set.set(tkn.values[3]);
			DC2_I_Set.set(tkn.values[4]);
			DC2_P_Set.set(tkn.values[5]);
		}, 13, 6);
		//apply DC1 and DC2 settings~~~
		//asyncSetRegister(tkn->{			
		//	if(is_valid(tkn)==false) { 	return; }			
		//}, 30, 0, 0);//DC1_CFG: 0=activated, 1=analog, 2=RS232
		
		addState("listener", listener);
		addState("transmit", transmit);
		playFlow("listener");
	}
	@Override
	public void beforeClose() {
	}
	
	private boolean is_valid(final Token tkn) {
		if(tkn.response==null){
			Misc.loge("[%s] after-get no, respose!!", TAG);
			return false;
		}
		if(tkn.response[2]!=0 || tkn.response[3]!=0){
			//we got AD pack, why ???
			Misc.logw("Not empty answer");
			Misc.dump_byte(tkn.response);
			return false;
		}
		return true;
	}
	//----------------------------------//

	public final BooleanProperty Run = new SimpleBooleanProperty(false);//1* Run: 1->off, 2->on 
	public final BooleanProperty DC1 = new SimpleBooleanProperty(false);
	public final BooleanProperty DC2 = new SimpleBooleanProperty(false);
	/**
	 * 0* Operation Mode:         writing
	 *   0x???1: Bipolar          0x00 - do nothing
	 *   0x???2: Unipolar neg     0x01 - set bipolar
	 *   0x???3: Unipolar pos     0x02 - set Unipolar neg
	 *   0x???4: DC neg(??)       0x03 - set Unipolar pos 
	 *   0x???5: DC pos           0x04 - set DC neg mode 
	 *   0x??0?: Multiplex OFF    0x05 - set DC pos mode 
	 *   0x??1?: Multiplex ON     0x10 - set multiplex off
	 *   0x?0??: 1us              0x11 - set multiplex on
	 *   Bit15 : permanent High   
	 */
	public final IntegerProperty Mode= new SimpleIntegerProperty(0);//addr:0, BL:0
	
	/**
	 * 1* Operation State:              writing
	 *   Bit15:High  Bit_7:DC2 ON         0x00 - do nothing
	 *   Bit14:      Bit_6:DC1 ON         0x01 - Running OFF
	 *   Bit13:      Bit_5:               0x02 - Running ON
	 *   Bit12:      Bit_4:               0x03 - Clear error
	 *   Bit11:      Bit_3:ARC Delay ON   0x10 - save CFG to EEPROM
	 *   Bit10:      Bit_2:Ready          0x20 - DC1 OFF
	 *   Bit_9:CFG   Bit_1:Running ON     0x21 - DC1 ON
	 *   Bit_8:CFG   Bit_0:error          0x22 - DC2 OFF
	 *                                    0x23 - DC2 ON
	 */
	public final IntegerProperty State= new SimpleIntegerProperty(0);//addr:1, BL:1
	
	/**
	 * 2* Communication-Master:
	 *   0: serial 1
	 *   1: serial 2
	 *   2: HMS-module
	 *   3: Dualport-RAM 2
	 */
	public final IntegerProperty ComState= new SimpleIntegerProperty(0);//addr:2, BL:2
	
	/**
	 * 3* Error Status:
	 *   Bit15:Watchdog reset   Bit_7:Rack-Temp
	 *   Bit14:Address Reset    Bit_6:Arc Overflow
	 *   Bit13:Config too long  Bit_5:Arc-
	 *   Bit12:DC2 Error        Bit_4:Arc+
	 *   Bit11:DC1 Error        Bit_3:Driver 2R
	 *   Bit10:Interlock        Bit_2:Driver 2L
	 *   Bit_9:Heat-Sink2       Bit_1:Driver 1R
	 *   Bit_8:Heat-Sink1       Bit_0:Driver 1L
	 */
	public final IntegerProperty Error = new SimpleIntegerProperty(0);//addr:3, BL:1
	
	public final IntegerProperty Ton_pos = new SimpleIntegerProperty(-1);//4* Ton +: 2-32000us, duration of the pulse
	public final IntegerProperty Tof_pos = new SimpleIntegerProperty(-1);//5* Toff+: 2-32000us, duration of the pause
	public final IntegerProperty Ton_neg = new SimpleIntegerProperty(-1);//6* Ton -: 2-32000us, duration of the pulse
	public final IntegerProperty Tof_neg = new SimpleIntegerProperty(-1);//7* Toff-: 2-32000us, duration of the pause
	
	public final IntegerProperty ARC_count = new SimpleIntegerProperty();//0-10000
	public final IntegerProperty DC1_V_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC1_I_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC1_P_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_V_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_I_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_P_Act = new SimpleIntegerProperty();//0-4000
	
	public final IntegerProperty DC1_V_Set = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC1_I_Set = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC1_P_Set = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_V_Set = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_I_Set = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_P_Set = new SimpleIntegerProperty();//0-4000
	
	private Runnable listener = ()->{
		final SerialPort dev = port.get();
		if(dev.isOpened()==false) {
			stopFlow();
			return;
		}
		try {
			//Misc.loge("[%s] listener",TAG);
			nextState("listener");
			byte cc = dev.readBytes(1,30)[0];//wait STX code....
			if(cc==STX) {
				byte[] buf = protocol_3964R_listen(dev);
				if(buf.length==0){
					return;
				}
				final Token tkn = new Token(buf);//unpack AD package
				if(tkn.address==19 && tkn.count==7) {
					Application.invokeLater(()->{
						ARC_count.set(tkn.values[0]);
						DC1_V_Act.set(tkn.values[1]);
						DC1_I_Act.set(tkn.values[2]);
						DC1_P_Act.set(tkn.values[3]);
						DC2_V_Act.set(tkn.values[4]);
						DC2_I_Act.set(tkn.values[5]);
						DC2_P_Act.set(tkn.values[6]);						
					});
				}
			}else {
				Misc.logw("[%s] listener - response error(%d)", TAG, cc);
			}	
						
		}catch(SerialPortTimeoutException e1) {
			//device no response, check whether host has packages.
			//Misc.loge("[%s] listen - %s", TAG, e1.getMessage());
			nextState("transmit");
		} catch (SerialPortException e2) {			
			Misc.loge("[%s] TTY FAIL - %s", TAG, e2.getMessage());
			stopFlow();
		}
	};
	
	public static interface TokenNotify {
		void token_notify(final Token tkn);
	};
	public static class Token {
		public final int address;
		public final int count;
		public int[] values;//如果是 null, 就是讀取資料
		public byte[] response;
		TokenNotify event;
		Token(final int addr, final int cnt){
			address= addr;
			count  = cnt;
			values = null;
		}
		Token(final int addr, final int[] val){
			address= addr;
			count  = val.length;
			values = val;
		}
		Token(final byte[] buf){
			response = buf;
			if(buf.length==0){
				address = -1;
				count = 0;
				values= null;
				notify_event();
				return;
			}
			if(buf[3]=='D' && buf.length>=10) {
				address= byte2int(buf[4],buf[5]);
				//count  = byte2int(buf[6],buf[7]);//???				
				if(buf[2]=='A') {
					count = (buf.length - 10 - 3)/2;
					unpack(10);
				}else {
					count = 0;
					values= null;
				}
			}else {
				address= -1;
				count  = 0;
				values = null;
			}		
		}
		int byte2int(final byte aa, final byte bb) {
			final int _a = (int)aa;
			final int _b = (int)bb;
			return ((_a&0x00FF)<<8) | (_b&0x00FF);
		}		
		void unpack() {
			if(response.length<=7){
				return;
			}
			if(response[2]!=0 || response[3]!=0){
				values= new int[0];
				Misc.logw("[Token] unpack non-empty package");
				Misc.dump_byte(response);				
				return;
			}				
			unpack(4);
		}
		void unpack(final int off) {
			if(values!=null) {
				return;
			}
			if(response.length<=7){
				return;
			}
			values = new int[count];
			for(int i=0; i<count; i++) {
				byte aa = response[off+0+i*2];
				byte bb = response[off+1+i*2];
				values[i] = byte2int(aa,bb);
			}
		}
		void notify_event() {
			if(event==null) { 
				return;
			}
			if(Application.isEventThread()==true) {
				event.token_notify(this);
			}else {
				Application.invokeLater(()->event.token_notify(this));
			}
		}
	};
	
	private final ConcurrentLinkedQueue<Token> tkn_queue = new ConcurrentLinkedQueue<Token>();
	
	private Runnable transmit = ()->{
		final SerialPort dev = port.get();
		if(dev.isOpened()==false) {
			stopFlow();
			return;
		}
		nextState("transmit");
		final Token tkn = tkn_queue.poll();
		if(tkn==null) {
			nextState("listener");
			return;
		}
		
		try {
			Misc.loge("[%s] transmit",TAG);
			
			final byte[] pkg = RK512_package(
				tkn.address, 
				tkn.count, 
				tkn.values
			);
			dev.writeByte(STX);//host want communication~~~
			if(protocol_3964R_express(dev,pkg)!=0){
				Misc.logw("[%s] lost package (addr:%d, size:%d)", TAG, tkn.address, tkn.count);
				return;
			}
			//wait STX code~~~~~
			//it must wait at least 3 second!!!....
			byte cc = dev.readBytes(1,3000)[0];
			if(cc==STX) {
				tkn.response = protocol_3964R_listen(dev);
				tkn.unpack();
				tkn.notify_event();
			}else {
				cc = dev.readBytes(1,500)[0];
				Misc.logw("[%s] transmit - response error(%d)", TAG, cc);
			}			
		} catch (SerialPortTimeoutException e1) {			
			Misc.logw("[%s] transmit timeout!!", TAG);
		} catch (SerialPortException e2) {			
			Misc.loge("[%s] transmit FAIL - %s", TAG, e2.getMessage());
		}		
	};
	
	public void asyncSetRegister(TokenNotify event, final int addr, int... vals) {
		Token tkn = new Token(addr,vals);
		tkn.event = event;//e->{
			//buf[0:2]--> token
			//buf[  3]--> error code
			//buf[  4]--> DLE
			//buf[  5]--> ETX
			//buf[  6]--> checksum
			//byte cc = e.response[0];
		//};
		tkn_queue.add(tkn);
	}
	public void asyncGetRegister(
		final TokenNotify event, 
		final int addr, 
		final int size
	) {
		Token tkn = new Token(addr,size);
		tkn.event = event;/*tkn->{
			String txt = "addr="+tkn.address+", vals= {\n";
			for(int v:tkn.values) {
				txt = txt + v + ",\n";
			}
			System.out.println(txt+"}\n");
		};*/
		tkn_queue.add(tkn);
	} 
	
	public void setRunning(final boolean flg) {
		asyncSetRegister(tkn->{
			Run.set(flg);
		},1,(flg==true)?(0x02):(0x01));
	}
	public void setDC1(final boolean flg) {
		asyncSetRegister(tkn->{
			DC1.set(flg);
		},1,(flg==true)?(0x21):(0x20));
	}
	public void setDC2(final boolean flg) {
		asyncSetRegister(tkn->{
			DC2.set(flg);
		},1,(flg==true)?(0x23):(0x22));
	}
	public void setAllOnOff(boolean run, boolean dc1, boolean dc2) {
		setRunning(run);
		setDC1(dc1);
		setDC2(dc2);
	}
	
	//device setting values
	//4* Ton +: 2-32000us, duration of the pulse
	//5* Toff+: 2-32000us, duration of the pause
	//6* Ton -: 2-32000us, duration of the pulse
	//7* Toff-: 2-32000us, duration of the pause
	public void set_Ton_P(final int us) {
		asyncSetRegister(null,4,us);
	}
	public void set_Toff_P(final int us) {
		asyncSetRegister(null,5,us);
	}
	public void set_Ton_N(final int us) {
		asyncSetRegister(null,6,us);
	}
	public void set_Toff_N(final int us) {
		asyncSetRegister(null,7,us);
	}
	
	public void set_T_pos(final int on_us,final int off_us) {
		asyncSetRegister(null,4,on_us,off_us);
	}
	public void set_T_neg(final int on_us,final int off_us) {
		asyncSetRegister(null,6,on_us,off_us);
	}
	
	public void set_pulse(
		final int Ton_P_us,
		final int Toff_P_us,
		final int Ton_N_us,
		final int Toff_N_us
	) {
		asyncSetRegister(null,4,Ton_P_us,Toff_P_us,Ton_N_us,Toff_N_us);
	}
	
	public void set_DC1_V(final int val) {
		if(val>4000) { Misc.logw("[%s] invalid DC1_V value", TAG); return; }
		asyncSetRegister(null,13,val);
	}
	public void set_DC1_I(final int val) {
		if(val>4000) { Misc.logw("[%s] invalid DC1_I value", TAG); return; }
		asyncSetRegister(null,14,val);
	}
	public void set_DC1_P(final int val) {
		if(val>4000) { Misc.logw("[%s] invalid DC1_P value", TAG); return; }
		asyncSetRegister(null,15,val);
	}
	public void set_DC2_V(final int val) {
		if(val>4000) { Misc.logw("[%s] invalid DC2_V value", TAG); return; }
		asyncSetRegister(null,16,val);
	}
	public void set_DC2_I(final int val) {
		if(val>4000) { Misc.logw("[%s] invalid DC2_I value", TAG); return; }
		asyncSetRegister(null,17,val);
	}
	public void set_DC2_P(final int val) {
		if(val>4000) { Misc.logw("[%s] invalid DC2_P value", TAG); return; }
		asyncSetRegister(null,18,val);
	}
	
	private void show_edit_pulse(
		final String title,
		final IntegerProperty prop,
		final String c_text,
		final int address
	) {		
		final TextInputDialog dd1 = new TextInputDialog(""+prop.get());
		dd1.setTitle("設定 "+title);
		dd1.setContentText(c_text);		
		Optional<String> res = dd1.showAndWait();
		if(res.isPresent()==false) {
			return;
		}
		try {
			final int value = Integer.valueOf(res.get());
			asyncSetRegister(tkn->{
				if(tkn.response[3]!=0) {					
				}
				Application.invokeLater(()->prop.set(value));
			}, address, value);			
		}catch(NumberFormatException exp) {
			final Alert dia = new Alert(AlertType.ERROR);
			dia.setTitle("錯誤！！");
			dia.setHeaderText("輸入必須為整數");
			dia.showAndWait();
		}
	}
	public void show_Ton_pos() { show_edit_pulse("Ton+" , Ton_pos, "時間(us)", 4); }
	public void show_Tof_pos() { show_edit_pulse("Toff+", Tof_pos, "時間(us)", 5); }
	public void show_Ton_neg() { show_edit_pulse("Ton-" , Ton_neg, "時間(us)", 6); }
	public void show_Tof_neg() { show_edit_pulse("Toff-", Tof_neg, "時間(us)", 7); }
	
	public void show_DC1_V() { show_edit_pulse("DC1電壓" , DC1_V_Set, "電壓(Volt)", 13); }
	public void show_DC1_I() { show_edit_pulse("DC1電流" , DC1_I_Set, "電流(Amp )", 14); }
	public void show_DC1_P() { show_edit_pulse("DC1功率" , DC1_P_Set, "功率(Watt)", 15); }
	
	public void show_DC2_V() { show_edit_pulse("DC2電壓" , DC2_V_Set, "電壓(Volt)", 16); }
	public void show_DC2_I() { show_edit_pulse("DC2電流" , DC2_I_Set, "電流(Amp )", 17); }
	public void show_DC2_P() { show_edit_pulse("DC2功率" , DC2_P_Set, "功率(Watt)", 18); }
	//---------------------------------//
		
	private static void show_error(Token tkn) {
		if(tkn.response[3]==0) {
			return;
		}
		String txt = String.format(
			"Addr:%d+%d, Error:%d",
			tkn.address,tkn.count,(int)tkn.response[3]
		);
		Misc.logv("[SPIK2000] %s",txt);
		//error code
		final Alert dia = new Alert(AlertType.ERROR);
		dia.setTitle("錯誤！！");
		dia.setHeaderText(txt);
		dia.showAndWait();
	}
	//-----------------------------------------
	
	public static Pane genInfoPanel(final DevSPIK2k dev) {
		
		Label t_on_pos= new Label();
		t_on_pos.textProperty().bind(dev.Ton_pos.asString("%3d"));
		t_on_pos.setOnMouseClicked(e->dev.show_Ton_pos());
		
		Label t_of_pos = new Label();
		t_of_pos.textProperty().bind(dev.Tof_pos.asString("%3d"));
		t_of_pos.setOnMouseClicked(e->dev.show_Tof_pos());
		
		Label t_on_neg = new Label();
		t_on_neg.textProperty().bind(dev.Ton_neg.asString("%3d"));
		t_on_neg.setOnMouseClicked(e->dev.show_Ton_neg());
		
		Label t_of_neg = new Label();
		t_of_neg.textProperty().bind(dev.Tof_neg.asString("%3d"));
		t_of_neg.setOnMouseClicked(e->dev.show_Tof_neg());
		
		//---------------------------------------
		
		Label t_vol_pv1 = new Label();
		t_vol_pv1.textProperty().bind(dev.DC1_V_Act.asString("%4d"));
		t_vol_pv1.setOnMouseClicked(e->dev.show_DC1_V());
		
		Label t_amp_pv1 = new Label();
		t_amp_pv1.textProperty().bind(dev.DC1_I_Act.asString("%4d"));
		t_amp_pv1.setOnMouseClicked(e->dev.show_DC1_I());
		
		Label t_pow_pv1 = new Label();
		t_pow_pv1.textProperty().bind(dev.DC1_P_Set.asString("%4d"));
		t_pow_pv1.setOnMouseClicked(e->dev.show_DC1_P());

		Label t_vol_sv1 = new Label();
		t_vol_sv1.textProperty().bind(dev.DC1_V_Set.asString("%4d"));
		t_vol_sv1.setOnMouseClicked(e->dev.show_DC1_V());
		
		Label t_amp_sv1 = new Label();
		t_amp_sv1.textProperty().bind(dev.DC1_I_Set.asString("%4d"));
		t_amp_sv1.setOnMouseClicked(e->dev.show_DC1_I());
		
		Label t_pow_sv1 = new Label();
		t_pow_sv1.textProperty().bind(dev.DC1_P_Set.asString("%4d"));
		t_pow_sv1.setOnMouseClicked(e->dev.show_DC1_P());		
		//---------------------------------------
		
		Button act_on_off = new Button("設定開關");
		act_on_off.setMaxWidth(Double.MAX_VALUE);
		act_on_off.setOnAction(e->{
			
			JFXCheckBox chk_run = new JFXCheckBox("Run");
			chk_run.setSelected(dev.Run.get());
			
			JFXCheckBox chk_dc1 = new JFXCheckBox("DC-1");
			chk_dc1.setSelected(dev.DC1.get());
			
			JFXCheckBox chk_dc2 = new JFXCheckBox("DC-2");
			chk_dc2.setSelected(dev.DC2.get());
			
			VBox lay0 = new VBox(chk_run,chk_dc1,chk_dc2);
			lay0.setSpacing(13);
			
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle("設定開關");
			dia.setHeaderText("確認開關設定");
			dia.getDialogPane().setContent(lay0);
			if(dia.showAndWait().get()==ButtonType.OK) {
				dev.setAllOnOff(chk_run.isSelected(), chk_dc1.isSelected(), chk_dc2.isSelected());	
			}			
		});
		//---------------------------------------
		Label txt_pv = new Label("PV");
		Label txt_sv = new Label("SV");
		Label[] txt = {
			t_on_pos, t_of_pos, 
			t_on_neg, t_of_neg,
			txt_pv, txt_sv,
			t_vol_pv1, t_vol_sv1, 
			t_amp_pv1, t_amp_sv1, 
			t_pow_pv1, t_pow_sv1,			
		};
		for(Label obj:txt) {
			obj.getStyleClass().addAll("font-size5");
			obj.setMinWidth(67.);
			obj.setMaxWidth(Double.MAX_VALUE);
			obj.setAlignment(Pos.CENTER_RIGHT);
			GridPane.setHgrow(obj, Priority.ALWAYS);
		}
		//--------------------------------------------
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-box");
		lay1.addRow(0, new Label("DC-1"), txt_pv, txt_sv);
		lay1.addRow(1, new Label("電壓:"), t_vol_pv1, t_vol_sv1);
		lay1.addRow(2, new Label("電流:"), t_amp_pv1, t_amp_sv1);
		lay1.addRow(3, new Label("功率:"), t_pow_pv1, t_pow_sv1);
		
		final HBox lay2 = new HBox(
			PanBase.genIndicator("Run", dev.Run),
			PanBase.genIndicator("DC-1", dev.DC1),
			PanBase.genIndicator("DC-2", dev.DC2)
		);
		lay2.getStyleClass().addAll("box-gap");
			
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad");
		lay0.addRow(0, new Label("Ton+ :"), t_on_pos, new Label("Toff+:"), t_of_pos);
		lay0.addRow(1, new Label("Ton- :"), t_on_neg, new Label("Toff-:"), t_of_neg);
		lay0.add(lay1, 0, 2, 4, 4);
		lay0.add(lay2, 0, 6, 4, 1);
		lay0.add(act_on_off, 0, 7, 4, 1);
		return lay0;
	}
	
	public static Pane genPanel(final DevSPIK2k dev) {
		
		final ToggleGroup grp = new ToggleGroup();
		
		final JFXRadioButton[] rad = {
			new JFXRadioButton ("Bipolar"),
			new JFXRadioButton ("Uni-"),
			new JFXRadioButton ("Uni+"),
			new JFXRadioButton ("DC-"),
			new JFXRadioButton ("DC+"),
		};
		for(int i=0; i<rad.length; i++) {
			rad[i].setToggleGroup(grp);
			rad[i].setUserData(i+1);
			rad[i].setOnAction(e->{
				dev.asyncSetRegister(
					tkn->show_error(tkn),
					0,
					(int)grp.getSelectedToggle().getUserData()
				);
			});
		}
		
		final JFXButton[] btn = new JFXButton[6];
		for(int i=0; i<btn.length; i++) {
			btn[i] = new JFXButton();
			btn[i].setMaxWidth(Double.MAX_VALUE);
			if(i%2==0) {
				btn[i].getStyleClass().add("btn-raised-1");
				btn[i].setText("ON ");
			}else {
				btn[i].getStyleClass().add("btn-raised-3");
				btn[i].setText("OFF");
			}
			GridPane.setHgrow(btn[i], Priority.ALWAYS);
		}
		btn[0].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x21));//DC-1 on
		btn[1].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x20));//DC-1 off
		btn[2].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x23));//DC-2 on
		btn[3].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x22));//DC-2 off
		btn[4].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x02));//RUN on
		btn[5].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x01));//RUN off
		
		final Label[] txt = new Label[4];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
			txt[i].setMaxWidth(Double.MAX_VALUE);			
			GridPane.setHgrow(txt[i], Priority.ALWAYS);
		}
		txt[0].textProperty().bind(dev.Ton_pos.asString("Ton+ : %3d"));
		txt[0].setOnMouseClicked(e->dev.show_Ton_pos());
		txt[1].textProperty().bind(dev.Tof_pos.asString("Toff+: %3d"));
		txt[1].setOnMouseClicked(e->dev.show_Tof_pos());
		txt[1].textProperty().bind(dev.Ton_neg.asString("Ton- : %3d"));
		txt[2].setOnMouseClicked(e->dev.show_Ton_neg());
		txt[2].textProperty().bind(dev.Tof_neg.asString("Toff-: %3d"));
		txt[3].setOnMouseClicked(e->dev.show_Tof_neg());

		JFXButton btn_test = new JFXButton();
		btn_test.setMaxWidth(Double.MAX_VALUE);
		btn_test.getStyleClass().add("btn-raised-1");
		//btn_test.setOnAction(e->{
		//	dev.asyncGetRegister(null,4, 4);
		//});
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad-inner");
		lay1.addRow(0, new Label("DC-1"),btn[0], btn[1]);
		lay1.addRow(1, new Label("DC-2"),btn[2], btn[3]);
		lay1.addRow(2, new Label("RUN"), btn[4], btn[5]);

		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad");
		lay0.add(new Label("操作模式"), 0, 0, 4, 1);
		lay0.addRow(1, rad[1], rad[3]);
		lay0.addRow(2, rad[2], rad[4]);
		lay0.addRow(3, rad[0]);
		lay0.add(new Separator(), 0, 4, 4, 1);
		lay0.add(txt[ 0], 0, 5, 4, 1);
		lay0.add(txt[ 1], 0, 6, 4, 1);
		lay0.add(txt[ 2], 0, 7, 4, 1);
		lay0.add(txt[ 3], 0, 8, 4, 1);		
		lay0.add(new Separator(), 0, 9, 4, 1);
		lay0.add(lay1, 0, 10, 4, 3);
		lay0.add(btn_test, 0, 14, 4, 1);	
		return lay0;
	}
}
