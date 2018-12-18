package prj.scada;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.DevBase;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * 
 * Control model for Multi-Film Rate/Thickness Monitor.
 * www.inficon.com
 * @author qq
 *
 */
public class DevSQM160 extends DevBase {

	private DevTTY conn = new DevTTY();
	
	public DevSQM160(){
		this("");
	}
	
	public DevSQM160(String tty){
		super("Dev-SQM160");
		conn.setPathName(tty);
	}	

	@Override
	protected boolean looper(TokenBase obj) {
		//if(conn.isOpen()==false){
		//	return false;
		//}
		((Token)obj).fetch();
		return true;
	}

	@Override
	protected boolean eventReply(TokenBase obj) {
		
		Token tkn = (Token)obj;
		
		//first, check response status
		char status = tkn.resp.charAt(0);				
		switch(status){
		case 'A':
			tkn.resp = tkn.resp.substring(1).trim();
			break;
		case 'C':
			Misc.loge("[%s] Invalid command",TAG);
			alert.setContentText("不合法的命令格式");
			alert.show();
			return true;
		case 'D':
			Misc.loge("[%s] Invalid data in command",TAG);
			alert.setContentText("不合法的參數格式");
			alert.show();
			return true;
		default:
			Misc.loge("[%s] ERROR!!!",TAG);
			alert.setContentText("內部錯誤");
			alert.show();
			return true;
		}				
		//second, update device property
		int idx = 0;
		try{					
			switch(tkn.getCMD()){
			case 'A':
				if(tkn.isQuery()==true){
					tkn.action();
				}else{							
					alert.setContentText("已更新薄膜參數");
					alert.show();
				}						
				break;
			
			case 'B':
				if(tkn.isQuery()==true){
					tkn.action();
				}else{							
					alert.setContentText("已更新系統-1參數");
					alert.show();
				}						
				break;
				
			case 'C':
				if(tkn.isQuery()==true){
					tkn.action();
				}else{							
					alert.setContentText("已更新系統-2參數");
					alert.show();
				}						
				break;
				
			case 'P'://Read the Frequency for a sensor
				idx = Integer.valueOf(tkn.getArgument())-1;
				listSwellRate[idx].set(tkn.resp);
				break;
				
			case 'N'://Read the Thickness for a sensor
				idx = Integer.valueOf(tkn.getArgument())-1;
				listThickness[idx].set(tkn.resp);
				break;
				
			case 'M'://Read the Frequency for all (Average)
				propSwellRate.set(tkn.resp);
				break;
				
			case 'O'://Read the Thickness for all (Average)
				propThickness.set(tkn.resp);
				break;
				
			case 'R'://Read the Crystal Life for a sensor
				break;
				
			case 'W'://Read All sensor simultaneously
				String[] val = tkn.resp.split(" ");
				for(int i=0; i<6; i++){
					listSwellRate[i].set(val[i*3+0]);
					listThickness[i].set(val[i*3+1]);
					listFrequency[i].set(val[i*3+2]);
				}
				break;
			
			case 'S'://Zero Average Thickness and Rate.						
				alert.setHeaderText("");
				alert.setContentText("平均厚度與頻率歸零");
				alert.show();
				break;
			case 'T'://Zero Time
				alert.setHeaderText("");
				alert.setContentText("計時歸零");
				alert.show();
				break;
			}
		}catch(NumberFormatException e){
			Misc.loge(
				"[%s] Invalid argument - %s",
				TAG,
				tkn.getContext()
			);
		}
		return true;
	}	
	
	@Override
	protected void eventLink() {
		conn.open();
		//update device parameter, firstly
		//period update device status
		setInterval(1);		
	}

	@Override
	protected void eventUnlink() {
		conn.close();
	}
	//---------------------------------//
	
	//the current Average Thickness
	public final StringProperty propThickness= new SimpleStringProperty();
		
	//the current Average Growth Rate 
	public final StringProperty propSwellRate = new SimpleStringProperty();
	
	//Sensor 1~6 Growth Rate
	public final StringProperty[] listSwellRate = {
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty()
	};
	
	//Sensor 1~6 Thickness data
	public final StringProperty[] listThickness = {
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty()
	};
	
	//Sensor 1~6 Frequency data
	protected final StringProperty[] listFrequency = {
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty(),
		new SimpleStringProperty()
	};
	
