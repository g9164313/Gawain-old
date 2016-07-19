package narl.itrc;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.scene.layout.Pane;

public class ImgRender {
	
	public ImgRender(){
	}
	
	public ImgRender(CamBundle... list){
		setBundle(list);
	}

	private Task<Integer> looper;

	private void loopInit(){
		for(ImgPreview prv:lstPreview){
			if(prv.bundle==null){
				continue;
			}
			if(prv.bundle.isReady()==true){
				continue;
			}
			prv.bundle.setup();
		}
	}

	private void loopBody(){		
		for(ImgPreview prv:lstPreview){
			prv.fetch();
		}
		for(ImgFilter flt:lstFilter){			
			flt.cookData(lstPreview);
			flt.state.set(ImgFilter.STA_COOK);
		}
		Application.invokeAndWait(eventShow);
	}
	
	private final Runnable eventShow = new Runnable(){
		@Override
		public void run() {
			for(ImgPreview prv:lstPreview){
				prv.refresh();
			}
			for(ImgFilter flt:lstFilter){
				if(flt.isCooked()==false){
					continue;//it is still RAW!!!!
				}
				//First, always set this because 
				//blocking queue not remove object immediately								
				if(flt.showData(lstPreview)==true){
					flt.state.set(ImgFilter.STA_SHOW);
					lstFilter.remove(flt);					
				}else{
					flt.state.set(ImgFilter.STA_IDLE);
				}
			}
		}
	};

	/**
	 * start to play video stream 
	 * @return self
	 */
	public ImgRender play(){
		if(looper!=null){
			if(looper.isDone()==false){
				return this;//looper is running,keep from reentry
			}
		}		
		looper = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				loopInit();
				while(
					looper.isCancelled()==false &&
					Application.GetApplication()!=null
				){	
					//long diff = System.currentTimeMillis();
					loopBody();
					if(Application.GetApplication()==null){
						break;//check application is valid
					}
					//diff = System.currentTimeMillis() - diff;
					//Misc.logv("tick=%d",diff);
				}
				return 0;
			}
		};
		new Thread(looper,"Image-Render").start();
		return this;
	}
	
	/**
	 * Stop looper.This is a blocking method.<p>
	 * @return self
	 */
	public ImgRender stop(){	
		if(looper!=null){
			if(looper.isDone()==true){
				return this;//looper is dead!!!
			}
		}
		looper.cancel();
		//wait for working thread~~~
		while(looper.isRunning()==true){
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			looper.cancel();
		}		
		return this;
	}
	
	/**
	 * If render is running, stop it.<p>
	 * If render is done, play it again.<p>
	 * This method is blocking~~~
	 * @return self
	 */
	public ImgRender pause(){	
		if(looper!=null){
			if(looper.isDone()==false){
				return stop();
			}
		}
		return play();
	}
	
	public boolean isPlaying(){
		if(looper!=null){
			if(looper.isDone()==false){
				return true;
			}
		}
		return false;
	}
	
	public ImgRender snap(String name){
		if(lstFilter.contains(fltrSnap)==true){
			PanBase.msgBox.notifyInfo("Render","忙碌中");
			return this;
		}
		int pos = name.lastIndexOf(File.separatorChar);
		if(pos<0){
			//set default path~~~
			fltrSnap.snapName[0] = "."+File.separatorChar;			
		}else{
			//trim path~~~
			fltrSnap.snapName[0] = name.substring(0,pos);
			name = name.substring(pos+1);
		}
		pos = name.lastIndexOf('.');
		if(pos<0){
			return this;
		}		
		fltrSnap.snapName[1] = name.substring(0,pos);
		fltrSnap.snapName[2] = name.substring(pos);		
		lstFilter.add(fltrSnap);
		return this;
	}
	
	public ImgRender addFilter(ImgFilter fltr){
		if(lstFilter.contains(fltr)==true){
			Misc.logw("已經有Filter");
			return this;
		}
		lstFilter.add(fltr);
		return this;
	}
	
	public ImgRender addFilter(ImgFilter... list){
		for(int i=0; i<list.length; i++){
			addFilter(list[i]);
		}
		return this;
	}
	//----------------------//
	
	private ArrayList<ImgPreview> lstPreview = new ArrayList<ImgPreview>();

	public ImgRender setBundle(CamBundle... list){
		for(int i=0; i<list.length; i++){
			lstPreview.add(new ImgPreview(list[i]));
		}
		return this;
	}
	
	public CamBundle getBundle(int idx){
		if(idx>=lstPreview.size()){
			return null;
		}
		return lstPreview.get(idx).bundle;
	}
	
	public ImgPreview getPreview(int idx){
		if(idx>=lstPreview.size()){
			return null;
		}
		return lstPreview.get(idx);
	}
	
	public Pane getBoard(int idx){
		if(idx>=lstPreview.size()){
			return null;
		}
		return lstPreview.get(idx).getBoard();
	}
	
	public Pane genBoard(String title,int... args){
		int index = 0;
		int width = 640;//default size~~~
		int height= 480;//default size~~~
		switch(args.length){
		case 0:
			index = 0;
			break;
		case 1:
			index = args[0];
			break;
		default:
		case 3:
			index = args[0];
			width = args[1];
			height= args[2];
			break;		
		}
		if(index>=lstPreview.size()){
			return null;
		}
		return lstPreview.get(index).genBoard(title,width,height);
	}
	//----------------------//
	
	private ArrayBlockingQueue<ImgFilter> lstFilter = new ArrayBlockingQueue<ImgFilter>(100);

	private class FilterSnap extends ImgFilter {
		/**
		 * This is file name for filter 'snap'.<p> 
		 * It means "path", "prefix" and "postfix"(.jpg, .png, .gif, etc).<p>
		 */
		public String[] snapName = {"","",""};
		public AtomicInteger snapIndx = new AtomicInteger(0);
		@Override
		public void cookData(ArrayList<ImgPreview> list) {
			int idx = snapIndx.incrementAndGet();
			for(int i=0; i<list.size(); i++){
				//first save the entire image
				ImgPreview prv = list.get(i);
				CamBundle bnd = prv.bundle;
				bnd.saveImage(String.format(
					"%s%s%d_%03d%s",
					snapName[0],snapName[1],(i+1),
					idx,
					snapName[2]
				));
				//second save all ROI inside image
				
				for(int j=0; j<prv.mark.length; j++){
					int[] roi = prv.mark[j].getROI();
					if(roi==null){
						continue;
					}
					bnd.saveImageROI(String.format(
						"%sroi_%s%d_%03d%s",
						snapName[0],snapName[1],(i+1),
						idx,
						snapName[2]
					), roi);
				}
			}	
		}
		@Override
		public boolean showData(ArrayList<ImgPreview> list) {
			PanBase.msgBox.notifyInfo("Render",
			String.format(
				"儲存影像(%d) %s",
				snapIndx.get(),
				snapName[1]
			));
			return true;
		}
	}
	
	private FilterSnap fltrSnap = new FilterSnap();
}
