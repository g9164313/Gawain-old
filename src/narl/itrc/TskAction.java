package narl.itrc;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Control;

public abstract class TskAction extends TskBase 
	implements EventHandler<ActionEvent> 
{
	protected String title = "TskAction";
	
	protected PanBase parent = null;
	
	public TskAction(){		
	}
	
	public TskAction(PanBase root){
		parent = root;		
	}
	
	public TskAction(Control ctrl){
		Object obj = ctrl.getScene().getUserData();
		if(obj==null){
			return;
		}else if(obj instanceof PanBase){
			parent = (PanBase)obj;			
		}		
	}
		
	protected void spinning(boolean flag){
		if(parent!=null){
			parent.spinning(flag);
		}
	} 
	
	@Override
	protected void eventFinish(){
		spinning(false);
	}

	@Override
	public void handle(ActionEvent event) {
		if(eventBegin()==false){
			return;
		}
		if(parent!=null){
			parent.spinning(true,this);
		}
		start(title);
	}
}
