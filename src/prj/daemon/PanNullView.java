package prj.daemon;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import narl.itrc.BoxLogger;
import narl.itrc.DevBase;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * It is just for testing device or observing view.
 * @author qq
 *
 */
public class PanNullView extends PanBase {

	public PanNullView(){
	}

	@Override
	public Node eventLayout(PanBase pan) {
		
		dev.taskLaunch("dev-null");
		dev.setUsual(e1->{
			Misc.delay(1000);
			Misc.logv("usual routine...");
		});
		
		final BoxLogger box = new BoxLogger();
		box.setPrefHeight(120);
		
		final BorderPane lay0 = new BorderPane();		
		final BorderPane lay1 = new BorderPane();
		
		lay1.setBottom(box);
		
		lay0.setRight(layout_action());
		lay0.setCenter(lay1);	
		return lay0;
	}
	//-----------------------//
	
	private class DevNullTest extends DevBase {
		public DevNullTest(){			
		}
		@Override
		public void eventShutdown() {
		}
	};
	
	private DevNullTest dev = new DevNullTest(); 
	
	//private DevNanoPZ dev = new DevNanoPZ();
	//private DevTTY dev = new DevTTY();
	//private DevLK_G5000 dev = new DevLK_G5000();
		
	private Node layout_action(){
		

		final Button btn1 = PanBase.genButton2("test-1",null);
		btn1.setOnAction(e->{
			dev.addEvent(btn1, e1->{
				Misc.logv("~~button click~~");
			});
		});
		
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium-vertical");
		lay.add(btn1, 0, 0);
		return lay;
	}
}
