package prj.scada;

import java.io.File;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import narl.itrc.BoxLogger;
import narl.itrc.ButtonScript;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;


public class PanSputter extends PanBase {

	public PanSputter(){
		//firstAction = FIRST_MAXIMIZED;
		customStyle = "-fx-background-color: white;";
	}
	
	private DevFatek devPLC = new DevFatek();
	private DevSQM160 devSQM = new DevSQM160();
	private DevSPIK2K devSPIK = new DevSPIK2K();
	private ApiMapPID mapper = new ApiMapPID();

	public static boolean DBG = false;
	
	@Override
	public Node eventLayout(PanBase pan) {
		final BorderPane root = new BorderPane();	
		final Node console = lay_dashboard();
		//final Node setting = lay_setting();
		
		final Node action = lay_action(root,
			"原理圖", mapper,
			"歷程紀錄", console,
			"薄膜量測", devSQM.eventLayout(pan)
		);			
		root.setCenter(mapper);
		root.setRight(action);
		return root;
	}

	//private Node lay_setting(){
	//	final VBox lay1 = new VBox();
	//	return lay1;
	//}
	
	private Node lay_dashboard(){
				
		final Node[] lst = {
			new JFXCheckBox("Ch.1 壓力"), mapper.presChamber1.genPanel(),
			new JFXCheckBox("Ch.2 壓力"), mapper.presChamber2.genPanel(),
			new JFXCheckBox("Ar 壓力"), mapper.presAr.genPanel(),
			new JFXCheckBox("O2 壓力"), mapper.presO2.genPanel(),
			new JFXCheckBox("N2 壓力"), mapper.presN2.genPanel(),
			new JFXCheckBox("CP1 溫度"),mapper.tempCP1.genPanel(),
			new JFXCheckBox("CP2 溫度"),mapper.tempCP2.genPanel(),
			new JFXCheckBox("CP1 壓力"),mapper.presCP1.genPanel(),
			new JFXCheckBox("CP2 壓力"),mapper.presCP2.genPanel(),
			new JFXCheckBox("MP1 電流"),mapper.ampMP1.genPanel(),
			new JFXCheckBox("加熱器"), mapper.ampHeater.genPanel(),
			new JFXCheckBox("Ch.2 溫度"), mapper.tempChamber2.genPanel(),
			new JFXCheckBox("冰水機"), mapper.tempChiller.genPanel(),
			new JFXCheckBox("基板溫度"), mapper.chuck.temp.genPanel(),
			new JFXCheckBox("DC1 功率"), mapper.burner_dc1.attr[0].genPanel(),
			new JFXCheckBox("DC1 電壓"), mapper.burner_dc1.attr[1].genPanel(),
			new JFXCheckBox("DC1 電流"), mapper.burner_dc1.attr[2].genPanel(),
		};
		final Node[] lstCtrl = new Node[lst.length/2];
		final Node[] lstInfo = new Node[lst.length/2];
		for(int i=0; i<lst.length; i+=2){
			lstCtrl[i/2] = lst[i+0];
			lstInfo[i/2] = lst[i+1];
			((JFXCheckBox)lst[i+0]).selectedProperty().bindBidirectional(lst[i+1].visibleProperty());
		}
		
		final Button btnUncheck = new Button("取消全部");
		btnUncheck.setPrefWidth(90);
		btnUncheck.setOnAction(e->{
			for(int i=0; i<lst.length; i+=2){
				((JFXCheckBox)lst[i+0]).setSelected(false);
			}
		});
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-one-direction");
		lay1.getChildren().add(btnUncheck);
		lay1.getChildren().addAll(lstCtrl);
		final ScrollPane lay1_1 = new ScrollPane();
		lay1_1.setFitToWidth(true);
		lay1_1.setContent(lay1);
		lay1_1.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		
		final VBox lay2 = new VBox();
		//lay2.getStyleClass().add("vbox-one-direction");		
		lay2.getChildren().addAll(lstInfo);
		final ScrollPane lay2_1 = new ScrollPane();
		lay2_1.setFitToWidth(true);
		lay2_1.setContent(lay2);
		lay2_1.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
		lay2_1.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		
		final BoxLogger box = new BoxLogger();
		box.setPrefHeight(120);
		
		final BorderPane lay0 = new BorderPane();
		lay0.setLeft(lay1_1);
		lay0.setCenter(lay2_1);
		lay0.setBottom(box);		
		return lay0;
	}
	
