package prj.refuge;

import com.jfoenix.controls.JFXTabPane;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import narl.itrc.BoxLogger;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;

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
		root.setRight(layout_ctrl());
		root.setBottom(layout_logger());
		return root;
	}
	
	private Node layout_ctrl(){
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("vbox-one-direction");
		lay0.getChildren().addAll(
			PanDecorate.group("CDR06"  , cdr.eventLayout(this)),
			PanDecorate.group("HUST-IO", hust.eventLayout(this)),
			PanDecorate.group("AT5350" , atom.eventLayout(this))
		);
		return lay0;
	}
	
	private Node layout_logger(){
		JFXTabPane pan = new JFXTabPane();
		pan.setPrefHeight(200.);
		Tab tab = new Tab();
		tab.setText("系統紀錄");
		tab.setContent(new BoxLogger());
		pan.getTabs().add(tab);
		return pan;
	}
}
