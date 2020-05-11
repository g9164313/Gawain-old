package prj.scada;

import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import narl.itrc.PanBase;
import narl.itrc.Stepper;

public class StepShutter extends Stepper {

	private DevSQM160 dev;
	
	public StepShutter(DevSQM160 dev){
		this.dev = dev;
		set(op_1,op_2);
	}
	
	final JFXToggleButton btn = new JFXToggleButton();
	
	final Runnable op_1 = ()->{
		final String cmd = (btn.isSelected())?("U1"):("U0");		
		dev.asyncBreakIn(()->{
		if(dev.exec(cmd).charAt(0)=='A') {
			result.set(NEXT);
		}else{
			result.set(ABORT);
			Application.invokeLater(()->PanBase.notifyError("失敗", "無法控制擋板!!"));
		}
	});};
	
	final Runnable op_2 = ()->{
		
	};
	
	@Override
	protected Node getContent(){
		btn.setText("擋板");
		HBox lay = new HBox(btn);
		lay.setAlignment(Pos.CENTER_LEFT);
		return lay;
	}
}
