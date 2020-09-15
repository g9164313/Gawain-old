package prj.LPS_8S;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import narl.itrc.DevModbus;

public class ModCoupler extends DevModbus {

	public final IntegerProperty pin_in;
	
	public final BooleanProperty flg = new SimpleBooleanProperty();
	
	public ModCoupler() {
		looperDelay = 0;
		mapAddress("i8000-8004");//8005-->output
		pin_in = inputRegister(8000);
		//flg.bind(pin_in..isEqualTo(3));
		
	}
}
