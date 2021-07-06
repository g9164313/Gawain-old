package prj.shelter;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.StatUtils;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.UtilPhysical;

public class DevAT5350 extends DevTTY {

	public DevAT5350(){
		TAG="AT5350";
		readTimeout = 3000;
	}
	public DevAT5350(final String path){
		this();
		setPathName(path);
	}

	protected void afterOpen() {
		playFlow("");
		asyncBreakIn(()->{
			String[] idn = wxr("*IDN?")
				.replaceAll("[\n|\\s]", "")
				.split(",");
			//wxr("TRIG:COUN 5");
			//Misc.logv("STB=%s", wxr("*STB?"));
			//Misc.logv("SRE=%s", wxr("*SRE?"));
			Application.invokeAndWait(()->{
				Identify[0].setValue(idn[0]);
				Identify[1].setValue(idn[1]);
				Identify[2].setValue(idn[2]);
				Identify[3].setValue(idn[3]);
			});	
		});
	}
	
	public final StringProperty[] Identify ={ 
		new SimpleStringProperty("＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊"),
	};
	
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
	 */
	/**
	 * Filter state(on or off) and value.<p>
	 * Only used in :CURRent|:DRATe.<p>
	 */
	/**
	 * Damper state(on or off) and value.<p>
	 * Only used in :CURRent|:DRATe.<p>
	 */
	/**
	 * Correction state(on or off) for measuring zero.<p>
	 * Only used in :CURRent|:DRATe.<p>
	 */
	/**
	 * Adjusting factor(on or off), Temperature and Pressure.<p>
	 */
	/**
	 * high voltage source(on or off).<p>
	 */

	public void compensate(){
		wxr(":CORRection:AUTO\n");
	}
	public void abort(){asyncBreakIn(()->{
		wxr("ABOR\n");
	});}
	
	/**
	 * keep the result of last measurement.<p>
	 */
	public String lastMeasure;
	/**
	 * average dose rate from last measurement.<p>
	 * value and unit.<p>
	 */
	public final StringProperty avgDose= new SimpleStringProperty("＊＊＊＊");
	/**
	 * standard deviation dose rate from last measurement.<p>
	 */
	public final StringProperty devDose= new SimpleStringProperty("＊＊＊＊");
	
