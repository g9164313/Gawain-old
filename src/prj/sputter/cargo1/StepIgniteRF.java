package prj.sputter.cargo1;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;


public class StepIgniteRF extends StepCommon {

	public static final String action_name = "RF 電漿";
	
	final TextField watt = new TextField();
	final TextField duty = new TextField();
	final TextField freq = new TextField();
	
	public StepIgniteRF() {
		watt.setPrefWidth(100.);
		duty.setPrefWidth(100.);
		freq.setPrefWidth(100.);
		set(
			op1,
			run_waiting(1000,null),
			op2, op2_1,
			run_hold,
			op4
		);
	}

	final Runnable op1 = ()->{
		adam1.asyncSetLevel(2, true);//close shutter~~~
		next_step();
	};
	final Runnable op2 = ()->{
		next_step();
		msg[1].setText("apply");
		sar2.applyPulseSetting(
			box2int(watt), 
			box2int(freq), 
			box2int(duty)
		);		
	};
	final Runnable op2_1 = ()->{
		next_step();
		int val;
		val = box2int(watt);
		if(val>=0 && val!=sar2.watt.get()) {
			hold_step(); 
			return;
		}
		val = box2int(freq);
		if(val>=0 && val!=sar2.freq.get()) {
			hold_step(); 
			return;
		}
		val = box2int(duty);
		if(val>=0 && val!=sar2.duty.get()) {
			hold_step(); 
			return;
		}
		msg[1].setText("done!!");
		sar2.setRFOutput(true);		
	};
	protected final Runnable op4 = ()->{
		next_step();
		if(chk_cont.isSelected()==false) {
			sar2.setRFOutput(false);
		}
	};
		
	@Override
	public Node getContent() {
		
		final Label inf1 = new Label();
		inf1.textProperty().bind(sar2.powerForward.asString("輸出: %3d"));
		
		final Label inf2 = new Label();
		inf2.textProperty().bind(sar2.powerReflect.asString("反射: %3d"));
		
		final Label inf3 = new Label();
		inf3.textProperty().bind(sar2.powerDelived.asString("遞送: %3d"));
		
		return gen_grid_pane(
			action_name,"5:00",true,
			new HBox(new Label("功率:"), watt), inf1,
			new HBox(new Label("頻率:"), duty), inf2,
			new HBox(new Label("週期:"), freq), inf3
		);
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return control2text(box_hold,chk_cont,watt,duty,freq);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,box_hold,chk_cont,watt,duty,freq);
	}
}
