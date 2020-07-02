package prj.shelter;

import javafx.scene.Node;
import narl.itrc.Stepper;

public class StepRadiate extends Stepper {

	DevHustIO hustio;
	
	public StepRadiate(
		final DevHustIO dev
	){
		hustio = dev;
	}
	
	@Override
	public Node getContent() {
		return null;
	}

	@Override
	public void eventEdit() {
	}

	@Override
	public String flatten() {
		return null;
	}

	@Override
	public void expand(String txt) {
	}

}
