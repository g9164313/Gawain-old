package prj.refuge;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
	
	private WidMarkTable mark = new WidMarkTable(
		DevHustIO.ISOTOPE_NAME[0],
		DevHustIO.ISOTOPE_NAME[1],
		DevHustIO.ISOTOPE_NAME[2]
	);
	
	public PanEntry(){
		//firstAction = FIRST_MAXIMIZED;
		customStyle = "-fx-background-color: white;";
	}
	
	@Override
	protected void eventShown(WindowEvent e){
		cdr.connect("");
		cdr.layout_grid();//re-layout again!!!
		cdr.update_auto(true);
		
		//hust.connect("");
		//atom.connect("");
	}
	//-------------------------------//
	
	private Node layHust, layAtom;
	
	@Override
	public Node eventLayout(PanBase self) {		
		BorderPane root = new BorderPane();
		root.setCenter(mark);
		root.setRight(layout_ctrl());
		root.setBottom(layout_actn());
		return root;
	}
	
	private Node layout_ctrl(){
		VBox lay0 = new VBox();
		layHust = hust.eventLayout(this);
		layAtom = atom.eventLayout(this);
		lay0.getStyleClass().add("vbox-one-direction");
		lay0.getChildren().addAll(			
			PanDecorate.group("HUST-IO", layHust),
			PanDecorate.group("AT5350" , layAtom)
		);
		return lay0;
	}
	
	private Node layout_actn(){

		Node nd1 = PanDecorate.group("溫溼度計", cdr.eventLayout(this));
		
		Node nd2 = PanDecorate.group("訊息紀錄",new BoxLogger());
		HBox.setHgrow(nd2, Priority.ALWAYS);
		
		final Button btnLoadRec = PanBase.genButton2("載入","toc.png");
		btnLoadRec.setMinWidth(93);
		btnLoadRec.setOnAction(event->{
			
		});
		
		final Button btnKickOff = PanBase.genButton3("量測","arrow-right-drop-circle-outline.png");
		btnKickOff.setMinWidth(93);
		btnKickOff.setMinHeight(93);		
		btnKickOff.setOnAction(event->{
			
		});
		
		VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		HBox lay0 = new HBox();
		lay0.getStyleClass().add("hbox-medium");
		lay1.getChildren().addAll(btnLoadRec,btnKickOff);
		lay0.getChildren().addAll(nd1,nd2,lay1);
		return lay0;
	}
}
