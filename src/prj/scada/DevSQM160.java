package prj.scada;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
		readTimeout = 1000;
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
	private String[] b_value= null;
	private String[] c_value= null;//range value~~~
	
	private void state_init() {
		b_value = exec_s("B?").split("\\s+");
		c_value = exec_s("C?").split("\\s+");
		Application.invokeAndWait(()->{
			if(b_value.length>=11) {
				switch(Integer.valueOf(b_value[3])) {
				case 0:
					unitRate .set("Å/s");
					unitHigh.set("kÅ");
					break;
				case 1:
					unitRate .set("nm/s");
					unitHigh.set("μm");
					break;
				case 2:
					unitRate .set("Hz");
					unitHigh.set("Hz");
					break;
				case 3:
					unitRate .set("ng/cm²/s");
					unitHigh.set("μg/cm²");
					break;
				}
			}
			if(c_value.length>=7) {
				freqRange[0].set(Float.valueOf(c_value[1]));
				freqRange[1].set(Float.valueOf(c_value[2]));
				rateRange[0].set(Float.valueOf(c_value[3]));
				rateRange[1].set(Float.valueOf(c_value[4]));
				highRange[0].set(Float.valueOf(c_value[5]));
				highRange[1].set(Float.valueOf(c_value[6]));
			}
		});
		nextState.set(STG_MONT);
	}
	private void state_monitor() {
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			return;
		}
		
		String[] col;
		col = exec_s("W").split("\\s+");
		if(col.length>=19) {
			//skip the first item~~~
			for(int ii=1; ii<=w_value.length; ii++) {
				int rr = (ii-1)%3;
				int cc = (ii-1)/3;
				int jj = rr * 6 + cc;
				w_value[jj] = Float.valueOf(col[ii]);
			}
		}
		col = exec_s("M").split("\\s+");
		if(col.length>=2) {
			m_value = Float.valueOf(col[1]);
		}
		col = exec_s("O").split("\\s+");
		if(col.length>=2) {
			o_value = Float.valueOf(col[1]);
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
		len = readByte(buf);
		if(len<=0) {
			return "";
		}
		int cnt = (buf[off-1]&0xFF)-34;
		if((off+cnt)>=len) {
			cnt = len - off;
		}
		return new String(buf,off,cnt-1);
	}
	public String exec_s(final String cmd) {	
		return exec_(true, cmd);
	}
	public String exec_c(final String cmd) {	
		return exec_(false, cmd);
	}
	//-------------------------//
	
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
	) { dev.interrupt(()->{
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
	
	public static Pane genPanel(final DevSQM160 dev) {
		
		final Label[] txt = new Label[4];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
			txt[i].setMaxWidth(Double.MAX_VALUE);			
			GridPane.setHgrow(txt[i], Priority.ALWAYS);
		}
		txt[0].textProperty().bind(dev.rate[0].asString("%.3f"));
		txt[1].textProperty().bind(dev.unitRate);
		txt[2].textProperty().bind(dev.high[0].asString("%.3f"));
		txt[3].textProperty().bind(dev.unitHigh);
		
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
		btn[2].setText("薄膜參數");
		btn[2].setOnAction(e->{});
		btn[3].setText("Shutter");
		//btn[3].setOnAction(e->exec_gui(dev,"",(res)->{	
		//}));
		btn[4].setText("恢復預設");
		//btn[4].setOnAction(e->{});
		
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad");
		lay0.add(new Separator(), 0, 1, 3, 1);
		lay0.addRow(2, new Label("速率："), txt[0], txt[1]);
		lay0.addRow(3, new Label("厚度："), txt[2], txt[3]);
		lay0.add(new Separator(), 0, 4, 3, 1);
		lay0.add(btn[0], 0, 5, 3, 1);
		lay0.add(btn[1], 0, 6, 3, 1);
		lay0.add(btn[2], 0, 7, 3, 1);
		lay0.add(btn[3], 0, 8, 3, 1);
		lay0.add(btn[4], 0, 9, 3, 1);
		return lay0;
	}
}
