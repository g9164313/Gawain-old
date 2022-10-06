package prj.sputter;

import java.util.concurrent.ConcurrentLinkedQueue;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import jssc.SerialPort;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
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
@SuppressWarnings("restriction")
public class DevSPIK2k extends DevTTY {

	public DevSPIK2k() {
		TAG = "SPIK2k";
	}	
	@Override
	public void afterOpen() {
		//load initial mode, state, pulse setting~~
		asyncGetRegister(tkn->{			
			if(tkn.is_valid_ED()==false) {	
				return; 
			}
			apply_mode_flag(tkn.values[0]);
			apply_state_flag(tkn.values[1]);
			
			Ton_pos.set(tkn.values[4]);
			Tof_pos.set(tkn.values[5]);
			Ton_neg.set(tkn.values[6]);
			Tof_neg.set(tkn.values[7]);
			
			DC1_V_Set.set(tkn.values[13]);
			DC1_I_Set.set(tkn.values[14]);
			DC1_P_Set.set(tkn.values[15]);
			DC2_V_Set.set(tkn.values[16]);
			DC2_I_Set.set(tkn.values[17]);
			DC2_P_Set.set(tkn.values[18]);
		}, 0, 19);
		
		addState("listener", listener);
		addState("transmit", transmit);
		playFlow("listener");
	}
	@Override
	public void beforeClose() {
	}
	//----------------------------------//

	private Runnable listener = ()->{
		if(port.isPresent()==false) {
			Misc.logw("[%s] no TTY port", TAG);
			stopFlow();
			return;
		}
		final SerialPort dev = port.get();		
		nextState("transmit");
		if(protocol_3964R_wait(dev,30)!=0) {
			return;
		}
		byte[] pkg = protocol_3964R_listen(dev,0);
		if(pkg.length<10) {
			return;
		}
		final int addr= byte2int(pkg[4],pkg[5]);
		final int size= byte2int(pkg[6],pkg[7]);
		final int[] vals = new int[size];
		for(int i=0; i<size; i++) {
			final int aa = 10+i*2+0;
			final int bb = 10+i*2+1;
			if(bb>=pkg.length) {
				break;
			}
			vals[i] = byte2int(pkg[aa],pkg[bb]);
		}
		Application.invokeLater(()->fresh_property(addr,vals));
	};
	
	private final ConcurrentLinkedQueue<Token> tkn_queue = new ConcurrentLinkedQueue<Token>();
	
	private Runnable transmit = ()->{
		final SerialPort dev = port.get();
		nextState("listener");	
		final Token tkn = tkn_queue.poll();
		if(tkn==null) {
			return;
		}
		final byte[] pkg = RK512_package(
			tkn.address, 
			tkn.count, 
			tkn.values
		);			
		if(protocol_3964R_express(dev,pkg)!=0){
			tkn_queue.add(tkn);//re-assign again!!!
			return;
		}
		tkn.unpack_ED(protocol_3964R_listen(dev,(tkn.values==null)?(tkn.count):(0)));		
	};
	//----------------------------------//

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
	public final BooleanProperty ModeBipolar = new SimpleBooleanProperty(false);
	public final BooleanProperty ModeUnipolar_neg = new SimpleBooleanProperty(false);
	public final BooleanProperty ModeUnipolar_pos = new SimpleBooleanProperty(false);
	public final BooleanProperty ModeDC_neg = new SimpleBooleanProperty(false);
	public final BooleanProperty ModeDC_pos = new SimpleBooleanProperty(false);
	
	public final BooleanProperty ModeMultiplex_off = new SimpleBooleanProperty(false);
	public final BooleanProperty ModeMultiplex_on = new SimpleBooleanProperty(false);
	
	void apply_mode_flag(final int mode) {
		ModeBipolar.set(((mode&0xF)==1)?(true):(false));
		ModeUnipolar_neg.set(((mode&0xF)==2)?(true):(false));
		ModeUnipolar_pos.set(((mode&0xF)==3)?(true):(false));
		ModeDC_neg.set(((mode&0xF)==4)?(true):(false));
		ModeDC_pos.set(((mode&0xF)==5)?(true):(false));
		
		ModeMultiplex_off.set(((mode&0xF0)==0)?(true):(false));
		ModeMultiplex_on.set(((mode&0xF0)==1)?(true):(false));
		Misc.logv("[%s]MODE=%04X", TAG, mode);
	}

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
	public final BooleanProperty Run = new SimpleBooleanProperty(false);//1* Run: 1->off, 2->on 
	public final BooleanProperty DC1 = new SimpleBooleanProperty(false);
	public final BooleanProperty DC2 = new SimpleBooleanProperty(false);
	
