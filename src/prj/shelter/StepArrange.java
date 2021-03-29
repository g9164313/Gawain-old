package prj.shelter;

import com.jfoenix.controls.JFXCheckBox;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
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
		set(op_2,op_3,op_4,op_5);
	}
		
	protected final Label title = new Label();
	protected final Label inform= new Label();
	
	//private JFXComboBox<String> act1 = new JFXComboBox<String>();	
	private JFXCheckBox act2 = new JFXCheckBox("原點回歸");
	private JFXCheckBox act3 = new JFXCheckBox("高壓補償");
	
	Runnable op_2 = ()->{
		if(act2.isSelected()==false){
			inform.setText("跳過回歸");
		}else{
			inform.setText("開始回歸");
			hustio.moveToAbs("");
		}
		next_step();
	};
	
	Runnable op_3 = ()->{				
		if(hustio.isMoving.get()==true){
			inform.setText("移動中");
			hold_step();
		}else{
			inform.setText("");
			next_step();
		}
	};
	
	Runnable op_4 = ()->{
		if(act3.isSelected()==false){
			inform.setText("跳過補償");
		}else{
			inform.setText("開始補償");
			at5350.compensate();
		}
		next_step();
	};
	
	Runnable op_5 = ()->{
		if(act3.isSelected()==false){
			next_step();
		}else {
			inform.setText(String.format(
				"等待 %s",
				Misc.tick2text(waiting_time(3*60*1000),true)
			));
		}
	};

	@Override
	public Node getContent() {
		
		title.setText("原點。補償");
		title.setPrefWidth(100.);
		
		act2.setSelected(true);
		act3.setSelected(true);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad","font-console");
		lay.addColumn(0,title,inform);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.addColumn(2,act2,act3);
		return lay;
	}

	@Override
	public void eventEdit() {
	}

	public void editValue(
		final String isotope,
		final boolean go_home,
		final boolean compenset
	){
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
