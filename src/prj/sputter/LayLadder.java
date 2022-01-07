package prj.sputter;

import java.util.ArrayDeque;
import java.util.ArrayList;

import org.apache.commons.math3.random.RandomVectorGenerator;
import org.apache.commons.math3.random.SobolSequenceGenerator;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import narl.itrc.DevBase;
import narl.itrc.Ladder;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class LayLadder extends Ladder {

	static LayLogger logger;
	static DevCouple coup;
	static DevDCG100 dcg1;	
	static DevSPIK2k spik;
	static DevSQM160 sqm1;
	
	public final static String TXT_SETTING = "參數設定";
	public final static String TXT_VOLTAGE = "高壓設定";
	public final static String TXT_EXPLOIT = "資料探勘";
	//public final static String TXT_LATENCE = "資料探勘";
	public final static String TXT_LATENCE = "實驗用-1";
	public final static String TXT_MONITOR = "厚度監控";
	
	public LayLadder(
		final LayLogger obj,
		final DevBase... dev
	) {
		logger = obj;
		coup = (DevCouple)dev[0];
		//dcg1 = (DevDCG100)dev[1];	
		spik = (DevSPIK2k)dev[2];
		sqm1 = (DevSQM160)dev[3];
		
		StepFlowCtrl.dev= coup;
		StepGunsHub.dev = coup;
		
		StepExtender.sqm = sqm1;
		StepExtender.spk = spik;
		StepExtender.dcg = dcg1;
		StepExtender.cup = coup;
		
		addStep("分隔線",Stepper.Sticker.class);
		addStep("薄膜設定",StepSetFilm.class, sqm1);
		addStep(TXT_SETTING,StepSetting.class);			
		addStep(TXT_VOLTAGE,StepKindler.class);			
		addStep(TXT_MONITOR,StepWatcher.class);	
		addStep(TXT_LATENCE,StepLatence.class);
		
		/*addSack(
			"<單層鍍膜.5>", 
			Stepper.Sticker.class,
			StepSetFilm.class,
			StepSetPulse.class,
			StepGunsHub.class,			
			StepKindler.class,
			StepWatcher.class
		);*/
	}
	
	@Override
	protected void prelogue() {
		super.prelogue();
		logger.show_progress();
	}
	@Override
	protected void epilogue() {
		super.epilogue();
		dcg1.asyncExec("SPW=0");//close power~~~
		logger.done_progress();
	}
	@Override
	protected void user_abort() {
		dcg1.asyncExec("SPW=0");
	}
	//------------------------------------//
	
	public static class StepLatence extends StepExtender {		
		//draw Hysteresis (繪製磁滯曲線)

		final ToggleGroup grp = new ToggleGroup();
		
		final Runnable op_watch_data_inc = ()->{
			log_data("SSS-inc");
			set_info("抄錄資料");
			if(waiting_time(3000)==0) {				
				TextField box = (TextField)(grp.getSelectedToggle().getUserData());
				final float max = (float)(box.getUserData());
				final float val = Float.valueOf(box.getText().trim());				
				if(val<max) {
					box.setText(String.format("%.1f", val+1.f));
					next_step(-2);
				}else {
					next_step();
				}				
			}
		};
		
		final Runnable op_watch_data_dec = ()->{
			log_data("SSS-dec");
			set_info("抄錄資料");
			if(waiting_time(3000)==0) {				
				TextField box = (TextField)(grp.getSelectedToggle().getUserData());
				final float val = Float.valueOf(box.getText().trim());				
				if(val>0.f) {
					box.setText(String.format("%.1f", val-1.f));
					next_step(-2);
				}else {
					next_step();
				}				
			}
		};
		
		@Override
		public Node getContent() {
			
			set(
				op_shutter_close,
				op_give_mass_flow,
				op_wait_mass_flow,
				op_power_trg,
				op_wait_fire,
				op_give_mass_flow,
				op_wait_mass_flow,
				op_watch_data_inc,
				op_give_mass_flow,
				op_wait_mass_flow,
				op_watch_data_dec,
				op_power_off,
				op_calm_down
			);
			
			final JFXRadioButton ch1 = new JFXRadioButton("通道-1(Ar)");
			final JFXRadioButton ch2 = new JFXRadioButton("通道-2(N2)");
			final JFXRadioButton ch3 = new JFXRadioButton("通道-3(O2)");
			
			ch1.setToggleGroup(grp);
			ch1.setUserData(ar_sccm);
			ch2.setToggleGroup(grp);
			ch2.setUserData(n2_sccm);
			ch3.setToggleGroup(grp);
			ch3.setUserData(o2_sccm);
			
			ch3.setSelected(true);
						
			final GridPane lay = new GridPane();
			lay.getStyleClass().addAll("box-pad","font-console");
			lay.addColumn(0, info[0], info[1], info[2]);
			
			lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
			lay.addColumn(2, new Label("輸出功率"), new Label("爬升時間"), new Label("穩定時間"));
			lay.addColumn(3, spw, spr, w_time);
			lay.addColumn(4, ch1, ch2, ch3);
			lay.addColumn(5, ar_sccm, n2_sccm, o2_sccm);
			return lay;
		}
		@Override
		public void eventEdit() { }
		@Override
		public String flatten() { return ""; }
		@Override
		public void expand(String txt) { }
	};
	//------------------------------------//
	
	public static class StepSetting extends Stepper {

		public StepSetting(){
			
			for(TextField box:args) {
				box.setMaxWidth(80);
			}
			
			btn_bipolar.setToggleGroup(grp_polar);
			btn_bipolar.setOnAction(e->{
				chk_by_gun1.setDisable(true);				
				chk_by_gun2.setDisable(true);
				chk_by_gun1.setSelected(true);
				chk_by_gun2.setSelected(true);
			});
			btn_unipolar.setToggleGroup(grp_polar);
			btn_unipolar.setOnAction(e->{
				chk_by_gun1.setDisable(false);				
				chk_by_gun2.setDisable(false);
				chk_by_gun1.setSelected(false);
				chk_by_gun2.setSelected(false);
			});

			set(op1,op2,op3,op4,op5,op6);
		}
		
		final Label[] info = {
			new Label(TXT_SETTING), 
			new Label(),
			new Label(),
		};
		
		final TextField[] args = {
			new TextField(),//T_on+
			new TextField(),//T_on-
			new TextField(),//T_off+
			new TextField(),//T_off-
			new TextField(),//Ar
			new TextField(),//N2
			new TextField(), //O2
		};
		final TextField Ton_p = args[0];
		final TextField Ton_n = args[1];
		final TextField Toff_p= args[2];
		final TextField Toff_n= args[3];
		final TextField mfc_Ar= args[4];
		final TextField mfc_N2= args[5];
		final TextField mfc_O2= args[6];
		
		final ToggleGroup grp_polar = new ToggleGroup();
		final JFXRadioButton btn_bipolar = new JFXRadioButton("Bipolar");
		final JFXRadioButton btn_unipolar= new JFXRadioButton("Unipolar");
		final JFXCheckBox chk_by_gun1 = new JFXCheckBox("Gun-1");
		final JFXCheckBox chk_by_gun2 = new JFXCheckBox("Gun-2");
		
		final Runnable op1 = ()->{
			info[1].setText("");
			info[2].setText("");
			next_step();
			
			if(grp_polar.getSelectedToggle()==null) {
				return;
			}
			info[1].setText("調整電極");
			coup.asyncSelectGunHub(
				btn_bipolar.isSelected(), 
				btn_unipolar.isSelected(), 
				chk_by_gun1.isSelected(), 
				chk_by_gun2.isSelected()
			);
		};
		final Runnable op2 = ()->{
			info[1].setText("");
			info[2].setText("");
			next_step();
			
			final String t_on_p= Ton_p.getText().trim();
			final String t_on_n= Ton_n.getText().trim();
			final String toff_p= Toff_p.getText().trim();
			final String toff_n= Toff_n.getText().trim();
			if( t_on_p.length()==0 && t_on_n.length()==0 &&
				toff_p.length()==0 && toff_n.length()==0
			) {
				return;
			}
			
			info[1].setText("調整脈衝");			
			try{
				spik.asyncSetPulse(
					Integer.valueOf(t_on_p), 
					Integer.valueOf(t_on_n), 
					Integer.valueOf(toff_p),
					Integer.valueOf(toff_n)
				);
			}catch(NumberFormatException e){
				Misc.loge(e.getMessage());
				abort_step();
			}
		};
		final Runnable op3 = ()->{
			boolean flg = spik.isAsyncDone() | coup.isAsyncDone() ;
			if(flg==true) {
				hold_step();
			}else {
				next_step();				
			}
		};
		
		final Runnable op4 = ()->{
			info[1].setText("");
			info[2].setText("");
			next_step();
			
			if(
				mfc_Ar.getText().length()==0 &&
				mfc_N2.getText().length()==0 &&
				mfc_O2.getText().length()==0 
			){
				next_step(2);
				return;
			}
			info[1].setText("調整氣體");
			init_statis(mfc_Ar);
			init_statis(mfc_N2);
			init_statis(mfc_O2);			
			coup.asyncSetMassFlow(
				mfc_Ar.getText(),
				mfc_N2.getText(),
				mfc_O2.getText()
			);
		};
		final Runnable op5 = ()->{
			info[1].setText("等待穩定");
			boolean flg = true;
			flg &= is_mass_stable(coup.PV_FlowAr, mfc_Ar);
			flg &= is_mass_stable(coup.PV_FlowO2, mfc_O2);
			flg &= is_mass_stable(coup.PV_FlowN2, mfc_N2);
			if(flg==true) {
				next_step();
			}else {
				hold_step();
			}
		};		
		final Runnable op6 = ()->{
			info[1].setText("");
			info[2].setText("");
			next_step();
		};
		
		private void init_statis(final TextField box) {
			if(box.getText().length()==0) {
				box.setUserData(null);
			}else {
				box.setUserData(new DescriptiveStatistics(30));
			}
		}
		private boolean is_mass_stable(
			final ReadOnlyFloatProperty prop,
			final TextField box
		) {
			Object obj = box.getUserData();
			if(obj==null) {
				return true;
			}
			DescriptiveStatistics sts = (DescriptiveStatistics)obj;
			try {
				sts.addValue(prop.get());				
				float ths = Float.valueOf(box.getText());
				float avg = (float) sts.getMean();
				float dev = (float) sts.getStandardDeviation();
				if(Math.abs(avg-dev)>=ths) {
					return true;
				}				
			}catch(NumberFormatException e) {		
			}
			return false;
		}
		
		@Override
		public Node getContent() {			
			final GridPane lay = new GridPane();
			lay.getStyleClass().addAll("box-pad","font-console");
			lay.addColumn(0, info);
			
			lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
			lay.add(new Label("電極"), 2, 0, 2, 1);
			lay.addColumn(2, btn_bipolar, btn_unipolar);
			lay.addColumn(3, chk_by_gun1, chk_by_gun2);
			
			lay.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 3);
			lay.add(new Label("脈衝"), 5, 0, 4, 1);
			lay.addColumn(5, new Label("Ton+"), new Label("Ton-"));
			lay.addColumn(6, Ton_p, Ton_n);
			lay.addColumn(7, new Label("Toff+"),new Label("Toff-"));
			lay.addColumn(8, Toff_p, Toff_n);

			lay.add(new Separator(Orientation.VERTICAL), 9, 0, 1, 3);
			lay.addColumn(10, new Label("Ar"), new Label("N2"), new Label("O2"));
			lay.addColumn(11, mfc_Ar, mfc_N2, mfc_O2);
			return lay;
		}
		@Override
		public void eventEdit() {	
		}

		@Override
		public String flatten() {
			return "";
		}
		@Override
		public void expand(String txt) {
		}
	};
	//------------------------------------//
	
	public static class StepExploit extends Stepper {
		//實驗用步驟~~~
		public StepExploit() {
			for(TextField obj:box) {
				obj.getStyleClass().add("font-console");
				obj.setMaxWidth(100.);
			}
			for(Label obj:inf) {
				obj.getStyleClass().add("font-console");
				obj.setMinWidth(100.);
			}
			set(op0,op1,op2,op3,op4,op5,op6);
		}
		
		static final String TXT_UNKNOW = "-----";
		
		final Label[] inf = {
			//title and information
			new Label(), 
			new Label(), 
			new Label(), 
			new Label(),
			//current setting value
			new Label(), 
			new Label(), 
			new Label(),
		};
		final TextField[] box = {
			//power generator
			new TextField("500"),
			new TextField("300"),
			new TextField("10"),
			//mass flow - Ar
			new TextField("30"),
			new TextField("20"),
			new TextField("5"),
			//mass flow - O2
			new TextField("10"),
			new TextField("5"),
			new TextField("5"),
		};
		
		final TextField max_pw = box[0];
		final Label     cur_pw = inf[4];
		final TextField min_pw = box[1];
		final TextField stp_pw = box[2];
		
		final TextField max_ar = box[3];
		final Label     cur_ar = inf[5];
		final TextField min_ar = box[4];	
		final TextField stp_ar = box[5];
		
		final TextField max_o2 = box[6];
		final Label     cur_o2 = inf[6];
		final TextField min_o2 = box[7];
		final TextField stp_o2 = box[8];

		final DescriptiveStatistics stat_rate = new DescriptiveStatistics(20);
		
		class ValPack {

			int[] x = {0, 0, 0,};//pw, Ar, O2
			float y1, y2;
			
			ValPack(double... val){
				x[0] = scale_value(val[0],min_pw,max_pw);
				x[1] = scale_value(val[1],min_ar,max_ar);
				x[2] = scale_value(val[2],min_o2,max_o2);
			}
			ValPack setY(double avg, double dev) {
				y1 = (float)avg;
				y2 = (float)dev;
				return this;
			}
			int scale_value(
				final double val, 
				final TextField box_min,
				final TextField box_max
			) {
				final float max = Float.valueOf(box_max.getText().trim());
				final float min = Float.valueOf(box_min.getText().trim());				
				return (int)((max-min)*val+min);
			}
			String txt_power()  { return String.valueOf(x[0]); }
			String txt_mass_Ar(){ return String.valueOf(x[1]); }
			String txt_mass_O2(){ return String.valueOf(x[2]); }
		};
		final ArrayList<ValPack>  s_pooler = new ArrayList<ValPack>();
		final ArrayDeque<ValPack> s_queuer = new ArrayDeque<ValPack>();
		final RandomVectorGenerator gen = new SobolSequenceGenerator(3);
		
		long tick_beg;		
		ValPack best_pck = null;		
		final Runnable op0 = ()->{
			tick_beg = System.currentTimeMillis();			
			best_pck = null;
			inf[0].setText("Exploit");
			inf[1].setText("");
			inf[2].setText("");
			inf[3].setText("");
			next_step();
		};
		
		final Runnable op1 = ()->{
			//prepare and reset data
			s_pooler.clear();			
			for(int i=0; i<60; i++) {
				final double[] val = gen.nextVector();				
				s_queuer.add(new ValPack(val));
			}
			next_step();
		};
		
		final Runnable op2 = ()->{
			//initialize all variables
			final ValPack pck = s_queuer.getFirst();
			cur_pw.setText(pck.txt_power());
			cur_ar.setText(pck.txt_mass_Ar());
			cur_o2.setText(pck.txt_mass_O2());
			dcg1.asyncSetWatt(
				cur_pw.getText().trim()
			);
			coup.asyncSetMassFlow(
				cur_ar.getText().trim(), 
				"", 
				cur_o2.getText().trim()
			);
			stat_rate.clear();
			next_step();
		};
		
		final Runnable op3 = ()->{
			if(spik.ARC_count.get()>=10) {
				inf[1].setText("!!電弧!!");
				hold_step();
				return;
			}
			inf[1].setText("等待中");
			inf[2].setText("");
			inf[3].setText("");
			if(waiting_time(5*1000)>0) {
				return;//等待氣體穩定~~~			
			};
		};
		
		final Runnable op4 = ()->{
			
			stat_rate.addValue(sqm1.meanRate.get());			
			
			final int nn = (int)stat_rate.getN();
			final int ww = stat_rate.getWindowSize();
			inf[1].setText("統計中");
			inf[2].setText(String.format(
				"%4.2f s %.3f",
				stat_rate.getMean(),
				stat_rate.getStandardDeviation()
			));
			inf[3].setText(String.format(
				"%d/%d",
				nn,ww
			));			
			if(nn<ww) {
				hold_step();
				return;
			}

			final double avg = stat_rate.getMean();
			final double dev = stat_rate.getStandardDeviation();
			
			s_pooler.add(s_queuer.pollFirst().setY(avg,dev));
			
			Misc.logv(
				"%s: "+
				"%s W, %s sccm, %s sccm, "+
				"%s, %s ", 
				TXT_EXPLOIT,
				cur_pw.getText(), cur_ar.getText(), cur_o2.getText(),
				String.format("%5.2f",avg), String.format("%5.3f",dev) 
			);
			if(s_queuer.isEmpty()==true) {
				next_step();
			}else {
				next_step(-2);
			}			
		};

		final Runnable op5 = ()->{
			//make decision~~~~
			//find outline value(use min and max)~~~
			
			ValPack min,max;
			min = max = s_pooler.get(0);
			for(int i=1; i<s_pooler.size(); i++) {
				ValPack p = s_pooler.get(i);
				if(max.y1<p.y1) {
					max = p;
				}
				if(p.y1<min.y1) {
					min = p;
				}
			}
			best_pck = max;
			
			if(Math.abs(max.y1-min.y1)<0.1) {
				//distribution is too uniform, stop process~~~
				next_step();
			}else {
				//generate distribution again!!
				next_step(-4);
			}			
		};
		
		final Runnable op6 = ()->{
			long tick_diff = System.currentTimeMillis() - tick_beg;
			inf[0].setText("Done");
			inf[1].setText(Misc.tick2text(tick_diff,true));
			inf[2].setText("");
			inf[3].setText("");
			cur_pw.setText(best_pck.txt_power());
			cur_ar.setText(best_pck.txt_mass_Ar());
			cur_o2.setText(best_pck.txt_mass_O2());
			next_step();
		};
		
		@Override
		public Node getContent() {
			final GridPane lay = new GridPane();
			lay.getStyleClass().addAll("box-pad","font-console");
			lay.addColumn(0, inf[0], inf[1], inf[2], inf[3]);
			lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
			lay.addColumn(2,
				new Label(),
				new Label("最大"),
				new Label("SV" ),
				new Label("最小"),
				new Label("修正")
			);
			lay.addColumn(3,
				new Label("功率(watt)"), max_pw, cur_pw, min_pw, stp_pw
			);
			lay.addColumn(4,
				new Label("Ar (sccm)"), max_ar, cur_ar, min_ar, stp_ar
			);
			lay.addColumn(5,
				new Label("O2 (sccm)"), max_o2, cur_o2, min_o2, stp_o2
			);
			// "速率(Å/s ):"
			return lay;
		}
		@Override
		public void eventEdit() {
		}
		@Override
		public String flatten() {
			return "";
		}
		@Override
		public void expand(String txt) {
		}
	};
}
