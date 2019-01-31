package prj.economy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.sun.glass.ui.Application;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import narl.itrc.Misc;
import prj.economy.CalendarEx.DayInfo;

public class FragDispatch extends HBox {
	
	private final static Image img_bill1 = Misc.getPicImage("file-1.png");
	private final static Image img_bill2 = Misc.getPicImage("file-2.png");
	private final static Image img_accnt = Misc.getPicImage("account.png");
	
	private class FaceBills extends GridPane {
		public final ItemBills ref;
		
		public FaceBills(final ItemBills itm,int type){		
			ref = itm;
			init_layout(type);
		}
		private void init_layout(int type){
			final Label[] info = {				
				new Label(ref.stampMeet),
				new Label(ref.info),
				new Label(ref.addr)				
			};
			ImageView img;
			switch(type){			
			case 1:
				img = new ImageView(img_bill1);
				break;
			default:
			case 2:
				img = new ImageView(img_bill2);
				break;
			}
			add(img,0,0,3,3);
			add(new Label("日期："),3,0);
			add(new Label("聯絡："),3,1);
			add(new Label("地點："),3,2); 
			add(info[0],4,0);
			add(info[1],4,1);
			add(info[2],4,2);
		}
	};	
	
	public class FaceHands extends GridPane {
		private ItemHands ref;

		private ArrayList<FaceBills> bills = new ArrayList<FaceBills>();
		
		public FaceHands(final ItemHands itm){
			ref = itm;//make reference~~~
			init_layout();
			DataProvider.refer("/pool-2/"+ref.takeUID(),new ValueEventListener(){
				@Override
				public void onCancelled(DatabaseError arg0) {
					bills.clear();
					Application.invokeAndWait(()->lstBills.getItems().setAll(bills));
				}
				@Override
				public void onDataChange(DataSnapshot arg0) {
					bills.clear();
					if(arg0.exists()==true){
						for(DataSnapshot ss:arg0.getChildren()){
							ItemBills itm = ss.getValue(ItemBills.class);
							itm.hangSerial(ss.getKey());//important!!!
							bills.add(new FaceBills(itm,2));
						}
					}
					Application.invokeAndWait(()->lstBills.getItems().setAll(bills));
				}
			});
			setOnMouseClicked(event->{
				lstBills.getItems().clear();
				if(calendar.isVisible()==true){					
					String day = info2day(calendar.getDayPick());
					for(FaceBills bill:bills){
						if(day.length()==0 || bill.ref.stampMeet.contains(day)==true){
							lstBills.getItems().add(bill);
						}
					}
				}else{
					lstBills.getItems().setAll(bills);
				}
			});
		}
		private void init_layout(){
			final Label[] info = {
				new Label(ref.name),
				new Label(ref.info),
				new Label(ref.zone)
			};
			add(new ImageView(img_accnt),0,0,3,3);
			add(new Label("名稱："), 3, 0);
			add(new Label("電話："), 3, 1);
			add(new Label("區域："), 3, 2);
			add(info[0], 4, 0);
			add(info[1], 4, 1);
			add(info[2], 4, 2);
		}
	};
	
	private final CalendarEx calendar = new CalendarEx();
	
	private final ListView<FaceHands> lstHands = new ListView<>();	
	private final ListView<FaceBills> lstBills = new ListView<>();
	private final ListView<FaceBills> lstWaits = new ListView<>();	
	private HBox panWaits = new HBox();
	
	public FragDispatch(){
	
		init_calendar();
		init_pan_wait();
		init_lstHands();
		init_lstBills();

		calendar.setVisible(true);
		panWaits.setVisible(false);
				
		getStyleClass().add("layout-medius");
		getChildren().addAll(
			new StackPane(panWaits, calendar),
			lstHands,
			lstBills
		);
	}
	
