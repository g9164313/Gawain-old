package prj.shelter;

import com.sun.glass.ui.Application;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import narl.itrc.DevModbus;

/**
 * 川得的溫濕度紀錄器，使用 Modbus 通訊
 * @author qq
 *
 */
public class DevCDR06 extends DevModbus {
	
	public DevCDR06(){
		TAG = "CDR06";
		mapAddress16("H300000-300006");
	}
	
	private IntegerProperty[] channel = new SimpleIntegerProperty[8];
	
	private FloatProperty[] ch_value = {
		new SimpleFloatProperty(0.f),
		new SimpleFloatProperty(0.f),
		new SimpleFloatProperty(0.f),
		new SimpleFloatProperty(0.f),
		new SimpleFloatProperty(0.f),
		new SimpleFloatProperty(0.f),
		new SimpleFloatProperty(0.f),
		new SimpleFloatProperty(0.f)
	};
	private StringProperty[] ch_v_txt = {
		new SimpleStringProperty("＊＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊＊"),
		new SimpleStringProperty("＊＊＊＊＊")
	};
	private StringProperty[] ch_name = {
		new SimpleStringProperty("壓力"),
		new SimpleStringProperty("濕度"),
		new SimpleStringProperty("溫度"),
		new SimpleStringProperty("ch4"),
		new SimpleStringProperty("ch5"),
		new SimpleStringProperty("ch6"),
		new SimpleStringProperty("ch7"),
		new SimpleStringProperty("ch8")
	};
	
	@SuppressWarnings("rawtypes")
	private Property get_prop(
		final Property[] array,
		final int idx //one-base~~~
	){
		int ii = idx - 1;
		if(ii>=array.length || ii<0){
			return null;
		}
		return array[ii];
	}
	
	public IntegerProperty getRegister(final int idx){
		return (IntegerProperty) get_prop(channel,idx);
	}
	public FloatProperty getChannelValue(final int idx){
		return (FloatProperty) get_prop(ch_value,idx);
	}
	public StringProperty getChannelName(final int idx){
		return (StringProperty) get_prop(ch_name,idx);
	}
	public StringProperty getChannelText(final int idx){
		return (StringProperty) get_prop(ch_v_txt,idx);
	}	
	
	@Override
	protected void ignite(){
		/**
		 * CDR06 holding register:
		 * h300000 : version
		 * h300001 : ???
		 * h300002 : CH0 decimal point
		 * h300003 : CH2 decimal point
		 * ....    : ....
		 */		
		final short[] reg = {
			0,0,
			1,1,1,1,1,1,1,1
		};
		implReadR(0x300000,reg);
		
		channel[0] = holdingRegister(0x300000);
		channel[1] = holdingRegister(0x300001);
		channel[2] = holdingRegister(0x300002);
		
		Application.invokeLater(()->{
			for(int i=0; i<channel.length; i++){
				if(channel[i]==null){
					continue;
				}
				double scale = Math.pow(10, (double)reg[i+2]); 
				ch_value[i].bind(
					channel[i]
					.subtract(19999)
					.divide(scale)
				);
				ch_v_txt[i].bind(
					ch_value[i]
					.asString("%3.1f")
				);
			}
		});
		super.ignite();//goto next stage~~~~
	}	
}
