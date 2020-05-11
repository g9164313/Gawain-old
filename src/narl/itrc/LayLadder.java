package narl.itrc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import javafx.concurrent.Task;
import javafx.scene.control.Accordion;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * A panel for executing steps one by one by GUI thread.<p>
 * User can arrange step sequence.<p>
 * @author qq
 *
 */
public class LayLadder extends BorderPane {
	
	public LayLadder(){
		
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
		btn[0].disableProperty().bind(is_climbing);
		btn[0].setOnAction(e->launch_task(taskImport));
		//Export procedure
		btn[1].setText("匯出");
		btn[1].getStyleClass().add("btn-raised-1");
		btn[1].setGraphic(Misc.getIconView("database-export.png"));
		btn[1].disableProperty().bind(is_climbing);
		btn[1].setOnAction(e->launch_task(taskOutport));
		//Run all steps
		btn[2].setText("執行");
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setGraphic(Misc.getIconView("run.png"));
		btn[2].disableProperty().bind(is_climbing);
		btn[2].setOnAction(e->start_climbing());
		//Pause the current step~~
		btn[3].setText("暫停");
		btn[3].getStyleClass().add("btn-raised-2");
		btn[3].setGraphic(Misc.getIconView("pause.png"));
		btn[3].disableProperty().bind(is_climbing.not());
		btn[3].setOnAction(e->pause_climbing(false));
		//Stop immediately
		btn[4].setText("停止");
		btn[4].getStyleClass().add("btn-raised-2");
		btn[4].setGraphic(Misc.getIconView("pan_tool.png"));
		btn[4].disableProperty().bind(is_climbing.not());
		btn[4].setOnAction(e->pause_climbing(true));
		
		cassette.getStyleClass().addAll("box-pad");
		cassette.getChildren().addAll(
			btn[0],btn[1],
			btn[2],btn[3],btn[4]			
		);
		
		final VBox lay3 = new VBox(btn);
		lay3.getStyleClass().addAll("box-pad");
		
		final TitledPane[] lay2 = {
			new TitledPane("操作",lay3),
			new TitledPane("步驟",cassette),
		};
		final Accordion accr = new Accordion(lay2);
		accr.setExpandedPane(lay2[0]);
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().addAll("box-pad");
		lay1.getChildren().addAll(recipe);
		VBox.setVgrow(recipe, Priority.ALWAYS);
		
		setLeft(accr);
		setCenter(lay1);
	}
	//--------------------------------//
	
	public Task<Integer> taskImport = null;
	public Task<Integer> taskOutport= null;
	
	private void launch_task(Task<?> tsk){
		if(tsk==null){
			PanBase.notifyWarning("!!警告!!", "不支援此動作。");
		}else{
			((PanBase)getScene().getUserData()).notifyTask(tsk);
		}
	}
	
	private final JFXListView<Stepper> recipe = new JFXListView<Stepper>();
	
	private final VBox cassette = new VBox();
	
	public LayLadder addCassette(
		final String title,
		final Class<?> stp,
		final Object... args
	){
		final JFXButton btn = new JFXButton(title);
		btn.getStyleClass().add("btn-raised-3");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.disableProperty().bind(is_climbing);
		btn.setOnAction(e->{
			try {
				Stepper obj = null;
				if(args.length==0){
					obj = (Stepper)stp.newInstance();
				}else{
					for(Constructor<?> cnst:stp.getConstructors()){
						if(cnst.getParameterCount()==args.length){
							obj = (Stepper) cnst.newInstance(args);
							break;
						}
					}
				}
				if(obj==null){
					Misc.logw("Invalid stepper class...");
					return;
				}
				obj.items = Optional.of(recipe.getItems());
				recipe.getItems().add(obj.doLayout());
			} catch (InstantiationException | IllegalAccessException e1) {
				e1.printStackTrace();
			} catch (IllegalArgumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}			
		});
		cassette.getChildren().add(btn);
		return this;
	}	
	//--------------------------------//
	

	private BooleanProperty is_climbing = new SimpleBooleanProperty(false);
	
	private Optional<Timeline> timer = Optional.empty();
		
	private void start_climbing(){
		is_climbing.set(true);
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
	
	private void pause_climbing(boolean abort){
		if(timer.isPresent()==false) {
			return;
		}		
		if(abort){
			timer.get().stop();
			timer = Optional.empty();
		}else{
			timer.get().pause();
		}
		is_climbing.set(false);
	}
	
	private static class Footstep {
		Stepper  step;
		Runnable work;
		Footstep(Stepper stp, Runnable wrk){
			stp.reset();
			step = stp;			
			work = wrk;
		}
		Footstep(){
			step = null;
			work = null;
		}
		void indicator(boolean flag){
			if(step==null){
				return;
			}
			step.indicator(flag);
		}
	};
	private static final Footstep end_step = new Footstep();
	
	private LinkedList<Footstep> queue = new LinkedList<Footstep>();
	
	private Timeline prepare_timer(){
		//flatten all runnable works~~~
		queue.clear();
		ObservableList<Stepper> lst = recipe.getItems();
		for(Stepper stp:lst){
			if(stp.works.isPresent()==false){
				continue;
			}
			for(Runnable wrk:stp.works.get()){
				queue.addLast(new Footstep(stp,wrk));
			}
		}
		//put a special stepper for ending queue~~
		queue.addLast(end_step);
		
		//setup timer to repeat all works~~~
		KeyFrame key = new KeyFrame(
			Duration.seconds(0.5),
			e->climbing()
		);
		Timeline obj = new Timeline(key);
		obj.setCycleCount(Animation.INDEFINITE);
		//obj.setOnFinished(e->{});//no calling in INDEFINITE mode.
		return obj;
	}
	
	private void climbing(){
		if(queue.isEmpty()==true){
			pause_climbing(true);
			return;
		}
		Footstep fst = queue.getFirst();
		Footstep lst = queue.getLast();
		//check whether is the last step~~~
		if(fst.equals(end_step)==true){
			pause_climbing(true);
			return;
		}
		//update indicator icon in every stepper.
		if(fst.step!=lst.step){
			fst.indicator(true);
			lst.indicator(false);
		}
		//working, working....
		fst.work.run();
		//decide forward, backward or camp.
		jumping(fst.step.result.get());
	}
	
	private void jumping(final int count){
		switch(count){
		case Stepper.HOLD: 
			return;
		case Stepper.PAUSE: 
			pause_climbing(false);
			return;
		case Stepper.ABORT:
			pause_climbing(true);
			return;
		}
		for(int i=0; i<Math.abs(count); i++){
			if(count>0){
				//forward steps...
				queue.addLast(queue.pollFirst());
			}else{
				//backward steps...
				queue.addFirst(queue.pollLast());
			}
		}
	}
}
