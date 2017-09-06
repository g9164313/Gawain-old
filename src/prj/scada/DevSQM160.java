package prj.scada;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import com.sun.glass.ui.Application;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import narl.itrc.BoxValFloat;
import narl.itrc.BoxValInteger;
import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.GrpToogle;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;

/**
 * INFICON SQM-160 Multi-Film Rate/Thickness Monitor
 * Default port setting are "19200,8n1"
 * @author qq
 *
 */
public class DevSQM160 extends DevTTY {
	
	public DevSQM160(String path) {
		this();
		connect(path);
	}

	public DevSQM160() {
	}
	
	/**
	 * connect TTY port and prepare to send commands.<p>
	 * @param path - device name or full name
	 * @return TRUE-success,<p> FALSE-fail to open TTY port
	 */
	public boolean connect(String path){
		return (open(path)>0L)?(true):(false);
	}
	
	/**
	 * Convenient way to connect device.It is same as 'connect(path)', but path is got from property file.
	 * @return - true/false
	 */
	public boolean connect(){
		String path = Gawain.prop.getProperty("DevSQM160", null);
		if(path==null){
			return false;
		}
		return connect(path);
	}
	
	/**
	 * just close TTY device
	 */
	public void disconnect(){
		close();
	}
	//--------------------------------//
	
	private final String STA_A = "Command understood, normal response";
	private final String STA_C = "Invalid command";
	private final String STA_D = "Problem with data in command";
	private final String STA_X = "--------";
	private String lastCommand = "";
	//private String lastResponse= "";
	private String lastStatus  = STA_X;
	
	private StringProperty propLastStatus = new SimpleStringProperty(STA_X);
	
	public String[] exec_arg(String cmd){
		String txt = exec(cmd);
		return txt.split("\\s+");
	}
	
	public String exec(String cmd){
		return exec(cmd,true);
	}
	public String exec(String cmd,boolean sync){
		if(PanSputter.DBG==true){
			return "";
		}
		send_command(cmd,sync);
		String res = have_response();
		if(Application.isEventThread()==true){
			//lastResponse = res;
			propLastStatus.set(String.format("%s - (%s)", lastStatus, lastCommand));
		}
		return res;
	}

	private void send_command(String cmd,boolean sync){
		if(Application.isEventThread()==true){
			lastCommand = cmd;
		}
		char len = (char) (cmd.length() + 34);
		cmd = len + cmd;
		short val = calc_CRC(cmd.toCharArray());
		if(sync==true){
			cmd = '!' + cmd;
		}		
		writeTxt(cmd);
		//It is strange!!!, we must send command first, then put CRC
		byte[] buf = {
			(byte)(crcLow(val)),
			(byte)(crcHigh(val))
		};
		writeBuf(buf);
	}
	
	private String have_response(){
		char tkn;
		short val1,val2;
		
		//first, wait 'Sync' character
		do{
			tkn = readChar();
		}while(tkn!='!');
		
		//second byte is the length of response message.
		tkn = readChar();
		val2 = (short)(tkn-34-1);
		String resp = "";
		for(val1=0; val1<val2; val1++){
			tkn = readChar();
			if(val1==0){
				//the first character is response status~~~
				switch(tkn){
				case 'A': lastStatus= STA_A; break;
				case 'C': lastStatus= STA_C; break;
				case 'D': lastStatus= STA_D; break;
				default: lastStatus = STA_X; break;
				}
			}
			resp = resp + tkn;
		}
		
		//get the final CRC code
		tkn = readChar();
		val1 = (short) (val1 + (int)tkn - 34);
		tkn = readChar();
		val2 = (short) (val1 + (int)tkn - 34);
		val2 = (short)(val2 << 8);
		
		val1 = (short)(val2 | val1);
		val2 = calc_CRC(resp.toCharArray());
		
		if(val1!=val2){
			//how to deal with this condition????
		}
		return resp;
	}
	
	/**
	 * calculate CRC value, Attention, Message only contains length and  
	 * @param msg - command, excluding Sync and CRC
	 * @return CRC value
	 */
	private short calc_CRC(char[] msg) {
		short crc = 0;
		short tmpCRC;
		if (msg.length > 0) {
			crc = (short) 0x3fff;
			for (int jx=0; jx<msg.length; jx++) {
				crc = (short) (crc ^ (short) msg[jx]);
				for (int ix = 0; ix < 8; ix++) {
					tmpCRC = crc;
					crc = (short) (crc >> 1);
					if ((tmpCRC & 0x1) == 1) {
						crc = (short) (crc ^ 0x2001);
					}
				}
				crc = (short) (crc & 0x3fff);
			}
		}
		return crc;
	}

