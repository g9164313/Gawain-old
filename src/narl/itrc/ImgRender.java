package narl.itrc;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;

public class ImgRender implements Gawain.EventHook {
	
	public ImgRender(CamBundle... list){		
		this();
		addPreview(list);		
	}

	public ImgRender(){
		Gawain.hook(this);
	}
	
	@Override
	public void release() {
		//finally, jump out the main looper ~~~
		stop();
	}

	@Override
	public void shutdown() {
		//camera will be released in this stage~~~~
		for(ImgPreview prv:lstPreview){
			prv.bundle.close();
		}				
	}
	//--------------------------------------------//
	
	private ImgFilter fltr = null;
	
	private ArrayList<ImgPreview> lstPreview = new ArrayList<ImgPreview>();

	private Task<Integer> looper;

	private void loopInit(){
		for(ImgPreview prv:lstPreview){
			if(prv.bundle!=null){
				if(prv.bundle.isReady()==false){
					prv.bundle.setup();
				}
			}
		}
	}

	/**
	 * main body, repeatedly, fetch picture and render it~~~
	 */
	private void loopBody(){		
		for(ImgPreview prv:lstPreview){
			prv.fetchBuff();
		}
		if(fltr!=null){
			cook_data();
		}
		for(ImgPreview prv:lstPreview){
			prv.fetchInfo();
		}
		Application.invokeAndWait(eventShowData);
	}
	private void cook_data(){
		if(fltr.asyncDone!=null){
			if(fltr.asyncDone.get()==false){
				return;
			}
			fltr.asyncDone = null;//reset this flag for next turn~~
		}
		if(fltr.state.get()==ImgFilter.STA_IDLE){
			return;
		}
		fltr.cookData(lstPreview);
		fltr.state.set(ImgFilter.STA_COOK);
	}
	private final Runnable eventShowData = new Runnable(){
		@Override
		public void run() {
			for(ImgPreview prv:lstPreview){
				prv.rendering();//Here!! we update pictures
			}
			if(fltr!=null){
				show_data();
			}			
		}
		private void show_data(){
			if(fltr.asyncDone!=null){
				return;
			}
			if(fltr.state.get()==ImgFilter.STA_COOK){
				if(fltr.showData(lstPreview)==true){
					fltr.state.set(ImgFilter.STA_IDLE);
					fltr = null;//reset it!!!!
				}
			}
		}
	};
	
	private void create_looper(){
		looper = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				loopInit();
				while(
					looper.isCancelled()==false &&
					Gawain.flgStop.get()==false
				){	
					//long diff = System.currentTimeMillis();
					loopBody();
					//if(Gawain.flgStop.get()==true){
					//	break;//check application is valid
					//}
					//diff = System.currentTimeMillis() - diff;
					//Misc.logv("tick=%d",diff);
				}
				return 0;
			}
		};
		new Thread(looper,"Image-Render").start();
	}
	//--------------------------------------------//
	
	/**
	 * start to play video stream.It must be called by GUI-event.<p>
	 * @return self
	 */
	public ImgRender play(){
		if(looper!=null){
			if(looper.isDone()==false){
				return this;
			}
		}
		create_looper();
		flagPlaying.set(true);
		return this;
	}

	/**
	 * Stop looper.This is a blocking method.
	 * It must be called by GUI-event, and it is a blocking function.<p>
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
		flagPlaying.set(false);
		return this;
	}
	
	/**
	 * If render is running, stop it.<p>
	 * If render is done, play it again.<p>
	 * It must be called by GUI-event, and it is a blocking function.<p>
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

	/**
	 * Check whether main looper is running.<p>
	 * @return true - it is running
	 */
	public boolean isPlaying(){
		if(looper!=null){
			if(looper.isDone()==false){
				return true;
			}
		}
		return false;
	}
	
	public SimpleBooleanProperty flagPlaying = new SimpleBooleanProperty(false);
	
	public final boolean getFlagPlaying(){
		flagPlaying.set(isPlaying());
		return flagPlaying.get();
	}
	public final void setFlagPlaying(boolean flag){
		if(flag==true){
			play();
		}else{
			pause();
		}
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
		return attach(fltrSnap);
	}
	
	public ImgRender execIJ(ImgPreview pv){
		fltrExecIJ.prvIndex = lstPreview.indexOf(pv);
		return attach(fltrExecIJ);
	}
	
	public ImgRender attach(ImgFilter filter){
		fltr = filter;
		if(fltr!=null){
			fltr.state.set(ImgFilter.STA_REDY);
		}			
		return this;
	}
	//--------------------------------------------//
	
	private static class FilterExecIJ extends ImgFilter {
		@Override
		public void cookData(ArrayList<ImgPreview> list) {
			String name = Misc.fsPathTemp+File.separator+"temp.png";
			ImgPreview prv = list.get(prvIndex);
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
	//--------------------------------------------//
	
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
			PanBase.notifyInfo("Render",
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
