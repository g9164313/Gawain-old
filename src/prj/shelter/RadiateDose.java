package prj.shelter;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;

public class RadiateDose extends RadiateStep {
	
	public static final String NAME = "劑量照射";
	
	public RadiateDose() {
		box_loca.setDisable(true);		
		chk_trac.selectedProperty().addListener((obv,oldVal,newVal)->{
			if(newVal.booleanValue()==true){
				chk_meas.setSelected(true);
			}
		});
		set(op_init,
			op_move_pallet, op_wait_pallet, 
			op_make_radiation, 
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
		//box_loca.setText(loca);//TODO: fitting value
		box_dose.setText(dose);
		box_left.setText(left);
		box_stng.setValue(stng);
		chk_meas.setSelected(meas);
		chk_mark.setSelected(mark);
		chk_trac.setSelected(trac);
		return this;
	}
	
	final Runnable op_init = () -> {

		next_step();
	};
	final Runnable op_foot = () -> {
		if(chk_trac.isSelected()==true) {
			DescriptiveStatistics ss1 = new DescriptiveStatistics();
			DescriptiveStatistics ss2 = new DescriptiveStatistics();
			TestUtils.tTest(ss1, ss2);
			next_step(this.op_foot,this.op_init);
		}else {
			show_info(NAME);
			event_add_mark();
			next_step();
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
