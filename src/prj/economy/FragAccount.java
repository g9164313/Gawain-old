package prj.economy;

import java.util.ArrayList;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;

public class FragAccount extends BorderPane {
	
	private TableView<ItemBills> tab = new TableView<>();
	
	@SuppressWarnings("unchecked")
	public FragAccount(){
		
		final TableColumn<ItemBills, String> col_order = new TableColumn<>("編號");
		col_order.setCellValueFactory(new Callback<CellDataFeatures<ItemBills,String>,ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				ItemBills itm = param.getValue();
				return new SimpleStringProperty(itm.order);
			}
		});
		
		final TableColumn<ItemBills, String> col_close = new TableColumn<>("結帳");
		col_close.setCellValueFactory(new Callback<CellDataFeatures<ItemBills,String>,ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				ItemBills itm = param.getValue();
				return new SimpleStringProperty(itm.stampClose);
			}
		});
		
		final TableColumn<ItemBills, String> col_amount= new TableColumn<>("金額");
		col_amount.setCellValueFactory(new Callback<CellDataFeatures<ItemBills,String>,ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				ItemBills itm = param.getValue();
				return new SimpleStringProperty(itm.amountText());
			}
		});
		
		tab.setRowFactory(new Callback<TableView<ItemBills>,TableRow<ItemBills>>(){
			@Override
			public TableRow<ItemBills> call(TableView<ItemBills> param) {
				TableRow<ItemBills> row = new TableRow<>();
				row.setOnMouseClicked(event->{
					if(event.getClickCount()==2 && row.isEmpty()==false){
						new PanEditBills(row.getItem()).appear();
					}
				});
				return row;
			}
		});
		
		tab.getColumns().addAll(
			col_order,
			col_close,
			col_amount
		);
		
		setCenter(tab);
	}
	
	public void init(){
		DataProvider.refer("/pool-3", new ValueEventListener(){
			ArrayList<ItemBills> lst = null;
			Runnable event = new Runnable(){
				@Override
				public void run() {
					if(lst==null){						
						tab.getItems().clear();
					}else{
						tab.getItems().setAll(lst);
					}
				}
			};
			@Override
			public void onCancelled(DatabaseError arg0) {
				lst = null;
				Application.invokeAndWait(event);
			}
			@Override
			public void onDataChange(DataSnapshot arg0) {
				lst = null;
				if(arg0.exists()==true){
					lst = new ArrayList<ItemBills>();
					for(DataSnapshot ss:arg0.getChildren()){
						ItemBills itm = ss.getValue(ItemBills.class);
						itm.hangSerial(ss.getKey());						
						lst.add(itm);
					}
				}
				Application.invokeAndWait(event);
			}
		});
	};
}
