package prj.economy;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
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

public class FragForm extends HBox {
	
	private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	private final static String TXT_SERIES_NUMBER = "序號:";
	
	private final Label seriesNumber = new Label(TXT_SERIES_NUMBER); 
	
	private final JFXDatePicker workingDate = new JFXDatePicker();
	
	private final JFXTextField[] informField = {
		new JFXTextField(),//名稱
		new JFXTextField(),//電話
		new JFXTextField(),//地址
		new JFXTextField(),//備註
	};
	
	private TableView<ItemBills> pool = new TableView<ItemBills>();

	public FragForm(){
		
		init_data_pool();
		
		final Node nd1 = layout_box();
		final Node nd2 = layout_ctrl();
		VBox.setVgrow(nd1, Priority.ALWAYS);
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("pad-space-small");
		lay1.getChildren().addAll(nd1,nd2);
		
		final StackPane lay2 = new StackPane();
		lay2.getStyleClass().add("pad-space-small");
		lay2.getChildren().add(pool);
		
		setStyle("-fx-spacing: 0px,13px,0px,13px;");
		getChildren().addAll(lay1, lay2);
	}
	
	public ObservableList<ItemBills> getBill(){
		return pool.getSelectionModel().getSelectedItems();
	} 
	
	public void putBill(ObservableList<ItemBills> lst){
		pool.getItems().addAll(lst);
	}
	
	public void clearBill(ObservableList<ItemBills> lst){
		pool.getItems().removeAll(lst);
	}
	
	private Node layout_box(){
		
		final String box_style = "-fx-padding: 3ex 0ex 0ex 0ex;";
		
		workingDate.setPromptText("日期");

		informField[0].setLabelFloat(true);
		informField[0].setPromptText("客戶名稱");
		informField[0].setStyle(box_style);
		
		informField[1].setLabelFloat(true);
		informField[1].setPromptText("聯絡電話");
		informField[1].setStyle(box_style);
		
		informField[2].setLabelFloat(true);
		informField[2].setPromptText("聯絡地址");
		informField[2].setStyle(box_style);
		
		informField[3].setLabelFloat(true);
		informField[3].setPromptText("備註");
		informField[3].setStyle(box_style);
		
		final VBox lay0 = new VBox();
		lay0.setStyle("-fx-spacing: 17px;");
		lay0.getChildren().addAll(
			seriesNumber,
			workingDate,
			informField[0],
			informField[1],
			informField[2],
			informField[3]
		);
		return lay0;
	}
	
	private BooleanProperty flagEdit = new SimpleBooleanProperty(false);
		
	private Node layout_ctrl(){
		
		final Button btn1 = PanBase.genButton1("新增", null);
		btn1.visibleProperty().bind(flagEdit.not());
		btn1.setMaxWidth(Double.MAX_VALUE);
		btn1.setOnAction(event->{			
			ItemBills obj=null;/* = new ItemBills(
				workingDate.getValue().format(fmt),
				informField[0].getText(),
				informField[1].getText(),
				informField[2].getText(),
				informField[3].getText()
			);*/
			pool.getItems().add(obj);
			clear_field();
			//LocalDate date = day.getValue();
			//if(date==null){
			//	return;
			//}
			//lstPool.getItems().add(new Bill(date, box));
			//clear input-field
		});
		
		final Button btn2 = PanBase.genButton1("更新", null);
		btn2.visibleProperty().bind(flagEdit);
		btn2.setMaxWidth(Double.MAX_VALUE);
		btn2.setOnAction(event->{
			ObservableList<ItemBills> lst = pool.getSelectionModel().getSelectedItems();
			if(lst.size()==0){
				return;
			}
			/*lst.get(0).setAll(
				workingDate.getValue().format(fmt),
				informField[0].getText(),
				informField[1].getText(),
				informField[2].getText(),
				informField[3].getText()
			);*/
		});
		
		final StackPane lay0 = new StackPane(btn1, btn2);		
		return lay0;
	}
	
	private void clear_field(){
		seriesNumber.setText(TXT_SERIES_NUMBER);
		workingDate.setValue(null);
		informField[0].setText("");
		informField[1].setText("");
		informField[2].setText("");
		informField[3].setText("");
	}
	
	private void fresh_field(ItemBills obj){
		workingDate.setValue(null);
		//informField[0].setText(obj.name.get());
		//informField[1].setText(obj.phone.get());
		//informField[2].setText(obj.addr.get());
		//informField[3].setText(obj.memo.get());
	}
	
	@SuppressWarnings("unchecked")
	private void init_data_pool(){
		
		final TableColumn<ItemBills,String> col1 = new TableColumn<ItemBills,String>("日期");
		col1.setCellValueFactory(new PropertyValueFactory<ItemBills,String>("workingDate"));

		final TableColumn<ItemBills,String> col2 = new TableColumn<ItemBills,String>("編號");
		col2.setCellValueFactory(new PropertyValueFactory<ItemBills,String>("serialNumber"));
		
		pool.getColumns().addAll(col1,col2);
		pool.setEditable(false);
		pool.getSelectionModel().selectedItemProperty().addListener((obs,old,late)->{
			if(flagEdit.get()==true){
				fresh_field(late);
			}
		});
		
		final String TXT_EDIT = "修改";
		final String TXT_CANCEL="取消";
		
		final MenuItem itm1 = new MenuItem(TXT_EDIT); 
		itm1.setOnAction(event->{
			if(flagEdit.get()==false){
				ObservableList<ItemBills> lst = pool.getSelectionModel().getSelectedItems();
				if(lst.size()==0){
					return;
				}
				//update input-field
				fresh_field(lst.get(0));
				//change button function~~~
				itm1.setText(TXT_CANCEL);
				flagEdit.set(true);
			}else{
				//clear field~~~
				clear_field();
				//user cancel editor, change back again!!!
				itm1.setText(TXT_EDIT);
				flagEdit.set(false);
			}						
		});
		
		final MenuItem itm2 = new MenuItem("刪除"); 
		itm2.setOnAction(event->{
			ObservableList<ItemBills> lst = pool.getSelectionModel().getSelectedItems();
			int cnt = lst.size();
			if(cnt==0){
				return;
			}
			final Alert dia = new Alert(AlertType.CONFIRMATION);
			dia.setTitle("確認");
			dia.setHeaderText(null);
			dia.setContentText("確認刪除 "+cnt+" 筆資料");
			Optional<ButtonType> res = dia.showAndWait();
			if(res.get()==ButtonType.OK){
				pool.getItems().removeAll(lst);
			}
		});
		
		pool.setContextMenu(new ContextMenu(itm1,itm2));
	}	
}
