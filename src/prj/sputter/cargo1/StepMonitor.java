package prj.sputter.cargo1;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

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
			op1,run_waiting(1000,msg[1]),op2,op3,op4
			//op_sim
		);
	}
	
	/*final Runnable op_sim = ()->{
		for(int i=0; i<100; i++) {
			Misc.logv("[%s] V:%.3f, I:%.3f, P:%.3f",
				action_name,Math.random(),Math.random(),Math.random()
			);
		}
		next_step();
	};*/
	
	private DescriptiveStatistics stats = new DescriptiveStatistics(30);
	
	long tick_beg = -1L, tick_end = -1L;
	
	boolean apply_DC1 = false;
	float apply_pow = 0f;
	
	Float goal, gain, minw, maxw, thres;
	Integer fsize;
	
	private void refresh_box() {
		goal = Misc.txt2Float(box_goal.getText());	
		gain = Misc.txt2Float(box_prop.getText());
		minw = Misc.txt2Float(box_minw.getText());
		maxw = Misc.txt2Float(box_maxw.getText());
		thres= Misc.txt2Float(box_inte.getText());
		fsize= Misc.txt2Int(box_deri.getText());
	}
	
	final void PID_feedback() {
		stats.addValue(sqm1.meanRate.get());
		
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
		refresh_box();
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
		msg[1].setText("");
		Misc.logv("[%s] V:%.3f, I:%.3f, P:%.3f, Rate:%6.3f",
			action_name,
			spik.DC1_V_Act.get()/4f, 
			spik.DC1_I_Act.get()/1024f, 
			spik.DC1_P_Act.get()/2f,
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
			//track time stamp~~~~
			tick_end = System.currentTimeMillis();
			//shutdown all powers~~~
			//PanMain.douse_fire();//end all~~~~
			spik.toggleDC1(false);
			//close upper-shutter!!!
			adam1.asyncSetAllLevel(false);
			next_step();
		}
	};
	final Runnable op3 = ()->{
		hold_step();
		final float volt = spik.DC1_V_Act.get()/4f;
		msg[0].setText("放電中");
		msg[1].setText(String.format("V:%.1f", volt));
		if(volt<=300f) {
			next_step();
		}
	};	
	final Runnable op4 = ()->{		
		next_step();
		final String txt = Misc.tick2text(tick_end-tick_beg, true);
		msg[0].setText(action_name);
		msg[1].setText(txt);
		Misc.logv("[監控時間] %s",txt);
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
