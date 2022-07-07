package prj.shelter;

import javafx.scene.Node;

public class RadiateLoca extends RadiateStep {
	
	public static final String NAME = "定點照射";

	public RadiateLoca() {
		box_dose.setDisable(true);		
		set(op_init, 
			op_move_pallet, op_wait_pallet, 
			op_make_radiation, 
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
		//box_dose.setText(value);//TODO: fitting value
		box_left.setText(left);
		box_stng.setValue(stng);
		chk_meas.setSelected(meas);
		chk_mark.setSelected(mark);
		return this;
	}
	
	final Runnable op_init = () -> {
		next_step();
	};
	final Runnable op_foot = () -> {
		show_info(NAME);
		event_add_mark();
		next_step();
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
