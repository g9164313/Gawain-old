package prj.shelter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;
import narl.itrc.UtilPhysical;
import prj.shelter.DevHustIO.Strength;

public class RadiateDose extends RadiateStep {
	
	public static final String NAME = "劑量照射";
	
	public RadiateDose() {
		box_loca.setDisable(true);
		box_dose.setOnAction(e->{
			box_loca.setText(abacus.predictLocation(
				cmb_stng.getValue(), 
				box_dose.getText()
			));
		});
		chk_trac.disableProperty().bind(chk_meas.selectedProperty().not());
		set(op_init,
			op_move_pallet, op_wait_pallet, 
			op_make_radiation, op_prewarm,
			op_make_measure, op_wait_measure,
			op_wait_radiation,
			op_foot
		);
	}

	protected final CheckBox chk_trac = new CheckBox("追溯");
	
	public RadiateDose setValues(
		final String dose,
		final String left,
		final DevHustIO.Strength stng,
		final boolean meas,
		final boolean mark,
		final boolean trac
	) {
		box_loca.setText(abacus.predictLocation(stng,dose));
		box_dose.setText(dose);
		box_left.setText(left);
		cmb_stng.setValue(stng);
		chk_meas.setSelected(meas);
		chk_mark.setSelected(mark);
		chk_trac.setSelected(trac);
		return this;
	}
	
	final int TRACE_MAX = 7;
	
	int trace_cnt = 0;
	double prev_pval;
	String prev_loca;
	String prev_desc;
	
	final Runnable op_init = () -> {
		trace_cnt = 0;
		prev_pval = -1.;
		prev_loca = "";
		prev_desc = "";
		box_dose.getOnAction().handle(null);
		next_step();
	};
	final Runnable op_foot = () -> {
		Misc.logv("[RadiateDose] foot");
		next_step();
		
		final SummaryStatistics stat = at5350.lastSummary();

		String ptxt = "";
		double pval = 0.;
		
		final double goal = UtilPhysical.convert(
			box_dose.getText(), 
			"uSv/hr"
		);
		pval = TestUtils.tTest(goal, stat);
		ptxt = String.format("P:%.0f%%",pval*100.);
		
		final String desc = String.format(
			"%s @ %.3f %s ± %.3f @ %s",
			hustio.locationText.get(),
			stat.getMean(), "uSv/hr", 
			stat.getStandardDeviation(), ptxt
		);
		
		if(chk_trac.isSelected()==true) {
			if(pval<0.9 && trace_cnt<TRACE_MAX) {
				trace_cnt+=1;
				
				if(pval<prev_pval && prev_pval>0.) {
					Misc.logv("Trace p-value: %.2f<%.2f", pval, prev_pval);
					box_loca.setText(prev_loca);
					txt_desc.setText(prev_desc);
				}else {
					abacus.addMark(
						cmb_stng.getValue(),
						box_loca.getText(), 
						at5350
					);
					abacus.applyFitting();
					
					final String loca = abacus.predictLocation(
						cmb_stng.getValue(), 
						box_dose.getText()
					);
					box_loca.setText(loca);
					txt_desc.setText(desc);
					txt_desc.setUserData(at5350.lastMeasure());

					prev_pval = pval;
					prev_loca = loca;
					prev_desc = txt_desc.getText();
					
					next_step(this.op_foot,this.op_move_pallet);
				}
			}
			show_info(NAME, "TRY:"+trace_cnt, ptxt);
		}else {
			event_check_meas();
			event_check_mark();
			show_info(NAME, ptxt);
		}		
	};

	@Override
	public Node getContent() {
		show_info(NAME);
		GridPane lay = gen_layout();
		lay.add(chk_trac, 6, 2);
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
}
