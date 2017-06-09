package prj.refuge;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.WindowEvent;
import narl.itrc.PanBase;

/**
 * A utility change routine for calibrate radiation 
 * @author qq
 *
 */
public class PanEntry extends PanBase {

	private DevCDR06  cdr  = new DevCDR06();
	private DevHustIO hust = new DevHustIO();
	private DevAT5350 atom = new DevAT5350();
	
	public PanEntry(){
	}
	
	@Override
	protected void eventShown(WindowEvent e){
		cdr.connect("");
		cdr.layout_grid();
		cdr.update_auto(true);
	}
	//-------------------------------//
	
	@Override
	public Node eventLayout(PanBase self) {
		
		BorderPane root = new BorderPane();
		root.setBottom(layout_info());
		return root;
	}
	
	private Node layout_info(){
		
		HBox lay0 = new HBox();
		
		lay0.getChildren().addAll(
			cdr.eventLayout(this)
		);
		return lay0;
	}
}
