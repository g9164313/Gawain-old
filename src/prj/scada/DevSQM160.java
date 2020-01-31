package prj.scada;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import narl.itrc.DevTTY;
import narl.itrc.Misc;

public class DevSQM160 extends DevTTY {

	public DevSQM160() {
		TAG = "SQM-160";
		readTimeout = 100;
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
	
	private void state_init() {
		String res;
		res = exec_s("A0?");
		if(res.charAt(0)=='A') {
			res = res.substring(1).trim();
			a_value = res.split("\\s+");
		}
		res = exec_s("B?");
		if(res.charAt(0)=='A') {
			res = res.substring(1).trim();
			b_value = res.split("\\s+");
		}
		res = exec_s("C?");
		if(res.charAt(0)=='A') {
			res = res.substring(1).trim();
			c_value = res.split("\\s+");
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
					unitHigh.set("kA");
					break;
				case 1:
					unitRate.set("nm/s");
					unitHigh.set("μm");
					break;
				case 2:
					unitRate.set("Hz");
					unitHigh.set("Hz");
					break;
				case 3:
					unitRate.set("ng/cm²/s");
					unitHigh.set("μg/cm²");
					break;
				}
			}
			if(c_value.length>=7) {
				freqRange[0].set(Float.valueOf(c_value[0]));
				freqRange[1].set(Float.valueOf(c_value[1]));
				rateRange[0].set(Float.valueOf(c_value[2]));
				rateRange[1].set(Float.valueOf(c_value[3]));
				highRange[0].set(Float.valueOf(c_value[4]));
				highRange[1].set(Float.valueOf(c_value[5]));
			}
		});
		nextState.set(STG_MONT);
	}
	private void state_monitor() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			return;
		}
		String res;
		String[] col;
		res = exec_s("W");
		if(res.charAt(0)=='A') {
			res = res.substring(1).trim();
			col = res.split("\\s+");
			for(int ii=1; ii<=w_value.length; ii++) {
				int rr = (ii-1)%3;
				int cc = (ii-1)/3;
				int jj = rr * 6 + cc;
				w_value[jj] = Float.valueOf(col[ii]);
			}
		}
		res = exec_s("M");
		if(res.charAt(0)=='A') {
			res = res.substring(1).trim();
			col = res.split("\\s+");
			m_value = Float.valueOf(col[0]);
		}
		res = exec_s("O");
		if(res.charAt(0)=='A') {
			res = res.substring(1).trim();
			col = res.split("\\s+");
			o_value = Float.valueOf(col[0]);
		}
		Application.invokeAndWait(()->{
			rate[0].set(m_value);
			high[0].set(o_value);
			for(int i=1; i<=6; i++) {
				rate[i].set(w_value[i+(6*0-1)]);
				high[i].set(w_value[i+(6*1-1)]);
				freq[i].set(w_value[i+(6*2-1)]);
			}
		});
	}	
	@Override
	protected void afterOpen() {
		setupState0(STG_INIT, ()->state_init()).
		setupStateX(STG_MONT, ()->state_monitor());
		playFlow();
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
	
	private String exec_(
		final boolean sync,
		final String cmd
	) {		
		if(cmd.length()>190) {
			Misc.loge("command is too long!!");
			return "";
		}
		//Command Packet (Host to SQM-160 Message)
		//<Sync character> <Length character> <Message> <CRC1><CRC2>		
		//test command: !#@O7
		final byte[] buf = new byte[200];
		
		short crc = 0x3fff;
		int off = (sync)?(2):(1);
		int len = cmd.length();
		
		byte _len = (byte)(len+34);
		
		if(sync==true) {
			buf[0] = '!';
			buf[1] = _len;
		}else {
			buf[0] = _len;
		}
		crc = calc_crc(crc,_len);		
		for(int i=0; i<len; i++) {			
			int val = cmd.charAt(i) & 0xFF;			
			buf[i+off] = (byte)val;			
			crc = calc_crc(crc,val);
		}
		buf[off+len+0] = (byte) (((crc   ) & 0x7f) + 34);
		buf[off+len+1] = (byte) (((crc>>7) & 0x7f) + 34);
				
		len = off + len + 2;//total packet size
		writeByte(buf,0,len);
		
		//Response Packet (SQM-160 to Host Message)
		buf[0] = readByte();
		if(buf[0]=='!') {
			//sync character!!
			buf[0] = readByte();
		}
		len = (buf[0]&0xFF)-35;
		for(int i=0; i<len; i++) {
			buf[i] = readByte();
		}
		buf[len] = 0;//null terminate
		readByte();//CRC-1
		readByte();//CRC-2
		return new String(buf,0,len);
	}
	public String exec_s(final String cmd) {	
		return exec_(true, cmd);
	}
	public String exec_c(final String cmd) {	
		return exec_(false, cmd);
	}
	//-------------------------//
	
	public final StringProperty[] filmData = {
		new SimpleStringProperty("??"),//name
		new SimpleStringProperty("??"),//density
		new SimpleStringProperty("??"),//tooling
		new SimpleStringProperty("??"),//z-ratio
		new SimpleStringProperty("??"),//final thickness
		new SimpleStringProperty("??"),//thickness set-point
		new SimpleStringProperty("??"),//time set-point
		new SimpleStringProperty("??"),//Sensor Average
		new SimpleStringProperty("??"),
	};
	
	public final StringProperty unitRate = new SimpleStringProperty("??");
	public final StringProperty unitHigh = new SimpleStringProperty("??");
	
	public final FloatProperty[] freqRange = {
		new SimpleFloatProperty(1f),
		new SimpleFloatProperty(6.4e6f)
	};//min and max value
	public final FloatProperty[] rateRange = {
		new SimpleFloatProperty(-99f ),
		new SimpleFloatProperty( 999f)
	};//min and max value
	public final FloatProperty[] highRange = {
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
	public final FloatProperty[] high= {
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
	
	private static void exec_gui(
		final DevSQM160 dev,
		final String cmd,
		final ReadBack hook
	) { dev.breakIn(()->{
		if(cmd.length()==0) {
			return;
		}
		final String res = dev.exec_s(cmd);
		if(hook!=null) {
			hook.callback(res);
		}else {
			switch(res.charAt(0)) {
			case 'A':
				//normal response, do nothing~~~
				break;
			case 'B':
				Misc.logv("[%s] CMD:%s--> invalid", dev.TAG, cmd);
				break;
			case 'C':
				Misc.logv("[%s] CMD:%s--> problem data", dev.TAG, cmd);
				break;
			default:
				Misc.logv("[%s] CMD:%s-->%s", dev.TAG, cmd, res);
				break;
			}
		}
	});}
	
	private static void set_film_gui(
		final DevSQM160 dev,
		final int idx
	) { dev.breakIn(()->{
		String res;
		res = dev.exec_s("D"+idx);
		if(res.charAt(0)!='A') {
			Application.invokeAndWait(()->{
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("!!錯誤!!");
				alert.setHeaderText("無法設定薄膜-"+idx);
				alert.setContentText(null);
				alert.showAndWait();
			});
			return;
		}
		//update information~~~
		res = dev.exec_s("A"+idx+"?");
		if(res.charAt(0)=='A') {
			res = res.substring(1).trim();
			dev.a_value = res.split("\\s+");
		}
		Application.invokeAndWait(()->{
			for(int i=0; i<dev.a_value.length; i++) {
				dev.a_value[i] = dev.a_value[i].trim();
				dev.filmData[i].set(dev.a_value[i]);
			}
		});
	});}
	
	public static Pane genPanel(final DevSQM160 dev) {
		
		final Label[] txt = new Label[8];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
			txt[i].setMaxWidth(Double.MAX_VALUE);			
			GridPane.setHgrow(txt[i], Priority.ALWAYS);
		}
		txt[0].textProperty().bind(dev.rate[0].asString("%.3f"));
		txt[1].textProperty().bind(dev.unitRate);
		txt[2].textProperty().bind(dev.high[0].asString("%.3f"));
		txt[3].textProperty().bind(dev.unitHigh);
		txt[4].textProperty().bind(dev.filmData[0]);//film name
		txt[5].textProperty().bind(dev.filmData[1]);//film Density (g/cm3)
		txt[6].textProperty().bind(dev.filmData[2]);//film Tooling (%)
		txt[7].textProperty().bind(dev.filmData[3]);//film Z-Ratio
		
		final JFXButton[] btn = new JFXButton[5];
		for(int i=0; i<btn.length; i++) {
			btn[i] = new JFXButton();
			btn[i].getStyleClass().add("btn-raised-1");
			btn[i].setMaxWidth(Double.MAX_VALUE);			
			GridPane.setHgrow(btn[i], Priority.ALWAYS);
		}
		btn[0].setText("讀值歸零");
		btn[0].setOnAction(e->exec_gui(dev,"S",null));
		btn[1].setText("重新計時");
		btn[1].setOnAction(e->exec_gui(dev,"T",null));		
		btn[2].setText("選取薄膜");
		btn[2].setOnAction(e->{
			ChoiceDialog<Integer> dia = new ChoiceDialog<>(
				 1, 2, 3, 4, 5, 6, 7, 8, 9,10,
				11,12,13,14,15,16,17,18,19,20,
				21,22,23,24,25,26,27,28,29,30,
				31,32,33,34,35,36,37,38,39,40
			);
			dia.setTitle("選取");
			dia.setHeaderText(null);
			dia.setContentText("薄膜編號:");
			Optional<Integer> opt = dia.showAndWait();
			if(opt.isPresent()) {
				set_film_gui(dev,opt.get().intValue());
			}			
		});
		btn[3].setText("設定薄膜");
		btn[3].setOnAction(e->{});
		//btn[3].setText("Shutter");
		//btn[3].setOnAction(e->exec_gui(dev,"",(res)->{	
		//}));
		//btn[4].setText("恢復預設");
		//btn[4].setOnAction(e->{});
		
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
		return lay0;
	}
}
