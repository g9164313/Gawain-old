package prj.daemon;

import com.jfoenix.controls.JFXButton;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.WindowEvent;
import narl.itrc.CamVidcap;
import narl.itrc.CamRender;
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
	
	private CamVidcap vid0 = new CamVidcap("V4L:0");
	
	private CamRender rndr = new CamRender(640,480,vid0);
	
	protected void eventShown(WindowEvent e){		
		rndr.play();
		checkPlaying();
	}
	
	protected void eventClose(WindowEvent e){
		rndr.stop();//let application release resource~~
	}
	
	private JFXButton btnPlaying,btnSnapImg,btnSetting;
	
	private void checkPlaying(){
		if(rndr.isPlaying()==true){
			btnPlaying.setText("暫停");
			btnPlaying.setGraphic(Misc.getIcon("pause.png"));
			btnSnapImg.setDisable(false);
			btnSetting.setDisable(false);
		}else{
			btnPlaying.setText("播放");
			btnPlaying.setGraphic(Misc.getIcon("play.png"));
			btnSnapImg.setDisable(true);
			btnSetting.setDisable(true);
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
		
		btnPlaying = new JFXButton();
		btnPlaying.getStyleClass().add("btn-raised");
		btnPlaying.setOnAction(event->{
			rndr.pause();
			checkPlaying();
		});
		
		btnSnapImg = new JFXButton("擷取");
		btnSnapImg.getStyleClass().add("btn-raised");
		btnSnapImg.setGraphic(Misc.getIcon("camera.png"));
		btnSnapImg.setOnAction(event->{
			rndr.snap("snap.png");
		});
		
		btnSetting = new JFXButton("設定");
		btnSetting.getStyleClass().add("btn-raised");
		btnSetting.setGraphic(Misc.getIcon("wrench.png"));
		btnSetting.setOnAction(event->rndr.getBundle(0).showPanel());
		
		final JFXButton btnClose = new JFXButton("關閉");		
		btnClose.getStyleClass().add("btn-raised");
		btnClose.setGraphic(Misc.getIcon("close.png"));
		btnClose.setOnAction(event->PanRender.this.dismiss());
		
		BorderPane root = new BorderPane();
		
		root.setCenter(lay0);
		root.setRight(PanBase.fillVBox(
			btnPlaying, 
			btnSnapImg, 
			btnSetting,
			btnClose
		));
		return root;
	}
}
