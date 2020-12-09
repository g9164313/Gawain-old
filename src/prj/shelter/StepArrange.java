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
		set(op_1,op_2,op_3,op_4);
	}
		
	protected final Label title = new Label();
	protected final Label status= new Label();
	
	private JFXCheckBox act2 = new JFXCheckBox("回歸原點");
	private JFXCheckBox act3 = new JFXCheckBox("高壓補償");
	
	Runnable op_1 = ()->{
		if(act2.isSelected()==false){
			status.setText("忽略原點");
			next_step(2);
		}else{
			status.setText("回歸原點");
			hustio.moveToAbs("");
			next_step();
		}
	};
	
	Runnable op_2 = ()->{				
		if(hustio.isMoving.get()==true){
			status.setText("移動中");
			hold_step();
		}else{
			status.setText("");
			next_step();
		}
	};
	
	Runnable op_3 = ()->{
		if(act3.isSelected()==false){
			status.setText("忽略補償");
			next_step(2);
		}else{
			status.setText("開始補償");
			at5350.compensate();
			next_step();
		}		
	};
	
	Runnable op_4 = ()->{
		if(at5350.isAsyncDone()==false){
			status.setText("補償中");
			hold_step();
		}else {
			status.setText("");
			next_step();
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
		lay.addColumn(0,title,status);
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
