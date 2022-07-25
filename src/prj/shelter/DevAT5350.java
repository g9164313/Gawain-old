package prj.shelter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import jssc.SerialPort;
import jssc.SerialPortException;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PanDialog;
import narl.itrc.UtilPhysical;

/**
 * ATOMTEX AT5350, dosimeter. 
 * this device follow IEEE488.2 standard.
 * @author qq
 *
 */
@SuppressWarnings("restriction")
public class DevAT5350 extends DevTTY {

	public DevAT5350(){
		TAG="AT5350";
	}
	@Override
	public void afterOpen() {
		//Only GPIB connector have status or state (*STB, STAT)
		addState(STA_IDENTIFY, stage_identify);
		addState(STA_MEASURE , stage_measure);
		addState(STA_CORRECT , stage_correct);
		playFlow(STA_IDENTIFY);
	}
	@Override
	public void beforeClose(){		
	}
	
	private final String STA_IDENTIFY= "stage_identify";
	private final String STA_MEASURE = "stage_measure";
	private final String STA_CORRECT = "stage_correct";
	
	public final StringProperty[] Identify ={ 
		new SimpleStringProperty("＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊"),
	};

	public final BooleanProperty isIdle = new SimpleBooleanProperty(false);
	
	private Runnable stage_identify = ()->{		

		wxr("CONF:DRAT LOW");
		wxr(":DRAT:FILT ON");
		//wxr(":DRAT:FILT 100");
		wxr(":DRAT:DAMP OFF");//這個會影響計時!!!
		wxr(":DRAT:CORR ON");//no work
		//wxr(":HVOL ON");
		//wxr(":HVOL 400");

		//wxr_log(":CONF?");
		//wxr_log(":DRAT:FILT?");
		//wxr_log(":DRAT:FILT:VAL?");
		//wxr_log(":DRAT:DAMP?");
		//wxr_log(":DRAT:DAMP:VAL?");
		//wxr_log(":DRAT:CORR?");
		//wxr_log(":HVOL?");
		//wxr_log(":HVOL:VAL?");
		//wxr_log(":FACT?");
		//wxr_log(":FACT:TEMP?");
		//wxr_log(":FACT:PRES?");

		final String[] idfy = wxr("*IDN?")
			.replaceAll("[\n|\\s]", "")
			.split(",");
		Application.invokeLater(()->{
			Identify[0].setValue(idfy[0]);
			Identify[1].setValue(idfy[1]);
			Identify[2].setValue(idfy[2]);
			Identify[3].setValue(idfy[3]);
			isIdle.set(true);
		});
		nextState("");
	};

	private Runnable stage_correct = ()->{
		wxr(":CORR:AUTO");
		wxr("*OPC?");//When working, tty reading will be blocked!!!
		Application.invokeLater(()->isIdle.set(true));
		nextState("");
	};
	
	private String meas_time = "";//unit is second~~~
	private String meas_filt = "";
	private String meas_damp = "";	
	private String meas_corr = "";
	private String meas_volt = "";
	private String meas_temp = "";
	private String meas_pres = "";//kPa, 1atm=100kPa
	
	//"\"+1.3844E-04 Sv/min #800\",\"+1.3842E-04 Sv/min #900\",\"+1.3840E-04 Sv/min #1000\"";	
	//text will be cook, remove '"' and ','
	private String meas_last_array="";
	
	public static final String summaryUnit = "uSv/hr";
	
	private final SummaryStatistics meas_last_summary = new SummaryStatistics();
	
	private Runnable meas_after = null;
	
