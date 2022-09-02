package prj.shelter;

import java.util.ArrayList;
import java.util.Collections;

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
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanDialog;
import narl.itrc.UtilPhysical;

/**
 * ATOMTEX AT5350, dosimeter. 
 * this device follow IEEE488.2 standard.
 * @author qq
 *
 */

public class DevAT5350 extends DevTTY {

	public DevAT5350(){
		TAG="AT5350";
		
		final String f_mode = Gawain.prop().getProperty("AT5350_FETCH","last20");
		if(f_mode.equals("each")==true) {
			fetch_mode = FETCH_MODE.F_EACH;
		}else if(f_mode.equals("last20")==true) {
			fetch_mode = FETCH_MODE.F_LAST20;
		}else if(f_mode.equals("cook1")==true) {
			fetch_mode = FETCH_MODE.F_COOK1;
		}else if(f_mode.equals("cook2")==true) {
			fetch_mode = FETCH_MODE.F_COOK2;
		}else {
			Misc.logw("[%s] invalid fecth mode: %s (it should be 'each', 'last20' 'cook1' or 'cook2')", f_mode);
		}
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

		wxr("CONF:DRAT LOW");//":DART LOW" is invalid???
		wxr(":DRAT:FILT ON");
		//wxr(":DRAT:FILT 100");
		wxr(":DRAT:DAMP OFF");//這個會影響計時!!!
		//wxr(":DRAT:CORR ON");//no work??
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
		
		//don't block device in other state
		//After sending this command,
		//tty reading will be blocked!!!
		block_sleep_msec(3*60*1000);//magic trick for preventing device crazy~~~
		wxr_log("*OPC?");

		Application.invokeLater(()->isIdle.set(true));
		nextState("");
	};
	
	private static enum FETCH_MODE { 
		F_EACH, F_LAST20, F_COOK1, F_COOK2,
	};
	private String meas_time = "";//unit is second~~~
	private String meas_filt = "";
	private String meas_rang = "";//range
	private String meas_damp = "";	
	private String meas_corr = "";
	private String meas_volt = "";
	private String meas_temp = "";
	private String meas_pres = "";//kPa, 1atm=100kPa
	private Runnable fetch_after = null;
	
	//Fetch data format is like:
	//"\"+1.3844E-04 Sv/min #800\",\"+1.3842E-04 Sv/min #900\",\"+1.3840E-04 Sv/min #1000\"";	
	//text will be cook, remove '"' and ','
	private FETCH_MODE fetch_mode = FETCH_MODE.F_LAST20;//default value~~~
	private String fetch_array="";
	private final SummaryStatistics fetch_summary = new SummaryStatistics();

	private Runnable stage_measure = ()->{
		//final long t1 = System.currentTimeMillis();

		//apply all settings!!!!!
		if(check_NRF(meas_filt)==true) {
			wxr(":DRAT:FILT ON");
			wxr(":DRAT:FILT:VAL "+meas_filt);
		}
		if(meas_rang.length()!=0) {
			wxr("CONF:DRAT "+meas_rang);
		}
		/*if(check_NRF(meas_damp)==true) {
			wxr(":DAMP ON");
			wxr(":DAMP:VAL "+meas_damp);
		}
		if(meas_corr.length()!=0) {
			wxr(":CORR ON");
		}
		if(check_NRF(meas_volt)==true) {
			wxr(":HVOL ON");
			wxr(":HVOL:VAL "+meas_volt);
		}*/
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
			final long samp = Misc.text2tick(meas_time)/100L;//1 sample = 0.1 second
			if(samp<=60) {
				count = samp;
				every = 1;
			}else {
				count = 60;
				every = samp/count;
			}
			wxr("TRIG:COUN "+count);
			wxr("TRIG:ECO  "+every);
		}	
		wxr("INIT");
		wxr("*TRG");

		Misc.logv("[%s] count=%d, every=%d, flt=%s, rang=%s, temp=%s, pres=%s", 
			TAG, count, every,
			meas_filt, meas_rang,
			meas_temp, meas_pres
		);
		
		//don't block device in other state
		//After sending this command,
		//tty reading will be blocked!!!
		block_sleep_msec(count*every+1000);//magic trick for preventing device crazy~~~
		wxr_log("*OPC?");
		
		switch(fetch_mode) {
		case F_COOK1:		
		case F_EACH: 
			fetch_array = wxr(String.format("FETC:ARR? %d", count)); 
			break;
		default:
		case F_COOK2:
		case F_LAST20:
			fetch_array = wxr(String.format("FETC:ARR? %d", (count<20)?(count):(20)));
			break;
		}
		fetch_array = fetch_array.replace('"', ' ').replace(',', '\n');
		summary_data();
		
		Misc.logv("[%s][fetch] avg=%.2f, cv=%.2f", 
			TAG, 
			fetch_summary.getMean(),
			fetch_summary.getStandardDeviation()
		);
		
