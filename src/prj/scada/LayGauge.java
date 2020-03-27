package prj.scada;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import javafx.beans.binding.FloatBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.scene.layout.FlowPane;
import narl.itrc.DevModbus;

public class LayGauge extends FlowPane {

	public final Tile gag[] = new Tile[6];
		
	public LayGauge() {

		//gauge for DCG-100
		gag[0] = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.title("電壓")
			.unit("Volt")
			.maxValue(1000)			
			.build();
		gag[0].setDecimals(1);
		gag[0].setId("v_volt");
				
		gag[1] = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.title("電流")			
			.unit("Amp")
			.maxValue(10)
			.build();
		gag[1].setDecimals(2);
		gag[1].setId("g_amps");
			
		gag[2] = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.title("功率")			
			.unit("Watt")
			.maxValue(5000)				
			.build();
		gag[2].setId("g_watt");
			
		gag[3] = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.title("焦耳")			
			.unit("Joules")
			.maxValue(5000)			
			.build();
		gag[3].setId("g_joul");
		
		//gauge for SQM-160
		gag[4] = TileBuilder.create()			
			.skinType(SkinType.GAUGE)
			.title("薄膜速率")	
			.build();
		gag[4].setDecimals(3);
		gag[4].setId("g_rate");
			
		gag[5] = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.title("薄膜厚度")
			.build();
		gag[5].setDecimals(3);
		gag[5].setId("g_high");
		
		getStyleClass().addAll("box-pad");
		setPrefWrapLength(800);
		getChildren().addAll(gag);
	}
	
	public LayGauge bindProperty(final DevModbus dev) {
		IntegerProperty prop;
		
		prop = dev.register(8001);
		if(prop==null) {
			return this;
		}
		final FloatBinding a_volt = prop.multiply(0.20f);
		gag[0].valueProperty().bind(a_volt);
		
		prop = dev.register(8002);
		if(prop==null) {
			return this;
		}
		final FloatBinding a_watt = prop.multiply(1.06f);
		gag[1].valueProperty().bind(a_watt.divide(a_volt.add(Float.MIN_VALUE)));
		gag[2].valueProperty().bind(a_watt);
		return this;
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
		gag[4].setMinValue(dev.rateRange[0].doubleValue());
		gag[4].setMaxValue(dev.rateRange[1].doubleValue());
		gag[4].setUnit(dev.unitRate.get());

		gag[5].valueProperty().bind(dev.high[0]);
		gag[5].setMinValue(dev.highRange[0].doubleValue());
		gag[5].setMaxValue(dev.highRange[1].doubleValue());
		gag[5].setUnit(dev.unitHigh.get());
		return this;
	}
	
	public LayGauge bindProperty(
		final DevSQM160 dev1,
		final DevModbus dev2
	) {			
		FloatProperty rate_min = dev1.rateRange[0];
		FloatProperty rate_max = dev1.rateRange[1];
		FloatProperty high_min = dev1.highRange[0];
		FloatProperty high_max = dev1.highRange[1];
				
		IntegerProperty prop;
		
		prop = dev2.register(8003);
		if(prop==null) {
			return this;
		}
		NumberBinding rate = prop
			.multiply(rate_max.subtract(rate_min))
			.divide(5.f)
			.add(rate_min);
		
		gag[4].valueProperty().bind(rate);
		
		prop = dev2.register(8004);
		if(prop==null) {
			return this;
		}
		NumberBinding high = prop
			.multiply(high_max.subtract(high_min))
			.divide(5.f)
			.add(high_min);
		gag[5].valueProperty().bind(high);
		
		return this;
	}
	
}
