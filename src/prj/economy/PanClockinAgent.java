package prj.economy;

import java.io.File;

import com.jfoenix.controls.JFXTextField;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import narl.itrc.ButtonEx;
import narl.itrc.PanBase;
import narl.itrc.nat.Loader;

public class PanClockinAgent extends PanBase {

	public PanClockinAgent(){
		Loader.hooker = new Runnable(){
			@Override
			public void run() {
				DataProvider.init(null);
			}
		};
	}
		
	@Override
	public Node eventLayout(PanBase self) {
		
		final FragDispatch frag1 = new FragDispatch(); 
		final FragSummary frag2 = new FragSummary();

		final BorderPane lay0 = new BorderPane();
		
		final ButtonEx btnConn = new ButtonEx(
			"斷線","lan-disconnect.png",
			"連接","lan-connect.png"			
		).setOnToggle(event->{
			connect_database();
		}, null);

		final ButtonEx btnEdit2 = new ButtonEx(
			"新增人員","account-plus.png"
		).setOnClick(e->frag1.dialog_hand(null));
		
		final ButtonEx btnDisp1 = new ButtonEx(
			"工作排程","event.png"
		).setOnClick(event->{
			frag1.modeCalendar();
			lay0.setCenter(frag1);			
		});
		final ButtonEx btnDisp2 = new ButtonEx(
			"工作清單","subject.png"
		).setOnClick(event->{
			frag1.modeWaitingList();
			lay0.setCenter(frag1);
		});
		final ButtonEx btnBank = new ButtonEx(
			"查詢帳單","file-search-outline.png"
		).setOnClick(event->{
			lay0.setCenter(frag2);
		});
		
		final ButtonEx btnSetIndx = new ButtonEx(
			"計數器","file-search-outline.png"
		);
		btnSetIndx.textProperty().bind(DataProvider.propIndex.asString("計數(%04d)"));
		btnSetIndx.setOnAction(event->{
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle(null);
			dia.setContentText(null);
			final VBox _lay0 = new VBox(
				new Label("計數"),
				new JFXTextField()
			);
			_lay0.getStyleClass().add("layout-small");
			dia.getDialogPane().setContent(_lay0);
			if(dia.showAndWait().get()==ButtonType.OK){
				JFXTextField box = (JFXTextField)lay0.getChildren().get(1);
				try{
					int val = Integer.valueOf(box.getText());
					DataProvider.propIndex.set(val);
				}catch(NumberFormatException e){					
				}
			}
		});
		
		if(DataProvider.isReady()==true){
			btnConn.setFace(1);
		}
		
		final ToolBar bar = new ToolBar(
			btnConn,
			new Separator(Orientation.VERTICAL),
			btnEdit2,
			new Separator(Orientation.VERTICAL),
			btnDisp1,
			btnDisp2,
			btnBank,
			new Separator(Orientation.VERTICAL),
			btnSetIndx			
		);
		
		lay0.setTop(bar);
		lay0.setCenter(frag1);
		
		stage().setOnShown(e->{
			frag1.eventShown();
			frag2.eventShown();
		});		
		return lay0;
	}

	private void connect_database(){
		if(DataProvider.isReady()==true){
			return;
		}
		final File fs = chooseFile("選擇鑰匙");
		if(fs==null){
			//notifyInfo("!!注意!!", "無鑰匙");
			return;
		}
		/*spin.kick(eventTask->{
			DataProvider.init(fs.getPath());
		},eventDone->{
			if(DataProvider.isReady()==false){
				notifyInfo("!!注意!!", "內部錯誤");
			}
		});*/
	}
}
