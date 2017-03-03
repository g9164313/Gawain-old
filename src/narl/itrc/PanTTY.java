package narl.itrc;

import java.io.File;
import java.io.FilenameFilter;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * A wrap panel for TTY device.
 * @author qq
 *
 */
public class PanTTY extends Pane {

	private DevTTY dev;
	
	public PanTTY(DevTTY device){
		dev = device;
		init_layout();
	}
	
	private void init_layout(){
				
		final JFXComboBox<String> cmbName = new JFXComboBox<String>();		
		getSupportList(cmbName).setMaxWidth(Double.MAX_VALUE);
		
		final JFXComboBox<String> cmbBaud = new JFXComboBox<String>();
		cmbBaud.setMaxWidth(Double.MAX_VALUE);
		cmbBaud.getItems().addAll(
			   "300",   "600",  "1200",   "1800",  
			  "2400",  "4800",  "9600",  "19200",
			 "38400", "57600","115200", "230400",
			"460800","500000"
		);
		cmbSelect(cmbBaud,dev.getBaud(),7);

		final JFXComboBox<String> cmbData = new JFXComboBox<String>();
		cmbData.setMaxWidth(Double.MAX_VALUE);
		cmbData.getItems().addAll("5","6","7","8");
		cmbSelect(cmbData,dev.getData(),3);

		final JFXComboBox<String> cmbPart = new JFXComboBox<String>();
		cmbPart.setMaxWidth(Double.MAX_VALUE);
		cmbPart.getItems().addAll("none","odd","event","mark","space");
		cmbSelect(cmbPart,dev.getParity(),0);

		final JFXComboBox<String> cmbStop = new JFXComboBox<String>();
		cmbStop.setMaxWidth(Double.MAX_VALUE);
		cmbStop.getItems().addAll("1","2");
		cmbSelect(cmbStop,dev.getStopBit(),0);

		final String TXT_OPEN  = "開啟";
		final String TXT_CLOSE = "關閉";
		final Button btnEnable = PanBase.genButton1(TXT_OPEN,"");
		btnEnable.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		btnEnable.setOnAction(event->{
			if(dev.isAlive.get()==true){
				dev.close();
				btnEnable.setText(TXT_OPEN);
			}else{
				String infoName = "" + 
					cmbName.getSelectionModel().getSelectedItem() + "," +
					cmbBaud.getSelectionModel().getSelectedItem() + "," +
					cmbData.getSelectionModel().getSelectedItem() + 
					cmbPart.getSelectionModel().getSelectedItem().charAt(0) + 
					cmbStop.getSelectionModel().getSelectedItem();				
				if(dev.open(infoName)==0L){
					btnEnable.setText(TXT_OPEN);					
				}else{
					btnEnable.setText(TXT_CLOSE);
				}
			}
		});
		if(dev.isAlive.get()==true){
			btnEnable.setText(TXT_CLOSE);
		}else{
			btnEnable.setText(TXT_OPEN);
		}
		
		TextArea boxOutput = new TextArea();
		boxOutput.disableProperty().bind(dev.isAlive.not());
		boxOutput.setMaxWidth(Double.MAX_VALUE);
		boxOutput.setPrefHeight(200);
				
		JFXComboBox<String> cmbFeed = new JFXComboBox<String>();
		cmbFeed.disableProperty().bind(dev.isAlive.not());
		cmbFeed.getItems().addAll("LF","CR","CR,LF","<HEX>");
		cmbFeed.getSelectionModel().select(0);
		
		JFXTextField boxInput = new JFXTextField();
		boxInput.disableProperty().bind(dev.isAlive.not());
		boxInput.requestFocus();
		HBox.setHgrow(boxInput, Priority.ALWAYS);		
		boxInput.setOnAction(event->{
			String txt = boxInput.getText();
			//The feed option is hard-code !!!
			int idx = cmbFeed.getSelectionModel().getSelectedIndex();
			switch(idx){
			case 0: txt = txt + "\n"  ; break;
			case 1: txt = txt + "\r"  ; break;
			case 2: txt = txt + "\r\n"; break;
			//the option <HEX> is no support now.
			}
			dev.writeTxt(txt);
			boxInput.setText("");//clear for next turn~~~
			boxInput.requestFocus();
		});
		
		GridPane lay0 = new GridPane();
		lay0.disableProperty().bind(dev.isAlive);
		lay0.getStyleClass().add("grid-small");
		lay0.addRow(0, 
			new Label("通訊埠"), new Label("："), cmbName,
			new Label("鮑率")  , new Label("："), cmbBaud
		);
		lay0.addRow(1, 
			new Label("位元")  , new Label("："), cmbData, 
			new Label("檢查碼"), new Label("："), cmbPart, 
			new Label("停止碼"), new Label("："), cmbStop
		);
		
		HBox lay1 = new HBox();
		lay1.getStyleClass().add("hbox-medium");
		lay1.getChildren().addAll(lay0,btnEnable);

		HBox lay2 = new HBox();
		lay2.getStyleClass().add("hbox-small");
		lay2.getChildren().addAll(boxInput,cmbFeed);
		
		VBox root = new VBox();
		root.getStyleClass().add("vbox-small-white");
		root.getChildren().addAll(lay1,boxOutput,lay2);
		
		getChildren().add(root);
	}
	
	private void cmbSelect(ComboBox<String> cmb, String val, int def_index){
		if(val.contains("?")==true){
			cmb.getSelectionModel().select(def_index);
			return;
		}
		ObservableList<String> lst = cmb.getItems();
		for(int i=0; i<lst.size(); i++){
			if(lst.get(i).equalsIgnoreCase(val)==true){
				cmb.getSelectionModel().select(i);
				return;
			}
		}
		lst.add(val);
		cmb.getSelectionModel().selectLast();
	}
	
	public static ComboBox<String> getSupportList(ComboBox<String> box){
		ObservableList<String> lst = box.getItems();
		if(Misc.isPOSIX()==true){
			//how to list device file??
			File fs = new File("/dev");
			String[] names = fs.list(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) { return 
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
			lst.addAll("/dev/ttyS0" ,"/dev/ttyS1" ,"/dev/ttyS2" ,"/dev/ttyS3");
		}else{
			//we don't know how to enumerate Windows Serial Ports 
			lst.addAll(
				"\\\\.\\COM1" ,"\\\\.\\COM2" ,"\\\\.\\COM3" ,"\\\\.\\COM4",
				"\\\\.\\COM5" ,"\\\\.\\COM6" ,"\\\\.\\COM7" ,"\\\\.\\COM8",
				"\\\\.\\COM9" ,"\\\\.\\COM10","\\\\.\\COM11","\\\\.\\COM12",
				"\\\\.\\COM13","\\\\.\\COM14","\\\\.\\COM15","\\\\.\\COM16"
			);
		}
		box.getSelectionModel().select(0);
		return box;
	}
}
