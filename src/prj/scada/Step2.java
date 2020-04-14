package prj.scada;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.Label;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class Step2 extends Stepper {

	public Step2(){
		Runnable[] ggyy = {bb1,bb2};
		works = Optional.of(ggyy);
	}
	final Runnable bb1 = ()->{
		Misc.logv("bb1");
	};
	final Runnable bb2 = ()->{
		Misc.logv("bb22");
	};
	
	@Override
	protected Node getContent(){
		Label txt = new Label("ggyy-2");
		txt.setMaxWidth(Double.MAX_VALUE);
		txt.getStyleClass().addAll("border");
		return txt;
	}
	
	@Override
	protected void eventEdit(){		
	}
}