	private Runnable stage_measure = ()->{
		//final long t1 = System.currentTimeMillis();

		//apply all parameters!!!!!
		if(check_NRF(meas_filt)==true) {
			wxr(":FILT ON");
			wxr(":FILT:VAL "+meas_filt);
		}
		if(check_NRF(meas_damp)==true) {
			wxr(":DAMP ON");
			wxr(":DAMP:VAL "+meas_damp);
		}
		if(meas_corr.length()!=0) {
			wxr(":CORR ON");
		}
		if(check_NRF(meas_volt)==true) {
			wxr(":HVOL ON");
			wxr(":HVOL:VAL "+meas_volt);
		}
		if(meas_temp.length()!=0 || meas_pres.length()!=0) {
			wxr(":FACT ON");
			if(check_NRF(meas_temp)==true) {			
				wxr(":FACT:TEMP "+meas_temp);
			}
			if(check_NRF(meas_pres)==true) {
				wxr(":FACT:PRES "+meas_pres);
			}
		}
				
		//count 計數外圈， every 計數內圈，一圈就測量 0.1sec
		long count,every;
		if(meas_time.length()==0) {
			count = parse_NR1(wxr("TRIG:COUN?"),1);
			every = parse_NR1(wxr("TRIG:ECO?" ),1);
		}else {
			final long c_sec = Misc.text2tick(meas_time)/100L;//1 sample = 0.1 second
			/*if(c_sec<=500L) {
				count = c_sec;
				every = 1;
			}else {
				count = 500L;
				every = c_sec/500L;
				if((c_sec%500L)!=0L) {
					every+=1;
					count = c_sec/every;
				}
				if(count>500L) {
					Misc.logw("[stage_measure] RoundUP!! count=%d,every=%d,time=%d", count,every,c_sec);
					count = 500L;
				}
			}*/
			if(c_sec<=20) {
				count = c_sec;
				every = 1;
			}else {
				count = 20;
				every = c_sec/count;
			}

			wxr("TRIG:COUN "+count);
			wxr("TRIG:ECO  "+every);
		}	
		wxr("INIT");
		wxr("*TRG");
		wxr("*OPC?");//When working, tty reading will be blocked!!!
		
		//final long t2 = System.currentTimeMillis();
		//Misc.logv("[stage_measure] %s",Misc.tick2text(t2-t1,true));
		block_sleep_msec(25);
		
		//final String array = wxr(String.format("FETC:ARR? %d", count*every));		
		final String array = wxr(String.format("FETC:ARR? %d", count));
		meas_last_array = array.replace('"', ' ').replace(',', '\n');
		//.replace("\"", "").replace(",", "\n");
		
		Misc.logv("[%s] %s", TAG, array);
		
		update_summary(
			meas_last_array,
			summaryUnit,
			meas_last_summary
		);

		Application.invokeLater(()->{
			isIdle.set(true);
			if(meas_after!=null) {
				meas_after.run();
			}
			//reset all parameters for next turn~~~~
			meas_time = "";
			meas_filt = "";
			meas_damp = "";	
			meas_corr = "";
			meas_volt = "";
			meas_temp = "";
			meas_pres = "";
			meas_after= null;
		});
		nextState("");
	};

	private String wxr(String cmd){
		if(cmd.endsWith("\n")==false){
			cmd = cmd + "\n";
		}
		final SerialPort dev = port.get();
		if(dev.isOpened()==false) {
			return "";
		}
		String recv = "";
		try {
			//the correct communication is that:
			//Send character, then Receive .
			for(char cc:cmd.toCharArray()) {			
				dev.writeByte((byte)cc);			
				cc = (char)dev.readBytes(1)[0];			
			}		
			if(cmd.contains("?")==false) {
				//no query statement, just go back~~~
				return recv;
			}
			while(true) {			
				char cc = (char)dev.readBytes(1)[0];
				if(cc==0x0A) { break; }
				recv = recv + cc;
			}
		} catch (SerialPortException e) {			
			Misc.loge("[%s] wxr - %s", TAG, e.getMessage());
		}
		return recv;	
	}
	
	@SuppressWarnings("unused")
	private String wxr_log(final String txt) {
		final String res = wxr(txt);
		Misc.logv("[%s]%s-->%s", TAG, txt, res);
		return res;
	} 
	
	//------------------------------------------------
	
	/**
	 |*OPC, *OPC? --> operation complete command
	 |*WAI --> wait-to-continue
	 |*CLS --> clear status (all event, error queue, cancel *OPC)
	 |*RST, ABORt, SYSTem:PRESet --> reset command
	 |*IDN? --> identify device
	 |*CAL? --> perform auto calibration
	 |*TRG --> trigger command
	 |*STB? --> read status byte 被 *SRE 影響
	 |*SRE, *SRE? --> service request enable
	 |*ESR? --> standard event status 被 *ESE 影響
	 |*ESE, *ESE? --> standard event status enable
	 |ABOR*RST
	 面板按鈕 [4]STA 開始測量，按鈕 [6]RES 停止測量
	 */
	
	public void asyncCorrection(){
		isIdle.set(false);
		asyncBreakIn(()->{
		nextState("stage_correct");
	});}

	public void asyncMeasure(
		final String time,
		final String filt,
		final String damp,	
		final String corr,
		final String volt,
		final String temp,
		final String pres,
		final Runnable event
	){
		isIdle.set(false);
		asyncBreakIn(()->{
		meas_time = time;
		meas_filt = filt;
		meas_damp = damp;
		meas_corr = corr;
		meas_volt = volt;
		meas_temp = temp;
		meas_pres = pres;
		meas_after= event;
		nextState("stage_measure");
	});}
	public void asyncMeasure(){
		asyncMeasure("20","","","","","","",null);
	}
	public void asyncMeasure(
		final String time
	){
		asyncMeasure(time,"","","","","","",null);
	}
	public void asyncMeasure(
		final String time,
		final String temp,
		final String pres
	){
		asyncMeasure(time,"","","","",temp,pres,null);
	}
	
