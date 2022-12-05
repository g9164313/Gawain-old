package prj.sputter.diagram;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

public class BrickDst extends Brick {
	
	public BrickDst() {
		setOnMouseClicked(e->popup_confirm());
	}
	
	private final SimpleBooleanProperty isWork = new SimpleBooleanProperty(false);
	
	private void popup_confirm() {
		String head="", ctxt="";
		final boolean flag = isWork.get();
		if(flag==true) {
			head = "是否關閉？";
			ctxt = "關閉抽氣馬達？";
		}else {
			head = "是否開啟？";
			ctxt = "開啟抽氣馬達？";
		}
		//--------------------
		Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setTitle("抽氣馬達控制");		
		dia.setHeaderText(head);
		dia.setContentText(ctxt);		
		Optional<ButtonType> opt = dia.showAndWait();
		if(opt.get()==ButtonType.CANCEL) {
			return;
		}
		//--------------------
		isWork.set(!flag);
	}
		
	public BrickDst connect(BooleanProperty... args) {
		for(BooleanProperty v:args) {
			v.bind(isWork);
		}
		return this;
	};
}
