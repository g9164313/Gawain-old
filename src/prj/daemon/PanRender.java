package prj.daemon;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import narl.itrc.CamDummy;
import narl.itrc.CamVidcap;
import narl.itrc.ImgRender;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.vision.CamXIMEA;

/**
 * Test camera bundle and represent as template
 * @author qq
 *
 */
@SuppressWarnings("unused")
public class PanRender extends PanBase {

	public PanRender(){
		//final String testFile = ":/home/qq/labor/opencv-3.1/contrib/modules/text/samples/scenetext_segmented_word%02d.jpg";
		final String testFile = "/home/qq/labor/bang/edge.pgm";
		rndr = new ImgRender(new CamDummy(testFile));
		//rndr = new ImgRender(new CamVidcap("FILE:0:"+testFile));
		//rndr = new ImgRender(new CamVidcap("0"));
		//rndr = new ImgRender(new CamVidcap("VFW:0"));
		//rndr = new ImgRender(new CamMulticam("ral12288-FULL"));
		//rndr = new ImgRender(new CamFlyCapture());
		//rndr = new ImgRender(new CamXIMEA());
	}
	
	protected ImgRender rndr;//don't assign object. Let inheritance object create instance~~
	
	@Override
	protected void eventShown(WindowEvent e){
		rndr.play();
	}
	
	@Override
	protected void eventClose(WindowEvent e){
	}
	
	private VBox layoutCtrl(){
		
		final Button chkPlaying = PanBase.genButton1("暫停","pause.png");
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
		
		final Button btnSnapImg = PanBase.genButton1("擷取","camera.png");
		btnSnapImg.setOnAction(event->{
			rndr.snap("img.png");
		});
		
		final Button btnSetting = PanBase.genButton1("設定","wrench.png");
		btnSetting.setOnAction(event->{
			//TODO: how to show settting panel
		});
		
		final FltrSlangEdge fltr = new FltrSlangEdge(rndr);
		
		final Button btnProbe = PanBase.genButton2("測試","walk.png");
		btnProbe.setOnAction(event->{
			fltr.ctrl.appear();
		});
		
		final Button btnClose = PanBase.genButton1("離開","close.png");	
		btnClose.setOnAction(event->PanRender.this.dismiss());
				
		return PanBase.fillVBox(
			chkPlaying, 
			btnSnapImg, 
			btnSetting,
			btnProbe,
			btnClose
		);	
	}
	
	@Override
	public Parent layout() {
		HBox lay0 = new HBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.getChildren().addAll(
			rndr.getPreview(0)
		);
		BorderPane lay1 = new BorderPane();			
		lay1.setCenter(lay0);
		lay1.setRight(layoutCtrl());
		return lay1;
	}
}
