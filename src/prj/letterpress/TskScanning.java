package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.WidMapBase;
import narl.itrc.TskAction;

public class TskScanning extends TskAction {

	private WidMapBase map = null;

	public TskScanning(WidMapBase mapper){
		init(mapper);
	}
	
	public TskScanning(WidMapBase mapper,PanBase pan){
		super(pan);
		init(mapper);
	}
	
	private void init(WidMapBase mapper){
		setName("Task-Scanning");
		map = mapper;
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		if(map==null){
			return -2;
		}
		
		//change speed!!!!
		Misc.logv("--------重置速度--------");
		Entry.stg0.exec(
			"SP 900000,900000;"+
			"AC 700000,700000;"+
			"DC 700000,700000;"+
			"TP\r\n"
		);
		Misc.delay(1500);
		
		long tick = System.currentTimeMillis();
		float tsec;
		map.resetSequence();//Do we need to ask user??
		Misc.logv("--------曝光程序--------");
		Double[] pos;
		while((pos = map.getSequencePath())!=null){		
			Misc.logv("移動至 (%3.1f,%3.1f)",pos[0],pos[1]);
			Entry.stg0.anchorTo("mm",pos);
			//start to exposure
			Misc.logv("照射中...");
			Entry.stg0.exec("OB 2,1\r\n");
			Misc.delay(200);
			Entry.stg0.exec("OB 2,0\r\n");
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
		Entry.stg0.anchorTo("mm",0.,0.);
		Misc.logv("Complete...");
		
		//change speed again!!!!
		Entry.resetDefSpeed();
		return -1;
	}
}
