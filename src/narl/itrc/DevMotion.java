package narl.itrc;

import java.util.HashMap;

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
	
	public void setFactor(double... val){
		factor = val;
	}
	
	/**
	 * This array keep the input value(move or arch).<p>
	 * The sequence is also same as stage axis-token.<p>
	 */
	private Double[] node = null;
	
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
	 *   Except '-' and 'space', two token mean 'no support'.
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

	private void convert(double[] val,String unit){
		for(int i=0; i<val.length; i++){
			val[i] = Misc.phyConvert(val[i],unit,DEF_UNIT);			
		}
	}
	
	private void routine(double[] val,boolean flagAbs){
		
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
		
		mapping(flagAbs,node);
	}
	
	public abstract void mapping(boolean abs,Double[] value);

	/**
	 * Stage must go to this absolute location.<p>
	 * @param location - x,y,z,a...
	 * @param unit - length unit, default is millimeter
	 */
	public void archTo(double[] location,String unit){
		convert(location,unit);
		routine(location,true);
	}
	
	/**
	 * Stage must go to this absolute location.<p>
	 * Default unit is millimeter.<p>
	 * @param location - [X,Y,Z,A] value
	 */
	public void archTo(double[] location){
		archTo(location,"mm");
	}
	
	/**
	 * Stage will make a relative move.<p>
	 * @param offset - relative [X,Y,Z,A] value/step
	 * @param unit - length unit, default is millimeter
	 */
	public void moveTo(double[] offset,String unit){
		convert(offset,unit);
		routine(offset,false);
	}
	
	/**
	 * Stage will make a relative move.<p> 
	 * @param offset - relative [X,Y,Z,A] value/step
	 */
	public void moveTo(double[] offset){
		moveTo(offset,"mm");
	}
}
