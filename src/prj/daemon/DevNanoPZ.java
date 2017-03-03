package prj.daemon;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.PanTTY;

/**
 * This implements command set for Newport NanoPZ actuator.
 * The product name is PZA12. It is a piezo motor.  
 * @author qq
 *
 */
public class DevNanoPZ extends DevTTY {
	
	private static final String STA_NULL = "----------";
	
	public DevNanoPZ(){
		
	}
	
	private IntegerProperty location = new SimpleIntegerProperty(0);//μ-step
	
	private IntegerProperty address = new SimpleIntegerProperty(1);
	
	private BooleanProperty STA_EPROM = new SimpleBooleanProperty();//bit-2
	private BooleanProperty STA_ERROR = new SimpleBooleanProperty();//bit-4
	private BooleanProperty STA_OA = new SimpleBooleanProperty();//bit-16
	private BooleanProperty STA_OB = new SimpleBooleanProperty();//bit-17
	private BooleanProperty STA_ENC = new SimpleBooleanProperty();//bit-18
	private BooleanProperty STA_INC = new SimpleBooleanProperty();//bit-19
	private BooleanProperty STA_DEC = new SimpleBooleanProperty();//bit-20
	private FloatProperty STA_TEMP = new SimpleFloatProperty();//bit-24~31
	
	private StringProperty STA_INFO1 = new SimpleStringProperty(STA_NULL);
	private StringProperty STA_INFO2 = new SimpleStringProperty(STA_NULL);
	private StringProperty STA_INFO3 = new SimpleStringProperty(STA_NULL);//last error message
	
	public void connect(String port_name){
		open(port_name+",19200,8n1");
		get_status(null);
		Misc.delay(10);
		writeTxt(String.format("%dMO\r\n", address.get()));//just for convenient~~~		
	}
	
	public void disconnect(){
		close();
	}

	private void get_last_error(){
		String cmd = String.format("%dTE?\r\n", address.get());
		fetch(cmd,"\r\n",event->{
			String txt = (String)event.getSource();
			int val = txt.indexOf("TE? ");
			if(val>=0){
				txt = txt.substring(val+4);
				val = Integer.valueOf(txt);
				STA_INFO3.set("error code="+val);
			}else{
				Misc.logv("fail to parse"+txt);
			}
		});
	}
	
	private void get_status(EventHandler<ActionEvent> value){
		int addr = address.get();
		String[] cmd = {
			String.format("%dPH?\r\n", addr),
			String.format("%dTP?\r\n", addr)
		};
		fetch(cmd, "\r\n", event->{
			String _txt = (String)event.getSource();
			String[] txt = _txt.split("\t");
			if(txt.length<=1){
				Misc.logv("??? --> "+_txt);
				return;
			}
			int pos =0;
			pos = txt[0].indexOf("PH? ");
			if(pos>=0){				
				try{
					txt[0] = txt[0].substring(pos+4);
					long stat = Long.valueOf(txt[0]);
					stat = shift_bit(stat, STA_EPROM, 2);
					stat = shift_bit(stat, STA_ERROR, 2);
					stat = shift_bit(stat, STA_OA, 12);
					stat = shift_bit(stat, STA_OB, 1);
					stat = shift_bit(stat, STA_ENC, 1);
					stat = shift_bit(stat, STA_INC, 1);
					stat = shift_bit(stat, STA_DEC, 1);
					stat = shift_bit(stat, null, 4);					
					STA_TEMP.set( (((float)stat)/1024.f)*500.f );//remainder-bits
					update_info();
				}catch(NumberFormatException e){
					Misc.logw("fail to parse PH:"+txt[0]);
				}
			}
			pos = txt[1].indexOf("TP? ");
			if(pos>=0){					
				try{
					txt[1] = txt[1].substring(pos+4);
					location.set(Integer.valueOf(txt[1]));
				}catch(NumberFormatException e){
					Misc.logw("fail to parse TP:"+txt[1]);
				}
			}
			if(value!=null){
				value.handle(new ActionEvent(txt,null));
			}			
		});
	}
	
	private long shift_bit(long val, BooleanProperty flag, int shift){
		val = val>>shift;
		if(flag!=null){
			if((val&0x1)==0){
				flag.set(false);
			}else{
				flag.set(true);
			}
		}		
		return val;
	}
	
