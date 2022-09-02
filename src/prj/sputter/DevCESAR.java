package prj.sputter;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PadTouch;

/**
 * remote control CESAR Generator Model 136.<p>
 * implement RS-232 with AE Bus.<p>
 * Advanced Energy.<p>
 * @author qq
 *
 */
public class DevCESAR extends DevTTY {

	public DevCESAR() {
		TAG = "CESAR";
	}
	public DevCESAR(final String tag) {
		TAG = tag;
	}
	
	@Override
	public void afterOpen() {
		addState(STG_INIT ,()->state_initial()).
		addState(STG_WATCH,()->state_watcher());
		playFlow(STG_INIT);
	}
	@Override
	public void beforeClose() {
	}
	
	private static final String STG_INIT = "initial";
	private static final String STG_WATCH= "watcher";
	
	public final IntegerProperty watt = new SimpleIntegerProperty();
	public final IntegerProperty freq = new SimpleIntegerProperty();	
	public final IntegerProperty duty = new SimpleIntegerProperty();

	private void state_initial() {
		
		set_active_mode(ACTIVE_HOST);		
		//set_active_mode(ACTIVE_FRONT_PANEL);
		
		int[] parm = get_pulse_watt();
		final int v_watt  = parm[0];
		//final int r_mode = parm[1];//regulation mode

		final int v_freq = get_pulse_freq();
		
		final int v_duty = get_pulse_duty();

		int mode = get_active_mode();
		if(mode==ACTIVE_FRONT_PANEL) {
			mode = set_active_mode(ACTIVE_HOST);
		}
		final int v_mode = mode;

		Application.invokeLater(()->{
			watt.set(v_watt);
			freq.set(v_freq);
			duty.set(v_duty);
			
			switch(v_mode) {
			case ACTIVE_HOST: 
				last_cmd_status.set("遠端操作");
				break;
			case ACTIVE_FRONT_PANEL: 
				last_cmd_status.set("面板控制");
				break;
			}
		});		
		nextState(STG_WATCH);
	}
	
	private final BooleanProperty[] status = {		
		new SimpleBooleanProperty(),//recipe running 
		new SimpleBooleanProperty(),//output power
		new SimpleBooleanProperty(),//RF on requested
		new SimpleBooleanProperty(),//out of tolerance
		
		new SimpleBooleanProperty(),//end of target life
		new SimpleBooleanProperty(),//over temperature
		new SimpleBooleanProperty(),//interlock_open
		
		new SimpleBooleanProperty(),//out of setpoint
		
		new SimpleBooleanProperty(),//current limit
		new SimpleBooleanProperty(),//PROFIBUS error
		new SimpleBooleanProperty(),//extend fault
		new SimpleBooleanProperty(),//CEX is locked		
	};
	
	public final IntegerProperty runtime = new SimpleIntegerProperty();//unit is seconds
	
	public final IntegerProperty powerForward = new SimpleIntegerProperty();
	public final IntegerProperty powerReflect = new SimpleIntegerProperty();
	public final IntegerProperty powerDelived = new SimpleIntegerProperty();
	
	private void state_watcher() {
		
		final byte[] p_status = AE_bus(1,162,"");//process status
		final boolean[] v_status = {
			(p_status[0] & 0x04)!=0,
			(p_status[0] & 0x20)!=0,
			(p_status[0] & 0x40)!=0,
			(p_status[0] & 0x80)!=0,
				
			(p_status[1] & 0x01)!=0,
			(p_status[1] & 0x08)!=0,
			(p_status[1] & 0x80)!=0,
				
			(p_status[2] & 0x20)!=0,

			(p_status[3] & 0x01)!=0,
			(p_status[3] & 0x04)!=0,
			(p_status[3] & 0x20)!=0,
			(p_status[3] & 0x80)!=0,
		};
				
		final int v_runtime = report_runtime();
		
		final int v_power_forward = report_power_forward();
		
		final int v_power_reflect = report_power_reflect();
		
		final int v_power_delived = report_power_deliver();
		
		Application.invokeLater(()->{
			for(int i=0; i<v_status.length; i++) {
				status[i].set(v_status[i]);
			}
			if(v_runtime>0) {
				runtime.set(v_runtime);
			}
			if(v_power_forward>0) {
				powerForward.set(v_power_forward);
			}
			if(v_power_reflect>0) {
				powerReflect.set(v_power_reflect);
			}
			if(v_power_delived>0) {
				powerDelived.set(v_power_delived);
			}
		});
		block_sleep_msec(50);
	}
	
