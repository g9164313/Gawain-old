package prj.sputter;

import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import narl.itrc.DevTTY;
import narl.itrc.Misc;

public abstract class DevAdam extends DevTTY {

	public static class RangeType {
		final float min, max;
		final String unit;
		public RangeType(float v_min, float v_max, String t_unit) {
			min = v_min;
			max = v_max;
			unit = t_unit;
		}
		@Override
		public String toString() {
			return String.format("%.0f～%.0f%s", min, max, unit);
		}
	};
	
	protected final static RangeType r4to20mA= new RangeType(   4f,  20f, "mA");
	protected final static RangeType d10V    = new RangeType(- 10f,+ 10f, "V");
	protected final static RangeType d5V     = new RangeType(-  5f,+  5f, "V");
	protected final static RangeType d1V     = new RangeType(-  1f,+  1f, "V");
	protected final static RangeType d500mV  = new RangeType(-500f,+500f, "mV");
	protected final static RangeType d150mV  = new RangeType(-150f,+150f, "mV");
	protected final static RangeType d20mA   = new RangeType(- 20f,+ 20f, "mA");
	protected final static RangeType d15V    = new RangeType(- 15f,  15f, "V");
	protected final static RangeType z10V    = new RangeType(   0f,  10f, "V");
	protected final static RangeType z5V     = new RangeType(   0f,   5f, "V");
	protected final static RangeType z1V     = new RangeType(   0f,   1f, "V");
	protected final static RangeType z500mV  = new RangeType(   0f, 500f, "mV");
	protected final static RangeType z150mV  = new RangeType(   0f, 150f, "mV");
	protected final static RangeType z20mA   = new RangeType(   0f,  20f, "mA");
	protected final static RangeType z15V    = new RangeType(   0f,  15f, "V");
	//Thermocouple~~ °C
		
	protected static final Misc.BiMap<String, RangeType> range_type = new Misc.BiMap<String, RangeType>().init(
		"07",r4to20mA,	
		"08",d10V,		
		"09",d5V,	
		"0A",d1V,	
		"0B",d500mV,
		"0C",d150mV,
		"0D",d20mA,
		"15",d15V,
		"48",z10V,
		"49",z5V,
		"4A",z1V,
		"4B",z500mV,
		"4C",z150mV,
		"4D",z20mA,
		"55",z15V
	);

	protected RangeType[] init_range = null;
	
	protected String AA="01";
		
	protected String TT="00", CC="06", FF="00";
	
	protected static final Misc.BiMap<Integer, String> CC_baud_rate = new Misc.BiMap<Integer, String>().init(
		  1200,"03",
		  2400,"04",
		  4800,"05",
		  9600,"06",
		 19200,"07",
		 38400,"08",
		 57600,"09",
		115200,"0A"
	);

	protected void get_range_type(final Channel ch) {
		String ans = exec("$"+AA+"8C"+ch.id);
		if(ans.startsWith("?")==true) {
			Misc.logw("[%s] unable get type and range about ch%d", TAG, ch.id);
			return;
		}
		final String code = ans.substring(ans.length()-2);		
		final RangeType rt = range_type.get(code);		
		ch.update_property(rt);
	};

	protected void set_range_type(final Channel ch, final RangeType rng) {
		final String code = range_type.bi_get(rng);
		final String cmd = "$"+AA+"7C"+ch.id+"R"+code;
		final String ans = exec(cmd);
		if(ans.startsWith("?")==true) {
			Misc.logw("[%s][ch%d] fail to set %s", TAG, ch.id, cmd);
		}
		ch.update_property(rng);
	}
	protected void init_range_type(final Channel[] ch) {
		if(init_range==null) {
			return;
		}
		for(int i=0; i<ch.length; i++) {
			if(i>=init_range.length) {
				break;
			}
			set_range_type(ch[i],init_range[i]);
		}
	}
	
	protected void set_configuration(String NN) {
		if(NN==null) {
			NN = AA;
		}
		final String cmd = String.format(
			"%%s",
			AA,NN,TT,CC,FF
		);
		final String ans = exec(cmd);
		if(ans.startsWith("?")==true) {
			Misc.logw("[%s] fail to set configuration", TAG);			
		}		
		AA = ans.substring(1);
	}
	protected void get_configuration() {		
		String ans = exec("$"+AA+"2");
		if(ans.startsWith("?")==true) {
			Misc.logw("[%s] fail to get configuration", TAG);
			return;
		}
		//response is TTCCFF
		//[TT] is ignore
		//[CC] means baud rate code
		//[FF] flag for device~~~
		TT = ans.substring(3,5);
		CC = ans.substring(5,7);
		FF = ans.substring(7,9);
	}
	
	protected String get_module_name() {
		return exec("$"+AA+"M");
	}
	
	protected String get_firmware_version() {
		return exec("$"+AA+"F");
	}
	
