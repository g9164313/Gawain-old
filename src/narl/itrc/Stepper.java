package narl.itrc;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.jfoenix.controls.JFXButton;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class Stepper extends HBox {
	
	public static final int PAUSE =  10001;//do we need this???
	public static final int NEXT  =      1;//next step
	public static final int HOLD  =      0;//camp or don't move
	public static final int ABORT = -10001;
	
	private static final Image v_empty = new WritableImage(24,24);
	private static final Image v_arrow = Misc.getIconImage("arrow-right.png");
	private static final Image v_pen = Misc.getIconImage("pen.png");
	private static final Image v_tash_can = Misc.getIconImage("trash-can.png");
	
	public final AtomicInteger result = new AtomicInteger(HOLD);
		
	private ImageView imgSign = new ImageView();

	private JFXButton btnEdit = new JFXButton();
	private JFXButton btnDrop = new JFXButton();
	
	public Optional<Runnable[]> works = Optional.empty();
	public Optional<ObservableList<Stepper>> items = Optional.empty();
	
	public Stepper(){
		imgSign.setImage(v_empty);
	}
	
	public void indicator(boolean flag){
		if(flag){
			imgSign.setImage(v_arrow);
		}else{
			imgSign.setImage(v_empty);
		}
	}
	
	public Stepper doLayout(){
		
		indicator(false);
		
		Node cntxt = getContent();
		HBox.setHgrow(cntxt, Priority.ALWAYS);
		
		btnEdit.setGraphic(new ImageView(v_pen));
		btnEdit.setOnAction(e->eventEdit());
		
		btnDrop.setGraphic(new ImageView(v_tash_can));
		btnDrop.setOnAction(e->dropping());
				
		setAlignment(Pos.CENTER_LEFT);
		getChildren().addAll(imgSign,cntxt,btnEdit,btnDrop);		
		return this;
	}
		
	private void dropping(){
		if(items.isPresent()==false){
			return;
		}
		items.get().remove(this);
	}
	
	protected Node getContent(){
		Label txt = new Label("-DEFAULT-");
		txt.setMaxWidth(Double.MAX_VALUE);
		txt.getStyleClass().addAll("border");
		return txt;
	}
	
	protected void eventEdit(){
		//let user pop a panel to edit parameter~~~
	}
	
	protected void set(Runnable run){
		Runnable[] tmp = {run};
		works = Optional.of(tmp);
	}
	
	protected void set(Runnable... runs){
		works = Optional.of(runs);
	}
	
	public boolean waiting_async = false;
	protected void waiting_async(){
		waiting_async = true;
		result.set(HOLD);
	}
	
	private long tick = -1L;	
	protected long waiting(long period){
		if(tick<=0L){
			tick = System.currentTimeMillis();
		}
		long pass = System.currentTimeMillis() - tick;
		if(pass>=period){
			tick = -1L;//reset for next turn~~~
			result.set(NEXT);
		}else{
			result.set(HOLD);
		}
		long rem = period - pass;
		return (rem>0)?(rem):(0);
	}
	protected long waiting(final String time){
		return waiting(Misc.time2tick(time));
	}
	
	protected void prepare(){
		indicator(false);
		result.set(HOLD);
	}
}
