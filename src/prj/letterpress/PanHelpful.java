package prj.letterpress;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import narl.itrc.DevMotion;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;
import narl.itrc.TskAction;

public class PanHelpful extends PanDecorate {

	public PanHelpful(){
		super("快速操作");		
	}
	
	private Node layoutOption1(){
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-small");
		
		/*Lcd[] lcd ={null,null,null};
		for(int i=0; i<lcd.length; i++){
			lcd[i] = LcdBuilder.create()
				.prefWidth(100)
				.prefHeight(32)
				.keepAspect(true)
				.lcdDesign(LcdDesign.STANDARD)
				.foregroundShadowVisible(true)
				.crystalOverlayVisible(false)
				.valueFont(Lcd.LcdFont.ELEKTRA)
				.animated(true)
				.build();
			lcd[i].valueProperty().bind(Entry.stg0.pulse[i].divide(50));
		}*/
		
		Label[] lcd = new Label[3];
		for(int i=0; i<lcd.length; i++){
			lcd[i] = new Label();
			lcd[i].textProperty().bind(Entry.stg0.pulse[i].divide(50).asString("%8d"));
			lcd[i].setPrefWidth(80);
			lcd[i].setAlignment(Pos.BASELINE_RIGHT);
		}//the difference between encoder and motor is 50!!!  

		final TextField[] box = {
			new TextField("0"),
			new TextField("0"),
			new TextField("0")
		};
		final Button[] btn = {
			new Button(DIR_NEG1),new Button(DIR_POS1),
			new Button(DIR_NEG1),new Button(DIR_POS1),
			new Button(DIR_NEG1),new Button(DIR_POS1),
			new Button(DIR_ZERO),
			new Button(DIR_ZERO),
			new Button(DIR_ZERO)
		};
		
		for(int i=0; i<6; i++){
			int id = i/2;
			char tkn = '?';
			switch(id){
			case 0: tkn = 'x'; break;
			case 1: tkn = 'y'; break;
			case 2: tkn = '@'; break;
			}
			char dir = (i%2==0)?('-'):('+');
			EventKick event = new EventKick(tkn,dir,box[id],btn[i]);
			btn[i].addEventFilter(MouseEvent.MOUSE_PRESSED,event);
			btn[i].addEventFilter(MouseEvent.MOUSE_RELEASED,event);
			box[id].setPrefWidth(80);
		}
		
		btn[6].setOnAction(event->{
			Entry.stg0.exec("DE ,0;DP ,0;\r\n"); 
			Entry.stg0.exec_TP();
		});
		btn[7].setOnAction(event->{
			Entry.stg0.exec("DE 0;DP 0;\r\n"); 
			Entry.stg0.exec_TP();
		});
		btn[8].setOnAction(event->{
			Entry.stg0.exec("DE ,,0;DP ,,0;\r\n"); 
			Entry.stg0.exec_TP();
		});
				
		lay.addRow(0, new Label("X軸"),lcd[0],btn[0],box[0],btn[1],btn[6]);
		lay.addRow(1, new Label("Y軸"),lcd[1],btn[2],box[1],btn[3],btn[7]);
		lay.addRow(2, new Label("θ軸"),lcd[2],btn[4],box[2],btn[5],btn[8]);
		return lay;
	}

	private final String TXT_START = "UV照射";
	private final String TXT_STOP  = "停止UV";
	private Timeline actExpose;
	private TextField boxExpose;
	private Button btnExpose;
		
	private void begExpose(){
		Entry.stg0.exec("OB 2,1\r\n");
		btnExpose.setText(TXT_STOP);
		btnExpose.setUserData(true);
		boxExpose.setDisable(true);		
	}
	
	private void endExpose(){
		Entry.stg0.exec("OB 2,0\r\n");
		btnExpose.setText(TXT_START);
		btnExpose.setUserData(false);
		boxExpose.setDisable(false);		
	}
	
	private int valLight = 45;
	
