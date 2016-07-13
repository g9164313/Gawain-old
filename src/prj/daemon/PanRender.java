package prj.daemon;

import com.jfoenix.controls.JFXButton;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.WindowEvent;
import narl.itrc.CamVidcap;
import narl.itrc.ImgRender;
import narl.itrc.Misc;
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
	
	private ImgRender rndr = new ImgRender(640,480,vid0);
	
	protected void eventShown(WindowEvent e){		
		rndr.play();
		checkPlaying();
	}
	
	protected void eventClose(WindowEvent e){
		rndr.stop();//let application release resource~~
	}
	
	private JFXButton btnPlay,btnSnap;
	
	private void checkPlaying(){
		if(rndr.isPlaying()==true){
			btnPlay.setText("暫停");
			btnPlay.setGraphic(Misc.getIcon("pause.png"));
		}else{
			btnPlay.setText("播放");
			btnPlay.setGraphic(Misc.getIcon("play.png"));
		}
	}
	
	@Override
	public Parent layout() {
		HBox lay0 = new HBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.getChildren().addAll(
			rndr.genPreview("預覽1")
			/*,rndr.genPreview("預覽2")*/
		);
		
		btnPlay = new JFXButton();
		btnPlay.getStyleClass().add("btn-raised");
		btnPlay.setOnAction(event->{
			rndr.pause();
			checkPlaying();
		});
		
		btnSnap = new JFXButton("擷取");
		btnSnap.getStyleClass().add("btn-raised");
		btnSnap.setGraphic(Misc.getIcon("camera.png"));
		btnSnap.setOnAction(event->{
			rndr.snap("snap.png");
		});
		
		final JFXButton btnClose = new JFXButton("關閉");		
		btnClose.getStyleClass().add("btn-raised");
		btnClose.setGraphic(Misc.getIcon("close.png"));
		btnClose.setOnAction(event->PanRender.this.dismiss());
		
		BorderPane root = new BorderPane();
		
		root.setCenter(lay0);
		root.setRight(PanBase.fillVBox(
			btnPlay, btnSnap,
			btnClose
		));
		return root;
	}
}
