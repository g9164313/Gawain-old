package prj.scada;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;

import javafx.event.ActionEvent;
import javafx.scene.control.Accordion;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import narl.itrc.DevModbus;
import narl.itrc.Gawain;
import narl.itrc.Ladder;
import narl.itrc.PanBase;

public class PanMain1 extends PanBase {
	
	final DevModbus coup = new DevModbus().mapRegister("h8000-8004");
	final DevDCG100 dcg1 = new DevDCG100();	
	final DevSPIK2k spik = new DevSPIK2k();
	final DevSQM160 sqm1 = new DevSQM160();

	final LayGauge lay_gauge = new LayGauge();
	final TblHistory tbl_history = new TblHistory();
	final Ladder lst_ladder = new Ladder();
	
	public PanMain1() {
		super();
		stage().setOnShown(e->on_shown());
	}
	
	private void on_shown(){
		String arg;
		arg = Gawain.prop().getProperty("modbus", "");
		if(arg.length()!=0) {
			lay_gauge.bindProperty(coup);
			coup.open(arg);		
		}
		arg = Gawain.prop().getProperty("DCG100", "");
		if(arg.length()>0) {
			lay_gauge.bindProperty(dcg1);
			dcg1.open(arg);
		}
		arg = Gawain.prop().getProperty("SPIK2k", "");
		if(arg.length()>0) {
			spik.open(arg);
		}
		arg = Gawain.prop().getProperty("SQM160", "");
		if(arg.length()>0) {
			lay_gauge.bindProperty(sqm1);
			sqm1.open(arg);
		}
		arg = Gawain.prop().getProperty("http_poster", "");
		if(arg.length()>0) {
			tbl_history.poster = new DevPoster(arg);
			tbl_history.poster.open();
		}
		tbl_history.bindProperty(
			dcg1.volt, dcg1.amps, 
			dcg1.watt, dcg1.joul,
			sqm1.rate[0], sqm1.high[0]
		);
	}
	
	@Override
	public Pane eventLayout(PanBase self) {

		final JFXTabPane lay4 = new JFXTabPane();
		lay4.getTabs().addAll(
			new Tab("管路"),
			new Tab("監測",lay_gauge),
			new Tab("製程",lst_ladder),
			new Tab("紀錄",tbl_history)
		);
		lay4.getSelectionModel().select(2);
	
		final TitledPane[] lay3 = {
			new TitledPane("快速設定"  ,lay_ctrl()),
			new TitledPane("DCG-100"  ,DevDCG100.genPanel(dcg1)),
			new TitledPane("SPIK-2000",DevSPIK2k.genPanel(spik)),
			new TitledPane("SQM-160"  ,DevSQM160.genPanel(sqm1))
		};
		final Accordion lay2 = new Accordion(lay3);
		lay2.setExpandedPane(lay3[0]);

		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay4);
		//lay0.setRight(lay2);
		return lay0;
	}
	
	private Pane lay_ctrl() {
		
		final Label[] txt = new Label[6];
		for(int i=0; i<txt.length; i++) {
			Label obj = new Label();
			obj.setMaxWidth(Double.MAX_VALUE);
			txt[i] = obj;
		}
		txt[0].textProperty().bind(sqm1.filmData[0]);
		txt[1].textProperty().bind(sqm1.filmData[1]);
		txt[2].textProperty().bind(sqm1.filmData[2]);
		txt[3].textProperty().bind(sqm1.filmData[3]);

		final JFXTextField[] box = new JFXTextField[5];
		for(int i=0; i<box.length; i++) {
			JFXTextField obj = new JFXTextField();
			obj.setMaxWidth(Double.MAX_VALUE);
			obj.setLabelFloat(true);
			//GridPane.setHgrow(obj, Priority.ALWAYS);
			//GridPane.setFillWidth(obj, true);
			box[i] = obj;
		}
		box[0].setPromptText("輸出功率(Watt)");
		box[0].setText("100");
		box[0].setOnAction(e->{
			try {				
				int val = Integer.valueOf(box[0].getText());
				//coup.asyncWriteVal(8006,val);
				dcg1.asyncExec("SPW="+val);
			}catch(NumberFormatException exp) {
			}
		});
		
		final JFXButton[] btn = new JFXButton[5];
		for(int i=0; i<btn.length; i++) {
			JFXButton obj = new JFXButton();
			obj.setMaxWidth(Double.MAX_VALUE);
			btn[i] = obj;
		}

		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setText("擋片-ON");
		btn[0].setOnAction(e->sqm1.shutter_zeros());
		btn[1].getStyleClass().add("btn-raised-2");
		btn[1].setText("擋片-OFF");
		btn[1].setOnAction(e->sqm1.shutter(false));
		
		btn[2].getStyleClass().add("btn-raised-1");
		btn[2].setText("電漿-ON");
		btn[2].setOnAction(e->{
			try {				
				int val = Integer.valueOf(box[0].getText());
				sqm1.zeros();
				dcg1.asyncBreakIn(()->{
					dcg1.exec("SPW="+val);
					dcg1.exec("TRG");
				});
				tbl_history.startRecord();
			}catch(NumberFormatException exp) {
			}
		});
		btn[3].getStyleClass().add("btn-raised-2");
		btn[3].setText("電漿-OFF");
		btn[3].setOnAction(e->{
			//coup.asyncWriteBit0(8005, 0);
			dcg1.asyncExec("OFF");
			tbl_history.stopRecord();
		});
		
		final GridPane lay2 = new GridPane();
		lay2.getStyleClass().addAll("box-pad-inner");
		lay2.addRow(0, new Label("薄膜名稱 ：" ), txt[0]);
		lay2.addRow(1, new Label("Density："), txt[1]);
		lay2.addRow(2, new Label("Tooling："), txt[2]);
		lay2.addRow(3, new Label("Z-Ratio："), txt[3]);
		
		final JFXCheckBox[] rad = {
			new JFXCheckBox("Shutter"),
			new JFXCheckBox("Bipolar"),
			new JFXCheckBox("Unipolar"),
			new JFXCheckBox("Gun-1"),
			new JFXCheckBox("Gun-2"),
		};
		rad[0].setDisable(true);
		rad[0].selectedProperty().bind(sqm1.shutter);
		rad[0].setStyle("-fx-opacity: 1.0;");
		
		rad[1].setOnAction(e->{
			//a mutex for checking I/O
			if(rad[1].isSelected()){
				rad[2].setSelected(false);
				coup.asyncBreakIn(()->{
					coup.writeBit1(8005, 0);
					coup.writeBit0(8005, 1);
				});
			}else{
				coup.asyncWriteBit0(8005,0);
			}
		});
		rad[2].setOnAction(e->{
			//a mutex for checking I/O
			if(rad[2].isSelected()){
				rad[1].setSelected(false);
				coup.asyncBreakIn(()->{
					coup.writeBit1(8005, 1);
					coup.writeBit0(8005, 0);
				});
			}else{
				coup.asyncWriteBit0(8005,1);
			}
		});
		rad[3].setOnAction(e->select_io_pin(e,8005,2));
		rad[4].setOnAction(e->select_io_pin(e,8005,3));
		rad[3].visibleProperty().bind(rad[2].selectedProperty());
		rad[4].visibleProperty().bind(rad[2].selectedProperty());
		
		final GridPane lay3 = new GridPane();
		lay3.getStyleClass().addAll("box-pad-inner");
		lay3.addColumn(0, rad);
		
		final VBox lay0 = new VBox();
		lay0.getStyleClass().addAll("box-pad");
		lay0.getChildren().addAll(new Label(),box[0]);
		lay0.getChildren().add(lay2);
		lay0.getChildren().add(new Separator());
		lay0.getChildren().add(lay3);
		lay0.getChildren().add(new Separator());
		lay0.getChildren().addAll(btn);
		return lay0;
	}
	
	private void select_io_pin(
		final ActionEvent event,
		final int address,
		final int bitmask
	){
		final CheckBox chk = (CheckBox)event.getSource();
		if(chk.isSelected()==true){
			coup.asyncWriteBit1(address, bitmask);
		}else{
			coup.asyncWriteBit0(address, bitmask);
		}
	}
}

