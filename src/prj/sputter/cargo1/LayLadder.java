package prj.sputter.cargo1;

import javafx.geometry.Orientation;
import narl.itrc.Ladder;
import narl.itrc.Stepper;
import prj.sputter.StepRunSPIK;
import prj.sputter.StepSetFilm;
import prj.sputter.StepSetSPIK;

public class LayLadder extends Ladder {

	public LayLadder(){
		super(Orientation.HORIZONTAL);
		recipe.setMinWidth(200);
		prelogue = event_prelogue;
		epilogue = event_epilogue;		
		addStep("分隔線", Stepper.Sticker.class);
		addStep(StepSetFilm.action_name, StepSetFilm.class, PanMain.sqm1);
		addStep(StepMassFlow.action_name, StepMassFlow.class);		
		addStep(StepGunsHub.action_name, StepGunsHub.class);
		//addStep(StepCleanPls.action_name, StepCleanPls.class);
		//addStep(StepIgniteRF.action_name, StepIgniteRF.class);
		addStep(StepSetSPIK.action_name, StepSetSPIK.class, PanMain.spik);
		addStep(StepRunSPIK.action_name, StepRunSPIK.class, PanMain.spik);		
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
