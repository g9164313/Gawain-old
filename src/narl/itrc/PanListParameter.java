package narl.itrc;

import com.jfoenix.controls.JFXButton;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public abstract class PanListParameter extends VBox {

	public TableView<PropBundle> lst = new TableView<PropBundle>();
	
	private HBox panCommand = new HBox();
	
	public PanListParameter(){
		init_layout();
	}
	
	public PanListParameter(ObservableList<PropBundle> data){
		init_layout();
		lst.setItems(data);
	}
	
	private void init_layout(){
		getStyleClass().add("vbox-small");

		TableColumn<PropBundle,String> col;

		col = new TableColumn<PropBundle,String>("參數");
		col.setSortable(false);
		col.setCellValueFactory(new PropertyValueFactory<PropBundle,String>("name"));		
		lst.getColumns().add(col);
		
		col = new TableColumn<PropBundle,String>("數值");
		col.setSortable(false);
		col.setEditable(true);
		col.setCellValueFactory(new PropertyValueFactory<PropBundle,String>("value"));
		col.setCellFactory(TextFieldTableCell.forTableColumn());
		col.setOnEditCommit(new EventHandler<CellEditEvent<PropBundle,String>>(){
			@Override
			public void handle(CellEditEvent<PropBundle, String> event) {
				updateData(event);
				lst.refresh();
			}
		});		
		lst.getColumns().add(col);
		
		col = new TableColumn<PropBundle,String>("描述");
		col.setSortable(false);
		col.setCellValueFactory(new PropertyValueFactory<PropBundle,String>("desc"));
		lst.getColumns().add(col);

		lst.setEditable(true);
		
		panCommand.getStyleClass().add("vbox-small");
		
		getChildren().addAll(lst,panCommand);
	}
	
	@SuppressWarnings("unchecked")
	protected void addButton(Object... titleEvent){
		int total = titleEvent.length/2;
		if(titleEvent.length%2!=0){
			return;
		}
		for(int i=0; i<titleEvent.length; i+=2){
			JFXButton btn = new JFXButton((String)titleEvent[i]);
			btn.setOnAction((EventHandler<ActionEvent>)titleEvent[i+1]);
			btn.getStyleClass().add("button-raised");
			btn.prefWidthProperty().bind(lst.widthProperty().divide(total));
			panCommand.getChildren().add(btn);
		}
	}
	
	public abstract void refreshData();
	
	public abstract void updateData(CellEditEvent<PropBundle, String> event);
}
