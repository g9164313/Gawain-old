package prj.daemon;

import java.io.File;
import java.io.FilenameFilter;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.sun.glass.ui.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanTTY extends PanBase {

	private static final String TEXT_CONNECT  = "連線";
	private static final String TEXT_DISCONNECT= "離線";
	private static final ImageView ICON_CONNECT   = Misc.getIconView("lan-connect.png");
	private static final ImageView ICON_DISCONNECT= Misc.getIconView("lan-disconnect.png");

	private DevTTY dev;
	private boolean closeAndShutdown = true;
	
	public PanTTY(){
		dev = new DevTTY();
	}
	
	public PanTTY(DevTTY device){
		dev = device;
		closeAndShutdown = false;
	}
	
	private SimpleBooleanProperty isOpen = new SimpleBooleanProperty();
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final JFXComboBox<String> cmbDevName = new JFXComboBox<String>();
		init_dev_name(cmbDevName);
		
		final JFXComboBox<String> cmbBaudRate = new JFXComboBox<String>();
		cmbBaudRate.getItems().addAll(
			   "300",   "600",  "1200",   "1800",  
			  "2400",  "4800",  "9600",  "19200",
			 "38400", "57600","115200", "230400",
			"460800","500000"
		);
		cmbBaudRate.getSelectionModel().select(6);
		
		final JFXComboBox<String> cmbDataBit = new JFXComboBox<String>();
		cmbDataBit.getItems().addAll("5","6","7","8");
		cmbDataBit.getSelectionModel().select(3);
		
		final JFXComboBox<String> cmbMaskBit = new JFXComboBox<String>();
		cmbMaskBit.getItems().addAll(
			"none", "odd",
			"event","mark",
			"space"
		);
		cmbMaskBit.getSelectionModel().select(0);
		
		final JFXComboBox<String> cmbStopBit = new JFXComboBox<String>();
		cmbStopBit.getItems().addAll("1","2");
		cmbStopBit.getSelectionModel().select(0);
		
		final ComboBox<?>[] lstCmb = {
			cmbDevName, cmbBaudRate,
			cmbDataBit,	cmbMaskBit, cmbStopBit
		};
		for(ComboBox<?> cmb:lstCmb) {
			cmb.setMaxWidth(Double.MAX_VALUE);
			cmb.disableProperty().bind(isOpen);
			GridPane.setFillWidth(cmb, true);			
		}
		
		final JFXButton btnLink = new JFXButton(TEXT_CONNECT);
		btnLink.getStyleClass().add("btn-raised-2");
		btnLink.setGraphic(ICON_CONNECT);
		btnLink.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(btnLink, true);
		btnLink.setOnAction(e->{		
			if(dev.isOpen()==true) {
				dev.close();
			} else {
				String name = gen_path_name(lstCmb);
				dev.open(name);
			}
			update_face(btnLink);
		});
		
		init_screen();
		
		stage().setOnShown(e->{
			dev.setPeek((buf,cnt)->update_screen(buf,cnt));
			update_item(lstCmb);
			update_face(btnLink);
		});
		stage().setOnCloseRequest(e->{
			dev.setPeek(null);
			if(closeAndShutdown) {
				dev.close();
			}
		});	
		
		final ComboBox<String> cmbFeed = new ComboBox<String>();
		cmbFeed.getItems().addAll("none","CR\\LF","CR","LF");
		cmbFeed.getSelectionModel().select(0);
		
		final CheckBox chkExpr = new CheckBox("HEX");
		
		final TextField boxInput = new TextField();
		boxInput.setOnAction(e->{
			String txt = boxInput.getText();
			if(chkExpr.isSelected()==true) {
				txt = Misc.unescapeJavaString(txt);
			}
			int feed = cmbFeed.getSelectionModel().getSelectedIndex();
			switch(feed) {
			case 1: txt = txt + "\r\n"; break;
			case 2: txt = txt + "\r"; break;
			case 3: txt = txt + "\n"; break;
			}
			dev.writeTxt(txt);
			boxInput.setText("");//for next round
		});
		
		final HBox lay3 = new HBox(chkExpr, boxInput, cmbFeed);
		lay3.getStyleClass().add("ground-pad");
		
		final GridPane lay2 = new GridPane();
		lay2.getStyleClass().add("ground-pad");
		lay2.addRow(0, new Label("通訊埠"), new Label("："), cmbDevName);
		lay2.addRow(1, new Label("鮑率"  ), new Label("："), cmbBaudRate);
		lay2.addRow(2, new Label("資料"  ), new Label("："), cmbDataBit);
		lay2.addRow(3, new Label("檢查碼"), new Label("："), cmbMaskBit);
		lay2.addRow(4, new Label("停止碼"), new Label("："), cmbStopBit);
		lay2.add(btnLink, 0, 5, 3, 1);
		
		final BorderPane lay1 = new BorderPane();
		lay1.disableProperty().bind(isOpen.not());
		lay1.getStyleClass().add("ground-pad");	
		lay1.setCenter(screen);
		lay1.setBottom(lay3);
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("ground-pad");
		lay0.setCenter(lay1);
		lay0.setRight(lay2);		
		return lay0;
	}
	
	private final int CUR_WIDTH = 20;
	private final int CUR_HEIGHT= 10;
	private int cursor = 0;//for screen
	private GridPane screen = new GridPane();	
	private void init_screen() { 
		screen.getStyleClass().addAll("border");
		for(int j=0; j<CUR_HEIGHT; j++) {
			for(int i=0; i<CUR_WIDTH; i++) {
				Label txt = new Label("  ");
				screen.add(txt, i, j);
			}
		}
	}
	
	private void update_screen(
		final byte[] buf, 
		final int cnt
	) {
		Application.invokeAndWait(()->{
			ObservableList<Node> lst = screen.getChildren();
			for(int i=0; i<cnt; i++) {
				if(cursor>=lst.size()) {
					//clear all text!!!
					for(Node nod:lst) {
						Label txt = (Label)nod;
						txt.setText("  ");
						txt.setTextFill(Color.BLACK);
					}
					cursor = 0;
				}
				Label txt = (Label)lst.get(cursor);
				if(32<=buf[i] && buf[i]<=126) {
					txt.setText(" "+(char)buf[i]);
					txt.setTextFill(Color.BLACK);
				}else {					
					txt.setText(""+buf[i]);
					txt.setTextFill(Color.CADETBLUE);
					if(buf[i]==10) {
						cursor = cursor -1 + (CUR_WIDTH - cursor%CUR_WIDTH);						
					}
				}
				cursor+=1;
			}
		});
	}
	
	private void update_face(final Button btn) {
		boolean flag = dev.isOpen();
		isOpen.set(flag);
		if(flag==true) {
			btn.setText(TEXT_DISCONNECT);
			btn.setGraphic(ICON_DISCONNECT);
		} else {
			btn.setText(TEXT_CONNECT);
			btn.setGraphic(ICON_CONNECT);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void update_item(ComboBox<?>... cmb) {
		if(dev.isOpen()==false) {
			return;
		}
		String txt = dev.getPathName();
		if(txt.length()==0) {
			return;
		}
		final String[] arg = txt.split(",");
		if(arg.length!=3) {
			Misc.loge("Invalid path name:%s", txt);
			return;
		}
		final String[] attr = {
			arg[0],
			arg[1],
			""+arg[2].charAt(0),
			mask_name(arg[2].charAt(1)),
			""+arg[2].charAt(2),
		};
		for(int i=0; i<attr.length; i++) {
			( (ComboBox<String>) cmb[i])
			.getSelectionModel()
			.select(attr[i]);
		}
	}
	
	private String mask_name(final char cc) {
		switch(cc) {
		case 'n': return "none"; 
		case 'o': return "odd";
		case 'e': return "event";
		case 'm': return "mark";
		case 's': return "space";
		}
		return "?? "+cc;
	}
	
	private String gen_path_name(final ComboBox<?>... cmb) {
		return String.format(
			"%s,%s,%s%c%s",
			(String)cmb[0].getSelectionModel().getSelectedItem(),//device name
			(String)cmb[1].getSelectionModel().getSelectedItem(),//baud rate
			(String)cmb[2].getSelectionModel().getSelectedItem(),//data bit
			((String)cmb[3].getSelectionModel().getSelectedItem()).charAt(0),//mask bit
			(String)cmb[4].getSelectionModel().getSelectedItem() //stop bit
		);
	}
	
	private static void init_dev_name(final ComboBox<String> box){
		ObservableList<String> lst = box.getItems();
		if(Gawain.isPOSIX==true){
			//how to list device file??
			File fs = new File("/dev");
			String[] names = fs.list(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) { return
					(name.matches("ttyS\\p{Digit}")) ||
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
		}else{
			//we don't know how to enumerate Windows Serial Ports
			final String[] name = new String[16];
			for(int i=0; i<name.length; i++) {
				name[i] = String.format("\\\\\\\\.\\\\COM%d", i+1);
			}
			lst.addAll(name);
		}
		box.getSelectionModel().select(0);
	}
}