	//Read the Crystal Life for a sensor
	protected final StringProperty propCrystalLife = new SimpleStringProperty();
	
	public void setInterval(int sec){
		remove_remainder();
		offer(new Token("M"), sec, TimeUnit.SECONDS, true);
		offer(new Token("O"), sec, TimeUnit.SECONDS, true);
	}
	
	/**
	 * update film parameter 
	 * @param indx - film index
	 * @param box - text parameter
	 * @param bitSensor - per bit present one sensor, bit0 is sensor-1
	 */
	public void updateFilm(
		final int indx, 
		final int bitSensor,
		final String... param		
	){
		String name = param[0].trim().toUpperCase();
		if(name.length()<8){
			name = " "+name;
		}else{
			name = name.substring(0, 8);//only 8-character
		}
		
		int spoint2 = 59999;
		String time = param[6].trim();
		String[] val = time.split(":");
		if(val.length==2){
			try{
				int mm = Integer.valueOf(val[0]);
				int ss = Integer.valueOf(val[1]);
				spoint2 = mm*60 + ss;
			}catch(NumberFormatException e){
				Misc.logw("[%s] wrong time value - %s", time);
			}
		}else{
			Misc.logw("[%s] wrong time format - %s", time);
		}
		
		offer(new Token(String.format(
			"A%d%s %s %s %s %s %s %d %d",
			indx, name,
			param[1],
			param[2],
			param[3],
			param[4],
			param[5], spoint2,
			bitSensor
		)));
	}
	public void queryFilm(final TextField[] box){
		queryFilm(0, box);
	}
	public void queryFilm(
		final int idx, 
		final TextField[] box
	){
		offer(new Token(String.format("A%d?", idx)).setOnAction(event->{
			Token tkn = (Token)event.getSource();
			String name = tkn.resp.substring(0,8);
			box[0].setText(name);//name
			String[] val = get_values(tkn.resp.substring(8));
			box[1].setText(val[0]);//density
			box[2].setText(val[1]);//tooling
			box[3].setText(val[2]);//Z-Ratio
			box[4].setText(val[3]);//Final Thickness
			box[5].setText(val[4]);//Thickness set-point
			int time = Integer.valueOf(val[5]);
			int mm = time / 60;
			int ss = time % 60;
			box[6].setText(String.format("%02d:%02d", mm, ss));//Time set-point
			//filmSensor.set(Integer.valueOf(val[7]));
		}));
	}
	
	public void updateSystem1(
		final boolean useSimulate,
		final int     modeDisplay,
		final boolean useHighRes,
		String... param
	){
		offer(new Token(String.format(
			"B %s %d %d %d %s %s %d %d",
			param[0],
			(useSimulate)?(1):(0),
			modeDisplay,
			(useHighRes)?(1):(0),
			param[1],
			param[2],
			param[3],
			param[4],
			param[5],
			param[6],
			param[7]
		)));
	}	
	public void querySystem1(
		final CheckBox    simulate,
		final ComboBox<?> display,
		final CheckBox    resolution,
		final TextField[] box
	){
		offer(new Token("B?").setOnAction(event->{
			Token tkn = (Token)event.getSource();
			String[] val = get_values(tkn.resp);
			box[0].setText(val[0]);
			simulate.setSelected(  (Integer.valueOf(val[1])==0)?(false):(true));			
			display.getSelectionModel().select(Integer.valueOf(val[2]));//Rate and Thickness format
			resolution.setSelected((Integer.valueOf(val[3])==0)?(false):(true));//high or low rate resolution
			box[1].setText(val[ 4]);//Rate Filter
			box[2].setText(val[ 5]);//Crystal-1 Tooling
			box[3].setText(val[ 6]);//Crystal-2 Tooling
			box[4].setText(val[ 7]);//Crystal-3 Tooling
			box[5].setText(val[ 8]);//Crystal-4 Tooling
			box[6].setText(val[ 9]);//Crystal-5 Tooling
			box[7].setText(val[10]);//Crystal-6 Tooling
		}));
	}
	