	public final StringProperty last_cmd_status = new SimpleStringProperty("");
	
	/**
	 * CSR - command status response code.<p>
	 * if the generator reject command, we got this code.<p>
	 * translate code to plain text.<p> 
	 * @param code
	 * @return
	 */
	private int code2text(final int code) {
		String txt = "";
		switch(code) {
		case -1: txt = "internal error!!";           break;
		case  0: return code;
		case  1: txt = "wrong mode"; break;
		case  2: txt = "RF is on";   break;
		case  4: txt = "data is out of range";       break;
		case  5: txt = "User Port RF signal is off"; break;
		case  7: txt = "active fault(s) exist";      break;
		case  9: txt = "data count is wrong";        break;
		case 19: txt = "recipe mode is actived";     break;
		case 50: txt = "frequency is out of range";  break;
		case 51: txt = "duty cycle is out of range"; break;
		case 99: txt = "not implemented";            break;
		}		
		final String _txt = txt;
		Application.invokeLater(()->last_cmd_status.set(_txt));
		return code;
	}
	
	private int set_RF_output(final boolean on) {	
		byte[] recv;
		if(on==true) {
			recv = AE_bus(1,2,"");
		}else {
			recv = AE_bus(1,1,"");
		}
		if(recv.length==0) { return code2text(-1); }
		return code2text(recv[0]);
	}
	
	/*private static int FREQ_MODE_FST = 0;
	private static int FREQ_INTEGRAL_POS = 1;
	private static int FREQ_INTEGRAL_NEG = 2;
	private static int FREQ_PROPORTION_POS = 3;
	private static int FREQ_PROPORTION_NEG = 4;
	private int set_frequency_tuning(final int freq,final int mode) {
		//frequency unit is kHz
		final byte[] recv = AE_bus(1,14,String.format("%04X %02X", freq, mode));
		if(recv.length==0) { return code2text(-1); }
		return code2text(recv[0]);
	}
	private int get_frequency_tuning() {
		final byte[] recv = AE_bus(1,187,"");
		if(recv.length==0) { return code2text(-1); }
		return 0;
	}*/
	/*
	private int set_regulation_mode(final int mode) {
		// 6 - forward power
		// 7 - load power 
		// 8 - external power
		final byte[] recv = AE_bus(1,3,String.format("%02X", mode));
		if(recv.length==0) { return code2text(-1); }
		return code2text(recv[0]);
	}	
	private int get_regulation_mode() {
		final byte[] recv = AE_bus(1,154,"");
		if(recv.length==0) { return code2text(-1); }
		return code2text(recv[0]);
	}
	
	private int set_remote_override(final int mode) {
		final byte[] recv = AE_bus(1,29,String.format("%02X", mode));
		if(recv.length==0) { return code2text(-1); }
		return code2text(recv[0]);
	}
	*/
	private static final int ACTIVE_UNKNOW     = -1;
	private static final int ACTIVE_HOST       = 2;
	//private static final int ACTIVE_USER_PORT  = 4;
	private static final int ACTIVE_FRONT_PANEL= 6;
	private int set_active_mode(final int mode) {
		final byte[] recv = AE_bus(1,14,String.format("%02X", mode));
		if(recv.length==0) { return code2text(-1); }
		return code2text(recv[0]);
	}
	private int get_active_mode() {
		final byte[] recv = AE_bus(1,155,"");
		if(recv.length==0) { code2text(-1); return ACTIVE_UNKNOW; }
		return recv[0];
	}
	
