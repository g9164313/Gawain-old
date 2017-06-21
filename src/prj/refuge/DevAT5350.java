package prj.refuge;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.PanBase;
import narl.itrc.Misc;

public class DevAT5350 extends DevTTY {

	public DevAT5350(){
	}
	
	public void connect(String attr){
		if(attr.length()==0){
			attr = Gawain.prop.getProperty("DevAT5350","/dev/ttyS0,9600,8n1");
		}
		open(attr);
	}
	
	public void disconnect(){
		close();
	}
	//--------------------------------//
	
	
	@Override
	protected Node eventLayout(PanBase pan) {
		
		final GridPane root = new GridPane();//show all sensor
		root.getStyleClass().add("grid-medium");
		
		root.add(new Label("取樣個數")       , 0, 0);
		root.add(new Label("取樣週期（sec）"), 0, 1);
		root.add(new Label("Filter（sec）")  , 0, 2);
		root.add(new Label("使用濾波器")     , 0, 3);
		root.add(new Label("測量形式")       , 0, 4);
		root.add(new Label("測量範圍")       , 0, 5);			
		root.add(new Label("Damper（0.01%）"), 0, 6);
		root.add(new Label("溫度因子（deg）"), 0, 7);
		root.add(new Label("壓力因子（kPa）"), 0, 8);
		root.add(new Label("高壓電（Volt）") , 0, 9);
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-one-dir");
		final Button btnTest = PanBase.genButton2("測試連線",null);
		btnTest.setOnAction(event->{
			String resp = fetch("*IDN?\n","\n");
			Misc.logv("got --> "+resp);
		});
		final Button btnLoad = PanBase.genButton2("讀取參數",null);
		btnLoad.setOnAction(event->{
		});
		final Button btnSave = PanBase.genButton2("儲存參數",null);
		btnSave.setOnAction(event->{
		});
		final Button btnComp = PanBase.genButton2("執行補償",null);
		btnComp.setOnAction(event->{
		});
		final Button btnVolt = PanBase.genButton2("高壓充電",null);
		btnVolt.setOnAction(event->{
		});
		final Button btnMeas = PanBase.genButton2("開始測量",null);
		btnMeas.setOnAction(event->{
		});
		lay1.getChildren().addAll(btnTest,btnLoad,btnSave,btnComp,btnVolt,btnMeas);
		
		root.add(new Separator(Orientation.VERTICAL), 2, 0, 1, 10);
		root.add(lay1, 3, 0, 4, 10);
		return root;
	}
}