	private short crcLow(short crc) {
		short val = (short) ((crc & 0x7f) + 34);
		return val;
	}
	
	private short crcHigh(short crc) {
		short val = (short) (((crc >> 7) & 0x7f) + 34);
		return val;
	}
	//--------------------------------//
	/*@Override
	protected boolean taskStart(){
		return isOpen();
	}
	@Override
	protected boolean taskLooper(){
		final String rate = exec("M").substring(1).trim();
		final String thick= exec("O").substring(1).trim();
		avgRate = Float.valueOf(rate);
		avgThick= Float.valueOf(thick);
		Misc.invoke(event->{
			propAvgRate.set(rate);
			propAvgThick.set(thick);
		});
		return true;
	}*/
	@Override
	protected void timeLooper(){
		final String rate = exec("M").substring(1).trim();
		final String thick= exec("O").substring(1).trim();
		avgRate = Float.valueOf(rate);
		avgThick= Float.valueOf(thick);
		propAvgRate.set(rate);
		propAvgThick.set(thick);
	}
	public void startMonitor(){
		//super.startTaskMonitor("Monitor-SQM160", 1000);
		super.startTimeMonitor(1000);
	}
	//--------------------------------//
	
	private void refresh_film(int idx){
		String res = exec(String.format("A%d?", idx));
		if(res.length()<=8){
			clear_info_flim();
			return;//it must be debug-mode
		}
		String name = res.substring(1,8);
		String[] arg= res.substring(8).trim().split("\\s+");

		boxFilmName.setText(name);
		boxDensity.set(Float.valueOf(arg[0]));
		boxFilmTooling.set(Integer.valueOf(arg[1]));
		boxZRatio.set(Float.valueOf(arg[2]));
		boxFinalThick.set(Float.valueOf(arg[3]));
		boxSetPoint1.set(Float.valueOf(arg[4]));
		boxSetPoint2.set(Integer.valueOf(arg[5]));
		int flag = Integer.valueOf(arg[6]);
		for(int i=0; i<6; i++){
			if((flag&(1<<i))!=0){
				chkSensor[i].setSelected(true);
			}else{
				chkSensor[i].setSelected(false);
			}
		}
	}
	private void clear_info_flim(){
		boxFilmName.setText("???");
		boxDensity.clear();
		boxFilmTooling.clear();
		boxZRatio.clear();
		boxFinalThick.clear();
		boxSetPoint1.clear();
		boxSetPoint2.clear();
		for(int i=0; i<6; i++){
			chkSensor[i].setSelected(false);
		}
	}
	private void rewrite_flim(){
		int idx = 1+cmbFilmIdx.getSelectionModel().getSelectedIndex();
		String name = boxFilmName.getText()
			.toUpperCase();
		if(name.length()<=8){
			int cnt = 8 - name.length();
			for(int i=0; i<cnt; i++){
				name = name + " ";
			}
		}else{
			name = name.substring(0,8);
		}
		int flag = 0;
		for(int i=0; i<6; i++){
			if(chkSensor[i].isSelected()==true){
				flag = flag | (1<<i);
			}
		}
		String cmd = String.format(
			"A%d%s %.2f %d %.3f %.3f %.3f %d %d",
			idx, name,
			boxDensity.get(),
			boxFilmTooling.get(),
			boxZRatio.get(),
			boxFinalThick.get(),
			boxSetPoint1.get(),
			boxSetPoint2.get(),
			flag
		);
		exec(cmd);//update film information
	}
	
