package prj.letterpress;

import com.jfoenix.controls.JFXCheckBox;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.BoxIntValue;
import narl.itrc.DevMotion;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * This is just a wrap object for GALIL DMC-B140-M motion card.<p>
 * It is a very old device and use DMC(Digital Motion Controller) code to program.<p>
 * It has a high-level, interpreted language parser like GCode.<p>
 * This version only supports serial port to address communication.<p>
 * Attention!!. colon sign ';' will not echo message to terminal screen
 * @author qq
 *
 */
public class DevB140M extends DevMotion {
	
	private final String TAIL="\r\n:";
	
	private DevTTY tty = new DevTTY(); 
	
	public DevB140M(){
		this("/dev/ttyS0,115200,8n1");//this is hard code!!!
	}
	
	public DevB140M(String tty_name){
		tty.open(tty_name);
		//watch();
		//The DP command is useful to redefine the absolute position.
	}

	public void parse_cmd(String cmd,BoxIntValue[] box){
		String txt = exec(cmd);
		String[] arg = txt.split(",");
		for(int i=0; i<arg.length; i++){
			box[i].setInteger(arg[0]);
		}
	}
	
	public String exec(String cmd){
		String txt = tty.fetch(cmd,TAIL);
		//System.out.format("%s GOT %s",cmd,txt);
		int pos = txt.indexOf("\r\n");
		if(pos>=0){
			//trip the first END~~
			txt = txt.substring(pos).trim();
		}
		txt =txt.replace("\r\n","").replace(":","").trim();//check again~~~
		return txt;
	}

	public void parse_TP(String txt){
		int pos = txt.indexOf("TP");
		if(pos>=0){
			//not sure why 'TP' is disappear~~~
			txt = txt.substring(pos+2);
		}
		txt =txt.replace("\r\n","").replace(":","").trim();
		int[] val = {0,0,0,0};
		Misc.logv("got counter="+txt);
		String[] arg = txt.split(",");
		try{
			val[0] = Integer.valueOf(arg[0]);
			val[1] = Integer.valueOf(arg[1]);
			val[2] = Integer.valueOf(arg[2]);
			val[3] = Integer.valueOf(arg[3]);
		}catch(NumberFormatException e){			
		}
		makePosition(val);
	}
	
	/**
	 * set output-bit, open-loop means "connected". close-loop means "disconnected".<p>
	 * @param id - 1~4
	 * @param open - true:open loop, false:close loop
	 */
	public void out(int id,boolean open){
		id = id + 1;//1-base
		int op = (open==true)?(0):(1);		
		tty.fetch(String.format("OB %d,%d\r",id,op),TAIL);
	}
	public void out1(boolean open){ out(0,open); }
	public void out2(boolean open){ out(1,open); }
	public void out3(boolean open){ out(2,open); }
	public void out4(boolean open){ out(3,open); }
	
	@Override
	protected void makeMotion(boolean abs,Double[] value) {
		String cmd;
		if(abs==true){
			cmd = "PA ";
		}else{
			cmd = "PR ";
		}
		for(int i=0; i<value.length; i++){
			if(value[i]!=null){
				cmd = cmd + String.format("%d, ",value[i].intValue());
			}else{
				cmd = cmd + ", ";
			}
		}
		parse_TP(tty.fetch(cmd+";BG;MC;TP\r",TAIL));
	}

	@Override
	public void takePosition(Double[] value) {
		String txt = "DE ";
		for(int i=0; i<value.length; i++){
			if(value[i]==null){
				txt = txt + ",";				
			}else{
				txt = txt + value[i].intValue() + ",";
			}
		}
		txt = tty.fetch(txt+";TP\r",TAIL);
		parse_TP(txt);
	}
	//----------------------------------//
	
	public void watch(){
		if(tty.isLive()==false){
			return;
		}
		//String txt = exec("MG TIME\r");
		//The command "CW?\r" will send the message "...Galil Motion Control..."
		String[] arg = exec("SP ?,?,?,?\r").split(",");
		for(int i=0; i<4; i++){
			panAxis[i].boxSSpeed.setInteger(arg[i]);
		}
		arg = exec("AC ?,?,?,?\r").split(",");
		for(int i=0; i<4; i++){
			panAxis[i].boxASpeed.setInteger(arg[i]);
		}
		arg = exec("DC ?,?,?,?\r").split(",");
		for(int i=0; i<4; i++){
			panAxis[i].boxDSpeed.setInteger(arg[i]);
		}
		
		parse_TP(tty.fetch("TP\r",TAIL));
	}
	
	class AxisInfo extends GridPane {
		private char tkn;
		public AxisInfo(char token){
			tkn = token;
			getStyleClass().add("grid-small");
			initLayout();
		}
		
		public JFXCheckBox chkOut;
		public BoxIntValue boxSSpeed,boxASpeed,boxDSpeed;
		public BoxIntValue boxCounter;
		
