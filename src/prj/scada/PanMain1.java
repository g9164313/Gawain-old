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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import narl.itrc.PanBase;

public class PanMain1 extends PanBase {

	private final DevDCG100 dcg1 = new DevDCG100();
	
	private final DevSPIK2000 spik = new DevSPIK2000();
	
	public PanMain1() {
	}

	@Override
	public Node eventLayout(PanBase self) {
		
		stage().setOnShowing(e->{
			//dcg1.open("/dev/ttyUSB0,19200,8n1");
			//spik.open("/dev/ttyUSB1,19200,8n1");
		});
		
		final JFXTabPane lay4 = new JFXTabPane();
		lay4.getTabs().addAll(
			new Tab("管路控制"),
			new Tab("數據監測",lay_gauge()),
			new Tab("Misc")
		);
		lay4.getSelectionModel().select(1);

		final TitledPane[] lay3 = {
			new TitledPane("快速設定",lay_control()),
			new TitledPane("DC 裝置", DevDCG100.genPanel(dcg1)),
			new TitledPane("RF 裝置", DevSPIK2000.genPanel(spik))
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
	
	private Pane lay_gauge() {
		final Gauge gag1 = GaugeBuilder.create()
			.skinType(SkinType.TILE_SPARK_LINE)
			.title("電壓")
			.unit("Volt")
			.subTitle("")
			.autoScale(true)			
			.build();
		gag1.valueProperty().bind(dcg1.volt);
		
		final Gauge gag2 = GaugeBuilder.create()
			.skinType(SkinType.TILE_SPARK_LINE)
			.title("電流")
			.unit("Amp")
			.subTitle("")
			.autoScale(true)
			.build();
		gag2.valueProperty().bind(dcg1.amps);
		
		final Gauge gag3 = GaugeBuilder.create()
			.skinType(SkinType.TILE_SPARK_LINE)
			.title("功率")
			.unit("Watt")
			.subTitle("")
			.autoScale(true)			
			.build();
		gag3.valueProperty().bind(dcg1.watt);
		
		final Gauge gag4 = GaugeBuilder.create()
			.skinType(SkinType.TILE_SPARK_LINE)
			.title("")
			.unit("")
			.subTitle("")
			.autoScale(true)			
			.build();
		//gag4.valueProperty().bind(dcg1.volt);
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("ground-pad");
		lay.addRow(0,gag1,gag3);
		lay.addRow(1,gag2,gag4);
		return lay;
	}	
}
