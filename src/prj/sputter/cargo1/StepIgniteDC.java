package prj.sputter.cargo1;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;


public class StepIgniteDC extends StepCommon {
	
	public static final String action_name = "DC 電漿";
	
	final TextField vol = new TextField();
	final TextField amp = new TextField();
	final TextField pow = new TextField();
	
	final TextField t_on_p = new TextField();
	final TextField t_on_n = new TextField();
	final TextField t_of_p = new TextField();
	final TextField t_of_n = new TextField();
	
	public StepIgniteDC() {
		vol.setPrefWidth(100.);
		amp.setPrefWidth(100.);
		pow.setPrefWidth(100.);
		
		t_on_p.setPrefWidth(87.);
		t_on_n.setPrefWidth(87.);
		t_of_p.setPrefWidth(87.);
		t_of_n.setPrefWidth(87.);
		
		set(op1,
			run_waiting(1000,null),
			op2,
			op3,
			op4, op4_1,
			run_hold,
			op5
		);
	}

	final Runnable op1 = ()->{
		adam1.asyncSetLevel(3, true);//close shutter~~~
		next_step();
	};
	final Runnable op2 = ()->{
		msg[1].setText("pulse");
		wait_async();
		spik.setPulseValue(tkn->{
				msg[1].setText("apply!");
				notify_async();
			}, 
			box2int(t_on_p), 
			box2int(t_of_p), 
			box2int(t_on_n), 
			box2int(t_of_n)
		);		
	};
	final Runnable op3 = ()->{
		msg[1].setText("DC1");
		wait_async();
		spik.set_DC1(tkn->{			
				msg[1].setText("apply!");
				notify_async();
			}, 
			vol.getText(),amp.getText(),pow.getText()
		);
	};

	final Runnable op4 = ()->{
		msg[1].setText("On/Off");
		next_step();
		spik.setAllOnOff(true, true, false);
	};
	final Runnable op4_1 = ()->{
		next_step();
		if(spik.Run.get()==false || spik.DC1.get()==false) {
			msg[1].setText("wait!!");
			hold_step();
		}
	};
	
	protected final Runnable op5 = ()->{
		next_step();
		if(chk_cont.isSelected()==false) {
			spik.setAllOnOff(false, false, false);
		}
	};
	
	@Override
	public Node getContent() {
		return gen_grid_pane(
			action_name,"5:00",true,
			new HBox(new Label("電壓:"), vol), new HBox(new Label("Ton +:"), t_on_p),
			new HBox(new Label("電流:"), amp), new HBox(new Label("Toff+:"), t_of_p),
			new HBox(new Label("功率:"), pow), new HBox(new Label("Ton -:"), t_on_n),
			new Label(), 
			new HBox(new Label("Toff-:"), t_of_n)
		);
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return control2text(box_hold,chk_cont,vol,amp,pow);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,box_hold,chk_cont,vol,amp,pow);
	}
}
