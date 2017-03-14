package prj.letterpress;

import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import narl.itrc.DevMotion;
import narl.itrc.Misc;
import narl.itrc.PanDecorate;

public class PanMotion1 extends PanDecorate {

	public PanMotion1(){
		super("XY Stage Control");
	}
		
	@Override
	public Node eventLayout() {
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-small");
		
		Label[] lcd = new Label[3];
		for(int i=0; i<lcd.length; i++){
			lcd[i] = new Label();
			lcd[i].textProperty().bind(Entry.stg0.pulse[i].divide(50).asString("%8d"));
			lcd[i].setPrefWidth(80);
			lcd[i].setAlignment(Pos.BASELINE_RIGHT);
		}//the difference between encoder and motor is 50!!!  

		final TextField[] box = {
			new TextField("0"),
			new TextField("0"),
			new TextField("0")
		};
		final Button[] btn = {
			new Button(DIR_NEG1),new Button(DIR_POS1),
			new Button(DIR_NEG1),new Button(DIR_POS1),
			new Button(DIR_NEG1),new Button(DIR_POS1),
			new Button(DIR_ZERO),
			new Button(DIR_ZERO),
			new Button(DIR_ZERO)
		};
		
		for(int i=0; i<6; i++){
			int id = i/2;
			char tkn = '?';
			switch(id){
			case 0: tkn = 'x'; break;
			case 1: tkn = 'y'; break;
			case 2: tkn = '@'; break;
			}
			char dir = (i%2==0)?('-'):('+');
			EventKick event = new EventKick(tkn,dir,box[id],btn[i]);
			btn[i].addEventFilter(MouseEvent.MOUSE_PRESSED,event);
			btn[i].addEventFilter(MouseEvent.MOUSE_RELEASED,event);
			box[id].setPrefWidth(80);
		}
		
		btn[6].setOnAction(event->{
			Entry.stg0.exec("DE ,0;DP ,0;\r\n"); 
			Entry.stg0.exec_TP();
		});
		btn[7].setOnAction(event->{
			Entry.stg0.exec("DE 0;DP 0;\r\n"); 
			Entry.stg0.exec_TP();
		});
		btn[8].setOnAction(event->{
			Entry.stg0.exec("DE ,,0;DP ,,0;\r\n"); 
			Entry.stg0.exec_TP();
		});
				
		lay.addRow(0, new Label("X軸"),lcd[0],btn[0],box[0],btn[1],btn[6]);
		lay.addRow(1, new Label("Y軸"),lcd[1],btn[2],box[1],btn[3],btn[7]);
		lay.addRow(2, new Label("θ軸"),lcd[2],btn[4],box[2],btn[5],btn[8]);
		return lay;
	}


	private static final String DIR_POS1="  >";
	private static final String DIR_POS2=">>";
	
	private static final String DIR_NEG1="<  ";
	private static final String DIR_NEG2="<<";
	
	private static final String DIR_ZERO="RST";
	
	class EventKick implements EventHandler<MouseEvent>{
		private char dir = '+';
		private char tkn = '?';		
		private TextField box;
		private Button btn;
		public EventKick(char tkn,char dir,TextField box,Button btn){
			this.tkn = tkn;
			this.dir = dir;
			this.box = box;
			this.btn = btn;
		}
		@Override
		public void handle(MouseEvent event) {
			int val = 0;
			String txt = box.getText().trim();
			try{
				val = Integer.valueOf(txt);
			}catch(NumberFormatException e){
				Misc.loge("必須是整數 --> "+txt);
				return;
			}
			if(dir=='-'){
				val = val * -1;
			}
			EventType<?> typ = event.getEventType();
			if(typ==MouseEvent.MOUSE_PRESSED){
				btn.setText((dir=='+')?(DIR_POS2):(DIR_NEG2));
				if(val==0){
					jogging(true,dir);
				}else{
					moving(val);
				}
			}else if(typ==MouseEvent.MOUSE_RELEASED){
				btn.setText((dir=='+')?(DIR_POS1):(DIR_NEG1));
				if(val==0){
					jogging(false,dir);
				}
			}
		}
		private void jogging(boolean go,char dir){
			double val = (dir=='+')?(2000):(-2000);
			switch(tkn){
			case 'x':
			case 'X':
				Entry.stg0.joggingTo(go,DevMotion.PULSE_UNIT, val);
				break;
			case 'y':
			case 'Y':
				Entry.stg0.joggingTo(go,DevMotion.PULSE_UNIT, null, val);
				break;
			case '@':
				val = val * 10.;//special~~~
				Entry.stg0.joggingTo(go,DevMotion.PULSE_UNIT, null, null, null, val);
				break;				
			}
		}
				
		private void moving(int val){
			//Misc.logv("move %c%c (%d)",dir,tkn,val);
			switch(tkn){
			case 'x':
			case 'X':
				Entry.stg0.asyncMoveTo(DevMotion.PULSE_UNIT,(double)val);
				break;
			case 'y':
			case 'Y':
				Entry.stg0.asyncMoveTo(DevMotion.PULSE_UNIT,null,(double)val);
				break;
			case '@':
				Entry.stg0.asyncMoveTo(DevMotion.PULSE_UNIT,null,null,null,(double)val);
				break;				
			}
		}		
	};
}
