package prj.sputter;

import java.util.Optional;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import javafx.scene.layout.FlowPane;


public class LayGauges extends FlowPane {

	public final Tile gag[] = new Tile[9];
	
	public LayGauges() {
		//gauge for DCG-100
		gag[0] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("電壓")
			.unit("Volt")
			.maxValue(1000)			
			.build();
		gag[0].setDecimals(1);
		gag[0].setId("v_volt");
				
		gag[1] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("電流")			
			.unit("Amp")
			.maxValue(10)
			.build();
		gag[1].setDecimals(2);
		gag[1].setId("g_amps");
			
		gag[2] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("功率")			
			.unit("Watt")
			.maxValue(5000)				
			.build();
		gag[2].setId("g_watt");
			
		gag[3] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
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
		
		gag[6] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("Ar")
			.unit("SCCM")
			.build();
		gag[6].setDecimals(2);
		gag[7] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("O2")
			.unit("SCCM")
			.build();
		gag[7].setDecimals(2);
		gag[8] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("N2")
			.unit("SCCM")
			.build();
		gag[8].setDecimals(2);

			
		getStyleClass().addAll("box-pad");
		setPrefWrapLength(800);
		getChildren().addAll(gag);
	}
	
	public LayGauges bindProperty(final DevDCG100 dev) {		
		gag[0].valueProperty().bind(dev.volt);
		gag[1].valueProperty().bind(dev.amps);
		gag[2].valueProperty().bind(dev.watt);
		gag[3].valueProperty().bind(dev.joul);
		return this;
	}
	
	public LayGauges bindProperty(final DevSQM160 dev) {
		
		gag[4].valueProperty().bind(dev.rate[0]);
		gag[4].setMinValue(dev.rateRange[0].doubleValue());
		set_max_limit(
			gag[4],
			dev.rateRange[1].doubleValue()
		);
		gag[4].setUnit(dev.unitRate.get());
		
		gag[5].valueProperty().bind(dev.thick[0]);
		gag[5].setMinValue(dev.thickRange[0].doubleValue());
		set_max_limit(
			gag[5],
			dev.thickRange[1].doubleValue()
		);
		gag[5].setUnit(dev.unitHigh.get());
		return this;
	}
	public LayGauges bindProperty(final ModCouple dev) {
		gag[6].valueProperty().bind(dev.PV_FlowAr);
		gag[7].valueProperty().bind(dev.PV_FlowO2);
		gag[8].valueProperty().bind(dev.PV_FlowN2);
		return this;
	}
	
	private void set_max_limit(
		final Tile obj,
		final double val
	) {
		obj.setMaxValue(val);
		obj.setThreshold(val-0.7);
	}
	
	private static Optional<LayGauges> self = Optional.empty();
	
	public static LayGauges getInstance() {
		if(self.isPresent()==false){
			self = Optional.of(new LayGauges());
		}
		return self.get();
	}
	public static void setRateMax(final double val) {
		if(self.isPresent()==false){
			return;
		}
		LayGauges inst = self.get();
		inst.set_max_limit(inst.gag[4],val);
	}
	public static void setThickMax(final double val) {
		if(self.isPresent()==false){
			return;
		}
		LayGauges inst = self.get();
		inst.set_max_limit(inst.gag[5],val);
	}
}
