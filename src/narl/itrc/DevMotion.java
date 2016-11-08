package narl.itrc;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;

/**
 * Every motion card should provide methods.<p>
 * These devices use pulse to communicate with stepper or servo motor(driver).<p>
 * @author qq
 *
 */
public abstract class DevMotion {

	public DevMotion(){		
	}

	protected String BASE_UNIT="mm";
	
	/**
	 * This is a special unit for motion device
	 */
	public static final String PULSE_UNIT="pls";
	
	/**
	 * Use this coefficient convert millimeter to pulse or counter.<p>
	 * The sequence is also same as stage axis-token.<p>
	 * The token is (1,2,3,4...) or (A,B,C,D...).<p>
	 */	
	private double[] factor = null;

	/**
	 * Use this method convert millimeter to pulse or counter.<p>
	 * @param val - pulse(counter) per millimeter
	 */
	public void setFactor(double... val){
		factor = val;
	}
	
	/**
	 * This array keep the input value(move or arch).<p>
	 * The sequence is also same as stage axis-token.<p>
	 * The token is (1,2,3,4...) or (A,B,C,D...).<p>
	 */
	private Double[] node = null;
	
	/**
	 * This array keep pulse/counter value.<p>
	 * Remember, device must use 'makePosition' to reflect this variable.<p>
	 * The token is (X,Y,Z,A...).<p>
	 */
	public SimpleIntegerProperty[] pulse = {
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty(),
		new SimpleIntegerProperty()
	};
	
	protected void updateCounter(int... val){		
		final Runnable event = new Runnable(){
			@Override
			public void run() {
				for(int i=0; i<pulse.length; i++){
					Integer idx = table.get(i);
					if(idx==null){
						continue;
					}
					if(idx>=val.length){
						continue;
					}
					pulse[i].set(val[idx]);
					//Misc.logv("pulse[%d]=%d",i,pulse[i].get());
				}
			}
		};		
		if(Application.isEventThread()==false){			
			Application.invokeAndWait(event);
		}else{
			event.run();
		}
	}
	
	public int getPulse(char tkn){
		int idx = tkn2idx(tkn);
		return pulse[idx].getValue();
	}
	
	public double getPosition(char tkn){
		int idx = tkn2idx(tkn);
		return (pulse[idx].getValue() * factor[idx]);
	}
	
	/**
	 * Key is the sequence of location (X,Y,Z,A...)<p>
	 * Value is the sequence of stage axis-token (1,2,3,4...) or (A,B,C,D...)<p>
	 */
	private HashMap<Integer,Integer> table = new HashMap<Integer,Integer>();
	
	/**
	 * Every stage should map axis to the fixed sequence.<p>
	 * The sequence is (X,Y,Z,A...), etc.<p>
	 * Remember, user must set token-base
	 * @param tkn - the name of motion stage port or axis.
	 *   Except '-' and 'space', these two tokens mean 'no support'.
	 *   The definition of axis token must also be a chain like A~Z or 1~10.
	 *   The first token is special, it is the first index.    
	 */
	public void setRoutine(char... tkn){
		if(tknBase==0){
			Misc.loge("No token-base");
			return;
		}
		
		node = new Double[tkn.length];
		for(int i=0; i<tkn.length; i++){
			if(tkn[i]=='-' || tkn[i]==' '){
				continue;
			}
			int j = (int)(tkn[i]-tknBase);
			if(j<0){
				Misc.loge("Invalid token: %c -> %d",tkn[i],j);
				continue;
			}
			table.put(i,j);
		}
	}
	
	private char tknBase = 0;
	
	public void setTokenBase(char base){
		tknBase = base;
	}

	private void convert(String unit,Double[] val){
		if(unit.equalsIgnoreCase(PULSE_UNIT)==true){
			return;
		}
		for(int i=0; i<val.length; i++){
			if(val[i]==null){
				continue;
			}
			val[i] = Misc.phyConvert(val[i],unit,BASE_UNIT);			
		}
	}
	
