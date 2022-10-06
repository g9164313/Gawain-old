package prj.sputter.cargo1;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import narl.itrc.Misc;

public class StepIgniteDC extends StepCommon {
	
	public static final String action_name = "DC 電漿";
	
	final TextField vol = new TextField();
	final TextField amp = new TextField();
	final TextField pow = new TextField();
	
	final TextField t_on_p = new TextField();
	final TextField t_on_n = new TextField();
	final TextField t_of_p = new TextField();
	final TextField t_of_n = new TextField();
	
	final ComboBox<String> polar = new ComboBox<String>();
	
	public StepIgniteDC() {
		vol.setPrefWidth(100.);
		amp.setPrefWidth(100.);
		pow.setPrefWidth(100.);
		
		t_on_p.setPrefWidth(87.);
		t_on_n.setPrefWidth(87.);
		t_of_p.setPrefWidth(87.);
		t_of_n.setPrefWidth(87.);
		
		polar.getItems().addAll(
			"unchange",
			"Bipolar",
			"Unipolar -","Unipolar +",
			"DC- Mode","DC+ Mode"
		);
		polar.getSelectionModel().select(0);
		
		set(op1, run_waiting(1000,null),
			op2_1, run_waiting(1000,null),
			op2_2, run_waiting(1000,null),
			op2_3, run_waiting(1000,null),
			op4, op4_1, run_holding, op5
		);
	}

	final Runnable op1 = ()->{
		msg[1].setText("關閉檔板");
		next_step();		
		adam1.asyncSetAllLevel(
			false,
			chk_sh2.isSelected(), 
			chk_sh3.isSelected()
		);		
	};
	final Runnable op2_1 = ()->{		
		msg[0].setText("設定脈衝");
		msg[1].setText("");
		//wait_async();
		next_step();
		spik.setPulseValue(tkn->{
				msg[1].setText("Pulse!");
				//notify_async();
			}, 
			Misc.txt2int(t_on_p.getText()), 
			Misc.txt2int(t_of_p.getText()), 
			Misc.txt2int(t_on_n.getText()), 
			Misc.txt2int(t_of_n.getText())
		);		
	};
	final Runnable op2_2 = ()->{
		msg[0].setText("設定電極");
		msg[1].setText("");
		next_step();
		final int idx = polar.getSelectionModel().getSelectedIndex();		
		if(idx!=0) {
			//wait_async();
			spik.asyncSetRegister(tkn->{
				msg[1].setText("Polar!");
				//notify_async();
			}, 0, idx);
		}
	};
	final Runnable op2_3 = ()->{
		msg[0].setText("設定電源");
		msg[1].setText("");
		next_step();
		//wait_async();
		spik.set_DC1(tkn->{			
				msg[1].setText("DC1!");
				//notify_async();
			},
			Misc.txt2Float(vol.getText()),
			Misc.txt2Float(amp.getText()),
			Misc.txt2Float(pow.getText())
		);
	};

	final Runnable op4 = ()->{
		msg[0].setText("-Run-");
		msg[1].setText("On!!");
		next_step();
		if(spik.Run.get()==true) {
			if(spik.DC1.get()==false) {
				spik.toggle(true, true, false);
			}
		}else {
			spik.toggle(true, true, false);
		}
	};
	final Runnable op4_1 = ()->{
		msg[1].setText("Ready?");
		next_step();
		if(spik.Run.get()==false || spik.DC1.get()==false) {
			hold_step();
		}
	};
	final Runnable op5 = ()->{
		msg[0].setText(action_name);
		msg[1].setText("cont-");
		next_step();
		if(chk_cont.isSelected()==false) {
			msg[1].setText("");
			spik.toggle(false, false, false);
		}
	};

	@Override
	public Node getContent() {
		return gen_grid_pane(
			action_name,"5:00",true,
			chk_sh2, chk_sh3, 
			null, null,
			new Label("電壓"), new Label("Ton +"), vol, t_on_p,
			new Label("電流"), new Label("Toff+"), amp, t_of_p,
			new Label("功率"), new Label("Ton -"), pow, t_on_n,
			new Label("極性"), new Label("Toff-"), polar, t_of_n
		);
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return control2text(
			chk_sh2, chk_sh3, box_hold, chk_cont, 
			vol, amp, pow, polar,
			t_on_p, t_on_n, t_of_p, t_of_n
		);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,
			chk_sh2, chk_sh3, box_hold, chk_cont, 
			vol,amp, pow, polar,
			t_on_p, t_on_n, t_of_p, t_of_n
		);
	}
}