	public void updateSystem2(
		final boolean etch,
		final String... param
	){
		offer(new Token(String.format(
			"C %s %s %s %s %s %s %d",
			param[0], param[1],
			param[2], param[3],
			param[4], param[5],
			(etch)?(1):(0)
		)));
	}
	public void querySystem2(
		final CheckBox etch, 
		final TextField[] box
	){
		offer(new Token("C?").setOnAction(event->{
			Token tkn = (Token)event.getSource();
			String[] val = get_values(tkn.resp);
			box[0].setText(val[0]);//minimum frequency
			box[1].setText(val[1]);//maximum frequency
			box[2].setText(val[2]);//minimum rate
			box[3].setText(val[3]);//maximum rate
			box[4].setText(val[4]);//minimum thickness
			box[5].setText(val[5]);//maximum thickness
			int _etch = Integer.valueOf(val[6]);
			etch.setSelected((_etch==0)?(false):(true));
		}));
	}
	
	private String[] get_values(String txt){
		txt = txt.trim();
		ArrayList<String> lst = new ArrayList<String>();
		int idx = txt.indexOf(' ');
		while(idx>0){
			lst.add(txt.substring(0, idx));
			txt = txt.substring(idx).trim();
			idx = txt.indexOf(' ');
		}
		lst.add(txt.trim());
		return lst.toArray(new String[lst.size()]);
	}
	
	/**
	 * Zero Average Thickness and Rate. <p>
	 * This also sets all active Sensor Rates and Thicknesses to zero.<p>
	 */
	public void zeroAverage(){
		offer(new Token("S"));
	}
	/**
	 * reset clock in machine.<p>
	 */
	public void zeroTime(){
		offer(new Token("T"));
	}
	
	private class Token extends TokenBase {
		
		public byte[] buff;
		
		public String resp = "";
		
		public Token(String msg){
			packet(msg);
		}
		
		private void packet(String msg){
			
			int cnt = msg.length();
			
			//There are four character in this packet.
			//They are <Sync>, <Length>, <CRC1> and <CRC2>			
			buff = new byte[cnt + 4];
			
			buff[0] = '!';
			buff[1] = (byte)(34 + cnt);
			for(int i=0; i<cnt; i++){
				buff[2+i] = (byte)(msg.charAt(i));
			}
			
			short crc = calcCRC(buff);
			cnt = buff.length;
			
			buff[cnt-2] = crcLow (crc);
			buff[cnt-1] = crcHigh(crc);
		}
		
		public char getCMD(){
			return (char)buff[2];
		}
		public boolean isQuery(){
			for(int i=3; i<3+getLength(buff); i++){
				if((char)buff[i]=='?'){
					return true;
				}
			}
			return false;
		}
		public String getArgument(){
			return new String(Arrays.copyOfRange(
				buff,
				3, 
				getLength(buff)
			));
		}
		
		public String getContext(){
			return new String(Arrays.copyOfRange(
				buff,
				2, 
				getLength(buff)
			));
		}
		
		public void fetch(){
			conn.writeBuff(buff);
			byte[] temp = conn.readBuff();
			if(temp[0]!='!'){				
				resp = "";
				Misc.loge("Invalid Sync character");
				return;
			}
			resp = new String(Arrays.copyOfRange(
				temp, 
				2, 
				2+getLength(temp)-1
			));
			//I don't know why checking CRC is always fail.... 
		}
	};
	
	private static int getLength(byte[] buff){			
		return ((int)buff[1]&0xFF)-34;
	}		
		
	private static short calcCRC(byte[] str) {
		short crc = 0;
		short tmpCRC;
		int length = 1 + str[1] - 34;
		crc = (short) 0x3fff;
		for (int jx = 1; jx <= length; jx++) {
			crc = (short) (crc ^ (short) str[jx]);
			for (int ix = 0; ix < 8; ix++) {
				tmpCRC = crc;
				crc = (short) (crc >> 1);
				if ((tmpCRC & 0x1) == 1) {
					crc = (short) (crc ^ 0x2001);
				}
			}
			crc = (short) (crc & 0x3fff);
		}
		return crc;
	}
	private static byte crcHigh(short crc) {
		byte val = (byte) (((crc >> 7) & 0x7f) + 34);
		return val;
	}
	private static byte crcLow(short crc) {
		byte val = (byte) ((crc & 0x7f) + 34);
		return val;
	}
	//--------below method are convenient for GUI event--------//
	
