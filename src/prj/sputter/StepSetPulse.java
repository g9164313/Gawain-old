package prj.sputter;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class StepSetPulse extends Stepper {

	private DevSPIK2k spk;
	
	public StepSetPulse(
		final DevSPIK2k dev1
	){
		spk = dev1;
		set(op_1,op_2,op_3,op_4,
			op_5
		);
	}
	
	private static final String TAG0 = "Ton+";
	private static final String TAG1 = "Ton-";
	private static final String TAG2 = "Toff+";
	private static final String TAG3 = "Toff-";
	
	private static final String init_text = "脈衝設定";
	
	private Label msg1 = new Label(init_text);
	private Label msg2 = new Label();
	
	private TextField[] args = {
		new TextField(""),//T_on+
		new TextField(""),//T_on-
		new TextField(""),//T_off+
		new TextField(""),//T_off-
	};
	
	final Runnable op_1 = ()->operation(TAG0,args[0],4);
	final Runnable op_2 = ()->operation(TAG1,args[1],6);
	final Runnable op_3 = ()->operation(TAG2,args[2],5);
	final Runnable op_4 = ()->operation(TAG3,args[3],7);	
	final Runnable op_5 = ()->{
		msg1.setText(init_text);
		msg2.setText("");
		next.set(LEAD);
	};
	
	private void operation(
		final String name,
		final TextField box,
		final int addr
	){
		String txt = box.getText();
		try{
			final int value= Integer.valueOf(txt.trim());
			msg1.setText("設定 "+name);
			msg2.setText("");
			waiting_async();			
			spk.asyncBreakIn(()->{
				spk.set_register(addr, value);
				next.set(LEAD);
			});	
		}catch(NumberFormatException e){
			msg1.setText("忽略 "+name);
			msg2.setText("");
			next.set(LEAD);
		}
	}
	
	@Override
	public Node getContent(){
		msg1.setPrefWidth(150);
		msg2.setPrefWidth(150);
		for(TextField box:args){
			box.setMaxWidth(80);
		}
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.add(msg1, 0, 0);
		lay.add(msg2, 0, 1);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
		lay.addRow(0, new Label("Ton+"), args[0], new Label("Toff+"), args[2]);
		lay.addRow(1, new Label("Ton-"), args[1], new Label("Toff-"), args[3]);
		return lay;
	}
	@Override
	public void eventEdit() {
	}

	@Override
	public String flatten() {
		return String.format(
			"%s:%s,  %s:%s, %s:%s,  %s:%s,",
			TAG0, args[0].getText().trim(),
			TAG1, args[1].getText().trim(),
			TAG2, args[2].getText().trim(),
			TAG3, args[3].getText().trim()
		);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\\\p{Space}]+[:]\\\\p{ASCII}*[,\\\\s]?)+")==false){
			Misc.loge("pasing fail");
			return;
		}
		String[] arg = txt.split(":|,");
		for(int i=0; i<arg.length; i+=2){
			final String tag = arg[i+0].trim();
			final String val = arg[i+1].trim();
			if(tag.equals(TAG0)==true){
				args[0].setText(val);
			}else if(tag.equals(TAG1)==true){
				args[1].setText(val);
			}else if(tag.equals(TAG2)==true){
				args[2].setText(val);
			}else if(tag.equals(TAG3)==true){
				args[3].setText(val);				
			}
		}
	}

}
