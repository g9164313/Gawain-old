package prj.scada;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXDrawer;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXToggleButton;
import com.jfoenix.controls.JFXToggleNode;
import com.jfoenix.transitions.hamburger.HamburgerSlideCloseTransition;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanEntry extends PanBase {

	private DevSPIK2000 dev = new DevSPIK2000();
	
	public PanEntry(){
	}

	@Override
	public Node eventLayout(PanBase self) {
	
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		//lay0.setLeft(checkBox);
		lay0.setCenter(PanLayout.gen_information(dev));
		//lay0.setRight(lay3);
		return lay0;
	}

	@Override
	public void eventShown(PanBase self) {
		//dev.link("\\\\.\\COM2,19200,8n1");		
	}

	@Override
	public void eventClose(PanBase self) {
		//dev.unlink();
	}
}
