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
import narl.itrc.PanBase;

/**
 * This is just a wrap object for GALIL DMC-B140-M motion card.<p>
 * It is a very old device and use DMC(Digital Motion Controller) code to program.<p>
 * It has a high-level, interpreted language parser like GCode.<p>
 * This version only supports serial port to address communication.<p>
 * @author qq
 *
 */
public class DevB140M extends DevMotion {
	
	private DevTTY tty = new DevTTY(); 
	
	public DevB140M(){
		tty.open("/dev/ttyS0,115200,8n1");//this is hard code!!!
		//watch();
	}
	
	public DevB140M(String tty_name){
		tty.open(tty_name);
		//watch();
		//The DP command is useful to redefine the absolute position.
	}
	
	/**
	 * Send command to device and wait for response. 
	 * @param cmd - command 
	 * @return
	 */
	public String exec(String cmd){
		//Misc.logv("exec -> "+cmd);
		String txt = tty.fetch(cmd,':');
		int pos = txt.indexOf('\n');
		if(pos<0){
			return txt;
		}
		txt = txt.substring(pos+1);//strip "echo" message, but why???
		pos = txt.indexOf('\n');
		if(pos<0){
			return txt;
		}
		return txt.substring(0,pos-1);
	}
		
	private void watch(){
		if(tty.isLive()==false){
			return;
		}
		//String txt = exec("MG TIME\r");
		//The command "CW?\r" will send the message "...Galil Motion Control..."
		setBox(exec("SP ?,?,?,?\r"),boxSSpd);
		setBox(exec("AC ?,?,?,?\r"),boxASpd);
		setBox(exec("DC ?,?,?,?\r"),boxDSpd);
		setTxt(exec("TP\r"),txtCount);
	}
	
	private void setBox(String msg,BoxIntValue[] box){
		String[] arg = msg.split(",");
		for(int i=0; i<arg.length; i++){
			box[i].setValue(arg[i].trim());
		}
	}
	
	private void setTxt(String msg,Label[] txt){
		String[] arg = msg.split(",");
		for(int i=0; i<arg.length; i++){
			txt[i].setText(arg[i].trim());
		}
	}
	
	public int[] getCounter(){
		int[] pos = {0,0,0,0};
		String txt = exec("TP\r");
		String[] val = txt.split(",");
		try{
			pos[0] = Integer.valueOf(val[0]);
			pos[1] = Integer.valueOf(val[1]);
			pos[2] = Integer.valueOf(val[2]);
			pos[3] = Integer.valueOf(val[3]);
		}catch(NumberFormatException e){			
		}
		return pos;
	}
	
	/**
	 * set output-bit, open-loop means "connected". close-loop means "disconnected".<p>
	 * @param id - 1~4
	 * @param open - true:open loop, false:close loop
	 */
	public void out(int id,boolean open){
		int op = (open==true)?(0):(1);
		exec(String.format("OB %d,%d\r",id,op));
	}
	public void out1(boolean open){ out(1,open); }
	public void out2(boolean open){ out(2,open); }
	public void out3(boolean open){ out(3,open); }
	public void out4(boolean open){ out(4,open); }
	
	private void setSpeed(char typ,int idx){
		BoxIntValue[] box = null;
		String cmd=null,tmp=null;
		switch(typ){
		case 's':
		case 'S':
			cmd = "SP ";
			box = boxSSpd;
			break;
		case 'a':
		case 'A':
			cmd = "AC ";
			box = boxASpd;
			break;
		case 'd':
		case 'D':
			cmd = "DC ";
			box = boxDSpd;
			break;
		default:
			return;
		}
		int val = box[idx].getValue();
		switch(idx){
		case 0: tmp=String.format("%s%d\r",cmd,val); break;
		case 1: tmp=String.format("%s,%d\r",cmd,val); break;
		case 2: tmp=String.format("%s,,%d\r",cmd,val); break;
		case 3: tmp=String.format("%s,,,%d\r",cmd,val); break;
		default: return;
		}		
		exec(tmp);//set value
		
		switch(idx){
		case 0: tmp=cmd+"?\r"; break;
		case 1: tmp=cmd+",?\r"; break;
		case 2: tmp=cmd+",,?\r"; break;
		case 3: tmp=cmd+",,,?\r"; break;
		default: return;
		}
		tmp = exec(tmp);//reload
		box[idx].setValue(tmp);
		//Misc.logv("reload:"+tmp);
	}

