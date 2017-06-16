package prj.refuge;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * 一個很老舊的單軸控制器，組裝廠商已經倒閉了
 * @author qq
 *
 */
public class DevHustIO extends DevTTY {

	public static String[] ISOTOPE_NAME = {
		"0.05Ci", "0.5Ci", "3Ci"
	};//hard_code, the index is also value!!!
	private final String[] ISOTOPE_CODE = {
		"M05", "M04", "M03"
	};//hard_code, the index is also value!!!
	
	public static final double MAX_LOCA = 5910.;//millimeter, this is dependent on mechine
	//private final char DC1 = 0x11;
	private final char DC2 = 0x12;
	//private final char DC3 = 0x13;
	private final char DC4 = 0x14;
	
	public DevHustIO(){		
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
		exec_cmd("O9000","N00000010000000001");//start report
		watcher.play();//We must always monitor it!!!
	}
	
	public void disconnect(){
		watcher.pause();
		eventTurnOff();
		close();
	}
	
	@Override
	protected void eventTurnOff(){	
		exec_cmd("O9000","N1");//stop report~~~
	}
		
	private int curIsotope = 0;

	public void radiStart(int code){		
		if(0<=code && code<ISOTOPE_CODE.length){
			curIsotope = code;
		}
		exec_cmd("O9000","N0000111",ISOTOPE_CODE[curIsotope]);
		exec_cmd("O9005","N10000000");
	}

	public void radiStop(){
		exec_cmd("O9005","N01000000");
	}
	
	public void moveToOrg(){
		/*waitfor(-1);
		double loca = Double.parseDouble(reg[LOCA]);
		if(loca<=0.1){
			return;//check again!!!
		}
		exec_cmd("O9005","N00100000");		
		waitfor(0);
		Misc.delay(7000);*/
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
	private final int CUT_X =5;
	private final int CUT_Y =6;
	private final int CUT_Z =7;
	private final int M_CODE=8;
	private final int N_CODE=9;
	private final int P_CODE=10;
	private final int SPEED =11;
	private final int CUTTER=12;
	private final int LOCA  =13;
	private final int AXIS_Y=14;
	private final int LOCA_Y=15;
	private final int AXIS_Z=16;
	private final int LOCA_Z=17;
	private final int AXIS_B=18;
	private final int LOCA_B=19;
	private final int C_BIT =20;
	private final int A_BIT =21;
	private final int S_BIT =22;
	
	private String info[] = new String[23];
	
	private void map_info(char sig,String val){
		switch(sig){
		case 'A':/*program don't exist*/break;
		case 'B': info[I_BIT ]=val; break;				
		case 'C': info[O_BIT ]=val; break;
		case 'E': info[MEMORY]=val; break;
		case 'F': info[F_CODE]=val; break;
		case 'G': info[G_CODE]=val; break;		
		case 'I': info[CUT_X ]=val; break;
		case 'J': info[CUT_Y ]=val; break;
		case 'K': info[CUT_Z ]=val; break;
		case 'M': info[M_CODE]=val; break;
		case 'N': info[N_CODE]=val; break;
		case 'O': /* command */     break;
		case 'P': info[P_CODE]=val; break;
		case 'R': /*undocumented*/  break;				
		case 'S': info[SPEED ]=val; break;
		case 'T': info[CUTTER]=val; break;
		case 'U':
		case 'X': info[LOCA  ]=val; break;
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
			Misc.logw("unknown report: [%c]-%s\n", sig,val); 
			break;
		}
	}
	
	private int parse(String txt){
		//Report Example:
		//[DC2]
		//%O9001
		//H001004U 1000.001R 00000018%
		//[DC4]
		if(txt.length()==0){
			return -1;
		}
		txt = txt.substring(2,txt.length()-1);//trim token(%)
		
		int pos = txt.indexOf('\r');
		if(pos>=0){
			//trip 'O900x'
			txt = txt.substring(pos+1);
		}
		//cut header!! DDHHHH		
		if(txt.length()==0){
			return -1;
		}
		pos = txt.indexOf('U');
		if(pos<0){
			return -1;
		}
		txt = txt.substring(pos+1);
		pos = txt.indexOf('R');
		if(pos<0){
			return -1;
		}
		txt = txt.substring(0,pos);
		map_info('U',txt.trim());
		return 0;
	}
	//--------------------------------//
		
	private String lastReport = "";//keep all messages
	
	private EventHandler<ActionEvent> eventWatcher = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			if(isOpen()==false){
				return;
			}
			lastReport = lastReport + readTxt();
			int beg = lastReport.indexOf(DC2);
			if(beg>=0){
				int end = lastReport.indexOf(DC4, beg);
				if(end>0){					
					//parse(lastReport.substring(beg, end));
					Misc.logv("==>", lastReport);//debug!!!
					lastReport = lastReport.substring(end+1);
				}
			}
		}
	};
	
	private Timeline watcher = new Timeline(new KeyFrame(
		Duration.millis(150),
		eventWatcher
	));
	
	private final String TXT_RAD_START = "開始照射";
	private final String TXT_RAD_STOP  = "開始照射";
	
	@Override
	protected Node eventLayout(PanBase pan) {
		
		final GridPane root = new GridPane();//show all sensor
		root.getStyleClass().add("grid-medium");

		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-one-dir");
		final Button btnTest = PanBase.genButton2("開始照射",null);
		//btnTest.textProperty().bind(observable);
		btnTest.setOnAction(event->{
		});
		final Button btnLoad = PanBase.genButton2("歸零",null);
		btnLoad.setOnAction(event->{
		});
		final Button btnSave = PanBase.genButton2("移動",null);
		btnSave.setOnAction(event->{
		});
		//lay1.getChildren().addAll(btnTest,btnLoad,btnSave,btnComp,btnVolt,btnMeas);
		
		root.add(new Separator(Orientation.VERTICAL), 2, 0, 1, 10);
		root.add(lay1, 3, 0, 4, 10);
		return root;
	}
}
