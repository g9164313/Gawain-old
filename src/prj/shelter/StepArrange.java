package prj.shelter;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class StepArrange extends Stepper {

	DevHustIO hustio;
	DevAT5350 at5350;
	
	public StepArrange(
		final DevHustIO dev1,
		final DevAT5350 dev2
	){
		hustio = dev1;
		at5350 = dev2;
		set(
			op_1,
			op_2,op_3,
			op_4,op_5
		);
	}
		
	private Label msg1 = new Label("");
	private JFXComboBox<String> act1 = new JFXComboBox<String>();
	private JFXCheckBox act2 = new JFXCheckBox("原點回歸");
	private JFXCheckBox act3 = new JFXCheckBox("高壓補償");
	
	Runnable op_1 = ()->{
		hustio.isotope.set(act1.getSelectionModel().getSelectedItem());
		next.set(1);
	};
	
	Runnable op_2 = ()->{
		if(act2.isSelected()==false){
			msg1.setText("跳過回歸");
		}else{
			msg1.setText("開始回歸");
			hustio.moveToAbs("");
		}
		next.set(LEAD);
	};
	
	Runnable op_3 = ()->{				
		if(hustio.isMoving()==true){
			msg1.setText("移動中");
			next.set(HOLD);
		}else{
			msg1.setText("");
			next.set(LEAD);
		}
	};
	
	Runnable op_4 = ()->{
		if(act3.isSelected()==false){
			msg1.setText("跳過補償");
		}else{
			msg1.setText("開始補償");
			at5350.compensate();
		}
		next.set(LEAD);		
	};
	
	Runnable op_5 = ()->{
		if(act3.isSelected()==false){
			next.set(LEAD);			
		}else {
			msg1.setText(String.format(
				"等待 %s",
				Misc.tick2text(waiting_time(3*60*1000),true)
			));
		}
	};

	@Override
	public Node getContent() {
		
		msg1.setPrefWidth(90.);
		
		act1.getItems().addAll("0.05Ci","0.5Ci","3Ci");
		act1.getSelectionModel().select(0);
		act2.setSelected(true);
		act3.setSelected(true);
		
		GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad");
		lay1.addColumn(0,act1,act2,act3);
		
		HBox lay0 = new HBox();
		lay0.getStyleClass().addAll("box-pad");
		lay0.getChildren().addAll(
			msg1,
			new Separator(Orientation.VERTICAL),
			lay1
		);
		return lay0;
	}

	@Override
	public void eventEdit() {
	}

	public void editValue(
		final String isotope,
		final boolean go_home,
		final boolean compenset
	){
		act1.getSelectionModel().select(isotope);
		act2.setSelected(go_home);
		act3.setSelected(compenset);
	}
	
	@Override
	public String flatten() {
		return null;
	}

	@Override
	public void expand(String txt) {
	}
}
