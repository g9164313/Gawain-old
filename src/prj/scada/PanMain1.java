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
import javafx.stage.Stage;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PanMain1 extends PanBase {

	private final DevDCG100 dcg1 = new DevDCG100();
	
	private final DevSPIK2000 spik = new DevSPIK2000();
	
	public PanMain1(Stage owner) {
		super(owner);
	}

	@Override
	public Node eventLayout(PanBase self) {
		
		final Pane lay_dcg1 = DevDCG100.genPanel(dcg1);
		final Pane lay_spik = DevSPIK2000.genPanel(spik);
		
		stage().setOnShowing(e->{			
			String txt;
			txt = Gawain.prop().getProperty("DCG100", "");
			if(txt.length()>=1) {
				dcg1.open(txt);
				((Gauge)root().lookup("#v_volt")).valueProperty().bind(dcg1.volt);
				((Gauge)root().lookup("#g_amps")).valueProperty().bind(dcg1.amps);
				((Gauge)root().lookup("#g_watt")).valueProperty().bind(dcg1.watt);
				((Gauge)root().lookup("#g_joul")).valueProperty().bind(dcg1.joul);
			}
			txt = Gawain.prop().getProperty("SPIK2000", "");
			if(txt.length()>=1) {
				spik.open(txt);
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
			new TitledPane("DCG-100", lay_dcg1),
			new TitledPane("SPIK2000",lay_spik)
		};
		final Accordion lay2 = new Accordion(lay3);
		lay2.setExpandedPane(lay3[1]);

		final BorderPane lay1 = new BorderPane();
		//lay1.getStyleClass().add("ground-pad");
		lay1.setCenter(lay4);
		
		final BorderPane lay0 = new BorderPane();
		//lay0.getStyleClass().add("ground-pad");
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
			.skinType(SkinType.DASHBOARD)
			.title("電壓")
			.unit("Volt")
			.subTitle("")
			.maxValue(500)			
			.build();
		gag1.setId("v_volt");
			
		final Gauge gag2 = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.title("電流")
			.unit("Amp")
			.subTitle("")
			.maxValue(10)	
			.build();
		gag2.setId("g_amps");
		
		final Gauge gag3 = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.title("功率")
			.unit("Watt")
			.subTitle("")
			.maxValue(5000)				
			.build();
		gag3.setId("g_watt");
		
		final Gauge gag4 = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.title("焦耳")
			.unit("Joules")
			.subTitle("")
			.maxValue(5000)			
			.build();
		gag4.setId("g_joul");
		
		final FlowPane lay = new FlowPane();
		lay.setMinWidth(480);
		lay.getStyleClass().addAll("box-pad");
		lay.getChildren().addAll(gag1,gag2,gag3,gag4);
		return lay;
	}
	
}