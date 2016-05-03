package narl.itrc;

import com.jfoenix.controls.JFXComboBox;

import eu.hansolo.enzo.onoffswitch.OnOffSwitch;
import eu.hansolo.enzo.onoffswitch.SelectionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

public class ImgControl extends VBox {

	private final int DEFAULT_CAM_TYPE = 0;
	private final int DEFAULT_CAM_INDX = 0;

	public JFXComboBox<String> lstType = new JFXComboBox<String>();
	public JFXComboBox<String> lstIndx = new JFXComboBox<String>();
	public OnOffSwitch swtEnable = new OnOffSwitch();
	public BtnToggle btnConfig = new PanSettingCam();
	public BtnToggle btnPlayer = new BtnToggle(
		"播放影像","ic_play_arrow_black_24dp_1x.png",
		"暫停播放","ic_pause_black_24dp_1x.png"
	);

	private VBox lay0 = new VBox();
	private VBox lay1 = new VBox();
	
	public ImgControl(){
		getStyleClass().add("hbox-small");
		
		lay0.getStyleClass().add("hbox-small");
		
		lstType.getItems().addAll("Files","Vidcap","Pylon","Ebus","Muticam");
		lstType.getSelectionModel().select(DEFAULT_CAM_TYPE);
		lstType.setMaxWidth(Double.MAX_VALUE);
		
		lstIndx.getItems().addAll("自動編號","編號-1","編號-2","編號-3","編號-4","編號-5");
		lstIndx.getSelectionModel().select(DEFAULT_CAM_INDX);
		lstIndx.setMaxWidth(Double.MAX_VALUE);

		swtEnable.getStyleClass().add("swt-raise");
		swtEnable.setOnSelect(eventSwitch);
		swtEnable.setOnDeselect(eventSwitch);

		btnConfig.getStyleClass().add("btn-raised");
		btnConfig.setMaxWidth(Double.MAX_VALUE);
		
		lay0.getChildren().addAll(lstType,lstIndx,swtEnable,btnConfig);
		//------------------------//
		lay1.getStyleClass().add("hbox-small");
		lay1.disableProperty().bind(swtEnable.selectedProperty().not());
				
		btnPlayer.getStyleClass().add("btn-raised");
		btnPlayer.setMaxWidth(Double.MAX_VALUE);

		lay1.getChildren().addAll(btnPlayer);
		//------------------------//
		getChildren().addAll(lay0,lay1);
	}
	//------------------------//

	private ImgPreview scrn = null;
	
	public void attachScreen(ImgPreview screen){
		if(scrn!=null){
			return;
		}
		scrn = screen;
		scrn.attachControl(this);
	}
	
	private EventHandler<SelectionEvent> eventSwitch = new EventHandler<SelectionEvent>(){
		@Override
		public void handle(SelectionEvent event) {
			if(scrn==null){
				return;
			}
			if(scrn.isRender()==false){
				CamBundle cam = null;
				scrn.camIndx = lstIndx.getSelectionModel().getSelectedIndex() - 1;				
				int typ = lstType.getSelectionModel().getSelectedIndex();
				switch(typ){
				case 0: cam = new CamVFiles(); break;
				case 1: cam = new CamVidcap(); break;
				case 2:	cam = new CamPylon(); break;
				case 3: cam = new CamEBus(); break;
				default: return;// give notify ???
				}				
				scrn.bindCamera(cam);
			}else{
				scrn.unbindCamera();
			}
		}
	};
}
