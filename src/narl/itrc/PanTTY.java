package narl.itrc;

import java.io.File;
import java.io.FilenameFilter;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.sun.glass.ui.Application;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PanTTY extends PanBase {

	private static final String TXT_CONNECT   = "連線";
	private static final String TXT_DISCONNECT= "離線";
	private static final String ICON_CONNECT   = "lan-connect.png";
	private static final String ICON_DISCONNECT= "lan-disconnect.png";
	
	private final DevTTY dev;
	
	private boolean useContactBox = false;
	
	public PanTTY(){
		dev = new DevTTY();
		useContactBox = true;
	}
	
	public PanTTY(DevTTY device){
		dev = device;
	}
	
	public static PanBase popup(DevTTY device){		
		return new PanTTY(device).appear();
	} 
	
	private static void init_dev_name(
		final ComboBox<String> box,
		final DevTTY dev
	){	
		ObservableList<String> lst = box.getItems();
		
		if(dev!=null){
			//set the first device name
			String name = dev.getPathName();
			if(name.length()!=0){
				String[] attr = name.split(",");
				if(attr.length>=3){
					lst.add(attr[0]);
				}
			}
		}
		
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
			lst.addAll("/dev/ttyS0", "/dev/ttyS1", "/dev/ttyS2", "/dev/ttyS3");
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
	}
	
	private class TaskDataDrain extends Task<Integer> {
		int cnt = 0;
		int len = 0;
		byte[] buf = new byte[1024];
				
		final TextArea boxOutput = new TextArea();
		final JFXCheckBox chkPrintHEX = new JFXCheckBox("HEX 輸出");	
		
		private Runnable event = new Runnable(){
			@Override
			public void run() {
				if(chkPrintHEX.isSelected()==true){
					for(int i=0; i<len; i++){
						if((cnt+i)%8==0){
							boxOutput.appendText("\n");
						}
						boxOutput.appendText(String.format("%02X ", (int)buf[i]));
					}
				}else{
					boxOutput.appendText(new String(buf,0,len));
				}
				cnt+=len;
			}
		};
		
		@Override
		protected Integer call() throws Exception {
			while(isCancelled()==false){
				len = dev.readBuff(buf);
				if(len!=0){
					Application.invokeAndWait(event);
				}				
			}
			return 0;
		}
	};
	private TaskDataDrain looper = null;
	
	private final GridPane layCtrl = new GridPane();
	
	private final Label txtInfo = new Label();
	
	private final Button btnLink = PanBase.genButton2("","");
		
	private void event_linked(){
		btnLink.setText(TXT_CONNECT);
		btnLink.setGraphic(Misc.getResIcon(ICON_CONNECT));
		layCtrl.setDisable(false);
	}
	
	private void event_unlink(){
		btnLink.setText(TXT_DISCONNECT);
		btnLink.setGraphic(Misc.getResIcon(ICON_DISCONNECT));
		layCtrl.setDisable(true);
	}
	
	private Parent gen_layout_contact(){
		
		final Button btnExit = PanBase.genButton3(TXT_DISCONNECT,ICON_DISCONNECT);
		btnExit.setMaxWidth(Double.MAX_VALUE);
		btnExit.setOnAction(event->{			
			//this is a trick!!!
			looper.cancel();
			dev.close();
			event_linked();
			getScene().setRoot(gen_layout_control());			
		});
		
		final Button btnClear = PanBase.genButton2("清除",null);
		btnClear.setMaxWidth(Double.MAX_VALUE);
		btnClear.setOnAction(event->{
			looper.boxOutput.clear();
		});
		
		final JFXCheckBox chkKeyinHEX = new JFXCheckBox("HEX 輸入");
		
		final JFXComboBox<String> cmbFeed = new JFXComboBox<String>();
		cmbFeed.setMaxWidth(Double.MAX_VALUE);
		cmbFeed.getItems().addAll(
			"LF",
			"CR",
			"CR,LF"
		);
		cmbFeed.getSelectionModel().select(0);
		
		final JFXComboBox<String> cmbDelay = new JFXComboBox<String>();
		cmbDelay.setMaxWidth(Double.MAX_VALUE);
		cmbDelay.getItems().addAll(
			"No Delay", 
			"10ms",
			"20ms",
			"30ms",
			"50ms",
			"100ms"
		);
		cmbDelay.getSelectionModel().select(0);
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		lay1.getChildren().addAll(
			btnExit,
			btnClear,
			looper.chkPrintHEX,
			chkKeyinHEX,
			cmbFeed,
			cmbDelay
		);

		final TextField boxInput = new TextField();
		boxInput.requestFocus();
		boxInput.setOnAction(event->{
			String txt = boxInput.getText();
			//The feed option is hard-code !!!
			int idx = cmbFeed.getSelectionModel().getSelectedIndex();
			switch(idx){
			case 0: txt = txt + "\n"  ; break;
			case 1: txt = txt + "\r"  ; break;
			case 2: txt = txt + "\r\n"; break;
			}
			dev.writeTxt(txt);
			boxInput.setText("");//clear for next turn~~~
			boxInput.requestFocus();
		});
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("vbox-small");		
		lay0.disableProperty().bind(layCtrl.disabledProperty().not());
		lay0.setCenter(looper.boxOutput);
		lay0.setRight(lay1);
		lay0.setBottom(boxInput);
		return lay0;
	}
	
	private Parent gen_layout_control(){
		
		looper = null;//reset this variable again!!!
		
		final JFXComboBox<String> cmbPortName = new JFXComboBox<String>();
		GridPane.setHgrow(cmbPortName, Priority.ALWAYS);
		cmbPortName.setMaxWidth(Double.MAX_VALUE);
		init_dev_name(cmbPortName,dev);
		
		final JFXComboBox<String> cmbBaudRate = new JFXComboBox<String>();
		GridPane.setHgrow(cmbBaudRate, Priority.ALWAYS);
		cmbBaudRate.setMaxWidth(Double.MAX_VALUE);
		cmbBaudRate.getItems().addAll(
			   "300",   "600",  "1200",   "1800",  
			  "2400",  "4800",  "9600",  "19200",
			 "38400", "57600","115200", "230400",
			"460800","500000"
		);
		cmbBaudRate.getSelectionModel().select(6);
		
		final JFXComboBox<String> cmbDataBit = new JFXComboBox<String>();
		GridPane.setHgrow(cmbDataBit, Priority.ALWAYS);
		cmbDataBit.setMaxWidth(Double.MAX_VALUE);
		cmbDataBit.getItems().addAll("5","6","7","8");
		cmbDataBit.getSelectionModel().select(3);
		
		final JFXComboBox<String> cmbMaskBit = new JFXComboBox<String>();
		GridPane.setHgrow(cmbMaskBit, Priority.ALWAYS);
		cmbMaskBit.setMaxWidth(Double.MAX_VALUE);
		cmbMaskBit.getItems().addAll(
			"none", "odd",
			"event","mark",
			"space"
		);
		cmbMaskBit.getSelectionModel().select(0);
		
		final JFXComboBox<String> cmbStopBit = new JFXComboBox<String>();
		GridPane.setHgrow(cmbStopBit, Priority.ALWAYS);
		cmbStopBit.setMaxWidth(Double.MAX_VALUE);
		cmbStopBit.getItems().addAll("1","2");
		cmbStopBit.getSelectionModel().select(0);
		
		layCtrl.setMaxWidth(Double.MAX_VALUE);
		layCtrl.getStyleClass().add("grid-medium");

		layCtrl.add(new Label("通訊埠"), 0, 0);
		layCtrl.add(new Label("：")    , 1, 0);
		layCtrl.add(cmbPortName        , 2, 0);

		layCtrl.add(new Label("連線鮑率"), 0, 1);
		layCtrl.add(new Label("：")      , 1, 1);
		layCtrl.add(cmbBaudRate          , 2, 1);

		layCtrl.add(new Label("資料位元"), 0, 2);
		layCtrl.add(new Label("：")      , 1, 2);
		layCtrl.add(cmbDataBit           , 2, 2);
		
		layCtrl.add(new Label("檢查碼"), 0, 3);
		layCtrl.add(new Label("：")    , 1, 3);
		layCtrl.add(cmbMaskBit         , 2, 3);
		
		layCtrl.add(new Label("停止碼"), 0, 4);
		layCtrl.add(new Label("：")    , 1, 4);
		layCtrl.add(cmbStopBit         , 2, 4);
		
		btnLink.setMaxWidth(Double.MAX_VALUE);
		btnLink.setOnAction(event->{
			if(dev==null){
				return;
			}
			if(dev.isOpen()==true){
				dev.close();
				event_linked();
			}else{
				String name = cmbPortName.getSelectionModel().getSelectedItem();
				String baud = cmbBaudRate.getSelectionModel().getSelectedItem();
				String data = cmbDataBit.getSelectionModel().getSelectedItem();
				String mask = cmbMaskBit.getSelectionModel().getSelectedItem();
				String stop = cmbStopBit.getSelectionModel().getSelectedItem();				
				dev.open(name + "," + baud + "," + data + mask.charAt(0) + stop);
				event_unlink();
				if(useContactBox==true){
					//generate a looper to take data~~~
					looper = new TaskDataDrain();
					new Thread(looper).start();
					getScene().setRoot(gen_layout_contact());
				}
			}
		});
		
		final VBox lay0 = new VBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.getChildren().addAll(
			layCtrl, 
			txtInfo, 
			btnLink
		);
		return lay0;
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		return gen_layout_control();
	}

	@Override
	public void eventShown(PanBase self) {
		if(dev==null){
			return;
		}
		if(dev.isOpen()==true){
			event_unlink();
		}else{
			event_linked();
		}
	}
	
	@Override
	public void eventClose(PanBase self) {
		if(useContactBox==true){
			if(looper!=null){
				looper.cancel();
			}
			dev.close();
		}
	}
}
