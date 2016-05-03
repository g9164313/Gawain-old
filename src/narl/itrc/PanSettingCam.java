package narl.itrc;

import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

public class PanSettingCam extends BtnPopping {

	private CamBundle cam;
	
	public PanSettingCam(){
		super(
			"相機設定",
			"相機設定",
			"ic_build_black_24dp_1x.png"
		);		
	}
	
	@Override
	protected void eventShown(WindowEvent event){
		//update the default information
		updateInfo();
	}
	
	private void updateInfo(){
		//don't bind this property, because we don't know whether bundle is ready
		boolean enable = cam.optEnbl.get();
		infoWidth.setDisable(enable);
		infoHeight.setDisable(enable);
		infoType.textProperty().set(CvType.typeToString(cam.getType()));
		infoWidth.textProperty().set(String.valueOf(cam.getWidth()));
		infoHeight.textProperty().set(String.valueOf(cam.getHeight()));
	}
	//--------------------------------------//
	
	private Label infoType = new Label();
	private JFXTextField infoWidth = new JFXTextField();
	private JFXTextField infoHeight = new JFXTextField();
	
	private Node layoutInfo(){
		GridPane pan = new GridPane();
		pan.getStyleClass().add("grid-small");
		pan.setAlignment(Pos.CENTER);
		
		pan.addRow(0,new Label("格式："),infoType);
		pan.addRow(1,new Label("寬："),infoWidth);
		pan.addRow(2,new Label("長："),infoHeight);
		return pan;
	}
	
	@Override
	Parent eventLayout() {
		JFXTabPane root = new JFXTabPane();
		Tab info = new Tab("資料");
		info.setContent(new ScrollPane(layoutInfo()));
		Tab parm = new Tab("參數");
		//parm.setContent(new ScrollPane(layoutParm()));
		parm.setContent(new Label("ggyy"));
		
		root.getTabs().addAll(info,parm);
		root.setPrefSize(300.,(300./1.618));
		
		/*VBox v0 = new VBox();
		v0.getStyleClass().add("vbox-small");
		v0.prefWidth(Double.MAX_VALUE);
		
		HBox h0 = new HBox();
		h0.getStyleClass().add("hbox-small");
		h0.getChildren().addAll(root,v0);*/		
		return root;
	}
}
