package prj.daemon;

import javafx.scene.Node;
import narl.itrc.PanBase;

/**
 * It is just for testing device or observing view.
 * @author qq
 *
 */
public class PanNullView extends PanBase {

	public PanNullView(){
		customStyle = "-fx-background-color: #FDFDFD;";
	}
	
	@Override
	public Node eventLayout(PanBase pan) {
		return null;
	}
	//-----------------------//
	
	//private DevNanoPZ dev = new DevNanoPZ();
	//private DevTTY dev = new DevTTY();
	//private DevLK_G5000 dev = new DevLK_G5000();
}
