package prj.daemon;

import com.jfoenix.controls.JFXTabPane;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import narl.itrc.PanBase;

public class PanFringePattern extends PanBase {

	@Override
	public Node eventLayout(PanBase self) {
		
		
		
		final JFXTabPane lay1 = new JFXTabPane();
		lay1.getTabs().addAll(
			new Tab("qqq1"),
			new Tab("qqq2")
		);
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		lay0.setCenter(lay1);
		return lay0;
	}
}
