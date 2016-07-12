package prj.daemon;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import narl.itrc.CamVidcap;
import narl.itrc.ImgRender;
import narl.itrc.PanBase;

/**
 * Test camera bundle and represent as template
 * @author qq
 *
 */
public class PanRender extends PanBase {

	public PanRender(){		
	}
	
	private CamVidcap vid0 = new CamVidcap("0");
	private CamVidcap vid1 = new CamVidcap("1");
	
	private ImgRender rndr = new ImgRender(640,480,vid0);
	
	protected void eventShown(WindowEvent e){
		
		rndr.play();
	}
	
	protected void eventClose(WindowEvent e){
		rndr.stop();//let application release resource~~
	}
	
	@Override
	public Parent layout() {
		HBox lay0 = new HBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.getChildren().addAll(
			rndr.genPreview("預覽1"),
			rndr.genPreview("預覽2")
		);
		
		final Button btnPlay = new Button("play");
		final Button btnPause= new Button("pause");
		final Button btnStop = new Button("stop");
		
		btnPlay.setOnAction(event->rndr.play());
		btnPause.setOnAction(event->rndr.pause());
		btnStop.setOnAction(event->rndr.stop());
		
		BorderPane root = new BorderPane();
		
		root.setCenter(lay0);
		root.setRight(PanBase.fillVBox(
			btnPlay,
			btnPause,
			btnStop
		));
		return root;
	}
}