	void apply_state_flag(final int stat) {
		Run.set((stat & 0x0002)!=0);
		DC1.set((stat & 0x0040)!=0);
		DC2.set((stat & 0x0080)!=0);
		Misc.logv("[%s]STAT=%04X", TAG, stat);
	}
	
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
	
	public final IntegerProperty ARC_pos = new SimpleIntegerProperty(-1);//8*
	public final IntegerProperty ARC_neg = new SimpleIntegerProperty(-1);//9*
	public final IntegerProperty ARC_delay = new SimpleIntegerProperty(-1);//10* delay
	public final IntegerProperty ARC_overflow = new SimpleIntegerProperty(-1);//11* overflow
	public final IntegerProperty ARC_interval = new SimpleIntegerProperty(-1);//12* interval??
	
	public final IntegerProperty DC1_V_Set = new SimpleIntegerProperty(-1);//13* in 1/4 (V)
	public final IntegerProperty DC1_I_Set = new SimpleIntegerProperty(-1);//14* in 1/1024(A)
	public final IntegerProperty DC1_P_Set = new SimpleIntegerProperty(60);//15* in 1/2 (W)
	public final IntegerProperty DC2_V_Set = new SimpleIntegerProperty(-1);//16* in 1/4 (V)
	public final IntegerProperty DC2_I_Set = new SimpleIntegerProperty(-1);//17* in 1/1024(A)
	public final IntegerProperty DC2_P_Set = new SimpleIntegerProperty(-1);//18* in 1/2 (W)
	
	public final IntegerProperty ARC_count = new SimpleIntegerProperty(-1);
	public final IntegerProperty DC1_V_Act = new SimpleIntegerProperty(-1);//in 1/4
	public final IntegerProperty DC1_I_Act = new SimpleIntegerProperty(-1);//in 1/1024 ?
	public final IntegerProperty DC1_P_Act = new SimpleIntegerProperty(-1);//in 1/2
	public final IntegerProperty DC2_V_Act = new SimpleIntegerProperty(-1);//in 1/4
	public final IntegerProperty DC2_I_Act = new SimpleIntegerProperty(-1);//in 1/1024 ? 
	public final IntegerProperty DC2_P_Act = new SimpleIntegerProperty(-1);//in 1/2

	private void fresh_property(final int addr, final int[] vals) {
		for(int i=0; i<vals.length; i++) {
			switch(addr+i) {
			case  0:
				apply_mode_flag(vals[i]);
				break;
			case 1:
				apply_state_flag(vals[i]);
				break;
			case 2:
				break;
			case 3:
				Error.set(vals[i]);
				break;
			case  4: Ton_pos.set(vals[i]); break;
			case  5: Tof_pos.set(vals[i]); break;
			case  6: Ton_neg.set(vals[i]); break;
			case  7: Tof_neg.set(vals[i]); break;
			case  8: ARC_pos.set(vals[i]); break;
			case  9: ARC_neg.set(vals[i]); break;
			case 10: ARC_delay.set(vals[i]); break;
			case 11: ARC_overflow.set(vals[i]); break;
			case 12: ARC_interval.set(vals[i]); break;
			case 13: DC1_V_Set.set(vals[i]); break;
			case 14: DC1_I_Set.set(vals[i]); break;
			case 15: DC1_P_Set.set(vals[i]); break;
			case 16: DC2_V_Set.set(vals[i]); break;
			case 17: DC2_I_Set.set(vals[i]); break;
			case 18: DC2_P_Set.set(vals[i]); break;				
			case 19: ARC_count.set(vals[i]); break;
			case 20: DC1_V_Act.set(vals[i]); break;
			case 21: DC1_I_Act.set(vals[i]); break;
			case 22: DC1_P_Act.set(vals[i]); break;
			case 23: DC2_V_Act.set(vals[i]); break;
			case 24: DC2_I_Act.set(vals[i]); break;
			case 25: DC2_P_Act.set(vals[i]); break;
			case 26: break;//1000
			case 27: break;//35
			case 28: break;//20
			case 29: break;//5000
			case 30: break;//1
			case 31: break;//1
			case 32: break;//2047
			case 33: break;//2047
			case 34: break;//2047
			case 35: break;//0
			case 36: break;//2047
			case 37: break;//2047
			case 38: break;//2047
			case 39: break;//0
			case 40: break;//2047
			case 41: break;//2047
			case 42: break;//2047
			case 43: break;//0
			case 44: break;//2047
			case 45: break;//2047
			case 46: break;//2047
			default: 
				Misc.logv("[%s] miss address: %d, value=%d",TAG, addr+i, vals[i]); 
				break;
			}
		}
	}
	
