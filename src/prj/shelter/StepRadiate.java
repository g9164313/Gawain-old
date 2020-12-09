package prj.shelter;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class StepRadiate extends Stepper {

	protected static int TAG_COUNT = 0;
	
	protected DevHustIO hustio;
	
	public StepRadiate(
		final DevHustIO dev
	){
		hustio = dev;
		addRun(op_move, op_wait, op_action, op_standby);
		title.setText("定點照射");
		TAG_COUNT+=1;
	}
	
	protected final Label title = new Label();
	protected final Label inform= new Label();
	protected final Label status= new Label();
	
	protected final ComboBox<String>  cmb_radi = new ComboBox<String>();
	protected final TextField txt_loca = new TextField();
	protected final TextField txt_time = new TextField();
	
	Runnable op_move = ()->{
		hustio.moveToAbs(txt_loca.getText());
		next_step();
	};	
	Runnable op_wait = ()->{
		if(hustio.isMoving.get()==true) {
			status.setText("移動中");
			hold_step();
		}else {
			status.setText("");
			next_step();
		}
	};
	Runnable op_action = ()->{
		String isotope=""; 
		switch(cmb_radi.getSelectionModel().getSelectedIndex()) {
		case 0: isotope = DevHustIO.ISOTOPE_005Ci; break;
		case 1: isotope = DevHustIO.ISOTOPE_05Ci; break;
		case 2: isotope = DevHustIO.ISOTOPE_3Ci; break;
		default: next_step(); return;
		}
		hustio.makeRadiation(
			isotope,
			(int)Misc.time2tick(txt_time.getText())
		);
		next_step();
	};
	Runnable op_standby = ()->{
		if(hustio.isRadiant.get()==true) {
			status.setText("照射中");
			hold_step();
		}else {
			status.setText("");
			next_step();
		}
	};
	
	@Override
	public Node getContent() {
		
		title.setPrefWidth(100.);
		
		cmb_radi.setMaxWidth(Double.MAX_VALUE);
		cmb_radi.getItems().addAll("0.05Ci","0.5Ci","3Ci");
		cmb_radi.getSelectionModel().select(0);
		
		txt_loca.setPrefWidth(53);
		txt_time.setPrefWidth(53);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad","font-console");
		lay.addColumn(0, 
			title,
			inform,
			status			
		);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);
		lay.addColumn(2, 
			new Label("源種"),
			new Label("位置(cm/mm)"),
			new Label("時間(時:分:秒)")
		);
		lay.addColumn(3, 
			cmb_radi,
			txt_loca,
			txt_time
		);
		lay.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 3);
		return lay;
	}

	@Override
	public void eventEdit() {
	}

	@Override
	public String flatten() {
		return null;
	}

	@Override
	public void expand(String txt) {
	}
}