	private void update_info(){
		String txt = "";
		if(STA_EPROM.get()==true){
			txt = txt + "WP ";
		}else{
			txt = txt + "wp ";
		}
		if(STA_ERROR.get()==true){
			txt = txt + "ER ";
		}else{
			txt = txt + "er ";
		}
		if(STA_OA.get()==true){
			txt = txt + "OA ";
		}else{
			txt = txt + "oa ";
		}
		if(STA_OB.get()==true){
			txt = txt + "OB ";
		}else{
			txt = txt + "ob ";
		}
		if(STA_ENC.get()==true){
			txt = txt + "ENC ";
		}else{
			txt = txt + "enc ";
		}
		if(STA_INC.get()==true){
			txt = txt + "INC ";
		}else{
			txt = txt + "inc ";
		}
		if(STA_DEC.get()==true){
			txt = txt + "DEC ";
		}else{
			txt = txt + "dec ";
		}
		STA_INFO1.set(txt);
		STA_INFO2.set(String.format("%.1f°C", STA_TEMP.get()));
	}
	
	public void jogging(int nn){
		if(Math.abs(nn)>7){
			Misc.logw("jogg parameter out of range");
			return;
		}
		writeTxt(String.format("%dJA%d\r\n", address.get(),nn));
	}
	
	public void zero_position(){
		writeTxt(String.format("%dOR\r\n", address.get()));		
	}
	//-----------------------//

