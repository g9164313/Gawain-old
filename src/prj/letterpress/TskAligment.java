package prj.letterpress;

import javafx.concurrent.Task;
import narl.itrc.ImgRender;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.TskAction;

public class TskAligment extends TskAction {
	
	private ImgRender render;
	
	public TskAligment(ImgRender rend,PanBase pan){		
		super(pan);
		render = rend;
	}
	
	@Override
	protected boolean eventBegin(){
		render.sleep();
		return true;
	}

	private int cnt = 0;
	@Override
	public int looper(Task<Integer> task) {
		Misc.logv("aligment-%d",++cnt);
		Misc.delay(50);
		if(cnt>=100){
			render.invoke();
			return -1;
		}
		return 0;
	}
}
