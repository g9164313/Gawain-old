package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.DevMotion;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.WidMapBase;
import narl.itrc.TskAction;

public class TskScan extends TskAction {

	private WidMapBase map = null;
	private DevMotion stg = null;
	
	public TskScan(WidMapBase mapper){
		init(mapper);
	}
	
	public TskScan(WidMapBase mapper,PanBase pan){
		super(pan);
		init(mapper);
	}
	
	private void init(WidMapBase mapper){
		title = "Task-Scanning";
		map = mapper;
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		if(stg==null||map==null){
			return -2;
		}
		
		long tick = System.currentTimeMillis();
		float tsec;
		map.resetSequence();//Do we need to ask user??
		Misc.logv("--------曝光程序--------");
		Double[] pos;
		while((pos = map.getSequencePath())!=null){		
			Misc.logv("移動至 (%3.1f,%3.1f)",pos[0],pos[1]);
			stg.anchorTo("mm",pos);
			//start to exposure
			Misc.logv("照射中...");
			Misc.delay(1000);
			if(tsk.isCancelled()==true){
				//user break-down this routine~~~~
				tick = System.currentTimeMillis() - tick;
				tsec = (((float)tick)/1000.f);
				Misc.logv("經歷: %.3fsec",tsec);
				return -1;
			}
		}
		tick = System.currentTimeMillis() - tick;
		tsec = ((float)tick)/1000.f;
		Misc.logv("歷時: %.3fsec",tsec);		
		stg.anchorTo("mm",0.,0.);
		Misc.logv("go home...");
		return -1;
	}
}
