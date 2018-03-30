package prj.daemon;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import narl.itrc.BoxLogger;
import narl.itrc.PanBase;


/**
 * It is just for testing device or observing view.
 * @author qq
 *
 */
public class PanNullView extends PanBase {

	public PanNullView(){
	}

	//private CamVidcap cam= new CamVidcap();	
	//private CamDummy cam = new CamDummy(Misc.pathRoot+"../bang/artificial-2.png");
	
	private WidFringeView map = new WidFringeView();
	
	//-----------------------//
	
	@Override
	public Node eventLayout(PanBase pan) {
		
		final BoxLogger box = new BoxLogger();
		box.setPrefHeight(120);
		
		//GridPane lay0 = new GridPane();
		//lay0.getStyleClass().add("grid-small");
		//GridPane.setHgrow(cam, Priority.ALWAYS);
		//GridPane.setVgrow(cam, Priority.ALWAYS);
		//GridPane.setHalignment(cam, HPos.CENTER);
		//GridPane.setValignment(cam, VPos.CENTER);
		//lay0.add(cam, 0, 0);
		//lay0.add(map, 0, 0);
				
		final BorderPane lay1 = new BorderPane();		
		lay1.setBottom(box);
		//lay1.setCenter(cam);
		lay1.setCenter(map);
		
		final BorderPane lay2 = new BorderPane();
		lay2.setRight(layout_action());
		//lay1.setLeft(layout_action());
		lay2.setCenter(lay1);	
		return lay2;
	}
	
	private Node layout_action(){
		
		final Button btn1 = PanBase.genButton2("test-1",null);
		btn1.setOnAction(e->{
			//dev.addEvent(btn1, e1->{
			//	Misc.logv("~~button click~~");
			//});
			map.loadImageFile("../bang/roi-01.png");
		});
		
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium-vertical");
		lay.add(btn1, 0, 0);
		return lay;
	}
	
	@Override
	public void eventShown(PanBase self) {
	}
}
