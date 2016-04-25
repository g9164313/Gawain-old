package prj.seesaw;

import narl.itrc.BtnTask;
import narl.itrc.DlgTask;
import narl.itrc.Misc;

public class TskMatch extends BtnTask {
	
	private Entry e;
	
	public TskMatch(Entry root) {
		//super("開始對位","取消對位");//TODO:we have bug!!!
		super("對位程序");
		e = root;
	}
	
	@Override
	public boolean execute(DlgTask dlg) {
		for(int i=0; i<100; i++){
			dlg.updateMessage("process-"+i);
			Misc.delay(25);
		}
		return true;
	}
	
	@Override
	public boolean isReady(DlgTask dlg) {
		e.panMotion.setDisable(true);
		return true; 
	}
	@Override
	public boolean prepare(DlgTask dlg) { 
		return true; 
	}
	@Override
	public boolean isEnded(DlgTask dlg) {
		e.panMotion.setDisable(false);
		return true;
	}
}