	private void init_pan_wait(){
		
		final MenuItem itm1 = new MenuItem("新增");
		itm1.setOnAction(e->dialog_bill(null,null));
		
		final MenuItem itm2 = new MenuItem("刪除");
		itm2.setOnAction(e->{
			FaceBills itm = lstWaits.getSelectionModel().getSelectedItem();
			DataProvider.delete("/pool-1/"+itm.ref.takeSerial());
		});
		
		final MenuItem itm3 = new MenuItem("修改");
		itm3.setOnAction(e->{
			FaceBills itm = lstWaits.getSelectionModel().getSelectedItem();
			dialog_bill(null,itm);
		});
		
		lstWaits.setContextMenu(new ContextMenu(itm1,itm2,itm3));
		
		final Button btn1 = new Button(">>");
		btn1.setMaxWidth(Double.MAX_VALUE);
		btn1.setMinHeight(48);
		btn1.setOnAction(e->{
			final FaceHands hand = lstHands.getSelectionModel().getSelectedItem();
			if(hand==null){
				return;
			}
			lstWaits.getSelectionModel().getSelectedItems().forEach(bill->{
				final String src = "/pool-1/"+bill.ref.takeSerial();
				final String dst = "/pool-2/"+hand.ref.takeUID()+"/"+bill.ref.takeSerial();				
				DataProvider.push_bills(dst, bill.ref);
				DataProvider.delete(src);
			});
		});
		
		final Button btn2 = new Button("<<");
		btn2.setMaxWidth(Double.MAX_VALUE);
		btn2.setMinHeight(48);
		btn2.setOnAction(e->{
			final FaceHands hand = lstHands.getSelectionModel().getSelectedItem();
			if(hand==null){
				return;
			}
			lstBills.getSelectionModel().getSelectedItems().forEach(bill->{
				final String src = "/pool-2/"+hand.ref.takeUID()+"/"+bill.ref.takeSerial();				
				final String dst = "/pool-1/"+bill.ref.takeSerial();
				bill.ref.stampOpen();
				DataProvider.push_bills(dst, bill.ref);
				DataProvider.delete(src);
				lstBills.getItems().remove(bill);
			});
		});
		
		final VBox lay = new VBox(13);
		lay.getChildren().addAll(btn1,btn2);
		lay.setAlignment(Pos.CENTER);
		HBox.setHgrow(lay, Priority.SOMETIMES);
		panWaits.getChildren().addAll(lstWaits,lay);
	}
	
	private void init_calendar(){
		calendar.eventPickup = new CalendarEx.DayHook() {			
			@Override
			public void callback(DayInfo info) {				
				//clear all items form bills
				lstBills.getItems().clear();
				//check the pick-up day
				String day = info2day(info);
				for(FaceHands hand:lstHands.getItems()){
					if(day.length()!=0 && hand.ref.holiday.contains(day)==true){
						hand.setDisable(true);
					}else{
						hand.setDisable(false);
					}
				}
				//show this hand hold bills in this pick-up day
				FaceHands hand = lstHands.getSelectionModel().getSelectedItem();
				if(hand==null){
					return;
				}
				hand.getOnMouseClicked().handle(null);
			}
		};
		calendar.eventUpdate = new CalendarEx.DayHook() {
			@Override
			public void callback(DayInfo info) {
				int cnt = lstHands.getItems().size();
				String day = info2day(info);
				for(FaceHands hand:lstHands.getItems()){
					if(hand.ref.holiday.contains(day)==true){
						cnt--;
					}
				}
				info.memo = String.format("%d員",cnt);
			}
		};
	}
	
	private void init_lstHands(){
		
		final MenuItem itm1 = new MenuItem("新增");
		itm1.setOnAction(e->dialog_hand(null));
		
		final MenuItem itm2 = new MenuItem("刪除");
		itm2.setOnAction(e->{
			FaceHands hand = lstHands.getSelectionModel().getSelectedItem();
			if(hand==null){
				return;
			}
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle("刪除");
			dia.setHeaderText("刪除工作人員？");
			if(dia.showAndWait().get()==ButtonType.OK){
				//TODO:move old bills to pool-1.				
				DataProvider.delete("/hands/"+hand.ref.takeUID());
			}
		});
		
		final MenuItem itm3 = new MenuItem("修改");
		itm3.setOnAction(e->{
			FaceHands hand = lstHands.getSelectionModel().getSelectedItem();
			if(hand==null){
				return;
			}
			dialog_hand(hand);
		});
		
		lstHands.setContextMenu(new ContextMenu(itm1, itm2, itm3));
	}
	
