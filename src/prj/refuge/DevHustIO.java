package prj.refuge;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.UtilPhysical;

/**
 * 一個很老舊的單軸控制器，組裝廠商已經倒閉了
 * @author qq
 *
 */
public class DevHustIO extends DevTTY {

	public static String[] ISOTOPE_NAME = {
		"3Ci","0.5Ci","0.05Ci",
	};//hard_code, the index is also value!!!
	private final String[] ISOTOPE_CODE = {
		"M03","M04","M05",
	};//hard_code, the index is also value!!!
	
	private int curIsotope = 2;//zero-base index

	public static final double MAX_LOCA = 5910.;//millimeter, this is dependent on machine
	//private final char DC1 = 0x11;
	private final char DC2 = 0x12;
	//private final char DC3 = 0x13;
	private final char DC4 = 0x14;
	
	public DevHustIO(){
		watcher.setCycleCount(Timeline.INDEFINITE);
	}
	
	public DevHustIO(String attr){
		this();
		connect(attr);
	}
	
	public void connect(String attr){
		if(attr.length()==0){
			attr = Gawain.prop.getProperty("DevHustIO","/dev/ttyS0,4800,7e2,x");
		}
		open(attr);
		exec_cmd("O9000","N01");//reset mechine
		exec_cmd("O9000","N001");//auto mode
		exec_cmd("O9000","N00000001");//start report
		watcher.play();//We must always monitor it!!!
	}
	
	public void disconnect(){
		watcher.pause();
		exec_cmd("O9000","N1");//stop report!!!			
		//eventTurnOff();
		close();
	}
	
	//@Override
	//protected void eventTurnOff(){	
	//	exec_cmd("O9000","N1");//stop report~~~
	//}

	public void setCurIsotope(int idx){
		if(0<=idx && idx<ISOTOPE_CODE.length){
			curIsotope = idx;
		}else{
			Misc.logw("Invalid Isotope Index(%d)", idx);
		}
	}
	
	public void radiStart(){
		radiStart(curIsotope);
	}
	
	public void radiStart(int idx){		
		if(0<=idx && idx<ISOTOPE_CODE.length){
			curIsotope = idx;
		}		
		exec_cmd("O9000","N0000111",ISOTOPE_CODE[curIsotope]);
		exec_cmd("O9005","N1");
		Misc.logv("開始照射 (%s)",ISOTOPE_NAME[curIsotope]);
	}

	public void radiStop(){		
		exec_cmd("O9005","N01");
		exec_cmd("O9000","N0000111");
		Misc.logv("停止照射");
	}
	
	public void syncRadiStop(){		
		exec_cmd("O9005","N01");
		exec_cmd("O9000","N0000111");//??? not complete ???
		Misc.delay(10*1000);//program must be cooling down~~~
		Misc.logv("停止照射");
	}
	
	public void moveToOrg(){
		exec_cmd("O9005","N001");		
	}
	
	public void moveToAbs(String mm){
		exec_cmd("O9000","N0000111","G01X"+mm);
	}
	
	public void moveToAbs(double mm){
		moveToAbs(String.format("%.4f",mm));
	}
	
	public boolean isMoving(){
		String sta = info[HEALTH];
		if(sta.indexOf("001004")>=0){
			return false;//after radiation
		}else if(sta.indexOf("001014")>=0){
			return false;//zero-position
		}else if(sta.indexOf("007004")>=0){
			return false;//before radiation
		}
		return true;
	}

	public double syncMoveTo(double mm){
		if(mm<=0){
			moveToOrg();
		}else{
			moveToAbs(mm);
		}
		for(int i=0; i<10; i++){
			do{
				Misc.delay(100);
			}while(isMoving()==true);
		}
		return Double.valueOf(info[LOCA]);
	}
	
	private void exec_cmd(String... arg){
		String txt = DC2+"%\r";
		for(String v:arg){
			char cc = v.charAt(v.length()-1);
			if(cc=='\r'){
				txt = txt+v;
			}else{
				txt = txt+v+'\r';
			}
		}
		txt = txt+'%'+DC4;
		writeTxt(txt);
	}
	
	private final int I_BIT =0;
	private final int O_BIT =1;
	private final int MEMORY=2;
	private final int F_CODE=3;
	private final int G_CODE=4;
	private final int HEALTH=5;
	private final int CUT_X =6;
	private final int CUT_Y =7;
	private final int CUT_Z =8;
	private final int M_CODE=9;
	private final int N_CODE=10;
	private final int P_CODE=11;
	private final int SPEED =12;
	private final int CUTTER=13;
	private final int LOCA  =14;
	private final int AXIS_Y=15;
	private final int LOCA_Y=16;
	private final int AXIS_Z=17;
	private final int LOCA_Z=18;
	private final int AXIS_B=19;
	private final int LOCA_B=20;
	private final int C_BIT =21;
	private final int A_BIT =22;
	private final int S_BIT =23;	
	private String info[] = new String[24];
	
