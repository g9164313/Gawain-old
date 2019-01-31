package prj.economy;

import java.util.ArrayList;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.sun.glass.ui.Application;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class FragSummary extends VBox {

	private final TableView<ItemBills> tab = new TableView<ItemBills>();
	
	@SuppressWarnings("unchecked")
	public FragSummary(){
		
		@SuppressWarnings("rawtypes")
		final TableColumn[] cols = {
			new TableColumn<ItemBills,String>("預約日期"),
			new TableColumn<ItemBills,String>("關帳日期"),
			new TableColumn<ItemBills,String>("訂單編號"),
			new TableColumn<ItemBills,String>("金額"),
			new TableColumn<ItemBills,String>("客戶名稱"),
			new TableColumn<ItemBills,String>("聯絡方式"),
			new TableColumn<ItemBills,String>("聯絡地址"),
		};
		
		cols[0].setCellValueFactory(new Callback<
			CellDataFeatures<ItemBills,String>, 
			ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				return new ReadOnlyObjectWrapper<String>(((ItemBills)param.getValue()).stampMeet);
			}
		});
		cols[1].setCellValueFactory(new Callback<
			CellDataFeatures<ItemBills,String>, 
			ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				return new ReadOnlyObjectWrapper<String>(((ItemBills)param.getValue()).stampClose);
			}
		});
		cols[2].setCellValueFactory(new Callback<
			CellDataFeatures<ItemBills,String>, 
			ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				return new ReadOnlyObjectWrapper<String>(((ItemBills)param.getValue()).order);
			}
		});		
		cols[3].setCellValueFactory(new Callback<CellDataFeatures<ItemBills,String>, ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				ItemBills itm = (ItemBills)param.getValue();
				return new ReadOnlyObjectWrapper<String>(itm.amountText());
			}
		});
		cols[4].setCellValueFactory(new Callback<
			CellDataFeatures<ItemBills,String>, 
			ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				return new ReadOnlyObjectWrapper<String>(((ItemBills)param.getValue()).name);
			}
		});
		cols[5].setCellValueFactory(new Callback<
			CellDataFeatures<ItemBills,String>, 
			ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				return new ReadOnlyObjectWrapper<String>(((ItemBills)param.getValue()).info);
			}
		});
		cols[6].setCellValueFactory(new Callback<
			CellDataFeatures<ItemBills,String>, 
			ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				return new ReadOnlyObjectWrapper<String>(((ItemBills)param.getValue()).addr);
			}
		});
		tab.getColumns().addAll(cols);
		
		final MenuItem itm1 = new MenuItem("退件");
		itm1.setOnAction(e->{
			tab.getSelectionModel().getSelectedItems().forEach(itm->{
				final String id = itm.takeSerial();
				itm.stampOpen();
				DataProvider.push_bills("/pool-1/"+id, itm);
				DataProvider.delete("/pool-3/"+id);
			});
		});
		final MenuItem itm2 = new MenuItem("刪除");
		itm2.setOnAction(e->{
			tab.getSelectionModel().getSelectedItems().forEach(itm->{
				DataProvider.delete("/pool-3/"+itm.takeSerial());
			});
		});
		tab.setContextMenu(new ContextMenu(itm1, itm2));
		
		VBox.setVgrow(tab, Priority.ALWAYS);
		
		getStyleClass().add("layout-medius");
		getChildren().add(tab);
	}
	
	public void eventShown(){
		DataProvider.refer("/pool-3", new ValueEventListener(){
			@Override
			public void onCancelled(DatabaseError arg0) {
				Application.invokeAndWait(()->tab.getItems().clear());
			}
			@Override
			public void onDataChange(DataSnapshot arg0) {
				ArrayList<ItemBills> lst = new ArrayList<ItemBills>();
				if(arg0.exists()==true){
					for(DataSnapshot ss:arg0.getChildren()){
						ItemBills itm = ss.getValue(ItemBills.class);
						itm.hangSerial(ss.getKey());//important!!!
						lst.add(itm);
					}
				}
				Application.invokeAndWait(()->tab.getItems().setAll(lst));
			}
		});
	}
}
