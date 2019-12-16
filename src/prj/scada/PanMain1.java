package prj.scada;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTabPane;

import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import narl.itrc.DevModbus;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanMain1 extends PanBase {
	
	final DevModbus coup = new DevModbus();
	final DevDCG100 dcg1 = new DevDCG100();	
	final DevSPIK2k spik = new DevSPIK2k();
	final DevSQM160 sqm1 = new DevSQM160();
		
	final LayGauge lay_gaug = new LayGauge();
	
	final LayHistory lay_hist = new LayHistory(
		dcg1.volt, dcg1.amps, dcg1.watt, dcg1.joul,
		sqm1.rate[0], sqm1.high[0]
	);
	
	public PanMain1() {		
		coup.mapRegister("h8000-8004");		
	}

	@Override
	public Pane eventLayout(PanBase self) {
		
		stage().setOnShown(e->{	
			String arg;
			arg = Gawain.prop().getProperty("modbus", "");
			if(arg.length()!=0) {
				coup.open(arg);
			}
			arg = Gawain.prop().getProperty("DCG100", "");
			if(arg.length()>0) {
				lay_gaug.bindProperty(dcg1);
				dcg1.open(arg);
			}
			arg = Gawain.prop().getProperty("SPIK2k", "");
			if(arg.length()>0) {
				spik.open(arg);
			}
			arg = Gawain.prop().getProperty("SQM160", "");
			if(arg.length()>0) {
				lay_gaug.bindProperty(sqm1);
				sqm1.open(arg);
			}
		});
		//--------------------//
		
		final JFXTabPane lay4 = new JFXTabPane();
		lay4.getTabs().addAll(
			new Tab("管路"),
			new Tab("監測",lay_gaug),
			new Tab("紀錄",lay_hist)
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
		lay2.setMaxHeight(200);
		
		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay4);
		lay0.setRight(lay2);
		return lay0;
	}
	
	private Pane lay_ctrl() {
		
		Label[] txt = { 
			new Label(),
			new Label(), 
			new Label(),
			new Label(), 
			new Label(), 
		};

		txt[0].textProperty().bind(coup.register(8000).asString("DI.x: %H"));
		txt[1].textProperty().bind(coup.register(8001).multiply(0.20f).asString("AI.1: %.1fV"));
		txt[2].textProperty().bind(coup.register(8002).multiply(1.06f).asString("AI.2: %.1fW"));
		txt[3].textProperty().bind(coup.register(8003).multiply(0.20f).asString("AI.3: %.1fV"));
		txt[4].textProperty().bind(coup.register(8004).multiply(1.06f).asString("AI.4: %.1fW"));
		
		final JFXButton[] btn = {
			new JFXButton("點火(ON )"),
			new JFXButton("熄火(OFF)"),
			new JFXButton("test-3"),
			new JFXButton("test-4"),
		};
		for(JFXButton b:btn) {
			b.setMaxWidth(Double.MAX_VALUE);
		}

		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setOnAction(e->{
			notifyFlow((spin,step)->{
				Misc.logv("step="+step);
				dcg1.asyncTrigger();
			},(spin,step)->{
				Misc.logv("step="+step);				
				if(dcg1.isFlowing()==true) {
					spin.remain(step);
				}				
			},(spin,step)->{
				Misc.logv("step="+step);
				lay_hist.startRecord();
			},(spin,step)->{
				Misc.logv("step="+step);
				spin.close();
			});			
		});
		btn[1].getStyleClass().add("btn-raised-2");
		btn[1].setOnAction(e->{
			//lay_hist.stopRecord();
		});		
		btn[2].setOnAction(e->{
			coup.writeXOR(8005, 0x3);
		});
		btn[3].setOnAction(e->{
			coup.write(8006, 0);
		});
		
		final VBox lay = new VBox();
		lay.getStyleClass().addAll("box-pad");
		lay.getChildren().addAll(btn);
		lay.getChildren().addAll(txt);
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



