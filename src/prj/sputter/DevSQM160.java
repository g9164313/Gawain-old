package prj.sputter;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PadTouch;

public class DevSQM160 extends DevTTY {

	public DevSQM160() {
		TAG = "SQM-160";
		readTimeout = 500;
	}
	public DevSQM160(String path_name) {
		this();
		setPathName(path_name);
	}
	
	private final String STG_INIT = "init";
	private final String STG_MONT = "monitor";
	
	private float[] w_value = new float[18];
	private float   m_value = 0.f;
	private float   o_value = 0.f;
	private String[] a_value= new String[0];
	private String[] b_value= new String[0];
	private String[] c_value= new String[0];
	private boolean  u_value = false;

	private void state_init() {
		String res;
		//res = exec("@");//!#@O7 --> !0AMON_Ver_4.13(85)(119)
		res = exec("A0?");
		parse_a_value(res);
		
		res = exec("B?");
		if(res.charAt(0)=='A') {
			res = res.substring(1).trim();
			b_value = res.split("\\s+");
		}
		res = exec("C?");
		if(res.charAt(0)=='A') {
			res = res.substring(1).trim();
			c_value = res.split("\\s+");
		}
		res = exec("U?");
		if(res.charAt(0)=='A') {
			res = res.substring(1).trim(); 
			u_value = (Integer.valueOf(res)>0)?(true):(false);
		}
		Application.invokeAndWait(()->{
			if(a_value.length>=8) {
				for(int i=0; i<a_value.length; i++) {
					a_value[i] = a_value[i].trim();
					filmData[i].set(a_value[i]);
				}
			}
			if(b_value.length>=11) {
				switch(Integer.valueOf(b_value[3])) {
				case 0:
					unitRate.set("A/s");//Å, font problem
					unitThick.set("kA");
					break;
				case 1:
					unitRate.set("nm/s");
					unitThick.set("μm");
					break;
				case 2:
					unitRate.set("Hz");
					unitThick.set("Hz");
					break;
				case 3:
					unitRate.set("ng/cm²/s");
					unitThick.set("μg/cm²");
					break;
				}
			}
			if(c_value.length>=7) {
				freqRange[0].set(Float.valueOf(c_value[0]));
				freqRange[1].set(Float.valueOf(c_value[1]));
				rateRange[0].set(Float.valueOf(c_value[2]));
				rateRange[1].set(Float.valueOf(c_value[3]));
				thickRange[0].set(Float.valueOf(c_value[4]));
				thickRange[1].set(Float.valueOf(c_value[5]));
			}
			shutter.set(u_value);
		});
		nextState(STG_MONT);
		//nextState("");
	}
	private void state_monitor() {
		try {
			Thread.sleep(250);
		} catch (InterruptedException e) {
			return;
		}
		String res;
		String[] col;
		try {
			res = exec("M");
			if(res.charAt(0)=='A') {
				res = res.substring(1).trim();
				col = res.split("\\s+");
				m_value = Float.valueOf(col[0]);
			}else {
				return;
			}
			res = exec("O");
			if(res.charAt(0)=='A') {
				res = res.substring(1).trim();
				col = res.split("\\s+");
				o_value = Float.valueOf(col[0]);
			}else {
				return;
			}
			res = exec("U?");
			if(res.charAt(0)=='A') {
				res = res.substring(1).trim(); 
				u_value = (Integer.valueOf(res)>0)?(true):(false);
			}
		}catch(NumberFormatException e) {
			Misc.logv("[%s] %s",TAG,e.getMessage());
		}
		
		Application.invokeAndWait(()->{
			shutter.set(u_value);
			rate[0].set(m_value);
			thick[0].set(o_value);
			for(int i=1; i<=6; i++) {
				rate[i].set(w_value[i+(6*0-1)]);
				thick[i].set(w_value[i+(6*1-1)]);
				freq[i].set(w_value[i+(6*2-1)]);
			}
		});
	}
	protected void afterOpen() {
		addState(STG_INIT, ()->state_init()).
		addState(STG_MONT, ()->state_monitor());
		playFlow(STG_INIT);
	}
	
	public String[] parse_a_value(String res) {
		if(res.length()==0){
			return null;
		}
		a_value = new String[8];
		for(int i=0; i<a_value.length; i++) {
			a_value[i] = "???";
		}
		if(res.charAt(0)!='A') {
			return null;
		}
		res = res.substring(1).trim();
		if(res.length()<8){
			return null;
		}
		a_value[0] = res.substring(0,8);
		String[] cols = res.substring(8).trim().split("\\s+");
		for(int i=1; i<a_value.length; i++) {
			a_value[i] = cols[i-1];
		}
		return a_value;
	}
		
