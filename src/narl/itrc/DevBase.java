package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public abstract class DevBase extends Pane implements Gawain.GlobalHook {

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
	 * Invoke a event to close device or release resources.<p>
	 * This will be invoked at the end of program.<p>
	 */
	@Override
	public void shutdown() {
		stopMonitor();
		eventShutdown();
	}
	
	/**
	 * Request the layout of console panel.<p>
	 * @return self instance
	 */
	public DevBase build(){
		return build("");
	}
	
	/**
	 * Request the layout of console panel with boards.<p>
	 * @param title - the title of panel, or null (it will not have board)
	 * @return self instance
	 */
	public DevBase build(final String title){
		if(getChildren().isEmpty()==true){
			Node nd = eventLayout(null);
			getChildren().add(nd);
			getStyleClass().add("decorate1-border");
			//setMaxWidth(Double.MAX_VALUE);
			/*if(nd!=null){
				if(title!=null){
					nd = PanDecorate.group(title,nd);
				}				
			}else{
				Misc.logw("No control-view");
			}*/			
		}
		return this;
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
		}.appear();
	}
		
	/**
	 * All devices need a view(console) to control its behaviors.<p>
	 * So, override this method to generate a control-view.
	 * @return
	 */
	protected Node eventLayout(PanBase pan){
		return null;
	}
	
	/**
	 * In this point, device can release its resource!!! <p>
	 */
	abstract void eventShutdown();	
	//---------------------------//
	
	private final long DEFAULT_DELAY = 250L;
	
	private long taskDelay = DEFAULT_DELAY;//millisecond
	
	private class TaskMonitor extends Task<Long>{
		@Override
		protected Long call() throws Exception {
			if(taskStart()==false){
				return -1L;
			}
			long tk1, tk2;
			while(isCancelled()==false){
				tk1 = System.currentTimeMillis();
				boolean goon = taskLooper();				
				tk2 = System.currentTimeMillis();
				updateValue(tk2-tk1);
				if(goon==false){
					break;
				}
				//delay, for next turn~~~~
				tk1 = System.currentTimeMillis();
				tk2 = 0L;
				while(tk2<taskDelay){
					tk2 = System.currentTimeMillis() - tk1;
					updateProgress(tk2,taskDelay);
				}
			}
			taskFinal();
			return -2L;
		}
	}
	
	protected boolean taskStart(){
		return false;
	}
	
	protected boolean taskLooper(){
		return false;
	}
	
	protected void taskFinal(){
	}
	
	private Task<Long> tskMonitor;
	
	protected void startMonitor(String name){
		startMonitor(name,DEFAULT_DELAY);
	}
	
	protected void startMonitor(String name,long delay){		
		if(tskMonitor!=null){
			if(tskMonitor.isDone()==false){
				return;
			}
		}
		taskDelay = delay;
		tskMonitor = new TaskMonitor();
		new Thread(tskMonitor,name).start();
	}
	
	protected void stopMonitor(){
		if(tskMonitor!=null){
			if(tskMonitor.isDone()==false){
				tskMonitor.cancel();
			}
		}
	}
	//---------------------------//
	
}
