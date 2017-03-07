package prj.daemon;

import java.math.BigDecimal;

import javafx.scene.Node;
import narl.itrc.DevTTY;
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
	public Node eventLayout() {
		return dev.build("test-panel");		
	}
	//-----------------------//
	
	//private DevNanoPZ dev = new DevNanoPZ();
	//private DevTTY dev = new DevTTY();
	private DevLK_G5000 dev = new DevLK_G5000("/dev/ttyS0");
}