	private void routine(String unit,Double[] val){
		
		if(table.isEmpty()==true || node==null){
			return;
		}
		for(int i=0; i<node.length; i++){
			node[i] = null;//reset all value again~~~
		}
		
		for(int i=0; i<val.length; i++){
			Integer j = table.get(i);
			if(j==null){
				continue;
			}
			if(val[i]==null){
				continue;
			} 
			node[j] = val[i];
		}
		if(unit.equalsIgnoreCase(PULSE_UNIT)==true){
			return;
		}
		
		if(factor!=null){
			//multiply coefficient
			for(int i=0; i<node.length; i++){
				if(node[i]==null){
					continue;
				}
				node[i] = node[i] * factor[i];
			}
		}		
	}

	protected int tkn2idx_route(char tkn){
		Integer idx = table.get(tkn2idx(tkn));
		if(idx==null){
			Misc.loge("Wrong axis name ("+tkn+")");
			return 0;
		}
		return idx;
	}
	
	private int tkn2idx(char tkn){
		switch(tkn){
		case 'x':
		case 'X':
			return 0;
		case 'y':
		case 'Y':
			return 1;
		case 'z':
		case 'Z':
			return 2;
		case 'a':
		case 'A':
			return 3;
		}
		return 0;
	}
	
	private Double[] dbl_array = null;
	protected Double[] prepare_double_array(double val,char tkn){
		if(table.isEmpty()==true || node==null){
			return null;
		}
		if(dbl_array==null){
			dbl_array = new Double[node.length];
		}
		for(int i=0; i<node.length; i++){
			dbl_array[i] = null;
		}
		dbl_array[tkn2idx(tkn)] = val;
		return dbl_array;
	}
	
	
	private Integer[] int_array = null;
	protected Integer[] prepare_int_array(int val,char tkn){
		if(table.isEmpty()==true || node==null){
			return null;
		}
		if(int_array==null){
			int_array = new Integer[node.length];
		}
		for(int i=0; i<node.length; i++){
			int_array[i] = null;
		}
		int_array[tkn2idx(tkn)] = val;
		return int_array;
	}

	/**
	 * The way to drive motor must be implemented in this method.<p>
	 * @param isABS - check whether this motion is absolute ot relative mode.<p>
	 * @param value - the sequence is [A,B,C,D] or [1,2,3,4]
	 */
	protected abstract void makeMotion(boolean isABS,Double... value);

	private Thread asyncThread = null;

	private AtomicBoolean asyncDone = new AtomicBoolean(true);
	
