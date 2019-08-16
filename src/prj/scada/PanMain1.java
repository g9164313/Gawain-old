package prj.scada;

import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import narl.itrc.PanBase;

public class PanMain1 extends PanBase {

	private final DevDCG100 dcg = new DevDCG100();
	
	private final DevSPIK2000 spik = new DevSPIK2000();
	
	public PanMain1() {
		
	}

	@Override
	public Node eventLayout(PanBase self) {
		
		stage().setOnShowing(e->{
			//dcg.open("/dev/ttyUSB0,19200,8n1");
			spik.open("/dev/ttyUSB1,19200,8n1");
		});
		
		final TitledPane[] lay3 = {
			new TitledPane("DC 裝置", DevDCG100.genPanel(dcg)),
			new TitledPane("RF 裝置", DevSPIK2000.genPanel(spik))
		};
		final Accordion lay2 = new Accordion(lay3);
		lay2.setExpandedPane(lay3[1]);
		
		final BorderPane lay1 = new BorderPane();
		lay1.getStyleClass().add("ground-pad");
		//layB.setCenter(cam);
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("ground-pad");
		lay0.setCenter(lay1);
		lay0.setLeft(lay2);		
		return lay0;
	}
	
}
