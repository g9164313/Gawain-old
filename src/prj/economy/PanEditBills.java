package prj.economy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXTimePicker;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanEditBills extends PanBase {

	public PanEditBills(){
		super("新增帳單");
	}
	
	public PanEditBills(ItemBills itm){
		super("修改帳單");
		target = itm;
	}
	
	private static final Image img_lock = Misc.getPicImage("lock.png");
	private static final Image img_unlock = Misc.getPicImage("unlock.png");
	
	/**
	 * Indicate this bill has been open or closed.<p>
	 */
	private final ImageView clk_done = new ImageView();	

	private final Label txt_order = new Label();
	private final JFXDatePicker pck_date = new JFXDatePicker();
	private final JFXTimePicker pck_time = new JFXTimePicker();
	
	private final JFXTextField[] info = {
		new JFXTextField(),//名稱
		new JFXTextField(),//聯絡
		new JFXTextField(),//地址
	};
	
	private final TableView<ItemBills.Thing> tab_cart = new TableView<>();
	
	private ItemBills target = null;
	
	@SuppressWarnings("unchecked")
	private void init_table_cart(){
		
		final TableColumn<ItemBills.Thing, String> col_name = new TableColumn<>("品名");
		col_name.setCellValueFactory(new Callback<CellDataFeatures<ItemBills.Thing,String>,ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills.Thing, String> param) {
				ItemBills.Thing itm = param.getValue();
				return new SimpleStringProperty(String.format("%s", itm.name));
			}
		});
		
		final TableColumn<ItemBills.Thing, String> col_price = new TableColumn<>("費用");
		col_price.setCellValueFactory(new Callback<CellDataFeatures<ItemBills.Thing,String>,ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills.Thing, String> param) {
				ItemBills.Thing itm = param.getValue();
				return new SimpleStringProperty(String.format("%.2f", itm.fee));
			}
		});
		
		final TableColumn<ItemBills.Thing, String> col_count = new TableColumn<>("數量");
		col_count.setCellValueFactory(new Callback<CellDataFeatures<ItemBills.Thing,String>,ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills.Thing, String> param) {
				ItemBills.Thing itm = param.getValue();
				return new SimpleStringProperty(String.format("%.2f", itm.cnt));
			}
		});
		
		final TableColumn<ItemBills.Thing, String> col_total = new TableColumn<>("金額");
		col_total.setCellValueFactory(new Callback<CellDataFeatures<ItemBills.Thing,String>,ObservableValue<String>>(){
			@Override
			public ObservableValue<String> call(CellDataFeatures<ItemBills.Thing, String> param) {
				ItemBills.Thing itm = param.getValue();
				return new SimpleStringProperty(itm.totalText());
			}
		});
		
		final MenuItem itm1 = new MenuItem("新增品項");
		itm1.setOnAction(event->show_edit_thing(-1));
		final MenuItem itm2 = new MenuItem("修改品項");
		itm2.setOnAction(event->{
			int idx = tab_cart.getSelectionModel().getSelectedIndex();
			if(idx<0){
				return;
			}
			show_edit_thing(idx);
		});
		final MenuItem itm3 = new MenuItem("刪除品項");
		itm3.setOnAction(event->{
			ItemBills.Thing obj = tab_cart.getSelectionModel().getSelectedItem();
			if(obj==null){
				return;
			}
			tab_cart.getItems().remove(obj);
		});
		final MenuItem itm4 = new MenuItem("清除選取");
		itm4.setOnAction(event->tab_cart.getSelectionModel().clearSelection());
		
		tab_cart.setContextMenu(new ContextMenu(
			itm1, itm2, itm3, itm4
		));
		
		tab_cart.getColumns().addAll(
			col_name,
			col_price,
			col_count,
			col_total
		);
	}
	
	private void show_edit_thing(int idx){
		
		ItemBills.Thing obj = null;
		
		final Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setTitle(null);
		dia.setContentText(null);
		
		final String box_style = "-fx-padding: 3.5ex 0ex 0ex 0ex;";
		final JFXTextField[] cols = {
			new JFXTextField(),
			new JFXTextField(),
			new JFXTextField(),
		};
		
		cols[0].setLabelFloat(true);
		cols[0].setPromptText("品項");
		cols[0].setStyle(box_style);
		
		cols[1].setLabelFloat(true);
		cols[1].setPromptText("價目");
		cols[1].setStyle(box_style);
		
		cols[2].setLabelFloat(true);
		cols[2].setPromptText("個數");
		cols[2].setStyle(box_style);
		
		final VBox lay0 = new VBox(cols);
		lay0.getStyleClass().add("vbox-small");
		if(idx>=0){
			obj = tab_cart.getItems().get(idx);
			cols[0].setText(obj.name);
			cols[1].setText(String.format("%.02f", obj.fee));
			cols[2].setText(String.format("%.02f", obj.cnt));
		}
		dia.getDialogPane().setContent(lay0);
		
		if(ButtonType.CANCEL==dia.showAndWait().get()){
			return;
		}
		try{
			final String name = cols[0].getText().trim();
			final float fee = Float.valueOf(cols[1].getText().trim());
			final float cnt = Float.valueOf(cols[2].getText().trim());
			if(idx<0){
				obj = new ItemBills.Thing();
				obj.name = name;
				obj.fee = fee;
				obj.cnt = cnt;
				tab_cart.getItems().add(obj);
			}else{
				obj = tab_cart.getItems().get(idx);
				obj.name = name;
				obj.fee = fee;
				obj.cnt = cnt;
				tab_cart.getItems().set(idx,obj);				
			}
		}catch(NumberFormatException e){
			Misc.logw("wrong value - %s, %s", 
				cols[1].getText(),
				cols[2].getText()
			);
		}		
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		init_table_cart();
		
		if(target!=null){
			clk_done.setPickOnBounds(true);			
			if(target.isClosed()==true){
				clk_done.setImage(img_lock);
			}else{
				clk_done.setImage(img_unlock);
			}
			clk_done.setOnMouseClicked(e->{
				if(target.isClosed()==true){
					//open account again!!!
					target.stampOpen();
					clk_done.setImage(img_unlock);
				}else{
					target.stampClose();
					clk_done.setImage(img_lock);
				}
			});
		}else{
			//After creating bills, user can just close it.
			clk_done.setVisible(false);
		}
		pck_date.setAccessibleHelp("預約日期");
		pck_time.setAccessibleHelp("預約時間");
		
		final String box_style = "-fx-padding: 3.5ex 0ex 0ex 0ex;";
		info[0].setLabelFloat(true);
		info[0].setPromptText("客戶名稱");
		info[0].setStyle(box_style);
		info[1].setLabelFloat(true);
		info[1].setPromptText("聯絡方式");
		info[1].setStyle(box_style);
		info[2].setLabelFloat(true);
		info[2].setPromptText("預約地點");
		info[2].setStyle(box_style);
		
		final String lay_style = 
			"-fx-padding: 17.0px 0.0px 17.0px 0.0px;"+
			"-fx-spacing: 17.0px 0.0px 17.0px 0.0px;";
		
		final GridPane lay1 = new GridPane();
		lay1.setStyle(lay_style);
		lay1.addRow(0, new Label("訂單編號："), txt_order);
		lay1.add(clk_done, 0, 0, 1, 1);
		lay1.add(pck_date, 0, 1, 2, 1);
		lay1.add(pck_time, 0, 2, 2, 1);
		lay1.add(info[0], 0, 3, 2, 1);
		lay1.add(info[1], 0, 4, 2, 1);
		lay1.add(info[2], 0, 5, 2, 1);
		
		final HBox layData = new HBox(lay1, tab_cart);
		layData.setStyle(lay_style);
		HBox.setHgrow(lay1, Priority.ALWAYS);
		HBox.setHgrow(tab_cart, Priority.ALWAYS);
		VBox.setVgrow(layData, Priority.ALWAYS);
		
		final Button btnNew = PanBase.genButton1("新增",null);
		btnNew.setMaxWidth(Double.MAX_VALUE);
		btnNew.setOnAction(event->{
			final ItemBills itm = new ItemBills();
			itm.order = txt_order.getText();
			stuff_field(itm);
			DataProvider.push_bills("/pool-1/"+itm.takeSerial(), itm);
			increase_index();
			clear_field();
		});
		final Button btnImp = PanBase.genButton2("匯入",null);
		btnImp.setMaxWidth(Double.MAX_VALUE);
		btnImp.setOnAction(event->{
		});
		final Button btnMod = PanBase.genButton1("修改",null);
		btnMod.setMaxWidth(Double.MAX_VALUE);
		btnMod.setOnAction(event->{
			stuff_field(target);
			String b_id = target.takeSerial();
			if(target.isClosed()==false){
				DataProvider.push_bills("/pool-1/"+b_id, target);
				DataProvider.delete("/pool-3/"+b_id);
			}else{				
				DataProvider.push_bills("/pool-3/"+b_id,	target);
				DataProvider.delete("/pool-1/"+b_id);
			}			
		});		
		final Button btnDel = PanBase.genButton4("刪除",null);
		btnDel.setMaxWidth(Double.MAX_VALUE);
		btnDel.setOnAction(event->{
			DataProvider.delete("/pool-1/"+target.takeSerial());
			dismiss();
		});
		HBox.setHgrow(btnNew, Priority.ALWAYS);
		HBox.setHgrow(btnImp, Priority.ALWAYS);
		HBox.setHgrow(btnMod, Priority.ALWAYS);
		HBox.setHgrow(btnDel, Priority.ALWAYS);

		final HBox lay_btn1 = new HBox(btnImp,btnNew);
		final HBox lay_btn2 = new HBox(btnDel,btnMod);		
		final StackPane layCtrl = new StackPane(lay_btn1,lay_btn2);
		if(target==null){
			lay_btn1.setVisible(true);
			lay_btn2.setVisible(false);
		}else{
			lay_btn1.setVisible(false);
			lay_btn2.setVisible(true);
		}
		
		final VBox lay0 = new VBox();
		lay0.getChildren().addAll(
			layData,
			layCtrl
		);
		return lay0;
	}
	
	private static final DateTimeFormatter dtf1 = DateTimeFormatter.ISO_LOCAL_DATE;	
	private static final DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("HH:mm");
	
	private void stuff_field(final ItemBills itm){
		String txt_date, txt_time;
		final LocalDate date = pck_date.getValue();
		if(date==null){
			txt_date = LocalDate.now().format(dtf1);
		}else{
			txt_date = date.format(dtf1);
		}
		final LocalTime time = pck_time.getValue();
		if(time==null){
			txt_time = LocalTime.now().format(dtf2);
		}else{
			txt_time = time.format(dtf2);
		}
		itm.stampMeet = txt_date + " " + txt_time;
		itm.name = info[0].getText();
		itm.info = info[1].getText();
		itm.addr = info[2].getText();
		itm.listCart = ItemBills.thing2cart(tab_cart.getItems());
	} 
	
	private void increase_index(){
		int tmp = DataProvider.propIndex.get();
		tmp = tmp + 1;
		DataProvider.propIndex.set(tmp);
	}
	
	private void clear_field(){
		pck_date.setValue(null);
		pck_time.setValue(null);
		info[0].setText("");
		info[1].setText("");
		info[2].setText("");
		tab_cart.getItems().clear();
	}

	@Override
	public void eventShown(Object[] args) {
		if(target==null){
			clear_field();
			txt_order.textProperty().bind(DataProvider.propIndex.asString("%04d"));
		}else{
			stuff_field(target);
			txt_order.setText(target.order);
		}
	}
}