	private Node layoutOption3(){
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-small");
		
		final int BOX_SIZE = 90;
		
		boxExpose = new TextField("1sec");
		boxExpose.setPrefWidth(BOX_SIZE);
		
		btnExpose = new Button(TXT_START);		
		btnExpose.setUserData(false);
		btnExpose.setOnAction(event->{
			double val = 0.;
			try{
				val = Misc.phyConvert(boxExpose.getText().trim(),"sec");
				val = val*1000.;
			}catch(NumberFormatException e){
				boxExpose.setText("1sec");
				return;
			}
			boolean flag = (boolean)(btnExpose.getUserData());
			if(flag==false){
				begExpose();
				actExpose = new Timeline(new KeyFrame(
					Duration.millis(val),
					event1->endExpose()
				));
				actExpose.play();
			}else{
				actExpose.stop();
				endExpose();
			}
		});
		btnExpose.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(btnExpose, Priority.ALWAYS);

		TextField boxLight = new TextField(""+valLight);
		boxLight.setPrefWidth(BOX_SIZE);
		Button btnLight = new Button("光源強度");		
		btnLight.setOnAction(event->{
			String txt = boxLight.getText();			
			try{
				valLight = Integer.valueOf(txt);
				Entry.stg1.writeTxt("1,"+valLight+"\r\n",20);
				Entry.stg1.writeTxt("2,"+valLight+"\r\n",20);
			}catch(NumberFormatException e){
				boxLight.setText(""+valLight);//We fail, just reset value~~~
			}
		});
		btnLight.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(btnLight, Priority.ALWAYS);
		
		lay.addRow(0,boxExpose,btnExpose);
		lay.addRow(1,boxLight,btnLight);
		return lay;
	}

	private Node layoutOption4(){
		HBox lay = new HBox();
		lay.getStyleClass().add("hbox-one-line");
		
		final CheckBox chkPump = new CheckBox("幫浦開關");
		chkPump.setIndeterminate(false);
		chkPump.setOnAction(event->{
			//get state from device
			if(chkPump.isSelected()){
				Entry.stg0.exec("OB 1,1\r\n");
			}else{
				Entry.stg0.exec("OB 1,0\r\n");
			}
		});
		
		final CheckBox chkMirror = new CheckBox("反射鏡開關");
		chkMirror.setIndeterminate(false);
		chkMirror.setOnAction(event->{
			//get state from device
			if(chkMirror.isSelected()){
				Entry.stg2.writeTxt('C');
			}else{
				Entry.stg2.writeTxt('K');
			}
		});
		
		lay.getChildren().addAll(chkPump,chkMirror);
		return lay;
	}

	@SuppressWarnings("unchecked")
	private Node layoutOption6(){
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium");
		
		final TskAction tsk_gohome = new TskGoHome(null);
		final TskAction tsk_holder = new TskHolder(null);
		final TskAction tsk_scanning = new TskScanning(Entry.inst.wmap,null);
		
		Button btnHome = PanBase.genButton1("原點校正","arrow-compress-all.png");
		btnHome.setOnAction(tsk_gohome);

		Button btnAlign = PanBase.genButton1("標靶對位","selection.png");
		btnAlign.setOnAction(Entry.inst.prvw.filterAlign);
		
		Button btnScan = PanBase.genButton1("晶圓曝光","blur.png");
		btnScan.setOnAction(tsk_scanning);

		Button btnGoing = PanBase.genButton2("快速執行","run.png");
		btnGoing.setOnAction(TskAction.createKeyframe(
			Entry.inst.prvw.filterAlign,
			event->{
				//switch mirror
				//close upper-LED
				Entry.stg1.writeTxt("1,0\r\n",20);
				Entry.stg1.writeTxt("2,0\r\n",20);
				//close bottom-LED
				//switch tab-pager
				Entry.pager.getSelectionModel().select(1);
			},
			tsk_scanning,
			event->{
				//switch mirror
				//open upper-LED
				Entry.stg1.writeTxt("1,"+valLight+"\r\n",20);
				Entry.stg1.writeTxt("2,"+valLight+"\r\n",20);
				//open bottom-LED
				//switch tab-pager
				Entry.pager.getSelectionModel().select(0);
			}
		));
		
		Button btnHold = PanBase.genButton2("進/退片","coffee-to-go.png");
		btnHold.setOnAction(tsk_holder);
		
		Button btnClose = PanBase.genButton3("關閉程式","close.png");	
		btnClose.setOnAction(event->Entry.inst.dismiss());
		
		lay.addRow(0, btnHome, btnAlign, btnScan);
		lay.addRow(1, btnHold, btnGoing, btnClose);
		return lay;
	}
	
