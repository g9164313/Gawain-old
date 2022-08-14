package prj.shelter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

import javafx.scene.Node;
import narl.itrc.Misc;
import narl.itrc.UtilPhysical;
import prj.shelter.ManBooker.Mark;

public class RadiateDose extends RadiateStep {
	
	public static final String NAME = "劑量照射";
	
	public RadiateDose() {
		box_loca.setDisable(true);
		box_dose.setOnAction(e->{			
		});
		chk_trac.disableProperty().bind(chk_meas.selectedProperty().not());
		set(op_init,
			op_move_pallet, op_wait_pallet, 
			op_make_radiate, 
			op_prewarm, op_make_measure, op_wait_measure,
			op_wait_radiation,
			op_foot
		);
	}


	public RadiateDose setValues(
		final String dose,
		final String left,
		final DevHustIO.Strength stng,
		final boolean meas,
		final boolean mark,
		final boolean trac
	) {
		//box_loca.setText();
		box_dose.setText(dose);
		box_left.setText(left);
		cmb_stng.setValue(stng);
		chk_meas.setSelected(meas);
		chk_mark.setSelected(mark);
		chk_trac.setSelected(trac);
		return this;
	}
	
	final int TRACE_MAX = 10;
	int trace_cnt = 0;
	Mark[] prev_mark = null;
	
	final Runnable op_init = () -> {
		trace_cnt = 0;		
		if(chk_trac.isSelected()==true) {
			prev_mark = decide_next_location();
		}else {
			prev_mark = null;
		}
				
		final String txt = box_loca.getText();
		if(txt.length()==0||txt.contains("?")) {
			Misc.loge("[RadiateDose] wrong location - %s", txt);
			abort_step();
		}else {
			next_step();
		}
	};
	final Runnable op_foot = () -> {
		next_step();
		if(chk_trac.isSelected()==true) {
			trace_cnt+=1;
			if(trace_cnt<=TRACE_MAX) {
				//first, mark the data~~~
				booker.insert(hustio, at5350);
				//Second, decide the next location~~~
				String res = txt_desc.getText();
				res = res+" @ "+trace_method();
				txt_desc.setText(res);
			}			
			show_info(NAME, "TRY:"+trace_cnt);
		}else {
			if(chk_mark.isSelected()==true) {
				booker.insert(hustio, at5350);
			}
			show_info(NAME);
		}		
	};

	private String trace_method() {
		
		if(prev_mark==null) {
			return "no prev_mark!!";
		}
		
		final double goal = UtilPhysical.convert(
			box_dose.getText(), 
			"uSv/hr"
		);
		final SummaryStatistics stat = at5350.lastSummary();

		final float epsilon= (goal<=10.)?(0.2f):(1f);
		final float diff = (float) Math.abs(goal-stat.getMean());
		
		Misc.logv("[trace] abs(goal-obv) @ abs(%.2f-%.2f)=%.2f", 
			goal, 
			stat.getMean(),
			diff
		);
		
		if(Math.abs(goal-stat.getMean())<=epsilon) {
			return "<ep";//we reach goal~~~~
		}

		prev_mark = decide_next_location();

		final String next_loca = box_loca.getText();
		
		Misc.logv("[trace] next=%s",next_loca);
		
		if(next_loca.length()==0) {
			return "ERROR!!, NO MARK!!";
		}
		
		next_step(this.op_foot,this.op_move_pallet);
		return "TRY!! "+next_loca;
	}
	
	private Mark[] decide_next_location() {
		
		final double goal = UtilPhysical.convert(
			box_dose.getText(), 
			"uSv/hr"
		);
		Mark[] mm = booker.selectRange(cmb_stng.getValue(), goal);
		
		double l0=600.,l1=0.;
		if(mm[0]!=null) {
			l0 = UtilPhysical.convert(mm[0].loca, "cm");
		}
		if(mm[1]!=null) {
			l1 = UtilPhysical.convert(mm[1].loca, "cm");
		}
		if(mm[0]==null && mm[1]==null) {
			box_loca.setText("");
			return mm;
		}
		
		box_loca.setText(String.format("%.4f cm", (l0+l1)/2.));
		return mm;
	}
	
	
	/*private String test_bound() {
		double p0=0., p1=0.;
		if(prev_mark[0]!=null) {
			p0 = TestUtils.tTest(stat, prev_mark[0].stat);
		}
		if(prev_mark[1]!=null) {
			p1 = TestUtils.tTest(stat, prev_mark[1].stat);
		}
		if(p0>=0.5 || p1>=0.5) {		
			return String.format("p:%.2f,%.2f",p0,p1);//we have no choices~~~
		}
		return "";
	}*/
	
	/*private String trace_method2() {		
		final String loca = box_loca.getText();
		
		final double goal = UtilPhysical.convert(
			box_dose.getText(), 
			"uSv/hr"
		);		
		final String meas = at5350.lastMeasure();
		final SummaryStatistics stat = at5350.lastSummary();
		
		final double pval = TestUtils.tTest(goal, stat);
		final String ptxt = String.format("P:%.0f%%",pval*100.);
		
		final String desc = String.format(
			"%s @ %.3f %s ± %.3f @ %s",
			hustio.locationText.get(),
			stat.getMean(), "uSv/hr", 
			stat.getStandardDeviation(), ptxt
		);
		//we may hit goal~~~~
		box_loca.setUserData(loca);
		txt_desc.setText(desc);
		txt_desc.setUserData(meas);

		final DevHustIO.Strength stng = cmb_stng.getSelectionModel().getSelectedItem();
		Misc.logv("[RadiateDose][%s] %s - %s\n%s", stng.toString(), loca, desc, meas);
		
		trace_cnt+=1;
		final double up_bound = goal + 1.5 * stat.getStandardDeviation();
		final double dw_bound = goal - 1.5 * stat.getStandardDeviation();
		final double t_val = stat.getMean();
		if((t_val<dw_bound || up_bound<t_val) && trace_cnt<7) {
			//try again!!!!
			double v_loca = UtilPhysical.convert(loca, "cm");		
			double prdt = -70. + Math.sqrt(Math.abs(stat.getMean())/goal) * (v_loca + 70.);
			
			if(Double.isNaN(prdt)) {
				Misc.logw(
					"[RadiateDose][nan] wrong prediction: (X/%.4f)^2=(%.4f/%.4f)",
					v_loca, stat.getMean(), goal
				);
				prdt = 250.;
			}else if(prdt<0.) {
				Misc.logw(
					"[RadiateDose][neg] wrong prediction: (X/%.4f)^2=(%.4f/%.4f)",
					v_loca, stat.getMean(), goal
				);
				prdt = 250.;
			}
			
			box_loca.setText(String.format("%.4f cm", prdt));//assign new location~~~
			next_step(this.op_foot,this.op_move_pallet);
		}		
		return ptxt;
	}
	*/
	
	
	@Override
	public Node getContent() {
		show_info(NAME);
		return gen_layout();
	}
	@Override
	public void eventEdit() {
		show_measure(txt_desc);		
	}
	@Override
	public String flatten() {
		return event_flatten_var();
	}
	@Override
	public void expand(String txt) {
		event_expand_var(txt);
	}
}
