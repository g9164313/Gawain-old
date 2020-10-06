package prj.LPS_8S;

import java.util.Optional;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class LogHistory extends BorderPane {

	private static long last_tick = -1L;
	
	private class XChart extends LineChart<Number,Number>{
		XChart(final String name){
			super(new NumberAxis(),new NumberAxis());
			setTitle(name);
			setPrefSize(800, 170);
			NumberAxis xx = (NumberAxis)getXAxis();
			xx.setTickLabelsVisible(false);
			xx.setTickUnit(1000);
			xx.setMinorTickVisible(false);
		}
		XChart setBound(final double lower,final double upper) {
			NumberAxis yy = (NumberAxis)getYAxis();
			yy.setAutoRanging(false);
			yy.setLowerBound(lower);			
			yy.setUpperBound(upper);
			yy.setTickUnit((upper+lower)/10.);
			yy.setMinorTickVisible(false);
			return this;
		}
		XChart addData(final double value) {
			final XYChart.Data<Number,Number> val = new XYChart.Data<Number,Number>(
				System.currentTimeMillis()-last_tick,
				value
			);
			Series<Number,Number> ss;
			if(getData().size()==0) {
				ss = new XYChart.Series<Number,Number>();
				ss.getData().add(val);
				getData().add(ss);
			}else {
				ss = getData().get(0);
				ss.getData().add(val);
			}
			return this;
		}
		XChart clearData() {
			getData().clear();
			return this;
		}
	};
	
	final XChart[] chart = {
		new XChart("電導(pH)").setBound(0.,100.),
		new XChart("流量(??)").setBound(0.,100.),	
		new XChart("溫度( C)").setBound(0.,100.),
		
		new XChart("主軸速度(RPM)").setBound(0.,6000.),
		new XChart("主軸轉矩(％)").setBound(0.,100.),	
		new XChart("加壓軸速度(RPM)").setBound(0.,3000.),
		new XChart("加壓軸轉矩(％)").setBound(0.,100.),
		new XChart("擺動軸速度(RPM)").setBound(0.,3000.),		
		new XChart("擺動軸轉矩(％)").setBound(0.,100.),
	};
	
	final VBox lay0 = new VBox(
		chart[0],chart[1],chart[2],new Separator(),
		chart[3],chart[4],new Separator(),
		chart[5],chart[6],new Separator(),
		chart[7],chart[8],new Separator()
	);
	
	final ScrollPane lay1 = new ScrollPane(lay0);
	
	final ModInnerBus ibus;
	final ModCoupler cups;
	
	public LogHistory(
		final ModInnerBus ibus,
		final ModCoupler cups
	) {
		this.ibus = ibus;
		this.cups = cups;

		lay1.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		lay1.setFitToWidth(true);
		
		setCenter(lay1);
		setPrefSize(800, 800);
	}
	
	private Optional<Timeline> opt_clock = Optional.empty();
	
	@SuppressWarnings("unused")
	private void record_data() {
		chart[3].addData(ibus.MAJOR_RPM.get());
		chart[4].addData(ibus.MAJOR_TOR.get());
		chart[5].addData(ibus.PRESS_RPM.get());
		chart[6].addData(ibus.PRESS_TOR.get());
		chart[7].addData(ibus.SWING_RPM.get());
		chart[8].addData(ibus.SWING_TOR.get());
	}
	
	private final double simBias = 25.;
	private double simValue = 50.;
	@SuppressWarnings("unused")
	private void simulation() {
		Math.random();
		for(XChart obj:chart) {	
			obj.addData(simValue);
			double dff = Math.random() * simBias;
			if(Math.random()>0.5) {
				if((simValue + dff)>=100.) {
					simValue -= dff;
				}else {
					simValue += dff;
				}
			}else {
				if((simValue - dff)<=0.) {
					simValue += dff;
				}else {
					simValue -= dff;
				}
			}
		}
	}
	
	public void kick() {
		if(opt_clock.isPresent()==true) {
			return;
		}		
		for(XChart obj:chart) {	
			obj.clearData(); 
		}
		final Timeline clock = new Timeline(new KeyFrame(
			Duration.seconds(1), 
			e->record_data()
			//e->simulation()
		));
		clock.setCycleCount(Animation.INDEFINITE);
		clock.play();
		opt_clock = Optional.of(clock);
		last_tick = System.currentTimeMillis();
	}
	public void stop() {
		if(opt_clock.isPresent()==false) {
			return;
		}		
		opt_clock.get().stop();
		opt_clock = Optional.empty();
		last_tick = -1L;
	}
}
