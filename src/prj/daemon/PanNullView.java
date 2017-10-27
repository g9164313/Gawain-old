package prj.daemon;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.WindowEvent;
import narl.itrc.BoxLogger;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.vision.CamDummy;
import narl.itrc.vision.CamVidcap;

/**
 * It is just for testing device or observing view.
 * @author qq
 *
 */
public class PanNullView extends PanBase {

	public PanNullView(){
	}

	//private CamVidcap cam0 = new CamVidcap();	
	private CamDummy cam1 = new CamDummy(Misc.pathRoot+"../bang/artificial-2.png");
	
	//private DevRender render = new DevRender(cam);
	
	@Override
	protected void eventShown(WindowEvent e){
		//cam0.setup();
		//cam0.timeRender(60.);
		
		cam1.setup();
		//cam1.timeRender(500.);
		
		//render.launchTask("cam-render");
		
		//dev.setUsual(e1->{
		//	Misc.delay(1000);
		//	Misc.logv("usual routine...");
		//});
		//dev.launchTask("dev-null");		
	}
	//-----------------------//
	
	@Override
	public Node eventLayout(PanBase pan) {
		
		final BoxLogger box = new BoxLogger();
		box.setPrefHeight(120);
		
		GridPane lay0 = new GridPane();
		lay0.getStyleClass().add("grid-small");
		//lay0.add(cam0, 0, 0);
		lay0.add(cam1, 1, 0);
		
		final BorderPane lay1 = new BorderPane();		
		lay1.setBottom(box);
		lay1.setCenter(lay0);
		
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
		});
		
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium-vertical");
		lay.add(btn1, 0, 0);
		return lay;
	}
}
