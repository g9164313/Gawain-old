package narl.itrc;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.layout.StackPane;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;

abstract class PinSelect extends StackPane {
	
	private final int MIN_SIZE = 24;
	private final int MAX_SIZE = 124;
	
	public JFXListView<String> lstItm = new JFXListView<String>();
	public JFXButton btnPin = new JFXButton();
	
	public PinSelect(String... itm){		
				
		lstItm.getItems().addAll(itm);
		lstItm.setVisible(false);
		
		btnPin.setPrefSize(MIN_SIZE,MIN_SIZE);
		btnPin.setOnAction(eventPin);
		
		this.setMinSize(MIN_SIZE, MIN_SIZE);
		this.setMaxSize(MAX_SIZE, MAX_SIZE);
		this.getChildren().addAll(btnPin,lstItm);
		//getStyleClass().add("btn-flat-tick");		
	}
	
	private EventHandler<ActionEvent> eventPin = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			boolean flag = lstItm.isVisible();
			lstItm.setVisible(!flag);
		}
	};
}