	@Override
	protected Node eventLayout(){
		
		GridPane root = new GridPane();
		root.getStyleClass().add("grid-medium");
		
		final ComboBox<String> cmbPort = new ComboBox<String>();
		cmbPort.setMaxWidth(Double.MAX_VALUE);
		PanTTY.getSupportList(cmbPort);
		
		Button btnPort =new Button("連接埠：");
		btnPort.setOnAction(event->{
			if(isAlive.get()==false){
				connect(cmbPort.getSelectionModel().getSelectedItem());
			}else{
				disconnect();
			}			
		});
		btnPort.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(btnPort, Priority.ALWAYS);
		
		Label txtAddr =new Label("編號：");

		ComboBox<Integer> cmbAddr = new ComboBox<Integer>();	
		cmbAddr.setMaxWidth(Double.MAX_VALUE);
		cmbAddr.getItems().addAll(1,2,3,4,5);
		cmbAddr.getSelectionModel().select(0);
		address.bind(cmbAddr.getSelectionModel().selectedIndexProperty().add(1));

		Label txtStat =new Label("狀態：");
		txtStat.disableProperty().bind(isAlive.not());
		txtStat.setMaxWidth(Double.MAX_VALUE);
		txtStat.setOnMouseClicked(event->{
			//update device status~~~
			get_status(null);
		});
		
		Label txtInfo1 = new Label();
		txtInfo1.disableProperty().bind(isAlive.not());
		txtInfo1.setMaxWidth(Double.MAX_VALUE);
		txtInfo1.textProperty().bind(STA_INFO1);
		
		Label txtInfo2 = new Label();
		txtInfo2.disableProperty().bind(isAlive.not());
		txtInfo2.setMaxWidth(Double.MAX_VALUE);
		txtInfo2.textProperty().bind(STA_INFO2);
		
		Label txtError1 = new Label("Last Error：");
		txtError1.disableProperty().bind(isAlive.not());
		txtError1.setMaxWidth(Double.MAX_VALUE);
		txtError1.setOnMouseClicked(event->{
			get_last_error();
		});
		
		Label txtError2 = new Label();
		txtError2.disableProperty().bind(isAlive.not());
		txtError2.setMaxWidth(Double.MAX_VALUE);
		txtError2.textProperty().bind(STA_INFO3);
		//----------------------------//
		
		Label txtLoca1 =new Label("μ-step：");
		txtLoca1.disableProperty().bind(isAlive.not());
		txtLoca1.setMaxWidth(Double.MAX_VALUE);
		txtLoca1.setOnMouseClicked(event->{
			//reset position to zero~~~
		});
		
		Label txtLoca2 = new Label(STA_NULL);
		txtLoca2.disableProperty().bind(isAlive.not());
		txtLoca2.setMaxWidth(Double.MAX_VALUE);
		txtLoca2.textProperty().bind(location.asString());
		txtLoca2.setOnMouseClicked(event->{
			//update location!!!
		});

		TextField boxStepRel = new TextField("1000");
		boxStepRel.disableProperty().bind(isAlive.not());
		boxStepRel.setMaxWidth(Double.MAX_VALUE);
		
		ComboBox<String> cmbStepJog = new ComboBox<String>();
		cmbStepJog.disableProperty().bind(isAlive.not());		
		cmbStepJog.setMaxWidth(Double.MAX_VALUE);
		cmbStepJog.getItems().addAll(
			"   3.2 μ-step",
			"    16 μ-step",
			"    80 μ-step",
			"   400 μ-step",
			" 2,000 μ-step",
			"10,000 μ-step",
			"48,000 μ-step"
		);
		cmbStepJog.getSelectionModel().select(0);
		
		final String TXT_MODE_REL = "相對模式："; 
		final String TXT_MODE_JOG = "搖桿模式：";		
		Label txtStepMode =new Label(TXT_MODE_JOG);		
		txtStepMode.disableProperty().bind(isAlive.not());
		txtStepMode.setMaxWidth(Double.MAX_VALUE);
		txtStepMode.setOnMouseClicked(event->{
			String txt = txtStepMode.getText();
			if(txt.equalsIgnoreCase(TXT_MODE_REL)==true){
				txtStepMode.setText(TXT_MODE_JOG);
				boxStepRel.setVisible(false);
				cmbStepJog.setVisible(true);				
			}else{
				txtStepMode.setText(TXT_MODE_REL);
				boxStepRel.setVisible(true);
				cmbStepJog.setVisible(false);
			}
		});
		boxStepRel.setVisible(false);
		cmbStepJog.setVisible(true);
		
		final EventHandler<MouseEvent> eventPressed = new EventHandler<MouseEvent>(){
			@Override
			public void handle(MouseEvent event) {				
				int stp = Integer.valueOf(boxStepRel.getText());
				int nn = cmbStepJog.getSelectionModel().getSelectedIndex()+1;
				char dir = (char)((Button)event.getSource()).getUserData();
				if(dir=='-'){
					stp = -1 * stp;
					nn = -1 * nn;
				}
				String mod = txtStepMode.getText();
				if(mod.equalsIgnoreCase(TXT_MODE_REL)==true){
										
				}else{
					jogging(nn);
				}
			}
		};
		
		final EventHandler<MouseEvent> eventRelease = new EventHandler<MouseEvent>(){
			@Override
			public void handle(MouseEvent event) {
				String mod = txtStepMode.getText();
				if(mod.equalsIgnoreCase(TXT_MODE_JOG)==true){
					jogging(0);
					Misc.delay(20);//motor may still be in progress....
				}
				get_status(null);
			}
		};
		
		Button btnDirNeg = PanBase.genButton0("","dir-left.png");
		btnDirNeg.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		btnDirNeg.setUserData('-');
		btnDirNeg.disableProperty().bind(isAlive.not());
		btnDirNeg.addEventFilter(MouseEvent.MOUSE_PRESSED,eventPressed);
		btnDirNeg.addEventFilter(MouseEvent.MOUSE_RELEASED,eventRelease);
		
		Button btnDirPos = PanBase.genButton0("","dir-right.png");
		btnDirPos.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		btnDirPos.setUserData('+');
		btnDirPos.disableProperty().bind(isAlive.not());
		btnDirPos.addEventFilter(MouseEvent.MOUSE_PRESSED,eventPressed);
		btnDirPos.addEventFilter(MouseEvent.MOUSE_RELEASED,eventRelease);
		
		HBox lay2 = new HBox();		
		lay2.getStyleClass().add("hbox-small-space");
		lay2.getChildren().addAll(btnDirNeg,btnDirPos);
		HBox.setHgrow(btnDirNeg, Priority.ALWAYS);
		HBox.setHgrow(btnDirPos, Priority.ALWAYS);
		
		root.add(btnPort, 0, 0);
		root.add(cmbPort, 1, 0);
		
		root.add(txtAddr, 0, 1);
		root.add(cmbAddr, 1, 1);
		
		root.add(txtStat, 0, 2);
		root.add(txtInfo1, 1, 2);
		root.add(txtInfo2, 1, 3);
		
		root.add(txtError1, 0, 4);
		root.add(txtError2, 1, 4);
		
		root.add(txtLoca1, 0, 5);
		root.add(txtLoca2, 1, 5);
		
		root.add(txtStepMode, 0, 6);
		root.add(boxStepRel, 1, 6);
		root.add(cmbStepJog, 1, 6);
		
		root.add(lay2, 0, 7, 2, 1);
		
		return root;
	}
}
