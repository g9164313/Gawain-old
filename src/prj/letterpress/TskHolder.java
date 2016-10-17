package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.DevMotion;
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
			int pre = Entry.stg0.pulse[1].get();
			Misc.delay(500);
			Entry.stg0.exec_TP();			
			int cur = Entry.stg0.pulse[1].get();			
			Misc.logv("AXIS-A：%d",cur);
			if(Math.abs(cur-pre)<=1){
				break;
			}
		}while(true);
		Entry.stg0.exec_TP();
	}
	
	private void remove_tray(){
		Entry.stg0.jogTo(true,DevMotion.PULSE_UNIT, null, -4000.);
		do{
			Misc.delay(500);
			Entry.stg0.exec_TP();
			int cur = Entry.stg0.pulse[1].get();			
			Misc.logv("AXIS-A：%d",cur);
		}while(Entry.stg0.isReverse('A')==true);
		Entry.stg0.jogTo(false,DevMotion.PULSE_UNIT, null, -4000.);
		Entry.stg0.exec_TP();
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		Entry.stg0.exec_TP();
		int pos = Entry.stg0.pulse[1].get();
		if(pos<=1000){
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
