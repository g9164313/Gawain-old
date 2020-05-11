package prj.scada;

import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import narl.itrc.PadTouch;
import narl.itrc.Stepper;

public class StepDelay extends Stepper {

	public StepDelay(){
		set(running);
	}
	
	private long tick = -1L;
	private long span = 10000L;//unit is millisecond.
	private long remain = 0L;//unit is millisecond.
	
	private StringProperty msg = new SimpleStringProperty();
	
	final Runnable running = ()->{
		if(tick<=0){
			tick = System.currentTimeMillis();
			result.set(HOLD);
			return;
		}
		remain = System.currentTimeMillis() - tick;
		if(remain<span){
			result.set(HOLD);
		}else{
			//for next turn~~~
			tick = -1L;
			result.set(NEXT);
		}
		tick2mesg();
	};
	
	private void tick2mesg(){
		long sec = span / 1000L;
		long min = sec / 60; sec = sec % 60;
		long hour= min / 60; min = min % 60;
		String txt1 = value2text(hour,min,sec);
		
		sec = remain / 1000L;
		min = sec / 60; sec = sec % 60;
		hour= min / 60; min = min % 60;
		String txt2 = value2text(hour,min,sec);
		
		msg.set("延遲:"+txt1+"/"+txt2);
	}
	
	private String value2text(long... time){
		if(time[0]!=0){
			return String.format(
				" %2d時 %2d分 %2d秒", 
				time[0], time[1], time[2]
			);
		}else if(time[1]!=0){
			return String.format(
				" %2d分 %2d秒",
				time[1], time[2]
			);
		}
		return String.format(" %2d秒",time[2]);
	}
	
	@Override
	protected Node getContent(){
		tick2mesg();
		Label txt = new Label();
		txt.setMaxWidth(Double.MAX_VALUE);
		//txt.getStyleClass().addAll("border");
		txt.textProperty().bind(msg);
		return txt;
	}
	
	@Override
	protected void eventEdit(){
		PadTouch pad = new PadTouch("時間(mm:ss)",'c');
		Optional<String> val = pad.showAndWait();
		if(val.isPresent()==false) {
			return;
		}
		//change value to millisecond
		span = Long.valueOf(PadTouch.toMillsec(val.get()));
		tick2mesg();
	}
}
