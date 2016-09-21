package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.DevMotion;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.WidPanMapBase;
import narl.itrc.TskAction;

public class TskScanning extends TskAction {

	private WidPanMapBase map = null;
	private DevMotion stg = null;
	
	public TskScanning(DevMotion stage,WidPanMapBase mapper){
		init(stage,mapper);
	}
	
	public TskScanning(DevMotion stage,WidPanMapBase mapper,PanBase pan){
		super(pan);
		init(stage,mapper);
	}
	
	private void init(DevMotion stage,WidPanMapBase mapper){
		title = "Task-Scanning";
		map = mapper;
		stg = stage;
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
			stg.archTo(pos);
			
			//start to exposure
			Misc.logv("expose...");
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
		Misc.logv("經歷: %.3fsec",tsec);	
		
		stg.archTo(0.,0.);
		Misc.logv("go home...");
		return -1;
	}
}
