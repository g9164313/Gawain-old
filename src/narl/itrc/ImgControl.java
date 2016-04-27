package narl.itrc;

import java.util.concurrent.atomic.AtomicBoolean;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTabPane;

import eu.hansolo.enzo.onoffswitch.OnOffSwitch;
import eu.hansolo.enzo.onoffswitch.SelectionEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;

import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

public class ImgControl extends VBox {

	private final int DEF_TYPE = 0;
	private final int DEF_INDX = 0;
	
	private class BtnPanROI extends BtnPanel{
		public Label txtMsgPoint = new Label();		
		public BtnPanROI(){
			super("設定ROI");
		}
		private Control layoutRoi(){
			JFXTabPane pan = new JFXTabPane();
			for(int i=0; i<CamBundle.PR_SIZE; i++){
				Tab tab = new Tab("ROI"+i);
				tab.setContent(new Label("Content"));
				pan.getTabs().add(tab);
			}
			return pan;
		}		
		@Override
		Parent eventLayout() {
			BorderPane root = new BorderPane();
			root.setCenter(layoutRoi());
			root.setBottom(txtMsgPoint);
			return root;
		}
		@Override
		void eventShown(WindowEvent event) {
		}
		@Override
		void eventClose(WindowEvent event) {
		}
	};
	
	private ImgPreview scrn = null;

	public JFXComboBox<String> lstType = new JFXComboBox<String>();
	public JFXComboBox<String> lstIndx = new JFXComboBox<String>();
	public OnOffSwitch swtEnable = new OnOffSwitch();
	public JFXButton btnConfig = new JFXButton("設定相機");
	public JFXButton btnPlayer = new JFXButton();
	public BtnPanROI btnPanROI = new BtnPanROI();
	
	private VBox lay0 = new VBox();
	private VBox lay1 = new VBox();
	
	public ImgControl(){
		getStyleClass().add("hbox-small");
		
		lay0.getStyleClass().add("hbox-small");
		
		lstType.getItems().addAll("Vidcap","Pylon","Ebus","Muticam");
		lstType.getSelectionModel().select(DEF_TYPE);
		lstType.setMaxWidth(Double.MAX_VALUE);
		
		lstIndx.getItems().addAll("自動編號","編號-1","編號-2","編號-3","編號-4","編號-5");
		lstIndx.getSelectionModel().select(DEF_INDX);
		lstIndx.setMaxWidth(Double.MAX_VALUE);

		swtEnable.getStyleClass().add("swt-raise");
		swtEnable.setOnSelect(eventSwitch);
		swtEnable.setOnDeselect(eventSwitch);
		
		lay0.getChildren().addAll(lstType,lstIndx,swtEnable);
		//------------------------//
		lay1.getStyleClass().add("hbox-small");
		lay1.disableProperty().bind(swtEnable.selectedProperty().not());
		
		btnConfig.getStyleClass().add("btn-raised");
		btnConfig.setMaxWidth(Double.MAX_VALUE);
		
		btnPlayer.getStyleClass().add("btn-raised");
		btnPlayer.setOnAction(eventPlayer);
		btnPlayer.setMaxWidth(Double.MAX_VALUE);
		initSwtPlayer();
		
		lay1.getChildren().addAll(btnConfig,btnPlayer,btnPanROI);
		//------------------------//
		getChildren().addAll(lay0,lay1);
	}
	
	public void bindScreen(ImgPreview screen){
		if(scrn!=null){
			return;
		}
		scrn = screen;
		scrn.bindControl(this);
	}
	
	private EventHandler<SelectionEvent> eventSwitch = new EventHandler<SelectionEvent>(){
		@Override
		public void handle(SelectionEvent event) {
			if(scrn==null){
				return;
			}
			if(swtEnable.selectedProperty().get()==true){
				scrn.camIndx = lstIndx.getSelectionModel().getSelectedIndex() - 1;
				CamBundle cam = null;
				int typ = lstType.getSelectionModel().getSelectedIndex();
				switch(typ){
				case 0: cam=new CamVidcap(); break;
				case 1:	cam=new CamPylon(); break;
				case 2: cam=new CamEBus(); break;
				case 3: break;	
				}
				initSwtPlayer();				
				scrn.bindCamera(cam);
			}else{
				initSwtPlayer();
				scrn.unbind();
			}
		}
	};
	
	public AtomicBoolean swtPlayer = new AtomicBoolean(false);
	private void initSwtPlayer(){
		swtPlayer.set(false);
		btnPlayer.setText("播放");
		btnPlayer.setGraphic(Misc.getIcon("ic_play_arrow_black_24dp_1x.png"));		
	}
	private EventHandler<ActionEvent> eventPlayer = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			boolean flag = swtPlayer.get();
			if(flag==false){
				btnPlayer.setText("暫停");
				btnPlayer.setGraphic(Misc.getIcon("ic_pause_black_24dp_1x.png"));
			}else{
				btnPlayer.setText("播放");
				btnPlayer.setGraphic(Misc.getIcon("ic_play_arrow_black_24dp_1x.png"));
			}
			swtPlayer.set(!flag);
		}
	};
}
