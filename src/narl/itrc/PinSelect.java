package narl.itrc;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.SelectionMode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;

abstract class PinSelect extends StackPane {
	
	private final int MIN_SIZE = 24;
	private final int MAX_SIZE = 124;
	
	public JFXListView<String> lstItm = new JFXListView<String>();
	public JFXButton btnPin = new JFXButton();
	
	public PinSelect(String... itm){		
		
		lstItm.setVisible(false);
		lstItm.getItems().addAll(itm);
		lstItm.getSelectionModel().select(0);		
		lstItm.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		lstItm.setOnMouseClicked(eventItm);

		btnPin.setGraphic(Misc.getIcon("thumb-circle.png"));
		btnPin.setOnAction(eventPin);
		btnPin.setPrefSize(MIN_SIZE,MIN_SIZE);	
		
		setAlignment(Pos.CENTER);
		setPrefSize(MIN_SIZE,MIN_SIZE);
		getChildren().addAll(btnPin,lstItm);				
	}

	private EventHandler<ActionEvent> eventPin = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			lstItm.setVisible(!lstItm.isVisible());
			PinSelect.this.setPrefSize(MAX_SIZE,MAX_SIZE);//how to calculate size???
		}
	};
	
	private EventHandler<MouseEvent> eventItm = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			int idx = lstItm.getSelectionModel().getSelectedIndex();
			String name = lstItm.getSelectionModel().getSelectedItem();
			eventSelect(idx,name);
			lstItm.setVisible(!lstItm.isVisible());
			PinSelect.this.setPrefSize(MIN_SIZE,MIN_SIZE);
		}
	};
	
	abstract void eventSelect(int idx,String name);
}