	public AtomicBoolean asyncAnchorTo(final String unit,Double... location){
		asyncDone.set(false);
		asyncThread = TskAction.create(
			"asyncArchTo", 
			asyncThread,
			new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				anchorTo(unit,location);
				asyncDone.set(true);
				return null;
			}
		});
		return asyncDone;
	}
	
	public AtomicBoolean asyncAnchorTo(Double... location){
		return asyncAnchorTo(PULSE_UNIT,location);
	}
	
	public AtomicBoolean asyncAnchorTo(char token,Double location){
		return asyncAnchorTo(PULSE_UNIT,prepare_double_array(location,token));
	}
	
	public AtomicBoolean asyncMoveTo(final String unit,Double... offset){
		asyncDone.set(false);
		asyncThread = TskAction.create(
			"asyncMoveTo",
			asyncThread,
			new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				moveTo(unit,offset);
				asyncDone.set(true);
				return null;
			}
		});
		return asyncDone;
	}
	
	public AtomicBoolean asyncMoveTo(Double... offset){
		return asyncMoveTo(PULSE_UNIT,offset);
	}
	
	public AtomicBoolean asyncMoveTo(char token,Double offset){
		return asyncMoveTo(PULSE_UNIT,prepare_double_array(offset,token));
	}	
	//-------------------------------------------------------//
	
	/**
	 * Stage must go to this absolute location.<p>
	 * @param unit - length unit, default is millimeter.<p>
	 * @param location - relative [X,Y,Z,A] value/step.<p>
	 */
	public void anchorTo(final String unit,Double... location){
		convert(unit,location);
		routine(unit,location);
		makeMotion(true,node);
	}
		
	/**
	 * Stage must go to this absolute location.<p>
	 * Default unit is millimeter.<p>
	 * @param location - relative [X,Y,Z,A] value/step.<p>
	 */
	public void anchorTo(Double... location){
		anchorTo(PULSE_UNIT,location);
	}
	
	/**
	 * Stage must go to this absolute location.<p>
	 * Default unit is millimeter.<p>
	 * @param unit - length unit, default is millimeter.<p>
	 * @param location - [X,Y,Z,A] value.<p>
	 * @param token - the sign for [X,Y,Z,A]
	 */
	public void anchorTo(char token,final String unit,Double location){
		anchorTo(unit,prepare_double_array(location,token));
	}
	
	/**
	 * Stage must go to this absolute location.<p>
	 * Default unit is millimeter.<p>
	 * @param location - .<p>
	 * @param token - the sign for [X,Y,Z,A].<p>
	 */
	public void anchorTo(char token,Double location){
		anchorTo(PULSE_UNIT,prepare_double_array(location,token));
	}

	/**
	 * Stage will make a relative move.<p>
	 * @param offset - relative [X,Y,Z,A] value/step.<p>
	 * @param unit - length unit, default is millimeter.<p>
	 */
	public void moveTo(final String unit,Double... offset){		
		convert(unit,offset);
		routine(unit,offset);
		makeMotion(false,node);
	}
	
	/**
	 * Stage will make a relative move.<p> 
	 * @param offset - relative [X,Y,Z,A] value/step
	 */
	public void moveTo(Double... offset){		
		moveTo(PULSE_UNIT,offset);
	}
	
	/**
	 * Stage will make a relative move.<p>	
	 * @param unit  - the unit for offset
	 * @param offset- relative [X,Y,Z,A] value/step
	 * @param token - the sign for [X,Y,Z,A]
	 */
	public void moveTo(char token,final String unit,Double offset){		
		moveTo(unit,prepare_double_array(offset,token));
	}
	
	/**
	 * Stage will make a relative move.<p>
	 * @param token - the sign for [X,Y,Z,A].<p>
	 * @param offset- relative [X,Y,Z,A] value/step.<p>
	 */
	public void moveTo(char token,Double offset){		
		moveTo(PULSE_UNIT,prepare_double_array(offset,token));
	}

	
	/**
	 * This is a special motion, motor is just running and stop at any time.<p> 
	 */
	public abstract void makeJogging(boolean go,Double[] step);
	
	
	public void joggingTo(boolean go,String unit,Double... step){
		convert(unit,step);
		routine(unit,step);
		makeJogging(go,node);
	}
	
	public void joggingTo(boolean go,String unit,Double step,char token){
		joggingTo(
			go,unit,
			prepare_double_array(step,token)
		);
	}
	
	public void joggingTo(boolean go,Double... step){
		joggingTo(
			go,PULSE_UNIT,
			step
		);
	}
	
	public void joggingTo(boolean go,Double step,char token){
		joggingTo(
			go,PULSE_UNIT,
			prepare_double_array(step,token)
		);
	}
	
	/**
	 * Reset counter or stepper in motion device
	 * @param value - the sequence is [A,B,C,D] or [1,2,3,4].<p>
	 */
	protected abstract void setPosition(Double[] value);
	
	/**
	 * User can reset 'step' or 'count'.<p>
	 * For motion card/controller, these values present the encoder position.
	 * @param step - the sign for [X,Y,Z,A].<p>
	 */
	public void setValue(Integer... step){
		Double[] val = Misc.Int2Double(step);
		routine(PULSE_UNIT,val);
		setPosition(node);
	}
	
	/**
	 * User can reset 'step' or 'count' by token.<p>
	 * For motion card/controller, these values present the encoder position.
	 * @param step
	 */
	public void setValue(int step,char tkn){
		setValue(prepare_int_array(step,tkn));
	}
	
	/**
	 * Update counter or stepper in motion device
	 * 
	 */
	protected abstract void getPosition();	
}
