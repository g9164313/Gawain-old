package prj.daemon;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.WindowEvent;
import narl.itrc.BoxLogger;
import narl.itrc.DevBase;
import narl.itrc.PanBase;
import narl.itrc.vision.CamBundle;
import narl.itrc.vision.CamVidcap;
import narl.itrc.vision.DevRender;

/**
 * It is just for testing device or observing view.
 * @author qq
 *
 */
public class PanNullView extends PanBase {

	public PanNullView(){
	}

	private CamVidcap vidcap = new CamVidcap();
	
	//private ImgPreview prv = new ImgPreview(800,600);
	
	private DevRender render = new DevRender(vidcap);
	
	@Override
	protected void eventShown(WindowEvent e){
		
		render.launchTask("cam-render");
		
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
		
		final BorderPane lay0 = new BorderPane();		
		lay0.setBottom(box);
		lay0.setCenter(vidcap);
		
		final BorderPane lay1 = new BorderPane();
		lay1.setRight(layout_action());
		//lay1.setLeft(layout_action());
		lay1.setCenter(lay0);	
		return lay1;
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