	public void syncAbort(){
		wxr("ABOR");
	}
	
	//AT5350 example:
	//SEND --> FETC:ARR? 
	//RECV --> "-1.1279E-08 Sv/min #774","-1.1279E-08 Sv/min #775","-1.1097E-08 Sv/min #776","-1.1097E-08 Sv/min #777","-1.1097E-08 Sv/min #778","-1.1097E-08 Sv/min #779","-1.1097E-08 Sv/min #780","-1.1279E-08 Sv/min #781","-1.1279E-08 Sv/min #782","-1.1279E-08 Sv/min #783","-1.1279E-08 Sv/min #784","-1.1279E-08 Sv/min #785","-1.1279E-08 Sv/min #786","-1.1097E-08 Sv/min #787","-1.1279E-08 Sv/min #788","-1.1097E-08 Sv/min #789","-1.1097E-08 Sv/min #790","-1.1279E-08 Sv/min #791","-1.1097E-08 Sv/min #792","-1.1097E-08 Sv/min #793"

	public String lastMeasure() {
		return new String(meas_last_array);
	}
	public SummaryStatistics lastSummary() {
		return new SummaryStatistics(meas_last_summary);
	}
	private static void update_summary(
		final String meas,
		final String unit,
		final SummaryStatistics stat
	) {
		stat.clear();
		if(meas.length()==0) {
			return;
		}
		for(String txt:meas.split("\n")) {
			final int pos = txt.indexOf("#");
			if(pos>=0) {
				txt = txt.substring(0,pos);
			}
			String val = UtilPhysical.convertScale(
				txt.trim(),
				unit
			);
			if(val.length()==0) {
				continue;
			}
			try {
				stat.addValue(Float.parseFloat(val));
			}catch(NumberFormatException e) {
				Misc.loge("[update_summary] %s", val);//Is it possible???
			}
		}
	}

	/*private static boolean parse_flg(final String txt, boolean def) {
		final int val = parse_NR1(txt,0);
		return (val!=0)?(true):(false);
	}*/
	private static int parse_NR1(final String txt, int def) {
		if(txt==null) { return def; }
		if(txt.length()==0) { return def; }
		try {
			def = Integer.parseInt(txt.trim());
		}catch(Exception e) {
		}
		return def;
	}
	/*private static float parse_NR2(final String txt, float def) {
		if(txt==null) { return def; }
		if(txt.length()==0) { return def; }
		try {
			def = Float.parseFloat(txt.trim());
		}catch(Exception e) {
		}
		return def;
	}*/
	private static boolean check_NRF(final String txt) {
		if(txt==null) { return false; }
		if(txt.length()==0) { return false; }
		try {
			Float.parseFloat(txt.trim());
		}catch(Exception e) {
			return false;
		}
		return true;
	}
	//-------------------------------------//