	private void map_info(char tkn,String val){
		switch(tkn){
		case 'A':/*program don't exist*/break;
		case 'B': info[I_BIT ]=val; break;				
		case 'C': info[O_BIT ]=val; break;
		case 'E': info[MEMORY]=val; break;
		case 'F': info[F_CODE]=val; break;
		case 'G': info[G_CODE]=val; break;
		case 'H': info[HEALTH]=val; break;//機械狀態
		case 'I': info[CUT_X ]=val; break;
		case 'J': info[CUT_Y ]=val; break;
		case 'K': info[CUT_Z ]=val; break;
		case 'M': info[M_CODE]=val; break;
		case 'N': info[N_CODE]=val; break;
		case 'O': /* command */     break;
		case 'P': info[P_CODE]=val; break;
		case 'R': /*unknown     */  break;	
		case 'S': info[SPEED ]=val; break;
		case 'T': info[CUTTER]=val; break;
		case 'U':		
		case 'X':
			if(val.equalsIgnoreCase(info[LOCA])==false){
				//location is different!!!
				info[LOCA] = val;
				update_location();				
			}
			break;
		case 'V': info[AXIS_Y]=val; break;
		case 'Y': info[LOCA_Y]=val; break;
		case 'W': info[AXIS_Z]=val; break;
		case 'Z': info[LOCA_Z]=val; break;
		case '!': info[AXIS_B]=val; break;
		case '\'':info[LOCA_B]=val; break;
		case '#': info[C_BIT ]=val; break;
		case '$': info[A_BIT ]=val; break;
		case '&': info[S_BIT ]=val; break;
		case '<': /*unknown     */ break;
		default: 
			Misc.logw("unknown report: [%c]%s\n", tkn,val); 
			break;
		}
	}
	
	private int parse(String txt){
		//Report Example:
		//[DC2]
		//%\rO9001
		//H001004U 1000.001R 00000018\r%
		//[DC4]
		txt = txt.substring(2,txt.length()-1);//trim token(%)
		txt = txt.replaceAll("\r","").replace("\n","");
		
		//Misc.logv("==>%s", txt);//debug!!!
		
		char[] dat = txt.toCharArray();
		int[] pos = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
		int idx = 0;
		
		for(int i=0; i<dat.length-1; i++){
			if(dat[i]<'0' || '9'<dat[i]){
				if(
					dat[i]!=' ' && 
					dat[i]!='.' && 
					dat[i]!='?' && 
					dat[i]!=':' &&
					dat[i]!='+' && 
					dat[i]!='-'
				){
					pos[idx++] = i;
				}
			}
		}
		for(int i=0; i<pos.length; i++){
			if(pos[i]<0){
				break;
			}
			if(pos[i+1]>0){
				map_info(
					dat[pos[i]],
					txt.substring(pos[i]+1, pos[i+1])
				);
			}else{
				map_info(
					dat[pos[i]],
					txt.substring(pos[i]+1)
				);
			}
		}
		return 0;
	}
	//--------------------------------//
		
	private String lastReport = "";//keep all messages

	private long radiBeg = -1;
	private long radiIdx = -1;
	private long radiEnd = 0;
	private StringProperty propCounter = new SimpleStringProperty("********");		
	
	private void update_counter(){
		int off = (int)(radiIdx - radiBeg);
		int sec = off/1000;
		int msc = off%1000;
		int min = sec/60;
		sec = sec % 60;
		propCounter.set(String.format("%d：%d.%03d", min,sec,msc));
	}
	
	private StringProperty propLocation = new SimpleStringProperty("********");
	private ReadOnlyProperty<String> locaUnit = null;
	private void update_location(){
		try{
			if(info[LOCA]==null){
				return;
			}
			final double val = Double.valueOf(info[LOCA]);
			if(locaUnit==null){
				Misc.invoke(e->{
					propLocation.set(String.format("%.4fmm",val));
				});
			}else{				
				Misc.invoke(e->{
					propLocation.set(String.format("%.4f %s",
						UtilPhysical.convert(val, "mm", locaUnit.getValue()),
						locaUnit.getValue()
					));
				});
			}			
		}catch(NumberFormatException e){
			Misc.loge("Wrong format - %s", info[LOCA]);
		}
	} 
	
