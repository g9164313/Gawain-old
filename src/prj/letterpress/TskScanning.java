package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.TskAction;

public class TskScanning extends TskAction {

	private PanMapWafer map = null;
	
	public TskScanning(PanMapWafer map){
		this.map = map;
	}
	
	@Override
	public int looper(Task<Integer> task) {
		return 0;
	}
}
