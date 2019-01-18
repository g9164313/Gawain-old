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
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import narl.itrc.Misc;
import prj.economy.Calendar.DayInfo;

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
	};	
	
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
			DataProvider.refer("/pool-2/"+ref.takeUID(), new ValueEventListener(){
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
			add(new Label("名稱："), 3, 0);
			add(new Label("電話："), 3, 1);
			add(new Label("區域："), 3, 2);
			add(info[0], 4, 0);
			add(info[1], 4, 1);
			add(info[2], 4, 2);
		}
	};
	
	private final Calendar calendar = new Calendar();
	
	private final ListView<FaceHands> lstHands = new ListView<>();
	
	private final ListView<FaceBills> lstBills = new ListView<>();
	
	public FragDispatch(){

		calendar.eventPickup = new Calendar.DayHook() {			
			@Override
			public void callback(DayInfo info) {
				if(hands==null){
					return;
				}
				for(FaceHands h:hands){
					h.setDisable(false);
				}
				if(info==null){
					//user cancel pick-up day
					return;
				}
				String day = info2day(info);
				for(FaceHands h:hands){
					if(h.ref.holiday.contains(day)==true){
						h.setDisable(true);
					}
				}
			}
		};
		calendar.eventUpdate = new Calendar.DayHook() {
			@Override
			public void callback(DayInfo info) {
				if(hands==null){
					info.memo = "?";
					return;
				}
				int cnt = hands.size();
				String day = info2day(info);
				for(FaceHands h:hands){
					if(h.ref.holiday.contains(day)==true){
						cnt--;
					}
				}
				info.memo = String.format("<%d",cnt);
			}
		};
		
		getStyleClass().add("layout-medius");
		getChildren().addAll(
			calendar,
			lstHands,
			lstBills
		);
	}
	
	private String info2day(DayInfo info){
		return String.format(
			"%d-%02d-%02d",
			info.year,
			info.month,
			info.day
		);
	}
	
	private ArrayList<FaceHands> hands = null;
	 
	private Runnable eventUpdateHands = new Runnable(){
		@Override
		public void run() {
			if(hands==null){
				lstHands.getItems().clear();
			}else{
				lstHands.getItems().setAll(hands);
			}
			calendar.update();
		}
	};
	
	public void eventShow(){
		
		DataProvider.refer("/hands", new ValueEventListener(){
			@Override
			public void onCancelled(DatabaseError arg0) {
				hands = null;
				Application.invokeAndWait(eventUpdateHands);
			}
			@Override
			public void onDataChange(DataSnapshot arg0) {
				hands = null;
				if(arg0.exists()==true){
					hands = new ArrayList<FaceHands>();
					for(DataSnapshot ss:arg0.getChildren()){
						ItemHands itm = ss.getValue(ItemHands.class);
						itm.fillUID(ss.getKey());
						hands.add(new FaceHands(itm));
					}
				}
				Application.invokeAndWait(eventUpdateHands);
			}
		});
	}
}
