package prj.sputter;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sun.glass.ui.Application;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.Stepper;

public class StepSetFilm extends Stepper {

	public static final String action_name = "薄膜設定";
	
	private DevSQM160 sqm1;
	
	final TextField box_stpoint = new TextField("0.5");//thick set-point(also final thick)
	
	final CheckBox chk_fild_id = new CheckBox("標號/自訂");
	final ComboBox<Integer> cmb_film_id = new ComboBox<Integer>();
	
	final TextField box_density = new TextField("50");//0.50 to 99.99 (g/cm³
	final TextField box_tooling = new TextField("100");//10 to 399 (%
	final TextField box_z_ratio = new TextField("1");//0.100~9.999
	final TextField box_sensors = new TextField("1-6");//bit map for sensor
	
	public StepSetFilm(final DevSQM160 dev) {
		sqm1 = dev;
		for(TextField box:new TextField[] {
			box_stpoint, 
			box_density, box_tooling, 
			box_z_ratio, box_sensors,
		}) {
			box.setPrefWidth(83);
		}
		cmb_film_id.setPrefWidth(100);
		for(int i=1; i<99; i++) {
			cmb_film_id.getItems().add(i);
		}
		cmb_film_id.getSelectionModel().select(0);
		cmb_film_id.disableProperty().bind(chk_fild_id.selectedProperty().not());

		box_density.disableProperty().bind(chk_fild_id.selectedProperty());
		box_tooling.disableProperty().bind(chk_fild_id.selectedProperty());
		box_z_ratio.disableProperty().bind(chk_fild_id.selectedProperty());
		box_sensors.disableProperty().bind(chk_fild_id.selectedProperty());
		
		chk_fild_id.setSelected(true);
		
		set(op1);
	}
		
	final Label[] msg = {
		new Label(), 
		new Label(),
	};
	
	final String check_float(
		final String txt,		
		final Float min,
		final Float max,
		final String fmt
	) {
		try {
			float val = Float.parseFloat(txt);
			if(min!=null) { if(val<min) { return String.format(fmt, min); } }
			if(max!=null) { if(max<val) { return String.format(fmt, max); } }
		}catch(NumberFormatException e) {
			return null;
		}
		return txt;
	}
	final String check_int(
		final String txt,
		final Integer min,
		final Integer max
	) {
		try {
			int val = Integer.parseInt(txt);
			if(min!=null) { if(val<min) { return ""+min; } }
			if(max!=null) { if(max<val) { return ""+max; } }
		}catch(NumberFormatException e) {
			return null;
		}
		return txt;
	}
	
	final Runnable op1 = ()->{
		msg[1].setText("apply!!");
		wait_async();
		final char id = (chk_fild_id.isSelected())?(
			(char)(48+cmb_film_id.getSelectionModel().getSelectedItem())
		):(
			(char)(48+99)
		);
		final String[] arg = {
			null,//always same, skip it, 'A[id][8 character name]'
			check_float(box_density.getText(),0.5f,99.99f,"%.2f"),//density (0.50~99.99 g/cm3 %.2f)
			check_int  (box_tooling.getText(),10  ,399),//tooling (10~399)
			check_float(box_z_ratio.getText(),0.1f,9.99f,"%.3f"),//z_ratio (0.10~9.999 %.3f)
			check_float(box_stpoint.getText(),0f  ,9999f,"%.3f"),//Final Thickness  (0.000~9999.000 %.3f)
			check_float(box_stpoint.getText(),0f  ,9999f,"%.3f"),//Set-point Thickness (0.000~9999.000 %.3f)
			null,//Set-point Time (0 to 5999 second)
			""+Misc.txt2bit_val(box_sensors.getText(),-1),//sensor bits (integer)
		};
		
		sqm1.asyncBreakIn(()->{
			String txt = sqm1.exec(String.format("A%c?", id));
			if(txt.charAt(0)=='A'){
				//modify arguments, name is special, replace space character
				txt=txt.substring(1, 9).replace(' ', '_').trim()+" "+
					txt.substring(9).trim();
				final String[] val = txt.split("\\s+");
				for(int i=0; i<val.length;i++) {
					if(arg[i]!=null) {
						val[i] = arg[i];
					}
				}
				//update commands
				final String cmd = "A"+id+String.join(" ", val);
				if(sqm1.exec(cmd).charAt(0)!='A') {
					Application.invokeLater(()->msg[1].setText("無法更新"));
				}else {
					if(sqm1.exec(String.format("D%c",id)).charAt(0)!='A'){
						Application.invokeLater(()->msg[1].setText("無法設定"));
					}else {
						Application.invokeLater(()->msg[1].setText(""));
					}
				}		
			}else{
				Application.invokeLater(()->msg[1].setText("無法讀取"));
			}
			notify_async();
		});
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
		lay.addRow(0,
			new Label("最終厚度(kÅ):"), box_stpoint
		);
		lay.add(new HBox(chk_fild_id,cmb_film_id), 2, 1, 2, 1);
		
		lay.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 2);
		lay.addRow(0, 
			new Label("密度(g/cm³):"), box_density,
			new Label("Tooling(%):"), box_tooling
		);
		lay.addRow(1,
			new Label("Z-ratio:"), box_z_ratio,
			new Label("感測器編號"), box_sensors
		);
		return lay;
	}

	@Override
	public void eventEdit() {
		ChoiceDialog<Integer> dia = new ChoiceDialog<Integer>(
			1,
			IntStream.rangeClosed(1, 99).boxed().collect(Collectors.toList())
		);
		dia.setTitle("讀取薄膜設定");
		dia.setHeaderText("依照編號讀取設定");
		dia.setContentText("薄膜編號:");
		Optional<Integer> opt = dia.showAndWait();
		if(opt.isPresent()==false) {
			return;
		}
		cmb_film_id.getSelectionModel().select(opt.get());
		
		final char id = (char)(48+opt.get());		
		sqm1.asyncBreakIn(()->{
			final String txt = sqm1.exec(String.format("A%c?", id));
			if(txt.charAt(0)=='A'){
				//restore film data to GUI box~~~
				Application.invokeAndWait(()->{
					//remove name
					final String[] val = txt.substring(9).trim().split("\\s+");
					box_density.setText(val[0]);//Density
					box_tooling.setText(val[1]);//Tooling
					box_z_ratio.setText(val[2]);//Z-Ratio
					//Final Thickness ~ 0.000 to 9999.000 (kÅ)
					box_stpoint.setText(val[4]);//Thickness Setpoint ~ 0.000 to 9999 (kÅ)
					//Time Setpoint ~ 0:00 to 99:59 (mm:ss)
					box_sensors.setText(val[6]);//Sensor Average
				});
			}else{
				Application.invokeAndWait(()->PanBase.notifyError("錯誤","無法讀取薄膜設定!!"));
			}				
		});
	}
	@Override
	public String flatten() {
		return control2text(
			box_stpoint, chk_fild_id, cmb_film_id, 
			box_density, box_tooling, box_z_ratio, box_sensors
		);
	}
	@Override
	public void expand(String txt) {
		text2control(txt,
			box_stpoint, chk_fild_id, cmb_film_id, 
			box_density, box_tooling, box_z_ratio, box_sensors
		);
	}
}
