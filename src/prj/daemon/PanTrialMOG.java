package prj.daemon;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.WindowEvent;

import narl.itrc.ChkCellFactory;
import narl.itrc.DevGrabber;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PanTrialMOG extends PanBase {

	public PanTrialMOG(){
		title = "TrialMOG";
	}

	private final String STR_REC_START="錄影";
	private final String STR_REC_STOP="停止";

	private final String STR_BACK="背景";//true
	private final String STR_FORE="前景";//false
	private JFXComboBox<String> chkType;
	
	private ToolBar actBar = null;
	
	private final EventHandler<ActionEvent> eventClear = 
		new EventHandler<ActionEvent>()
	{
		@Override
		public void handle(ActionEvent event) {
			boolean type = (chkType.getSelectionModel().getSelectedIndex()==0)?(true):(false);
			String name = (type)?(STR_BACK):(STR_FORE);
			Optional<ButtonType> result;
			TrialMOG obj = table.getSelectionModel().getSelectedItem();			
			if(obj==null){
				//new Alert(AlertType.INFORMATION,"請選相機來源").showAndWait();
				result = new Alert(AlertType.CONFIRMATION,"清除所有相機資料？").showAndWait();
				if(result.get()==ButtonType.OK){
					for(TrialMOG cc:cam){
						cc.clearDir(type);
					}
				}
				return;
			}
			result = new Alert(AlertType.CONFIRMATION,"清除"+name+"資料？").showAndWait();
			if(result.get()==ButtonType.OK){
				obj.clearDir(type);
			}			
		}
	};

	private final EventHandler<ActionEvent> eventRecord = 
		new EventHandler<ActionEvent>()
	{
		@Override
		public void handle(ActionEvent event) {
			TrialMOG cam = table.getSelectionModel().getSelectedItem();
			if(cam==null){
				new Alert(AlertType.INFORMATION,"請選相機來源").showAndWait();
				return;
			}
			Button btn = (Button)event.getSource();
			String txt = btn.getText();
			int tkn = btn.hashCode();
			if(txt.equals(STR_REC_START)==true){
				//start to record~~~~
				boolean type = (chkType.getSelectionModel().getSelectedIndex()==0)?(true):(false);
				cam.recordStart(type);
				btn.setText(STR_REC_STOP);
				disableAction(true,tkn);
			}else if(txt.equals(STR_REC_STOP)==true){
				//stop record~~~
				cam.recordStop();
				btn.setText(STR_REC_START);
				disableAction(false,tkn);
			}
		}
	};
	
	private final EventHandler<ActionEvent> eventTrain = 
		new EventHandler<ActionEvent>()
	{
		@Override
		public void handle(ActionEvent event) {
			TrialMOG cam = table.getSelectionModel().getSelectedItem();
			if(cam==null){
				new Alert(AlertType.INFORMATION,"請選相機來源").showAndWait();
				return;
			}
			cam.cmdTrain();
		}
	};
		
	private final EventHandler<ActionEvent> eventMeas1 = 
		new EventHandler<ActionEvent>()
	{
		@Override
		public void handle(ActionEvent event) {
			TrialMOG cam = table.getSelectionModel().getSelectedItem();
			if(cam==null){
				new Alert(AlertType.INFORMATION,"請選相機來源").showAndWait();
				return;
			}
			cam.cmdMeasure();
		}
	};
		
	private void disableAction(boolean flag,int except){
		for(Node n:actBar.getItems()){
			if(n.hashCode()==except){
				continue;
			}
			n.setDisable(flag);
		}
	}
	
	private TableView<TrialMOG> table = new TableView<TrialMOG>();
	
	private final ObservableList<TrialMOG> cam = FXCollections.observableArrayList(
		new TrialMOG("cam1",DevGrabber.SRC_NUL_MEM),
		new TrialMOG("cam2",DevGrabber.SRC_NUL_MEM),
		new TrialMOG("cam3",DevGrabber.SRC_NUL_MEM),
		new TrialMOG("cam4",DevGrabber.SRC_NUL_MEM),
		new TrialMOG("cam5",DevGrabber.SRC_NUL_MEM),
		new TrialMOG("cam6",DevGrabber.SRC_NUL_MEM)
	);

	@Override
	protected void eventShown(WindowEvent event){
		//user can define some properties before the stage is shown		
		for(int i=0; i<cam.size(); i++){
			TrialMOG obj = cam.get(i);
			String[] attr = Gawain.prop.getProperty(obj.wndName,"").split(";");
			if(attr.length==0){
				continue;
			}
			boolean flagEN = false;
			for(String val:attr){
				if(val.startsWith("enable")==true){
					flagEN = true;
				}else if(val.startsWith("disable")==true){
					flagEN = false;
				}else if(val.startsWith("silence")==true){
					obj.optPreview.set(false);
				}else if(val.startsWith("preview")==true){
					obj.optPreview.set(true);
				}
			}
			obj.optEnable.set(flagEN);//this will launch device, so put it finally~~~
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Parent layout() {
		BorderPane root = new BorderPane();
		
		table.setItems(cam);
		
		TableColumn<TrialMOG,String> colSrcName = new TableColumn<>("來源");
		colSrcName.setSortable(false);
		colSrcName.setCellValueFactory(new PropertyValueFactory<>("srcName"));
		
		TableColumn<TrialMOG,String> colInfo1 = new TableColumn<>("B/F");
		colInfo1.setSortable(false);
		colInfo1.setCellValueFactory(new PropertyValueFactory<>("info1"));
				
		TableColumn<TrialMOG,Boolean> colSrcEnable = new TableColumn<>("啟動");
		colSrcEnable.setSortable(false);
		colSrcEnable.setCellFactory(new ChkCellFactory<TrialMOG>("optEnable:DevGrabber"));
		
		TableColumn<TrialMOG,Boolean> colSrcPreview = new TableColumn<>("預覽");
		colSrcPreview.setSortable(false);
		colSrcPreview.setCellFactory(new ChkCellFactory<TrialMOG>("optPreview:DevGrabber"));
		
		TableColumn<TrialMOG,String> colInfo2 = new TableColumn<>("狀態");
		colInfo2.setSortable(false);
		colInfo2.setCellValueFactory(new PropertyValueFactory<>("info2"));
		colInfo2.prefWidthProperty().bind(table.widthProperty()
			.subtract(colSrcName.widthProperty())
			.subtract(colInfo1.widthProperty())
			.subtract(colSrcEnable.widthProperty())
			.subtract(colSrcPreview.widthProperty())
			.subtract(16)
		);
		
		table.getColumns().addAll(
			colSrcName,
			colInfo1,
			colSrcEnable,
			colSrcPreview,
			colInfo2
		);
		
		chkType = new JFXComboBox<String>();
		chkType.getItems().add(STR_BACK);
		chkType.getItems().add(STR_FORE);
		chkType.setEditable(false);
		chkType.getSelectionModel().select(0);
		
		final JFXButton btnClear = new JFXButton("清除");
		btnClear.setOnAction(eventClear);
		
		final JFXButton btnRecord = new JFXButton(STR_REC_START);
		btnRecord.setOnAction(eventRecord);
		
		final JFXButton btnTrain = new JFXButton("訓練模型");
		btnTrain.setOnAction(eventTrain);
		
		final JFXButton btnMeas1 = new JFXButton("單次測量");
		btnMeas1.setOnAction(eventMeas1);
		
		actBar = new ToolBar(
			chkType,			
			btnRecord,
			btnClear,
			new Separator(),
			btnTrain,
			new Separator(),
			btnMeas1
		);
		
		root.setTop(actBar);
		root.setCenter(table);
		return root;
	}
}
