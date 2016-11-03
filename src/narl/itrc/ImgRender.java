package narl.itrc;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;

public class ImgRender implements Gawain.EventHook {
	
	public ImgRender(){
		Gawain.hook(this);
	}
	
	public ImgRender(CamBundle... list){		
		this();
		addPreview(list);		
	}

	@Override
	public void release() {
		stop();
	}

	@Override
	public void shutdown() {
		//camera will be released in this stage~~~~
	}
	
	private ArrayBlockingQueue<ImgFilter> lstFilter = new ArrayBlockingQueue<ImgFilter>(100);
	
	private ArrayList<ImgPreview> lstPreview = new ArrayList<ImgPreview>();

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
			prv.fetchBuff();
		}
		for(ImgFilter flt:lstFilter){			
			flt.cookData(lstPreview);
			flt.state.set(ImgFilter.STA_COOK);
		}
		for(ImgPreview prv:lstPreview){
			prv.fetchInfo();
		}
		Application.invokeAndWait(eventRender);
		for(ImgFilter flt:lstFilter){
			if(flt.state.get()==ImgFilter.STA_SHOW){
				lstFilter.remove(flt);//??not sure??
			}
		}
	}

	private final Runnable eventRender = new Runnable(){
		@Override
		public void run() {
			for(ImgPreview prv:lstPreview){
				prv.rendering();//Here!! we update pictures
			}
			for(ImgFilter flt:lstFilter){
				if(flt.isCooked()==false){
					continue;//it is still RAW!!!!
				}
				//First, always set this because 
				//blocking queue not remove object immediately	
				//'showData' decides whether we should remove this filter~~~
				if(flt.showData(lstPreview)==true){
					flt.state.set(ImgFilter.STA_SHOW);	
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
				return this;
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
					if(Gawain.flgStop.get()==true){
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
		if(looper==null){
			return this;
		}
		while(looper.isDone()==false){
			looper.cancel();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
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
	//--------------------------------------------//

	public int getSize(){
		return lstPreview.size();
	}
	
	public ImgRender addPreview(CamBundle... list){
		for(CamBundle bnd:list){			
			lstPreview.add(new ImgPreview(this,bnd));
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
	//--------------------------------------------//
	
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
	
	public ImgRender execIJ(ImgPreview pv){
		if(lstFilter.contains(fltrExecIJ)==true){
			PanBase.msgBox.notifyInfo("Render","忙碌中");
			return this;
		}
		fltrExecIJ.prvIdx = lstPreview.indexOf(pv);
		lstFilter.add(fltrExecIJ);
		return this;
	}
	
	public ImgRender attach(ImgFilter... list){
		//They are attached by GUI-event
		for(ImgFilter fltr:list){
			if(lstFilter.contains(fltr)==false){
				lstFilter.add(fltr);
			}
		}
		return this;
	}
	
	/*public void detach(ImgFilter fltr){
		if(lstFilter.contains(fltr)==true){			
			lstFilter.remove(fltr);
		}
	}*/
	//--------------------------------------------//
	
	private static class FilterExecIJ extends ImgFilter {
		@Override
		public void cookData(ArrayList<ImgPreview> list) {
			String name = Misc.fsPathTemp+File.separator+"temp.png";
			ImgPreview prv = list.get(prvIdx);
			CamBundle bnd = prv.bundle;
			bnd.saveImage(name);
			Misc.execIJ(name);
		}
		@Override
		public boolean showData(ArrayList<ImgPreview> list) {
			return true;
		}
	}	
	
	private static FilterExecIJ fltrExecIJ = new FilterExecIJ();
	
	private static class FilterSnap extends ImgFilter {
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

	private static FilterSnap fltrSnap = new FilterSnap();
}
