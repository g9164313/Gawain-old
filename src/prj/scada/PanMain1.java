package prj.scada;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTabPane;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PanMain1 extends PanBase {

	private final DevDCG100 dcg1 = new DevDCG100();	
	private final DevSPIK2000 spik = new DevSPIK2000();
	private final DevSQM160 sqm1 = new DevSQM160();
	
	public PanMain1() {
	}

	@Override
	public Node eventLayout(PanBase self) {
		
		final Pane lay_dcg1 = DevDCG100.genPanel(dcg1);
		final Pane lay_spik = DevSPIK2000.genPanel(spik);
		final Pane lay_sqm1 = DevSQM160.genPanel(sqm1);
		
		stage().setOnShown(e->{			
			String txt;
			Gauge gag;
			txt = Gawain.prop().getProperty("DCG100", "");
			if(txt.length()>0) {
				gag = (Gauge)root().lookup("#v_volt");
				gag.valueProperty().bind(dcg1.volt);
				
				gag = (Gauge)root().lookup("#g_amps");
				gag.valueProperty().bind(dcg1.amps);
				
				gag = (Gauge)root().lookup("#g_watt");				
				gag.valueProperty().bind(dcg1.watt);
				
				gag = (Gauge)root().lookup("#g_joul");
				gag.valueProperty().bind(dcg1.joul);
				
				dcg1.open(txt);
			}
			txt = Gawain.prop().getProperty("SPIK2k", "");
			if(txt.length()>0) {
				spik.open(txt);
			}
			txt = Gawain.prop().getProperty("SQM160", "");
			if(txt.length()>0) {
				gag = (Gauge)root().lookup("#g_rate");
				gag.valueProperty().bind(sqm1.rate[0]);
				gag.minValueProperty().bind(sqm1.rateRange[0]);
				gag.maxValueProperty().bind(sqm1.rateRange[1]);
				gag.unitProperty().bind(sqm1.unitRate);

				gag = (Gauge)root().lookup("#g_high");
				gag.valueProperty().bind(sqm1.high[0]);
				gag.minValueProperty().bind(sqm1.highRange[0]);
				gag.maxValueProperty().bind(sqm1.highRange[1]);
				gag.unitProperty().bind(sqm1.unitHigh);
				
				sqm1.open(txt);
			}
		});
				
		final JFXTabPane lay4 = new JFXTabPane();
		lay4.getTabs().addAll(
			new Tab("管路控制"),
			new Tab("數據監測",lay_gague()),
			new Tab("其他")
		);
		lay4.getSelectionModel().select(1);

		final TitledPane[] lay3 = {
			new TitledPane("快速設定", lay_control()),
			new TitledPane("DCG-100",lay_dcg1),
			new TitledPane("SPIK-2000",lay_spik),
			new TitledPane("SQM-160",lay_sqm1)
		};
		final Accordion lay2 = new Accordion(lay3);
		lay2.setExpandedPane(lay3[1]);

		final BorderPane lay1 = new BorderPane();
		lay1.setCenter(lay4);
		
		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay1);
		lay0.setRight(lay2);
		return lay0;
	}
	
	private Pane lay_control() {
		final JFXButton[] btn = {
			new JFXButton("test-1"),
			new JFXButton("test-2"),
			new JFXButton("test-3"),
			new JFXButton("test-4"),
		};
		for(JFXButton b:btn) {
			b.getStyleClass().add("btn-raised-2");
			b.setMaxWidth(Double.MAX_VALUE);
		}
		final VBox lay = new VBox();
		lay.getStyleClass().addAll("box-pad");
		lay.getChildren().addAll(btn);
		return lay;
	}
	
	private Pane lay_gague() {
		
		final Gauge gag1 = GaugeBuilder.create()
			.title("電壓")
			.skinType(SkinType.DASHBOARD)
			.unit("Volt")
			.maxValue(1000)			
			.build();
		gag1.setDecimals(1);
		gag1.setId("v_volt");
			
		final Gauge gag2 = GaugeBuilder.create()
			.title("電流")
			.skinType(SkinType.DASHBOARD)
			.unit("Amp")
			.maxValue(10)
			.build();
		gag2.setDecimals(2);
		gag2.setId("g_amps");
		
		final Gauge gag3 = GaugeBuilder.create()
			.title("功率")
			.skinType(SkinType.DASHBOARD)
			.unit("Watt")
			.maxValue(5000)				
			.build();
		gag3.setId("g_watt");
		
		final Gauge gag4 = GaugeBuilder.create()
			.title("焦耳")
			.skinType(SkinType.DASHBOARD)
			.unit("Joules")
			.maxValue(5000)			
			.build();
		gag4.setId("g_joul");
		
		final Gauge gag5 = GaugeBuilder.create()
			.title("薄膜速率")
			.skinType(SkinType.DASHBOARD)
			.build();
		gag5.setDecimals(3);
		gag5.setId("g_rate");
		
		final Gauge gag6 = GaugeBuilder.create()
			.title("薄膜厚度")
			.skinType(SkinType.DASHBOARD)
			.build();
		gag6.setDecimals(3);
		gag6.setId("g_high");
			
		final FlowPane lay = new FlowPane();
		lay.setMinWidth(480);
		lay.getStyleClass().addAll("box-pad");
		lay.getChildren().addAll(
			gag1,gag2,gag3,gag4,
			gag5,gag6
		);
		return lay;
	}
	
}
