package narl.itrc;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Scanner;

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

		final JFXButton[] btn = new JFXButton[6];
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
		btn[0].setOnAction(e->import_step());
		//Export procedure
		btn[1].setText("匯出");
		btn[1].getStyleClass().add("btn-raised-1");
		btn[1].setGraphic(Misc.getIconView("database-export.png"));
		btn[1].disableProperty().bind(is_climbing);
		btn[1].setOnAction(e->export_step());
		//clear all items
		btn[2].setText("清除");
		btn[2].getStyleClass().add("btn-raised-1");
		btn[2].setGraphic(Misc.getIconView("trash-can.png"));
		btn[2].disableProperty().bind(is_climbing);
		btn[2].setOnAction(e->recipe.getItems().clear());		
		//Run all steps
		btn[3].setText("執行");
		btn[3].getStyleClass().add("btn-raised-2");
		btn[3].setGraphic(Misc.getIconView("run.png"));
		btn[3].disableProperty().bind(is_climbing);
		btn[3].setOnAction(e->start_again());
		//Pause the current step~~
		btn[4].setText("暫停");
		btn[4].getStyleClass().add("btn-raised-2");
		btn[4].setGraphic(Misc.getIconView("pause.png"));
		btn[4].disableProperty().bind(is_climbing.not());
		btn[4].setOnAction(e->abort_or_pause(false));
		//Stop immediately
		btn[5].setText("停止");
		btn[5].getStyleClass().add("btn-raised-2");
		btn[5].setGraphic(Misc.getIconView("pan_tool.png"));
		btn[5].disableProperty().bind(is_climbing.not());
		btn[5].setOnAction(e->abort_or_pause(true));
		
		main_kits.getStyleClass().addAll("box-pad");
		main_kits.getChildren().addAll(btn);
		
		step_kits.getStyleClass().addAll("box-pad");
		
		final TitledPane[] lay2 = {
			new TitledPane("操作",main_kits),
			new TitledPane("步驟",step_kits),
		};
		final Accordion accr = new Accordion(lay2);
		accr.setExpandedPane(lay2[0]);
		
		recipe.getStyleClass().addAll("box-pad","ss1","ss2");
		setLeft(accr);
		setCenter(recipe);
	}
	//--------------------------------//
	
	protected VBox main_kits = new VBox();
	protected VBox step_kits = new VBox();	
	protected JFXListView<Stepper> recipe = new JFXListView<Stepper>();
	
	/**
	 * the container of stepper.<p>
	 * @author qq
	 *
	 */
	private class StepPack {
		String name;
		Class<?> clzz;
		Object[] args;
		StepPack(
			String title,
			Class<?> class_type, 
			Object... argument
		){
			name = title;
			clzz = class_type;
			args = argument;
		}		
		Stepper instance(){
			Stepper obj = null;
			try {				
				if(args.length==0){
					obj = (Stepper)clzz.newInstance();
				}else{
					for(Constructor<?> cnst:clzz.getConstructors()){
						if(cnst.getParameterCount()==args.length){
							obj = (Stepper) cnst.newInstance(args);
							break;
						}
					}
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
			return obj;
		}
	};
	private ArrayList<StepPack> steps = new ArrayList<StepPack>();
	
	private StepPack getPack(final Stepper src){
		final Class<?> s_clzz = src.getClass();
		for(StepPack dst:steps){
			if(dst.clzz==s_clzz){
				return dst;
			}
		}
		return null;
	}
	private StepPack getPack(final String src){
		for(StepPack obj:steps){
			String dst = obj.clzz.getName();
			if(dst.equals(src)==true){
				return obj;
			}
		}
		return null;
	}
	private StepPack getPack(final Class<?> src){
		for(StepPack obj:steps){			
			if(src==obj.clzz){
				return obj;
			}
		}
		return null;
	}
	public Stepper genStep(final Class<?> clzz){
		return getPack(clzz).instance();		
	}
	
	public Ladder addStep(
		final String title,
		final Class<?> clazz,
		final Object... argument
	){
		final StepPack obj = new StepPack(title,clazz,argument);
		steps.add(obj);
		final JFXButton btn = new JFXButton(title);
		btn.getStyleClass().add("btn-raised-3");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.disableProperty().bind(is_climbing);
		btn.setOnAction(e->obj.instance());
		step_kits.getChildren().add(btn);
		return this;
	}
	public Ladder addSack(
		final String title,
		final Class<?>... sack
	){
		final JFXButton btn = new JFXButton(title);
		btn.getStyleClass().add("btn-raised-3");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.disableProperty().bind(is_climbing);
		btn.setOnAction(e->{
			for(Class<?> clzz:sack){
				getPack(clzz).instance();
			}
		});
		step_kits.getChildren().add(btn);
		return this;
	}	
	//--------------------------------//
	
	public void setPrelogue(final Runnable run){
		beg_step.work = run;
	}
	public void setEpilogue(final Runnable run){
		end_step.work = run;
	}
	
		
	protected void import_step(){
		final PanBase pan = PanBase.self(this);
		final File fid = pan.loadFrom();
		if(fid==null){
			return;
		}
		recipe.getItems().clear();
		final Task<?> tsk = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				Scanner stm = new Scanner(fid);
				int l_id = 0;
				while(stm.hasNextLine()==true){
					l_id+=1;
					String txt = stm.nextLine().replace("\r\n", "");
					if(txt.matches(".*[>].*[@].*")==false){
						updateMessage("無法解析的內文: L"+l_id);
						continue;
					}
					final String[] arg = txt.split("[>]|[@]");
					final StepPack typ = getPack(arg[2].trim());
					if(typ==null){
						Misc.loge("[Ladder] 找不到 class - %s",arg[2]);
						continue;
					}
					Misc.invoke(()->typ.instance().expand(arg[1].trim()));
					updateMessage(String.format("匯入 %s", arg[0]));
				}
				stm.close();
				return 3;
			}
		};
		pan.notifyTask(tsk);
	}
	
	protected void export_step(){
		final PanBase pan = PanBase.self(this);
		final File fid = pan.saveAs("recipe.txt");
		if(fid==null){
			return;
		}
		final Task<?> tsk = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				ObservableList<Stepper> lst = recipe.getItems();
				FileWriter out = new FileWriter(fid);				
				for(int i=0; i<lst.size(); i++){
					updateMessage(String.format(
						"匯出中 %2d/%2d",
						i+1, lst.size()
					));
					final Stepper stp = lst.get(i);
					final StepPack typ = getPack(stp);
					if(typ==null){
						Misc.loge("[Ladder] 找不到 class - %s",stp.toString());
						continue;
					}
					out.write(String.format(
						"%s> %s @ %s", 
						typ.name, 
						stp.flatten(), 
						typ.clzz.getName()
					));
					out.write("\r\n");
				}				
				out.close();
				return 0;
			}
		};
		pan.notifyTask(tsk);		
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
