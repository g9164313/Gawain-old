package prj.daemon;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.stage.WindowEvent;
import narl.itrc.BoxLogger;
import narl.itrc.PanBase;

public class PanCalculateFringe extends PanBase {

	public PanCalculateFringe() {
	}
	//--------------------//
	
	//private ImgPreview prv = new ImgPreview(800,600);  
	
	//--------------------//
	
	@Override
	protected void eventShown(WindowEvent e){	
	}
	
	@Override
	public Node eventLayout(PanBase pan) {
		
		final BorderPane root = new BorderPane();
		//lay1.setRight(layout_action());
		//root.setCenter(prv);
		return root;
	}
}
