package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.TskAction;

public class TskGoHome extends TskAction {

	public TskGoHome(PanBase root){
		super(root);
	}

	private Double[] _offset = {0., 0.};
	
	private void moving(char tkn,double step){
		Entry.stg0.joggingTo(true,step,tkn);
		do{
			Misc.delay(500);
			Entry.stg0.exec_TP();
			int cur = Entry.stg0.getPulse(tkn);
			Misc.logv("AXIS-%c：%d",tkn,cur);
		}while(Entry.stg0.isReverse(tkn)==true);
		Entry.stg0.joggingTo(false);
		Entry.stg0.exec_TP();
	}
	
	private void walking(char tkn){
		moving(tkn,-4000.);
		Entry.stg0.setValue(0,tkn);
		moving(tkn, 4000.);
		int pos = Entry.stg0.getPulse(tkn);		
		pos = pos / 2;
		Entry.stg0.anchorTo((double)pos,tkn);
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		Misc.logv("Panzer Vor !!!");
		walking('X');		
		walking('Y');
		Entry.stg0.setValue(0,0,0);//reset the first origin~~~~
		//Misc.logv("偏移量修正!!!");
		//Entry.stg0.moveTo(_offset);
		//Entry.stg0.setValue(0,0,0);//reset the second origin~~~
		
		Entry.stg0.exec_TP();
		Misc.logv("Mission complete");
		return 1;
	}
}
