package narl.itrc;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.sun.glass.ui.Application;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.util.Duration;

public abstract class DevBase implements Gawain.EventHook {

	public DevBase(){
		Gawain.hook(this);
	}
	
	/**
	 * This property indicates whether device is connected or not.
	 */
	private BooleanProperty isAlive = new SimpleBooleanProperty(false);
	
	/**
	 * Other-thread may update the device condition.<p>
	 * Directly changing property value will induce safety-exception.<p>
	 * @param flag - set boolean value
	 */
	protected void setAlive(final boolean flag){
		if(Application.isEventThread()==true){
			isAlive.set(flag);
		}else if(Application.GetApplication()!=null){
			//When program is destroyed, the method, 'GetApplication()' will get null!!!!
			Application.invokeAndWait(()->isAlive.set(flag));
		}
	}
	
	public ReadOnlyBooleanProperty isAlive(){
		return BooleanProperty.readOnlyBooleanProperty(isAlive);
	}
	
	/**
	 * Invoke this event when we need to prepare something, just one time.<p>
	 * And this is invoked by GUI-thread
	 */
	@Override
	public void kickoff() {
	}
	
	/**
	 * Invoke a event to close device or release resources.<p>
	 * This will be invoked at the end of program.<p>
	 */
	@Override
	public void shutdown() {
		finishTask();
		eventShutdown();
	}
	
	/**
	 * In this point, device can release its resource!!! <p>
	 */
	public abstract void eventShutdown();
	
	/**
	 * All devices need a view(console) to control its behaviors.<p>
	 * So, override this method to generate a control-view.
	 * @return
	 */
	protected Node eventLayout(PanBase pan){
		return null;
	}

	public PanBase showConsole(){
		return showConsole("");
	}
	
	public PanBase showConsole(final String title){
		return new PanBase(title){
			@Override
			public Node eventLayout(PanBase pan) {				
				return DevBase.this.eventLayout(pan);
			}
			@Override
			public void eventShown(PanBase self) {
			}
		}.appear();
	}
	//---------------------------//
	
	private ConcurrentLinkedQueue<TaskEvent> taskQueue;
	
	private ArrayList<TaskEvent> taskUsual = new ArrayList<TaskEvent>();//repeated-routine
	
	private Task<Long> taskLoop;
		
	private class TaskCore extends Task<Long>{
		@Override
		protected Long call() throws Exception {
			
			if(taskInit()==true){
				return -1L;
			}
			
			long tk0, tk1, tk2;
			tk0 = System.currentTimeMillis();
			while(isCancelled()==false){					
				if(Application.GetApplication()==null){
					taskFinal();
					return -2L;
				}
				tk1 = System.currentTimeMillis();
				
				TaskEvent e = taskQueue.poll();				
				if(e!=null){
					e.fireEvent();
				}else if(taskUsual!=null){
					taskUsual.forEach(obj->{
						obj.fireEvent();
					});
				}

				tk2 = System.currentTimeMillis();
				updateValue(tk2-tk0);				
				updateProgress(tk2-tk1,5000L);
			}
			taskFinal();
			return -4L;
		}
	}
	
	protected boolean taskInit(){
		return false;
	}
	
	protected void taskFinal(){
	}

	public void addUsual(TaskEvent event){
		taskUsual.add(event);
	}
	public void addUsual(
		EventHandler<ActionEvent> pro
	){
		addUsual(new TaskEvent(null,pro,null));
	}
	public void addUsual(
		EventHandler<ActionEvent> pro, 
		EventHandler<ActionEvent> epi
	){
		addUsual(new TaskEvent(null,pro,epi));
	}
	
	public void delUsual(TaskEvent event){
		taskUsual.remove(event);
	}
	
	public void addEvent(TaskEvent e){
		if(e.node!=null){
			taskQueue.forEach(obj->{
				if(obj.node.equals(e.node)==true){
					return;
				}
			});
		}
		taskQueue.add(e);
	}
	public void addEvent(
		EventHandler<ActionEvent> pro
	){
		addEvent(new TaskEvent(null,pro,null));
	}
	public void addEvent(
		Control nod,
		EventHandler<ActionEvent> pro
	){
		addEvent(new TaskEvent(nod,pro,null));
	}
	public void addEvent(
		Control nod,
		EventHandler<ActionEvent> pro, 
		EventHandler<ActionEvent> epi
	){
		addEvent(new TaskEvent(nod,pro,epi));
	}
	
	public void launchTask(String name){
		if(taskQueue!=null){
			taskQueue.clear();
		}else{
			taskQueue= new ConcurrentLinkedQueue<TaskEvent>();
		}
		if(taskLoop!=null){
			if(taskLoop.isDone()==false){
				return;
			}
		}
		//task may be in finished-state, just regenerate it again~~~ 
		taskLoop = new TaskCore();		
		new Thread(taskLoop,name).start();		
	}
	
	public void finishTask(){
		if(taskLoop!=null){
			if(taskLoop.isDone()==false){
				taskLoop.cancel();
			}
		}
	}
	//---------------------------//
	
	private Timeline timMonitor = null;
	
	protected void timeLooper(){
	}
	
	protected void startTimeMonitor(long millisec){
		if(timMonitor!=null){
			timMonitor.stop();
		}
		timMonitor = new Timeline(new KeyFrame(
			Duration.millis(millisec),
			event->timeLooper()
		));
		timMonitor.setCycleCount(Timeline.INDEFINITE);	
		timMonitor.play();
	}
	
	protected void pauseTimeMonitor(){
		if(timMonitor==null){
			return;
		}
		timMonitor.pause();
	}
	//---------------------------//
}
