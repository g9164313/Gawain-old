package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.DevMotion;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.PanMapBase;
import narl.itrc.TskAction;

public class TskScanning extends TskAction {

	private PanMapBase map = null;
	private DevMotion stg = null;
	
	public TskScanning(DevMotion stage,PanMapBase mapper){
		init(stage,mapper);
	}
	
	public TskScanning(DevMotion stage,PanMapBase mapper,PanBase pan){
		super(pan);
		init(stage,mapper);
	}
	
	private void init(DevMotion stage,PanMapBase mapper){
		title = "Task-Scanning";
		map = mapper;
		stg = stage;
		stg.setFactor(1000.,1000.,1000.,1000);
		stg.setTokenBase('A');
		//'X'->'B',
		//'Y'->'A', 
		stg.setRoutine('A','B');
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		if(stg==null||map==null){
			return -2;
		}
		
		long tick = System.currentTimeMillis();
		float tsec;
		
		map.resetSequence();//Do we need to ask user??		
		Double[] pos;
		while((pos = map.getSequencePath())!=null){
			stg.archTo(pos);
			//start to exposure
			Misc.delay(50);
			if(tsk.isCancelled()==true){
				//user break-down this routine~~~~
				tick = System.currentTimeMillis() - tick;
				tsec = ((float)tick)/1000.f;
				Misc.logv("consume: %.3fsec",tsec);
				return -1;
			}
		}
		
		tick = System.currentTimeMillis() - tick;
		tsec = ((float)tick)/1000.f;
		Misc.logv("consume: %.3fsec",tsec);		
		return -1;
	}
}
