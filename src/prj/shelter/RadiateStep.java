package prj.shelter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.jfoenix.controls.JFXCheckBox;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public abstract class RadiateStep extends Stepper {

	public static DevHustIO hustio;
	public static DevAT5350 at5350;
	public static DevCDR06  cdr06;
	public static LayAbacus abacus;
	
	public final Label[] info = { 
		new Label(), 
		new Label(), 
		new Label(),
	};
	
	public RadiateStep() {
		for(Label obj:info) { 
			obj.setPrefWidth(100);
		}
		box_loca.setPrefWidth(150);
		box_dose.setPrefWidth(150);
		box_left.setPrefWidth(100);
		txt_desc.setMinWidth(600);
		chk_mark.disableProperty().bind(chk_meas.selectedProperty().not());		
	}

	protected ComboBox<DevHustIO.Strength> gen_act_combo() {
		final ComboBox<DevHustIO.Strength> box = new ComboBox<DevHustIO.Strength>();
		box.getItems().addAll(
			DevHustIO.Strength.V_005Ci,
			DevHustIO.Strength.V_05Ci,
			DevHustIO.Strength.V_3Ci
		);
		box.getSelectionModel().select(DevHustIO.Strength.V_005Ci);
		return box;
	}
	protected GridPane gen_layout() {
		GridPane lay = new GridPane();			
		lay.getStyleClass().add("box-pad");
		lay.addColumn(0,info);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		lay.addColumn(2, new Label("距離"), box_loca);
		lay.addColumn(3, new Label("劑量"), box_dose);
		lay.addColumn(4, new Label("時間"), box_left);
		lay.addColumn(5, new Label("強度"), cmb_stng);
		lay.add(chk_meas, 6, 0);
		lay.add(chk_mark, 6, 1);
		lay.add(txt_desc, 2, 2, 4, 1);
		return lay;
	};
	protected void show_info(String... txt) {
		for(int i=0; i<info.length; i++) {
			if(i<txt.length) {
				info[i].setText(txt[i]);
			}else {
				info[i].setText("");
			}			
		}
	}
	
	/*protected void event_update_loca() {
		final Runnable event = ()->{
			final Strength ss = box_stng.getValue();
			final String xx = box_dose.getText();
			final String yy = abacus.predictLocation(ss, xx);
			box_loca.setText(yy);
		};
		box_stng.selectionModelProperty().addListener((obv,oldVal,newVal)->event.run());
		box_dose.setOnAction(e->event.run());
	}
	protected void event_update_dose() {
		final Runnable event = ()->{
			final Strength ss = box_stng.getValue();
			final String xx = box_loca.getText();
			final String yy = abacus.predictDoseRate(ss, xx);
			box_dose.setText(yy);
		};
		box_stng.selectionModelProperty().addListener((obv,oldVal,newVal)->event.run());
		box_loca.setOnAction(e->event.run());
	}*/
	
	protected void event_check_meas() {
		if(chk_meas.isSelected()==false ) {
			return;
		}
		final String unit= "uSv/hr";			
		final SummaryStatistics stat = at5350.lastSummary();
		final String txt = String.format(
			"%s @ %.3f %s ± %.3f",
			hustio.locationText.get(),
			stat.getMean(), unit, 
			stat.getStandardDeviation()
		);
		txt_desc.setText(txt);
		txt_desc.setUserData(at5350.lastMeasure());
	}
	
	protected void event_check_mark() {
		if(chk_mark.isSelected()==false) {
			return;
		}
		abacus.addMark(
			cmb_stng.getValue(), 
			box_loca.getText(), 
			at5350
		);
		abacus.applyFitting();
	}
	
	protected final TextField box_loca = new TextField("100 cm");
	protected final TextField box_dose = new TextField("0 uSv/hr");
	protected final TextField box_left = new TextField("01:00");
	
	protected final ComboBox<DevHustIO.Strength> cmb_stng = gen_act_combo();
	
	protected final CheckBox chk_meas = new CheckBox("量測");
	protected final CheckBox chk_mark = new CheckBox("標定");
	protected final Label txt_desc = new Label();

	protected final Runnable op_move_pallet = ()->{		
		if(hustio.isMoving()==true) {
			show_info("移動中","busy!!");
			hold_step();
		}else {
			show_info("開始移動");
			hustio.asyncMoveTo(box_loca.getText());
			next_step();
		}
	};
	protected final Runnable op_wait_pallet = ()->{		
		if(hustio.isMoving()==true) {
			show_info("移動中","busy!!");
			hold_step();
		}else {
			final String txt = hustio.locationText.get();
			show_info("定位",txt);
			Misc.logv("[HOSTIO] move to %s", txt);
			next_step();
		}
	};
	
	private final int prewarm_tick = 7000;
	
	protected final Runnable op_make_radiation = ()->{
		final DevHustIO.Strength stng = cmb_stng.getSelectionModel().getSelectedItem();
		show_info(
			stng.toString(),
			"00:00:00",
			box_left.getText()
		);
		long tick = Misc.text2tick(box_left.getText());
		if(chk_meas.isSelected()==true) {
			tick = tick + prewarm_tick + 3000;//多加7秒的預熱跟3秒的結束。 
		}		
		hustio.asyncRadiation(stng,tick);
		next_step();
	};
	protected final Runnable op_prewarm = ()->{
		if(chk_meas.isSelected()==true) {
			final long rem = waiting_time(prewarm_tick);
			show_info(
				"預熱中",
				"剩 "+Misc.tick2text(rem, true)
			);
			if(rem>0) {
				hold_step();
			}else {
				next_step();
			}
		}else {
			next_step();
		}
	};
	protected final Runnable op_wait_radiation = ()->{
		if(hustio.isRadiant()==true) {
			final DevHustIO.Strength stng = cmb_stng.getSelectionModel().getSelectedItem();
			show_info(
				stng.toString(),
				Misc.tick2text(hustio.getLeftCount(), false, 3),
				box_left.getText()
			);
			hold_step();
		}else {
			next_step();
		}
	};
	
	protected final Runnable op_make_measure = ()->{
		next_step();
		if(chk_meas.isSelected()==true) {
			at5350.asyncMeasure(
				box_left.getText(),
				cdr06.getTxtTemperature(),
				cdr06.getTxtPression()
			);	
		}
	};
	protected final Runnable op_wait_measure = ()->{
		next_step();
		if(chk_meas.isSelected()==true) { 
			if(hustio.isRadiant()==true) {
				final DevHustIO.Strength stng = cmb_stng.getSelectionModel().getSelectedItem();
				show_info(
					stng.toString(),
					Misc.tick2text(hustio.getLeftCount(), false, 3),
					box_left.getText()
				);
			}
			if(at5350.isIdle.get()==false) {	
				hold_step();
			}else {
				box_loca.setUserData(box_loca.getText());
				box_dose.setUserData(box_dose.getText());
			}
		}	
	};
	//---------------------------------------
	
	public static class Reset extends RadiateStep {
		public Reset() {
			set(op_1,op_2);
		}

		final JFXCheckBox chk1 = new JFXCheckBox("HustIO原點");
		final JFXCheckBox chk2 = new JFXCheckBox("AT5350補償");
		final JFXCheckBox chk3 = new JFXCheckBox("清除標記");
		
		public Reset setValue(boolean... chk) {
			if(chk.length>=1) { chk1.setSelected(chk[0]); }
			if(chk.length>=2) { chk2.setSelected(chk[1]); }
			if(chk.length>=3) { chk3.setSelected(chk[2]); }
			return this;
		}
		
		final Runnable op_1 = ()->{
			if(chk1.isSelected()==true) {
				hustio.asyncMoveTo("");
			}
			if(chk2.isSelected()==true) {
				at5350.asyncCorrection();
			}
			if(chk3.isSelected()==true) {
				abacus.clearAllMark();
			}
			next_step();
		};
		final Runnable op_2 = ()->{
			next_step();
			if(chk1.isSelected() && hustio.isMoving()) {
				hold_step();
			}
			if(chk2.isSelected() && at5350.isIdle.get()==false) {
				hold_step();
			}
		};
		@Override
		public Node getContent() {
			show_info();
			
			chk1.setSelected(true);
			chk2.setSelected(true);
			chk3.setSelected(true);
			
			final GridPane lay = new GridPane();
			lay.getStyleClass().addAll("box-pad","font-console");
			lay.addColumn(0,info);
			lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
			lay.addColumn(2,chk1,chk2,chk3);
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
	//-----------------------------------

}
