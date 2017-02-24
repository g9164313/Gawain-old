package prj.daemon;

import javafx.scene.Parent;
import narl.itrc.PanBase;

/**
 * It is just for testing device or observing view.
 * @author qq
 *
 */
public class PanNullView extends PanBase {

	public PanNullView(){
		
	}
	
	@Override
	public Parent layout() {
		return dev.build();
	}
	//-----------------------//
	
	private DevNanoPZ dev = new DevNanoPZ();
}
