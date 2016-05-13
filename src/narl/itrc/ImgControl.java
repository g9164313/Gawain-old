package narl.itrc;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.VBox;

public class ImgControl extends VBox {

	private VBox lay0 = new VBox();
	private VBox lay1 = new VBox();
	
	public JFXComboBox<String> lstType = new JFXComboBox<String>();
	public JFXComboBox<String> lstIndx = new JFXComboBox<String>();
	public BtnToggle btnEnable = new BtnToggle(
		"開啟裝置","camera.png",
		"關閉裝置","camera-off.png"
	){
		@Override
		protected void eventInit(boolean state){
			lay1.setDisable(true);
		}
		@Override
		protected void eventSelect(){
			lstType.setDisable(true);
			lstIndx.setDisable(true);
			lay1.setDisable(false);
			if(scrn!=null){
				openCamera();				
			}
		}
		@Override
		protected void eventDeselect(){
			lstType.setDisable(false);
			lstIndx.setDisable(false);
			lay1.setDisable(true);
			if(scrn!=null){
				closeCamera();
			}			
		}
	};
	
	public PanSettingCam btnConfig = new PanSettingCam();
	public BtnToggle btnPlayer = new BtnToggle(
		"播放影像","ic_play_arrow_black_24dp_1x.png",
		"暫停播放","ic_pause_black_24dp_1x.png"
	);

	public ImgControl(){
		getStyleClass().add("hbox-small");
		
		lay0.getStyleClass().add("hbox-small");

		lstType.getItems().addAll("Files","Vidcap","Pylon","Ebus","Muticam");
		lstType.getSelectionModel().select(Integer.valueOf(Gawain.prop.getProperty("imgType","0")));
		lstType.setMaxWidth(Double.MAX_VALUE);
		
		lstIndx.getItems().addAll("自動編號","編號-1","編號-2","編號-3","編號-4","編號-5");
		lstIndx.getSelectionModel().select(Integer.valueOf(Gawain.prop.getProperty("imgIndx","0")));
		lstIndx.setMaxWidth(Double.MAX_VALUE);

		btnEnable.getStyleClass().add("btn-raised");
		btnEnable.setMaxWidth(Double.MAX_VALUE);
		
		btnConfig.getStyleClass().add("btn-raised");
		btnConfig.setMaxWidth(Double.MAX_VALUE);
			
		lay0.getChildren().addAll(lstType,lstIndx,btnEnable,btnConfig);
		//------------------------//
		lay1.getStyleClass().add("hbox-small");
		
		btnPlayer.getStyleClass().add("btn-raised");
		btnPlayer.setMaxWidth(Double.MAX_VALUE);
		
		lay1.getChildren().addAll(btnPlayer);
		//------------------------//
		getChildren().addAll(lay0,lay1);
	}
	//------------------------//

	public JFXButton addFilter(ImgRender.Filter fltr){
		final JFXButton btn = new JFXButton();
		btn.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(scrn==null){
					return;
				}
				scrn.render.hookFilter(fltr);				
			}
		});		
		btn.getStyleClass().add("btn-raised");
		btn.setMaxWidth(Double.MAX_VALUE);
		lay1.getChildren().add(btn);
		return btn;
	}
	
	private ImgPreview scrn = null;
	public void attachScreen(ImgPreview screen){
		if(scrn!=null){
			return;
		}
		scrn = screen;
		scrn.attachControl(this);
	}
	
	private void openCamera(){
		CamBundle cam = null;
		ImgRender.camIndx = lstIndx.getSelectionModel().getSelectedIndex() - 1;
		ImgRender.camConf = btnConfig.getCamText();
		int typ = lstType.getSelectionModel().getSelectedIndex();
		switch(typ){
		case 0: cam = new CamVFiles(); break;
		case 1: cam = new CamVidcap(); break;
		case 2:	cam = new CamPylon(); break;
		case 3: cam = new CamEBus(); break;
		default: return;// give notify ???
		}				
		scrn.bindCamera(cam);
		btnConfig.setBundle(cam);
		btnPlayer.setState(true);
	}
	
	private void closeCamera(){
		btnConfig.setBundle(null);//close setting-panel
		if(scrn.isRender()==true){
			scrn.unbindCamera();
		}	
	}
}
