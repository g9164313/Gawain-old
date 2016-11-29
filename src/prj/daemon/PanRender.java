package prj.daemon;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.WindowEvent;
import narl.itrc.CamFlyCapture;
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
		//final String testFile = ":/home/qq/labor/opencv-3.1/contrib/modules/text/samples/scenetext_segmented_word%02d.jpg";
		//rndr = new ImgRender(new CamVidcap("FILE:0"+testFile));
		rndr = new ImgRender(new CamVidcap("0"));
		//rndr = new ImgRender(new CamVidcap("VFW:0"));
		//rndr = new ImgRender(new CamMulticam("ral12288-FULL"));
		//rndr = new ImgRender(new CamFlyCapture());
	}
	
	protected ImgRender rndr = null;
	
	@Override
	protected void eventShown(WindowEvent e){
		rndr.play();
		//Here, we add control item to test function~~~
		panControl.getChildren().add(new FilterNMText().getControl(rndr));
		panControl.getChildren().add(new FilterIsBlur().getPanel(rndr));
	}
	
	@Override
	protected void eventClose(WindowEvent e){
	}
	
	protected Pane panControl = null;
	
	@Override
	public Parent layout() {
		HBox lay0 = new HBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.getChildren().addAll(
			rndr.getPreview(0)
		);

		final JFXButton btnSnapImg = new JFXButton("擷取");
		btnSnapImg.getStyleClass().add("btn-raised-1");
		btnSnapImg.setGraphic(Misc.getIcon("camera.png"));
		btnSnapImg.setOnAction(event->{
			rndr.snap("img.png");
		});
		
		final JFXButton btnSetting = new JFXButton("設定");
		btnSetting.getStyleClass().add("btn-raised-1");
		btnSetting.setGraphic(Misc.getIcon("wrench.png"));
		btnSetting.setOnAction(event->rndr.getBundle(0).showPanel());
		
		final JFXButton btnClose = new JFXButton("離開");		
		btnClose.getStyleClass().add("btn-raised-1");
		btnClose.setGraphic(Misc.getIcon("close.png"));
		btnClose.setOnAction(event->PanRender.this.dismiss());
				
		final JFXButton chkPlaying = new JFXButton("暫停");
		chkPlaying.getStyleClass().add("btn-raised-1");
		chkPlaying.setGraphic(Misc.getIcon("pause.png"));
		chkPlaying.setOnAction(event->{
			if(rndr.isPlaying()==true){
				rndr.pause();
				chkPlaying.setText("播放");
				chkPlaying.setGraphic(Misc.getIcon("play.png"));
			}else{
				rndr.play();
				chkPlaying.setText("暫停");
				chkPlaying.setGraphic(Misc.getIcon("pause.png"));
			}
		});
		
		BorderPane root = new BorderPane();		
		panControl = PanBase.fillVBox(
			chkPlaying, 
			btnSnapImg, 
			btnSetting,
			btnClose
		);		
		root.setCenter(lay0);
		root.setRight(panControl);
		return root;
	}
}
