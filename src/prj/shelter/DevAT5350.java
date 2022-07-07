package prj.shelter;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.StatUtils;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import jssc.SerialPort;
import jssc.SerialPortException;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.UtilPhysical;

public class DevAT5350 extends DevTTY {

	public DevAT5350(){
		TAG="AT5350";
	}
	@Override
	public void afterOpen() {
		addState(STA_IDENTIFY, stage_identify);
		addState(STA_GETSTATUS, stage_get_status);
		//playFlow(STA_IDENTIFY);
		playFlow("");//goto idle state~~~
	}
	@Override
	public void beforeClose(){		
	}
	
	public final StringProperty[] Identify ={ 
		new SimpleStringProperty("＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊"),
	};
	
	public final StringProperty[] Status ={ 
		new SimpleStringProperty("＊＊＊＊"),//STB
		new SimpleStringProperty("＊＊＊＊"),//SRE
	};
	
	private final String STA_IDENTIFY = "stage_identify";
	private final String STA_GETSTATUS= "stage_getstatus";
	
	@SuppressWarnings("restriction")
	private Runnable stage_identify = ()->{
		String[] idn = wxr("*IDN?")
			.replaceAll("[\n|\\s]", "")
			.split(",");
		Application.invokeLater(()->{
			Identify[0].setValue(idn[0]);
			Identify[1].setValue(idn[1]);
			Identify[2].setValue(idn[2]);
			Identify[3].setValue(idn[3]);
		});
		nextState(STA_GETSTATUS);
	};
	
