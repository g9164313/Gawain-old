package prj.daemon;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXToggleButton;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.CamBundle;
import narl.itrc.CamEBus;
import narl.itrc.CamPylon;
import narl.itrc.CamVidcap;
import narl.itrc.ImgControl;
import narl.itrc.ImgPreview;
import narl.itrc.PanBase;

public class PanCamera extends PanBase {
	
	private CamBundle cam = null;
	
	public PanCamera(){
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
