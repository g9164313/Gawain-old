package prj.daemon;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.stage.WindowEvent;

import narl.itrc.CamBundle;
import narl.itrc.ImgControl;
import narl.itrc.ImgPreview;
import narl.itrc.PanBase;

public class PanCamera extends PanBase {
	
	public PanCamera(){
	}
	
	@Override
	protected void eventClose(WindowEvent event){ 
		imgScrn.release();//important!!!
	}
	
	private ImgPreview imgScrn = new ImgPreview();
	private ImgControl imgCtrl = new ImgControl();
	@Override
	public Parent layout() {
		BorderPane root = new BorderPane();
		root.setLeft(PanBase.decorate("控制",imgCtrl));
		root.setCenter(PanBase.decorate("預覽",imgScrn));
		imgScrn.bindControl(imgCtrl);
		return root;
	}
	//----------------------------//
}
