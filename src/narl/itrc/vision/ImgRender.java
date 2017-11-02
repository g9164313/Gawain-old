package narl.itrc.vision;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;

public class ImgRender {

	private CamBundle[] lstCam;
	
	public ImgRender(CamBundle... cam){
		lstCam = cam;
	}
	//-------------------------------//
	
	private class Looper extends Task<Long>{
		@Override
		protected Long call() throws Exception {
			for(CamBundle cam:lstCam){
				cam.setup();
			}
			
			long tick;
			while(isCancelled()==false){
				
				if(Application.GetApplication()==null){
					break;
				}
				tick = System.currentTimeMillis();
				
				for(CamBundle cam:lstCam){
					if(cam.ptrCntx!=0L){
						continue;
					}
					if(cam.ctrlPlay==false){
						continue;
					}
					cam.fetch();
				}
				
				tick = System.currentTimeMillis() - tick;
				updateValue(tick);				
				updateProgress(tick,1000);
			}
			
			for(CamBundle cam:lstCam){
				cam.close();
			}
			return 0L;
		}		
	};
	
	private Looper looper;
		
	public void launch(String name){
		if(looper!=null){
			if(looper.isDone()==false){
				return;
			}
		}
		looper = new Looper();		
		new Thread(looper,name).start();
	}
	
	public void launch(){
		launch("cam-render");
	}
}
