package narl.itrc;

import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;
import javafx.stage.WindowEvent;

public class PanSettingCam extends BtnPopping {

	public PanSettingCam(){
		super(
			"相機設定",
			"相機設定",
			"ic_build_black_24dp_1x.png"
		);
		confCamera.setText(Gawain.prop.getProperty("imgConf",""));
	}
	
	@Override
	protected void eventShowing(WindowEvent event){
		updateInfo();
		if(cam!=null){ 
			panParm.setContent(cam.getPanSetting());
		}
	}
	
	@Override
	protected void eventClose(WindowEvent event){
		panParm.setContent(null);
	}
	
	private void updateInfo(){
		if(cam==null){
			final String unknow = "???";
			infoType.setText(unknow);
			infoWidth.setText(unknow);
			infoHeight.setText(unknow);
			infoCntx.setText(unknow);
			infoMatx.setText(unknow);
			infoLast.setText(unknow);
			return;
		}
		infoType.setText(CvType.typeToString(cam.getType()));			
		infoWidth.textProperty().set(String.valueOf(cam.getWidth()));
		infoWidth.disableProperty().bind(cam.optEnbl);			
		infoHeight.textProperty().set(String.valueOf(cam.getHeight()));
		infoHeight.disableProperty().bind(cam.optEnbl);			
		infoCntx.setText(String.format(
			"0x%X",
			cam.getCntx()
		));		
		infoMatx.setText(String.format(
			"0x%X,0x%X",
			cam.getMatSrc(),cam.getMatOva()
		));
		infoLast.textProperty().bind(cam.msgLast);
	}
	
	private CamBundle cam = null;	
	public void setBundle(CamBundle bnd){
		if(bnd!=null){
			cam = bnd;
		}else{
			//drop bundle~~
			infoWidth.disableProperty().unbind();
			infoHeight.disableProperty().unbind();
			infoLast.textProperty().unbind();
			dismiss();
		}
	}
	
	public String getCamText(){
		return confCamera.getText();
	}
	//--------------------------------------//
	
	private JFXTextField confCamera = new JFXTextField();
	private Label infoType = new Label();
	private JFXTextField infoWidth = new JFXTextField();
	private JFXTextField infoHeight = new JFXTextField();
	private Label infoCntx = new Label();
	private Label infoMatx = new Label();
	private Label infoLast = new Label();
	
	private Node layoutInfo(){
		GridPane pan = new GridPane();
		pan.getStyleClass().add("grid-small");
		pan.setAlignment(Pos.BASELINE_LEFT);
		
		pan.addRow(0,new Label("設定："),confCamera);
		pan.addRow(1,new Label("格式："),infoType);
		pan.addRow(2,new Label("寬："),infoWidth);
		pan.addRow(3,new Label("高："),infoHeight);
		pan.addRow(4,new Label("內容："),infoCntx);
		pan.addRow(5,new Label("矩陣："),infoMatx);
		pan.addRow(6,new Label("訊息："),infoLast);
		return pan;
	}
	
	private ScrollPane panParm = new ScrollPane();
	@Override
	Parent eventLayout() {
		final double WIDTH=300.;
		final double HEIGHT=260.;
		
		JFXTabPane root = new JFXTabPane();
		Tab info = new Tab("資料");
		info.setContent(new ScrollPane(layoutInfo()));
		Tab parm = new Tab("參數");
		parm.setContent(panParm);

		root.getSelectionModel().selectedItemProperty().addListener(
			new ChangeListener<Tab>(){
			@Override
			public void changed(
				ObservableValue<? extends Tab> observable,
				Tab oldValue, Tab newValue
			) {
				if(newValue==info){
					updateInfo();
				}
			}
		});

		confCamera.setPrefWidth(210);
		//infoWidth.setPrefWidth(210);
		//infoHeight.setPrefWidth(210);
		
		root.getTabs().addAll(info,parm);
		root.setPrefSize(WIDTH,HEIGHT);
		
		/*VBox v0 = new VBox();
		v0.getStyleClass().add("vbox-small");
		v0.prefWidth(Double.MAX_VALUE);
		
		HBox h0 = new HBox();
		h0.getStyleClass().add("hbox-small");
		h0.getChildren().addAll(root,v0);*/		
		return root;
	}
}
