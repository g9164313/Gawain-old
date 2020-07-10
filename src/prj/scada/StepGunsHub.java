package prj.scada;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

import narl.itrc.DevModbus;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class StepGunsHub extends Stepper {

	private DevModbus coup;
	
	public StepGunsHub(final DevModbus dev){
		coup = dev;
		set(op_1,op_2,op_3);
	}
	
	private final static String init_txt = "電極切換";
	private Label msg1 = new Label(init_txt);
	private Label msg2 = new Label("");
	
	private JFXRadioButton bipolar = new JFXRadioButton("Bipolar");
	private JFXRadioButton unipolar = new JFXRadioButton("Unipolar");
	private JFXCheckBox gun1 = new JFXCheckBox("Gun-1");
	private JFXCheckBox gun2 = new JFXCheckBox("Gun-2");
	
	final Runnable op_1 = ()->{
		waiting_async();
		final boolean use_bipolar = bipolar.isSelected();
		final boolean use_gun1 = gun1.isSelected();
		final boolean use_gun2 = gun2.isSelected();
		msg1.setText("設定電極");
		msg2.setText("");
		coup.asyncBreakIn(()->{
			int v = 0;
			if(use_bipolar==true){
				v = 0x1;
			}else{
				v = 0x2;
				if(use_gun1){
					v = v | 0x4;
				}
				if(use_gun2){
					v = v | 0x8;
				}
			}
			coup.writeVal(8005, v);
			next.set(LEAD);
		});
	};
	
	final Runnable op_2 = ()->{
		//wait e-gun ready
		msg1.setText("等待中");
		msg2.setText(String.format(
			"剩餘  %s",
			Misc.tick2time(waiting(3000),true)
		));
	};
	
	final Runnable op_3 = ()->{
		msg1.setText(init_txt);
		msg2.setText("");
	};
	
	@Override
	public Node getContent(){

		msg1.setPrefWidth(150);
		msg2.setPrefWidth(150);
		//default selection
		unipolar.setSelected(true);
		gun1.setSelected(true);

		final ToggleGroup grp = new ToggleGroup();
		bipolar.setToggleGroup(grp);
		unipolar.setToggleGroup(grp);
		gun1.disableProperty().bind(unipolar.selectedProperty().not());
		gun2.disableProperty().bind(unipolar.selectedProperty().not());
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.add(msg1, 0, 0);
		lay.add(msg2, 0, 1);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.add(bipolar, 2, 0);		
		lay.add(new Separator(Orientation.VERTICAL), 3, 0, 1, 2);
		lay.add(unipolar, 4, 0, 2, 1);
		lay.add(gun1, 4, 1);
		lay.add(gun2, 5, 1);
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
		if(txt.matches("([^:,\\s]+[:][^:,]+[,]?[\\s]*)+")==false){
			return;
		}
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
}