	private short calc_crc(short crc, int val) {
		crc = (short) (crc ^ (short)val);
		for (int ix = 0; ix < 8; ix++) {
			short tmpCRC = crc;
			crc = (short) (crc >> 1);
			if ((tmpCRC & 0x1) == 1) {
				crc = (short) (crc ^ 0x2001);
			}
		}
		return (short) (crc & 0x3fff);
	}
	
	public String exec(final String cmd) {
		
		//Command Packet (Host to SQM-160 Message)
		//<Sync character> <Length character> <Message> <CRC1><CRC2>
		//max-length is 190 byte
		//test command: !#@O7
		final byte[] buf = new byte[200];
		int len = cmd.length();
		short crc;
		
		buf[0] = (byte)(len+34);
		crc = 0x3fff;
		crc = calc_crc(crc,buf[0]);		
		for(int i=0; i<len; i++) {			
			int val = cmd.charAt(i) & 0xFF;			
			buf[i+1] = (byte)val;			
			crc = calc_crc(crc,val);
		}
		buf[len+1] = (byte) (((crc   ) & 0x7f) + 34);
		buf[len+2] = (byte) (((crc>>7) & 0x7f) + 34);
				
		writeByte('!');//write sync character~~~
		writeByte(buf,0,1);//write length
		writeByte(buf,1,len+2);//write command and CRC
		
		//Response package (SQM-160 to Host Message)
		do{
			buf[0] = readByte();
			//Misc.loge("[%s] no sync character...", TAG);
		}while(buf[0]!='!');
		
		buf[0] = readByte();//package length, it must be ASCII code
		len = (buf[0]&0xFF)-35;
		if(buf[0]<0x20 || 0x7E<buf[0] || len<=0) {
			Misc.loge("[%s] wrong package length...", TAG);
			return "E";//what is going on?
		}
		readByte(buf,0,len+2);//include CRC
		
		//verify CRC...
		crc = 0x3fff;
		crc = calc_crc(crc,len+35);		
		for(int i=0; i<len; i++) {			
			int val = buf[i] & 0xFF;			
			crc = calc_crc(crc,val);
		}
		if((((crc   )&0x7f)+34)!=(buf[len+0]&0xFF) ){
			Misc.loge("[%s] CRC is wrong!!", TAG);
			return "E";
		}
		if((((crc>>7)&0x7f)+34)!=(buf[len+1]&0xFF) ){
			Misc.loge("[%s] CRC is wrong!!", TAG);
			return "E";
		}
		return new String(buf,0,len);
	}
	public void shutter(final boolean flag) {asyncBreakIn(()->{
		//U1 --> shutter open
		//U0 --> shutter close
		String res = exec((flag)?("U1"):("U0"));
		if(res.charAt(0)=='A'){
			Application.invokeAndWait(()->shutter.set(flag));
		}
		nextState(STG_MONT);
	});}
	public void zeros() {asyncBreakIn(()->{
		String res;
		res = exec("T");
		if(res.charAt(0)!='A'){
			Misc.logw("[%s] fail to reset reading value", TAG);
		}
		res = exec("S");
		if(res.charAt(0)!='A'){
			Misc.logw("[%s] fail to reset time value", TAG);
		}
		nextState(STG_MONT);
	});}
	public void shutter_on_zeros(){asyncBreakIn(()->{		
		String res;
		res = exec("T");
		res = exec("S");
		res = exec("U1");
		if(res.charAt(0)=='A'){
			Application.invokeAndWait(()->shutter.set(true));
		}
		nextState(STG_MONT);
	});}	
	//-------------------------//
	public final BooleanProperty shutter = new SimpleBooleanProperty(false);

	public final StringProperty unitRate = new SimpleStringProperty("??");
	public final StringProperty unitThick= new SimpleStringProperty("??");
	
	public final StringProperty[] filmData = {
		new SimpleStringProperty("??"),//name
		new SimpleStringProperty("??"),//density
		new SimpleStringProperty("??"),//tooling
		new SimpleStringProperty("??"),//z-ratio
		new SimpleStringProperty("??"),//final thickness
		new SimpleStringProperty("??"),//thickness set-point
		new SimpleStringProperty("??"),//time set-point
		new SimpleStringProperty("??"),//sensor Average
	};

	public final FloatProperty[] freqRange = {
		new SimpleFloatProperty(1f),
		new SimpleFloatProperty(6.4e6f)
	};//min and max value
	public final FloatProperty[] rateRange = {
		new SimpleFloatProperty(-99f ),
		new SimpleFloatProperty( 999f)
	};//min and max value
	public final FloatProperty[] thickRange = {
		new SimpleFloatProperty(0f   ),
		new SimpleFloatProperty(9999f)
	};//min and max value
	
