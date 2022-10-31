package prj.sputter.labor1;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import narl.itrc.Gawain;

public class Record {
	
	private static final SimpleDateFormat s_fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	final StringProperty stmp = new SimpleStringProperty();
	final StringProperty volt = new SimpleStringProperty();
	final StringProperty amps = new SimpleStringProperty();
	final StringProperty watt = new SimpleStringProperty();
	final StringProperty joul = new SimpleStringProperty();
	final StringProperty rate = new SimpleStringProperty();
	final StringProperty high = new SimpleStringProperty();
	
	public String getStmp() { return stmp.get(); }
	public String getVolt() { return volt.get(); }
	public String getAmps() { return amps.get(); }
	public String getWatt() { return watt.get(); }
	public String getjoul() { return joul.get(); }
	public String getRate() { return rate.get(); }
	public String getHigh() { return high.get(); }

	public double getValue(final int i) {
		String txt = null;
		switch(i) {
		case 1: txt = volt.get(); break;
		case 2: txt = amps.get(); break;
		case 3: txt = watt.get(); break;
		case 4: txt = joul.get(); break;
		case 5: txt = rate.get(); break;
		case 6: txt = high.get(); break;
		}
		if(txt==null) {
			return 0.;
		}
		return Double.valueOf(txt);
	}
	
	private String json_appx = "";
	
	public Record() {
		final Timestamp ss = new Timestamp(System.currentTimeMillis());
		stmp.set(s_fmt.format(ss));
		
		//user can append some information in jason 
		json_appx = Gawain.prop().getProperty("json_appx", "");
		if(json_appx.length()!=0 && json_appx.endsWith(",")==false){
			json_appx = json_appx + ", ";
		}
		return;
	}
	
	public Record(final FloatProperty[] arg) {
		this();
		volt.set(String.format("%.1f", arg[0].get()));
		amps.set(String.format("%.2f", arg[1].get()));
		watt.set(String.format("%.0f", arg[2].get()));
		joul.set(String.format("%.0f", arg[3].get()));
		rate.set(String.format("%.3f", arg[4].get()));
		high.set(String.format("%.3f", arg[5].get()));
	}
	
	public String toJSON(){
		return String.format(
			"{"+
			"\"dt\":\"%s\", "+	json_appx  +
			"\"volt\":\"%s\", "+"\"amps\":\"%s\", "+
			"\"watt\":\"%s\", "+"\"joul\":\"%s\", "+
			"\"rate\":\"%s\", "+"\"high\":\"%s\" "+
			"}",
			stmp.get(),
			volt.get(), amps.get(), 
			watt.get(), joul.get(),
			rate.get(), high.get()
		);
	}
}

