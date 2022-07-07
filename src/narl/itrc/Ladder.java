package narl.itrc;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Scanner;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.sun.glass.ui.Application;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
@SuppressWarnings("restriction")
public class Ladder extends BorderPane {
	
	public Ladder(){
		
		timer.setCycleCount(Animation.INDEFINITE);
		//obj.setOnFinished(e->{});//no callback in INDEFINITE mode.
		
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
		btn[0].disableProperty().bind(is_running);
		btn[0].setOnAction(e->import_step());
		//Export procedure
		btn[1].setText("匯出");
		btn[1].getStyleClass().add("btn-raised-1");
		btn[1].setGraphic(Misc.getIconView("database-export.png"));
		btn[1].disableProperty().bind(is_running);
		btn[1].setOnAction(e->export_step());
		//clear all items
		btn[2].setText("清除");
		btn[2].getStyleClass().add("btn-raised-1");
		btn[2].setGraphic(Misc.getIconView("trash-can.png"));
		btn[2].disableProperty().bind(is_running);
		btn[2].setOnAction(e->recipe.getItems().clear());		
		//Run all steps
		btn[3].setText("執行");
		btn[3].getStyleClass().add("btn-raised-2");
		btn[3].setGraphic(Misc.getIconView("run.png"));
		btn[3].disableProperty().bind(is_running);
		btn[3].setOnAction(e->start());
		//Pause the current step~~
		btn[4].setText("暫停");
		btn[4].getStyleClass().add("btn-raised-2");
		btn[4].setGraphic(Misc.getIconView("pause.png"));
		btn[4].disableProperty().bind(is_running.not());
		btn[4].setOnAction(e->pause());
		//Stop immediately
		btn[5].setText("停止");
		btn[5].getStyleClass().add("btn-raised-0");
		btn[5].setGraphic(Misc.getIconView("pan_tool.png"));
		btn[5].disableProperty().bind(is_running.not());
		btn[5].setOnAction(e->{
			abort();
			user_abort();
		});
		
		main_kits.getStyleClass().addAll("box-pad");
		main_kits.getChildren().addAll(btn);
		
		step_kits.getStyleClass().addAll("box-pad");
		
		final TitledPane[] lay = {
			new TitledPane("操作",main_kits),
			new TitledPane("步驟",step_kits),
		};
		final Accordion accr = new Accordion(lay);
		accr.setExpandedPane(lay[1]);
		
		recipe.getStyleClass().addAll("box-pad","ss1","ss2");
		recipe.setMinWidth(250.);

		setLeft(accr);
		setCenter(recipe);
	}
	//--------------------------------//
	
	protected VBox main_kits = new VBox();
	protected VBox step_kits = new VBox();	
	protected JFXListView<Stepper> recipe = new JFXListView<Stepper>();

	/**
	 * the wrapper of stepper.<p>
	 * @author qq
	 *
	 */
	private class StpWrapper {
		String name;
		Class<?> clzz;
		Object[] args;
		
