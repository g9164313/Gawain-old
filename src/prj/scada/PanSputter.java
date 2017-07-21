package prj.scada;

import java.io.File;
import java.util.Arrays;

import com.jfoenix.controls.JFXTabPane;

import eu.hansolo.medusa.Gauge;
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
		//load the default mapping....
		//mapper.loadCell(Misc.pathSock+"PID.xml");
	}
	
	private DevSQM160 devSQM160 = new DevSQM160();
	
	private DevSPIK2000 devSPIK2K = new DevSPIK2000();
	
	private DevFatek devPLC = new DevFatek();

	private WidMapPumper mapper = new WidMapPumper(devPLC);
	
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
		//devPLC.get(1, "R00001", "Y0009", "DWM0000");
		//devSQM160.open("/dev/ttyS0,19200,8n1");
		//devSQM160.exec("@");
		devPLC.startMonitor("R01000-40");
		root.setLeft(lay_gauge());
		//watcher.setCycleCount(Timeline.INDEFINITE);
		//watcher.play();
	}
	
	private Timeline watcher = new Timeline(new KeyFrame(
		Duration.millis(1000), event->{
			//System.out.println("period check~~~");
			//mapper.refresh();
			//System.out.println("測試!!");
		}
	));
	
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
			watcher.pause();
			btnExec.eval(nameScript);//When fail, what should we do?
		},end_event->{
			watcher.play();
		});

		final Button btnEdit = PanBase.genButton2("編輯",null);
		btnEdit.setOnAction(event->{
		});

		final Button btnPLC = PanBase.genButton2("PLC 設定",null);
		btnPLC.setOnAction(event->{
			devPLC.showConsole("Fatek PLC");
		});
		
		lay.addRow(0, txt[0], txt[1]);
		lay.add(btnExec, 0, 1, 3, 1);
		lay.add(btnEdit, 0, 2, 3, 1);
		lay.add(btnPLC , 0, 3, 3, 1);
		return lay;
	}
	
	private Node lay_gauge(){
		
		mapper.addGauge("Ar 流量", "sccm", 0., 10000., devPLC.getMarker("R1000"));
		mapper.addGauge("O2 流量", "sccm", 0., 10000., devPLC.getMarker("R1001"));
		mapper.addGauge("N2 流量", "sccm", 0., 10000., devPLC.getMarker("R1002"));
			
		mapper.addGauge("絕對氣壓", "mTorr", 0., 10000., devPLC.getMarker("R1004"));
		mapper.addGauge("腔體氣壓", "Torr", 0., 10000., devPLC.getMarker("R1024"));				
		mapper.addGauge("腔體溫度", "°C", 0., 100., devPLC.getMarker("R1032").divide(10.f));
		mapper.addGauge("基板溫度", "°C", 0., 100., devPLC.getMarker("R1007").divide(10.f));
		
		mapper.addGauge("CP1 氣壓", "Torr", 0., 10000., devPLC.getMarker("R1025"));
		mapper.addGauge("CP1 溫度", "K", 0., 30., devPLC.getMarker("R1028"));
		mapper.addGauge("CP2 氣壓", "Torr", 0., 10000., devPLC.getMarker("R1035"));		
		mapper.addGauge("CP2 溫度", "K", 0., 30., devPLC.getMarker("R1034"));
		
		mapper.addGauge("加熱器電流", "A", 0., 1., devPLC.getMarker("R1026"));	
		mapper.addGauge("MP 電流", "A", 0., 1., devPLC.getMarker("R1027"));	
		
		Gauge[] gag = mapper.listGauge();
		
		GridPane lay0 = new GridPane();
		lay0.addRow(0, gag[ 0], gag[11]);
		lay0.addRow(1, gag[ 1]);
		lay0.addRow(2, gag[ 2]);

		GridPane lay1 = new GridPane();
		lay1.addRow(0, gag[ 3], gag[ 4]);
		lay1.addRow(1, gag[ 5], gag[ 6]);
		
		GridPane lay2 = new GridPane();
		lay2.addRow(0, gag[ 7], gag[ 9]);
		lay2.addRow(1, gag[ 8], gag[10]);
		lay2.addRow(2, gag[12]);
		
		JFXTabPane lay = new JFXTabPane();
		Tab[] tab = {
			new Tab("進氣/其他"),
			new Tab("腔體"),
			new Tab("壓縮機")
		};
		tab[0].setContent(lay0);
		tab[1].setContent(lay1);
		tab[2].setContent(lay2);
		lay.getTabs().addAll(tab);
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
