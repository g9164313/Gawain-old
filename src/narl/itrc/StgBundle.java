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

}
