package prj.plan;

import java.util.Optional;

import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import narl.itrc.PanBase;

public class FragAuth extends HBox {

	private TableView<ItemHand> hand = new TableView<ItemHand>();
	
	private ListView<ItemBill> pool = new ListView<ItemBill>();
	
	public FragForm form;
	
	public FragAuth(FragForm form){
		
		this.form = form;
		init_hand_pool();
		init_data_pool();
		
		final Button btnAssign = PanBase.genButton2("指派", null);
		btnAssign.setMaxWidth(Double.MAX_VALUE);
		btnAssign.setOnAction(event->{
			final ObservableList<ItemBill> lst = form.getBill();
			if(lst.size()==0){
				return;
			}
			pool.getItems().addAll(lst);
			//TODO: synchronize database
			form.clearBill(lst);
		});
		VBox.setVgrow(hand,Priority.ALWAYS);
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("pad-space-small");
		lay1.getChildren().addAll(hand,btnAssign);
		
		final Button btnCancel = PanBase.genButton2("退回", null);
		btnCancel.setMaxWidth(Double.MAX_VALUE);
		btnCancel.setOnAction(event->{
			final ObservableList<ItemBill> lst = pool.getSelectionModel().getSelectedItems();
			if(lst.size()==0){
				return;
			}
			//TODO: synchronize database
			form.putBill(lst);
			pool.getItems().removeAll(lst);
		});
		
		final VBox lay2 = new VBox();
		lay2.getStyleClass().add("pad-space-small");
		lay2.getChildren().addAll(pool,btnCancel);

		setStyle("-fx-spacing: 0px,13px,0px,13px;");
		getChildren().addAll(lay1, lay2);
	}
	
	@SuppressWarnings("unchecked")
	private void init_hand_pool(){
		
		final TableColumn<ItemHand,String> col1 = new TableColumn<ItemHand,String>("姓名");
		col1.setCellValueFactory(new PropertyValueFactory<ItemHand,String>("name"));

		final TableColumn<ItemHand,String> col2 = new TableColumn<ItemHand,String>("地點");
		col2.setCellValueFactory(new PropertyValueFactory<ItemHand,String>("zone"));
		
		final TableColumn<ItemHand,Integer> col3 = new TableColumn<ItemHand,Integer>("負載");
		col3.setCellValueFactory(new PropertyValueFactory<ItemHand,Integer>("loading"));
		
		hand.getColumns().addAll(col1,col2,col3);
		hand.setEditable(false);
		hand.getSelectionModel().selectedItemProperty().addListener((obs,old,late)->{

		});
		
		final MenuItem itm1 = new MenuItem("新增");
		itm1.setOnAction(event->{
			//TODO: synchronize database
		});
		
		final MenuItem itm2 = new MenuItem("刪除");
		itm2.setOnAction(event->{			
			ItemHand obj = hand.getSelectionModel().getSelectedItem();
			if(obj==null){
				return;
			}
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle("確認");
			dia.setHeaderText(null);
			dia.setContentText("確認刪除人員：");
			Optional<ButtonType> res = dia.showAndWait();
			if(res.get()==ButtonType.OK){
				hand.getItems().remove(obj);
				//TODO: synchronize database
			}
		});
		
		hand.setContextMenu(new ContextMenu(itm1,itm2));
	}
	
	private void init_data_pool(){
		
	}
}
