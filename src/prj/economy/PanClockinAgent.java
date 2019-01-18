package prj.economy;

import java.io.File;

import com.jfoenix.controls.JFXDialog;
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
import javafx.scene.layout.StackPane;
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
		args[0] = frag1;
		
		final BorderPane lay0 = new BorderPane();
		
		final ButtonEx btnConn = new ButtonEx(
			"斷線","lan-disconnect.png",
			"連接","lan-connect.png"			
		).setOnToggle(event->{
			connect_database();
		}, null);
		if(DataProvider.isReady()==true){
			btnConn.setFace(1);
		}
		
		final ButtonEx btnEdit2 = new ButtonEx(
			"新增人員","account-plus.png"
		).setOnClick(event->{
			new PanEditHands().appear();
		});
		
		final ButtonEx btnDisp = new ButtonEx(
			"分派工作","directions-fork.png"
		).setOnClick(event->{
		});
		
		final ButtonEx btnBank = new ButtonEx(
			"查詢帳單","file-search-outline.png"
		).setOnClick(event->{
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
			_lay0.getStyleClass().add("vbox-small");
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

		final ToolBar bar = new ToolBar(
			btnConn,
			new Separator(Orientation.VERTICAL),
			btnEdit2,
			new Separator(Orientation.VERTICAL),
			btnDisp,
			btnBank,
			new Separator(Orientation.VERTICAL),
			btnSetIndx			
		);
		
		lay0.setTop(bar);
		lay0.setCenter(frag1);
		return lay0;
	}

	@Override
	public void eventShown(Object[] args) {
		((FragDispatch)args[0]).eventShow();
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
		spinner.kick(eventTask->{
			DataProvider.init(fs.getPath());
		},eventDone->{
			if(DataProvider.isReady()==false){
				notifyInfo("!!注意!!", "內部錯誤");
			}
		});
	}
}