/**
 * PHOENIX CONTACT coupler:
 * 
 * ETH BK DIO8 DO4 2TX-PAC:
 * 1.1 OUT1  2.1 OUT2
 * 1.2 GND   2.2 GND
 * 1.3 FE    2.3 FE
 * 1.4 OUT3  2.4 OUT4
 * 
 * 1.1 IN1   2.1 IN2
 * 1.2 Um    2.1 Um
 * 1.3 GND   2.2 GND
 * 1.4 IN3   2.3 IN4
 * 
 * 1.1 IN5   2.1 IN6
 * 1.2 Um    2.1 Um
 * 1.3 GND   2.2 GND
 * 1.4 IN7   2.3 IN8
 * -----------------
 * IB IL AI 4-ECO
 * 1.1 IN1   2.1 GND
 * 1.2 IN2   2.1 GND
 * 1.3 IN3   2.2 GND
 * 1.4 IN4   2.3 GND 
 * -----------------
 * IB IL AO 4-ECO
 * 1.1 OUT1  2.1 OUT2
 * 1.2 GND   2.1 GND
 * 1.3 OUT3  2.2 OUT4
 * 1.4 GND   2.3 GND
 * -----------------
 * Um - 24V
 * FE - Function Earth
 */
/**
 * h8000       - digital input
 * h8001~h8004 - analog  input
 * r8005       - digital output
 * r8006~r8009 - analog  output
 */
/**
 * DCG-100 analog control
 * pin.1 -->I1-2.1 (ch.B: out, DC on read-back
 * pin.2 -->AI-1.1 (ch.A: out, voltage get
 * pin.3 -->AI-1.2 (ch.A: out, power get
 * pin.4 -->O1-1.1 (ch.A: in , closure relay
 * pin.5 -->AO-1.1 (ch.A: in , power set
 * pin.6 -->AI-2.1 (    : GND, analog common
 * pin.7 -->I1-1.1 (ch.A: out, DC on read-back
 * pin.8 -->I1-1.3 (    : GND, DC read common
 * pin.9 -->O1-1.2 (    : GND, closure common
 * pin.10-->AO-2.1 (ch.B: in , power set
 * pin.11-->O1-1.4 (    : in , safety lock
 * pin.12-->O1-2.2 (    : GND, safety lock common
 * pin.13-->O1-2.1 (ch.B: in , closure relay
 * pin.14-->AI-1.3 (ch.B: out, voltage get
 * pin.15-->AI-1.4 (ch.B: out, power get
 */



