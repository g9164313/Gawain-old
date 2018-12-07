package prj.economy;

import java.time.LocalDate;
import java.util.Date;

import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXTextField;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanBillsEditor extends PanBase {

	public PanBillsEditor(){
		super("帳單編輯器");
	}
	
	private static final Image img_lock = Misc.getPicImage("lock.png");
	private static final Image img_unlock = Misc.getPicImage("unlock.png");
	
	/**
	 * serial number of bills
	 */
	private final Label txt_indx = new Label("");
	
	//private ObjectProperty<javafx.scene.image.Image> imageProperty = new SimpleObjectProperty<>();
	/**
	 * show this bill has been open or closed.<p>
	 */
	private final ImageView icon = new ImageView();	
	private final Label txt_open = new Label("");
	private final Label txt_done = new Label("");
	private final JFXTextField[] info = {
		new JFXTextField(),//名稱
		new JFXTextField(),//電話
		new JFXTextField(),//地址
		new JFXTextField(),//備註
	};
	
	private final JFXDatePicker pck_meet = new JFXDatePicker();
	
	private final TableView<Integer> tab_cart = new TableView<Integer>();
	
	private ItemBills target = null;
	
	@Override
	public Node eventLayout(PanBase self) {
		
		//Misc.getResImage(pkg, name)
		//lay1.setStyle("-fx-spaceing: 0px 13px 0px 0px;");

		final GridPane lay1 = new GridPane();
		lay1.setMinWidth(200.);
		lay1.add(new Label("帳單編號："), 3, 0);
		lay1.add(new Label("建立日期："), 3, 1);
		lay1.add(new Label("關帳日期："), 3, 2);
		lay1.add(txt_indx, 4, 0);
		lay1.add(txt_open, 4, 1);
		lay1.add(txt_done, 4, 2);
		lay1.add(icon, 0, 0, 3, 3);
		icon.setPickOnBounds(true);
		if(txt_done.getText().length()==0){
			icon.setImage(img_unlock);
		}else{
			icon.setImage(img_lock);
		}
		/*icon.setOnMouseClicked(event->{
			if(txt_done.getText().length()==0){
				txt_done.setText("2018-1-1");
				icon.setImage(img_lock);
			}else{
				txt_done.setText("");
				icon.setImage(img_unlock);
			}
		});*/
		
		final String box_style = "-fx-padding: 3.5ex 0ex 0ex 0ex;";
		
		info[0].setLabelFloat(true);
		info[0].setPromptText("客戶名稱");
		info[0].setStyle(box_style);
		
		info[1].setLabelFloat(true);
		info[1].setPromptText("聯絡電話");
		info[1].setStyle(box_style);
		
		info[2].setLabelFloat(true);
		info[2].setPromptText("聯絡地址");
		info[2].setStyle(box_style);
		
		info[3].setLabelFloat(true);
		info[3].setPromptText("備註");
		info[3].setStyle(box_style);
		
		pck_meet.setAccessibleHelp("預約日期");
		
		final VBox lay2 = new VBox();
		lay2.getStyleClass().add("vbox-small");
		lay2.getChildren().addAll(
			pck_meet,
			info[0],
			info[1],
			info[2],
			info[3]
		);
		
		final VBox lay3 = new VBox(
			lay1,
			lay2
		);
				
		final HBox lay4 = new HBox(lay3, tab_cart);
		HBox.setHgrow(lay3, Priority.ALWAYS);
		HBox.setHgrow(tab_cart, Priority.ALWAYS);
		VBox.setVgrow(lay4, Priority.ALWAYS);
		
		final Button btn = PanBase.genButton1(null,null);
		if(target==null){
			btn.setText("新增");
		}else{
			btn.setText("修改");
		}
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(e->{
			add_item_bills();
		});
		
		final VBox lay0 = new VBox();
		lay0.getChildren().addAll(
			lay4,
			btn
		);
		return lay0;
	}
	
	private void add_item_bills(){
		
		ItemBills itm = new ItemBills();
		itm.stampOpen= txt_open.getText();
		itm.setMeeting(pck_meet.getValue());
		itm.stampDone= txt_done.getText();
		itm.infoName = info[0].getText();
		itm.infoPhone= info[1].getText();
		itm.infoHome = info[2].getText();
		itm.infoMemo = info[3].getText();
		itm.listCart = "";
		
		DataProvider.put_item("/pool-1/"+txt_indx.getText(), itm);
		
		int tmp = DataProvider.propIndex.get();
		DataProvider.propIndex.set(++tmp);
	} 
	
	@Override
	public void eventShown(PanBase self) {
		if(target==null){
			txt_indx.textProperty().bind(DataProvider.propIndex.asString("%04d"));
			txt_open.setText(ItemBills.getOpenDate());
		}else{
			
		}	
	}
}
