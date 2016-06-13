package narl.itrc;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.VBox;

public class ImgControl extends VBox {

	private ImgRender render = new ImgRender(this);
	
	public static SimpleStringProperty txtConfig = new SimpleStringProperty("");
	
	public ImgControl(ImgPreview... preview){
		render.setPreview(preview);
		txtConfig.set(Gawain.prop.getProperty("imgConf",""));
		initPanel();
	}
	
	private VBox lay0 = new VBox();
	private VBox lay1 = new VBox();
	
	public JFXComboBox<String> lstType = new JFXComboBox<String>();
	
	public BtnToggle btnPlayer = new BtnToggle(
		"播放影像","play.png",
		"暫停播放","pause.png"
	);
	
	private void initPanel(){
		getStyleClass().add("hbox-small");
		lay0.getStyleClass().add("hbox-small");
		lay1.getStyleClass().add("hbox-small");
		
		lstType.getItems().addAll("Files","Vidcap","Pylon","Ebus","Muticam");
		lstType.disableProperty().bind(lay1.disableProperty().not());		
		lstType.getSelectionModel().select(Integer.valueOf(Gawain.prop.getProperty("imgType","0")));
		lstType.setMaxWidth(Double.MAX_VALUE);
		
		//JFXComboBox<String> lstIndx = new JFXComboBox<String>();
		//lstIndx.getItems().addAll("自動編號","編號-1","編號-2","編號-3","編號-4","編號-5");
		//lstIndx.getSelectionModel().select(Integer.valueOf(Gawain.prop.getProperty("imgIndx","0")));
		//lstIndx.setMaxWidth(Double.MAX_VALUE);

		BtnSettingCam btnConfig = new BtnSettingCam();
		btnConfig.getStyleClass().add("btn-raised");
		btnConfig.setMaxWidth(Double.MAX_VALUE);

		BtnToggle btnEnable = new BtnToggle(
			"開啟裝置","camera.png",
			"關閉裝置","camera-off.png"
		){
			@Override
			protected void eventInit(boolean state){
				lay1.setDisable(true);
			}
			@Override
			protected void eventSelect(){				
				lay1.setDisable(false);
				if(createBundle()==true){
					btnPlayer.setState(true);
					render.launch();
				}else{
					PanBase.msgBox.notifyInfo("Controller","無法連接相機");
					setState(false);
				}
			}
			@Override
			protected void eventDeselect(){
				lay1.setDisable(true);
				render.cancel();
			}
		};
		btnEnable.getStyleClass().add("btn-raised");
		btnEnable.setMaxWidth(Double.MAX_VALUE);
		lay0.getChildren().addAll(lstType,btnConfig,btnEnable);
		
		btnPlayer.getStyleClass().add("btn-raised");
		btnPlayer.setMaxWidth(Double.MAX_VALUE);
		lay1.getChildren().addAll(btnPlayer);
		getChildren().addAll(lay0,lay1);
	}
	//------------------------//
	
	private boolean createBundle(){
		ImgPreview[] prvw = render.getPreview();
		int typ = lstType.getSelectionModel().getSelectedIndex();
		String txt = txtConfig.get();
		switch(typ){
		case 0:
			for(int i=0; i<prvw.length; i++){
				prvw[i].bundle = new CamVFiles();
				prvw[i].bundle.optIndex = i;
				prvw[i].bundle.optConfig=txt;
			}
			break;
		case 1: 
			for(int i=0; i<prvw.length; i++){
				prvw[i].bundle = new CamVidcap();
				prvw[i].bundle.optIndex = i;
				prvw[i].bundle.optConfig=txt;
			}
			break;
		case 2:
			for(int i=0; i<prvw.length; i++){
				prvw[i].bundle = new CamPylon();
				prvw[i].bundle.optIndex = i;
				prvw[i].bundle.optConfig=txt;
			}
			break;
		case 3:
			for(int i=0; i<prvw.length; i++){
				prvw[i].bundle = new CamEBus();
				prvw[i].bundle.optIndex = i;
				prvw[i].bundle.optConfig=txt;
			}
			break;
		default: 
			return false;// give notify ???
		}
		return true;
	}

	public void release(){
		if(render.isAlive()==true){
			render.cancel();
		}
	}
	
	public JFXButton addFilter(ImgRender.Filter fltr){
		final JFXButton btn = new JFXButton();
		btn.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(render==null){
					return;
				}
				render.hookFilter(fltr);				
			}
		});		
		btn.getStyleClass().add("btn-raised");
		btn.setMaxWidth(Double.MAX_VALUE);
		lay1.getChildren().add(btn);
		return btn;
	}
}
