package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public abstract class DevBase extends Pane implements Gawain.EventHook {

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
			Node nd = eventLayout();
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
				return eventLayout(pan);
			}
		}.appear();
	}
		
	/**
	 * All devices need a view(console) to control its behaviors.<p>
	 * So, override this method to generate a control-view.
	 * @return
	 */
	protected Node eventLayout(){
		return null;
	}
	
	/**
	 * In this point, device can release its resource!!! <p>
	 */
	abstract void eventShutdown();
}
