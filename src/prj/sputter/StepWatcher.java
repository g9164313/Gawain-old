package prj.sputter;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.sun.glass.ui.Application;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
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
		set_info(action_name);
		set(op_1,op_3,
			op_4,op_5,
			op_6
		);
	}
	
	private final Label stats_avg = new Label("");
	private final Label stats_dev = new Label("");
	
	final TextField[] box_args = {
		new TextField("30"),
		new TextField("3."),new TextField("0.3"),
		new TextField("200"),new TextField("500"),
		new TextField(),
		new TextField(),
	};
	final TextField box_stat_win = box_args[0];//filter window, or cycle
	final TextField box_max_rate = box_args[1];
	final TextField box_min_rate = box_args[2];
	final TextField box_max_watt = box_args[3];
	final TextField box_min_watt = box_args[4];
	final TextField box_ctl_arg1 = box_args[5];
	final TextField box_ctl_arg2 = box_args[6];
	
	private static String FILTER_NONE = "無自動調整";
	private static String FILTER_PID1 = "平均算術調整";
	private static String FILTER_PID2 = "訊號比調整";
	private static String FILTER_PID3 = "卡爾曼濾波";
	private static String FILTER_PID4 = "高斯過程";	
	private final ComboBox<String> cmb_filter = new ComboBox<String>(); 
	
	private DescriptiveStatistics stats = new DescriptiveStatistics();
	
	long tick_beg = -1L, tick_end = -1L;
	
	final Runnable op_1 = ()->{
		//open shutter
		final String txt = "開啟檔板";
		set_info(action_name,txt);
		wait_async();
		sqm.shutter_and_zeros(true, ()->{
			Misc.logv("%s: %s",action_name, txt);
			tick_beg = System.currentTimeMillis();
			next.set(LEAD);
		}, ()->{
			Misc.logv("%s: %s",action_name, txt+"失敗");
			abort_step();
			Application.invokeLater(()->PanBase.notifyError("失敗", "無法控制擋板!!"));
		});		
		//statistic for rate value~~~		
		stats.setWindowSize(Integer.valueOf(box_stat_win.getText().trim()));
		stats.clear();
		//initialize value for PID2
		hit_count = 0;
	};
	//--------------------------------//
	
	final void PID_feedback_filter1() {
		int gain=100,max_watt,min_watt;
		float max_rate,min_rate;
		float goal,thrs;
		try {
			gain= Integer.valueOf(box_ctl_arg1.getText().trim());
			thrs= Float.valueOf(box_ctl_arg2.getText().trim());
			
			max_watt = Integer.valueOf(box_max_watt.getText().trim());
			min_watt = Integer.valueOf(box_min_watt.getText().trim());
			
			max_rate = Integer.valueOf(box_max_watt.getText().trim());
			min_rate = Integer.valueOf(box_min_watt.getText().trim());			
			
			goal = (max_rate + 3f*min_rate)/2f;
			
			int wsize = Integer.valueOf(box_stat_win.getText().trim());
			if(wsize!=stats.getWindowSize()) {
				stats.setWindowSize(wsize);
				stats.clear();
				return;
			}			
		}catch(NumberFormatException e) {
			return;
		}		
		int n_size = (int)stats.getN();
		float delta = 0f; 
		for(int i=0; i<n_size; i++) {
			delta = delta + Math.abs(((float)stats.getElement(i)-goal));
		}
		delta = delta / ((float)n_size);
		if(delta<=thrs) {
			return;
		}
		if(stats.getMean()>goal) {
			gain = -1*gain;
		}
		dcg.asyncAdjustWatt(gain, min_watt, max_watt);
	}
	//--------------------------------//
	private int hit_count = 0;
	final void PID_feedback_filter2() {
		int gain,thrs,max_watt,min_watt;
		float max_rate,min_rate;
		try {
			gain = Integer.valueOf(box_ctl_arg1.getText().trim());
			thrs = Integer.valueOf(box_ctl_arg2.getText().trim());
			
			max_watt = Integer.valueOf(box_max_watt.getText().trim());
			min_watt = Integer.valueOf(box_min_watt.getText().trim());
			
			max_rate = Integer.valueOf(box_max_watt.getText().trim());
			min_rate = Integer.valueOf(box_min_watt.getText().trim());
			
		}catch(NumberFormatException e) {
			return;
		}
		final float rate = sqm.meanRate.get();		
		if(min_rate<=rate && rate<=max_rate) {
			hit_count+=1;
			return;
		}
		if(hit_count<thrs) {
			if(max_rate<rate) {
				gain = -1 * gain;
			}
			dcg.asyncAdjustWatt(gain, min_watt, max_watt);
		}
		hit_count = 0;
	}
	//--------------------------------//
	
	final Runnable op_3 = ()->{
		//monitor film data~~~

		tick_end = System.currentTimeMillis();
		set_info(
			TAG_WATCH, 
			Misc.tick2text(tick_end-tick_beg,true), 
			String.format("%5.3f%s", sqm.thick[0].get(), sqm.unitThick.get())
		);
		
		stats.addValue(sqm.meanRate.get());
		
		stats_avg.setText(String.format("%5.3f", stats.getMean()));
		double sigma = stats.getVariance();
		if(sigma==Double.NaN) {
			stats_dev.setText("-----");
		}else {
			stats_dev.setText(String.format("%5.3f", sigma));
		}
		log_data(TAG_WATCH);
				
		if(sqm.shutter.get()==false){
			next_step();
		}else{
			final String itm = cmb_filter.getSelectionModel().getSelectedItem();
			if(itm.equals(FILTER_PID1)==true) {				
				PID_feedback_filter1();//平均算術調整
			}else if(itm.equals(FILTER_PID2)==true){
				PID_feedback_filter2();//訊號比調整
			}else if(itm.equals(FILTER_PID3)==true){
				//卡爾曼濾波
			}else if(itm.equals(FILTER_PID4)==true){
				//高斯過程
			}
			hold_step();
		}
	};
	final Runnable op_4 = ()->{
		//extinguish plasma		
		set_info("關閉高壓");
		Misc.logv("%s: 關閉高壓",action_name);
		wait_async();		
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
		set_info(
			"放電中",
			String.format("%3dV %3dW",vv,ww)
		);
	};
	final Runnable op_6 = ()->{
		set_info(
			action_name,
			Misc.tick2text(tick_end-tick_beg,true),
			String.format("%5.3f%s", sqm.thick[0].get(), sqm.unitThick.get())
		);
	};
	
	private final void disable_ctl_box(final Label txt, final TextField box) {
		txt.setText("-------");
		txt.setDisable(true);
		box.setText("");
		box.setDisable(true);
	}
	private final void enable_ctl_box(
		final Label txt,final TextField box,
		final String key,final String val
	) {
		txt.setText(key);
		txt.setDisable(false);
		if(box.getText().length()==0) {
			box.setText(val);
		}
		box.setDisable(false);
	}
	
	@Override
	public Node getContent(){

		stats_avg.setPrefWidth(80);
		stats_dev.setPrefWidth(80);
		
		for(TextField obj:box_args) {
			obj.setPrefWidth(80);
		}

		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, info);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
		lay.addColumn(2,new Label("統計值"),new Label("標準差"),new Label("週期"));
		lay.addColumn(3,stats_avg,stats_dev,box_stat_win);
		lay.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 4);
		
		final Label txt_ctl_arg1 = new Label("");
		final Label txt_ctl_arg2 = new Label("");
		
		//how to load item step by step ?
		cmb_filter.getItems().addAll(
			FILTER_NONE,
			FILTER_PID1,FILTER_PID2,
			FILTER_PID3,FILTER_PID4
		);
		cmb_filter.valueProperty().addListener((obv,oldVal,newVal)->{
			disable_ctl_box(txt_ctl_arg1,box_ctl_arg1);
			disable_ctl_box(txt_ctl_arg2,box_ctl_arg2);
			if(newVal.equals(FILTER_PID1)==true) {				
				//平均算術調整
				enable_ctl_box(txt_ctl_arg1,box_ctl_arg1,"增益值","10");
				enable_ctl_box(txt_ctl_arg2,box_ctl_arg2,"門檻值","1.5");
			}else if(newVal.equals(FILTER_PID2)==true){
				//訊號比調整
				enable_ctl_box(txt_ctl_arg1,box_ctl_arg1,"增益值","10");
				enable_ctl_box(txt_ctl_arg2,box_ctl_arg2,"門檻值","10");
			}else if(newVal.equals(FILTER_PID3)==true){
				//卡爾曼濾波
			}else if(newVal.equals(FILTER_PID4)==true){
				//高斯過程
			}
		});
		cmb_filter.getSelectionModel().select(0);
		
		lay.add(cmb_filter, 5, 0, 4, 1);
		lay.addColumn( 5,new Label("最大速率"),new Label("最小速率"),txt_ctl_arg1);
		lay.addColumn( 6,box_max_rate,box_min_rate,box_ctl_arg1);		
		lay.addColumn( 7,new Label("最大功率"),new Label("最小功率"),txt_ctl_arg2);
		lay.addColumn( 8,box_max_watt,box_min_watt,box_ctl_arg2);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	
	/*private static final String TAG0 = "ATTR-VAL";
	private static final String TAG1 = "ATTR-MIN";
	private static final String TAG2 = "ATTR-MAX";
	private static final String TAG3 = "PID-P";
	private static final String TAG4 = "PID-I";
	private static final String TAG5 = "PID-IT";*/
	
	@Override
	public String flatten() {
		/*return String.format(
			"%s:%s, %s:%s, %s:%s, %s:%s, %s:%s, %s:%s",
			TAG0, box_goal.getText().trim(),
			TAG1, box_minw.getText().trim(),
			TAG2, box_maxw.getText().trim(),
			TAG3, box_prop.getText().trim(),
			TAG4, box_inte.getText().trim(),
			TAG5, box_deri.getText().trim()
		);*/
		return "";
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
			/*final String tag = col[i+0].trim();
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
			}*/
		}
	}
}