	private static class PanFilm extends PanBase{
		private DevSQM160 dev;
		public PanFilm(DevSQM160 device){
			dev = device;
		}
		private TextField[] box = {
			new JFXTextField(), new JFXTextField(),
			new JFXTextField(), new JFXTextField(),
			new JFXTextField(), new JFXTextField(),
			new JFXTextField(),
		};
		@Override
		public Node eventLayout(PanBase self) {
			
			final GridPane lay0 = new GridPane();
			lay0.setStyle("-fx-hgap: 7px; -fx-vgap: 7px;");			
			lay0.addRow(0, new Label("名稱"), new Label("最終厚度"));
			lay0.addRow(1, box[0], box[1]);
			lay0.addRow(2, new Label("密度(g/cm³)"), new Label("厚度紀錄點"));
			lay0.addRow(3, box[2], box[3]);
			lay0.addRow(4, new Label("Tooling(%)" ), new Label("時間紀錄點"));
			lay0.addRow(5, box[4], box[5]);
			lay0.addRow(6, new Label("Z-Ratio"));
			lay0.addRow(7, box[6]);

			final Button btn1 = PanBase.genButton1("更新",null);
			btn1.setMaxWidth(Double.MAX_VALUE);
			btn1.setOnAction(e->dev.queryFilm(box));			
			final Button btn2 = PanBase.genButton1("套用",null);			
			btn2.setMaxWidth(Double.MAX_VALUE);
			btn2.setOnAction(e->dev.updateFilm(
				0, 2,
				box[0].getText(),
				box[1].getText(),
				box[2].getText(),
				box[3].getText(),
				box[4].getText(),
				box[5].getText(),
				box[6].getText()
			));
			HBox.setHgrow(btn1, Priority.ALWAYS);
			HBox.setHgrow(btn2, Priority.ALWAYS);			
			final HBox lay1 = new HBox();
			lay1.setStyle("-fx-spacing: 7;");
			lay1.getChildren().addAll(btn1, btn2);
			final VBox lay2 = new VBox();
			lay2.setStyle("-fx-padding: 13; -fx-spacing: 7;");
			lay2.getChildren().addAll(lay0, lay1);
			return lay2;
		}
		@Override
		public void eventShown(Object[] args) {
			dev.queryFilm(box);
		}
	};
	
	private static class PanSys1 extends PanBase{
		private DevSQM160 dev;
		public PanSys1(DevSQM160 device){
			dev = device;
			cmbUnit.getItems().add("Å/s，kÅ");
			cmbUnit.getItems().add("nm/s，μm");
			cmbUnit.getItems().add("Hz");
			cmbUnit.getItems().add("ng/cm²/s，μg/cm²");
			cmbUnit.getSelectionModel().select(0);
		}		
		private CheckBox chkSim = new JFXCheckBox("模擬模式");
		private ComboBox<String> cmbUnit = new JFXComboBox<String>();
		private CheckBox chkRes = new JFXCheckBox("高解析度");
		private TextField[] boxVal = {
			new JFXTextField(), new JFXTextField(), 
			new JFXTextField(), new JFXTextField(), new JFXTextField(),
			new JFXTextField(), new JFXTextField(), new JFXTextField(),
		};
		@Override
		public Node eventLayout(PanBase self) {
			final GridPane lay0 = new GridPane();
			lay0.setStyle("-fx-hgap: 7px; -fx-vgap: 7px;");
			lay0.add(chkSim ,0,1,2,1);
			lay0.add(chkRes ,0,2,2,1);
			lay0.add(cmbUnit,0,3,2,1);
			lay0.addRow( 4, new Label("Time Base(sec)"), new Label("Rate Filter"));
			lay0.addRow( 5, boxVal[0]                  , boxVal[1]);
			lay0.addRow( 6, new Label("Tooling-1"), new Label("Tooling-4"));
			lay0.addRow( 7, boxVal[2]             , boxVal[5]);
			lay0.addRow( 8, new Label("Tooling-2"), new Label("Tooling-5"));
			lay0.addRow( 9, boxVal[3]             , boxVal[6]);
			lay0.addRow(10, new Label("Tooling-3"), new Label("Tooling-6"));
			lay0.addRow(11, boxVal[4]             , boxVal[7]);
			
			final Label cmbTitle = new Label("顯示單位");
			cmbTitle.setAlignment(Pos.BASELINE_CENTER);
			final HBox lay3 = new HBox();
			lay3.getChildren().addAll(cmbTitle, cmbUnit);
			
			final Button btn1 = PanBase.genButton1("更新",null);
			btn1.setMaxWidth(Double.MAX_VALUE);
			btn1.setOnAction(e->dev.querySystem1(chkSim, cmbUnit, chkRes, boxVal));		
			final Button btn2 = PanBase.genButton1("套用",null);
			btn2.setMaxWidth(Double.MAX_VALUE);
			btn2.setOnAction(e->dev.updateSystem1(
				chkSim.isSelected(), 
				cmbUnit.getSelectionModel().getSelectedIndex(), 
				chkRes.isSelected(), 
				boxVal[0].getText(),
				boxVal[1].getText(),
				boxVal[2].getText(),
				boxVal[3].getText(),
				boxVal[4].getText(),
				boxVal[5].getText(),
				boxVal[6].getText(),
				boxVal[7].getText()
			));	
			HBox.setHgrow(btn1, Priority.ALWAYS);
			HBox.setHgrow(btn2, Priority.ALWAYS);
			final HBox lay1 = new HBox();
			lay1.setStyle("-fx-spacing: 7;");
			lay1.getChildren().addAll(btn1, btn2);
			final VBox lay2 = new VBox();
			lay2.setStyle("-fx-padding: 13; -fx-spacing: 7;");
			lay2.getChildren().addAll(lay3, lay0, lay1);
			return lay2;
		}
		@Override
		public void eventShown(Object[] args) {
			dev.querySystem1(chkSim, cmbUnit, chkRes, boxVal);
		}
	};
	
