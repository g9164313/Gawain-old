package narl.itrc;

import java.io.File;
import java.io.FilenameFilter;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/**
 * A wrap panel for TTY device.
 * @author qq
 *
 */
public class PanTTY extends PanBase {

	private DevTTY dev;
	
	public PanTTY(){
		dev = new DevTTY();
	}

	public PanTTY(DevTTY device){
		dev = device;
	}
	
	private Button btnInfomat = new Button("????");
	private TextArea  boxScreen = new TextArea();
	private TextField boxInput  = new TextField();
	private Timeline  clkSinker = new Timeline(new KeyFrame(
		Duration.millis(5),
		ae->{
			if(dev.isLive()==false){
				return;
			}
			String txt = dev.readTxt();
			if(txt==null){
				return;
			}
			boxScreen.appendText(txt);
			boxScreen.setScrollTop(Double.MAX_VALUE);
		}
	));
	
	@Override
	protected void eventShown(WindowEvent e){
		btnInfomat.textProperty().bind(dev.ctrlName);
		boxInput.requestFocus();
		dev.setSync(false);
		clkSinker.setCycleCount(Timeline.INDEFINITE);
		clkSinker.play();
	}
	
	@Override
	protected void eventClose(WindowEvent e){
		clkSinker.stop();
		dev.setSync(true);//this is default~~~
	}
	
