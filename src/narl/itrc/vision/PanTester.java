package narl.itrc.vision;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import narl.itrc.PanBase;

public class PanTester extends PanBase {

	DevPreview prvw = new DevPreview();
	
	public PanTester(){
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		//Group grp = new Group();
		//grp.getStyleClass().add("group-border");
		//grp.getChildren().add();
		
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		//lay0.setLeft(lay1);
		lay0.setCenter(prvw.getNode());
		//lay0.setRight(lay3);
		return lay0;
	}

	@Override
	public void eventShown(Object[] args) {
		prvw.test_load_file();
	}
}
