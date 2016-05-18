package prj.letterpress;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTabPane;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import narl.itrc.BtnToggle;
import narl.itrc.CamBundle;
import narl.itrc.CamVidcap;
import narl.itrc.ImgPreview;
import narl.itrc.ImgRender;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.PanJoystick;

public class Entry extends PanBase {

	public Entry(){
		//firstAction = FIRST_MAXIMIZED;
	}
	
	private CamBundle cam0 = new CamVidcap(0,"");
	private CamBundle cam1 = new CamVidcap(1,"");
	
	private ImgPreview prv0 = new ImgPreview(cam0);
	private ImgPreview prv1 = new ImgPreview(cam1);
	private ImgRender rndr = new ImgRender(prv0,prv1);
	
	private Node layAligment(){
		final int SIZE=200;
		
		HBox lay0 = PanBase.decorateHBox(
			"預覽1",prv0,
			"預覽2",prv1
		);

		JFXButton btnAction = new JFXButton("快速執行");
		btnAction.getStyleClass().add("btn-raised");
		btnAction.setPrefWidth(SIZE);
		btnAction.setGraphic(Misc.getIcon("run.png"));
		btnAction.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				
			}
		});
		
		JFXButton btnAligment = new JFXButton("標靶定位");
		btnAligment.getStyleClass().add("btn-raised");
		btnAligment.setPrefWidth(SIZE);
		btnAligment.setGraphic(Misc.getIcon("selection.png"));
		btnAligment.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				
			}
		});
		
		PanJoystick joyStick = new PanJoystick(SIZE);
		
		JFXButton btnClose = new JFXButton("關閉程式");
		btnClose.getStyleClass().add("btn-raised2");
		btnClose.setPrefWidth(SIZE);
		btnClose.setGraphic(Misc.getIcon("window-close.png"));
		btnClose.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				Entry.this.dismiss();
			}
		});
		
		VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		lay1.getChildren().addAll(
			btnAction,
			btnAligment,
			joyStick
		);
		
		AnchorPane lay2 = new AnchorPane();
		AnchorPane.setTopAnchor(lay1, 17.);
		AnchorPane.setBottomAnchor(btnClose, 17.);
		lay2.getChildren().addAll(lay1,btnClose);
		
		BorderPane root = new BorderPane();
		
		root.setCenter(lay0);
		root.setRight(lay2);
		return root;
	}
	
	private Node layScanning(){
		BorderPane root = new BorderPane();
		return root;
	}
	
	@Override
	public Parent layout() {
		JFXTabPane root = new JFXTabPane();
		root.setSide(Side.LEFT);
		root.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		Tab stp1 = new Tab("對位");
		stp1.setContent(layAligment());
		Tab stp2 = new Tab("曝光");
		stp2.setContent(layScanning());
		
		root.getTabs().addAll(stp1,stp2);
		return root;
	}
}