	public static Pane genPanel(final DevAT5350 dev){
		
		final Label txt_idfy = new Label();
		txt_idfy.textProperty().bind(dev.Identify[0]);
		
		final TextField box_meas_time = new TextField("1:30");
		final TextField box_meas_filt = new TextField();
		final TextField box_meas_damp = new TextField();
		final TextField box_meas_corr = new TextField();
		final TextField box_meas_volt = new TextField();
		final TextField box_meas_temp = new TextField();//0~60 degree
		final TextField box_meas_pres = new TextField();//50~140 kPa

		for(TextField box:new TextField[] {
			box_meas_time, box_meas_filt,
			box_meas_damp, box_meas_corr, 
			box_meas_volt, box_meas_temp, 
			box_meas_pres
		}) {
			box.setPrefWidth(137);
		}
		
		//final ComboBox<String> cmb_meas_range = new ComboBox<String>();
		//cmb_meas_range.getItems().addAll("LOW","MED","HIGH");
				
		final JFXButton btn_meas = new JFXButton("量測");		
		final JFXButton btn_corr = new JFXButton("補償");
		final JFXButton btn_attr = new JFXButton("參數");
		final JFXButton btn_stop = new JFXButton("中止");
		for(JFXButton bb:new JFXButton[] {
			btn_meas, btn_corr, btn_attr, btn_stop
		}) {			
			bb.setMaxWidth(Double.MAX_VALUE);
			if(bb!=btn_stop) {
				bb.disableProperty().bind(dev.isIdle.not());
			}
			HBox.setHgrow(bb, Priority.ALWAYS);
		}
		btn_meas.getStyleClass().add("btn-raised-1");
		btn_corr.getStyleClass().add("btn-raised-1");
		btn_attr.getStyleClass().add("btn-raised-1");
		btn_stop.getStyleClass().add("btn-raised-0");
		//btn_stop.disableProperty().bind(dev.isIdle);
		
		btn_meas.setOnAction(e->{
			dev.asyncMeasure(
				box_meas_time.getText(),
				box_meas_filt.getText(),
				box_meas_damp.getText(),
				box_meas_corr.getText(),
				box_meas_volt.getText(),
				box_meas_temp.getText(),
				box_meas_pres.getText(),
				()->{
					final SummaryStatistics ss = dev.lastSummary();
					final String txt =String.format(
						"AVG:%.2f %s, DEV:%.4f\n------------\n%s",
						ss.getMean(), DevAT5350.summaryUnit, 
						ss.getStandardDeviation(),
						dev.lastMeasure()
					);
					new PanDialog.ShowTextArea(txt)
					.setPrefSize(100, 400)
					.showAndWait();
				}
			);
		});
		btn_corr.setOnAction(e->dev.asyncCorrection());
		btn_stop.setOnAction(e->dev.syncAbort());

		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad","font-console");
		lay0.addRow(0, new Label("裝置識別:"), txt_idfy);
		lay0.addRow(1, new Label("量測時間:"), box_meas_time);
		lay0.addRow(2, new Label("Filter :"), box_meas_filt);
		lay0.addRow(3, new Label("Damper :"), box_meas_damp);
		lay0.addRow(4, new Label("量測補償:"), box_meas_corr);
		lay0.addRow(5, new Label("量測電壓:"), box_meas_volt);
		lay0.addRow(6, new Label("量測溫度:"), box_meas_temp);
		lay0.addRow(7, new Label("量測壓力:"), box_meas_pres);		
		lay0.add(btn_meas, 0, 8, 2, 1);
		lay0.add(btn_corr, 0, 9, 2, 1);
		lay0.add(btn_attr, 0,10, 2, 1);
		lay0.add(btn_stop, 0,11, 2, 1);
		return lay0;
	}
	//---------[deprecate]-----------//

	/**
	 * use 'CONF?' to get type and range:
	 * CURRent - 電流 [LOW|MEDium|HIGH]
	 * CHARge - 電荷 [LOW|HIGH]
	 * DRATe - Kerma rate [LOW|MEDium|HIGH]
	 * DOSE  - Kerma [LOW|HIGH]
	 * ICHarge- 累積電荷 integration of current [LOW|MEDium|HIGH]
	 * IDOSe  - 累積劑量 integration of kerma rate [LOW|MEDium|HIGH]
	 */

	/*private static void load_param(
		final DevAT5350 dev,
		final ComboBox<String> cmb,
		final ToggleButton[] opt,
		final TextField[] val
		
	){dev.asyncBreakIn(()->{
		dev.wxr("SYSTem:RWLock");//lock panel
		
		final boolean[] flg = new boolean[5];
		flg[0] = dev.get_flag(":DRATe:FILTer?");
		flg[1] = dev.get_flag(":DRATe:DAMPer?");
		flg[2] = dev.get_flag(":DRATe:CORRection?");
		flg[3] = dev.get_flag(":HVOLtage?");
		flg[4] = dev.get_flag(":FACTor?");
		
		final String[] txt = new String[8];
		txt[0] = ":"+dev.wxr("CONF?").replace("\"", "");
		txt[1] = dev.wxr(":DRATe:FILTer:VALue?");
		txt[2] = dev.wxr(":DRATe:DAMPer:VALue?");
		txt[3] = dev.wxr(":HVOLtage:VALue?");
		txt[4] = dev.wxr(":FACTor:TEMP?")
				.replace("\"", "")
				.replace("C", "")
				.trim();
		txt[5] = dev.wxr(":FACTor:PRES?")
				.replace("\"", "")
				.replace("kPa", "")
				.trim();
		txt[6] = dev.wxr(":TRIGger:COUNt?");
		txt[7] = dev.wxr(":TRIGger:ECOunt?");
		
		dev.wxr("SYSTem:LOCal\n");//unlock panel
	});}
	private static void save_param(
		final DevAT5350 dev,
		final ComboBox<String> cmb,
		final ToggleButton[] opt,
		final TextField[] val
	){
		dev.asyncBreakIn(()->{
		String res;
		res = dev.wxr("SYSTem:RWLock\n");//lock panel
		res = dev.wxr("SYSTem:LOCal\n");//unlock panel
	});}

	@SuppressWarnings("unchecked")
	private static TreeView<String> gen_SCPI_tree(final DevAT5350 dev) {
		final TreeItem<String> current = new TreeItem<String>("range");
		final TreeItem<String> root = new TreeItem<String>(":");
		root.setExpanded(true);
		root.getChildren().add(current);
		return new TreeView<String>(root);
	}*/
}
