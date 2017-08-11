package prj.scada;

import java.io.File;

import com.jfoenix.controls.JFXTabPane;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import narl.itrc.BoxLogger;
import narl.itrc.BtnScript;
import narl.itrc.Misc;
import narl.itrc.PanBase;


public class PanSputter extends PanBase {

	public PanSputter(){
		//firstAction = FIRST_MAXIMIZED;
		customStyle = "-fx-background-color: white;";
	}
	
	//private DevSQM160 devSQM160 = new DevSQM160();
	
	//private DevSPIK2000 devSPIK2K = new DevSPIK2000();
	
	private DevFatek devPLC = new DevFatek();

	private WidMapPumper mapper = new WidMapPumper();
	
	private String nameScript = Misc.pathSock+"test.js";

	private BtnScript btnExec = new BtnScript("執行",this);

	@Override
	protected void eventShowing(WindowEvent e){		
		//hook each action of indicator, motor, valve or pump
		if(devPLC.connect()==false){
			Misc.logv("fail to connect PLC(%s)\n",devPLC.getName());
		}else{
			Misc.logv("connect FATEK PLC device...");
		}
	}
	
	@Override
	protected void eventShown(WindowEvent e){
		//devPLC.startMonitor(true,"R01000-40", "X0000-24", "Y0000-40");
		devPLC.startMonitor(false,"R01000-40", "X0000-24", "Y0000-40");
		mapper.hookPart(devPLC);
	}

	BorderPane root = new BorderPane();
	
	@Override
	public Node eventLayout(PanBase pan) {
		
		Button btn = new Button("test");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(event->{
			//spinning(true);
			Misc.logv("---check---");
		});		
		root.setCenter(mapper);
		root.setRight(lay_action());
		root.setBottom(lay_inform());
		return root;
	}
	
	private Node lay_action(){
		
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium-vertical");
		
		final Label txt[] = {
			new Label("Recipe："), new Label(Misc.trimPathAppx(nameScript)),
		};
		for(int i=0; i<txt.length; i++){
			txt[i].getStyleClass().add("txt-medium");
		}
		
		txt[1].setMinWidth(110.);
		txt[1].setOnMouseClicked(event->{
			FileChooser dia = new FileChooser();
			dia.setTitle("讀取...");
			File fs = dia.showOpenDialog(getScene().getWindow());
			if(fs==null){
				return;
			}
			nameScript = fs.getAbsolutePath();
			txt[1].setText(Misc.trimPathAppx(nameScript));
		});

		btnExec.setOnAction(beg_event->{
			if(nameScript.length()==0){
				final Alert dia = new Alert(AlertType.INFORMATION);
				dia.setHeaderText("沒有指定腳本！！");
				dia.showAndWait();
				return;
			}
			btnExec.eval(nameScript);//When fail, what should we do?
		},end_event->{
		});

		final Button btnEdit = PanBase.genButton2("編輯",null);
		btnEdit.setOnAction(event->{
		});

		final Button btnTest1 = PanBase.genButton2("測試-1",null);
		btnTest1.setOnAction(event->{
			//tower lamp - red light
			if(devPLC.getMarker("Y0029").get()==0){
				devPLC.setNode(1, "M0029", 3);
				devPLC.setNode(1, "M0028", 3);
			}else{
				devPLC.setNode(1, "M0029", 4);
				devPLC.setNode(1, "M0028", 4);
			}	
		});
		
		final Button btnTest2 = PanBase.genButton2("測試-2",null);
		btnTest2.setOnAction(event->{
			//tower lamp - yellow light
			if(devPLC.getMarker("Y0030").get()==0){
				devPLC.setNode(1, "M0030", 3);
			}else{
				devPLC.setNode(1, "M0030", 4);
			}
		});
		
		final Button btnTest3 = PanBase.genButton2("測試-3",null);
		btnTest3.setOnAction(event->{
			if(devPLC.getMarker("Y0022").get()==0){
				devPLC.setNode(1, "M0022", 3);
			}else{
				devPLC.setNode(1, "M0022", 4);
			}
		});
		
		final Button btnPLC = PanBase.genButton2("PLC 設定",null);
		btnPLC.setOnAction(event->{
			devPLC.showConsole("Fatek PLC");
		});
		
		lay.addRow(0, txt[0], txt[1]);
		lay.add(btnExec , 0, 1, 3, 1);
		lay.add(btnEdit , 0, 2, 3, 1);
		lay.add(btnPLC  , 0, 3, 3, 1);
		lay.add(btnTest1, 0, 4, 3, 1);
		lay.add(btnTest2, 0, 5, 3, 1);
		lay.add(btnTest3, 0, 6, 3, 1);
		return lay;
	}
	
	private Node lay_inform(){
		JFXTabPane lay = new JFXTabPane();
		lay.setPrefHeight(200.);
		Tab tab = new Tab();
		tab.setText("系統紀錄");
		tab.setContent(new BoxLogger());
		lay.getTabs().add(tab);
		return lay;
	}	
}
