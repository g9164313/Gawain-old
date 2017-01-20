package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.TskAction;

/**
 * insert/remove wafer tray
 * @author qq
 *
 */
public class TskHolder extends TskAction {

	public TskHolder(PanBase root){
		super(root);
	}
	
	private void insert_tray(char tkn){
		Entry.stg0.exec("PA -500;BG A;MC A\r\n");
		Entry.stg0.exec_TP();
		/*for(;;){
			Entry.stg0.exec_TP();
			int pre = Entry.stg0.getPulse(tkn);
			Misc.delay(50);
			Entry.stg0.exec_TP();			
			int cur = Entry.stg0.getPulse(tkn);		
			if(Math.abs(cur-pre)<=1){
				break;
			}
		}*/	
	}
	
	private void remove_tray(char tkn){
		Entry.stg0.joggingTo(true,-4000.,tkn);
		do{
			Misc.delay(200);
			Entry.stg0.exec_TP();
		}while(Entry.stg0.isReverse(tkn)==true);
		Entry.stg0.joggingTo(false);
		Entry.stg0.exec_TP();
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		Entry.stg0.exec_TP();
		int pos = Entry.stg0.getPulse('Y');
		if(pos<=1000){
			Misc.logv("退出\n Panzer Vor !!!");
			Entry.stg0.exec("OB 1,0\r\n");//disable air pump~~~
			remove_tray('Y');
		}else{
			Misc.logv("吸入\n Panzer Vor !!!");
			Entry.stg0.exec("OB 1,1\r\n");//enable air pump!!!
			insert_tray('Y');
		}
		Misc.logv("Mission Complete!!!");
		return 1;
	}
}