	private static class PanSys2 extends PanBase{
		private DevSQM160 dev;
		public PanSys2(DevSQM160 device){
			dev = device;
		}
		private CheckBox chkEtch = new JFXCheckBox("Etch mode");
		private TextField[] boxVal = {
			new JFXTextField(), new JFXTextField(), 
			new JFXTextField(), new JFXTextField(),
			new JFXTextField(), new JFXTextField(),
		};
		@Override
		public Node eventLayout(PanBase self) {
			final GridPane lay0 = new GridPane();
			lay0.setStyle("-fx-hgap: 7px; -fx-vgap: 7px;");
			lay0.addRow( 0, new Label("最小頻率"), new Label("最大 - MHz"));
			lay0.addRow( 1, boxVal[0]            , boxVal[1]);
			lay0.addRow( 2, new Label("最小速率"), new Label("最大 - Å/s"));
			lay0.addRow( 3, boxVal[2]            , boxVal[3]);
			lay0.addRow( 4, new Label("最小厚度"), new Label("最大 - kÅ"));
			lay0.addRow( 5, boxVal[4]            , boxVal[5]);
			lay0.add(chkEtch, 0, 6, 2, 1);
			
			final Button btn1 = PanBase.genButton1("更新",null);
			btn1.setMaxWidth(Double.MAX_VALUE);
			btn1.setOnAction(e->dev.querySystem2(chkEtch,boxVal));			
			final Button btn2 = PanBase.genButton1("套用",null);
			btn2.setMaxWidth(Double.MAX_VALUE);
			btn2.setOnAction(e->dev.updateSystem2(
				chkEtch.isSelected(), 
				boxVal[0].getText(),
				boxVal[1].getText(),
				boxVal[2].getText(),
				boxVal[3].getText(),
				boxVal[4].getText(),
				boxVal[5].getText()
			));
			HBox.setHgrow(btn1, Priority.ALWAYS);
			HBox.setHgrow(btn2, Priority.ALWAYS);
			final HBox lay1 = new HBox();
			lay1.setStyle("-fx-spacing: 7;");
			lay1.getChildren().addAll(btn1, btn2);
			final VBox lay2 = new VBox();
			lay2.setStyle("-fx-padding: 13; -fx-spacing: 7;");
			lay2.getChildren().addAll(lay0, lay1);
			return lay2;
		}
		@Override
		public void eventShown(Object[] args) {
			dev.querySystem2(chkEtch,boxVal);
		}
	};
	
