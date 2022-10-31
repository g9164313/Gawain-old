package prj.sputter.labor1;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;

public class StepKindler extends StepCommon {
		
	private final String action_name = "高壓設定";
	
	public final static String TAG_KINDLE = "點火";
	
	public StepKindler(){	
		set(op1,
			shutter_close,
			spik_running, run_waiting(1000,msg[2]),
			turn_on, turn_on_wait
		);
	}
	//rad[0].setDisable(true);
	//rad[0].setStyle("-fx-opacity: 1");

	final TextField[] args = {
		new TextField("100"),//regulator value
		new TextField("5"  ),//ramp time
		new TextField("30" ),//stable time
	};
	
	final Runnable op1 = ()->{
		show_mesg(TAG_KINDLE);
		try{
			dcg_power = Integer.valueOf(args[0].getText());
			args[0].getStyleClass().remove("error");
		}catch(NumberFormatException e) {
			args[0].getStyleClass().add("error");
			abort_step();
			return;
		}
		try{
			dcg_t_rise = (int)Misc.text2tick(args[1].getText());
			args[1].getStyleClass().remove("error");
		}catch(NumberFormatException e) {
			args[1].getStyleClass().add("error");
			abort_step();
			return;
		}
		try{
			dcg_t_stable = (int)Misc.text2tick(args[2].getText());
			args[2].getStyleClass().remove("error");
		}catch(NumberFormatException e) {
			args[2].getStyleClass().add("error");
			abort_step();
			return;
		}
		next_step();
	};
	
	@Override
	public Node getContent(){
		show_mesg(action_name);
		
		for(TextField box:args){
			box.setMaxWidth(83);
		}
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad","font-console");
		lay.addColumn(0, msg[0]);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.addRow(0, 
			new Label("輸出功率"), args[0],
			new Label("爬升時間"), args[1],
			new Label("穩定時間"), args[2],
			msg[1]
		);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	
	private static final String TAG0 = "option";
	private static final String TAG1 = "ramp";
	private static final String TAG2 = "value";
	private static final String TAG3 = "time";
	
	@Override
	public String flatten() {
		//trick, replace time format.
		//EX: mm:ss --> mm#ss
		return String.format(
			"%s:%d-%d, %s:%s, %s:%s, %s:%s",
			TAG0, 0, 0,
			TAG1, args[1].getText().trim().replace(':','.'),
			TAG2, args[0].getText().trim(),
			TAG3, args[2].getText().trim().replace(':','.')
		);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\p{Space}]+[:]\\p{ASCII}*[,\\s]?)+")==false){
			Misc.loge("pasing fail-->%s",txt);
			return;
		}
		//trick, replace time format.
		//EX: mm#ss --> mm:ss
		String[] col = txt.split(":|,");
		for(int i=0; i<col.length; i+=2){
			final String tag = col[i+0].trim();
			final String val = col[i+1].trim();
			if(tag.equals(TAG1)==true){
				args[1].setText(val.replace('.',':'));
			}else if(tag.equals(TAG2)==true){
				args[0].setText(val);
			}else if(tag.equals(TAG3)==true){
				args[2].setText(val.replace('.',':'));
			}
		}
	}
	//----------------------------
	
	/*private void exec(final String... lst) {
		for(String cmd:lst) {
			if(exec(cmd)==false) { return; }
		}
	}
	private boolean exec(final String cmd) {
		if(cmd==null) {
			return true;
		}
		if(cmd.length()==0) {
			return true;
		}
		boolean res = dcg1.exec(cmd).endsWith("*");
		if(res==false) {
			String _txt = cmd + "設定失效!!";
			abort_step();
			Misc.logv(_txt);
			Application.invokeLater(()->PanBase.notifyError("",_txt));
		}
		try {
			TimeUnit.MILLISECONDS.sleep(100);
		} catch (Exception e) {
			//for next command~~~
		}
		return res;		
	}*/
}