	private int set_pulse_watt(final int watt) {
		final byte[] recv = AE_bus(1,8,String.format("%04X",watt));
		if(recv.length==0) { return code2text(-1); }
		return code2text(recv[0]);
	}
	private int[] get_pulse_watt() {
		final byte[] recv = AE_bus(1,164,"");
		final int[] parm = new int[2];
		if(recv.length>=3) {			
			parm[0] = byte2int(false,recv[0], recv[1]);//watt
			parm[1] = recv[2];//regulation mode
		}else {
			code2text(-1);
		}
		return parm;
	}

	private int set_pulse_freq(int freq) {
		if(freq<=      0) { freq=      1; }
		if(freq>=100_000) { freq=100_000; }	
		final byte[] recv = AE_bus(1,93,String.format("%06X",freq));
		if(recv.length==0) { return code2text(-1); }
		return code2text(recv[0]);
	}
	private int get_pulse_freq() {
		final byte[] recv = AE_bus(1,193,"");
		if(recv.length==0) { return code2text(-1); }
		return byte2int(false,recv);
	}
	
	private int set_pulse_duty(int duty) {
		if(duty<0   ) { duty= 0; }
		if(duty>=100) { duty=99; }		
		final byte[] recv = AE_bus(1,96,String.format("%04X",duty));
		if(recv.length==0) { return code2text(-1); }
		return code2text(recv[0]);
	}
	private int get_pulse_duty() {
		final byte[] recv = AE_bus(1,196,"");
		if(recv.length==0) { return code2text(-1); }
		return byte2int(false,recv);
	}
	
	/*private String report_type() {
		final byte[] recv = AE_bus(1,128,"");
		if(recv.length==0) { code2text(-1); return ""; }
		return new String(recv);
	}*/
	private int report_power_forward() {
		final byte[] recv = AE_bus(1,165,"");
		if(recv.length==0) { return code2text(-1); }
		return byte2int(false,recv);
	}
	private int report_power_reflect() {
		final byte[] recv = AE_bus(1,166,"");
		if(recv.length==0) { return code2text(-1); }
		return byte2int(false,recv);
	}
	private int report_power_deliver() {
		final byte[] recv = AE_bus(1,167,"");
		if(recv.length==0) { return code2text(-1); }
		return byte2int(false,recv);
	}
	/*private int report_externel_feedback() {
		final byte[] recv = AE_bus(1,168,"");
		if(recv.length==0) { return code2text(-1); }
		return byte2int(false,recv);//DC bias
	}*/	
	private int report_runtime() {
		final byte[] recv = AE_bus(1,205,"");
		if(recv.length==0) { return code2text(-1); }
		return byte2int(false,recv);//unit is second~~~
	}
	
	public void setRFOutput(final boolean on) {
		asyncBreakIn(()->set_RF_output(on));
	}
	public void setPulseWatt(final int val) {		
		asyncBreakIn(()->{
			final int code = set_pulse_watt(val);
			if(code==0) {
				Application.invokeLater(()->watt.set(val));
			}
		});
	}
	public void setPulseFreq(final int val) {		
		asyncBreakIn(()->{
			final int code = set_pulse_freq(val);
			if(code==0) {
				Application.invokeLater(()->freq.set(val));
			}
		});
	}
	public void setPulseDuty(final int val) {		
		asyncBreakIn(()->{
			final int code = set_pulse_duty(val);
			if(code==0) {
				Application.invokeLater(()->duty.set(val));
			}
		});
	}
	
	public void toggleActiveMode() {
		asyncBreakIn(()->{
			final int mode = get_active_mode();
			switch(mode) {
			case ACTIVE_HOST:
				if(set_active_mode(ACTIVE_FRONT_PANEL)==0) {
					Application.invokeLater(()->last_cmd_status.set("面板控制"));
				}
				break;
			case ACTIVE_FRONT_PANEL:
				if(set_active_mode(ACTIVE_HOST)==0) {
					Application.invokeLater(()->last_cmd_status.set("遠端操作"));
				}
				break;
			}
		});
	}	
	//-------------------------------------//
	
