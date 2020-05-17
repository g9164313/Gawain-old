package narl.itrc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
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
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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
		btn[2].setOnAction(e->start_again());
		//Pause the current step~~
		btn[3].setText("暫停");
		btn[3].getStyleClass().add("btn-raised-2");
		btn[3].setGraphic(Misc.getIconView("pause.png"));
		btn[3].disableProperty().bind(is_climbing.not());
		btn[3].setOnAction(e->abort_or_pause(false));
		//Stop immediately
		btn[4].setText("停止");
		btn[4].getStyleClass().add("btn-raised-2");
		btn[4].setGraphic(Misc.getIconView("pan_tool.png"));
		btn[4].disableProperty().bind(is_climbing.not());
		btn[4].setOnAction(e->abort_or_pause(true));
				
		steps.getStyleClass().addAll("box-pad");
		steps.getChildren().addAll(
			btn[0],btn[1],
			btn[2],btn[3],btn[4]			
		);
		
		final VBox lay3 = new VBox(btn);
		lay3.getStyleClass().addAll("box-pad");
		
		final TitledPane[] lay2 = {
			new TitledPane("操作",lay3),
			new TitledPane("步驟",steps),
		};
		final Accordion accr = new Accordion(lay2);
		accr.setExpandedPane(lay2[0]);
		
		recipe.getStyleClass().addAll("box-pad","ss1","ss2");
		setLeft(accr);
		setCenter(recipe);
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
	
	private final VBox steps = new VBox();
	
	public Ladder addStep(
		final String title,
		final Class<?> stp,
		final Object... arg
	){
		final JFXButton btn = new JFXButton(title);
		btn.getStyleClass().add("btn-raised-3");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.disableProperty().bind(is_climbing);
		btn.setOnAction(e->step_instance(stp,arg));
		steps.getChildren().add(btn);
		return this;
	}
	
	public Ladder addStepBag(
		final String title,
		final Object[][] bag
	){
		final JFXButton btn = new JFXButton(title);
		btn.getStyleClass().add("btn-raised-3");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.disableProperty().bind(is_climbing);
		btn.setOnAction(e->{
			for(Object[] obj:bag){
				if(obj.length>1){					
					step_instance(
						(Class<?>)obj[0],
						Arrays.copyOfRange(obj, 1, obj.length)
					);
				}else{
					step_instance((Class<?>)obj[0]);
				}
			}
		});
		steps.getChildren().add(btn);
		return this;
	}
	
	private void step_instance(
		final Class<?> step,
		final Object... args
	){
		try {
			Stepper obj = null;
			if(args.length==0){
				obj = (Stepper)step.newInstance();
			}else{
				for(Constructor<?> cnst:step.getConstructors()){
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
			obj = obj.doLayout();
			recipe.getItems().add(obj);
			recipe.scrollTo(obj);
		} catch (InstantiationException | IllegalAccessException e1) {
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		} catch (InvocationTargetException e1) {
			e1.printStackTrace();
		}
	}
	
	public void setPrelogue(final Runnable run){
		beg_step.work = run;
	}
	public void setEpilogue(final Runnable run){
		end_step.work = run;
	}
	//--------------------------------//
	

	private BooleanProperty is_climbing = new SimpleBooleanProperty(false);
	
	private Optional<Timeline> timer = Optional.empty();
		
	private void start_again(){
		is_climbing.set(true);
		if(timer.isPresent()==false) {
			timer = Optional.of(prepare_footstep());
			if(timer.isPresent()==false){
				return;
			}
			timer.get().playFromStart();
			//recipe.setDisable(true);
		}else{
			timer.get().play();
		}
	}
	
	private void abort_or_pause(boolean abort){
		if(timer.isPresent()==false) {
			return;
		}		
		if(abort){
			timer.get().stop();
			timer = Optional.empty();
			//recipe.setDisable(false);
		}else{
			timer.get().pause();
		}
		is_climbing.set(false);
	}
	
	private static class Footstep {
		Stepper  step;
		Runnable work;
		Footstep(Stepper stp, Runnable wrk){
			stp.result.set(Stepper.NEXT);
			step = stp;			
			work = wrk;
		}
		Footstep(){
			step = null;
			work = null;
		}
		void indicator(boolean flag){
			if(step!=null){
				step.indicator(flag);
			}
		}
		void make(){
			if(work!=null){
				work.run();
			}
		}
	};	
	private static final Footstep beg_step = new Footstep();
	private static final Footstep end_step = new Footstep();
	
	private LinkedList<Footstep> queue = new LinkedList<Footstep>();
	
	private Timeline prepare_footstep(){
		//flatten all runnable works~~~
		queue.clear();
		ObservableList<Stepper> lst = recipe.getItems();
		for(Stepper stp:lst){
			stp.prepare();
			if(stp.works.isPresent()==false){
				continue;
			}			
			for(Runnable wrk:stp.works.get()){
				queue.addLast(new Footstep(stp,wrk));
			}
		}
		//put a special stepper for ending queue~~
		queue.addFirst(beg_step);
		queue.addLast(end_step);
		
		//setup timer to repeat all works~~~
		KeyFrame key = new KeyFrame(
			Duration.seconds(1.),
			e->climbing()
		);
		Timeline obj = new Timeline(key);
		obj.setCycleCount(Animation.INDEFINITE);
		//obj.setOnFinished(e->{});//no calling in INDEFINITE mode.
		return obj;
	}
	
	private void climbing(){
		//check the tail, because it is the previous step~~~
		Footstep cur = queue.getFirst();
		Footstep prv = queue.getLast();
		//check whether it is the special step.
		if(cur.equals(beg_step)==true){
			cur.make();
			queue.pollFirst();//remove the first one~~~
			return;
		}else if(cur.equals(end_step)==true){
			cur.make();
			abort_or_pause(true);
			return;
		}
		//update indicator icon in every stepper.
		//TODO: when backward, how to indicate step?
		recipe.getSelectionModel().select(cur.step);
		if(cur.step!=prv.step){
			cur.indicator(true);
			prv.indicator(false);
		}
		if(cur.step.waiting_async==false){
			//working, working....		
			cur.make();		
		}else{
			if(cur.step.result.get()!=Stepper.HOLD){
				cur.step.waiting_async = false;
			}
		}
		//decide forward, backward or camp.
		jumping(cur.step.result.get());
	}
	
	private void jumping(final int count){
		switch(count){
		case Stepper.HOLD: 
			return;
		case Stepper.PAUSE: 
			abort_or_pause(false);
			return;
		case Stepper.ABORT:
			abort_or_pause(true);
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
