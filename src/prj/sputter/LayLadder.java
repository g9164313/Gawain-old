package prj.sputter;

import narl.itrc.Ladder;
import narl.itrc.Misc;
import narl.itrc.Stepper;
import prj.sputter.action.StepExploit;
import prj.sputter.action.StepFlowCtrl;
import prj.sputter.action.StepGunsHub;
import prj.sputter.action.StepKindler;
import prj.sputter.action.StepSetFilm;
import prj.sputter.action.StepSetPulse;
import prj.sputter.action.StepWatcher;

public class LayLadder extends Ladder {
	
	public LayLogger logger;
	public DevDCG100 dcg1;
	
	public LayLadder() {
		addStep("分隔線",Stepper.Sticker.class);
		addStep("計數器",Stepper.Counter.class);
		addStep("薄膜設定",StepSetFilm.class);
		addStep("流量控制",StepFlowCtrl.class);
		addStep("電極切換",StepGunsHub.class);
		addStep("脈衝設定",StepSetPulse.class);
		addStep("高壓設定",StepKindler.class);		
		addStep("厚度監控",StepWatcher.class);
		addStep("實驗用"  ,StepExploit.class);
	}
	
	@Override
	protected void prelogue() {
		super.prelogue();
		logger.show_progress();
	}
	@Override
	protected void epilogue() {
		super.epilogue();
		Misc.logw("Lader complete works.");
		dcg1.asyncExec("OFF");//close power~~~
		logger.done_progress();
	}
	@Override
	protected void user_abort() {
		Misc.logw("!!User Abort!!");
		dcg1.asyncExec("OFF");
	}
}
