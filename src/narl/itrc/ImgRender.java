package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.scene.image.Image;

public class ImgRender extends Task<Integer> {
	
	public static int camIndx = 0;
	public static String camConf = null;

	public CamBundle  bund = null;
	public ImgPreview scrn = null;
	public ImgControl ctrl = null;
	
	public ImgRender(CamBundle bundle){
		this(bundle,null,null);
	}
	
	public ImgRender(CamBundle bundle,ImgPreview screen){
		this(bundle,screen,null);
	}
	
	public ImgRender(CamBundle bundle,ImgControl control){
		this(bundle,null,control);
	}
	
	public ImgRender(CamBundle bundle,ImgPreview screen,ImgControl control){
		bund = bundle;
		scrn = screen;
		ctrl = control;
	}
	
	public Image buff = null;
	@Override
	protected Integer call() throws Exception {
		//stage.1 - try to open camera~~~
		bund.setup(camIndx, camConf);

		//stage.2 - continue to grab image from camera			
		while(isCancelled()==false){
			if(Application.GetApplication()==null){						
				return 1;//Platform is shutdown
			}
			if(bund.optEnbl.get()==false){
				if(scrn!=null){
					Application.invokeAndWait(scrn.eventFinal);
				}
				return -2;//always check property
			}
			if(ctrl!=null){
				if(ctrl.btnPlayer.getState()==false){
					Thread.sleep(50);
					continue;
				}
			}
			bund.fetch();
			bund.markData();
			//TODO: hook something~~~~
			//update some information
			buff = bund.getImage(1);//show overlay~~
			if(scrn!=null){
				Application.invokeAndWait(scrn.eventUpdate);
			}
		}
		//Thread is canceled, don't run final-event
		//Let this event be invoked by user
		return 0;
	}
	
}