	protected String exec(String cmd) {
		if(cmd.endsWith("\r")==false) {
			cmd = cmd + "\r";
		}
		if(port.isPresent()==false) {
			return "?no_port";
		}
		final SerialPort tty = port.get();
		if(tty.isOpened()==false) {
			return "?"+cmd;
		}
		synchronized(tty){
			try {
				tty.writeString(cmd);
				String ans = "";			
				do {
					char cc = (char) tty.readBytes(1,1000)[0];
					if(cc=='\r') {
						break;
					}
					ans = ans + cc;
				}while(true);
				return ans;
			} catch (SerialPortException e) {			
				Misc.loge("[%s(%s] %s", TAG, AA, e.getMessage());
				return "?IO_error";
			} catch (SerialPortTimeoutException e) {			
				Misc.loge("[%s(%s] %s-%s", TAG, AA, cmd, e.getMessage());
				return "?timeout";
			}
		}
	}
	//---------------------------------------------
	
	protected void refresh_flag(
		final BooleanProperty[]... flag
	) {
		final String ans = exec("$"+AA+"6");
		if(ans.matches("![0123456789ABCDEF]{4}?00")==true) {
			int cnt = (ans.length()-1)/2;
			if(cnt>flag.length) {
				cnt = flag.length;
			}
			final boolean[][] bits = new boolean[cnt][]; 
			for(int i=0; i<cnt; i++) {
				bits[i] = int2flag(ans.substring(1+i*2, 3+i*2));
			}	
			Application.invokeLater(()->{
				for(int i=0; i<bits.length; i++) {
					for(int j=0; j<8; j++) {
						flag[i][j].set(bits[i][j]);
					}
				}				
			});
		}else {
			Misc.logw("[%s] invalid: $AA6 --> %s", TAG, ans);
		}
	}
	
	protected void set_flag(final int idx, final boolean flg) {
		final String val = (flg)?("01"):("00");
		final String ans = exec("#"+AA+"1"+idx+val);
		if(ans.charAt(0)=='>') {
			return;//action is success!!!!
		}
		Misc.loge("[%s] fail to set flag[%d]: %s", TAG, idx, ans);
	}

	protected boolean[] override_flag(
		final ReadOnlyBooleanProperty[] src,
		final Boolean... flg
	) {
		final boolean[] dst = new boolean[src.length];
		for(int i=0; i<src.length; i++) {
			dst[i] = src[i].get();
			if(i<flg.length) {
				if(flg[i]!=null) {
					dst[i] = flg[i].booleanValue();
				}
			}
		}
		return dst;
	}
	protected int flag2val(final boolean[] flg) {
		int val = 0;
		for(int i=0; i<flg.length; i++) {
			if(flg[i]==true) {
				val = val | (1<<i);
			}				
		}
		return val;
	}
	protected void assign_val(final int val) {
		final String ans = exec("#"+AA+"00"+String.format("%02X", val));
		if(ans.charAt(0)=='>') {
			return;//action is success!!!!
		}
		Misc.loge("[%s] fail to set flags: %s(0x%02X)", TAG, ans, val);
	}
	
	//---------------------------------------------
	
	public static class Channel {
		final int id;
		public Channel(final int index){
			id = index;
		}		
		//channel information~~~~		
		public final SimpleFloatProperty val = new SimpleFloatProperty(0f);
		public final SimpleFloatProperty min = new SimpleFloatProperty(0f);
		public final SimpleFloatProperty max = new SimpleFloatProperty(0f);
		
		public final SimpleStringProperty txt = new SimpleStringProperty("");
		public final SimpleStringProperty unit = new SimpleStringProperty("");
		public final SimpleStringProperty title= new SimpleStringProperty("");//give default value, or it will be null!!!

		public RangeType range_type;
		
		void update_property(final RangeType rt) {
			range_type = rt;
			final Runnable func = ()->{				
				min.setValue(rt.min);
				max.setValue(rt.max);
				unit.setValue(rt.unit);
				if(rt.unit.contains("V")) {
					title.setValue(String.format("ch%d 電壓",id));
				}else if(rt.unit.contains("A")) {
					title.setValue(String.format("ch%d 電流",id));
				}else {
					title.setValue(rt.unit);
				}
			};			
			if(Application.isEventThread()==true) {
				func.run();
			}else {
				Application.invokeLater(func);
			}
		}
	};
	//-------------------------------
	
	private static final Image img_check = Misc.getIconImage("radio_checked.png");
	private static final Image img_close = Misc.getIconImage("radio_uncheck.png");
	
	protected static class Pin extends StackPane {
		ImageView img0 = new ImageView(img_close);
		ImageView img1 = new ImageView(img_check);
		final int idx;		
		Pin(			
			final BooleanProperty[] prop,
			final int index
		){
			idx = index;
			getChildren().addAll(img0, img1);
			bind(prop[index]);
		}
		Pin bind(final BooleanProperty prop) {
			img0.visibleProperty().bind(prop.not());
			img1.visibleProperty().bind(prop);
			return this;
		}
		boolean getValue() {
			return img1.visibleProperty().get();
		}
	};
	
	protected boolean[] int2flag(final String txt) {
		final int val = Integer.parseInt(txt,16);
		boolean[] flg = new boolean[8];
		for(int i=0; i<flg.length; i++) {
			flg[i] = ((val & (1<<i))!=0);
		}
		return flg;
	}
}
