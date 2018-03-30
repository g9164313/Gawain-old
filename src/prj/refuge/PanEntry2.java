package prj.refuge;

import javafx.scene.Node;
import narl.itrc.PanBase;

public class PanEntry2 extends PanBase {

	public PanEntry2(){
	}

	@Override
	public Node eventLayout(PanBase self) {
		return null;
	}

	@Override
	public void eventShown(PanBase self) {
		//demo code
		/*Task<Integer> tsk = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				long t1 = System.currentTimeMillis();
				while(isCancelled()==false){
					long t2 = System.currentTimeMillis();
					long curr = (t2 - t1)/1000L;
					updateProgress(curr, 10L);
					updateMessage(String.format("prog=%d",(int)curr));
					if(curr>=10L){
						break;
					}										
				}				
				return 3;
			}
		};		
		spinner.kick('m',tsk);*/
	}
}
