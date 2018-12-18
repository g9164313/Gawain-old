package prj.economy;

import com.jfoenix.controls.JFXTextField;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import narl.itrc.PanBase;

public class PanEditHands extends PanBase {

	private ItemHands target = null;
	
	public PanEditHands(){
		super("新增工作人員");
	}
	
	public PanEditHands(ItemHands itm){
		super("修改工作人員");
		target = itm;
	}
	
	private final JFXTextField[] info = {
		new JFXTextField(),//人員名稱
		new JFXTextField(),//聯絡電話
		new JFXTextField(),//負責區域
		new JFXTextField(),//備註
	};
	
	private final ListView<String> lstBills = new ListView<>();
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final String box_style = "-fx-padding: 3.5ex 0ex 0ex 0ex;";
		info[0].setLabelFloat(true);
		info[0].setPromptText("人員名稱");
		info[0].setStyle(box_style);
		
		info[1].setLabelFloat(true);
		info[1].setPromptText("工作電話");
		info[1].setStyle(box_style);
		
		info[2].setLabelFloat(true);
		info[2].setPromptText("負責區域");
		info[2].setStyle(box_style);
		
		info[3].setLabelFloat(true);
		info[3].setPromptText("備註");
		info[3].setStyle(box_style);
		
		
		final VBox lay1 = new VBox(
			info[0],
			info[1],
			info[2],
			info[3]
		);
		lay1.getStyleClass().add("vbox-small");

		lstBills.setPrefWidth(133.);
		lstBills.prefHeightProperty().bind(lay1.heightProperty());
				
		final HBox lay2 = new HBox(lay1, lstBills);
		lay2.getStyleClass().add("hbox-small");
		
		final Button btnNew = PanBase.genButton1("新增",null);
		btnNew.setMaxWidth(Double.MAX_VALUE);
		btnNew.setOnAction(event->{
			ItemHands itm = new ItemHands();
			stuff_field(itm);
			DataProvider.push_hands(itm);
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
			DataProvider.push_hands(target);
		});		
		final Button btnDel = PanBase.genButton4("刪除",null);
		btnDel.setMaxWidth(Double.MAX_VALUE);
		btnDel.setOnAction(event->{
			DataProvider.delete("/worker/"+target.info);
			//TODO: pop-up all bills!!!!!
			dismiss();
		});
		
		HBox.setHgrow(btnImp, Priority.ALWAYS);
		HBox.setHgrow(btnNew, Priority.ALWAYS);
		HBox.setHgrow(btnDel, Priority.ALWAYS);
		HBox.setHgrow(btnMod, Priority.ALWAYS);
		VBox.setVgrow(lay2, Priority.ALWAYS);
		
		final HBox lay3 = new HBox(btnImp,btnNew);
		final HBox lay4 = new HBox(btnDel,btnMod);		
		if(target==null){
			lay3.setVisible(true);
			lay4.setVisible(false);
		}else{
			lay3.setVisible(false);
			lay4.setVisible(true);
		}
		final StackPane lay5 = new StackPane(lay3,lay4);
		
		return new VBox(lay2,lay5);
	}

	private ItemHands stuff_field(final ItemHands itm){
		itm.name = info[0].getText().trim();
		itm.info = info[1].getText().trim();
		itm.zone = info[2].getText().trim();
		itm.memo = info[3].getText().trim();
		return itm;
	} 
	
	private void clear_field(){
		info[0].setText("");
		info[1].setText("");
		info[2].setText("");
		info[3].setText("");
	}
	
	@Override
	public void eventShown(Object[] args) {
		if(target!=null){
			info[0].setText(target.name);
			info[1].setText(target.info);
			info[2].setText(target.zone);
			info[3].setText(target.memo);
		}
	}
}
