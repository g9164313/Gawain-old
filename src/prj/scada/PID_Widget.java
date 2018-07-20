package prj.scada;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import narl.itrc.Misc;

public class PID_Widget extends AnchorPane {
	
	private PID_Node nd;
	
	private int[] loca = {0, 0};
	
	private Cursor old_cursor = null;
	
	public PID_Widget(){
		this("");
		this.setStyle("-fx-padding: 7px");
		
		nd = new PID_Node(0);
				
		this.getChildren().add(nd);
				
		AnchorPane.setLeftAnchor(nd, (double)loca[0]);
		AnchorPane.setTopAnchor (nd, (double)loca[1]);
	}
	
	public PID_Widget(String name){
		
		setOnMouseMoved(event_move);
		
		setOnMouseEntered(event->{
			Scene sc = getScene();
			old_cursor = sc.getCursor();
			sc.setCursor(Cursor.HAND);
		});
		setOnMouseExited(event->{
			getScene().setCursor(old_cursor);;
		});
		
		if(name.length()==0){
			return;
		}		
		//this.setOnMousePressed();
		//this.setOnMouseClicked();
	}
	
	private EventHandler<MouseEvent> event_move = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			int mx = (int)event.getX();
			int my = (int)event.getY();
			if(
				Math.abs(mx-loca[0])>32 ||
				Math.abs(my-loca[1])>32
			){
				loca[0] = mx;
				loca[1] = my;
				AnchorPane.setLeftAnchor(nd, (double)loca[0]);
				AnchorPane.setTopAnchor (nd, (double)loca[1]);
			}			
		}
	};

}
