package prj.sputter.labor1;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import prj.sputter.DevCouple;
import prj.sputter.DevDCG100;
import prj.sputter.DevSPIK2k;
import prj.sputter.DevSQM160;


public class PanMain2 extends PanBase {
	
	final DevCouple coup = new DevCouple();
	final DevDCG100 dcg1 = new DevDCG100();	
	final DevSPIK2k spik = new DevSPIK2k();
	final DevSQM160 sqm1 = new DevSQM160();
	//final DevSQM2Usb sqm2 = new DevSQM2Usb();
	
	final LayLogger  logger = new LayLogger();	
	final DrawVaccumOld digram = new DrawVaccumOld(coup);	
	final LayLadder  ladder = new LayLadder();
	
	public PanMain2(final Stage stg) {
		super(stg);
		stg.setTitle("二號濺鍍機");
		stg.setOnShown(e->on_shown());
		ladder.logger = logger;
		ladder.dcg1 = dcg1;
		StepCommon.coup = coup;		
		StepCommon.dcg1 = dcg1;
		StepCommon.spik = spik;
		StepCommon.sqm1 = sqm1;
		StepCommon.logg = logger;
	}

	private void on_shown(){
		String arg;
		arg = Gawain.prop().getProperty("modbus", "");
		if(arg.length()!=0) {			
			coup.open(arg);			
		}
		dcg1.open();
		spik.open();
		sqm1.open();
		//sqm2.open();
		logger.bindProperty(sqm1);
		logger.bindProperty(dcg1);
		logger.bindProperty(coup);
		logger.bindProperty(spik);
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		//rad[0].setDisable(true);
		//rad[0].setStyle("-fx-opacity: 1.0;");
		
		final HBox lay3 = new HBox();
		lay3.getStyleClass().addAll("box-pad");
		lay3.getChildren().addAll(
			DevDCG100.genPanel(dcg1),
			DevSPIK2k.genPanel(spik)
		);
		final ScrollPane lay2 = new ScrollPane(lay3);
		lay2.setPrefViewportWidth(800);
		lay2.setMinViewportHeight(500);
		
		final JFXTabPane lay1 = new JFXTabPane();
		lay1.getTabs().addAll(
			//new Tab("管路",digram),
			new Tab("監測",logger),
			new Tab("製程",ladder),
			new Tab("裝置",lay3)
		);
		lay1.getSelectionModel().select(1);

		final BorderPane lay0 = new BorderPane();		
		lay0.setCenter(lay1);		
		lay0.setRight(lay_ctrl());
		return lay0;
	}

	private Pane lay_ctrl() {
		final VBox lay = new VBox();
		lay.getStyleClass().addAll("box-pad");
		lay.getChildren().addAll(
			Misc.addBorder(StepFlowCtrl.genCtrlPanel()),
			Misc.addBorder(lay_info_fire()),
			Misc.addBorder(lay_info_dash())
		);				
		return lay;
	}

