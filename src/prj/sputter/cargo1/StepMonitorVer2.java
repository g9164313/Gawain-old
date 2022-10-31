package prj.sputter.cargo1;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;

public class StepMonitorVer2 extends StepCommon {

	public static final String action_name = "薄膜監控";

	final Label txt_mean = new Label();
	final TextField box_goal = new TextField("1.0");//rate, unit is Å/s
	final TextField box_test = new TextField("80");//t-test percent(0~100)
		
	final TextField box_minw = new TextField("30");//lower bound
	final TextField box_gain = new TextField("5");//gain
	final TextField box_maxw = new TextField("100");//upper bound
		
	public StepMonitorVer2() {
		//style all box~~~~
		for(TextField box:new TextField[] {
			box_goal, box_test,
			box_minw, box_gain, box_maxw,			
		}) {
			box.setPrefWidth(97);
		}
		set(
			op1,run_waiting(1000,msg[1]),
			op2,
			op3
		);
	}
	
	private long tick_cnt;
	
	private Float pow_min, pow_gain, pow_max;
	private Float rate_goal, rate_test;
	
	private DescriptiveStatistics stats = new DescriptiveStatistics(20);
	
	private float pow_mdfy;
	
	final void PID_feedback() {
		final float rate= sqm1.meanRate.get();
		stats.addValue(rate);
		txt_mean.setText(String.format("%5.2f", stats.getMean()));
		if(stats.getN()<stats.getWindowSize()) {
			return;
		}		
		if(
			pow_min==null || pow_gain==null || pow_max==null ||
			rate_goal==null || rate_test==null
		) {
			return;
		}
		
		final double tst = TestUtils.t(rate_goal, stats);
		msg[1].setText(String.format("T:%.1f%%", tst*100.));
		
		if(tst>=rate_test) {
			return;
		}
		
		float pow = (float)(spik.DC1_P_Act.get() / 2);
		
		if(Math.abs(pow-pow_mdfy)>=pow_gain) {
			Misc.logv("[PID_feedback] RAMP:%.1f-->%.1f", pow, pow_mdfy);
			return;//still ramping~~~
		}
		
		if(rate_goal<=rate) {
			pow = pow - pow_gain;
		}else if(rate<rate_goal) {
			pow = pow + pow_gain;
		}
		if(pow<pow_min) {
			pow = pow_min;
			return;
		}
		if(pow_max<pow) {
			pow = pow_max;
			return;
		}
		
		pow_mdfy = pow;
		
		spik.set_DC_value(
			tkn->{
				stats.clear();//for next turn~~~~
				Misc.logv("[PID_feedback] DC1_pow=%.1f", pow_mdfy);
			}, 
			'1', 'P', pow
		);
	}
	
	final Runnable op1 = ()->{
		//open shutter and zero meter!!!
		stats.clear();
		pow_mdfy = (float)(spik.DC1_P_Act.get()/2);
		refresh_box();
		wait_async();		
		sqm1.shutter_and_zeros(true, ()->{
			tick_cnt = System.currentTimeMillis();
			msg[1].setText("開啟檔板");
			adam1.asyncSetAllLevel(
				true, 
				chk_sh2.isSelected(),
				chk_sh3.isSelected()
			);
			notify_async();
		}, ()->{
			msg[1].setText("SQM錯誤");
			abort_step();
		});	
	};
	final Runnable op2 = ()->{		
		hold_step();
		msg[0].setText("濺鍍中");
		msg[1].setText("");
		Misc.logv("[%s] V:%4d, I:%4d, P:%4d, Film:%6.3f",
			action_name,
			spik.DC1_V_Act.get(), 
			spik.DC1_I_Act.get(), 
			spik.DC1_P_Act.get(),
			sqm1.meanRate.get()
		);
		PID_feedback();
		//to much ARC, or other error~~~~ 
		if(
			spik.Run.get()==false ||
			spik.DC1.get()==false
		) {			
			msg[0].setText("SPIK");
			msg[1].setText("沉默");
			abort_step();
			return;
		}
		//wait quartz fail(True) or shutter close(False)~~~
		if(
			adam1.DI[0].get()==true || 
			adam1.DI[1].get()==false
		) {	
			tick_cnt = System.currentTimeMillis() - tick_cnt;
			adam1.asyncSetAllLevel(false);//close upper-shutter!!!
			next_step();
		}
	};
	final Runnable op3 = ()->{
		//shutdown all powers~~~
		msg[0].setText(action_name);
		msg[1].setText(Misc.tick2text(tick_cnt, true));
		next_step();
		spik.toggleDC1(false);//douse fire~~~~
		
		//PanMain.douse_fire();//end all~~~~
	};
	
	private void refresh_box() {
		stats.clear();
		pow_min = Misc.txt2Float(box_minw.getText());
		pow_gain= Misc.txt2Float(box_gain.getText());
		pow_max = Misc.txt2Float(box_maxw.getText());
		rate_goal=Misc.txt2Float(box_goal.getText());
		rate_test=Misc.txt2Float(box_test.getText());
		if(rate_test!=null) {
			rate_test = rate_test / 100f;
		}
	}
	
	@Override
	public Node getContent() {
		msg[0].setText(action_name);
		msg[0].setMinWidth(100.);		
		msg[1].setMinWidth(100.);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,msg);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.addColumn(2, chk_sh2, chk_sh3);
		lay.add(new Separator(Orientation.VERTICAL), 3, 0, 1, 2);
		lay.addRow(0,
			new Label("最小(W):"), box_minw,
			new Label("增益(W):"), box_gain, 
			new Label("最大(W):"), box_maxw
		);
		lay.addRow(1,
			new Label("平均(Å/s)"), txt_mean,
			new Label("目標(Å/s)"), box_goal,			
			new Label("相似(%)"), box_test
		);
		return lay;
	}
	@Override
	public void eventEdit() {		
		final Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setTitle("設定PID");
		dia.setHeaderText("確認更新參數?");
		if(dia.showAndWait().get()==ButtonType.OK) {
			refresh_box();
		}	
	}
	@Override
	public String flatten() {
		return control2text(
			chk_sh2, chk_sh3,
			box_gain, box_minw, box_maxw, 
			box_goal, box_test
		);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,
			chk_sh2, chk_sh3,
			box_gain, box_minw, box_maxw, 
			box_goal, box_test
		);
	}
}