	//SQM-160 can connect 6 sensors~~~
	public final FloatProperty[] rate = {
		new SimpleFloatProperty(),//average rate value
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty()
	};
	public final FloatProperty[] thick= {
		new SimpleFloatProperty(),//average thick value
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty()
	};
	//Frequency for each sensor, no average reading~~~
	public final FloatProperty[] freq = {
		null,
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty()
	};
	//Crystal Life for each sensor, no average reading~~~
	public final FloatProperty[] life = {
		null,
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty(),
		new SimpleFloatProperty()
	};
	
	public static void exec_gui(
		final DevSQM160 dev,
		final String cmd,
		final ReadBack hook
	) { dev.asyncBreakIn(()->{
		if(cmd.length()==0) {
			return;
		}
		final String res = dev.exec(cmd);
		if(hook!=null) {
			hook.callback(res);
		}else {
			switch(res.charAt(0)) {
			case 'A':
				//normal response, do nothing~~~
				break;
			case 'C':
				Misc.logv("[%s] CMD:%s--> invalid", dev.TAG, cmd);
				break;
			case 'D':
				Misc.logv("[%s] CMD:%s--> problem data", dev.TAG, cmd);
				break;
			default:
				Misc.logv("[%s] CMD:%s--> internal error", dev.TAG, cmd);
				break;
			}
		}
		dev.nextState(dev.STG_MONT);
	});}
	
	public static Pane genPanel(final DevSQM160 dev) {
		
		final Label[] txt = new Label[8];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
			txt[i].setMaxWidth(Double.MAX_VALUE);			
			GridPane.setHgrow(txt[i], Priority.ALWAYS);
		}
		txt[0].textProperty().bind(dev.rate[0].asString("%6.3f"));
		txt[1].textProperty().bind(dev.unitRate);
		txt[2].textProperty().bind(dev.thick[0].asString("%6.3f"));
		txt[3].textProperty().bind(dev.unitThick);
		txt[4].textProperty().bind(dev.filmData[0]);//film name
		txt[5].textProperty().bind(dev.filmData[1]);//film Density (g/cm3)
		txt[6].textProperty().bind(dev.filmData[2]);//film Tooling (%)
		txt[7].textProperty().bind(dev.filmData[3]);//film Z-Ratio
		
