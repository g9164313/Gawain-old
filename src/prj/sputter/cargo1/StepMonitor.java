package prj.sputter.cargo1;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;

public class StepMonitor extends StepCommon {

	public static final String action_name = "薄膜監控";

	public static class FilmID {
		final int id;
		FilmID(int val){
			id = val;
		}
		public String toString() { return String.format("Film-%02d", id); }
		public char getID() { return (char)(0+48); }
		public int getVal() { return id; }
	};
	
	final TextField box_setp1 = new TextField("10");//thick set-point(also final thick)
	final TextField box_setp2 = new TextField("00:00");//clock set-point
	
	final RadioButton  chk_idfy = new RadioButton("編號");
	final RadioButton  chk_args = new RadioButton("自訂");

	final ComboBox<FilmID> cmb_idfy = new ComboBox<FilmID>();
	
	final TextField box_density = new TextField("50");//0.50 to 99.99 (g/cm³
	final TextField box_tooling = new TextField("100");//10 to 399 (%
	final TextField box_z_ratio = new TextField("1");//0.100~9.999
	final TextField box_sensors = new TextField("1-6");//bit map for sensor
	
	public StepMonitor() {
		box_setp1.setPrefWidth(97);
		box_setp2.setPrefWidth(97);
		
		for(int i=1; i<100; i++) {
			cmb_idfy.getItems().add(new FilmID(i));
		}
		cmb_idfy.getSelectionModel().select(0);
		cmb_idfy.setMaxWidth(Double.MAX_VALUE);
		
		ToggleGroup grp = new ToggleGroup();
		chk_idfy.setToggleGroup(grp);
		chk_args.setToggleGroup(grp);
		grp.selectToggle(chk_idfy);

		cmb_idfy.disableProperty().bind(chk_idfy.selectedProperty().not());		
		for(TextField obj:new TextField[] {
			box_density,box_tooling,box_z_ratio,box_sensors
		}) {
			obj.setPrefWidth(97);
			obj.disableProperty().bind(chk_args.selectedProperty().not());
		}
		
		set(
			op1,run_waiting( 500,msg[1]),
			op2,run_waiting(1000,msg[1]),
			op3,
			op4,run_waiting(2000,msg[1])
		);
	}
	
	final Runnable op1 = ()->{
		//apply Film setting
		next_step();
		if(chk_idfy.isSelected()==true) {
			sqm1.activeFilm(
				cmb_idfy
				.getSelectionModel()
				.getSelectedItem()
				.getVal()
			);
		}else if(chk_args.isSelected()==true) {
			sqm1.activeFilm(
				99, "O_TEMP_O", 
				Misc.txt2float(box_density.getText(),-1f), 
				Misc.txt2int(box_tooling.getText(),-1), 
				Misc.txt2float(box_z_ratio.getText(),-1f),
				Misc.txt2float(box_setp1.getText(),-1), 
				box_setp2.getText(), 
				Misc.txt2bit_val(box_sensors.getText(),-1)
			);
		}else {
			Misc.loge("[%s] impossible!!",action_name);
			abort_step();
		}		
	};	
	final Runnable op2 = ()->{
		//open shutter~~~
		next_step();
		adam2.asyncSetRelayAll(false, false, false);		
	};
	final Runnable op3 = ()->{
		//wait indicator~~~
		hold_step();
		if(spik.ARC_count.get()>=10) {
			abort_step();
			PanMain.douse_fire();
		}
		if(adam1.DI[3].get()) {
			//close all shutter!!!
			adam2.asyncSetRelayAll(true, true, true);
			next_step();
		}
	};
	final Runnable op4 = ()->{
		//shutdown all powers~~~
		next_step();
		PanMain.douse_fire();
	};

	@Override
	public Node getContent() {
		msg[0].setText(action_name);
		msg[0].setMinWidth(100.);		
		msg[1].setMinWidth(100.);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,msg);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.addColumn(2, new Label("目標厚度(kÅ)"), new Label("目標時間(mm:ss)"));
		lay.addColumn(3,box_setp1, box_setp2);
		lay.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 2);
		lay.addColumn(5,chk_idfy, chk_args);
		lay.add(cmb_idfy, 6, 0, 2, 1);
		lay.addRow(1, 
			new Label("密度(g/cm³):"), box_density,
			new Label("Tooling(%):"), box_tooling,
			new Label("Z-ratio:"), box_z_ratio,
			new Label("感測器編號"), box_sensors
		);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return control2text(
			box_setp1, box_setp2,
			chk_idfy, cmb_idfy,
			chk_args, box_density, box_tooling, box_z_ratio, box_sensors
		);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,
			box_setp1, box_setp2,
			chk_idfy, cmb_idfy,
			chk_args, box_density, box_tooling, box_z_ratio, box_sensors
		);
	}
}
