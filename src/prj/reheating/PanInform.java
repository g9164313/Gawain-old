package prj.reheating;

import java.util.Random;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import narl.itrc.Misc;
import narl.itrc.PanBase;
import eu.hansolo.enzo.common.SectionBuilder;
import eu.hansolo.enzo.gauge.Gauge;
import eu.hansolo.enzo.gauge.GaugeBuilder;
import eu.hansolo.enzo.vumeter.VuMeter;
import eu.hansolo.enzo.vumeter.VuMeterBuilder;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.GridPane;

public class PanInform extends GridPane {

	public Gauge gauVacc,gauTube,gauTubx,gauBuck;
	public VuMeter vumHook;
	
	private LineChart<Number,Number> chrPrss,chrTemp;
	
	private Series<Number,Number> serPrssVacc = new XYChart.Series<Number,Number>();
	private Series<Number,Number> serTempTube = new XYChart.Series<Number,Number>();
	private Series<Number,Number> serTempBuck = new XYChart.Series<Number,Number>();
	
	private int HistorySize = 600;
	private int PeriodCount = 10;
	private int PeriodIndex = 0;
	private SummaryStatistics bufPrssVacc = new SummaryStatistics();
	private SummaryStatistics bufTempTube = new SummaryStatistics();
	private SummaryStatistics bufTempBuck = new SummaryStatistics();
	
	private static final Random RND = new Random();
	
	public void updateInfo(Entry e){		
		double prssV1 = 10.;//TODO: how to get pressure value?
		double tempT1= e.pidT1.getValue();
		double tempT2= e.pidT2.getValue();
		double tempB1= e.pidB1.getValue();
		if(Math.abs(tempT1-tempT2)>=5.){
			//TODO: check difference, what information can we show~~
		}
		gauVacc.setValue(prssV1);
		gauTube.setValue(tempT1);
		gauTubx.setValue(tempT2);
		gauBuck.setValue(tempB1);
		vumHook.setValue(RND.nextDouble());
		if(bufPrssVacc.getN()>=PeriodCount){
			push_value(serPrssVacc,bufPrssVacc);
			push_value(serTempTube,bufTempTube);
			push_value(serTempBuck,bufTempBuck);
			PeriodIndex++;
			PeriodIndex = PeriodIndex % HistorySize;
		}else{
			bufPrssVacc.addValue(prssV1);
			bufTempTube.addValue(tempT1);
			bufTempBuck.addValue(tempB1);
		}		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void push_value(Series<Number,Number> ser,SummaryStatistics ss){
		double avg = ss.getMean();
		ObservableList<Data<Number,Number>> lst = ser.getData();
		lst.add(new XYChart.Data(PeriodIndex,avg));
		if(lst.size()>HistorySize){
			lst.remove(0);
		}
		ss.clear();
	}

	public void initTubeGauge(double min,double max,double val){
		gauTube = create_gauge("爐管","ºC",min,max,val);
		add(gauTube,0,0,1,1);
	}
	
	public void initTubxGauge(double min,double max,double val){
		gauTubx = create_gauge("爐管(監控)","ºC",min,max,val);
		add(gauTubx,1,0,1,1);
	}
	
	public void initBuckGauge(double min,double max,double val){
		gauBuck = create_gauge("油桶","ºC",min,max,val);
		add(gauBuck,0,1,1,1);
	}
	
	public void initPressGauge(double min,double max,double val){
		gauVacc = create_gauge("氣壓","pa",min,max,val);
		add(gauVacc,1,1,1,1);
	}
	
	private Gauge create_gauge(
		String title,String unit,
		double min,double max,
		double val
	){
		final int GAU_SIZE = 200;
		Gauge gau = GaugeBuilder
			.create()
			.prefSize(GAU_SIZE,GAU_SIZE)
			.animated(true)
			.startAngle(330)
			.angleRange(300)
			.minValue(min)
			.maxValue(max)
			.value(val)
			.sectionsVisible(true)
			.majorTickSpace((max-min)/10.)
			.plainValue(false)
			.tickLabelOrientation(Gauge.TickLabelOrientation.HORIZONTAL)
			.minMeasuredValueVisible(true)
			.maxMeasuredValueVisible(true)
			.title(title).unit(unit)
			.build();
		gau.prefWidthProperty().bind(widthProperty().divide(3));
		gau.prefHeightProperty().bind(heightProperty().divide(3));
		return gau;
	}
	
	private LineChart<Number,Number> init_chart(String title,String unit){
		final NumberAxis x_axis = new NumberAxis();
		final NumberAxis y_axis = new NumberAxis();
		x_axis.setLabel(title);
		x_axis.setSide(Side.TOP);
		y_axis.setLabel(unit);
		LineChart<Number,Number> chart = new LineChart<Number,Number>(x_axis,y_axis);
		chart.setLegendVisible(false);
        //chart.setCreateSymbols(false);
        return chart;
	}
	
	public void setPeriod(String freqUpdate,String freqSample){
		//PeriodCount = count;
		PeriodCount = (int)Misc.convertRatio(freqSample,freqUpdate);
		
		bufPrssVacc.clear();
		bufTempTube.clear();
		bufTempBuck.clear();
		
		String txt = "時間("+freqSample+")";
		chrPrss.getXAxis().setLabel(txt);
		chrTemp.getXAxis().setLabel(txt);
	}
	
	public PanInform() {

		getStyleClass().add("grid-small");
		setAlignment(Pos.CENTER);
		setMinSize(0,0);

		vumHook = VuMeterBuilder.create()
			.noOfLeds(25)
			//.peakValueVisible(true)
			.orientation(Orientation.VERTICAL)
			//.orientation(Orientation.HORIZONTAL)
			.sections(
				SectionBuilder.create().text("ggy1").start(0.0).stop(0.1).styleClass("led-section-0").build(),
				SectionBuilder.create().text("ggy2").start(0.1).stop(0.7).styleClass("led-section-1").build(),
				SectionBuilder.create().text("ggy3").start(0.7).stop(1.0).styleClass("led-section-2").build()
			).build();
		add(PanBase.decorate("掛勾高度",vumHook),2,0,1,2);
		
		chrPrss = init_chart("時間","pa");
		chrPrss.getData().add(serPrssVacc);
		
		chrTemp = init_chart("時間","ºC");
		chrTemp.getData().add(serTempTube);
		chrTemp.getData().add(serTempBuck);
		
		TabPane tabs = new TabPane();
		tabs.getStyleClass().add("tabs-body");
		tabs.setSide(Side.LEFT);
		tabs.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		
		Tab tabPress = new Tab();
		tabPress.setContent(chrPrss);
		tabPress.disableProperty().set(false);
		tabPress.setText("氣壓");
        
		Tab tabTemp = new Tab();
		tabTemp.setContent(chrTemp);
		tabTemp.disableProperty().set(false);
		tabTemp.setText("溫度");
		tabs.setPrefHeight(300);
		tabs.getTabs().addAll(tabPress,tabTemp);
		
		add(tabs,0,2,3,1);
	}
}