	@Override
	public void mapping(boolean abs,Double[] value) {
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
		cmd = cmd.substring(0, cmd.length()-2);
		cmd = cmd + ";BG;MC;\r";	
		exec(cmd);
	}
	//----------------------------------//
	
	private JFXCheckBox chkOut[]={
		new JFXCheckBox("OUT1"),
		new JFXCheckBox("OUT2"),
		new JFXCheckBox("OUT3"),
		new JFXCheckBox("OUT4")
	};
	
	private BoxIntValue boxSSpd[]={
		new BoxIntValue("A-Slew Speed").setEventEnter(event->setSpeed('s',0)),
		new BoxIntValue("B-Slew Speed").setEventEnter(event->setSpeed('s',1)),
		new BoxIntValue("C-Slew Speed").setEventEnter(event->setSpeed('s',2)),
		new BoxIntValue("D-Slew Speed").setEventEnter(event->setSpeed('s',3))
	};
	
	private BoxIntValue boxASpd[]={
		new BoxIntValue("A-Acceleration").setEventEnter(event->setSpeed('a',0)),
		new BoxIntValue("B-Acceleration").setEventEnter(event->setSpeed('a',1)),
		new BoxIntValue("C-Acceleration").setEventEnter(event->setSpeed('a',2)),
		new BoxIntValue("D-Acceleration").setEventEnter(event->setSpeed('a',3))
	};
	
	private BoxIntValue boxDSpd[]={
		new BoxIntValue("A-Deceleration").setEventEnter(event->setSpeed('d',0)),
		new BoxIntValue("B-Deceleration").setEventEnter(event->setSpeed('d',1)),
		new BoxIntValue("C-Deceleration").setEventEnter(event->setSpeed('d',2)),
		new BoxIntValue("D-Deceleration").setEventEnter(event->setSpeed('d',3))
	};
	
	private final String NON_COUNT="--------";
	private Label txtCount[]={
		new Label(NON_COUNT),
		new Label(NON_COUNT),
		new Label(NON_COUNT),
		new Label(NON_COUNT)
	};
	
	private Node layoutAxisInfo(String title,int idx){
		GridPane root = new GridPane(); 
		root.getStyleClass().add("grid-small");
		
		chkOut[idx].setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(chkOut[idx],Priority.ALWAYS);
		
		root.add(chkOut[idx],0,0,3,1);
		root.addRow(1,new Label("速度(脈衝/秒)"),new Label("："),boxSSpd[idx]);
		root.addRow(2,new Label("加速度"),new Label("："),boxASpd[idx]);
		root.addRow(3,new Label("減速度"),new Label("："),boxDSpd[idx]);
		root.addRow(4,new Label("計數器"),new Label("："),txtCount[idx]);
		
		return PanBase.decorate(title,root);
	}
	
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
		
		chkOut[0].setOnAction(event->out1(chkOut[0].isSelected()));
		chkOut[1].setOnAction(event->out2(chkOut[1].isSelected()));
		chkOut[2].setOnAction(event->out3(chkOut[2].isSelected()));
		chkOut[3].setOnAction(event->out4(chkOut[3].isSelected()));
		
		pan1.getChildren().addAll(
			layoutAxisInfo("AXIS-A",0),
			layoutAxisInfo("AXIS-B",1),
			layoutAxisInfo("AXIS-C",2),
			layoutAxisInfo("AXIS-D",3)
		);
		lay0.getChildren().addAll(pan0,pan1);
		return lay0;
	}	
}