	private void refresh_sys1(){
		String[] arg = exec_arg("B?");
		if(arg.length==1){
			return;
		}
		boxTimeBase.set(Float.valueOf(arg[1]));
		chkSimulateMode.setSelected((Integer.valueOf(arg[2])==0)?(false):(true));		
		grpDiaplayMode.select(Integer.valueOf(arg[3]));		
		chkRateResolution.setSelected((Integer.valueOf(arg[4])==0)?(false):(true));
		boxRateFilter.set(Integer.valueOf(arg[5]));
		for(int i=0; i<boxCrystalTooling.length; i++){
			boxCrystalTooling[i].set(Integer.valueOf(arg[6+i]));
		}
	}
	private void rewrite_sys1(){
		String cmd = String.format(
			"B %.2f %d %d %d %d %d %d %d %d %d %d",
			boxTimeBase.get(),
			(chkSimulateMode.isSelected()==true)?(1):(0),
			grpDiaplayMode.getSelectIndx(),
			(chkRateResolution.isSelected()==true)?(1):(0),
			boxRateFilter.get(),
			boxCrystalTooling[0].get(),
			boxCrystalTooling[1].get(),
			boxCrystalTooling[2].get(),
			boxCrystalTooling[3].get(),
			boxCrystalTooling[4].get(),
			boxCrystalTooling[5].get()
		);
		exec(cmd);//update SYS-1 information
	}
	
	private void refresh_sys2(){
		String[] arg = exec_arg("C?");
		if(arg.length==1){
			return;
		}
		boxMinFreq.set(Float.valueOf(arg[1]));
		boxMaxFreq.set(Float.valueOf(arg[2]));
		boxMinRate.set(Float.valueOf(arg[3]));
		boxMatRate.set(Float.valueOf(arg[4]));
		boxMinThick.set(Float.valueOf(arg[5]));
		boxMaxThick.set(Float.valueOf(arg[6]));
		chkEtchMode.setSelected((Integer.valueOf(arg[7])==0)?(false):(true));
	}

	private void rewrite_sys2(){		
		String cmd = String.format(
			"C %.3f %.3f %.3f %.3f %.3f %.3f %d",
			boxMinFreq.get(),
			boxMaxFreq.get(),
			boxMinRate.get(),
			boxMatRate.get(),
			boxMinThick.get(),
			boxMaxThick.get(),
			(chkEtchMode.isSelected()==true)?(1):(0)
		);
		exec(cmd);//update SYS-2 information
	}
	//--------------------------------//
	
	private float avgRate, avgThick;
	//private FloatProperty propAvgRate = new SimpleFloatProperty();
	//private FloatProperty propAvgThick= new SimpleFloatProperty();
	private StringProperty propAvgRate = new SimpleStringProperty(); 
	private StringProperty propAvgThick= new SimpleStringProperty();
	
	private Node layout_info_meas(){
		
		Label[] txt = {
			new Label("平均速率："), new Label(),
			new Label("平均厚度："), new Label(),
		};
		txt[1].textProperty().bind(propAvgRate);
		txt[3].textProperty().bind(propAvgThick);
		
		final GridPane root = new GridPane();
		root.getStyleClass().add("grid-medium");
		root.addRow(0, txt[0], txt[1]);
		root.addRow(1, txt[2], txt[3]);
		return PanDecorate.group("Measurement", root);
	}
	
	private JFXComboBox<String> cmbFilmIdx = new JFXComboBox<String>();
	
	private JFXTextField  boxFilmName = new JFXTextField();
	private BoxValFloat   boxDensity = new BoxValFloat(0.5f,2).setRange(0.50f, 99.99f);
	private BoxValInteger boxFilmTooling = new BoxValInteger().setRange(10, 399);
	private BoxValFloat   boxZRatio = new BoxValFloat(0.1f,3).setRange(0.10f, 9.999f);
	private BoxValFloat   boxFinalThick= new BoxValFloat(0.f,3).setRange(0.000f, 9999.000f);
	private BoxValFloat   boxSetPoint1 = new BoxValFloat(0.f,3).setRange(0.000f, 9999.000f);
	private BoxValInteger boxSetPoint2 = new BoxValInteger().setRange(0, 9959);
	private JFXCheckBox[] chkSensor = {
		new JFXCheckBox("Sensor-1"), new JFXCheckBox("Sensor-2"), new JFXCheckBox("Sensor-3"),
		new JFXCheckBox("Sensor-4"), new JFXCheckBox("Sensor-5"), new JFXCheckBox("Sensor-6"),
	};
	
	private Button btnFilmRefresh= PanBase.genButton3("更新",null);
	private Button btnFilmRewrite= PanBase.genButton3("套用",null);
	private Button btnFilmActive = PanBase.genButton3("指定",null);
	