	@SuppressWarnings("restriction")
	private Runnable stage_get_status = ()->{
		final String ss1 = wxr("*STB?");
		final String ss2 = wxr("*SRE?");
		Application.invokeLater(()->{
			Status[0].setValue(ss1);
			Status[1].setValue(ss2);
		});
		try {
			TimeUnit.MILLISECONDS.sleep(500);
		} catch (InterruptedException e) {
			return;
		}
		nextState(STA_GETSTATUS);
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
	//------------------------------------------------
	
	public void asyncCompensate(){asyncBreakIn(()->{
		wxr(":CORRection:AUTO");
	});}
	public void asyncAbort(){asyncBreakIn(()->{
		wxr("ABOR");
	});}
	
	/**
	 * keep the result of last measurement.<p>
	 */
	public String lastMeasure = "";

	public void asyncMeasure(){
		working = true;
		asyncBreakIn(()->{
			//simulation~~~			
			lastMeasure = "\"+1.3844E-04 Sv/min #800\",\"+1.3842E-04 Sv/min #900\",\"+1.3840E-04 Sv/min #1000\"";
			working = false;
		});
	}
	
	private boolean working = false;//TODO: simulation
	
	public boolean isWorking() {
		return working;
	}
	
	
	/**
	 * @param configue - measurement type and range.<p>
	 * @param count - number of measurement result.<p>
	 * @param ecount - order number of measurement result.<p>
	 * @param useOption - flag for below lines:<p>
	 * 		Filter, damper, High-Voltage, Correction, Factor.<p>
	 * @param valOption - value for below lines:<p>
	 * 		Filter, damper, High-Voltage, Temperature, Pressure.<p>
	 */
	/*public void asyncMeasure(){asyncBreakIn(()->{
		int coun,eco;
		try{
			coun= Integer.valueOf(wxr("TRIG:COUN?\n"));
			eco = Integer.valueOf(wxr("TRIG:ECO?\n"));
		}catch(NumberFormatException e){
			Misc.loge("[%s] ECOunt or COUNt fail!!", TAG);
			return;
		}
		wxr("INIT\n");//delay = ECO * COUN * 0.1sec
		try {
			Thread.sleep(1000*(1+(coun*eco)/10));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		lastMeasure = wxr("FETC:ARR? "+coun);
		split_data();
		double avg = StatUtils.mean(data_value);
		double sig = Math.sqrt(StatUtils.variance(data_value));
		//change SI scale
		final String data_value = UtilPhysical.addPrefix(avg);
	});}*/

	private boolean get_flag(final String cmd){
		String res = wxr(cmd);
		try{
			if(Integer.valueOf(res)!=0){
				return true;
			}
		}catch(NumberFormatException e){
			Misc.loge(
				"[%s] boolean fail!! %s --> %s",
				TAG,cmd,res
			);
		}
		return false;
	}

	private String[] split_measurement_text(String txt){
		ArrayList<String> vals = new ArrayList<String>(10);
		int beg, end;
		char quo = '"';
		do{
			beg = txt.indexOf(quo);
			if(beg<0){
				break;
			}
			end = txt.indexOf(quo, beg+1);
			if(end<0){
				break;
			}
			vals.add(txt.substring(beg+1, end));
			txt = txt.substring(end+1);
		}while(true);
		return vals.toArray(new String[0]);
	}
	
	private double[] data_value;
	private String data_unit;
	
	private void split_data(){
		String[] vals = lastMeasure.split(",");
		for(String txt:vals) {
			String[] cols = txt.replace("\"", "").split("\\s");
			double numb = Double.valueOf(cols[0].trim());
			String unit = cols[1].trim();
			Misc.logv("%f", numb);
		}
	}
	//-------------------------------------//
	
	/**
	 * use 'CONF?' to get type and range:
	 * CURRent - 電流 [LOW|MEDium|HIGH]
	 * CHARge - 電荷 [LOW|HIGH]
	 * DRATe - Kerma rate [LOW|MEDium|HIGH]
	 * DOSE  - Kerma [LOW|HIGH]
	 * ICHarge- 累積電荷 integration of current [LOW|MEDium|HIGH]
	 * IDOSe  - 累積劑量 integration of kerma rate [LOW|MEDium|HIGH]
	 * Measurement range:
	 * HIGH  -  1.7 Sv/min
	 * MEDium- 17  mSv/min
	 * LOW   -179  uSv/min
	 * 
	 * Filter state(on or off) and value.<p>
	 * Only used in :CURRent|:DRATe.<p>
	 * 
	 * Damper state(on or off) and value.<p>
	 * Only used in :CURRent|:DRATe.<p>
	 * 
	 * Correction state(on or off) for measuring zero.<p>
	 * Only used in :CURRent|:DRATe.<p>
	 * 
	 * Adjusting factor(on or off), Temperature and Pressure.<p>
	 * 
	 * high voltage source(on or off).<p>
	 */

	private static void load_param(
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
		Application.invokeAndWait(()->{
			cmb.getSelectionModel().select(txt[0]);
			for(int i=0; i<flg.length; i++){
				opt[i].setSelected(flg[i]);
			}
			for(int i=1; i<txt.length; i++){
				val[i-1].setText(txt[i]);
			}
		});
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

	//AT5350 example:
	//SEND --> FETC:ARR? 
	//RECV --> "-1.1279E-08 Sv/min #774","-1.1279E-08 Sv/min #775","-1.1097E-08 Sv/min #776","-1.1097E-08 Sv/min #777","-1.1097E-08 Sv/min #778","-1.1097E-08 Sv/min #779","-1.1097E-08 Sv/min #780","-1.1279E-08 Sv/min #781","-1.1279E-08 Sv/min #782","-1.1279E-08 Sv/min #783","-1.1279E-08 Sv/min #784","-1.1279E-08 Sv/min #785","-1.1279E-08 Sv/min #786","-1.1097E-08 Sv/min #787","-1.1279E-08 Sv/min #788","-1.1097E-08 Sv/min #789","-1.1097E-08 Sv/min #790","-1.1279E-08 Sv/min #791","-1.1097E-08 Sv/min #792","-1.1097E-08 Sv/min #793"

	public static Pane genPanel(final DevAT5350 dev){
		
		final TreeItem<String> fact1 = new TreeItem<String>("TEMP");
		final TreeItem<String> fact2 = new TreeItem<String>("PRES");
		
		final TreeItem<String> syst1 = new TreeItem<String>("RWLock");
		final TreeItem<String> syst2 = new TreeItem<String>("LOCal");
		
		final TreeItem<String> drat1 = new TreeItem<String>("FILTer");
		final TreeItem<String> drat2 = new TreeItem<String>("DAMPer");
		final TreeItem<String> drat3 = new TreeItem<String>("CORRection");
		
		final TreeItem<String> colon1 = new TreeItem<String>("DRATe");
		colon1.getChildren().addAll(drat1,drat2,drat3);
		
		final TreeItem<String> colon2 = new TreeItem<String>("HVOLtage");
		
		final TreeItem<String> colon3 = new TreeItem<String>("FACTor");
		colon3.getChildren().addAll(fact1,fact2);
		
		final TreeItem<String> colon4 = new TreeItem<String>("TRIGger");
		
		final TreeItem<String> colon5 = new TreeItem<String>("SYSTem");
		colon5.getChildren().addAll(syst1,syst2);
		
		final TreeItem<String> root = new TreeItem<String>(":");
		root.setExpanded(true);
		root.getChildren().addAll(colon1,colon2,colon3,colon4,colon5);
		
		final Label[] txt = {
			new Label(), new Label(),
			new Label(), new Label(),
		};
		txt[0].textProperty().bind(dev.Identify[0]);
		txt[1].textProperty().bind(dev.Identify[1]);
		txt[2].textProperty().bind(dev.Status[0]);
		txt[3].textProperty().bind(dev.Status[1]);
		
		final JFXButton[] btn = {
			new JFXButton("test-1"),
			new JFXButton("test-2")
		};
		for(JFXButton bb:btn) {
			bb.getStyleClass().add("btn-raised-1");
			bb.setMaxWidth(Double.MAX_VALUE);
			HBox.setHgrow(bb, Priority.ALWAYS);
		}
		final HBox lay1 = new HBox(btn);
		lay1.getStyleClass().addAll("box-pad","font-console");
		
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad","font-console");
		lay0.addRow(0, new Label("型號識別:"), new HBox(txt[0],txt[1]));
		lay0.addRow(1, new Label("狀態-1:"), txt[2]);
		lay0.addRow(2, new Label("狀態-2:"), txt[3]);
		lay0.add(new TreeView<String>(root), 0, 3, 2, 1);
		lay0.add(lay1, 0, 4, 2, 1);
		return lay0;
	}
}
