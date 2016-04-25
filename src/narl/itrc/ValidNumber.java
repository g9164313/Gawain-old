package narl.itrc;

import javafx.beans.DefaultProperty;
import javafx.scene.control.TextInputControl;

import com.jfoenix.validation.base.ValidatorBase;

@DefaultProperty(value="icon")
public class ValidNumber extends ValidatorBase {
		
	private final int TYP_INTEGER_RANGE= 0x10;
	private final int TYP_LONG_RANGE   = 0x20;
	private final int TYP_FLOAT_RANGE  = 0x30;
	private final int TYP_DOUBLE_RANGE = 0x40;
	
	private int typ = TYP_INTEGER_RANGE;
	
	private Number min,max;
	
	public ValidNumber(int _min,int _max){
		typ = TYP_INTEGER_RANGE;
		min = new Integer(_min);
		max = new Integer(_max);
	}
	
	public ValidNumber(long _min,long _max){
		typ = TYP_DOUBLE_RANGE;
		min = new Long(_min);
		max = new Long(_max);
	}
	
	public ValidNumber(float _min,float _max){
		typ = TYP_FLOAT_RANGE;
		min = new Float(_min);
		max = new Float(_max);
	}
	
	public ValidNumber(double _min,double _max){
		typ = TYP_DOUBLE_RANGE;
		min = new Double(_min);
		max = new Double(_max);
	}
	
	@Override
	protected void eval() {
		if(srcControl.get() instanceof TextInputControl){
			TextInputControl box = (TextInputControl)srcControl.get();
			String val = box.getText();
			boolean res = true;			
			switch(typ){
			case TYP_INTEGER_RANGE:
				res = IntOutofRange(val);
				break;
			case TYP_LONG_RANGE:
				res = LongOutofRange(val);
				break;
			case TYP_FLOAT_RANGE:
				res = FloatOutofRange(val);
				break;
			case TYP_DOUBLE_RANGE:
				res = DoubleOutofRange(val);
				break;
			}
			hasErrors.set(res);
		}
	}

	private boolean IntOutofRange(String txt){
		try{
			int val = Integer.parseInt(txt);
			if(min.intValue()<=val && val<=max.intValue()){
				return false;
			}
		}catch(NumberFormatException e){
			return true;
		}
		return true;
	}
	
	private boolean LongOutofRange(String txt){
		try{
			long val = Long.parseLong(txt);
			if(min.longValue()<=val && val<=max.longValue()){
				return false;
			}
		}catch(NumberFormatException e){
			return true;
		}
		return true;
	}
	
	private boolean FloatOutofRange(String txt){
		try{
			float val = Float.parseFloat(txt);
			if(min.floatValue()<=val && val<=max.floatValue()){
				return false;
			}
		}catch(NumberFormatException e){
			return true;
		}
		return true;
	}
	
	private boolean DoubleOutofRange(String txt){
		try{
			double val = Double.parseDouble(txt);
			if(min.doubleValue()<=val && val<=max.doubleValue()){
				return false;
			}
		}catch(NumberFormatException e){
			return true;
		}
		return true;
	}
}
