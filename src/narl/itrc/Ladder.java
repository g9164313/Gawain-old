package narl.itrc;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;
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
import javafx.scene.control.ButtonType;
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
		btn[3].setOnAction(e->starter());
		//Pause the current step~~
		btn[4].setText("暫停");
		btn[4].getStyleClass().add("btn-raised-2");
		btn[4].setGraphic(Misc.getIconView("pause.png"));
		btn[4].disableProperty().bind(is_climbing.not());
		btn[4].setOnAction(e->pause());
		//Stop immediately
		btn[5].setText("停止");
		btn[5].getStyleClass().add("btn-raised-2");
		btn[5].setGraphic(Misc.getIconView("pan_tool.png"));
		btn[5].disableProperty().bind(is_climbing.not());
		btn[5].setOnAction(e->abort());
		
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
				obj.ladder= Ladder.this;
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
	private ArrayList<StepPack> stpk = new ArrayList<StepPack>();
	
	private StepPack getPack(final Stepper src){
		final Class<?> s_clzz = src.getClass();
		for(StepPack dst:stpk){
			if(dst.clzz==s_clzz){
				return dst;
			}
		}
		return null;
	}
	private StepPack getPack(final String src){
		for(StepPack obj:stpk){
			String dst = obj.clzz.getName();
			if(dst.equals(src)==true){
				return obj;
			}
		}
		return null;
	}
	private StepPack getPack(final Class<?> src){
		for(StepPack obj:stpk){			
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
		stpk.add(obj);
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
	
	protected void import_step(){
		final PanBase pan = PanBase.self(this);
		final File fid = pan.loadFrom();
		if(fid==null){
			return;
		}
		if(recipe.getItems().size()!=0) {
			ButtonType btn = PanBase.notifyConfirm("", "清除舊步驟？");
			if(btn==ButtonType.OK) {
				recipe.getItems().clear();
			}
		}
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
	
	public Runnable prelogue = null;
	public Runnable epilogue = null;
	private String uuid_text = "";
	public String uuid() {
		return uuid_text;
	} 
	private void prelogue() {
		uuid_text = UtilRandom.uuid(6,16);
		if(prelogue!=null) { prelogue.run(); }
	}
	private void epilogue() {
		if(epilogue!=null) { epilogue.run(); }
		uuid_text = "";
	}
	
	private void starter(){
		is_climbing.set(true);
		if(timer.isPresent()==false) {
			timer = Optional.of(prepare_footstep());
			timer.get().playFromStart();
		}else{
			timer.get().play();
		}
	}
	private void abort(){
		try {
			timer.get().stop();
			is_climbing.set(false);
			timer = Optional.empty();
		}catch(NoSuchElementException e) {
		}
	}
	private void pause() {
		try {
			timer.get().pause();
			is_climbing.set(false);
		}catch(NoSuchElementException e) {
		}
	}
	
	private static class Footstep {
		Stepper  step;
		Runnable work;
		Footstep(Stepper stp, Runnable wrk){
			step = stp;			
			work = wrk;
		}
		Footstep(){
			//this is special footstep for head and tail
			step = null;
			work = null;
		}
		void indicate(final boolean flag) {
			if(step==null) {return;}
			step.indicator(flag);
		}
		void working() {
			if(work==null) {return;}
			work.run();
		}
	};	
	private final Footstep ending = new Footstep();
	
	private LinkedList<Footstep> queue = new LinkedList<Footstep>();
	
	private Timeline prepare_footstep(){
		//flatten all runnable works~~~
		queue.clear();
		ObservableList<Stepper> lst = recipe.getItems();
		//flatten running works in all steps
		for(Stepper stp:lst){
			stp.prepare();
			if(stp.works.isPresent()==false){
				continue;
			}			
			for(Runnable wrk:stp.works.get()){
				queue.addLast(new Footstep(stp,wrk));
			}
		}
		//let timer know the working is ending
		queue.addLast(ending);
		//let user prepare something
		prelogue();
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
		Footstep cur = queue.getFirst();
		Footstep prv = queue.getLast();
		//check the tail, because it may be the previous step~~~
		if(cur.equals(ending)==true){
			abort();
			epilogue();
			return;
		}
		//update indicator icon in every stepper.
		//TODO: when backward, how to indicate step?
		recipe.getSelectionModel().select(cur.step);
		cur.indicate(true);;
		prv.indicate(false);
		//working!! After work is done, check asynchronous state.
		if(cur.step.isAsync==false){
			cur.working();	
		}else{
			//Asynchronous case, other thread will change flag again.
			if(cur.step.next.get()!=Stepper.HOLD){
				cur.step.isAsync = false;//rest for next turn~~~~
			}
		}
		//decide forward, backward or camp.
		//it means running sequence will be reordered.
		int stp = cur.step.next.get();
		int cnt = Math.abs(stp);
		boolean dir = (stp>=0)?(true):(false);		
		if(cnt>=Stepper.SPEC_JUMP) {
			if(stp==Stepper.ABORT) {
				abort();
			}else if(stp==Stepper.PAUSE) {
				pause();
			}else {
				major_jump(cnt-Stepper.SPEC_JUMP,dir);
			}
		}else if(stp!=Stepper.HOLD) {
			minor_jump(cnt,dir);
		}
	}	
	private void major_jump(final int cnt, final boolean direction) {
		aligment_foot();
		for(int i=0; i<cnt; i++) {
			drawn_foot(direction);
		}
	}
	private void aligment_foot() {
		Stepper head = queue.getFirst().step;
		Stepper tail = queue.getLast().step;
		for(;head==tail;){
			queue.addFirst(queue.pollLast());
			tail = queue.getLast().step;		
		}
	}
	private void drawn_foot(final boolean flag) {
		Stepper aa = (flag==true)?(
			queue.getFirst().step
		):(
			queue.getLast().step
		);
		Stepper bb;
		do{
			if(flag==true) {
				queue.addLast(queue.pollFirst());
				bb = queue.getFirst().step;
			}else {
				queue.addFirst(queue.pollLast());
				bb = queue.getLast().step;
			}
			if(bb==null) {
				return;
			}
		}while(aa==bb);
	}
	private void minor_jump(final int cnt, final boolean flag) {
		for(int i=0; i<cnt; i++) {
			if(flag==true) {
				queue.addLast(queue.pollFirst());
			}else {
				queue.addFirst(queue.pollLast());
			}
		}
	}	
}
