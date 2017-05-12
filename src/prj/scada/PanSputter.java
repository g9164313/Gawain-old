package prj.scada;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import com.sun.glass.ui.Application;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import narl.itrc.BtnScript;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.WidDiagram;

public class PanSputter extends PanBase {

	public PanSputter(){
		customStyle = "-fx-background-color: white;";
		//load the default mapping....
		//mapper.loadCell(Misc.pathSock+"PID.xml");
	}
	
	private DevSQM160 devSQM160 = new DevSQM160();
	
	private DevSPIK2000 devSPIK2K = new DevSPIK2000();
	
	private DevFatek devPLC = new DevFatek("/dev/tty11");
	
	//private WidMapPumper mapper = new WidMapPumper("sample-pid");
	private WidDiagram mapper = new WidDiagram();
	
	private String nameScript = Misc.pathSock+"test.js";

	private BtnScript btnExec = new BtnScript("執行",this);
	
	private Timeline watcher = new Timeline(new KeyFrame(
		Duration.millis(1000), event->{
			//System.out.println("period check~~~");
			//mapper.refresh();
		}
	));
	
	@Override
	protected void eventShowing(WindowEvent e){		
		//hook each action of indicator, motor, valve or pump
		/*mapper.hookWith("valve_1", itm->{
		});
		mapper.hookWith("valve_2", itm->{
		});
		mapper.hookWith("valve_3", itm->{
		});
		mapper.hookWith("valve_4", itm->{
		});
		mapper.hookWith("valve_5", itm->{
		});
		mapper.hookWith("valve_6", itm->{
		});
		
		mapper.hookWith("motor_1", itm->{
		});
		mapper.hookWith("motor_2", itm->{
		});
		mapper.hookWith("motor_3", itm->{
		});
		
		mapper.hookWith("gauge_1", itm->{
			itm.value = 100*Math.random();//simulation~~~~
		});
		mapper.hookWith("gauge_2", itm->{
			itm.value = 100*Math.random();//simulation~~~~
		});
		mapper.hookWith("gauge_3", itm->{
			itm.value = 100*Math.random();//simulation~~~~
		});
		mapper.hookWith("gauge_4", itm->{
			itm.value = 100*Math.random();//simulation~~~~
		});
		mapper.hookWith("gauge_5", itm->{
			itm.value = 100*Math.random();//simulation~~~~
		});
		mapper.hookWith("gauge_6", itm->{
			itm.value = 100*Math.random();//simulation~~~~
		});*/
				
		watcher.setCycleCount(Timeline.INDEFINITE);
		watcher.play();
	}
	
	//---- below lines are entry points for script parse ----//
	/*public void valve_1(boolean flag){ mapper.doTask("valve_1"); }
	public void valve_2(boolean flag){ mapper.doTask("valve_2"); }
	public void valve_3(boolean flag){ mapper.doTask("valve_3"); }
	public void valve_4(boolean flag){ mapper.doTask("valve_4"); }
	public void valve_5(boolean flag){ mapper.doTask("valve_5"); }
	public void valve_6(boolean flag){ mapper.doTask("valve_6"); }
	
	public void refresh(){ mapper.refresh(); }*/
	
	@Override
	protected void eventShown(WindowEvent e){
		//devSQM160.open("/dev/ttyS0,19200,8n1");
		//devSQM160.exec("@");
	}

	@Override
	public Node eventLayout(PanBase pan) {
		
		Button btn = new Button("test");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(event->{
			//spinning(true);
			Misc.logv("---check---");
		});
		
		BorderPane root = new BorderPane();
		root.setRight(lay_action());
		root.setCenter(mapper);
		root.setLeft(lay_gauge());
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

		lay.addRow(0, txt[0], txt[1]);
		lay.add(btnExec, 0, 1, 3, 1);
		lay.add(btnEdit, 0, 2, 3, 1);
		return lay;
	}
	
	private Node lay_gauge(){
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium");
		/*Gauge[] lst = mapper.createGauge();
		final int MAX_ROW = 4;
		for(int i=0; i<lst.length; i++){
			lay.add(lst[i], i/MAX_ROW, i%MAX_ROW);
		}*/
		return lay;
	}
}
