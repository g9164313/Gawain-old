package prj.shelter;

import javafx.scene.Node;
import narl.itrc.Misc;

public class RadiateLoca extends RadiateStep {
	
	public static final String NAME = "定點照射";

	public RadiateLoca() {
		box_loca.setOnAction(e->{
			box_dose.setText(abacus.predictDoseRate(
				cmb_stng.getValue(), 
				box_loca.getText()
			));
		});
		box_dose.setDisable(true);		
		set(op_init, 
			op_move_pallet, op_wait_pallet, 
			op_make_radiation, op_prewarm,
			op_make_measure, op_wait_measure,
			op_wait_radiation,
			op_foot
		);
	}
	
	public RadiateLoca setValues(
		final String loca,
		final String left,
		final DevHustIO.Strength stng,
		final boolean meas,
		final boolean mark
	) {
		box_loca.setText(loca);
		box_dose.setText(abacus.predictDoseRate(stng,loca));
		box_left.setText(left);
		cmb_stng.setValue(stng);
		chk_meas.setSelected(meas);
		chk_mark.setSelected(mark);
		return this;
	}
	
	final Runnable op_init = () -> {
		box_loca.getOnAction().handle(null);
		next_step();
	};
	final Runnable op_foot = () -> {
		Misc.logv("[RadiateLoca] foot");
		event_check_meas();
		event_check_mark();
		next_step();
		show_info(NAME);
	};

	@Override
	public Node getContent() {
		show_info(NAME);
		return gen_layout();
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
