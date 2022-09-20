package prj.sputter.cargo1;

import narl.itrc.Ladder;
import narl.itrc.Stepper;

public class LayLadder extends Ladder {

	public LayLadder(){
		recipe.setMinWidth(200);
		prelogue = event_prelogue;
		epilogue = event_epilogue;		
		addStep("分隔線", Stepper.Sticker.class);
		addStep(StepMassFlow.action_name, StepMassFlow.class);
		addStep(StepGunsHub.action_name, StepGunsHub.class);
		addStep(StepPlsClean.action_name, StepPlsClean.class);
		addStep(StepIgniteRF.action_name, StepIgniteRF.class);
		addStep(StepIgniteDC.action_name, StepIgniteDC.class);
		addStep(StepMonitor.action_name, StepMonitor.class);
		
		/*genButton("--layer--",e->{			
			genStep(Stepper.Sticker.class);
			genStep(StepIgnite1.class);
			genStep(StepIgnite2.class);
			genStep(StepperMonitor.class);
		});*/
	}
	
	private final Runnable event_prelogue = ()->{
		
	};
	
	private final Runnable event_epilogue = ()->{
		
	};
}