	private String pathScript = Gawain.pathSock+"test.js";

	private Node lay_action(
		final BorderPane root,
		Object... args
	){
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium-vertical");
		
		final ToggleGroup group = new ToggleGroup();		
		JFXRadioButton[] page = new JFXRadioButton[args.length/2];
		for(int i=0; i<args.length; i+=2){
			String name = (String)args[i+0];
			JFXRadioButton rad = new JFXRadioButton(name);
			rad.setToggleGroup(group);
			if(args[i+1] instanceof Pane){
				Pane pane = (Pane)args[i+1];
				rad.setOnAction(e->root.setCenter(pane));
			}else if(args[i+1] instanceof Control){
				Control ctrl = (Control)args[i+1];
				rad.setOnAction(e->root.setCenter(ctrl));
			}
			page[i/2] = rad;
		}
		group.selectToggle(page[0]);
		
		final Label txtScript[] = {
			new Label("執行程序："), 
			new Label(""),
		};
		txtScript[1].getStyleClass().add("txt-medium");
		final EventHandler<MouseEvent> eventScriptPath = new EventHandler<MouseEvent>(){
			@Override
			public void handle(MouseEvent event) {
				FileChooser dia = new FileChooser();
				dia.setTitle("讀取...");
				File fs = dia.showOpenDialog(getScene().getWindow());
				if(fs==null){
					return;
				}
				pathScript = fs.getAbsolutePath();
				txtScript[1].setText(Misc.trimPathAppx(pathScript));
			}
		};
		txtScript[0].setOnMouseClicked(eventScriptPath);
		txtScript[1].setOnMouseClicked(eventScriptPath);
		if(new File(pathScript).exists()==true){
			txtScript[1].setText(Misc.trimPathAppx(pathScript));
		}
		
		final ButtonScript btnExec = new ButtonScript("執行",mapper);
		btnExec.setOnAction(beg_event->{
			if(btnExec.eval(pathScript)==false){
				final Alert dia = new Alert(AlertType.INFORMATION);
				dia.setHeaderText("內部錯誤！！");
				dia.showAndWait();
				return;
			}
		},end_event->{
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
		});
		
		final Button btnTest2 = PanBase.genButton2("test-2",null);
		btnTest2.setOnAction(event->{			
			//IntegerProperty prop = devPLC.getMarker("R1004");
			//int off = (int)(Math.random()*1000);
			//int cur = prop.get();
			//prop.set(3000+off);
			//devSPIK.getVariable(-1,-1);
		});
		
		final Button btnTest3 = PanBase.genButton2("test-3",null);
		btnTest3.setOnAction(event->{
			//devPLC.setNode(1, "M0128", 4);
		});

		for(int i=0; i<page.length; i++){
			lay.add(page[i], 0, i, 4, 1);
		}
		
		lay.add(new Separator(), 0, 0+page.length, 4, 1);
		lay.add(txtScript[0],    0, 1+page.length, 4, 1);
		lay.add(txtScript[1],    0, 2+page.length, 4, 1);
		lay.add(btnExec,         0, 3+page.length, 4, 1);
		
		lay.add(new Separator(), 0, 4+page.length, 4, 1);
		lay.add(btnTest1, 0, 5+page.length, 4, 1);
		lay.add(btnTest2, 0, 6+page.length, 4, 1);
		lay.add(btnTest3, 0, 7+page.length, 4, 1);
		return lay;
	}
	
	@Override
	protected void eventShowing(PanBase self){
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
	public void eventShown(PanBase self) {
		devPLC.startMonitor("R01000-40","X0000-24","Y0000-40","Y0120-8");
		devSQM.startMonitor();
		mapper.hookPart(devPLC);
	}
}
