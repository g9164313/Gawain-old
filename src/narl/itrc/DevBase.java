package narl.itrc;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

public abstract class DevBase extends Pane implements Gawain.EventHook {

	public DevBase(){
		Gawain.hook(this);
	}
	
	/**
	 * This property indicates whether device is connected or 
	 */
	protected BooleanProperty isAlive = new SimpleBooleanProperty(false);
	
	@Override
	public void shutdown() {
		eventShutdown();
	}
	
	public DevBase build(){
		return build(null);
	}
	
	public DevBase build(final String title){
		if(getChildren().isEmpty()==true){
			Node nd = eventLayout();
			if(nd!=null){
				if(title!=null){
					nd = PanBase.decorate(title,nd);
				}
				getChildren().add(nd);
			}else{
				Misc.logw("No control-view");
			}			
		}
		return this;
	}
	
	public PanBase showPanel(){
		return showPanel("");
	}
	
	public PanBase showPanel(final String title){
		new PanBase(title){
			@Override
			public Parent layout() {
				
				return null;
			}
		};
		return null;
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
