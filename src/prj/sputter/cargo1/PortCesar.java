package prj.sputter.cargo1;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import prj.sputter.DevAdam4024;
import prj.sputter.DevAdam4068;
import prj.sputter.LayHelper;

/**
 * support for Cesar 136 USER Port.<p>  
 * @author qq
 *
 */
public class PortCesar extends GridPane {

	final CheckBox chk_ready= PanBase.genIndicator("Ready");
	final CheckBox chk_error= PanBase.genIndicator("Error");
	final CheckBox chk_rungo= PanBase.genIndicator("RF on");
	
	public final Label txt_forward = new Label();
	public final Label txt_reflect = new Label();
	final Label txt_rf_setp = new Label();
	final Label txt_dc_setp = new Label();
	
	final DevAdam4068 adam2 = PanMain.adam2;//ADAM4068, Relay module
	final DevAdam4024 adam5 = PanMain.adam5;
	
	final int MODEA, RF_ON, SV_RF, SV_DC;
	
	final float GND_BIAS = 0.045f;
	
	public PortCesar(final int... pin) {
		
		MODEA = pin[0];
		RF_ON = pin[1];
		SV_DC = pin[2];
		SV_RF = pin[3];
		
		txt_rf_setp.textProperty().bind(adam5.aout[SV_RF].val.subtract(GND_BIAS).multiply( 600f).divide(10f).asString("%.2f"));
		txt_dc_setp.textProperty().bind(adam5.aout[SV_DC].val.subtract(GND_BIAS).multiply(4000f).divide(10f).asString("%.2f"));
		
		for(Label obj:new Label[] {txt_forward, txt_reflect, txt_rf_setp, txt_dc_setp}) {
			obj.setMaxWidth(Double.MAX_VALUE);
			obj.setAlignment(Pos.CENTER_RIGHT);
			GridPane.setHgrow(obj, Priority.ALWAYS);
		}
		
		final Button btn_set = new Button("設定輸出");
		btn_set.setMaxWidth(Double.MAX_VALUE);
		btn_set.setOnAction(e->{
			//TODO: get analogy-output value~~~
			final TextField box_pw = new TextField(txt_rf_setp.getText());
			final TextField box_dc = new TextField(txt_dc_setp.getText());
			GridPane lay = new GridPane();
			lay.setAlignment(Pos.CENTER);
			lay.setHgap(10);
			lay.setVgap(10);
			lay.setPadding(new Insets(25, 25, 25, 25));
			lay.addRow(0, new Label("RF輸出(W)"), box_pw);
			lay.addRow(1, new Label("DC偏壓(V)"), box_dc);
			
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle("Set Point");
			dia.setHeaderText("設定 RF輸出/DC偏壓");
			dia.getDialogPane().setContent(lay);
			if(dia.showAndWait().get()==ButtonType.OK) {
				apply_setpoint(
					Misc.txt2Float(box_pw.getText()),
					Misc.txt2Float(box_dc.getText())
				);
			}			
		});
		
		final Button btn_run = new Button("開啟/關閉");
		btn_run.setMaxWidth(Double.MAX_VALUE);
		btn_run.setOnAction(e->{
			final boolean flag = adam2.Relay[RF_ON].get();
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle("RF On/Off");
			dia.setHeaderText("RF output");
			dia.setContentText((flag==true)?("關閉 RF輸出"):("開啟 RF輸出"));
			if(dia.showAndWait().get()==ButtonType.OK) {
				set_RF_fire(!flag);
			}			
		});
		
		final String TXT_REMOTE= "遠端";
		final String TXT_LOCAL = "面板";
		final Button btn_remote = new Button((adam2.Relay[MODEA].get()==true)?(TXT_REMOTE):(TXT_LOCAL));
		btn_remote.setMaxWidth(Double.MAX_VALUE);
		btn_remote.setOnAction(e->{
			final boolean flag = adam2.Relay[MODEA].get();
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle("選擇遠端/面板控制");
			dia.setHeaderText((flag==true)?("Remote control"):("Local control"));
			dia.setContentText((flag==true)?("面板控制"):("遠端控制"));
			if(dia.showAndWait().get()==ButtonType.OK) {
				set_onoff(!flag);
			}			
		});
		adam2.Relay[MODEA].addListener((obv,oldVal,newVal)->{
			if(newVal.booleanValue()==true) {
				btn_remote.setText(TXT_REMOTE);
			}else {
				btn_remote.setText(TXT_LOCAL);
			}
		});
		
		HBox lay0 = new HBox(chk_ready,chk_rungo,chk_error);
		lay0.getStyleClass().addAll("box-pad");
		
		getStyleClass().addAll("box-pad");
		add(lay0, 0, 0, 2, 1);
		add(btn_remote, 0, 1, 2, 1);
		addRow(2, new Label("輸出(W)"), txt_forward);
		addRow(3, new Label("反射(W)"), txt_reflect);
		addRow(4, new Label("RF設定"), txt_rf_setp);
		addRow(5, new Label("DC偏壓"), txt_dc_setp);
		add(btn_set, 0, 6, 2, 1);
		add(btn_run, 0, 7, 2, 1);
	}
	
	public void bind(
		final ReadOnlyBooleanProperty flag_ready,
		final ReadOnlyBooleanProperty flag_error,
		final ReadOnlyBooleanProperty flag_rungo,
		final ReadOnlyFloatProperty val_forward,
		final ReadOnlyFloatProperty val_reflect
	) {		
		chk_ready.selectedProperty().bind(flag_ready);
		chk_error.selectedProperty().bind(flag_error);
		chk_rungo.selectedProperty().bind(flag_rungo);
		
		final LayHelper.Translate v2w = v->{
			return (v*600f)/10f; 
		};	
		txt_forward.textProperty().bind(LayHelper.transform(val_forward,v2w).asString("%.1f"));
		txt_reflect.textProperty().bind(LayHelper.transform(val_reflect,v2w).asString("%.1f"));
	}
	
	public void set_onoff(final boolean on_off) {
		adam2.asyncSetRelay(MODEA, on_off);
	}
	public void set_RF_fire(final boolean on_off) {
		adam2.asyncSetRelay(RF_ON, on_off);
	}
	public void apply_setpoint(
		final Float val_rf, 
		final Float val_dc
	) {
		Float[] vals = {null, null, null, null};
		if(val_rf!=null) {
			//0~10V --> 0~600W
			vals[SV_RF] = (val_rf*10f)/600f;
		}
		if(val_dc!=null) {
			//0~10V --> 0~4000V
			vals[SV_DC] = (val_dc*10f)/4000f; 
		}
		adam5.asyncDirectOuput(vals);
	}
}
