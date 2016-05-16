package prj.daemon;

import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.stage.WindowEvent;

import narl.itrc.ImgControl;
import narl.itrc.ImgPreview;
import narl.itrc.PanBase;

public class PanCamera extends PanBase {
	
	public PanCamera(){
	}
	
	@Override
	protected void eventClose(WindowEvent event){ 
		super.eventClose(event);
		imgCtrl.release();//it must be shutdown!!!
	}

	protected ImgPreview imgPrvw = new ImgPreview();
	protected ImgControl imgCtrl = new ImgControl(imgPrvw);
	
	@Override
	public Parent layout() {
		BorderPane root = new BorderPane();
		root.setLeft(PanBase.decorate("控制",imgCtrl));
		root.setCenter(PanBase.decorate("預覽",imgPrvw));
		imgCtrl.addFilter(new FltrSlangEdge()).setText("SFR分析");
		return root;
	}
}