	private Node layout_info_flim(){
		
		for(int i=1; i<=99; i++){
			cmbFilmIdx.getItems().add(String.format("Film-%d",i));
		}
		cmbFilmIdx.getSelectionModel().select(0);
		cmbFilmIdx.setOnAction(e->{
			int idx = 1+cmbFilmIdx.getSelectionModel().getSelectedIndex();
			refresh_film(idx);
		});
		
		final GridPane root = new GridPane();
		root.getStyleClass().add("grid-medium");
		
		btnFilmRefresh.setOnAction(e->{
			int idx = 1+cmbFilmIdx.getSelectionModel().getSelectedIndex();
			refresh_film(idx);
		});
		
		btnFilmActive.setOnAction(e->{
			int idx = 1+cmbFilmIdx.getSelectionModel().getSelectedIndex();
			exec(String.format("D%d",idx));
		});
		
		btnFilmRewrite.setOnAction(e->{
			rewrite_flim();
			//btnFilmRefresh.getOnAction().handle(null);
		});
		
		root.add(cmbFilmIdx    , 0, 0);
		root.add(boxFilmName   , 0, 1);
		root.add(btnFilmRefresh, 0, 2);	
		root.add(btnFilmRewrite, 0, 3);
		root.add(btnFilmActive , 0, 4);			
		root.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 6);		
		root.add(new Label("Density(g/cm³)")        , 2, 0); root.add(boxDensity    , 3, 0);
		root.add(new Label("Film Tooling(%)")       , 2, 1); root.add(boxFilmTooling, 3, 1);
		root.add(new Label("Z-Ratio")               , 2, 2); root.add(boxZRatio     , 3, 2);
		root.add(new Label("Final Thickness(kÅ)")   , 2, 3); root.add(boxFinalThick , 3, 3);
		root.add(new Label("Thickness setpoint(kÅ)"), 2, 4); root.add(boxSetPoint1  , 3, 4);
		root.add(new Label("Time Setpoint(mm:ss)")  , 2, 5); root.add(boxSetPoint2  , 3, 5);
		root.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 6);
		root.add(chkSensor[0], 5, 0); 
		root.add(chkSensor[1], 5, 1);
		root.add(chkSensor[2], 5, 2); 
		root.add(chkSensor[3], 5, 3);
		root.add(chkSensor[4], 5, 4); 
		root.add(chkSensor[5], 5, 5);
		
		root.sceneProperty().addListener((obv,oldVal,newVal)->{
			if(newVal==null){
				return;//not showing....
			}
			refresh_film(0);//Use '0' to query the current film~~~			
		});
		return PanDecorate.group("Film Setting", root);
	}
	
	//system information - 1
	private BoxValFloat boxTimeBase = new BoxValFloat(0.1f,2).setRange(0.1f, 2.f);
	private JFXCheckBox chkSimulateMode = new JFXCheckBox("Simulation Mode");
	private GrpToogle grpDiaplayMode = new GrpToogle(
		"Å/s，kÅ",
		"nm/s，μm",
		"Hz",
		"μg/cm²/s，μg/cm²"
	);
	private JFXCheckBox chkRateResolution = new JFXCheckBox("High Rate Resolution");
	private BoxValInteger boxRateFilter = new BoxValInteger().setRange(1, 20);	
	private BoxValInteger[] boxCrystalTooling = {
		new BoxValInteger().setRange(10, 399),
		new BoxValInteger().setRange(10, 399),
		new BoxValInteger().setRange(10, 399),
		new BoxValInteger().setRange(10, 399),
		new BoxValInteger().setRange(10, 399),
		new BoxValInteger().setRange(10, 399),
	};
	
	//system information - 2 	
	private BoxValFloat boxMinFreq = new BoxValFloat(1f,3).setRange(1.f, 6.4f);
	private BoxValFloat boxMaxFreq = new BoxValFloat(1f,3).setRange(1.f, 6.4f);
	private BoxValFloat boxMinRate = new BoxValFloat(0f,3).setRange(-99f, 999f);
	private BoxValFloat boxMatRate = new BoxValFloat(0f,3).setRange(-99f, 999f);
	private BoxValFloat boxMinThick= new BoxValFloat(0f,3).setRange(0.f, 9999f);
	private BoxValFloat boxMaxThick= new BoxValFloat(0f,3).setRange(0.f, 9999f);
	private JFXCheckBox chkEtchMode = new JFXCheckBox("Etch Mode");
	
	private Node layout_info_sys(){

		final GridPane root = new GridPane();
		root.getStyleClass().add("grid-medium");

		root.add(new Label("Time Base(sec)"), 0, 0); root.add(boxTimeBase      , 1, 0, 1, 1);
		root.add(new Label("Rate Filter")   , 0, 1); root.add(boxRateFilter    , 1, 1, 1, 1);
		root.add(chkSimulateMode  , 0, 2, 2, 1);
		root.add(chkRateResolution, 0, 3, 2, 1);
		root.add(new Separator(Orientation.VERTICAL), 2, 0, 1, 7);
		root.add(new Label("--Display Mode--"), 3, 0);
		root.add(grpDiaplayMode, 3, 1, 1, 6);
		root.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 7);
		root.add(new Label("Crystal Tooling-1"), 5, 0); root.add(boxCrystalTooling[0], 6, 0, 1, 1);
		root.add(new Label("Crystal Tooling-2"), 5, 1); root.add(boxCrystalTooling[1], 6, 1, 1, 1);
		root.add(new Label("Crystal Tooling-3"), 5, 2); root.add(boxCrystalTooling[2], 6, 2, 1, 1);
		root.add(new Label("Crystal Tooling-4"), 5, 3); root.add(boxCrystalTooling[3], 6, 3, 1, 1);
		root.add(new Label("Crystal Tooling-5"), 5, 4); root.add(boxCrystalTooling[4], 6, 4, 1, 1);
		root.add(new Label("Crystal Tooling-6"), 5, 5); root.add(boxCrystalTooling[5], 6, 5, 1, 1);
		root.add(new Separator(Orientation.VERTICAL), 7, 0, 1, 7);
		root.add(chkEtchMode, 8, 0, 2, 1);
		root.add(new Label("Min Freq(MHz)"), 8, 1); root.add(boxMinFreq , 9, 1, 1, 1);
		root.add(new Label("Max Freq(MHz)"), 8, 2); root.add(boxMaxFreq , 9, 2, 1, 1);
		root.add(new Label("Min Rate(Å/s)"), 8, 3); root.add(boxMinRate , 9, 3, 1, 1);
		root.add(new Label("Max Rate(Å/s)"), 8, 4); root.add(boxMatRate , 9, 4, 1, 1);
		root.add(new Label("Min Thick(kÅ)"), 8, 5); root.add(boxMinThick, 9, 5, 1, 1);
		root.add(new Label("Max Thick(kÅ)"), 8, 6); root.add(boxMaxThick, 9, 6, 1, 1);
		
		root.sceneProperty().addListener((obv,oldVal,newVal)->{
			if(newVal==null){
				return;//not showing....
			}			
			refresh_sys1();
			refresh_sys2();
		});
		return PanDecorate.group("System Infomation", root);
	}
	
	private Node layout_ctrl(){
		
		final Button[] btn = {
			PanBase.genButton3("更新系統",null),
			PanBase.genButton3("套用系統",null),
			PanBase.genButton3("重設時間",null),
			PanBase.genButton3("重設結果",null),
			PanBase.genButton3("恢復預設",null)
		};
		for(int i=0; i<btn.length; i++){
			HBox.setHgrow(btn[i], Priority.ALWAYS);
		}
		btn[0].setOnAction(e->{
			refresh_sys1();
			refresh_sys2();
		});
		btn[1].setOnAction(e->{
			rewrite_sys1();
			rewrite_sys2();
		});
		btn[2].setOnAction(e->{
			exec("T");
		});
		btn[3].setOnAction(e->{
			exec("S");			
		});
		btn[4].setOnAction(e->{
			exec("Z");
			refresh_sys1();
			refresh_sys2();
		});
		
		HBox root = new HBox();
		root.getStyleClass().add("hbox-small");
		root.setAlignment(Pos.BASELINE_CENTER);
		root.getChildren().addAll(btn);
		return root;
	}
	//---------------------------------------------//
	
	@Override
	protected Node eventLayout(PanBase pan) {

		Label txtStatus = new Label();
		txtStatus.textProperty().bind(propLastStatus);
				
		final GridPane root = new GridPane();		
		root.add(layout_info_flim(), 0, 0, 1, 1);
		root.add(layout_info_meas(), 1, 0, 1, 1);
		root.add(layout_info_sys() , 0, 1, 2, 1);
		root.add(txtStatus         , 0, 2, 2, 1);
		root.add(layout_ctrl()     , 0, 3, 2, 1);
		return root;
	}
}
