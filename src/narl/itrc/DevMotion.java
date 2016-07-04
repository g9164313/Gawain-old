package narl.itrc;

import java.util.HashMap;

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

	protected String DEF_UNIT="mm";
	
	/**
	 * Use this coefficient convert millimeter to pulse or counter.<p>
	 * The sequence is also same as stage axis-token.<p>
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
	 */
	private Double[] node = null;
	
	/**
	 * This array keep pulse/counter value.<p>
	 * Some motion devices support this array.<p>
	 * Remember, device must use 'makePosition' to reflect this variable.<p>
	 */
	public SimpleIntegerProperty[] pulse;
	
	protected void makePosition(int... val){		
		final Runnable event = new Runnable(){
			@Override
			public void run() {
				for(int i=0; i<table.size(); i++){
					int idx = table.get(i);
					if(idx>=val.length){
						continue;
					}
					pulse[i].set(val[idx]);
					//Misc.logv("pulse[%d]=%d",i,puls[i].get());
				}
			}
		};		
		if(Application.isEventThread()==false){			
			Application.invokeAndWait(event);
		}else{
			event.run();
		}
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
		pulse = new SimpleIntegerProperty[tkn.length];

		for(int i=0; i<tkn.length; i++){
			pulse[i] = new SimpleIntegerProperty();
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

	private void convert(Double[] val,String unit){
		for(int i=0; i<val.length; i++){
			val[i] = Misc.phyConvert(val[i],unit,DEF_UNIT);			
		}
	}
	
	private void routine(Double[] val){
		
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

	protected abstract void makeMotion(boolean isABS,Double[] value);

	private Thread async = null;

	public void asyncArchTo(String unit,Double... location){
		async = TskBase.macro("asyncArchTo", async,
			new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				archTo(DEF_UNIT,location);
				return null;
			}
		});
	}
	
	public void asyncArchTo(Double... location){
		asyncArchTo(DEF_UNIT,location);
	}
	
	public void asyncMoveTo(String unit,Double... offset){
		async = TskBase.macro("asyncArchTo", async,
			new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				moveTo(unit,offset);	
				return null;
			}
		});
	}
	
	public void asyncMoveTo(Double... offset){
		asyncMoveTo(DEF_UNIT,offset);
	}
	
	/**
	 * Stage must go to this absolute location.<p>
	 * @param location - x,y,z,a...
	 * @param unit - length unit, default is millimeter
	 */
	public void archTo(String unit,Double... location){
		convert(location,unit);
		routine(location);
		makeMotion(true,node);
	}
	
	/**
	 * Stage must go to this absolute location.<p>
	 * Default unit is millimeter.<p>
	 * @param location - [X,Y,Z,A] value
	 */
	public void archTo(Double... location){
		archTo("mm",location);
	}
	
	/**
	 * Stage will make a relative move.<p>
	 * @param offset - relative [X,Y,Z,A] value/step
	 * @param unit - length unit, default is millimeter
	 */
	public void moveTo(String unit,Double... offset){
		convert(offset,unit);
		routine(offset);
		makeMotion(false,node);
	}
	
	/**
	 * Stage will make a relative move.<p> 
	 * @param offset - relative [X,Y,Z,A] value/step
	 */
	public void moveTo(Double... offset){
		moveTo("mm",offset);
	}
	
	public abstract void setValue(Double[] value);
	
	/**
	 * User can reset 'step' or 'count'.<p>
	 * For motion card/controller, these values present the encoder position.
	 * @param step
	 */
	public void setStep(Integer[] step){
		Double[] val = Misc.Int2Double(step);
		routine(val);
		setValue(val);
	}
}