		final JFXButton[] btn = new JFXButton[6];
		for(int i=0; i<btn.length; i++) {
			btn[i] = new JFXButton();			
			btn[i].setMaxWidth(Double.MAX_VALUE);			
			GridPane.setHgrow(btn[i], Priority.ALWAYS);
		}
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setText("讀值歸零");
		btn[0].setOnAction(e->dev.zeros());
		btn[1].getStyleClass().add("btn-raised-2");
		btn[1].setText("選取薄膜");
		btn[1].setOnAction(e->{
			PadTouch pad = new PadTouch('N',"薄膜編號:");
			Optional<String> opt = pad.showAndWait();
			if(opt.isPresent()==true) {
				int idx = Integer.valueOf(opt.get());
				if(idx==0) {
					return;
				}
				active_film_gui(dev,idx);
			}		
		});
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setText("設定薄膜");
		btn[2].setOnAction(e->dev.updateFilm());
		btn[3].getStyleClass().add("btn-raised-3");
		btn[3].setText("擋板-開");
		btn[3].setOnAction(e->exec_gui(dev,"U1",null));
		btn[4].getStyleClass().add("btn-raised-3");
		btn[4].setText("擋板-關");
		btn[4].setOnAction(e->exec_gui(dev,"U0",null));

		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad");
		lay0.add(new Separator(), 0, 1, 3, 1);
		lay0.addRow(2, new Label("速率："), txt[0], txt[1]);
		lay0.addRow(3, new Label("厚度："), txt[2], txt[3]);
		lay0.add(new Label("薄膜參數"), 0, 4, 3, 1);
		lay0.addRow(5, new Label("名稱："), txt[4]);
		lay0.addRow(6, new Label("密度："), txt[5], new Label("g/cm³"));
		lay0.addRow(7, new Label("Tooling："), txt[6], new Label("%"));
		lay0.addRow(8, new Label("Z-Ratio："), txt[7]);
		lay0.add(new Separator(), 0, 9, 3, 1);
		lay0.add(btn[0], 0, 10, 3, 1);
		lay0.add(btn[1], 0, 11, 3, 1);
		lay0.add(btn[2], 0, 12, 3, 1);
		lay0.add(btn[3], 0, 13, 3, 1);
		lay0.add(btn[4], 0, 14, 3, 1);
		return lay0;
	}
	//------------------------------
	
	private static void active_film_gui(
		final DevSQM160 dev,
		final int idx
	) { dev.asyncBreakIn(()->{
		char id = (char)(idx+48);
		String cmd,res;
		//active film parameter
		cmd = String.format("D%c", id);		
		res = dev.exec(cmd);
		if(res.charAt(0)=='A') {
			//update information~~~
			cmd = String.format("A%c?", id);
			res = dev.exec(cmd);
			if(res.charAt(0)=='A') {
				dev.parse_a_value(res);
				Application.invokeAndWait(()->{
					for(int i=0; i<dev.a_value.length; i++) {
						dev.filmData[i].set(dev.a_value[i].trim());
					}
				});
			}
		}else{
			Application.invokeAndWait(()->{
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("!!錯誤!!");
				alert.setHeaderText("無法使用薄膜-"+idx);
				alert.setContentText(null);
				alert.showAndWait();
			});
		}	
	});}
	
	private static class DialogFilm extends Dialog<String[]> {		
		//public DialogFilm() {
		//	this(null);
		//}
		public DialogFilm(String[] param){		
			getDialogPane().setContent(create_layout(param));
			getDialogPane().getStylesheets().add(Gawain.sheet);
			getDialogPane().getButtonTypes().addAll(
				ButtonType.CANCEL, ButtonType.OK
			);
		}
		private GridPane create_layout(final String[] param) {
			String[] arg = {
				"0",//film index
				null, null, null, null,
				null, null, null, null,
			};
			if(param.length==8) {
				for(int i=0; i<param.length; i++) {
					arg[i+1] = param[i];
				}
			}else {
				//default value~~~
				arg[1] = "FILM";//name
				arg[2] = "0.5";//density
				arg[3] = "10";//tooling
				arg[4] = "0.1";//z-ratio
				arg[5] = "0.0";//final thickness
				arg[6] = "0.0";//thickness set-point(kA)
				arg[7] = "60";//time set-point (mm:ss)
				arg[8] = "32";//active sensor
			}
			
			final TextField[] box = new TextField[9];
			for(int i=0; i<box.length; i++) {
				TextField obj = new TextField(arg[i]);
				obj.setPrefColumnCount(9);
				box[i] = obj;
			}
			setResultConverter(dia->{
				ButtonData btn = (dia==null)?(null):(dia.getButtonData());
				if(btn!=ButtonData.OK_DONE) {
					return null;				
				}
				String[] res = new String[9];
				for(int i=0; i<res.length; i++) {
					res[i] = box[i].getText().trim();
				}
				return res;
			});
						
			final GridPane lay = new GridPane();
			lay.getStyleClass().addAll("box-pad");
			lay.addRow(0, new Label("薄膜編號："), box[0]);
			lay.addRow(1, new Label("薄膜名稱："), box[1]);
			lay.addRow(2, new Label("薄膜密度："), box[2]);
			lay.addRow(3, new Label("tooling："), box[3]);
			lay.addRow(4, new Label("z-ratio："), box[4]);
			lay.addRow(5, new Label("最終厚度："), box[5]);
			lay.addRow(6, new Label("厚度停止點："), box[6]);
			lay.addRow(7, new Label("時間停止點："), box[7]);
			lay.addRow(8, new Label("偵測器編號："), box[8]);
			return lay;
		}
	};
	
	//call by GUI-thread
	public void updateFilm(){
		DialogFilm dia = new DialogFilm(a_value);
		Optional<String[]> opt = dia.showAndWait();
		if(opt.isPresent()==true) {
			update_film_gui(this,opt.get());
		}
	}
	
	private static void update_film_gui(
		final DevSQM160 dev,
		final String[] arg
	) { dev.asyncBreakIn(()->{
		char id = (char)(48+Integer.valueOf(arg[0]));
		int cnt = arg[1].length();
		if(cnt<8){
			//padding~~~
			cnt = 8 - cnt;
			for(int i=0; i<cnt; i++){
				arg[1] = arg[1] + ' ';
			}
		}else{
			arg[1] = arg[1].substring(0,8);
		}
		arg[1] = arg[1]
			.replace(' ', '_')
			.toUpperCase();
		String cmd = String.format(
			"A%C%s %s %s %s %s %s %s %s",
			id, arg[1], 
			arg[2], arg[3], arg[4], arg[5], 
			arg[6], arg[7], arg[8]
		);
		if(dev.exec(cmd).charAt(0)=='A') {
			if(id!=48){
				return;
			}
			String res = dev.exec("A0?");
			if(res.charAt(0)=='A') {
				dev.parse_a_value(res);
				Application.invokeAndWait(()->{
					for(int i=0; i<dev.a_value.length; i++) {
						dev.a_value[i] = dev.a_value[i].trim();
						dev.filmData[i].set(dev.a_value[i]);
					}
				});
			}
		}else{
			Application.invokeAndWait(()->{
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("!!錯誤!!");
				alert.setHeaderText("無法設定薄膜參數");
				alert.setContentText(null);
				alert.showAndWait();
			});
		}
	});}
}