		Application.invokeLater(()->{
			isIdle.set(true);
			if(fetch_after!=null) {
				fetch_after.run();
			}
			//reset all parameters for next turn~~~~
			meas_time = "";
			meas_filt = "";
			meas_rang = "";
			meas_damp = "";	
			meas_corr = "";
			meas_volt = "";
			meas_temp = "";
			meas_pres = "";
			fetch_after= null;
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
				return recv;//no query, just go back~~~
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
	//--------------------------------------------

	private void summary_data() {
		fetch_summary.clear();//reset old data~~~
		if(fetch_array.length()==0) {
			return;
		}
		ArrayList<Double> lst = new ArrayList<Double>();
		for(String txt:fetch_array.split("\n")) {
			final int pos = txt.indexOf("#");
			if(pos>=0) {
				txt = txt.substring(0,pos);
			}
			final String val = UtilPhysical.convertScale(txt.trim(), "uSv/hr");
			if(val.length()==0) {
				continue;
			}
			lst.add(Double.parseDouble(val));			
		}		
		switch(fetch_mode) {
		default:
		case F_EACH:
		case F_LAST20:
			for(Double v:lst) {
				fetch_summary.addValue(v);
			}
			break;			
		case F_COOK1:		
			cook_data(lst,20);
			break;
		case F_COOK2:
			cook_data(lst,10);
			break;
		}
	}
	private void cook_data(ArrayList<Double> lst, final int size) {
		Collections.sort(lst);
		final int end = lst.size()-size;
		if(end<=0) { return; }
		int beg_array = -1;
		double per_sigma = Double.MAX_VALUE;
		for(int i=0; i<=end; i++) {
			SummaryStatistics ss = new SummaryStatistics();			
			for(int j=i; j<(i+size); j++) {
				ss.addValue(lst.get(j));
			}
			final double ps = Math.abs(ss.getStandardDeviation() / ss.getMean());
			if(ps<per_sigma) {
				per_sigma = ps;
				beg_array = i;
			}
		}
		if(beg_array<0) {
			return;//is it possible???
		}
		
		String s_txt = "";
		for(int j=beg_array; j<(beg_array+size); j++) {
			s_txt = s_txt+ String.format("%.3f uSv/hr #--\n", lst.get(j));		
			fetch_summary.addValue(lst.get(j));
		}
		fetch_array = s_txt;
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
		final String rang,	
		//final String corr,
		//final String volt,
		final String temp,
		final String pres,
		final Runnable event
	){
		isIdle.set(false);
		asyncBreakIn(()->{
		meas_time = time;
		meas_filt = filt;
		meas_rang = rang;
		//meas_damp = damp;
		//meas_corr = corr;
		//meas_volt = volt;
		meas_temp = temp;
		meas_pres = pres;
		fetch_after= event;
		nextState("stage_measure");
	});}
	public void asyncMeasure(){
		asyncMeasure("30","","","","",null);
	}
	public void asyncMeasure(
		final String time
	){
		asyncMeasure(time,"","","","",null);
	}
	public void asyncMeasure(
		final String time,
		final String temp,
		final String pres
	){
		asyncMeasure(time,"","",temp,pres,null);
	}
	public void asyncPopMeasure(
		final String time,
		final String temp,
		final String pres		
	){
		final Runnable event = ()->{
			final SummaryStatistics ss = lastSummary();
			final String txt =String.format(
				"AVG:%.3f %s ± %.3f\n------------\n%s",
				ss.getMean(), "uSv/hr", ss.getStandardDeviation(),
				lastMeasure()
			);
			new PanDialog.ShowTextArea(txt)
			.setPrefSize(100, 400)
			.showAndWait();
		};
		asyncMeasure(time,"","",temp,pres,event);
	}
	
	public void syncAbort(){
		wxr("ABOR");
	}
	
	/**
	 * get last measurement, the result will be cook, pattern is below:
	 * -1.1279E-08 Sv/min #774
	 * -1.1279E-08 Sv/min #775
	 * ....
	 * @return: text for dose rate value
	 */
	public String lastMeasure() {		
		return new String(fetch_array);
	}
	public SummaryStatistics lastSummary() {
		return new SummaryStatistics(fetch_summary);
	}
	public Object[] lastResult() {
		return new Object[] {
			lastMeasure(), lastSummary(),
		};
	}
	//--------------------------------------
	
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
		
		final TextField box_meas_time = new TextField("2:00");
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
				"",
				box_meas_temp.getText(),
				box_meas_pres.getText(),
				()->{
					final SummaryStatistics ss = dev.lastSummary();
					final String txt =String.format(
						"AVG:%.3f %s ± %.3f\n------------\n%s",
						ss.getMean(), "uSv/hr", ss.getStandardDeviation(),
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
		lay0.addRow(3, new Label("量測溫度:"), box_meas_temp);
		lay0.addRow(4, new Label("量測壓力:"), box_meas_pres);		
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
}
