package prj.scada;

import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.PanBase;
import narl.itrc.Stepper;

public class StepKindler extends Stepper {

	private DevDCG100 dev;
	
	public StepKindler(DevDCG100 dev){
		this.dev = dev;
		set(op_1,op_2);
	}
	
	final TextField arg1 = new TextField("100");
	
	final JFXToggleButton arg2 = new JFXToggleButton();
	
	final Runnable op_1 = ()->{
		final int val = Integer.valueOf(arg1.getText().trim());
		final boolean flg = arg2.isSelected();
		dev.asyncBreakIn(()->{			
		String res;
		if(flg==true){
			res = dev.exec("SPW="+val);
			if(res.endsWith("*")==false) {
				result.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法設定功率!!"));
				return;
			}
			res = dev.exec("TRG");
			if(res.endsWith("*")==false) {
				result.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法點燃!!"));
				return;
			}
		}else{
			res = dev.exec("OFF");
			if(res.endsWith("*")==false) {
				result.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法關閉!!"));
				return;
			}
		}		
		result.set(NEXT);
	});};
	
	final Runnable op_2 = ()->{
		
	};
	
	@Override
	protected Node getContent(){
		
		arg1.setPrefWidth(50);
		arg1.disableProperty().bind(arg2.selectedProperty().not());
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(1, new Label("功率(Watt)"), arg1);
		lay.add(arg2, 0, 0, 1, 2);
		return lay;
	}
}
