package prj.letterpress;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTabPane;

import javafx.concurrent.Task;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import narl.itrc.CamBundle;
import narl.itrc.CamVidcap;
import narl.itrc.ImgPreview;
import narl.itrc.ImgRender;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.PanJoystick;
import narl.itrc.TskAction;
import narl.itrc.TskDialog;

public class Entry extends PanBase {

	public Entry(){
		//firstAction = FIRST_MAXIMIZED;
	}
	
	private PanMapWafer wmap = new PanMapWafer();
	
	private CamBundle cam0 = new CamVidcap(0,"");
	private CamBundle cam1 = new CamVidcap(1,"");
	
	private ImgPreview prv0 = new ImgPreview(cam0);
	private ImgPreview prv1 = new ImgPreview(cam1);
	private ImgRender rndr = new ImgRender(prv0,prv1);
	
	private DevB140M stg0 = new DevB140M();
	
	private TskAction tsk0 = new TskAligment(rndr,Entry.this);
	private TskAction tsk1 = new TskScanning(stg0,wmap,Entry.this);
	
	@Override
	protected void eventShown(WindowEvent e){
		//rndr.launch();
	}
	
	private Node layAligment(){
		final int SIZE=200;
		
		HBox lay0 = PanBase.decorateHBox(
			"預覽1",prv0,
			"預覽2",prv1
		);

		JFXButton btnAction = new JFXButton("快速執行");
		btnAction.getStyleClass().add("btn-raised");
		btnAction.setMaxWidth(Double.MAX_VALUE);
		btnAction.setGraphic(Misc.getIcon("run.png"));
		//btnAction.setOnAction();
		
		JFXButton btnAligment = new JFXButton("標靶定位");
		btnAligment.getStyleClass().add("btn-raised");
		btnAligment.setMaxWidth(Double.MAX_VALUE);
		btnAligment.setGraphic(Misc.getIcon("selection.png"));
		btnAligment.setOnAction(tsk0);
		
		PanJoystick joyStick = new PanJoystick(Orientation.VERTICAL,SIZE);

		JFXButton btnClose = new JFXButton("關閉程式");
		btnClose.getStyleClass().add("btn-raised2");
		btnClose.setMaxWidth(Double.MAX_VALUE);
		btnClose.setGraphic(Misc.getIcon("close.png"));
		btnClose.setOnAction(EVENT->Entry.this.dismiss());
		
		VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		lay1.getChildren().addAll(
			btnAction,
			btnAligment,
			joyStick,
			btnClose
		);

		/*AnchorPane lay2 = new AnchorPane();
		AnchorPane.setTopAnchor(lay1, 17.);
		AnchorPane.setBottomAnchor(btnClose, 17.);
		lay2.getChildren().addAll(lay1,btnClose);*/
		
		BorderPane root = new BorderPane();		
		root.setCenter(lay0);
		root.setRight(lay1);
		return root;
	}

	private Node layScanning(){

		JFXButton btnAction = new JFXButton("快速執行");
		btnAction.getStyleClass().add("btn-raised");
		btnAction.setGraphic(Misc.getIcon("run.png"));
		btnAction.setMaxWidth(Double.MAX_VALUE);
		btnAction.setOnAction(new TskDialog(Entry.this){
			@Override
			public int looper(Task<Integer> tsk) {
				logv("working...");
				Misc.delay(100);
				return 0;
			}
		});
		
		JFXButton btnScan = new JFXButton("掃描程序");
		btnScan.getStyleClass().add("btn-raised");
		btnScan.setGraphic(Misc.getIcon("play.png"));
		btnScan.setMaxWidth(Double.MAX_VALUE);
		btnScan.setOnAction(tsk1);
		
		JFXButton btnLight = new JFXButton("光源照射");
		btnLight.getStyleClass().add("btn-raised");
		btnLight.setGraphic(Misc.getIcon("blur.png"));
		btnLight.setMaxWidth(Double.MAX_VALUE);
		btnLight.setOnAction(new TskAction(Entry.this){
			@Override
			protected void DelayBegin(long tick){
			}
			@Override
			protected void DelayFinish(long tick){
			}			
			@Override
			public int looper(Task<Integer> task) {
				return DelayLooper(2000);
			}			
		});
		
		JFXButton btnClose = new JFXButton("關閉程式");
		btnClose.getStyleClass().add("btn-raised2");
		btnClose.setGraphic(Misc.getIcon("close.png"));
		btnClose.setMaxWidth(Double.MAX_VALUE);		
		btnClose.setOnAction(EVENT->Entry.this.dismiss());
		
		VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		lay1.getChildren().addAll(
			btnAction,
			btnScan,
			btnLight,
			wmap.getConsole(),
			btnClose
		);
		
		BorderPane root = new BorderPane();
		root.setCenter(wmap);
		root.setRight(lay1);
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
		
		Tab pg1 = new Tab("B140M");
		pg1.setContent(stg0.layoutConsole());
		
		root.getTabs().addAll(stp1,stp2,pg1);
		//root.getSelectionModel().select(0);
		root.getSelectionModel().select(1);
		//root.getSelectionModel().select(2);
		return root;
	}
}
