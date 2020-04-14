package prj.scada;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.Label;
import narl.itrc.Misc;
import narl.itrc.Stepper;

public class Step1 extends Stepper {

	public Step1(){
		Runnable[] ggyy = {aa1,aa2,aa3};
		works = Optional.of(ggyy);
	}
	final Runnable aa1 = ()->{
		Misc.logv("aa1");
	};
	final Runnable aa2 = ()->{
		Misc.logv("aa22");
	};
	final Runnable aa3 = ()->{
		Misc.logv("aa333");
	};
	
	@Override
	protected Node getContent(){
		Label txt = new Label("ggyy-1");
		txt.setMaxWidth(Double.MAX_VALUE);
		txt.getStyleClass().addAll("border");
		return txt;
	}
	
	@Override
	protected void eventEdit(){		
	}
	
}
