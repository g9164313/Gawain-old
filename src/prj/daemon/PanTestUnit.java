package prj.daemon;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import narl.itrc.PanBase;

public class PanTestUnit extends PanBase {

	public PanTestUnit() {		
	}
	
	private DevLKIF2 lkif = new DevLKIF2(); 
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		stage().setOnShown(e->{
			lkif.open();
		});
		
		final BorderPane lay0 = new BorderPane();
		//lay0.setCenter(DevLKIF2.genPanel(lkif,0));
		lay0.setCenter(DevLKIF2.genPanelMulti(lkif,0,1));
		//lay0.setRight(lay2);
		return lay0;
	}
}