	/**
	 * @param configue - measurement type and range.<p>
	 * @param count - number of measurement result.<p>
	 * @param ecount - order number of measurement result.<p>
	 * @param useOption - flag for below lines:<p>
	 * 		Filter, damper, High-Voltage, Correction, Factor.<p>
	 * @param valOption - value for below lines:<p>
	 * 		Filter, damper, High-Voltage, Temperature, Pressure.<p>
	 */
	public void measure(){asyncBreakIn(()->{
		
		String res;
		int coun,eco;
		try{
			coun= Integer.valueOf(wxr("TRIG:COUN?"));
			eco = Integer.valueOf(wxr("TRIG:ECO?"));
		}catch(NumberFormatException e){
			Misc.loge("[%s] ECOunt or COUNt fail!!", TAG);
			return;
		}
		wxr("INIT");//delay = ECO * COUN * 0.1sec
		try {
			Thread.sleep(1000*(1+(coun*eco)/10));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		lastMeasure = wxr("FETC:ARR? "+coun);
		split_data();
		double avg = StatUtils.mean(data_value);
		double sig = Math.sqrt(StatUtils.variance(data_value));
		//change UI scale
		final String data_value = UtilPhysical.addPrefix(avg);
		
		Application.invokeAndWait(()->{
			avgDose.setValue(String.format("%s%s", data_value, data_unit));
			devDose.setValue(String.format("%E", sig));
		});
	});}

	private static boolean verbose = true;
	
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
	private String wxr(String cmd){
		if(cmd.endsWith("\n")==false){
			cmd = cmd + "\n";
		}
		for(char cc:cmd.toCharArray()) {
			writeByte(cc);
			cc = (char)readByte();
			//Misc.logv("recv=%c(%d)",cc,(int)cc);
		}
		String res = "";
		if(cmd.contains("?")==false) {
			return res;
		}
		char rr=0;
		while(true) {
			rr = (char)readByte();
			if(rr==0x0A) { break; }
			res = res + (char)rr;
		}
		return res;	
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
	
	private final static StringConverter<String> conf_name = new StringConverter<String>(){
		@Override
		public String toString(String object) {
			String txt = object.replace("\\s", "-")
				.replace(":IDOSe","累積劑量")
				.replace(":ICHarge","累積電荷")
				.replace(":DRATe","劑量率")
				.replace(":DOSE","劑量")
				.replace(":CHARge","電荷")
				.replace(":CURRent","電流");
			return txt;
		}
		@Override
		public String fromString(String string) {
			return "";
		}
	};
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
	private static void reset_param(
		final DevAT5350 dev,
		final ComboBox<String> cmb,
		final ToggleButton[] opt,
		final TextField[] val
	){
		
	}
	
	//AT5350 example:
	//SEND --> FETC:ARR? 
	//RECV --> "-1.1279E-08 Sv/min #774","-1.1279E-08 Sv/min #775","-1.1097E-08 Sv/min #776","-1.1097E-08 Sv/min #777","-1.1097E-08 Sv/min #778","-1.1097E-08 Sv/min #779","-1.1097E-08 Sv/min #780","-1.1279E-08 Sv/min #781","-1.1279E-08 Sv/min #782","-1.1279E-08 Sv/min #783","-1.1279E-08 Sv/min #784","-1.1279E-08 Sv/min #785","-1.1279E-08 Sv/min #786","-1.1097E-08 Sv/min #787","-1.1279E-08 Sv/min #788","-1.1097E-08 Sv/min #789","-1.1097E-08 Sv/min #790","-1.1279E-08 Sv/min #791","-1.1097E-08 Sv/min #792","-1.1097E-08 Sv/min #793"

	public static Pane genPanel(final DevAT5350 dev){
		
		Label name = new Label();
		name.textProperty().bind(dev.Identify[1]);
		
		final JFXComboBox<String> cmb = new JFXComboBox<String>();
		cmb.getItems().addAll(
			":CHARge LOW", ":CHARge HIGH",
			":DOSE LOW", ":DOSE HIGH",
			":CURRent LOW", ":CURRent MED", ":CURRent HIGH",
			":DRATe LOW", ":DRATe MED", ":DRATe HIGH",
			":ICHarge LOW", ":ICHarge MED", ":ICHarge HIGH",
			":IDOSe LOW", ":IDOSe MED", ":IDOSe HIGH"
		);
		cmb.getSelectionModel().select(7);
		cmb.setConverter(conf_name);
		
		final JFXToggleButton[] tgl = {
			new JFXToggleButton(),//FILTer
			new JFXToggleButton(),//DAMPer
			new JFXToggleButton(),//HVOLtage
			new JFXToggleButton(),//CORRection
			new JFXToggleButton(),//FACTor
		};
		final JFXTextField[] box = {
			new JFXTextField("600"),//FILTer:VALue - 1~3000, unit is 100ms
			new JFXTextField(),//DAMPer:VALue - 1~99999
			new JFXTextField("400"),//HVOLtage:VALue - -500~+500 Voltage
			new JFXTextField(),//FACTor:TEMPerature:VALue 0~60 C
			new JFXTextField(),//FACTor:PRESsure:VALue 50~140 kPa
			new JFXTextField("30"),//TRIGger:COUNt 1~500, results in dosimeter
			new JFXTextField("100"),//TRIGger:ECOunt 1~2^31, every tenth result, this will be 10
		};
		for(JFXTextField obj:box){
			obj.setPrefWidth(90);
		}
		box[0].setPromptText("平均(0.1s)");
		box[0].setLabelFloat(true);
		box[0].disableProperty().bind(tgl[0].selectedProperty().not());
		box[1].disableProperty().bind(tgl[1].selectedProperty().not());
		box[2].disableProperty().bind(tgl[2].selectedProperty().not());
		box[3].setPromptText("溫度");
		box[3].disableProperty().bind(tgl[4].selectedProperty().not());
		box[3].setLabelFloat(true);
		box[4].setPromptText("壓力");
		box[4].setLabelFloat(true);
		box[4].disableProperty().bind(tgl[4].selectedProperty().not());
		
		JFXButton[] btn = new JFXButton[6];
		for(int i=0; i<btn.length; i++){
			JFXButton obj = new JFXButton();
			obj.setMaxWidth(Double.MAX_VALUE);
			btn[i] = obj;
		}
		
		btn[0].setText("讀取量測");
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setOnAction(e->{
			/*String conf = cmb.getSelectionModel().getSelectedItem();
			boolean[] use = {
				tgl[0].isSelected(),
				tgl[1].isSelected(),
				tgl[2].isSelected(),
				tgl[3].isSelected(),
				tgl[4].isSelected()
			};
			String[] val = {
				box[0].getText().trim(),
				box[1].getText().trim(),
				box[2].getText().trim(),
				box[3].getText().trim(),
				box[4].getText().trim()
			};
			int _cnt=20, ecnt=10;
			try{
				_cnt = Integer.valueOf(box[5].getText().trim());
				ecnt = Integer.valueOf(box[5].getText().trim());
			}catch(NumberFormatException exp){
				return;
			}*/
			dev.measure();
		});
		
		btn[1].setText("testing~~~");
		btn[1].getStyleClass().add("btn-raised-1");
		btn[1].setOnAction(e->{
			dev.lastMeasure =Gawain.prop().getProperty("lastmeas");
			dev.split_data();
			//dev.asyncBreakIn(()->{
				//String txt = dev.wxr("FETC:ARR? "+20);

			//});
		});
		
		btn[2].setText("下載參數");
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setOnAction(e->load_param(dev,cmb,tgl,box));
		
		btn[3].setText("上傳參數");
		btn[3].getStyleClass().add("btn-raised-2");
		btn[3].setOnAction(e->save_param(dev,cmb,tgl,box));
		
		btn[4].setText("預設參數");
		btn[4].getStyleClass().add("btn-raised-2");
		btn[4].setOnAction(e->reset_param(dev,cmb,tgl,box));
				
		btn[5].setText("放棄操作");
		btn[5].getStyleClass().add("btn-raised-3");
		btn[5].setOnAction(e->dev.abort());
		
		final GridPane lay1  =new GridPane();
		lay1.getStyleClass().addAll("box-pad","font-console");
		lay1.add(name, 0, 0, 3, 1);//vendor name
		lay1.add(cmb, 0, 1, 3, 1);
		lay1.addColumn(0, 
			new Label("Filter"), 
			new Label("Damper"),
			new Label("高壓電源"),
			new Label("自動修正"),
			new Label("校正因子"),
			new Label(""),
			new Label("Count"),
			new Label("ECount")
		);
		lay1.addColumn(1, 
			tgl[0], tgl[1],tgl[2],
			tgl[3],
			tgl[4]
		);
		lay1.addColumn(2,
			box[0],box[1],box[2],
			new Label(),
			box[3],box[4],
			box[5],box[6]
		);
		final VBox lay2 = new VBox();
		lay2.getChildren().add(lay1);
		lay2.getChildren().addAll(btn);
		lay2.getStyleClass().addAll("box-pad");
		
		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay1);
		lay0.setRight(lay2);
		return lay0;
	}
	
	private static void compensate(
		final Node obj, 
		final DevAT5350 dev
	){
		PanBase pan = PanBase.self(obj);
		Task<?> tsk = new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				updateMessage("開始高壓補償");
				dev.compensate();
				int sec = 3 * 60; 
				do{
					Thread.sleep(1000);
					sec-=1;
					updateMessage("剩餘時間 "+Misc.tick2text(sec*1000));
				}while(sec>0);
				return null;
			}
		};
		pan.notifyTask(dev.TAG+"-高壓補償", tsk);
	}	
}
