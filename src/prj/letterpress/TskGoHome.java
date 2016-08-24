package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.TskAction;

public class TskGoHome extends TskAction {

	private DevB140M stg = null;
	
	public TskGoHome(DevB140M stage,PanBase root){
		super(root);
		stg = stage;		
	}

	private void walking(char tkn){
		
		String col=null;
		switch(tkn){
		case 'A': col=""   ;break;
		case 'B': col=","  ;break;
		case 'C': col=",," ;break;
		case 'D': col=",,,";break;
		default:
			return;
		}
		int idx = tkn - 'A';
		int pre,cur;
		
		Misc.logv("Panzer Vor !!!");
		stg.exec("JG "+col+"-4000;BG "+tkn+"\r\n");
		do{
			pre = stg.pulse[idx].get();
			Misc.delay(500);
			stg.exec_TP();
			cur = stg.pulse[idx].get();			
			Misc.logv("Count-[%c] %d --> %d",tkn,pre,cur);
		}while(stg.isReverse(tkn)==true);
		
		Misc.logv("Panzer Vor !!!");
		stg.exec("PR "+col+"16000;BG "+tkn+"\r\n");
		
		do{			
			pre = stg.pulse[idx].get();
			Misc.delay(500);
			stg.exec_TP();			
			cur = stg.pulse[idx].get();			
			Misc.logv("Count-[%c] %d --> %d",tkn,pre,cur);
		}while(Math.abs(cur-pre)>5);
		
		stg.exec("WT 500;DE "+col+"0;DP "+col+"0;TP\r\n");
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		
		walking('A');
		
		walking('B');
		
		Misc.logv("Mission complete");
		return 1;
	}
}