	@Override
	public Node layoutBody() {
		
		VBox lay2 = new VBox();
		lay2.getStyleClass().add("vbox-small");
		lay2.getChildren().addAll(
			layoutOption3(),
			layoutOption4()
		);

		HBox lay1 = new HBox();
		lay1.getStyleClass().add("hbox-small");
		lay1.getChildren().addAll(
			layoutOption1(),
			new Separator(Orientation.VERTICAL),
			lay2,
			new Separator(Orientation.VERTICAL),
			layoutOption6()
		);
		return lay1;
	}
	
	private static final String DIR_POS1="  >";
	private static final String DIR_POS2=">>";
	
	private static final String DIR_NEG1="<  ";
	private static final String DIR_NEG2="<<";
	
	private static final String DIR_ZERO="RST";
	
	class EventKick implements EventHandler<MouseEvent>{
		private char dir = '+';
		private char tkn = '?';		
		private TextField box;
		private Button btn;
		public EventKick(char tkn,char dir,TextField box,Button btn){
			this.tkn = tkn;
			this.dir = dir;
			this.box = box;
			this.btn = btn;
		}
		@Override
		public void handle(MouseEvent event) {
			int val = 0;
			String txt = box.getText().trim();
			try{
				val = Integer.valueOf(txt);
			}catch(NumberFormatException e){
				Misc.loge("必須是整數 --> "+txt);
				return;
			}
			if(dir=='-'){
				val = val * -1;
			}
			EventType<?> typ = event.getEventType();
			if(typ==MouseEvent.MOUSE_PRESSED){
				btn.setText((dir=='+')?(DIR_POS2):(DIR_NEG2));
				if(val==0){
					jogging(true,dir);
				}else{
					moving(val);
				}
			}else if(typ==MouseEvent.MOUSE_RELEASED){
				btn.setText((dir=='+')?(DIR_POS1):(DIR_NEG1));
				if(val==0){
					jogging(false,dir);
				}
			}
		}
		private void jogging(boolean go,char dir){
			double val = (dir=='+')?(2000):(-2000);
			switch(tkn){
			case 'x':
			case 'X':
				Entry.stg0.joggingTo(go,DevMotion.PULSE_UNIT, val);
				break;
			case 'y':
			case 'Y':
				Entry.stg0.joggingTo(go,DevMotion.PULSE_UNIT, null, val);
				break;
			case '@':
				val = val * 10.;//special~~~
				Entry.stg0.joggingTo(go,DevMotion.PULSE_UNIT, null, null, null, val);
				break;				
			}
		}
				
		private void moving(int val){
			Misc.logv("move %c%c (%d)",dir,tkn,val);
			switch(tkn){
			case 'x':
			case 'X':
				Entry.stg0.asyncMoveTo(DevMotion.PULSE_UNIT,(double)val);
				break;
			case 'y':
			case 'Y':
				Entry.stg0.asyncMoveTo(DevMotion.PULSE_UNIT,null,(double)val);
				break;
			case '@':
				Entry.stg0.asyncMoveTo(DevMotion.PULSE_UNIT,null,null,(double)val);
				break;				
			}
		}		
	};	
}
