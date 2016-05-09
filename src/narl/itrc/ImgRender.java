package narl.itrc;

import java.util.concurrent.atomic.AtomicInteger;

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
	
	public interface Filter{
		abstract void initData();//this invoked by controller
		abstract void procData(CamBundle bnd,long ptrMat0,long patMat1);
		abstract void markData(CamBundle bnd,long ptrMat1);//this invoked by render-thread
		abstract void showData(CamBundle bnd,int count);//this invoked by GUI thread
	}
	
	public AtomicInteger fltrCnt = new AtomicInteger(0);
	public Filter fltrObj = null;	
	private Runnable fltrEnd = new Runnable(){
		@Override
		public void run() {
			if(fltrObj==null){
				return;
			}
			fltrObj.showData(bund,fltrCnt.get());
		}
	};
	private void eventFilter(){
		if(fltrObj==null){
			return;
		}
		int cnt = fltrCnt.get();
		if(cnt!=0){
			fltrObj.procData(bund,bund.getMatSrc(),bund.getMatOva());
			Application.invokeAndWait(fltrEnd);			
			if(cnt>0){
				cnt--;
				fltrCnt.set(cnt);
			}
		}
		fltrObj.markData(bund,bund.getMatOva());//keep drawing~~~
	}
	
	private Image buff = null;
	public Image getBuffer(){ 
		return buff;
	}
	@Override
	protected Integer call() throws Exception {
		fltrCnt.set(0);//reset this variable~~~
		//stage.1 - try to open camera~~~
		bund.setup(camIndx, camConf);

		//stage.2 - continue to grab image from camera			
		while(isCancelled()==false){			
			if(Application.GetApplication()==null){						
				return 1;//Platform is shutdown
			}			
			if(bund.optEnbl.get()==false){
				//we have problems to build camera
				if(scrn!=null){
					Application.invokeAndWait(scrn.eventFinal);
				}
				return -2;//always check property
			}
						
			if(ctrl!=null){
				//check whether we need to play video~~~
				if(ctrl.btnPlayer.getState()==false){
					Thread.sleep(50);
					continue;
				}
			}
			
			bund.fetch();
			bund.markData();

			//process image and show data~~~
			eventFilter();

			//update some information			
			if(scrn!=null){
				buff = bund.getImage(1);//show overlay~~
				Application.invokeAndWait(scrn.eventUpdate);
			}
		}
		//Thread is canceled, don't run final-event
		//Let this event be invoked by user
		return 0;
	}	
}
