package prj.scada;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;

import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import narl.itrc.DevModbus;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PanMain1 extends PanBase {
	
	final DevModbus coup = new DevModbus();
	//final DevDCG100 dcg1 = new DevDCG100();	
	final DevSPIK2k spik = new DevSPIK2k();
	final DevSQM160 sqm1 = new DevSQM160();

	final LayGauge lay_gaug = new LayGauge();
	
	final TblHistory tbl_hist = new TblHistory();

	public PanMain1() {
	}
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		stage().setOnShown(e->{			
			tbl_hist.bindProperty(
				LayDCG100.prop[0], LayDCG100.prop[1], 
				LayDCG100.prop[2], LayDCG100.prop[2],
				sqm1.rate[0], sqm1.high[0]
			);			
			String arg;
			arg = Gawain.prop().getProperty("modbus", "");
			if(arg.length()!=0) {				
				coup.mapRegister("h8000-8004");
				coup.open(arg);
				lay_gaug.bindProperty(coup);
			}
			//arg = Gawain.prop().getProperty("DCG100", "");
			//if(arg.length()>0) {
				//lay_gaug.bindProperty(dcg1);
				//dcg1.open(arg);
			//}
			arg = Gawain.prop().getProperty("SPIK2k", "");
			if(arg.length()>0) {
				spik.open(arg);
			}
			arg = Gawain.prop().getProperty("SQM160", "");
			if(arg.length()>0) {
				sqm1.open(arg);
				lay_gaug.bindProperty(sqm1);				
			}
		});
		//--------------------//
		
		final JFXTabPane lay4 = new JFXTabPane();
		lay4.getTabs().addAll(
			new Tab("管路"),
			new Tab("監測",lay_gaug),
			new Tab("紀錄",tbl_hist)
		);
		lay4.getSelectionModel().select(2);
	
		final TitledPane[] lay3 = {
			new TitledPane("快速設定"  ,lay_ctrl()),
			//new TitledPane("DCG-100"  ,DevDCG100.genPanel(dcg1)),
			new TitledPane("DCG-100"  ,LayDCG100.genPanel(coup)),
			new TitledPane("SPIK-2000",DevSPIK2k.genPanel(spik)),
			new TitledPane("SQM-160"  ,DevSQM160.genPanel(sqm1))
		};
		final Accordion lay2 = new Accordion(lay3);
		lay2.setExpandedPane(lay3[0]);

		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay4);
		lay0.setRight(lay2);
		return lay0;
	}
	
	private Pane lay_ctrl() {
		
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
		box[0].setText("50");
		box[0].setOnAction(e->{
			try {				
				int val = Integer.valueOf(box[0].getText());
				coup.asyncWriteVal(8006,val);
			}catch(NumberFormatException exp) {
				return;
			}
		});
		
		final JFXButton[] btn = new JFXButton[4];
		for(int i=0; i<btn.length; i++) {
			JFXButton obj = new JFXButton();
			obj.setMaxWidth(Double.MAX_VALUE);
			//GridPane.setHgrow(obj, Priority.ALWAYS);
			//GridPane.setFillWidth(obj, true);
			btn[i] = obj;
		}

		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setText("點火");
		btn[0].setOnAction(e->{
			try {				
				int val = Integer.valueOf(box[0].getText());
				coup.syncWriteVal(8006, val);
				coup.syncWriteXOR(8005, 0x1);
				tbl_hist.startRecord();
			}catch(NumberFormatException exp) {
				return;
			}
		});
		
		btn[1].getStyleClass().add("btn-raised-2");
		btn[1].setText("熄火");
		btn[1].setOnAction(e->{
			coup.syncWriteXOR(8005, 0x1);
			tbl_hist.stopRecord();
		});
		
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setText("匯出");
		btn[2].setOnAction(e->{
			String name = saveAsFile("record.xlsx");
			if(name.length()==0) {
				return;
			}
			notifyTask(tbl_hist.dumpRecord(name));	
		});
		
		final VBox lay = new VBox();
		lay.getStyleClass().addAll("box-pad");
		lay.getChildren().addAll(new Label(),box[0]);
		lay.getChildren().addAll(btn);
		return lay;
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
//h8000       - digital input
//h8001~h8004 - analog  input
//r8005       - digital output
//r8006~r8009 - analog  output
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