	private static int byte2int(final byte aa, final byte bb) {
		final int _a = (int)aa;
		final int _b = (int)bb;
		return ((_a&0x00FF)<<8) | (_b&0x00FF);
	}
	public static interface TokenNotify {
		void event_notify(final Token tkn);
	};
	
	public static class Token {
		public final int address;
		public final int count;
		public int[] values;
		public byte[] payload;
		TokenNotify event;
		
		Token(final int addr, final int cnt, final TokenNotify e){
			address= addr;
			count  = cnt;
			values = null;//讀取資料
			event = e;
		}
		Token(final int addr, final int[] val, final TokenNotify e){
			address= addr;
			count  = val.length;
			values = val;//寫入資料
			event = e;
		}
		void unpack_ED(final byte[] pkg) {
			//pkg[0:2]--> token
			//pkg[  3]--> error code
			//pkg[...]--> Data values(2 byte)
			//pkg[ -3]--> DLE (forgot this!!!)
			//pkg[ -2]--> ETX (forgot this!!!)
			//pkg[ -1]--> checksum (forgot this!!!)
			payload = pkg;
			if(values==null) {
				values = new int[count];
				for(int i=0; i<count; i++) {
					final int aa = 4+i*2+0;
					final int bb = 4+i*2+1;
					if(bb>=payload.length) {
						Misc.logw("invalid ED response");
						Misc.dump_byte(pkg);
						break;
					}
					values[i] = byte2int(payload[aa], payload[bb]);
				}
			}		
			if(event==null || is_valid_ED()==false) { 
				return; 
			}
			if(Application.isEventThread()==true) {
				event.event_notify(this);
			}else {
				Application.invokeLater(()->event.event_notify(this));
			}
		}		
		private boolean is_valid_ED() {
			if(payload==null){				
				return false;
			}
			final int t = payload.length-1;
			if(
				payload[0+0]==0 &&
				payload[0+1]==0 &&
				payload[0+2]==0 &&				
				payload[0+3]==0 &&
				payload[t-2]==DLE &&
				payload[t-1]==ETX				
			){
				return true;
			}
			return false;
		}
	};
	//----------------------------------//
	
	public void asyncSetRegister(
		final TokenNotify event, 
		final int addr, 
		int... vals
	) {
		tkn_queue.add(new Token(addr,vals,event));
	}
	public void asyncGetRegister(
		final TokenNotify event, 
		final int addr, 
		final int size
	) {
		Token tkn = new Token(addr,size,event);
		tkn_queue.add(tkn);
	} 
	
	public void setMode(final int cmd) {
		setMode(null,cmd);
	}
	public void setMode(final TokenNotify event,final int cmd) {
		asyncSetRegister(tkn->{
			apply_mode_flag(tkn.values[0]);
			if(event!=null) {
				event.event_notify(tkn);
			}
		},0,cmd);
	}
	
	public void toggleRun(final boolean flg) {
		asyncSetRegister(tkn->{
			apply_state_flag(tkn.values[0]);
		},1,(flg==true)?(0x02):(0x01));
	}
	public void toggleDC1(final boolean flg) {
		asyncSetRegister(tkn->{
			apply_state_flag(tkn.values[0]);
		},1,(flg==true)?(0x21):(0x20));
	}
	public void toggleDC2(final boolean flg) {
		asyncSetRegister(tkn->{
			apply_state_flag(tkn.values[0]);
		},1,(flg==true)?(0x23):(0x22));
	}
	public void toggle(
		final boolean run, 
		final boolean dc1, 
		final boolean dc2
	) {
		toggleDC1(dc1);
		toggleDC2(dc2);
		toggleRun(run);
	}

