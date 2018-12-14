package prj.economy;

import java.util.ArrayList;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.sun.glass.ui.Application;

import javafx.geometry.Pos;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import narl.itrc.Misc;

public class FragDispatch extends HBox {
	
	private static class FaceBills extends GridPane {
		
		private static Image img = Misc.getPicImage("sticker-emoji.png");
		
		public final ItemBills ref;
		
		public FaceBills(final ItemBills itm){		
			ref = itm;
			init_layout();
			setOnMouseClicked(event->{
				if(event.getClickCount()<2){
					return;
				}
				new PanEditBills(itm).appear();
			});
		}

		private void init_layout(){
			final Label[] info = {				
				new Label(ref.stampMeet),
				new Label(ref.addr),
				new Label(ref.info)
			};
			final ImageView icon = new ImageView(img);
			icon.setPickOnBounds(true);
			add(icon,0,0,3,3);			
			add(new Label("日期："), 3, 0);
			add(new Label("地點："), 3, 1);
			add(new Label("聯絡："), 3, 2);
			add(info[0], 4, 0);
			add(info[1], 4, 1);
			add(info[2], 4, 2);
		}
	}
	
	
	private ListView<FaceBills> lstBills = new ListView<>();
	
	private ListView<FaceHands> lstHands = new ListView<>();
	
	public FragDispatch(){

		/*final TableColumn<ItemBills, String> col_indx = new TableColumn<>("編號");
		col_indx.setCellValueFactory(new Callback<CellDataFeatures<ItemBills,String>,ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				ItemBills itm = param.getValue();
				return new SimpleStringProperty(itm.order_idx());
			}
		});
		final TableColumn<ItemBills, String> col_name = new TableColumn<>("客戶");
		col_name.setCellValueFactory(new Callback<CellDataFeatures<ItemBills,String>,ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				ItemBills itm = param.getValue();
				return new SimpleStringProperty(itm.name);
			}
		});
		
		final TableColumn<ItemBills, String> col_phone= new TableColumn<>("聯絡方式");
		col_phone.setCellValueFactory(new Callback<CellDataFeatures<ItemBills,String>,ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills, String> param) {
				ItemBills itm = param.getValue();
				return new SimpleStringProperty(itm.info);
			}
		});
		lstBills.setRowFactory(new Callback<TableView<ItemBills>,TableRow<ItemBills>>(){
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
		lstBills.getColumns().addAll(
			col_indx,
			col_name,
			col_phone
		);*/
		
		final Button btnForward = new Button();
		btnForward.setGraphic(Misc.getIconView("chevron-double-right.png"));
		btnForward.setOnAction(e->forward());
		
		final Button btnBackward = new Button();
		btnBackward.setGraphic(Misc.getIconView("chevron-double-left.png"));
		btnBackward.setOnAction(e->backward());
		
		VBox lay1 = new VBox(btnForward,btnBackward);
		lay1.getStyleClass().add("vbox-one-direction");
		
		lay1.setAlignment(Pos.CENTER);
		getChildren().addAll(
			lstBills,
			lay1,
			lstHands
		);		
	}
	
	/**
	 * push the selected bills into pool.<p>
	 */
	private void forward(){
		
		final FaceBills bills = lstBills.getSelectionModel().getSelectedItem();
		if(bills==null){
			return;
		}		
		final FaceHands hands = lstHands.getSelectionModel().getSelectedItem();
		if(hands==null){
			return;
		}
		final long b_id = bills.ref.takeSerial();
		final String h_id = hands.descr_idx();
				
		DataProvider.push_item(String.format(
			"/pool-2/%s/%d", 
			h_id, b_id
		), bills.ref);
		
		DataProvider.delete(String.format("/pool-1/%d", b_id));

		lstBills.getSelectionModel().clearSelection();
	}
	
	/**
	 * pop-up the last bills item from pool.<p>
	 */
	private void backward(){
		final FaceHands hands = lstHands.getSelectionModel().getSelectedItem();
		final String h_id = hands.descr_idx();
		final String org_path = "/pool-2/"+h_id;		
		DataProvider.refer_once_last(
			org_path, 
			1, 
			new ValueEventListener(){
			@Override
			public void onCancelled(DatabaseError arg0) {
			}
			@Override
			public void onDataChange(DataSnapshot arg0) {
				if(arg0.exists()==false){
					return;
				}
				arg0 = arg0.getChildren().iterator().next();	
				
				DataProvider.push_item(String.format(
					"/pool-1/%s", 
					arg0.getKey()
				), arg0.getValue(ItemBills.class));
		
				DataProvider.delete(org_path+"/"+arg0.getKey());
			}
		});
	}
	
	public void init(){
		
		DataProvider.refer("/worker", new ValueEventListener(){
			private ArrayList<FaceHands> lst = null;
			private Runnable event = new Runnable(){
				@Override
				public void run() {
					if(lst==null){
						lstHands.getItems().clear();
					}else{
						lstHands.getItems().setAll(lst);
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
				if(arg0.exists()==false){
					Application.invokeAndWait(event);
					return;
				}
				lst = new ArrayList<FaceHands>();
				for(DataSnapshot ss:arg0.getChildren()){
					ItemHands itm = ss.getValue(ItemHands.class);
					lst.add(new FaceHands(itm));
				}
				Application.invokeAndWait(event);
			}
		});
		
		DataProvider.refer("/pool-1", new ValueEventListener(){
			private ArrayList<FaceBills> lst = null;
			private Runnable event = new Runnable(){
				@Override
				public void run() {
					if(lst==null){
						lstBills.getItems().clear();
					}else{
						lstBills.getItems().setAll(lst);
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
				if(arg0.exists()==false){					
					Application.invokeAndWait(event);
					return;
				}
				lst = new ArrayList<FaceBills>();
				for(DataSnapshot ss:arg0.getChildren()){
					ItemBills itm = ss.getValue(ItemBills.class);
					itm.setSerial(Long.valueOf(ss.getKey()));
					lst.add(new FaceBills(itm));
				}
				Application.invokeAndWait(event);
			}	
		});
	}
}
