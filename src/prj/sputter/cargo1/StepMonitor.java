package prj.sputter.cargo1;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;

public class StepMonitor extends StepCommon {

	public static final String action_name = "薄膜監控";

	final TextField box_goal = new TextField();//rate, unit is Å/s
	final TextField box_minw = new TextField();//min-power
	final TextField box_maxw = new TextField();//max-power	
	final TextField box_prop = new TextField();//proportional
	final TextField box_inte = new TextField();//integral
	final TextField box_deri = new TextField();//derivative
	
	public StepMonitor() {
		//style all box~~~~
		for(TextField box:new TextField[] {
			box_goal, box_minw, box_maxw, 
			box_prop, box_inte, box_deri
		}) {
			box.setPrefWidth(97);
		}
		set(
			op1,run_waiting(1000,msg[1]),
			op2,
			op3
		);
	}
	
	private DescriptiveStatistics stats = new DescriptiveStatistics(30);
	
	long tick_beg = -1L, tick_end = -1L;
	
	boolean apply_DC1 = false;
	float apply_pow = 0f;
	
	final void PID_feedback() {
		stats.addValue(sqm1.meanRate.get());
		
		Float   goal = Misc.txt2Float(box_goal.getText());	
		Float   gain = Misc.txt2Float(box_prop.getText());
		Float   minw = Misc.txt2Float(box_minw.getText());
		Float   maxw = Misc.txt2Float(box_maxw.getText());
		Float   thres= Misc.txt2Float(box_inte.getText());
		Integer fsize= Misc.txt2Int(box_deri.getText());
		if(
			goal==null || gain==null || 
			thres==null || minw==null || maxw==null 
		) {
			msg[1].setText("~PASS~");
			return;
		}
		if(fsize!=null) {
			if(fsize!=stats.getWindowSize()) {
				msg[1].setText("重新統計");
				stats.setWindowSize(fsize);
				stats.clear();
				return;
			}
		}		
		if(stats.getN()<stats.getWindowSize()) {			
			msg[1].setText("~fill~");
			return;
		}
		
		final double avg = stats.getMean();
		final int n_size = (int)stats.getN();
		float tst = 0f; 
		for(int i=0; i<n_size; i++) {
			tst = tst + Math.abs(((float)stats.getElement(i)-goal));
		}
		tst = tst / ((float)n_size);
		if(tst<=thres) {
			msg[1].setText(String.format("=%.2f=", tst));
			return;
		}
		/*final double tst = TestUtils.t(goal, stats);
		msg[0].setText(String.format("AVG:%.2f", avg));
		if(tst>=thres) {
			msg[1].setText(String.format("=%.2f=", tst));
			return;
		}*/
		
		if(apply_DC1==true) {
			msg[1].setText("~wait~");
			return;
		}
		if(avg>=goal) {
			msg[1].setText(String.format("^%.2f^", tst));
			apply_pow = apply_pow - gain;
		}else {
			msg[1].setText(String.format("_%.2f_", tst));
			apply_pow = apply_pow + gain;
		}
		if(apply_pow>=maxw) {
			apply_pow = maxw;
		}
		if(apply_pow<=minw) {
			apply_pow = minw;
		}

		spik.set_DC_value(
			tkn->{ apply_DC1 = false; }, 
			'1', 'P', apply_pow
		);
		apply_DC1 = true;
		stats.clear();//for next measurement~~~
	}
	
	final Runnable op1 = ()->{
		//open shutter and zero meter!!!
		apply_DC1= false;
		apply_pow= spik.DC1_P_Set.get() / 2f;		
		wait_async();		
		sqm1.shutter_and_zeros(true, ()->{
			tick_beg = System.currentTimeMillis();
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
		PID_feedback();
		//to much ARC, or other error~~~~ 
		if(
			spik.Run.get()==false ||
			spik.DC1.get()==false
		) {			
			msg[0].setText("SPIK");
			msg[0].setText("沉默");
			abort_step();
			return;
		}		
		//wait quartz fail(True) or shutter close(False)~~~
		if(
			adam1.DI[0].get()==true || 
			adam1.DI[1].get()==false
		) {
			//close upper-shutter!!!
			adam1.asyncSetAllLevel(false);
			next_step();
		}
	};
	final Runnable op3 = ()->{
		//shutdown all powers~~~
		tick_end = System.currentTimeMillis();
		next_step();
		msg[0].setText(action_name);
		msg[1].setText(Misc.tick2text(tick_end-tick_beg, true));
		PanMain.douse_fire();//end all~~~~
	};
	
	@Override
	public Node getContent() {
		msg[0].setText(action_name);
		msg[0].setMinWidth(100.);		
		msg[1].setMinWidth(100.);
		
		box_deri.setText(""+stats.getWindowSize());
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,msg);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.addColumn(2, chk_sh2, chk_sh3);
		lay.add(new Separator(Orientation.VERTICAL), 3, 0, 1, 2);
		lay.addRow(0,
			new Label("最小輸出(W)"), box_minw,
			new Label("目標速率(Å/s)"), box_goal,
			new Label("最大輸出(W)"), box_maxw
		);
		lay.addRow(1, 
			new Label("P(W):"), box_prop, 
			new Label("I(Å/s):"), box_inte, 
			new Label("D(N):"), box_deri
		);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return control2text(
			chk_sh2, chk_sh3,
			box_goal, box_minw, box_maxw, 
			box_prop, box_inte, box_deri
		);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,
			chk_sh2, chk_sh3,
			box_goal, box_minw, box_maxw, 
			box_prop, box_inte, box_deri
		);
	}
}