	//device setting values
	//4* Ton +: 2-32000us, duration of the pulse
	//5* Toff+: 2-32000us, duration of the pause
	//6* Ton -: 2-32000us, duration of the pulse
	//7* Toff-: 2-32000us, duration of the pause
	public void setPulseValue(
		final TokenNotify event,
		final Integer Ton__P_us,
		final Integer Toff_P_us,
		final Integer Ton__N_us,
		final Integer Toff_N_us
	) {
		final int[] arg = {
			(Ton__P_us!=null)?(Ton__P_us.intValue()):(Ton_pos.get()),
			(Toff_P_us!=null)?(Toff_P_us.intValue()):(Tof_pos.get()),
			(Ton__N_us!=null)?(Ton__N_us.intValue()):(Ton_neg.get()),
			(Toff_N_us!=null)?(Toff_N_us.intValue()):(Tof_neg.get()),
		};
		asyncSetRegister(event,
		4,
		arg[0],arg[1], arg[2], arg[3]
	);}

	public void set_DC1(
		final TokenNotify event,
		final Float vol, 
		final Float amp,
		final Float pow
	) {set_DC_value(
		event, 13,
		vol, DC1_V_Set.get(),
		amp, DC1_I_Set.get(),
		pow, DC1_P_Set.get()
	);}
	public void set_DC2(
		final TokenNotify event,
		final Float vol, 
		final Float amp,
		final Float pow
	) {set_DC_value(
		event, 16,
		vol, DC2_V_Set.get(),
		amp, DC2_I_Set.get(),
		pow, DC2_P_Set.get()
	);}
	private void set_DC_value(
		final TokenNotify event, final int address,
		final Float vol, final int def_vol, 
		final Float amp, final int def_amp, 
		final Float pow, final int def_pow
	) {
		if(vol==null && amp==null && pow==null) {
			return;//nothing to do~~~~
		}
		final int[] arg = {
			(vol!=null)?((int)(vol*4f   )):(def_vol),
			(amp!=null)?((int)(amp*1024f)):(def_amp),
			(pow!=null)?((int)(pow*2f   )):(def_pow),
		};
		asyncSetRegister(event,
		address,
		arg[0], arg[1], arg[2]
	);}	
	public void set_DC_value(
		final TokenNotify event,
		final char name, final char attr,
		final Float value, final Float bias
	) {
		if(value==null) {
			return;
		}		
		if(name!='1' && name!='2') {
			Misc.logw("[%s][set_DC_value] no support name", TAG);
			return;
		}
		if(attr!='V' && attr!='I' && attr!='P') {
			Misc.logw("[%s][set_DC_value] no support attribute", TAG);
			return;
		}		
		final int j = (name=='1')?(0):(1);
		final int i = (attr=='V')?(0):((attr=='I')?(1):(2));
		final int ii= (bias==null)?(0):(i+1);

		final int[][] addr = {
			{13, 14, 15},
			{16, 17, 18},
		};
		final int[][] _off = {
			{0, DC1_V_Set.get(), DC1_I_Set.get(), DC1_P_Set.get()},
			{0, DC2_V_Set.get(), DC2_I_Set.get(), DC2_P_Set.get()},
		};
		final float[] _sca= {4f, 1024f, 2f};
		
		final int _val = _off[j][ii]+(int)(value.floatValue()*_sca[i]);
		
		final IntegerProperty[][] prop = {
			{DC1_V_Set, DC1_I_Set, DC1_P_Set},
			{DC2_V_Set, DC2_I_Set, DC2_P_Set},
		}; 
		
		//Misc.logv("[%s] set DC%C_%C=%d", TAG, name, attr, _val);
		
		asyncSetRegister(tkn->{
			prop[j][i].set(_val);
			if(event!=null) {
				event.event_notify(tkn);
			}
		}, addr[j][i], _val);
	}
	public void set_DC_value(
		final TokenNotify event,
		final char name, final char attr,
		final Float value
	) {
		set_DC_value(event,name,attr,value,null);
	}
	//-------------------------------------------------------
	