	public static Node gen_panel(final DevSQM160 dev){
	
		final Label[] txt = {
			new Label("平均厚度")    , new Label("："), new Label(), new Label("kÅ"),
			new Label("平均速率")    , new Label("："),	new Label(), new Label("Å/s"),
			new Label("Crystal Life"), new Label("："), new Label(), new Label(),
			
			new Label()          , new Label()    , new Label("厚度"), new Label("速率"),
			new Label("Sensor-1"), new Label("："),	new Label()      , new Label()      ,
			new Label("Sensor-2"), new Label("："),	new Label()      , new Label()      ,
			new Label("Sensor-3"), new Label("："),	new Label()      , new Label()      ,
			new Label("Sensor-4"), new Label("："),	new Label()      , new Label()      ,
			new Label("Sensor-5"), new Label("："),	new Label()      , new Label()      ,
			new Label("Sensor-6"), new Label("："),	new Label()      , new Label()      ,			
		};
		
		txt[ 2].textProperty().bind(dev.propThickness);
		txt[ 6].textProperty().bind(dev.propSwellRate);
		txt[10].textProperty().bind(dev.propCrystalLife);
		
		txt[18].textProperty().bind(dev.listThickness[0]);
		txt[22].textProperty().bind(dev.listThickness[1]);
		txt[26].textProperty().bind(dev.listThickness[2]);
		txt[30].textProperty().bind(dev.listThickness[3]);
		txt[34].textProperty().bind(dev.listThickness[4]);
		txt[38].textProperty().bind(dev.listThickness[5]);
		
		txt[19].textProperty().bind(dev.listSwellRate[0]);
		txt[23].textProperty().bind(dev.listSwellRate[1]);
		txt[27].textProperty().bind(dev.listSwellRate[2]);
		txt[31].textProperty().bind(dev.listSwellRate[3]);
		txt[35].textProperty().bind(dev.listSwellRate[4]);
		txt[39].textProperty().bind(dev.listSwellRate[5]);
		
		final GridPane lay0 = new GridPane();
		lay0.setStyle("-fx-hgap: 7px; -fx-vgap: 7px;");
		lay0.addRow(0, txt[ 0], txt[ 1], txt[ 2], txt[ 3]);
		lay0.addRow(1, txt[ 4], txt[ 5], txt[ 6], txt[ 7]);
		lay0.addRow(2, txt[ 8], txt[ 9], txt[10], txt[11]);
		lay0.add(new Separator(), 0, 3, 4, 1);
		lay0.addRow(4, txt[12], txt[13], txt[14], txt[15]);
		lay0.addRow(5, txt[16], txt[17], txt[18], txt[19]);
		lay0.addRow(6, txt[20], txt[21], txt[22], txt[23]);
		lay0.addRow(7, txt[24], txt[25], txt[26], txt[27]);
		lay0.addRow(8, txt[28], txt[29], txt[30], txt[31]);
		lay0.addRow(9, txt[32], txt[33], txt[34], txt[35]);
		lay0.addRow(10,txt[36], txt[37], txt[38], txt[39]);
		
		final VBox lay1 = new VBox();
		
		final Button btn1 = PanBase.genButton1("平均值歸零",null);
		btn1.setMaxWidth(Double.MAX_VALUE);
		btn1.setOnAction(e->dev.zeroAverage());
		
		final Button btn2 = PanBase.genButton1("計數歸零",null);
		btn2.setMaxWidth(Double.MAX_VALUE);
		btn2.setOnAction(e->dev.zeroTime());
		
		final Button btn3 = PanBase.genButton2("薄膜參數",null);
		btn3.setMaxWidth(Double.MAX_VALUE);
		btn3.setOnAction(e->new PanFilm(dev).appear((Stage)lay1.getScene().getWindow()));
		
		final Button btn4 = PanBase.genButton2("系統設定-1",null);
		btn4.setMaxWidth(Double.MAX_VALUE);
		btn4.setOnAction(e->new PanSys1(dev).appear((Stage)lay1.getScene().getWindow()));
		
		final Button btn5 = PanBase.genButton2("系統設定-2",null);
		btn5.setMaxWidth(Double.MAX_VALUE);
		btn5.setOnAction(e->new PanSys2(dev).appear((Stage)lay1.getScene().getWindow()));
		
		//final Button btnX = PanBase.genButton1("test-test",null);
		//btnX.setMaxWidth(Double.MAX_VALUE);
		//btnX.setOnAction(e->{ dev.test_method(); });
		
		lay1.setStyle("-fx-padding: 7; -fx-spacing: 7;");
		lay1.getChildren().addAll(
			lay0, 
			btn1, 
			btn2, 
			btn3, 
			btn4, 
			btn5
		);		
		return lay1;
	}
}