	private void init_lstBills(){
		
		final MenuItem itm1 = new MenuItem("新增");
		itm1.setOnAction(e->{
			FaceHands hand = lstHands.getSelectionModel().getSelectedItem();
			if(hand==null){
				return;
			}
			String day = info2day(calendar.getDayPick());
			dialog_bill(hand,null,day);
		});
		
		final MenuItem itm2 = new MenuItem("刪除");
		itm2.setOnAction(e->{
			FaceBills bill = lstBills.getSelectionModel().getSelectedItem();
			if(bill==null){
				return;
			}
			FaceHands hand = lstHands.getSelectionModel().getSelectedItem();
			if(hand==null){
				return;
			}
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle("刪除");
			dia.setHeaderText("確認刪除訂單？");
			if(dia.showAndWait().get()==ButtonType.OK){				
				DataProvider.delete("/pool-2/"+hand.ref.takeUID()+"/"+bill.ref.takeSerial());
			}
		});
		
		final MenuItem itm3 = new MenuItem("修改");
		itm3.setOnAction(e->{
			FaceHands hand = lstHands.getSelectionModel().getSelectedItem();
			if(hand==null){
				return;
			}
			FaceBills bill = lstBills.getSelectionModel().getSelectedItem();
			if(bill==null){
				return;
			}
			dialog_bill(hand,bill);
		});
		
		final MenuItem itm4 = new MenuItem("關帳");
		itm4.setOnAction(e->{
			FaceHands hand = lstHands.getSelectionModel().getSelectedItem();
			if(hand==null){
				return;
			}
			FaceBills bill = lstBills.getSelectionModel().getSelectedItem();
			if(bill==null){
				return;
			}
			bill.ref.stampClose();
			DataProvider.push_bills("/pool-3/"+bill.ref.takeSerial(), bill.ref);
			DataProvider.delete("/pool-2/"+hand.ref.takeUID()+"/"+bill.ref.takeSerial());
		});
		
		lstBills.setContextMenu(new ContextMenu(itm1, itm2, itm3, itm4));
		
		lstBills.setOnMouseClicked(event->{
			if(event.getButton()!=MouseButton.PRIMARY){
				return;
			}
			if(event.getClickCount()<2){
				return;
			}
			itm1.getOnAction().handle(null);
		});
	}
	
	private static final DateTimeFormatter fmtDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	private static class PanEditBill extends GridPane {
		private final Label txtOrder = new Label();
		private final DatePicker dayPick = new DatePicker();
		private final TextField[] info = {
			new TextField(),//名稱
			new TextField(),//聯絡
			new TextField(),//地址
		};
		PanEditBill(final ItemBills itm){
			txtOrder.textProperty().bind(DataProvider.propIndex.asString("%04d"));
			dayPick.setConverter(new StringConverter<LocalDate>(){
				@Override
				public String toString(LocalDate object) {
					if(object==null){
						return "";
					}
					return fmtDate.format(object);
				}
				@Override
				public LocalDate fromString(String string) {
					return LocalDate.parse(string);
				}
			});
			getStyleClass().add("layout-medius");
			addRow(0, new Label("訂單編號： "), txtOrder);
			addRow(1, new Label("預約日期： "), dayPick);
			addRow(2, new Label("客戶名稱： "), info[0]);
			addRow(3, new Label("聯絡方式： "), info[1]);
			addRow(4, new Label("聯絡地址： "), info[2]);
			copyFrom(itm);
		}
		ItemBills copyTo(ItemBills itm){
			itm.order = txtOrder.getText();
			itm.stampMeet = dayPick.getValue().format(fmtDate);
			itm.name = info[0].getText();
			itm.info = info[1].getText();
			itm.addr = info[2].getText();
			return itm;
		}
		PanEditBill copyFrom(final ItemBills itm){
			if(itm==null){
				return this;
			}
			dayPick.setValue(LocalDate.parse(itm.stampMeet,fmtDate));
			info[0].setText(itm.name);
			info[1].setText(itm.info);
			info[2].setText(itm.addr);
			return this;
		}
	}
	
	private void dialog_bill(
		final FaceHands hand,
		final FaceBills bill,
		final String... args
	){
		final String txt = (bill==null)?("新增"):("刪除");
		PanEditBill lay = new PanEditBill((bill==null)?(null):(bill.ref));
		if(bill==null){
			if(args.length>=1){
				lay.dayPick.setValue(LocalDate.parse(args[0],fmtDate));
			}
		}
		final Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setTitle(txt);
		dia.setContentText(txt+"訂單");
		dia.getDialogPane().setContent(lay);
		if(dia.showAndWait().get()==ButtonType.OK){
			ItemBills itm;
			if(bill==null){
				itm = new ItemBills();
			}else{
				itm = (bill.ref);
			}
			lay.copyTo(itm);
			if(hand==null){
				DataProvider.push_bills(
					"/pool-1/"+itm.takeSerial(), 
					itm
				);
			}else{
				DataProvider.push_bills(
					"/pool-2/"+hand.ref.takeUID()+"/"+itm.takeSerial(), 
					itm
				);
			}
		}
	}
	
