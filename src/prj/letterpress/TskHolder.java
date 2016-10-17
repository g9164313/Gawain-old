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
	
	private void insert_tray(){
		Entry.stg0.exec("PA 0;BG A\r\n");
		do{			
			int pre = Entry.stg0.pulse[0].get();
			Misc.delay(500);
			Entry.stg0.exec_TP();			
			int cur = Entry.stg0.pulse[0].get();			
			Misc.logv("AXIS-A：%d",cur);
			if(Math.abs(cur-pre)<5){
				break;
			}
		}while(true);
		Entry.stg0.exec("WT 500;DE 0;DP 0;TP\r\n");
	}
	
	private void remove_tray(){
		Entry.stg0.exec("JG -4000;BG A\r\n");
		do{
			Misc.delay(500);
			Entry.stg0.exec_TP();
			int cur = Entry.stg0.pulse[0].get();			
			Misc.logv("AXIS-A：%d",cur);
		}while(Entry.stg0.isReverse('A')==true);
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		Entry.stg0.exec_TP();
		int pos = Entry.stg0.pulse[0].get();
		if(pos>=-400){
			Misc.logv("退出\n Panzer Vor !!!");
			Entry.stg0.exec("OB 1,0\r\n");
			remove_tray();
		}else{
			Misc.logv("吸入\n Panzer Vor !!!");
			Entry.stg0.exec("OB 1,1\r\n");
			insert_tray();
		}		
		return 1;
	}
}
