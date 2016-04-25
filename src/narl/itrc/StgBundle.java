package narl.itrc;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public abstract class StgBundle implements Gawain.EventHook {

	public StgBundle(){
		Gawain.hook(this);
	}
		
	@Override
	public void shutdown() {
		close();
	}
		
	public abstract void setup(int idx,String confName);
	public abstract long getPosition(int idx);
	public abstract void setPosition(char typ,int idx,int pulse);
	public abstract void setJogger(char typ,int idx);
	public abstract void close();
	
	//the type of motion
	//'*' = stall, do nothing~~~ 
	//'r','R' = relative,
	//'a','A' = absolute, 
	//'+','-' = positive and negative jog
	protected char type = '*';//the type of motion	
	protected int aidx = -1;//which axis~~
	protected int ppse = 0;//how many pulse will be consumed~~~
	
	private SimpleIntegerProperty axis0 = new SimpleIntegerProperty();
	private SimpleIntegerProperty axis1 = new SimpleIntegerProperty();
	private SimpleIntegerProperty axis2 = new SimpleIntegerProperty();
	private SimpleIntegerProperty axis3 = new SimpleIntegerProperty();
	private SimpleIntegerProperty axis4 = new SimpleIntegerProperty();
	private SimpleIntegerProperty axis5 = new SimpleIntegerProperty();
	
	public ReadOnlyIntegerProperty axis0Property(){ return axis0; }
	public ReadOnlyIntegerProperty axis1Property(){ return axis1; }
	public ReadOnlyIntegerProperty axis2Property(){ return axis2; }
	public ReadOnlyIntegerProperty axis3Property(){ return axis3; }
	public ReadOnlyIntegerProperty axis4Property(){ return axis4; }
	public ReadOnlyIntegerProperty axis5Property(){ return axis5; }
	
	public ReadOnlyIntegerProperty getAxisProperty(int aid){
		return getAxis(aid);
	}
	
	protected SimpleIntegerProperty getAxis(int aid){
		switch(aid){
		case 0: return axis0;
		case 1: return axis1;
		case 2: return axis2;
		case 3: return axis3;
		case 4: return axis4;
		case 5: return axis5;
		}
		return null;
	}

	protected void setAxis(int aid,int val){
		final int new_val = val;
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				SimpleIntegerProperty axs = getAxis(aid);
				if(axs==null){
					return;
				}
				axs.set(new_val);
			}				
		});
	}
	
	protected char checkTypeJog(char cc){
		if(cc=='+' || cc=='-'){
			return cc;
		}
		return '*';
	}
	
	protected char checkType(char cc){
		if(
			cc=='*' || 
			cc=='r' || cc=='R' || 
			cc=='a' || cc=='A' ||
			cc=='+' || cc=='-'
		){
			return cc;
		}
		return '*';
	}
}
