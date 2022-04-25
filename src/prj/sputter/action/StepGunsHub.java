package prj.sputter.action;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class StepGunsHub extends Stepper {

	public StepGunsHub(){
		set(op_1,op_2);
		lay = init_layout();
	}
	
	private Pane lay;
	
	private Pane init_layout() {
		
		final Label msg1 = new Label("電極切換");
		msg1.setId("msg1");		
		msg1.setPrefWidth(150);
		
		final JFXRadioButton bipolar = new JFXRadioButton("Bipolar");
		final JFXRadioButton unipolar= new JFXRadioButton("Unipolar");
		final JFXCheckBox gun1 = new JFXCheckBox("Gun-1");
		final JFXCheckBox gun2 = new JFXCheckBox("Gun-2");
		
		bipolar.setOnAction(e->{
			fix_selected(gun1,true);
			fix_selected(gun2,true);
		});
		unipolar.setOnAction(e->{
			fix_selected(gun1,false);
			fix_selected(gun2,false);
		});
		
		bipolar.setId("bipolar");
		unipolar.setId("unipolar");
		gun1.setId("gun-1");
		gun2.setId("gun-2");
		
		final ToggleGroup grp = new ToggleGroup();		
		bipolar.setToggleGroup(grp);
		unipolar.setToggleGroup(grp);
				
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,msg1);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.addColumn(2,bipolar,unipolar);
		lay.add(new Separator(Orientation.VERTICAL), 3, 0, 1, 2);
		lay.addColumn(4,gun1,gun2);
		return lay;
	};
	
	final Runnable op_1 = ()->{
		JFXRadioButton rad1 = (JFXRadioButton)lay.lookup("#bipolar");
		JFXRadioButton rad2 = (JFXRadioButton)lay.lookup("#unipolar");
		JFXCheckBox chk1 = (JFXCheckBox)lay.lookup("#gun-1");
		JFXCheckBox chk2 = (JFXCheckBox)lay.lookup("#gun-2");
		btn_bipolar.setSelected(rad1.isSelected());
		btn_unipolar.setSelected(rad2.isSelected());
		chk_gun1.setSelected(chk1.isSelected());
		chk_gun2.setSelected(chk2.isSelected());
		select_gunhub();
		next_step();
	};
	final Runnable op_2 = ()->{
		next_step();//delay for e-gun ready
	};
	
	@Override
	public Node getContent() {
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	
	private static final String TAG0 = "unipolar";
	private static final String TAG1 = "gun-1";
	private static final String TAG2 = "gun-2";
	@Override
	public String flatten() {
		
		JFXRadioButton unipolar= (JFXRadioButton)lay.lookup("#unipolar");
		JFXCheckBox gun1 = (JFXCheckBox)lay.lookup("#gun-1");
		JFXCheckBox gun2 = (JFXCheckBox)lay.lookup("#gun-2");
		
		boolean[] flg = {
			unipolar.isSelected(),
			gun1.isSelected(),
			gun2.isSelected()
		};		
		return String.format(
			"%s:%b,  %s:%b,  %s:%b", 
			TAG0, flg[0], 
			TAG1, flg[1], 
			TAG2, flg[2]
		);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\p{Space}]+[:]\\p{ASCII}*[,\\s]?)+")==false){
			Misc.loge("pasing fail-->%s",txt);
			return;
		}
		
		JFXRadioButton bipolar = (JFXRadioButton)lay.lookup("#bipolar");
		JFXRadioButton unipolar= (JFXRadioButton)lay.lookup("#unipolar");
		JFXCheckBox gun1 = (JFXCheckBox)lay.lookup("#gun-1");
		JFXCheckBox gun2 = (JFXCheckBox)lay.lookup("#gun-2");
		
		String[] arg = txt.split(":|,");
		for(int i=0; i<arg.length; i+=2){
			final String tag = arg[i+0].trim();
			final String val = arg[i+1].trim();
			if(tag.equals(TAG0)==true){
				if(val.equals("true")){
					bipolar.setSelected(false);
					unipolar.setSelected(true);
				}else{
					bipolar.setSelected(true);
					unipolar.setSelected(false);
				}
			}else if(tag.equals(TAG1)==true){
				if(val.equals("true")){
					gun1.setSelected(true);
				}else{
					gun1.setSelected(false);
				}
			}else if(tag.equals(TAG2)==true){
				if(val.equals("true")){
					gun2.setSelected(true);
				}else{
					gun2.setSelected(false);
				}
			}
		}
	}

	//----------------------------------//
	
	private final static JFXRadioButton btn_bipolar = new JFXRadioButton("Bipolar");
	private final static JFXRadioButton btn_unipolar= new JFXRadioButton("Unipolar");
	private final static JFXCheckBox chk_gun1 = new JFXCheckBox("Gun-1");
	private final static JFXCheckBox chk_gun2 = new JFXCheckBox("Gun-2");
	
	private static void fix_selected(final CheckBox chk,final boolean flg) {
		chk.setSelected(flg);			
		chk.setDisable(flg);
		chk.setStyle("-fx-opacity: 1.0;");
	}
	private static void select_gunhub() {
		Bumper.coup.asyncSelectGunHub(
			btn_bipolar.isSelected(), 
			btn_unipolar.isSelected(), 
			chk_gun1.isSelected(), 
			chk_gun2.isSelected()
		);
	}
	
	public static Pane genPanel() {
		
		final ToggleGroup grp = new ToggleGroup();
		btn_bipolar.setToggleGroup(grp);
		btn_unipolar.setToggleGroup(grp);
		
		btn_bipolar.setOnAction(e->{
			fix_selected(chk_gun1,true);
			fix_selected(chk_gun2,true);
			chk_gun1.setOnAction(null);
			chk_gun2.setOnAction(null);
			select_gunhub();
		});		
		btn_unipolar.setOnAction(e->{
			fix_selected(chk_gun1,false);
			fix_selected(chk_gun2,false);
			chk_gun1.setOnAction(e1->select_gunhub());
			chk_gun2.setOnAction(e1->select_gunhub());
			select_gunhub();
		});
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, btn_bipolar, btn_unipolar);
		lay.addColumn(1, chk_gun1, chk_gun2);
		return lay;
	}
	//---------------------
	
	public static Pane genCtrlPanel() {
		
		final ToggleGroup grp = new ToggleGroup();
		btn_bipolar.setToggleGroup(grp);
		btn_unipolar.setToggleGroup(grp);
		
		btn_bipolar.setOnAction(e->{
			fix_selected(chk_gun1,true);
			fix_selected(chk_gun2,true);
			chk_gun1.setOnAction(null);
			chk_gun2.setOnAction(null);
			select_gunhub();
		});		
		btn_unipolar.setOnAction(e->{
			fix_selected(chk_gun1,false);
			fix_selected(chk_gun2,false);
			chk_gun1.setOnAction(e1->select_gunhub());
			chk_gun2.setOnAction(e1->select_gunhub());
			select_gunhub();
		});
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, btn_bipolar, btn_unipolar);
		lay.addColumn(1, chk_gun1, chk_gun2);
		return lay;
	}	
}
