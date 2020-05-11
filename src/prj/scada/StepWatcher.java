package prj.scada;

import com.sun.glass.ui.Application;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.PanBase;
import narl.itrc.Stepper;

public class StepWatcher extends Stepper {

	private DevSQM160 dev;
	
	public StepWatcher(DevSQM160 dev){
		this.dev = dev;
		set(oper_1,oper_2);
	}
	
	final TextField[] args = {
		new TextField("50.00"),//density
		new TextField("200"),//tooling
		new TextField("5.0"),//z-ratio
		new TextField("100.0")//final thickness
	};
		
	//final Runnable oper_0 = ()->{
		//PanBase pan = (PanBase)getScene().getUserData();
		//pan.notifyEvent("ggyy", null, null);
		//flag.set(0);
	//};
	final Runnable oper_1 = ()->{
		
		final String[] vals = {
			args[0].getText().trim(),//density
			args[1].getText().trim(),//tooling
			args[2].getText().trim(),//z-ratio
			args[3].getText().trim(),//final thickness
			"0.000",//thickness set-point(kA)
			"0",//time set-point (mm:ss)
			"1",//active sensor
		};
		
		dev.asyncBreakIn(()->{
		//reset film data, include its tooling and final thick
		String cmd = String.format(
			"A%C%s %s %s %s %s %s %s %s",
			(char)(48+1), "TEMPFILM", 
			vals[0], vals[1], vals[2], vals[3], 
			vals[4], vals[5], vals[6]
		);
		if(dev.exec(cmd).charAt(0)=='A') {
			result.set(NEXT);
		}else{
			result.set(ABORT);
			Application.invokeLater(()->PanBase.notifyError("失敗", "無法設定薄膜參數!!"));
		}
	});};
	final Runnable oper_2 = ()->{
		//monitor shutter state
		if(dev.shutter.get()==false){
			result.set(Stepper.HOLD);
		}else{
			result.set(Stepper.NEXT);
		}
	};
	
	@Override
	protected Node getContent(){
		for(TextField box:args){
			box.setPrefWidth(100);
		}
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, new Label("密度(g/cm3)"), args[0]);
		lay.addColumn(2, new Label("Tooling(%)"), args[1]); 
		lay.addColumn(4, new Label("Z 參數")     , args[2]); 
		lay.addColumn(6, new Label("最終厚度(kÅ)"), args[3]);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.add(new Separator(Orientation.VERTICAL), 3, 0, 1, 2);
		lay.add(new Separator(Orientation.VERTICAL), 5, 0, 1, 2);
		return lay;
	}
	
	@Override
	protected void eventEdit(){
	}
}
