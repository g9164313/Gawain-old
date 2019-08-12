package prj.scada;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import narl.itrc.PanBase;

public class PanMain1 extends PanBase {

	public PanMain1() {
		
	}

	@Override
	public Node eventLayout(PanBase self) {
		
		
		
		final BorderPane lay1 = new BorderPane();
		lay1.getStyleClass().add("ground-pad");
		//layB.setCenter(cam);
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("ground-pad");
		//lay0.setLeft(lay1);
		//lay0.setCenter(layB);		
		//lay0.setRight(layA);
		return lay0;
	}
	
}