	private static void show_set_Pulse(final DevSPIK2k dev) {
		final TextField[] box = {
			new	TextField(""+dev.Ton_pos.get()),
			new	TextField(""+dev.Tof_pos.get()),
			new	TextField(""+dev.Ton_neg.get()),
			new	TextField(""+dev.Tof_neg.get()),
		};
		for(TextField obj:box) {
			obj.setPrefWidth(60);
		}
		GridPane lay = new GridPane();
		lay.setAlignment(Pos.CENTER);
		lay.setHgap(10);
		lay.setVgap(10);
		lay.setPadding(new Insets(25, 25, 25, 25));
		lay.addRow(0, new Label("Ton+ :"), box[0], new Label("Toff+:"), box[1]);
		lay.addRow(1, new Label("Ton- :"), box[2], new Label("Toff-:"), box[3]);
		
		final Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setTitle("設定脈衝時間(us)");
		//dia.setHeaderText();
		dia.getDialogPane().setContent(lay);
		if(dia.showAndWait().get()==ButtonType.OK) {
			dev.setPulseValue(null,
				Misc.txt2int(box[0].getText(),dev.Ton_pos.get()),
				Misc.txt2int(box[1].getText(),dev.Tof_pos.get()),
				Misc.txt2int(box[2].getText(),dev.Ton_neg.get()),
				Misc.txt2int(box[3].getText(),dev.Tof_neg.get())
			);
		}
	}
	
	private static void show_set_DC(
		final char id,
		final DevSPIK2k dev
	) {
		final int[] val = {
			(id=='1')?(dev.DC1_V_Set.get()):(dev.DC2_V_Set.get()),
			(id=='1')?(dev.DC1_I_Set.get()):(dev.DC2_I_Set.get()),
			(id=='1')?(dev.DC1_P_Set.get()):(dev.DC2_P_Set.get()),
		};
		
		final TextField box_vol = new TextField(String.format("%.1f",(float)val[0]/4f));
		final TextField box_amp = new TextField(String.format("%.2f",(float)val[1]/1024f));
		final TextField box_pow = new TextField(String.format("%.1f",(float)val[2]/2f));
		
		GridPane lay = new GridPane();
		lay.setAlignment(Pos.CENTER);
		lay.setHgap(10);
		lay.setVgap(10);
		lay.setPadding(new Insets(25, 25, 25, 25));
		lay.addRow(0, new Label("電壓(Volt)"), box_vol);
		lay.addRow(1, new Label("電流(Amp )"), box_amp);
		lay.addRow(2, new Label("功率(Watt)"), box_pow);
		
		final Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setTitle((id=='1')?("設定 DC1"):("設定 DC2"));
		dia.setHeaderText((id=='1')?("確認 DC1 設定值"):("設定 DC2 設定值"));
		dia.getDialogPane().setContent(lay);
		if(dia.showAndWait().get()==ButtonType.OK) {
			if(id=='1') {
				dev.set_DC1(null,
					Misc.txt2Float(box_vol.getText()), 
					Misc.txt2Float(box_amp.getText()), 
					Misc.txt2Float(box_pow.getText())
				);
			}else if(id=='2') {
				dev.set_DC2(null,
					Misc.txt2Float(box_vol.getText()), 
					Misc.txt2Float(box_amp.getText()), 
					Misc.txt2Float(box_pow.getText())
				);
			}
		}
	}
	//---------------------------------//
	
	private static void show_set_mode(final DevSPIK2k dev) {
		
		JFXRadioButton[] lst = {
			new JFXRadioButton("Bipolar"),
			new JFXRadioButton("Unipolar -"),	
			new JFXRadioButton("Unipolar +"),	
			new JFXRadioButton("DC- Mode"),
			new JFXRadioButton("DC+ Mode"),
			//new JFXRadioButton("Multiplex On"),
			//new JFXRadioButton("Multiplex Off"),
		};
		ToggleGroup grp = new ToggleGroup();
		for(JFXRadioButton obj:lst) {
			obj.setToggleGroup(grp);
		}
		lst[0].setUserData(0x01); lst[0].setSelected(dev.ModeBipolar.get());
		lst[1].setUserData(0x02); lst[1].setSelected(dev.ModeUnipolar_neg.get());
		lst[2].setUserData(0x03); lst[2].setSelected(dev.ModeUnipolar_pos.get());
		lst[3].setUserData(0x04); lst[3].setSelected(dev.ModeDC_neg.get());
		lst[4].setUserData(0x05); lst[4].setSelected(dev.ModeDC_pos.get());
		//lst[5].setUserData(0x10);
		//lst[6].setUserData(0x11);
		
		VBox lay0 = new VBox(lst);
		lay0.getStyleClass().addAll("box-pad");
		lay0.setSpacing(13);
		
		final Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setTitle("設定模式");
		dia.setHeaderText("選擇模式，極性");
		dia.getDialogPane().setContent(lay0);
		if(dia.showAndWait().get()==ButtonType.OK) {
			final int cmd = (int)grp.getSelectedToggle().getUserData();
			dev.setMode(cmd);
		}
	}
	
