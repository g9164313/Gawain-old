package narl.itrc;

import java.util.ArrayList;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.NumberBinding;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.Duration;

public class WidValueNote extends LineChart<Number,Number>{
	
	private XYChart.Series<Number,Number> marks = new XYChart.Series<Number,Number>();
	
	private XYChart.Series<Number,Number> series = new XYChart.Series<Number,Number>();

	@SuppressWarnings("unchecked")
	public WidValueNote(){
		
		super(new NumberAxis(),new NumberAxis());
		
		setAnimated(false);
		setCreateSymbols(false);
		
		for(int i=0; i<SLOT_SIZE; i++){
			series.getData().add(new XYChart.Data<Number,Number>((i*DURATION)/1000.f,0));
		}
		marks.getData().add(new XYChart.Data<Number,Number>(0,0));
		marks.getData().add(new XYChart.Data<Number,Number>(0,100));
		
		getData().addAll(series,marks);
		
		NumberAxis axs;
		//set x-axis
		axs = (NumberAxis) getXAxis();
		axs.setAutoRanging(false);
		axs.setAnimated(false);
		axs.setLowerBound(0);
		axs.setUpperBound(PERIOD/1000);//second as unit!!!
		axs.setTickUnit(60);//minute as tick!!!
		axs.setMinorTickCount(10);		
		axs.setForceZeroInRange(false);
		//set y-axis
		axs = (NumberAxis) getYAxis();
		axs.setAutoRanging(false);
		axs.setAnimated(false);
		axs.setMinorTickCount(1);
	}
	

	public WidValueNote setRange(double min,double max, double tick){
		NumberAxis axs = (NumberAxis) getYAxis();
		axs.setLowerBound(min);
		axs.setUpperBound(max);
		marks.getData().get(0).setYValue(min);
		marks.getData().get(1).setYValue(max);
		//axs.setMinorTickCount(1);
		//axs.setTickUnit(tick);
		return this;
	}
	
	public WidValueNote setRange(double min,double max){
		NumberAxis axs = (NumberAxis) getYAxis();
		axs.setLowerBound(min);
		axs.setUpperBound(max);
		marks.getData().get(0).setYValue(min);
		marks.getData().get(1).setYValue(max);
		//axs.setMinorTickCount(1);
		return this;
	}	
	//------------------------------//
	
	public WidValueNote bind(NumberBinding prop){
		lstInstance.add(new ItemValue(prop, this));
		return this;
	}
	
	private static class ItemValue {	
		private NumberBinding prop;
		private WidValueNote note;
		public ItemValue(NumberBinding p, WidValueNote n){
			prop = p;
			note = n;
		}
	};
	
	private static final int PERIOD = 5*60*1000;//millisecond	
	private static final int DURATION = 1000;//millisecond, TODO: 不要設低於 1sec，啟動速度會變慢
	private static final int SLOT_SIZE = PERIOD / DURATION;
		
	private static ArrayList<ItemValue> lstInstance = new ArrayList<ItemValue>();
	
	private static EventHandler<ActionEvent> eventMonitor = new EventHandler<ActionEvent>(){
		
		private int indx = 0;
		@Override
		public void handle(ActionEvent event) {
			
			for(ItemValue itm:lstInstance){
				
				itm.note.series.getData()
					.get(indx)
					.setYValue(itm.prop.getValue());
				
				float tick = (indx*DURATION)/1000.f;
				itm.note.marks.getData()
					.get(0)
					.setXValue(tick);
				itm.note.marks.getData()
					.get(1)
					.setXValue(tick);
			}
			//System.out.println("update indx="+tick);
			indx = indx + 1;
			indx = indx % SLOT_SIZE;
		}
	};
	
	private static Timeline monitor = new Timeline(new KeyFrame(
		Duration.millis(DURATION),
		eventMonitor
	));
	
	static {
		monitor.setCycleCount(Timeline.INDEFINITE);
		monitor.play();
	}
	//------------------------------//
	
	/*public WidValueNote bind(NumberBinding prop){
		prop.addListener(event);
		return this;
	}
	
	public WidValueNote bind(IntegerBinding prop){
		prop.addListener(event);		
		return this;
	}
	
	private ChangeListener<Number> event = new ChangeListener<Number>(){
		@Override
		public void changed(
			ObservableValue<? extends Number> observable, 
			Number oldValue, 
			Number newValue
		) {	
			long curTick = System.currentTimeMillis();
			ObservableList<Data<Number,Number>> lst = series.getData();		
			
			XYChart.Data<Number,Number> cur = new XYChart.Data<Number,Number>(0,0);
			curTick = curTick % (PERIOD*1000L);
			float cur_tick = (float)(curTick) / 1000.f;
			cur.setXValue(cur_tick);
			cur.setYValue(newValue);
			lst.add(cur);
			if(lst.size()==1){
				return;//skip the first-one...
			}
			
			XYChart.Data<Number,Number> prv = lst.get(0);
			float pre_tick = (float)prv.getXValue();
			long diffTick = (long)(cur_tick-pre_tick);
			if(diffTick<0){
				lst.clear();
				return;
			}else if(diffTick<=PERIOD){
				return;
			}
			lst.remove(0);
		}
	};*/
}