	private void listDevice(ComboBox<String> box){
		ObservableList<String> lst = box.getItems();
		if(Misc.isPOSIX()==true){
			//how to list device file??
			File fs = new File("/dev");
			String[] names = fs.list(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					return 
						(name.matches("ttyUSB\\p{Digit}")) ||
						(name.matches("ttyACM\\p{Digit}")) ||
						(name.matches("rfcomm\\p{Digit}")) ||
						(name.matches("cuau\\p{Digit}"));
				}
			});
			for(int i=0; i<names.length; i++){
				names[i] = "/dev/"+names[i];
			}
			lst.addAll(names);
			lst.addAll(
				"/dev/ttyS0","/dev/ttyS1",
				"/dev/ttyS2","/dev/ttyS3",
				"/dev/ttyS4","/dev/ttyS6"
			);//this is default
		}else{
			lst.addAll(
				"\\\\.\\COM1",  "\\\\.\\COM2",  "\\\\.\\COM3",  "\\\\.\\COM4",
				"\\\\.\\COM5",  "\\\\.\\COM6",  "\\\\.\\COM7",  "\\\\.\\COM8",
				"\\\\.\\COM9",  "\\\\.\\COM10", "\\\\.\\COM11", "\\\\.\\COM12",
				"\\\\.\\COM13", "\\\\.\\COM14", "\\\\.\\COM15", "\\\\.\\COM16"
			);
		}	
	}
	
	@Override
	public Parent layout() {
		
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.setDisable(false);
		VBox.setVgrow(boxScreen,Priority.ALWAYS);
		//-------------------------------//
		
		GridPane lay1 = new GridPane(); 
		lay1.getStyleClass().add("grid-small");
		lay1.setStyle("-fx-background-color: aliceblue");
		lay1.setVisible(false);

		final ComboBox<String> cmbName = new ComboBox<String>();
		final ComboBox<String> cmbBaud = new ComboBox<String>();
		final ComboBox<String> cmbData = new ComboBox<String>();
		final ComboBox<String> cmbPart = new ComboBox<String>();
		final ComboBox<String> cmbStop = new ComboBox<String>();
		
		listDevice(cmbName);
		cmbBaud.getItems().addAll(
			  "300",  "600", "1200",  "1800",  "2400",  "4800",  "9600",
			"19200","38400","57600","115200","230400","460800","500000"
		);
		cmbData.getItems().addAll("5","6","7","8");
		cmbPart.getItems().addAll("none","odd","event","mark","space");
		cmbStop.getItems().addAll("1","2");

		final int CMB_SIZE=137;
		cmbName.setPrefWidth(CMB_SIZE);
		cmbBaud.setPrefWidth(CMB_SIZE);
		cmbData.setPrefWidth(CMB_SIZE);
		cmbPart.setPrefWidth(CMB_SIZE);
		cmbStop.setPrefWidth(CMB_SIZE);
		
		final Button btnCancel = new Button("取消");
		btnCancel.setMaxWidth(Double.MAX_VALUE);
		btnCancel.setOnAction(event->{
			lay0.setDisable(false);
			lay1.setVisible(false);
		});
		final Button btnConfirm= new Button("確認");
		btnConfirm.setMaxWidth(Double.MAX_VALUE);
		btnConfirm.setOnAction(event->{
			lay0.setDisable(false);
			lay1.setVisible(false);
		});
		
		HBox lay1_row1 = new HBox();
		lay1_row1.setAlignment(Pos.CENTER);
		HBox.setHgrow(btnCancel, Priority.ALWAYS);
		HBox.setHgrow(btnConfirm, Priority.ALWAYS);
		lay1_row1.getChildren().addAll(btnCancel,btnConfirm);
		
		lay1.addRow(0,new Label("通訊埠"),new Label("："),cmbName);
		lay1.addRow(1,new Label("鮑率")  ,new Label("："),cmbBaud);
		lay1.addRow(2,new Label("位元")  ,new Label("："),cmbData);
		lay1.addRow(3,new Label("檢查碼"),new Label("："),cmbPart);
		lay1.addRow(4,new Label("停止碼"),new Label("："),cmbStop);
		lay1.add(lay1_row1, 0, 5, 3, 1);
		//-------------------------------//
		
		boxScreen.setFocusTraversable(false);
		boxScreen.setPrefSize(350, 137);
		
		final ComboBox<String> cmbFeed = new ComboBox<String>();
		cmbFeed.getItems().addAll("LF","CR","CR,LF","<HEX>");
		cmbFeed.getSelectionModel().select(0);
		
		boxInput.setOnAction(EVENT->{
			String txt = boxInput.getText();
			if(dev.isLive()==true){
				int typ = cmbFeed.getSelectionModel().getSelectedIndex();//hard code!!!
				switch(typ){
				case 0://feed - LF
					txt = txt + "\n";
					break;
				case 1://feed - CR
					txt = txt + "\r";
					break;
				case 2://feed - CR,LF
					txt = txt + "\r\n";
					break;
				}
				dev.writeTxt(txt);
			}			
			boxInput.setText("");//clear!!!
			boxInput.requestFocus();
		});
		HBox.setHgrow(boxInput,Priority.ALWAYS);
		
		btnInfomat.setFocusTraversable(false);
		btnInfomat.setMaxWidth(Double.MAX_VALUE);
		btnInfomat.setAlignment(Pos.CENTER_LEFT);
		btnInfomat.setOnMouseClicked(EVENT->{			
			if(dev.isLive()==true){
				//select combo data!!!
				Misc.selectTxt(cmbName, dev.getName());
				Misc.selectTxt(cmbBaud, dev.getBaud());
				Misc.selectTxt(cmbData, dev.getDataBit());
				Misc.selectTxt(cmbPart, dev.getParity());
				Misc.selectTxt(cmbStop, dev.getStopBit());
			}
			lay0.setDisable(true);
			lay1.setVisible(true);			
		});
		HBox.setHgrow(btnInfomat, Priority.ALWAYS);
		
		HBox lay0_row1 = new HBox();
		lay0_row1.getStyleClass().add("hbox-small");
		lay0_row1.setAlignment(Pos.CENTER_LEFT);		
		lay0_row1.getChildren().addAll(
			new Label("通訊埠："),
			btnInfomat
		);

		HBox lay0_row3 = new HBox();
		lay0_row3.getStyleClass().add("hbox-small");
		lay0_row3.setAlignment(Pos.CENTER_LEFT);
		lay0_row3.getChildren().addAll(
			new Label("輸入："),
			boxInput,
			cmbFeed
		);
		
		lay0.getChildren().addAll(
			lay0_row1,
			boxScreen,
			lay0_row3
		);
				
		StackPane root = new StackPane();
		root.getChildren().addAll(lay0,lay1);
		return root;
	}
}