	private Pane lay_info_fire() {
		
		JFXButton[] btn_pulse_edit = {
			new JFXButton(),//Ton+
			new JFXButton(),//Toff+
			new JFXButton(),//Ton-
			new JFXButton(),//Toff-
		};
		for(JFXButton btn:btn_pulse_edit) {
			btn.setGraphic(Misc.getIconView("pen.png"));
		}
		btn_pulse_edit[0].setOnAction(e->DevSPIK2k.show_set_Pulse(spik));
		btn_pulse_edit[1].setOnAction(e->DevSPIK2k.show_set_Pulse(spik));
		btn_pulse_edit[2].setOnAction(e->DevSPIK2k.show_set_Pulse(spik));
		btn_pulse_edit[3].setOnAction(e->DevSPIK2k.show_set_Pulse(spik));
		
		Label[] box_pulse_value = {
			new Label(),//Ton+
			new Label(),//Toff+
			new Label(),//Ton-
			new Label(),//Toff-
		};
		box_pulse_value[0].textProperty().bind(spik.Ton_pos.asString("%3d"));
		box_pulse_value[1].textProperty().bind(spik.Tof_pos.asString("%3d"));
		box_pulse_value[2].textProperty().bind(spik.Ton_neg.asString("%3d"));
		box_pulse_value[3].textProperty().bind(spik.Tof_neg.asString("%3d"));
		
		for(int i=0; i<4; i++){
			box_pulse_value[i].setPrefWidth(45);
		}
		box_pulse_value[0].setOnMouseClicked(e->btn_pulse_edit[0].getOnAction().handle(null));		
		box_pulse_value[1].setOnMouseClicked(e->btn_pulse_edit[1].getOnAction().handle(null));		
		box_pulse_value[2].setOnMouseClicked(e->btn_pulse_edit[2].getOnAction().handle(null));		
		box_pulse_value[3].setOnMouseClicked(e->btn_pulse_edit[3].getOnAction().handle(null));
				
		JFXComboBox<String> cmb_polar = new JFXComboBox<String>();
		cmb_polar.setMaxWidth(Double.MAX_VALUE);
		cmb_polar.getItems().addAll(
			"選擇極性",
			"Bipolar",
			"Unipolar: Gun-1",
			"Unipolar: Gun-2"
		);
		cmb_polar.setEditable(false);
		cmb_polar.getSelectionModel().select(0);
		cmb_polar.setOnAction(e->{
			SingleSelectionModel<String> sel = cmb_polar.getSelectionModel();
			switch(sel.getSelectedIndex()) {
			case 0: return;
			case 1: coup.asyncSelectGunHub(true , false); break;
			case 2: coup.asyncSelectGunHub(false, true); break;
			case 3: coup.asyncSelectGunHub(false, false); break;
			}
			Misc.logv("選擇極性 %s", sel.getSelectedItem());
		});	
		
		JFXButton btn_pulse_run = new JFXButton("開啟");
		btn_pulse_run.getStyleClass().add("btn-raised-1");
		btn_pulse_run.setOnAction(e->spik.toggleRun(true));
		
		JFXButton btn_pulse_off = new JFXButton("關閉");
		btn_pulse_off.getStyleClass().add("btn-raised-0");
		btn_pulse_off.setOnAction(e->spik.toggleRun(false));
		
		JFXTextField box_power_on = new JFXTextField("100");
		box_power_on.setPrefWidth(73);
		box_power_on.setOnAction(event->{
			try {
				final int val = Integer.valueOf(box_power_on.getText());
				box_power_on.getStyleClass().remove("error");
				if(spik.Run.get()==false) {
					final Alert dia = new Alert(AlertType.WARNING);
					dia.setTitle("！！警告！！");
					dia.setHeaderText("High-Pin 沒有開啟");
					dia.showAndWait();
					return;
				}
				Misc.logv("手動調整功率=%d", val);
				dcg1.asyncExec("CHL=W","SPW="+val,"TRG");
			}catch(NumberFormatException e) {
				box_power_on.getStyleClass().add("error");
			}			
		});
		
		JFXButton btn_power_off = new JFXButton("熄火");
		btn_power_off.getStyleClass().add("btn-raised-0");	
		btn_power_off.setOnAction(e->dcg1.asyncExec("OFF"));
		
		JFXButton btn_shutter_open = new JFXButton("開啟");
		btn_shutter_open.getStyleClass().add("btn-raised-1");
		btn_shutter_open.setOnAction(e->sqm1.shutter(true, null, null));
		
		JFXButton btn_shutter_close= new JFXButton("關閉");
		btn_shutter_close.getStyleClass().add("btn-raised-0");	
		btn_shutter_close.setOnAction(e->sqm1.shutter(false, null, null));
		
		JFXButton[] btn_lst = {
			btn_pulse_run, btn_pulse_off,
			btn_power_off,
			btn_shutter_open, btn_shutter_close
		};
		for(JFXButton obj:btn_lst) {
			obj.setMaxWidth(Double.MAX_VALUE);
			GridPane.setHgrow(obj, Priority.ALWAYS);
		}
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad");
		lay1.add(cmb_polar,0,0,3,1);
		lay1.addRow(1, new Label("\\"), new Label("on"), new Label("off"));
		lay1.addRow(2, 
			new Label("+"), 
			new HBox(box_pulse_value[0],btn_pulse_edit[0]), 
			new HBox(box_pulse_value[1],btn_pulse_edit[1])
		);
		lay1.addRow(3, 
			new Label("-"), 
			new HBox(box_pulse_value[2],btn_pulse_edit[2]), 
			new HBox(box_pulse_value[3],btn_pulse_edit[3])
		);		
		
		btn_shutter_open.disableProperty().bind(sqm1.shutter);
		btn_shutter_close.disableProperty().bind(sqm1.shutter.not());
		
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad");
		lay0.addRow(0, new Label("脈衝："), btn_pulse_run, btn_pulse_off);
		lay0.addRow(1, new Label("功率："), box_power_on, btn_power_off);
		lay0.addRow(2, new Label("擋板："), btn_shutter_open,btn_shutter_close);
		
		return new VBox(lay1,lay0);
	}
	
	private Pane lay_info_dash() {
		
		JFXButton btn_rate1_zero = new JFXButton();
		btn_rate1_zero.setGraphic(Misc.getIconView("sync.png"));
		btn_rate1_zero.setOnAction(e->sqm1.zeros());
		
		//JFXButton btn_rate2_zero = new JFXButton();
		//btn_rate2_zero.setGraphic(Misc.getIconView("sync.png"));
		//btn_rate2_zero.setOnAction(e->sqm2.zeros());
		
		Label txt_rate_1 = new Label();
		txt_rate_1.textProperty().bind(sqm1.rate[0].concat(sqm1.unitRate));
		txt_rate_1.setPrefWidth(110);
		
		//Label txt_rate_2 = new Label();
		//txt_rate_2.textProperty().bind(sqm2.rate);
		//txt_rate_2.setPrefWidth(110);
		
		Label txt_pow = new Label();
		txt_pow.textProperty().bind(dcg1.watt.asString("%05.1fW"));
		
		Label txt_volt = new Label();
		txt_volt.textProperty().bind(dcg1.volt.asString("%05.1fV"));
		
		Label txt_amp = new Label();
		txt_amp.textProperty().bind(dcg1.amps.asString("%05.3fA"));
		
		Label txt_arc = new Label();
		txt_arc.textProperty().bind(spik.ARC_count.asString("%d"));
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addRow(0, new Label("速率-1："), new HBox(txt_rate_1, btn_rate1_zero));
		//lay.addRow(1, new Label("速率-2："), new HBox(txt_rate_2, btn_rate2_zero));
		lay.addRow(2, new Label("功率："), txt_pow);
		lay.addRow(3, new Label("電壓："), txt_volt);
		lay.addRow(4, new Label("電流："), txt_amp);
		lay.addRow(5, new Label("電弧："), txt_arc);		
		return lay;
	}
}





