package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.TskAction;

public class TskGoHome extends TskAction {

	public TskGoHome(PanBase root){
		super(root);
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
		
		Entry.stg0.exec("JG "+col+"-4000;BG "+tkn+"\r\n");
		do{
			pre = Entry.stg0.pulse[idx].get();
			Misc.delay(500);
			Entry.stg0.exec_TP();
			cur = Entry.stg0.pulse[idx].get();			
			Misc.logv("AXIS-%c %d --> %d",tkn,pre,cur);
		}while(Entry.stg0.isReverse(tkn)==true);
		
		Misc.logv("Panzer Vor !!!");
		
		Entry.stg0.exec("PR "+col+"16000;BG "+tkn+"\r\n");
		do{			
			pre = Entry.stg0.pulse[idx].get();
			Misc.delay(500);
			Entry.stg0.exec_TP();			
			cur = Entry.stg0.pulse[idx].get();			
			Misc.logv("AXIS-%c %d --> %d",tkn,pre,cur);
		}while(Math.abs(cur-pre)>5);
		
		Entry.stg0.exec("WT 500;DE "+col+"0;DP "+col+"0;TP\r\n");
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		walking('A');
		walking('B');
		Entry.stg0.exec_TP();
		Misc.logv("Mission complete");
		return 1;
	}
}