	private static class PanEditHand extends GridPane {
		private final TextField[] info = {
			new TextField(),//名稱
			new TextField(),//電話
			new TextField(),//區域
			new TextField(),//備註
		};
		PanEditHand(final ItemHands itm){
			getStyleClass().add("layout-medius");
			addRow(0, new Label("人員名稱： "), info[0]);
			addRow(1, new Label("聯絡電話： "), info[1]);
			addRow(2, new Label("負責區域： "), info[2]);
			addRow(3, new Label("備註資料： "), info[3]);
			copyFrom(itm);
		}
		ItemHands copyTo(ItemHands itm){
			itm.name = info[0].getText().trim();
			itm.info = info[1].getText().trim();
			itm.zone = info[2].getText().trim();
			itm.memo = info[3].getText().trim();
			return itm;
		}
		PanEditHand copyFrom(final ItemHands itm){
			if(itm==null){
				return this;
			}
			info[0].setText(itm.name);
			info[1].setText(itm.info);
			info[2].setText(itm.zone);
			info[3].setText(itm.memo);
			return this;
		}
	}
	
	public void dialog_hand(final FaceHands hand){
		String txt = (hand==null)?("新增"):("修改");
		PanEditHand lay = new PanEditHand((hand==null)?(null):(hand.ref));
		final Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setTitle(txt);
		dia.setHeaderText(txt+"工作人員");
		dia.getDialogPane().setContent(lay);
		if(dia.showAndWait().get()==ButtonType.OK){
			ItemHands itm;
			if(hand==null){
				//first initialization!!!
				//use contact information for identification.
				itm = new ItemHands();
				itm.fillUID(itm.info);
			}else{
				itm = (hand.ref);
			}
			lay.copyTo(itm);
			DataProvider.push_hands(itm.takeUID(),itm);
		}
	}
	
	private String info2day(DayInfo info){
		if(info==null){
			return "";
		}
		return String.format(
			"%d-%02d-%02d",
			info.year,
			info.month,
			info.day
		);
	}
	
	public void modeCalendar(){
		calendar.setVisible(true);
		panWaits.setVisible(false);			
	}
	
	public void modeWaitingList(){
		calendar.cancelPickup();
		calendar.setVisible(false);		
		panWaits.setVisible(true);			
	}
	
	public void eventShown(){
		
		DataProvider.refer("/pool-1", new ValueEventListener(){
			@Override
			public void onCancelled(DatabaseError arg0) {
				Application.invokeAndWait(()->{
					lstWaits.getItems().clear();
				});
			}
			@Override
			public void onDataChange(DataSnapshot arg0) {				
				ArrayList<FaceBills> lst = new ArrayList<FaceBills>();
				if(arg0.exists()==true){
					for(DataSnapshot ss:arg0.getChildren()){
						ItemBills itm = ss.getValue(ItemBills.class);
						itm.hangSerial(ss.getKey());//important!!!
						lst.add(new FaceBills(itm,1));
					}
				}
				Application.invokeAndWait(()->{
					lstWaits.getItems().setAll(lst);
				});
			}
		});
			
		DataProvider.refer("/hands", new ValueEventListener(){
			@Override
			public void onCancelled(DatabaseError arg0) {
				Application.invokeAndWait(()->{
					lstHands.getItems().clear();
					calendar.update();
				});
			}
			@Override
			public void onDataChange(DataSnapshot arg0) {				
				ArrayList<FaceHands> lst = new ArrayList<FaceHands>();
				if(arg0.exists()==true){
					for(DataSnapshot ss:arg0.getChildren()){
						ItemHands itm = ss.getValue(ItemHands.class);
						itm.fillUID(ss.getKey());
						lst.add(new FaceHands(itm));
					}
				}
				Application.invokeAndWait(()->{
					lstHands.getItems().setAll(lst);
					calendar.update();
				});
			}
		});
	}
}
