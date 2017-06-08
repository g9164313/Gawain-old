package prj.refuge;

import javafx.scene.Node;
import narl.itrc.DevModbus;
import narl.itrc.PanBase;

public class DevCDR06 extends DevModbus {

	public DevCDR06(){
		
	}
	//--------------------------------//
	
	@Override
	protected Node eventLayout(PanBase pan) {
		return null;
	}
}
