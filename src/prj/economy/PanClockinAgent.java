package prj.economy;

import java.io.File;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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
		
	private final FragDispatch fragDisp = new FragDispatch();
	
	@Override
	public Node eventLayout(PanBase self) {

		final ButtonEx btnConn = new ButtonEx(
			"斷線","lan-disconnect.png",
			"連接","lan-connect.png"			
		).setOnToggle(event0->{
			connect_database();
		}, null);
		if(DataProvider.isReady()==true){
			btnConn.setFace(1);
		}
		
		final ButtonEx btnEdit1 = new ButtonEx(
			"新增訂單","file-document-outline.png"
		).setOnClick(event->{
			new PanEditBills().appear();
		});
		
		final ButtonEx btnEdit2 = new ButtonEx(
			"新增人員","account-plus.png"
		).setOnClick(event->{
			new PanEditHands().appear();
		});
		
		final ButtonEx btnDisp = new ButtonEx(
			"分派工作","directions-fork.png"
		).setOnClick(e->{
			fragDisp.setVisible(true);
		});
		
		final ButtonEx btnBank = new ButtonEx(
			"查詢帳單","file-search-outline.png"
		);
		
		final ButtonEx btnSetIndx = new ButtonEx(
			"計數","file-search-outline.png"
		);
		btnSetIndx.textProperty().bind(DataProvider.propIndex.asString("計數(%04d)"));
		btnSetIndx.setOnAction(event->{
			
		});
		
		final StackPane lay1 = new StackPane(fragDisp);
		
		final ToolBar bar = new ToolBar(
			btnConn,
			new Separator(Orientation.VERTICAL),
			btnEdit1,
			btnEdit2,
			new Separator(Orientation.VERTICAL),
			btnDisp,
			btnBank,
			new Separator(Orientation.VERTICAL),
			btnSetIndx			
		);
		final BorderPane lay0 = new BorderPane();
		lay0.setTop(bar);
		lay0.setCenter(lay1);
		return lay0;
	}

	@Override
	public void eventShown(PanBase self) {
		fragDisp.init();
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
