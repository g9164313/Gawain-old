package prj.sputter;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.sun.glass.ui.Application;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import narl.itrc.Misc;
import narl.itrc.PanBase;

public class StepWatcher extends StepExtender {
	
	private final static String action_name = "厚度監控";
	public final static String TAG_WATCH = "監控";
	
	public StepWatcher(){
		set_mesg(action_name);
		set(op_1,op_3,
			op_4,op_5,
			op_6
		);
	}
	
	private final Label inf_rate= new Label("");
	private final Label inf_avg = new Label("");
	private final Label inf_dev = new Label("");
	
	private final TextField[] box_args = {
		new TextField(""),new TextField("400"),new TextField("200"),
		new TextField("3"),new TextField("0.3"),new TextField(""),
	};
	final TextField box_goal = box_args[0];
	final TextField box_maxw = box_args[1];
	final TextField box_minw = box_args[2];
	final TextField box_prop = box_args[3];
	final TextField box_inte = box_args[4];
	final TextField box_deri = box_args[5];
	
	private DescriptiveStatistics stats = new DescriptiveStatistics(30);
	
	long tick_beg = -1L, tick_end = -1L;
	String tick_txt, high_txt;
	
	final Runnable op_1 = ()->{
		//open shutter
		final String txt = "開啟檔板";
		set_mesg(action_name,txt);
		waiting_async();
		sqm.shutter_and_zeros(true, ()->{
			Misc.logv("%s: %s",action_name, txt);
			tick_beg = System.currentTimeMillis();
			next.set(LEAD);
		}, ()->{
			Misc.logv("%s: %s",action_name, txt+"失敗");
			abort_step();
			Application.invokeLater(()->PanBase.notifyError("失敗", "無法控制擋板!!"));
		});
		stats.setWindowSize(Integer.valueOf(box_deri.getText().trim()));
		stats.clear();
	};
	//final Runnable op_2 = ()->{
	//	set_mesg(
	//		"等待檔板",
	//		String.format("%s",Misc.tick2text(waiting_time(5000),true)
	//	));		
	//};
	
	final void PID_feedback() {
		int gain,maxw,minw;
		float goal,thres;
		try {
			gain = Integer.valueOf(box_prop.getText().trim());
			maxw = Integer.valueOf(box_maxw.getText().trim());
			minw = Integer.valueOf(box_minw.getText().trim());
			goal = Float.valueOf(box_goal.getText().trim());			
			int filter_size = Integer.valueOf(box_deri.getText().trim());
			if(filter_size!=stats.getWindowSize()) {
				stats.setWindowSize(filter_size);
				stats.clear();
				return;
			}			
			thres = Float.valueOf(box_inte.getText().trim());
		}catch(NumberFormatException e) {
			return;
		}		
		int n_size = (int)stats.getN();
		float delta = 0f; 
		for(int i=0; i<n_size; i++) {
			delta = delta + Math.abs(((float)stats.getElement(i)-goal));
		}
		delta = delta / ((float)n_size);
		if(delta<=thres) {
			return;
		}
		if(stats.getMean()>goal) {
			gain = -1*gain;
		}
		dcg.asyncAdjustWatt(gain,minw,maxw);
	}
	
	final Runnable op_3 = ()->{
		//monitor shutter
		tick_end = System.currentTimeMillis();
		tick_txt = Misc.tick2text(tick_end-tick_beg,true);
		high_txt = String.format(
			"%5.3f%s",
			sqm.thick[0].get(), sqm.unitHigh.get()
		);
		set_mesg(
			action_name,
			tick_txt,
			high_txt
		);
		
		final float rate_value= sqm.rate[0].get();
		final String rate_unit= sqm.unitRate.get();
		
		stats.addValue(rate_value);
		
		inf_rate.setText(String.format(
			"%5.3f%s", 
			rate_value, rate_unit
		));
		inf_avg.setText(String.format(
			"%5.3f", stats.getMean()
		));
		
		double sigma = stats.getVariance();
		if(sigma==Double.NaN) {
			inf_dev.setText("-----");
		}else {
			inf_dev.setText(String.format("%5.3f", sigma));
		}
		print_info(TAG_WATCH);
				
		if(sqm.shutter.get()==false){
			next_step();
		}else{
			PID_feedback();
			hold_step();
		}
	};
	final Runnable op_4 = ()->{
		//extinguish plasma		
		set_mesg("關閉高壓");
		Misc.logv("%s: 關閉高壓",action_name);
		waiting_async();		
		dcg.asyncBreakIn(()->{
			if(dcg.exec("OFF").endsWith("*")==false) {
				abort_step();
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法關閉!!"));
			}else {
				next_step();
			}
		});
	};	
	final Runnable op_5 = ()->{
		int vv = (int)dcg.volt.get();
		int ww = (int)dcg.watt.get();
		if(vv>=30 && ww>=1){
			next.set(HOLD);
		}else{
			next.set(LEAD);
		}
		set_mesg(
			"放電中",
			String.format("%3dV %3dW",vv,ww)
		);
	};
	final Runnable op_6 = ()->{
		set_mesg(
			action_name,
			tick_txt,
			high_txt
		);
	};
	
	@Override
	public Node getContent(){

		inf_rate.setPrefWidth(80);
		inf_avg.setPrefWidth(80);
		inf_dev.setPrefWidth(80);
		
		for(TextField obj:box_args) {
			obj.setPrefWidth(80);
		}

		box_deri.setText(""+stats.getWindowSize());
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, msg);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		lay.addColumn(2,new Label("成長速率"),new Label("統計值"),new Label("標準差"));
		lay.addColumn(3,inf_rate,inf_avg,inf_dev);
		lay.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 3);
		lay.addColumn(5,new Label("目標速率"),new Label("最大功率"),new Label("最小功率"));
		lay.addColumn(6,box_goal,box_maxw,box_minw);
		lay.addColumn(7,new Label("P"),new Label("I"),new Label("IT"));
		lay.addColumn(8,box_prop,box_inte,box_deri);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	
	private static final String TAG0 = "ATTR-VAL";
	private static final String TAG1 = "ATTR-MIN";
	private static final String TAG2 = "ATTR-MAX";
	private static final String TAG3 = "PID-P";
	private static final String TAG4 = "PID-I";
	private static final String TAG5 = "PID-IT";
	
	@Override
	public String flatten() {
		return String.format(
			"%s:%s, %s:%s, %s:%s, %s:%s, %s:%s, %s:%s",
			TAG0, box_goal.getText().trim(),
			TAG1, box_minw.getText().trim(),
			TAG2, box_maxw.getText().trim(),
			TAG3, box_prop.getText().trim(),
			TAG4, box_inte.getText().trim(),
			TAG5, box_deri.getText().trim()
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
			if(tag.equals(TAG0)==true){
				box_goal.setText(val);
			}else if(tag.equals(TAG1)==true){
				box_minw.setText(val);
			}else if(tag.equals(TAG2)==true){
				box_maxw.setText(val);
			}else if(tag.equals(TAG3)==true){
				box_prop.setText(val);
			}else if(tag.equals(TAG4)==true){
				box_inte.setText(val);
			}else if(tag.equals(TAG5)==true){
				box_deri.setText(val);
			}
		}
	}
}
