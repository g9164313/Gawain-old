package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.ImgRender;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.TskAction;

/**
 * AXIS-A : 10pps <==> 50um
 * AXIS-B : 10pps <==> 50um
 * @author qq
 *
 */
public class TskAligment extends TskAction {
		
	private ImgRender rndr;
	
	public TskAligment(ImgRender render,PanBase root){		
		super(root);
		rndr = render;
	}
	
	@Override
	protected boolean eventBegin(){
		rndr.stop();
		return true;
	}

	private int cnt = 0;
	@Override
	public int looper(Task<Integer> task) {
		Misc.logv("aligment-%d",++cnt);
		Misc.delay(50);
		if(cnt>=100){
			
			return -1;
		}
		return 0;
	}
}
