package prj.scada;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import narl.itrc.PanBase;

public class PanMain1 extends PanBase {

	private final DevDCG100 dcg = new DevDCG100();
	
	public PanMain1() {
		
	}

	@Override
	public Node eventLayout(PanBase self) {
		
		stage().setOnShowing(e->{
			dcg.open("/dev/ttyUSB0,19200,8n1");
		});
		
	
		final BorderPane lay1 = new BorderPane();
		lay1.getStyleClass().add("ground-pad");
		//layB.setCenter(cam);
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("ground-pad");
		//lay0.setRight();
		//lay0.setCenter(layB);
		lay0.setLeft(DevDCG100.genPanel(dcg));
		
		return lay0;
	}
	
}
