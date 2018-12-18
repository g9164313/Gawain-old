package narl.itrc;

import com.jfoenix.controls.JFXCheckBox;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.Duration;
import narl.itrc.Gawain.LogMessage;

public class PanLogger extends PanBase {

	private TableView<LogMessage> table = new TableView<>();		
	
	private Timeline watcher = new Timeline(new KeyFrame(Duration.millis(350),event->{		
		LogMessage msg = Gawain.logQueue.poll();
		if(msg==null){
			return;
		}
		if(table.getItems().size()>=200){
			table.getItems().remove(0, 10);
		}
		table.getItems().add(msg);
		table.scrollTo(table.getItems().size()-1);
	}));
	
	public PanLogger(){
		watcher.setCycleCount(Timeline.INDEFINITE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Node eventLayout(PanBase self) {

		TableColumn<LogMessage,Long> col_1 = new TableColumn<>("時間");
		col_1.setPrefWidth(100);
		col_1.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LogMessage,Long>,ObservableValue<Long>>(){
			@Override
			public ObservableValue<Long> call(CellDataFeatures<LogMessage, Long> param) {				
				return new ReadOnlyLongWrapper(param.getValue().tick).asObject();
			}
		});
		col_1.setCellFactory(new Callback< TableColumn<LogMessage,Long>, TableCell<LogMessage,Long> >(){
			@Override
			public TableCell<LogMessage, Long> call(TableColumn<LogMessage, Long> param) {
				return new TableCell<LogMessage, Long>(){
					@Override
					protected void updateItem(Long val, boolean empty) {
						if(empty==false){
							setText(String.format(
								"%tH:%tM:%tS.%tL",
								val,val,val,val
							));
						}else{
							setText(null);
						}
					}
				};
			}
		});
		
		TableColumn<LogMessage,Integer> col_2 = new TableColumn<>("層級");
		col_2.setPrefWidth(60);
		col_2.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LogMessage,Integer>,ObservableValue<Integer>>(){
			@Override
			public ObservableValue<Integer> call(CellDataFeatures<LogMessage, Integer> param) {				
				return new ReadOnlyIntegerWrapper(param.getValue().type).asObject();
			}
		});
		col_2.setCellFactory(new Callback<
			TableColumn<LogMessage,Integer>,
			TableCell<LogMessage,Integer>
		>(){
			@Override
			public TableCell<LogMessage, Integer> call(TableColumn<LogMessage, Integer> param) {
				return new TableCell<LogMessage, Integer>(){
					@Override
					protected void updateItem(Integer val, boolean empty) {
						if(empty==false){
							switch(val.intValue()){
							case 21: setText("info "); break;
							case 22: setText("warn "); break;
							case 23: setText("error"); break;
							}							
						}else{
							setText(null);
						}
					}
				};
			}
		});
		
		TableColumn<Gawain.LogMessage,String> col_3 = new TableColumn<>("訊息"); 
		col_3.prefWidthProperty().bind(table.widthProperty()
			.subtract(col_1.widthProperty())
			.subtract(col_2.widthProperty())
			.subtract(7)
		);
		col_3.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<LogMessage,String>,ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<LogMessage, String> param) {				
				return new ReadOnlyStringWrapper(param.getValue().text);
			}
		});
		col_3.setCellFactory(new Callback<
			TableColumn<LogMessage,String>,
			TableCell<LogMessage,String>
		>(){
			@Override
			public TableCell<LogMessage, String> call(TableColumn<LogMessage, String> param) {
				return new TableCell<LogMessage, String>(){
					@Override
					protected void updateItem(String val, boolean empty) {
						if(empty==false){
							setText(val);
						}else{
							setText(null);
						}
					}
				};
			}
		});
		
		table.setPrefWidth(600);
		table.setPrefHeight(250);
		table.setEditable(false);
		table.getColumns().addAll(col_1,col_2,col_3);
		
		JFXCheckBox chkWatcher = new JFXCheckBox("自動更新");
		chkWatcher.setSelected(true);
		chkWatcher.setOnAction(e->{
			if(chkWatcher.isSelected()==true){
				watcher.play();
			}else{
				watcher.pause();
			}
		});
		
		VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");		
		lay1.getChildren().addAll(chkWatcher);
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		//lay0.setLeft(layAdvn);
		lay0.setCenter(table);
		lay0.setRight(lay1);
		return lay0;
	}

	@Override
	public void eventShown(Object[] args) {
		while(Gawain.logQueue.peek()!=null){
			table.getItems().add(Gawain.logQueue.poll());
		}		
		watcher.play();
	}
	
	@Override
	protected void eventClose(PanBase self) {	
		watcher.stop();
	}
}
