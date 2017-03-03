package prj.daemon;

import com.jfoenix.controls.JFXBadge;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.controls.JFXDrawer;
import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.controls.JFXSnackbar.SnackbarEvent;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import narl.itrc.DevTTY;
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
		//return dev.build("NanoPZ");
		return dev.build("DevTTY");		
	}
	//-----------------------//
	
	//private DevNanoPZ dev = new DevNanoPZ();
	private DevTTY dev = new DevTTY();
}