		private void updateSPD(String cmd,BoxIntValue box){
			int val = box.propValue.get();
			int idx = tkn2idx();
			String tmp = null;
			//write value
			switch(idx){
			case 0: tmp=String.format("%s %d\r",cmd,val); break;
			case 1: tmp=String.format("%s ,%d\r",cmd,val); break;
			case 2: tmp=String.format("%s ,,%d\r",cmd,val); break;
			case 3: tmp=String.format("%s ,,,%d\r",cmd,val); break;
			default: return;
			}
			tty.fetch(tmp,TAIL);
			//reload value
			switch(idx){
			case 0: tmp=cmd+" ?\r"; break;
			case 1: tmp=cmd+" ,?\r"; break;
			case 2: tmp=cmd+" ,,?\r"; break;
			case 3: tmp=cmd+" ,,,?\r"; break;
			default: return;
			}
			tmp = exec(tmp);//reload
			box.setInteger(tmp);
		}
		
		private void initLayout(){
			chkOut = new JFXCheckBox("OUT-"+tkn);
			chkOut.setOnAction(event->out(tkn2idx(),chkOut.isSelected()));
			chkOut.setMaxWidth(Double.MAX_VALUE);			
			GridPane.setHgrow(chkOut,Priority.ALWAYS);
						
			boxSSpeed = new BoxIntValue("Slew Speed-"+tkn)
				.setEvent(event->updateSPD("SP",boxSSpeed));
			
			boxASpeed = new BoxIntValue("Acceleration-"+tkn)
				.setEvent(event->updateSPD("AC",boxASpeed));
			
			boxDSpeed = new BoxIntValue("Deceleration-"+tkn)
				.setEvent(event->updateSPD("DC",boxDSpeed));
			
			boxCounter = new BoxIntValue("Counter-"+tkn).setEvent(event->{
				Double[] val = {null,null,null,null};
				val[tkn2idx()] = new Double(boxCounter.propValue.get());
				takePosition(val);
			});
			
			HBox pan1 = new HBox();
			pan1.getStyleClass().add("hbox-small");
			
			BoxIntValue boxOffset = new BoxIntValue("步伐",5000);
			
			Button btnPos = new Button("＋");
			btnPos.setOnAction(event->{
				Double[] val = {null,null,null,null};
				val[tkn2idx()] = (double)boxOffset.propValue.get();
				moveTo(val);
			});	
			Button btnNeg = new Button("－");
			btnNeg.setOnAction(event->{
				Double[] val = {null,null,null,null};
				val[tkn2idx()] = (double)(-1*boxOffset.propValue.get());
				moveTo(val);
			});	
			
			pan1.getChildren().addAll(btnNeg,boxOffset,btnPos);
			pan1.setMaxWidth(Double.MAX_VALUE);			
			GridPane.setHgrow(pan1,Priority.ALWAYS);
			
			add(chkOut,0,0,3,1);
			addRow(1,new Label("脈衝/秒"),new Label("："),boxSSpeed);
			addRow(2,new Label("加速度"),new Label("："),boxASpeed);
			addRow(3,new Label("減速度"),new Label("："),boxDSpeed);
			addRow(4,new Label("計數器"),new Label("："),boxCounter);
			add(pan1,0,5,3,1);
			
			pulse[tkn2idx()].bindBidirectional(boxCounter.propValue);
		}
		
		private int tkn2idx(){
			switch(tkn){
			case '0': case 'a': case 'A': return 0;
			case '1': case 'b': case 'B': return 1;
			case '2': case 'c': case 'C': return 2;
			case '3': case 'd': case 'D': return 3;
			}
			return 0;
		}
	};
	
	private AxisInfo[] panAxis = {
		new AxisInfo('A'),
		new AxisInfo('B'),
		new AxisInfo('C'),
		new AxisInfo('D'),
	};
	
	public Node layoutConsole(){
		
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("hbox-small");
		//----------------------------//
		
		GridPane pan0 = new GridPane(); 
		pan0.getStyleClass().add("grid-small");
		
		final Button btnName = new Button("dev-name");
		//btnName.setMaxWidth(Double.MAX_VALUE);
		btnName.textProperty().bind(tty.ctrlName);
		btnName.setOnAction(event->{
		});
		//GridPane.setHgrow(btnName,Priority.ALWAYS);
		
		final Button btnWatch = new Button("Watch");
		btnWatch.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(btnWatch,Priority.ALWAYS);
		btnWatch.setOnAction(event->watch());
		
		pan0.addRow(0,new Label("連接埠"),new Label("："),btnName);
		pan0.add(btnWatch, 0, 1, 3, 1);
		//----------------------------//
		
		HBox pan1 = new HBox();
		pan1.getStyleClass().add("hbox-small");		
		pan1.getChildren().addAll(
			PanBase.decorate("A 軸",panAxis[0]),	
			PanBase.decorate("B 軸",panAxis[1]),
			PanBase.decorate("C 軸",panAxis[2]),
			PanBase.decorate("D 軸",panAxis[3])
		);
		lay0.getChildren().addAll(pan0,pan1);
		return lay0;
	}	
}