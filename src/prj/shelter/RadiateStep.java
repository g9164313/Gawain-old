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
import narl.itrc.PanDialog;
import narl.itrc.Stepper;
import prj.shelter.DevHustIO.Strength;

public abstract class RadiateStep extends Stepper {

	public static DevHustIO hustio;
	public static DevAT5350 at5350;
	public static DevCDR06  cdr06;
	public static ManBooker booker;
	
	public final Label[] info = { 
		new Label(), 
		new Label(), 
		new Label(),
	};
	
	public RadiateStep() {
		for(Label obj:info) { 
			obj.setPrefWidth(80);
		}
		box_loca.setPrefWidth(130);
		box_dose.setPrefWidth(130);
		box_left.setPrefWidth(100);
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
		lay.add(chk_trac, 6, 2);
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
	
	protected final ComboBox<DevHustIO.Strength> cmb_stng = gen_act_combo();
	
	protected final TextField box_loca = new TextField("100 cm");
	protected final TextField box_dose = new TextField("100 uSv/hr");
	protected final TextField box_left = new TextField("01:00");
	
	protected final CheckBox chk_meas = new CheckBox("量測");
	protected final CheckBox chk_mark = new CheckBox("標定");
	protected final CheckBox chk_trac = new CheckBox("追溯");
	
	protected final Label txt_desc = new Label();
	
	//----------------------------------------//
	private final static String TAG0 = "loca";
	private final static String TAG1 = "dose";
	private final static String TAG2 = "left";
	
	private final static String TAG4 = "meas";
	private final static String TAG5 = "mark";
	private final static String TAG6 = "trac";
	
	protected String event_flatten_var() {		
		return String.format(
			"%s=%s, %s=%s, %s=%s,  %s=%s, %s=%s, %s=%s,",
			TAG0, box_loca.getText().trim(),
			TAG1, box_dose.getText().trim(),
			TAG2, box_left.getText().trim(),
			TAG4, (chk_meas.isSelected())?("Y"):("N"),
			TAG5, (chk_mark.isSelected())?("Y"):("N"),
			TAG6, (chk_trac.isSelected())?("Y"):("N")
		);
	}	
	protected void event_expand_var(final String flat) {
		if(flat.matches("([^:,\\p{Space}]+[=]\\p{ASCII}*[,\\s]?)+")==false){
			Misc.loge("pasing fail-->%s",flat);
			return;
		}
		final String[] lst = flat.split(",");
		for(String txt:lst){
			final String[] var = txt.split("=");
			final String tag = var[0].trim();
			final String val = var[1].trim();
			if(tag.equals(TAG0)==true){
				box_loca.setText(val);
			}else if(tag.equals(TAG1)==true){
				box_dose.setText(val);
			}else if(tag.equals(TAG2)==true){
				box_left.setText(val);
			}else if(tag.equals(TAG4)==true){
				val2chk(val,chk_meas);
			}else if(tag.equals(TAG5)==true){
				val2chk(val,chk_mark);
			}else if(tag.equals(TAG6)==true){
				val2chk(val,chk_trac);				
			}
		}
	}
	private static void val2chk(final String v, final CheckBox chk) {
		if(v.charAt(0)=='Y') {
			chk.setSelected(true);
		} else {
			chk.setSelected(false);
		}
	}
	//----------------------------------------//
	
	protected final Runnable op_move_pallet = ()->{
		hustio.asyncMoveTo(box_loca.getText());
		final long rem = waiting_time(1500);
		if(rem>0) {
			hold_step();
		}else {
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
			Misc.logv("[HOSTIO] hold in %s", txt);
			next_step();
		}
	};
	
	private final int prelogue_tick = 4000;
	private final int epilogue_tick = 2000;
	
	protected final Runnable op_make_radiate = ()->{
		final DevHustIO.Strength stng = cmb_stng.getValue();
		show_info(
			stng.toString(),
			"00:00:00",
			box_left.getText()
		);
		long tick = Misc.text2tick(box_left.getText());
		if(chk_meas.isSelected()==true) {
			tick = prelogue_tick + tick + epilogue_tick;
		}		
		hustio.asyncRadiation(stng,tick);
		next_step();
	};
	protected final Runnable op_prewarm = ()->{
		if(chk_meas.isSelected()==true) {
			final long rem = waiting_time(prelogue_tick);
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
			final DevHustIO.Strength stng = cmb_stng.getValue();
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
		if(chk_meas.isSelected()==false) {
			return;
		}
		String filt_var = "600";//60sec
		String rang_var = "LOW";
		final Strength ss = hustio.activity.get();
		final float dd = hustio.get_location_cm();
		
		if(ss==Strength.V_005Ci && Math.round(dd-150.)>=0) {
			//In this position, dose rate is too weak!!!
			filt_var = "1200";//120sec
		}else if(ss==Strength.V_3Ci && Math.round(50.-dd)>=0) {
			//In this position, dose rate is too strong!!!
			rang_var = "MED";
		}
		Misc.logv("[AT5350] measurement");
		at5350.asyncMeasure(
			box_left.getText(),
			filt_var,
			rang_var,
			cdr06.getTxtTemperature(),
			cdr06.getTxtPression(),
			null
		);
	};
	protected final Runnable op_wait_measure = ()->{
		next_step();		
		if(chk_meas.isSelected()==true) { 
			if(hustio.isRadiant()==true) {
				show_info(
					cmb_stng.getValue().toString(),
					Misc.tick2text(hustio.getLeftCount(), false, 3),
					box_left.getText()
				);
			}
			if(at5350.isIdle.get()==false) {	
				hold_step();
			}else {
				Object[] res = at5350.lastResult();				
				txt_desc.setUserData(res);
				final SummaryStatistics ss = (SummaryStatistics)res[1];
				final double avg = ss.getMean();
				final double scv = ss.getStandardDeviation();
				final double pcv = (Double.isNaN(scv)||scv==0.)?(-1.):((scv*100.)/avg);
				txt_desc.setText(String.format(
					"AVG:%.3f %s ± %.3f, %%S:%.1f", 
					avg, "uSv/hr", scv, pcv
				));
			}
		}	
	};
	//---------------------------------------
	
	protected static void show_measure(Label txt_desc) {		
		Object[] res = (Object[])txt_desc.getUserData();
		if(res==null) {
			return;
		}
		final String txt = 
			txt_desc.getText() + 
			"\n------------\n" +
			((String)res[0]);
		new PanDialog.ShowTextArea(txt)
		.setPrefSize(100, 400)
		.showAndWait();
	}
		
	public static class Reset extends RadiateStep {
		public Reset() {
			set(op_1,op_2);
		}

		final JFXCheckBox chk1 = new JFXCheckBox("HustIO原點");
		final JFXCheckBox chk2 = new JFXCheckBox("AT5350補償");

		public Reset setValue(boolean... chk) {
			if(chk.length>=1) { chk1.setSelected(chk[0]); }
			if(chk.length>=2) { chk2.setSelected(chk[1]); }
			return this;
		}
		
		final Runnable op_1 = ()->{
			next_step();
			String[] txt = {"",""};
			if(chk1.isSelected()==true) {
				hustio.asyncMoveTo("");
				txt[0] = "回歸中";
			}
			if(chk2.isSelected()==true) {
				at5350.asyncCorrection();
				txt[1] = "補償中";
			}
			show_info(txt);			
		};
		final Runnable op_2 = ()->{
			next_step();
			String[] txt = {"",""};
			if(chk1.isSelected() && hustio.isMoving()) {
				hold_step();
				txt[0] = "回歸中";
			}
			if(chk2.isSelected() && at5350.isIdle.get()==false) {
				hold_step();
				txt[1] = "補償中";
			}
			show_info(txt);
		};
		@Override
		public Node getContent() {
			show_info();
			chk1.setSelected(true);
			chk2.setSelected(true);
			final GridPane lay = new GridPane();
			lay.getStyleClass().addAll("box-pad","font-console");
			lay.addColumn(0,info[0],info[1]);
			lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
			lay.addColumn(2,chk1,chk2);
			return lay;
		}
		@Override
		public void eventEdit() {
		}
		@Override
		public String flatten() {
			return String.format("%s%s",
				(chk1.isSelected())?("Y"):("N"),
				(chk2.isSelected())?("Y"):("N")
			);
		}
		@Override
		public void expand(String txt) {
			if(txt.length()<2) {
				return;
			}
			val2chk(""+txt.charAt(0),chk1);
			val2chk(""+txt.charAt(1),chk2);
		}
	};
	//-----------------------------------

}