	private static void show_set_state(final DevSPIK2k dev) {
		
		JFXCheckBox chk_run = new JFXCheckBox("Run");
		chk_run.setSelected(dev.Run.get());
		
		JFXCheckBox chk_dc1 = new JFXCheckBox("DC-1");
		chk_dc1.setSelected(dev.DC1.get());
		
		JFXCheckBox chk_dc2 = new JFXCheckBox("DC-2");
		chk_dc2.setSelected(dev.DC2.get());
		
		VBox lay0 = new VBox(chk_run,chk_dc1,chk_dc2);
		lay0.getStyleClass().addAll("box-pad");
		lay0.setSpacing(13);
		
		final Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setTitle("設定開關");
		dia.setHeaderText("確認開關設定");
		dia.getDialogPane().setContent(lay0);
		if(dia.showAndWait().get()==ButtonType.OK) {
			dev.toggle(chk_run.isSelected(), chk_dc1.isSelected(), chk_dc2.isSelected());	
		}
	}
	
	public static Pane genInfoPanel(final DevSPIK2k dev) {
		
		Label t_arc_cnt = new Label();
		t_arc_cnt.textProperty().bind(dev.ARC_count.asString("%d"));
		
		Label[] t_mode = {
			new Label("bipolar"),
			new Label("Unipolar +"),
			new Label("Unipolar -"),
			new Label("DC +"),
			new Label("DC -"),
		};
		t_mode[0].visibleProperty().bind(dev.ModeBipolar);
		t_mode[1].visibleProperty().bind(dev.ModeUnipolar_pos);
		t_mode[2].visibleProperty().bind(dev.ModeUnipolar_neg);
		t_mode[3].visibleProperty().bind(dev.ModeDC_pos);
		t_mode[4].visibleProperty().bind(dev.ModeDC_neg);
		//---------------------------------------
		
		Label t_on_pos= new Label();
		t_on_pos.textProperty().bind(dev.Ton_pos.asString("%3d"));

		Label t_of_pos = new Label();
		t_of_pos.textProperty().bind(dev.Tof_pos.asString("%3d"));

		Label t_on_neg = new Label();
		t_on_neg.textProperty().bind(dev.Ton_neg.asString("%3d"));

		Label t_of_neg = new Label();
		t_of_neg.textProperty().bind(dev.Tof_neg.asString("%3d"));
		//---------------------------------------
				
		Label t_vol_pv1 = new Label();
		t_vol_pv1.textProperty().bind(dev.DC1_V_Act.divide(4f).asString("%.1f"));
		t_vol_pv1.setOnMouseClicked(e->show_set_DC('1',dev));
		
		Label t_amp_pv1 = new Label();
		t_amp_pv1.textProperty().bind(dev.DC1_I_Act.divide(1024f).asString("%.1f"));
		t_amp_pv1.setOnMouseClicked(e->show_set_DC('1',dev));
		
		Label t_pow_pv1 = new Label();
		t_pow_pv1.textProperty().bind(dev.DC1_P_Act.divide(2f).asString("%.1f"));
		t_pow_pv1.setOnMouseClicked(e->show_set_DC('1',dev));
		
		Label t_vol_pv2 = new Label();
		t_vol_pv2.textProperty().bind(dev.DC2_V_Act.divide(4f).asString("%.1f"));
		t_vol_pv2.setOnMouseClicked(e->show_set_DC('2',dev));
		
		Label t_amp_pv2 = new Label();
		t_amp_pv2.textProperty().bind(dev.DC2_I_Act.divide(1024f).asString("%.1f"));
		t_amp_pv2.setOnMouseClicked(e->show_set_DC('2',dev));
		
		Label t_pow_pv2 = new Label();
		t_pow_pv2.textProperty().bind(dev.DC2_P_Act.divide(2f).asString("%.1f"));
		t_pow_pv2.setOnMouseClicked(e->show_set_DC('2',dev));
		//---------------------------------------
		
		Label[] txt = {
			t_on_pos, t_of_pos, 
			t_on_neg, t_of_neg,			
			t_vol_pv1, t_vol_pv2,
			t_amp_pv1, t_amp_pv2,
			t_pow_pv1, t_pow_pv2,
			t_arc_cnt,
		};
		for(Label obj:txt) {
			obj.getStyleClass().addAll("font-size5");
			obj.setMinWidth(67.);
			obj.setMaxWidth(Double.MAX_VALUE);
			obj.setAlignment(Pos.CENTER_RIGHT);
			GridPane.setHgrow(obj, Priority.ALWAYS);
		}
		//--------------------------------------------
		
		Button act_mode = new Button("設定模式");
		act_mode.setMaxWidth(Double.MAX_VALUE);
		act_mode.setOnAction(e->show_set_mode(dev));
		
		Button act_state = new Button("設定開關");
		act_state.setMaxWidth(Double.MAX_VALUE);
		act_state.setOnAction(e->show_set_state(dev));
		//---------------------------------------
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-box");
		lay1.addRow(0, new Label("Ton+ :"), t_on_pos, new Label("Toff+:"), t_of_pos);
		lay1.addRow(1, new Label("Ton- :"), t_on_neg, new Label("Toff-:"), t_of_neg);
		lay1.setOnMouseClicked(e->show_set_Pulse(dev));
		
		final GridPane lay2 = new GridPane();
		lay2.getStyleClass().addAll("box-box");
		lay2.addRow(0, new Label("     "), new Label("電壓"), new Label("電流"), new Label("功率"));
		lay2.addRow(1, new Label("DC1"), t_vol_pv1, t_amp_pv1, t_pow_pv1);
		lay2.addRow(2, new Label("DC2"), t_vol_pv2, t_amp_pv2, t_pow_pv2);
		lay2.add(new Label("電弧"), 0, 3, 1, 1);
		lay2.add(t_arc_cnt, 1, 3, 3, 1);
		
		final HBox lay3 = new HBox(
			new Label("極性:"),
			new StackPane(t_mode)
		); 
		lay3.getStyleClass().addAll("box-box");
		
		final HBox lay4 = new HBox(
			PanBase.genIndicator("Run", dev.Run),
			PanBase.genIndicator("DC-1", dev.DC1),
			PanBase.genIndicator("DC-2", dev.DC2)
		);
		lay4.getStyleClass().addAll("box-box");
			
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad");
		lay0.add(lay1, 0, 0, 4, 2);
		lay0.add(lay2, 0, 2, 4, 4);
		lay0.add(lay3, 0, 6, 4, 1);	
		lay0.add(act_mode, 0, 7, 4, 1);
		lay0.add(lay4, 0, 8, 4, 1);
		lay0.add(act_state, 0, 10, 4, 1);
		return lay0;
	}
	//---------------------------------------------
	
	private static void show_error(Token tkn) {
		if(tkn.payload[3]==0) {
			return;
		}
		String txt = String.format(
			"Addr:%d+%d, Error:%d",
			tkn.address,tkn.count,(int)tkn.payload[3]
		);
		Misc.logv("[SPIK2000] %s",txt);
		//error code
		final Alert dia = new Alert(AlertType.ERROR);
		dia.setTitle("錯誤！！");
		dia.setHeaderText(txt);
		dia.showAndWait();
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
		txt[0].setOnMouseClicked(e->show_set_Pulse(dev));
		txt[1].textProperty().bind(dev.Tof_pos.asString("Toff+: %3d"));
		txt[1].setOnMouseClicked(e->show_set_Pulse(dev));
		txt[2].textProperty().bind(dev.Ton_neg.asString("Ton- : %3d"));
		txt[2].setOnMouseClicked(e->show_set_Pulse(dev));
		txt[3].textProperty().bind(dev.Tof_neg.asString("Toff-: %3d"));
		txt[3].setOnMouseClicked(e->show_set_Pulse(dev));

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
