package prj.sputter.action;

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
import narl.itrc.Misc;
import narl.itrc.Stepper;
import prj.sputter.DevCouple;
import prj.sputter.DevSPIK2k;

public class StepSetting extends Stepper {

	public static DevCouple coup;
	public static DevSPIK2k spik;
	
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
		new Label("參數設定"), 
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
			/* TODO: spik.asyncSetPulse(
				Integer.valueOf(t_on_p), 
				Integer.valueOf(t_on_n), 
				Integer.valueOf(toff_p),
				Integer.valueOf(toff_n)
			);*/
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
}
