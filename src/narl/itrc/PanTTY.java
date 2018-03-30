package narl.itrc;

import java.io.File;
import java.io.FilenameFilter;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * A wrap panel for TTY device.
 * @author qq
 *
 */
public class PanTTY extends Pane {

	private DevTTY dev;
	
	public PanTTY(DevTTY device){
		dev = device;
		watcher.setCycleCount(Timeline.INDEFINITE);
		init_layout();
	}
	
	private TextArea boxOutput = new TextArea();
	
	private Timeline watcher = new Timeline(new KeyFrame(
		Duration.millis(250),
		event->{
			String txt = dev.readTxt();
			if(txt==null){
				return;
			}
			boxOutput.appendText(txt);
		}
	));

	private void init_layout(){
		
		boxOutput.disableProperty().bind(dev.isAlive().not());
		boxOutput.setMaxWidth(Double.MAX_VALUE);
		boxOutput.setPrefHeight(200);
				
		JFXComboBox<String> cmbFeed = new JFXComboBox<String>();
		cmbFeed.disableProperty().bind(dev.isAlive().not());
		cmbFeed.getItems().addAll("LF","CR","CR,LF","<HEX>");
		cmbFeed.getSelectionModel().select(0);
		
		JFXTextField boxInput = new JFXTextField();
		boxInput.disableProperty().bind(dev.isAlive().not());
		
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
		
		LayPortSetting laySetting = new LayPortSetting(
			dev,
			eventOpen->{ 
				dev.open(); 
				watcher.play(); 
				boxInput.requestFocus();
			},
			eventClose->{ 
				dev.close(); 
				watcher.pause(); 
			}
		);
			
		HBox layInput = new HBox();
		layInput.getStyleClass().add("hbox-small");
		layInput.getChildren().addAll(boxInput,cmbFeed);
		
		VBox root = new VBox();
		root.getStyleClass().add("vbox-small");
		root.getChildren().addAll(laySetting,boxOutput,layInput);
		
		getChildren().add(root);
	}
	//---------------------------------//
	
	private static final String TXT_OPEN  = "開啟";
	private static final String TXT_CLOSE = "關閉";

	/**
	 * Common TTY setting panel.
	 * @author qq
	 *
	 */
	private static class LayPortSetting extends HBox {
		
		private DevTTY dev;
		
		/**
		 * Generate a common TTY setting panel.<p>
		 * User must manually pen and close device in lambda event.
		 * @param device - TTY device
		 * @param eventOpen - when user press 'open' button
		 * @param eventClose - when user press 'close' button
		 */
		public LayPortSetting(
			final DevTTY device,
			final EventHandler<ActionEvent> eventOpen,
			final EventHandler<ActionEvent> eventClose
		){
			dev = device;

			final JFXComboBox<String> cmbBaud = new JFXComboBox<String>();
			cmbBaud.setMaxWidth(Double.MAX_VALUE);
			cmbBaud.getItems().addAll(
				   "300",   "600",  "1200",   "1800",  
				  "2400",  "4800",  "9600",  "19200",
				 "38400", "57600","115200", "230400",
				"460800","500000"
			);
			cmbSelect(cmbBaud,dev.getBaud(),7);

			final JFXComboBox<String> cmbDataBit = new JFXComboBox<String>();
			cmbDataBit.setMaxWidth(Double.MAX_VALUE);
			cmbDataBit.getItems().addAll("5","6","7","8");
			cmbSelect(cmbDataBit,dev.getData(),3);

			final JFXComboBox<String> cmbPartityBit = new JFXComboBox<String>();
			cmbPartityBit.setMaxWidth(Double.MAX_VALUE);
			cmbPartityBit.getItems().addAll("none","odd","event","mark","space");
			cmbSelect(cmbPartityBit,dev.getParity(),0);

			final JFXComboBox<String> cmbStopBit = new JFXComboBox<String>();
			cmbStopBit.setMaxWidth(Double.MAX_VALUE);
			cmbStopBit.getItems().addAll("1","2");
			cmbSelect(cmbStopBit,dev.getStopBit(),0);

			ComboBox<String> cmbPortName = genPortCombo(null,dev);
			
			Button btnPortEnable = PanBase.genButton2(TXT_OPEN, "");			
			btnPortEnable.setOnAction(e->{
				if(dev.isAlive().get()==true){
					btnPortEnable.setText(TXT_OPEN);//Next state is 'open TTY' again~~
					eventClose.handle(e);					
				}else{	
					btnPortEnable.setText(TXT_CLOSE);//Next state is 'close TTY' again~~
					String path = "" + 
						cmbPortName.getSelectionModel().getSelectedItem() + "," +
						cmbBaud.getSelectionModel().getSelectedItem() + "," +
						cmbDataBit.getSelectionModel().getSelectedItem() + 
						cmbPartityBit.getSelectionModel().getSelectedItem().charAt(0) + 
						cmbStopBit.getSelectionModel().getSelectedItem();
					dev.setInfoPathAttr(path);
					eventOpen.handle(e);
				}
			});
			//check device state~~~~
			if(dev.isAlive().get()==true){
				btnPortEnable.setText(TXT_CLOSE);
			}else{			
				btnPortEnable.setText(TXT_OPEN);
			}
			btnPortEnable.setPrefSize(64, 64);
			
			GridPane lay0 = new GridPane();
			lay0.getStyleClass().add("grid-medium");
			lay0.disableProperty().bind(dev.isAlive());
			
			lay0.add(new Label("通訊埠："), 0, 0);
			lay0.add(cmbPortName, 1, 0);

			lay0.add(new Label("鮑率："), 0, 1);
			lay0.add(cmbBaud, 1, 1);

			lay0.add(new Label("位元："),3, 0);
			lay0.add(cmbDataBit, 4, 0);
			
			lay0.add(new Label("檢查碼："), 3, 1);
			lay0.add(cmbPartityBit, 4, 1);
			
			lay0.add(new Label("停止碼："), 5, 0);
			lay0.add(cmbStopBit, 6, 0);

			getStyleClass().add("hob-medium");			
			getChildren().addAll(btnPortEnable,lay0);
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
	};
	
	private static class PanPortSetting extends PanBase {
		
		private LayPortSetting root;
		
		public PanPortSetting(
			final DevTTY device,
			final EventHandler<ActionEvent> eventOpen,
			final EventHandler<ActionEvent> eventClose
		){
			root = new LayPortSetting(
				device,
				e1->{
					eventOpen.handle(e1);
					dismiss();
				},
				e2->{
					eventClose.handle(e2);
					dismiss();
				}
			);
		}
		@Override
		public Node eventLayout(PanBase self) {
			return root;
		}
		@Override
		public void eventShown(PanBase self) {
		}
	};
	
	public static void showSetting(
		final DevTTY device,
		final EventHandler<ActionEvent> eventOpen,
		final EventHandler<ActionEvent> eventClose
	){
		new PanPortSetting(
			device,
			eventOpen,
			eventClose
		).standby();
	}
	//---------------------------------//

	public static ComboBox<String> genPortCombo(
		ComboBox<String> box,
		final DevTTY dev
	){
		if(box==null){
			box = new JFXComboBox<String>();
		}
		box.disableProperty().bind(dev.isAlive());
		
		ObservableList<String> lst = box.getItems();
		if(Gawain.isPOSIX==true){
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
