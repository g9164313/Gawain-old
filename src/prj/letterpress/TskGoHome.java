package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.TskAction;

public class TskGoHome extends TskAction {

	public TskGoHome(PanBase root){
		super(root);
	}

	private void jog_to_limit(char tkn,double step){
		Entry.stg0.joggingTo(true,step,tkn);
		for(;;){
			Misc.delay(500);
			Entry.stg0.exec_TP();
			//int cur = Entry.stg0.getPulse(tkn);
			//Misc.logv("AXIS-%c：%d",tkn,cur);
			if(step<0){
				if(Entry.stg0.isReverse(tkn)==false){
					break;
				}
			}else{
				if(Entry.stg0.isForward(tkn)==false){
					break;
				}
			}
		}
		Entry.stg0.joggingTo(false);
		Entry.stg0.exec_TP();
	}
	
	private void walking(char tkn){
		jog_to_limit(tkn,-4000.);
		Entry.stg0.setValue(0,tkn);
		jog_to_limit(tkn, 4000.);
		Misc.logv("到達正極限");
		int pos = Entry.stg0.getPulse(tkn);
		pos = pos / (2*50);//why????
		Entry.stg0.moveTo(tkn,(double)pos);
		Misc.logv("到達負極限");
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		Misc.logv("Panzer Vor !!!");
		walking('X');		
		walking('Y');
		Entry.stg0.setValue(0,0,0);
		Misc.logv("偏移量修正!!!");
		Entry.stg0.moveTo(1500.+50.,-1560.+50.);//check this error offset~~~
		Entry.stg0.setValue(0,0,0);
		Entry.stg0.exec_TP();
		Misc.logv("Mission complete");
		return 1;
	}
}