	private static void pop_editor(		
		final String title,
		final DevCESAR dev,
		final IntegerProperty prop
	) {		
		final TextInputDialog dia = new TextInputDialog(""+prop.get());
		dia.setTitle("設定 "+title);
		dia.setContentText("");		
		Optional<String> res = dia.showAndWait();
		if(res.isPresent()==false) {
			return;
		}
		try {
			final int val = Integer.parseInt(res.get());
			if(prop==dev.watt) {
				dev.setPulseWatt(val);
			}else if(prop==dev.freq) {
				dev.setPulseFreq(val);
			}else if(prop==dev.duty) {
				dev.setPulseDuty(val);
			}			
		}catch(NumberFormatException e) {			
		}		
	}
	
	public static Node genInfoPanel(final DevCESAR dev) {
		
		Label txt_watt = new Label();
		txt_watt.textProperty().bind(dev.watt.asString("%6dW"));
		txt_watt.setOnMouseClicked(e->pop_editor("功率",dev,dev.watt));

		Label txt_freq = new Label();
		txt_freq.textProperty().bind(dev.freq.asString("%6dHz"));
		txt_freq.setOnMouseClicked(e->pop_editor("頻率",dev,dev.freq));
		
		Label txt_duty = new Label();
		txt_duty.textProperty().bind(dev.duty.asString("%6d%%"));
		txt_duty.setOnMouseClicked(e->pop_editor("週期",dev,dev.duty));

		Label txt_ford = new Label();
		txt_ford.textProperty().bind(dev.powerForward.asString("%3d(F"));
		
		Label txt_rflt = new Label();
		txt_rflt.textProperty().bind(dev.powerReflect.asString("%3d(R"));
		
		Label txt_dliv = new Label();
		txt_dliv.textProperty().bind(dev.powerDelived.asString("%3d(D"));
		
		final Label[] txt = {
			new Label("功率:"), txt_watt, txt_ford,
			new Label("頻率:"), txt_freq, txt_rflt,
			new Label("週期:"), txt_duty, txt_dliv
		};
		
		for(Label obj:txt) {
			obj.getStyleClass().addAll("font-size5");
			if(obj.textProperty().isBound()==true){
				obj.setMinWidth(67.);
				obj.setMaxWidth(Double.MAX_VALUE);
			}
			obj.setAlignment(Pos.CENTER_RIGHT);
			GridPane.setHgrow(obj, Priority.ALWAYS);
		}
		
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad");
		lay0.addColumn(0, txt[0], txt[3], txt[6]);
		lay0.addColumn(1, txt[1], txt[4], txt[7]);
		lay0.addColumn(2, txt[2], txt[5], txt[8]);
		return lay0;
	}
	
	
	public static Node genCtrlPanel(final DevCESAR dev) {
		
		final Button btn_freq = new Button();
		btn_freq.setGraphic(Misc.getIconView("settings.png"));
		btn_freq.setOnAction(e->{
			final PadTouch pad = new PadTouch('N',"Pulse Frequency(Hz)");
			final Optional<String> opt = pad.showAndWait();		
			if(opt.isPresent()==false) { return; }
			int val = Integer.valueOf(opt.get());
			dev.setPulseFreq(val);
		});
		
		final Button btn_duty = new Button();
		btn_duty.setGraphic(Misc.getIconView("settings.png"));
		btn_duty.setOnAction(e->{
			final PadTouch pad = new PadTouch('N',"Pulse Duty(%)");
			final Optional<String> opt = pad.showAndWait();		
			if(opt.isPresent()==false) { return; }
			int val = Integer.valueOf(opt.get());
			dev.setPulseDuty(val);
		});
		
		final Button btn_watt = new Button();
		btn_watt.setGraphic(Misc.getIconView("settings.png"));
		btn_watt.setOnAction(e->{
			final PadTouch pad = new PadTouch('N',"Setpoint(Watt)");
			final Optional<String> opt = pad.showAndWait();		
			if(opt.isPresent()==false) { return; }
			int val = Integer.valueOf(opt.get());
			dev.setPulseWatt(val);
		});
		
		final double width = 200.;
		
		final Label txt_freq = new Label();
		txt_freq.textProperty().bind(dev.freq.asString("%6dHz"));
		txt_freq.setOnMouseClicked(e->btn_freq.getOnAction().handle(null));
		
		final Label txt_duty = new Label();
		txt_duty.textProperty().bind(dev.duty.asString("%6d%%"));
		txt_duty.setOnMouseClicked(e->btn_duty.getOnAction().handle(null));
		
		final Label txt_watt = new Label();
		txt_watt.textProperty().bind(dev.watt.asString("%6dW"));
		txt_watt.setOnMouseClicked(e->btn_watt.getOnAction().handle(null));
		
		for(Label txt:new Label[] {txt_freq, txt_duty, txt_watt}) {
			txt.getStyleClass().addAll("font-size7");
			txt.setMinWidth(width);
			txt.setAlignment(Pos.CENTER_RIGHT);
		}
		//------------------------------------
		
		final JFXTextField box_level = new JFXTextField();
		box_level.setPrefSize(60, 37);
		box_level.setOnAction(e->{
			final String txt = box_level.getText().trim();
			try {
				final int val = Integer.valueOf(txt);
				dev.setPulseWatt(val);				
			}catch(NumberFormatException e1) {				
			}
			box_level.setText("");
		});
		
		final Label[] txt_power = {
			new Label(), new Label("/"), 
			new Label(), new Label("/"),
			new Label(),
		};
		txt_power[0].textProperty().bind(dev.powerForward.asString("%4d"));
		txt_power[2].textProperty().bind(dev.powerReflect.asString("%4d"));
		txt_power[4].textProperty().bind(dev.powerDelived.asString("%4d"));
		
		final HBox lay4 = new HBox(txt_power);
		lay4.getStyleClass().addAll("box-pad");
		HBox.setHgrow(txt_power[0],Priority.ALWAYS);
		HBox.setHgrow(txt_power[2],Priority.ALWAYS);
		HBox.setHgrow(txt_power[4],Priority.ALWAYS);	
		//------------------------------------
		
		final Button btn_connect = new Button();
		btn_connect.setGraphic(Misc.getIconView("lan-connect.png"));
		btn_connect.setOnAction(e->dev.toggleActiveMode());
		
		final Label txt_status = new Label();
		txt_status.setMaxWidth(Double.MAX_VALUE);
		txt_status.textProperty().bind(dev.last_cmd_status);

		final HBox lay3 = new HBox(txt_status,btn_connect);
		lay3.getStyleClass().addAll("box-pad");
		HBox.setHgrow(txt_status ,Priority.ALWAYS);
		//------------------------------------
		
		final JFXButton btn_on = new JFXButton("ON");
		btn_on.getStyleClass().add("btn-raised-1");
		btn_on.setMaxWidth(Double.MAX_VALUE);
		btn_on.setOnAction(e->dev.set_RF_output(true));
		
		final JFXButton btn_off = new JFXButton("OFF");
		btn_off.getStyleClass().add("btn-raised-0");
		btn_off.setMaxWidth(Double.MAX_VALUE);
		btn_off.setOnAction(e->dev.set_RF_output(false));	
		
		final HBox lay2 = new HBox(btn_on,btn_off);
		lay2.getStyleClass().addAll("box-pad");
		HBox.setHgrow(btn_on ,Priority.ALWAYS);
		HBox.setHgrow(btn_off,Priority.ALWAYS);
		//------------------------------------
	
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad");
		lay1.addRow(0,txt_freq,btn_freq);
		lay1.addRow(1,txt_watt,btn_watt);
		lay1.addRow(2,txt_duty,btn_duty);		
		lay1.add(lay4, 0, 3, 2, 1);
		lay1.add(lay3, 0, 4, 2, 1);
		lay1.add(lay2, 0, 5, 2, 1);
		return lay1;
	}	
}
