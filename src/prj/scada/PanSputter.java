package prj.scada;

import java.io.File;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;
import narl.itrc.BtnScript;
import narl.itrc.Misc;
import narl.itrc.PanBase;


public class PanSputter extends PanBase {

	public PanSputter(){
		//firstAction = FIRST_MAXIMIZED;
		customStyle = "-fx-background-color: white;";
	}
	
	private DevFatek devPLC = new DevFatek();
	private DevSQM160 devSQM = new DevSQM160();
	private DevSPIK2000 devSPIK = new DevSPIK2000();
		
	private WidMapPID mapper = new WidMapPID();
	
	private BtnScript btnExec = new BtnScript("執行",this);
	private String nameScript = Misc.pathSock+"test.js";
	
	@Override
	protected void eventShowing(WindowEvent e){		
		//hook each action of indicator, motor, valve or pump
		if(devPLC.connect()==true){
			Misc.logv("connect FATEK PLC...");
		}else{
			Misc.logv("fail to connect PLC(%s)",devPLC.getName());
		}
		if(devSQM.connect()==true){
			Misc.logv("connect SQM160...");
		}else{
			Misc.logv("fail to connect SQM160(%s)",devSQM.getName());
		}
		if(devSPIK.connect()==true){
			Misc.logv("connect SPIK2000A...");
		}else{
			Misc.logv("fail to connect SPIK2000A(%s)",devSPIK.getName());
		}
	}
	
	@Override
	protected void eventShown(WindowEvent e){
		devPLC.startMonitor(true,"R01000-40","X0000-24","Y0000-40","Y0120-8");
		//devPLC.startMonitor(false,"R01000-40","X0000-24","Y0000-40","Y0120-8");
		devSQM.startMonitor();
		mapper.hookPart(devPLC);
	}

	BorderPane root = new BorderPane();
	
	@Override
	public Node eventLayout(PanBase pan) {		
		root.setCenter(mapper);
		root.setRight(lay_action());
		//root.setBottom(lay_inform());
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

		final Button btnTest1 = PanBase.genButton2("test-1",null);
		btnTest1.setOnAction(event->{
			//tower lamp - red light
			/*if(devPLC.getMarker("Y0029").get()==0){
				devPLC.setNode(1, "M0029", 3);
				devPLC.setNode(1, "M0028", 3);
			}else{
				devPLC.setNode(1, "M0029", 4);
				devPLC.setNode(1, "M0028", 4);
			}*/
			String msg = devSQM.exec("@");
			System.out.println("==>"+msg);
		});
		
		final Button btnTest2 = PanBase.genButton2("test-2",null);
		btnTest2.setOnAction(event->{
			devSPIK.getVariable(-1,-1);
		});
		
		final Button btnTest3 = PanBase.genButton2("test-3",null);
		btnTest3.setOnAction(event->{
			devPLC.setNode(1, "M0128", 4);
		});
		
		final Button btnPLC = PanBase.genButton2("PLC setting",null);
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
	
	/*private Node lay_inform(){
		JFXTabPane lay = new JFXTabPane();
		lay.setPrefHeight(200.);
		Tab tab = new Tab();
		tab.setText("系統紀錄");
		tab.setContent(new BoxLogger());
		lay.getTabs().add(tab);
		return lay;
	}*/	
}
