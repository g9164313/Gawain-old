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
		
		stg.exec("JG "+col+"-2000;BG "+tkn+"\r\n");
		do{
			stg.exec_TP();
			Misc.delay(100);
		}while(stg.isReverse(tkn)==true);
		
		stg.exec("DE "+col+"0;TP\r\n");

		stg.exec("JG "+col+"2000;BG "+tkn+"\r\n");
		do{
			stg.exec_TP();
			Misc.delay(100);
		}while(stg.isForward(tkn)==true);
		
		stg.exec(
			"PR "+col+"-15000;"+
		    "BG"+tkn+";MC;WT 1000;"+
			"DE "+col+"0;"+
			"TP\r\n"
		);
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		
		walking('A');
		
		walking('B');
		
		Misc.logv("Mission complete");
		return 1;
	}
}