	private EventHandler<ActionEvent> eventWatcher = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {			
			if(radiBeg>=0){
				radiIdx = System.currentTimeMillis();
				update_counter();
				if(radiIdx>=radiEnd){
					radiStop();
					radiBeg = radiIdx = -1;//for next turn~~~~
				}				
			}
			if(isOpen()==false){
				return;
			}			
			lastReport = lastReport + readTxt();
			int beg = lastReport.indexOf(DC2);
			if(beg>=0){
				int end = lastReport.indexOf(DC4, beg+1);
				if(end>0){			
					lastReport = lastReport.substring(beg+1,end);
					parse(lastReport);
					//Misc.logv("==>%s", info[LOCA]);//debug!!!
					//Misc.logv("==>%s", lastReport);//debug!!!
					lastReport = "";//reset it~~~~
				}
			}
		}
	};
	
	private Timeline watcher = new Timeline(new KeyFrame(
		Duration.millis(150),
		eventWatcher
	));

	protected Node eventLayout(PanBase pan) {

		Label txtInfo1 = new Label("--------");
		txtInfo1.textProperty().bind(propLocation);
		Label txtInfo2 = new Label("--------");	
		txtInfo2.textProperty().bind(propCounter);
		
		GridPane.setFillWidth(txtInfo1, true);
		GridPane.setFillWidth(txtInfo2, true);
				
		JFXTextField txtValue = new JFXTextField();
		txtValue.setText("100");
		txtValue.setMaxWidth(97);
		
		JFXComboBox<String> cmbUnit = new JFXComboBox<String>();
		cmbUnit.getItems().addAll("cm","mm","μm");
		cmbUnit.setMaxWidth(100);
		cmbUnit.getSelectionModel().select(0);
		locaUnit = cmbUnit.getSelectionModel().selectedItemProperty();
		
		JFXTextField txtMin = new JFXTextField();
		txtMin.setText("00");
		txtMin.setMaxWidth(30);
		
		JFXTextField txtSec = new JFXTextField();
		txtSec.setText("10");
		txtSec.setMaxWidth(30);
		
		final ToggleGroup grpIso = new ToggleGroup();		
		final JFXRadioButton[] chkIso = {
			new JFXRadioButton(ISOTOPE_NAME[0]),
			new JFXRadioButton(ISOTOPE_NAME[1]),
			new JFXRadioButton(ISOTOPE_NAME[2])
		};
		for(int i=0; i<chkIso.length; i++){
			chkIso[i].setToggleGroup(grpIso);
			chkIso[i].setUserData(i);
		}
		grpIso.selectToggle(chkIso[curIsotope]);
		
		final GridPane lay2 = new GridPane();//show all sensor
		lay2.getStyleClass().add("grid-medium");
		lay2.add(new Label("距離"), 0, 0); lay2.add(txtInfo1, 1, 0, 2, 1);
		lay2.add(new Label("計時"), 0, 1); lay2.add(txtInfo2, 1, 1, 2, 1); 
		lay2.add(new Label("射源"), 0, 2); lay2.add(chkIso[0], 1, 2, 3, 1);
		lay2.add(chkIso[1], 1, 3, 3, 1);
		lay2.add(chkIso[2], 1, 4, 3, 1);		
		lay2.add(new Label("移動距離"), 0, 5); lay2.add(txtValue, 1, 5, 3, 1);		
		lay2.add(new Label("移動單位"), 0, 6); lay2.add(cmbUnit , 1, 6, 3, 1);
		lay2.add(new Label("照射時間"), 0, 7); lay2.add(txtMin , 1, 7); lay2.add(new Label("："), 2, 7); lay2.add(txtSec , 3, 7);
		
		final Button btnMove = PanBase.genButton2("移動","dir-right.png");
		btnMove.setOnAction(event->{
			String val = txtValue.getText();
			String unt = cmbUnit.getSelectionModel().getSelectedItem();
			double mm = UtilPhysical.convert(val+unt, "mm");
			if(mm>MAX_LOCA){				
				Misc.loge("超過機械限制");
				return;
			}
			moveToAbs(mm);
		});
		final Button btnZero = PanBase.genButton2("歸零","clock-ccw.png");
		btnZero.setOnAction(event->{
			moveToOrg();
		});
		final Button btnRadi = PanBase.genButton2("照射","lightbulb-on-outline.png");
		btnRadi.setOnAction(event->{
			if(radiBeg>=0){
				return;
			}
			curIsotope = (int)(grpIso.getSelectedToggle().getUserData());
			int min = Misc.txt2int(txtMin.getText());
			int sec = Misc.txt2int(txtSec.getText());
			radiBeg = radiIdx = System.currentTimeMillis();			
			radiEnd = radiBeg + (long)((60*min+sec)*1000);//the unit is milli-second
			radiStart();
		});
		final Button btnStop = PanBase.genButton2("停止","lightbulb.png");
		btnStop.setOnAction(event->{			
			radiStop();
			update_counter();
			radiBeg = radiIdx = -1L;
		});
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-one-dir");		
		lay1.getChildren().addAll(
			btnMove,
			btnZero,
			btnRadi,
			btnStop
		);
		lay2.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 10);
		lay2.add(lay1, 5, 0, 4, 10);
		return lay2;
	}
}