		StpWrapper(
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
				obj.items= Optional.of(recipe.getItems());
				obj.uuid = uuid_text;
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
	private ArrayList<StpWrapper> stp_wrapper = new ArrayList<StpWrapper>();
	
	private StpWrapper getPack(final Stepper src){
		final Class<?> s_clzz = src.getClass();
		for(StpWrapper dst:stp_wrapper){
			if(dst.clzz==s_clzz){
				return dst;
			}
		}
		return null;
	}
	private StpWrapper getPack(final String src){
		for(StpWrapper obj:stp_wrapper){
			String dst = obj.clzz.getName();
			if(dst.equals(src)==true){
				return obj;
			}
		}
		return stp_wrapper.get(0);
	}
	private StpWrapper getPack(final Class<?> src){
		for(StpWrapper obj:stp_wrapper){			
			if(src==obj.clzz){
				return obj;
			}
		}
		return stp_wrapper.get(0);
	}
	public Stepper genStep(final Class<?> clzz){
		return getPack(clzz).instance();		
	}
	
	public interface EventInitStep {
		void callback(Stepper stp);
	};
	public Ladder addStep(
		final String title,
		final Class<?> clazz,
		final Object... argument
	){
		return addStep(title,clazz,null,argument);
	}
	public Ladder addStep(
		final String title,
		final Class<?> clazz,
		final EventInitStep event,
		final Object... argument
	){
		final StpWrapper obj = new StpWrapper(title,clazz,argument);
		stp_wrapper.add(obj);
		return genButton(title,e->{
			final Stepper stp = obj.instance();
			if(event!=null) {
				event.callback(stp);
			}
		});
	}
	
	public interface EventInitSack {
		void callback(Stepper[] lst);
	};	
	public Ladder addSack(
		final String title,
		final Class<?>... sack
	){
		return addSack(title,null,sack);
	}
	public Ladder addSack(
		final String title,
		final EventInitSack event,
		final Class<?>... sack
	){	
		return genButton(title,e->{
			Stepper[] lst = new Stepper[sack.length];
			for(int i=0; i<sack.length; i++) {
				lst[i] = getPack(sack[i]).instance();
			}
			if(event!=null) {
				event.callback(lst);
			}
		});
	}
	
	protected Ladder genButton(
		final String title,
		final EventHandler<ActionEvent> event
	){
		final JFXButton btn = new JFXButton(title);
		btn.getStyleClass().add("btn-raised-3");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.disableProperty().bind(is_running);
		btn.setOnAction(event);
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
					final StpWrapper typ = getPack(arg[2].trim());
					if(typ==null){
						Misc.loge("[Ladder] 找不到 class - %s",arg[2]);
						continue;
					}
					Application.invokeLater(()->typ.instance().expand(arg[1].trim()));
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
					final StpWrapper wpp = getPack(stp);
					if(wpp==null){
						Misc.loge("[Ladder] 找不到 class - %s",stp.toString());
						continue;
					}
					out.write(String.format(
						"%s> %s @ %s", 
						wpp.name, 
						stp.flatten(), 
						wpp.clzz.getName()
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
	
	//private BooleanProperty is_climbing = new SimpleBooleanProperty(false);
	
	private final StpFoot ending = new StpFoot();
	
	private final ArrayDeque<StpFoot> queue = new ArrayDeque<StpFoot>();
	
	private final Timeline timer = new Timeline(new KeyFrame(
		Duration.millis(200),
		//Duration.seconds(1),
		e->climbing()
	));
	
	private final BooleanBinding is_running = timer.statusProperty().isEqualTo(Animation.Status.RUNNING);
	
	public Runnable prelogue = null;	
	public Runnable epilogue = null;
	public Runnable user_abort = null;
	private String uuid_text = "";//every running, we have a new UUID. 
	
	public String uuid() {
		return uuid_text;
	} 
	protected void prelogue() {
		uuid_text = UtilRandom.uuid(6,16);
		if(prelogue!=null) { prelogue.run(); }
	}
	protected void epilogue() {
		if(epilogue!=null) { epilogue.run(); }
		uuid_text = "";
	}
	protected void user_abort() {
		if(user_abort!=null) { user_abort.run(); }		
	}
	
	private void start(){		
		if(timer.getStatus()==Animation.Status.STOPPED) {
			prepare_all_foot();
			timer.playFromStart();
		}else if(timer.getStatus()==Animation.Status.PAUSED){
			timer.play();
		}		
	}
	private void abort(){		
		queue.forEach(foot->{
			//clear state and clock for next turn~~~
			if(foot.step==null){
				return;
			}
			foot.step.tick = 0L;
			foot.step.async.set(0);
		});
		timer.stop();
		recipe.getSelectionModel()
			.getSelectedItem()
			.imgSign
			.setVisible(false);
	}
	private void pause() {
		timer.pause();
	}
	
	private static class StpFoot {
		Stepper  step = null;
		Runnable work;
		StpFoot(Stepper stp, Runnable wrk){
			step = stp;			
			work = wrk;
		}
		StpFoot(){
			//this is special footstep for head and tail
			step = null;
			work = null;
		}
		void indicate(final boolean flag) {
			if(step==null) {return;}
			step.imgSign.setVisible(flag);
		}
		void working() {
			if(work==null) {return;}
			work.run();
		}
	};	

	private Timeline prepare_all_foot(){
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
				queue.addLast(new StpFoot(stp,wrk));
			}
		}
		//let timer know the working is ending
		queue.addLast(ending);
		//let user prepare something
		prelogue();
		//setup timer to repeat all works~~~
		return timer;
	}
	
	private void climbing(){
		StpFoot cur = queue.getFirst();
		StpFoot prv = queue.getLast();
		//check the tail, because it may be the previous step~~~
		if(cur.equals(ending)==true){			
			prv.indicate(false);
			abort();
			epilogue();
			return;
		}
		//update indicator icon in every stepper.
		recipe.getSelectionModel().select(cur.step);
		cur.indicate(true);
		if(cur.step.equals(prv.step)==false) {
			prv.indicate(false);
		}	

		//working!! After work is done, check asynchronous state.
		int async = cur.step.async.get();
		if(async==0){
			cur.working();	
		}else if(async>0){
			return;
		}else if(async<0){
			cur.step.async.set(0);//reset async flag!!!
		}
		//decide forward, backward or camp.
		//it means running sequence will be reordered.
		final int stp = cur.step.next.get();
		final int cnt = Math.abs(stp);
		final boolean dir = (stp>=0)?(true):(false);
		
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
		//reset the 'next' counter!!!
		cur.step.next_step();
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
