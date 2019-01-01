package prj.economy;

import java.util.ArrayList;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.jfoenix.controls.JFXBadge;
import com.sun.glass.ui.Application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
		
		private static Image img_bills = Misc.getPicImage("sticker-emoji.png");
		
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
			final ImageView icon = new ImageView(img_bills);
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
	
	public static class FaceHands extends GridPane {

		private static Image img_accnt = Misc.getPicImage("account.png");
		
		private final IntegerProperty loading = new SimpleIntegerProperty();

		private ItemHands ref;
		
		public FaceHands(final ItemHands itm){
			ref = itm;//make reference~~~
			init_layout();
			setOnMouseClicked(event->{
				if(event.getClickCount()<2){
					return;
				}
				new PanEditHands(itm).appear();
			});
			DataProvider.refer("/pool-2/"+ref.info, new ValueEventListener(){
				int val = 0;
				Runnable event = new Runnable(){
					@Override
					public void run() {
						loading.set(val);
					}
				};
				@Override
				public void onCancelled(DatabaseError arg0) {	
				}
				@Override
				public void onDataChange(DataSnapshot arg0) {
					if(arg0.exists()==false){
						val = 0;
					}else{
						val = (int)arg0.getChildrenCount();
					}
					//trick!! Property doesn't be changed when node is showing...
					Application.invokeLater(event);
				}
			});
		}

		private void init_layout(){
			
			final Label[] info = {
				new Label(ref.name),
				new Label(ref.info),
				new Label(ref.zone)
			};
			
			final ImageView icon = new ImageView(img_accnt);
			final JFXBadge badge = new JFXBadge(icon,Pos.TOP_LEFT);
			badge.getStyleClass().add("icons-badge");
			badge.textProperty().bind(loading.asString());
			
			add(badge,0,0,3,3);
			add(new Label("人員名稱："), 3, 0);
			add(new Label("聯絡電話："), 3, 1);
			add(new Label("負責區域："), 3, 2);
			add(info[0], 4, 0);
			add(info[1], 4, 1);
			add(info[2], 4, 2);
		}
	};
	
	private ListView<FaceBills> lstBills = new ListView<>();
	
	private ListView<FaceHands> lstHands = new ListView<>();
	
	public FragDispatch(){

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
		final String b_id = bills.ref.takeSerial();
		final String h_id = hands.ref.info;
				
		DataProvider.push_bills(String.format(
			"/pool-2/%s/%s", 
			h_id, b_id
		), bills.ref);
		
		DataProvider.delete(String.format("/pool-1/%s", b_id));

		lstBills.getSelectionModel().clearSelection();
	}
	
	/**
	 * pop-up the last bills item from pool.<p>
	 */
	private void backward(){
		final FaceHands hands = lstHands.getSelectionModel().getSelectedItem();
		final String org_path = "/pool-2/"+hands.ref.info;		
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
				
				DataProvider.push_bills(String.format(
					"/pool-1/%s", 
					arg0.getKey()
				), arg0.getValue(ItemBills.class));
		
				DataProvider.delete(org_path+"/"+arg0.getKey());
			}
		});
	}
	
	public void init(){
		
		DataProvider.refer("/hands", new ValueEventListener(){
			ArrayList<FaceHands> lst = null;
			Runnable event = new Runnable(){
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
				if(arg0.exists()==true){
					lst = new ArrayList<FaceHands>();
					for(DataSnapshot ss:arg0.getChildren()){
						ItemHands itm = ss.getValue(ItemHands.class);
						lst.add(new FaceHands(itm));
					}
				}
				Application.invokeAndWait(event);
			}
		});
		
		DataProvider.refer("/pool-1", new ValueEventListener(){
			ArrayList<FaceBills> lst = null;
			Runnable event = new Runnable(){
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
				if(arg0.exists()==true){					
					lst = new ArrayList<FaceBills>();
					for(DataSnapshot ss:arg0.getChildren()){
						ItemBills itm = ss.getValue(ItemBills.class);
						itm.hangSerial(ss.getKey());
						lst.add(new FaceBills(itm));
					}
				}
				Application.invokeAndWait(event);
			}	
		});
	}
}
