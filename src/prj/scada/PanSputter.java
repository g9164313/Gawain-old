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
import narl.itrc.BtnScript;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanSputter extends PanBase {

	public PanSputter(){
		customStyle = "-fx-background-color: white;";
		
		//load the default mapping....
		//mapper.loadCell(Misc.pathSock+"PID.xml");
	}
	
	private DevSQM160 devSQM160 = new DevSQM160();
	
	private DevSPIK2000 devSPIK2K = new DevSPIK2000();
	
	private DevFatek devPLC = new DevFatek("/dev/tty11");
	
	private WidMapPumper mapper = new WidMapPumper("sample-pid");
	
	private String nameScript = Misc.pathSock+"test.js";

	private BtnScript btnExec = new BtnScript("執行",this);
	
	@Override
	protected void eventShown(WindowEvent e){
		//devSQM160.open("/dev/ttyS0,19200,8n1");
		//devSQM160.exec("@");
				
		//hook each action of indicator, motor, valve or pump
	}
	
	public void valve_1(boolean flag){
		System.out.println("flag="+flag);
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
		
		GridPane lay = new GridPane();//show all sensor
		lay.getStyleClass().add("grid-medium-vertical");
		
		final Label txt[] = {
			new Label("Recipe："), new Label(Misc.trim_path_appx(nameScript)),
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
			txt[1].setText(Misc.trim_path_appx(nameScript));
		});

		btnExec.setOnAction(event->{
			if(nameScript.length()==0){
				final Alert dia = new Alert(AlertType.INFORMATION);
				dia.setHeaderText("沒有指定腳本！！");
				dia.showAndWait();
				return;
			}
			btnExec.eval(nameScript);//When fail, what should we do?
		});

		final Button btnEdit = PanBase.genButton2("編輯",null);
		btnEdit.setOnAction(event->{
			
		});

		lay.addRow(0, txt[0], txt[1]);
		lay.add(btnExec, 0, 1, 3, 1);
		lay.add(btnEdit, 0, 2, 3, 1);
		return lay;
	}
	
	private Gauge[] gauge = { 
		null, null, null, null, 
		null, null, null, null,
	};
	
	private Node lay_gauge(){
		
		GridPane lay = new GridPane();//show all sensor
		lay.getStyleClass().add("grid-medium-vertical");
		
		gauge[0] = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.animated(true)
			.title("溫度-1")
			.unit("Å/s")
			.minValue(10)
			.maxValue(250)
			.build();

		gauge[1] = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.animated(true)
			.title("溫度-2")
			.unit("AAA")
			.minValue(10)
			.maxValue(40)
			.build();
		
		gauge[2] = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.animated(true)
			.title("溫度-3")
			.unit("AAA")
			.minValue(10)
			.maxValue(40)
			.build();

		gauge[3] = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.animated(true)
			.title("溫度-4")
			.unit("AAA")
			.minValue(10)
			.maxValue(40)
			.build();
		
		gauge[4] = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.animated(true)
			.title("溫度-4")
			.unit("AAA")
			.minValue(10)
			.maxValue(40)
			.build();
		
		lay.add(gauge[0], 0, 0);
		lay.add(gauge[1], 0, 1);
		lay.add(gauge[2], 0, 2);
		lay.add(gauge[3], 0, 3);
		lay.add(gauge[4], 0, 4);
		return lay;
	}
}
