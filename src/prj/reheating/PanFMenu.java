package prj.reheating;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import com.jfoenix.controls.JFXButton;

class PanFMenu extends HBox implements EventHandler<ActionEvent> {
	
	public interface Showing {
		public void callback();
	}
	
	private BorderPane root = null;
	private Node[] frame = null;
	private Showing[] event = null;
	
	PanFMenu(){
		getStyleClass().add("vbox-small");
	}
	
	public void setEachPane(BorderPane parent,Object... TitleFrameEvent){		
		final int COLS=3;//title, node, event
		int size = TitleFrameEvent.length/COLS;
		root = parent;
		frame= new Node[size];
		event= new Showing[size];
		//event= new EventHandler<ActionEvent>[size];
		JFXButton btn;
		for(int i=0; i<TitleFrameEvent.length; i+=COLS){
			int idx = i/COLS;
			btn = new JFXButton((String)TitleFrameEvent[i]);
			btn.setUserData(idx);//page index
			btn.setOnAction(this);			
			getChildren().add(btn);
			frame[idx] = (Node)TitleFrameEvent[i+1];
			event[idx] = (Showing)TitleFrameEvent[i+2];
		}
		hideAll(0);
	}
	
	@Override
	public void handle(ActionEvent e) {
		JFXButton btn = (JFXButton)e.getSource();
		int i = (int)btn.getUserData();
		if(event[i]!=null){
			event[i].callback();
		}
		hideAll(i);
	}
	
	private void hideAll(int except){
		if(frame==null || root==null){
			return;
		}
		for(int i=0; i<frame.length; i++){
			if(frame[i]==null){
				continue; 
			}
			if(except==i){
				frame[i].setVisible(true);
				root.setCenter(frame[i]);
			}else{
				frame[i].setVisible(false);
			}
		}
	}
}

