package narl.itrc;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Control;

public abstract class TskAction extends TskBase 
	implements EventHandler<ActionEvent> 
{
	protected String title = "TskAction";
	
	private PanBase pan = null;
	
	public TskAction(){		
	}
	
	public TskAction(PanBase root){
		pan = root;		
	}
	
	public TskAction(Control ctrl){
		Object obj = ctrl.getScene().getUserData();
		if(obj==null){
			return;
		}else if(obj instanceof PanBase){
			pan = (PanBase)obj;			
		}		
	}
	
	@Override
	public void handle(ActionEvent event) {
		if(eventInit()==false){
			return;
		}
		if(pan!=null){
			pan.spinning(true,this);
		}
		start(title);
	}
}
