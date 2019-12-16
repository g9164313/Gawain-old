package prj.scada;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.Gauge.SkinType;
import javafx.scene.layout.FlowPane;

public class LayGauge extends FlowPane {

	private final Gauge gag[] = new Gauge[6];
		
	public LayGauge() {
		
		//gauge for DCG-100
		gag[0] = GaugeBuilder.create()
			.title("電壓")
			.skinType(SkinType.DASHBOARD)
			.unit("Volt")
			.maxValue(1000)			
			.build();
		gag[0].setDecimals(1);
		gag[0].setId("v_volt");
				
		gag[1] = GaugeBuilder.create()
			.title("電流")
			.skinType(SkinType.DASHBOARD)
			.unit("Amp")
			.maxValue(10)
			.build();
		gag[1].setDecimals(2);
		gag[1].setId("g_amps");
			
		gag[2] = GaugeBuilder.create()
			.title("功率")
			.skinType(SkinType.DASHBOARD)
			.unit("Watt")
			.maxValue(5000)				
			.build();
		gag[2].setId("g_watt");
			
		gag[3] = GaugeBuilder.create()
			.title("焦耳")
			.skinType(SkinType.DASHBOARD)
			.unit("Joules")
			.maxValue(5000)			
			.build();
		gag[3].setId("g_joul");
		
		//gauge for SQM-160
		gag[4] = GaugeBuilder.create()
			.title("薄膜速率")
			.skinType(SkinType.DASHBOARD)
			.build();
		gag[4].setDecimals(3);
		gag[4].setId("g_rate");
			
		gag[5] = GaugeBuilder.create()
			.title("薄膜厚度")
			.skinType(SkinType.DASHBOARD)
			.build();
		gag[5].setDecimals(3);
		gag[5].setId("g_high");
		
		getStyleClass().addAll("box-pad");
		setMinWidth(250);		
		getChildren().addAll(gag);
	}
	
	public LayGauge bindProperty(final DevDCG100 dev) {
		
		gag[0].valueProperty().bind(dev.volt);
		gag[1].valueProperty().bind(dev.amps);
		gag[2].valueProperty().bind(dev.watt);
		gag[3].valueProperty().bind(dev.joul);
		return this;
	}
	
	public LayGauge bindProperty(final DevSQM160 dev) {
		
		gag[4].valueProperty().bind(dev.rate[0]);
		gag[4].minValueProperty().bind(dev.rateRange[0]);
		gag[4].maxValueProperty().bind(dev.rateRange[1]);
		gag[4].unitProperty().bind(dev.unitRate);

		gag[5].valueProperty().bind(dev.high[0]);
		gag[5].minValueProperty().bind(dev.highRange[0]);
		gag[5].maxValueProperty().bind(dev.highRange[1]);
		gag[5].unitProperty().bind(dev.unitHigh);
		return this;
	}
	
}
