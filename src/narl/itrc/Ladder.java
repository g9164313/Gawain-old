package narl.itrc;

import java.util.LinkedList;
import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import prj.scada.Step1;
import prj.scada.Step2;

/**
 * A panel for executing steps one by one by GUI thread.<p>
 * User can arrange step sequence.<p>
 * @author qq
 *
 */
public class Ladder extends BorderPane {

	public Ladder(){
		
		final JFXButton[] btn = new JFXButton[5];
		for(int i=0; i<btn.length; i++) {
			JFXButton obj = new JFXButton();
			obj.setMaxWidth(Double.MAX_VALUE);
			btn[i] = obj;
		}
		//Import procedure
		btn[0].setText("匯入");
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setGraphic(Misc.getIconView("database-import.png"));
		btn[0].disableProperty().bind(is_coming_down);
		//Export procedure
		btn[1].setText("匯出");
		btn[1].getStyleClass().add("btn-raised-1");
		btn[1].setGraphic(Misc.getIconView("database-export.png"));
		btn[1].disableProperty().bind(is_coming_down);
		//Run all steps
		btn[2].setText("執行");
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setGraphic(Misc.getIconView("run.png"));
		btn[2].disableProperty().bind(is_coming_down);
		btn[2].setOnAction(e->start_come_down());
		//Pause the current step~~
		btn[3].setText("暫停");
		btn[3].getStyleClass().add("btn-raised-2");
		btn[3].setGraphic(Misc.getIconView("pause.png"));
		btn[3].disableProperty().bind(is_coming_down.not());
		btn[3].setOnAction(e->pause_come_down(false));
		//Stop immediately
		btn[4].setText("停止");
		btn[4].getStyleClass().add("btn-raised-2");
		btn[4].setGraphic(Misc.getIconView("pan_tool.png"));
		btn[4].disableProperty().bind(is_coming_down.not());
		btn[4].setOnAction(e->pause_come_down(true));
		
		cassette.getStyleClass().addAll("box-pad");
		cassette.getChildren().addAll(
			btn[0],btn[1],
			btn[2],btn[3],btn[4]			
		);
				
		final VBox lay1 = new VBox();
		lay1.getStyleClass().addAll("box-pad");
		lay1.getChildren().addAll(recipe);
		VBox.setVgrow(recipe, Priority.ALWAYS);
		
		setLeft(cassette);
		setCenter(lay1);
		
		addCassette("test-1", Step1.class);
		addCassette("test-2", Step2.class);
	}
	//--------------------------------//
	
	private final JFXListView<Stepper> recipe = new JFXListView<Stepper>();
	
	private final VBox cassette = new VBox();
	
	public Ladder addCassette(
		final String title,
		final Class<?> stp
	){
		final JFXButton btn = new JFXButton(title);
		btn.getStyleClass().add("btn-raised-3");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.disableProperty().bind(is_coming_down);
		btn.setOnAction(e->{
			try {
				Stepper obj = (Stepper)stp.newInstance();
				obj.items = Optional.of(recipe.getItems());
				recipe.getItems().add(obj.doLayout());
			} catch (InstantiationException | IllegalAccessException e1) {
				e1.printStackTrace();
			}			
		});
		cassette.getChildren().add(btn);
		return this;
	}	
	//--------------------------------//
	

	private BooleanProperty is_coming_down = new SimpleBooleanProperty(false);
	
	private Optional<Timeline> timer = Optional.empty();
		
	private void start_come_down(){
		is_coming_down.set(true);
		if(timer.isPresent()==false) {
			timer = Optional.of(prepare_timer());
			if(timer.isPresent()==false){
				return;
			}
			timer.get().playFromStart();
		}else{
			timer.get().play();
		}
	}
	
	private void pause_come_down(boolean abort){
		if(timer.isPresent()==false) {
			return;
		}		
		if(abort){
			timer.get().stop();
			timer = Optional.empty();
			if(prevs.isPresent()==true){
				prevs.get().indicate(false);
				prevs = Optional.empty();
			}
		}else{
			timer.get().pause();
		}
		is_coming_down.set(false);
	}
	
	private class Footstep {
		Stepper  step;
		Runnable work;
		Footstep(Stepper stp, Runnable wrk){
			stp.result = 0;
			step = stp;			
			work = wrk;
		}
	};
	
	private LinkedList<Footstep> queue = new LinkedList<Footstep>();
	
	private Timeline prepare_timer(){
		//flatten all runnable works~~~
		ObservableList<Stepper> lst = recipe.getItems();
		for(Stepper stp:lst){
			if(stp.works.isPresent()==false){
				continue;
			}
			for(Runnable wrk:stp.works.get()){
				queue.addLast(new Footstep(stp,wrk));
			}
		}
		//setup timer to repeat all works~~~
		KeyFrame key = new KeyFrame(
			Duration.seconds(1.),
			e->come_down()
		);
		Timeline obj = new Timeline(key);
		obj.setCycleCount(Animation.INDEFINITE);
		//obj.setOnFinished(e->{});//no calling in INDEFINITE mode.
		return obj;
	}
	
	private Optional<Stepper> prevs = Optional.empty();
	
	private void come_down(){
		if(queue.isEmpty()==true){
			pause_come_down(true);
			return;
		}
		Footstep fs = queue.pollFirst();
		fs.step.indicate(true);
		if(prevs.isPresent()==false){
			prevs = Optional.of(fs.step);
		}else if(prevs.get()!=fs.step){
			prevs.get().indicate(false);
			prevs = Optional.of(fs.step);
		}
		fs.work.run();
		if(fs.step.result>0){
			queue.addFirst(fs);
		}else if(fs.step.result<0){			
			pause_come_down(true);
		}
	}
}
