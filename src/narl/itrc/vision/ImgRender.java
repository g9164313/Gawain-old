package narl.itrc.vision;

import java.io.File;
import java.util.ArrayList;

import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import narl.itrc.Gawain;

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
		cook_data();
		for(ImgPreview prv:lstPreview){
			prv.fetchInfo();
		}
		Application.invokeAndWait(eventShowData);
	}
	private synchronized void cook_data(){
		if(fltr==null){
			return;
		}
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
			lstPreview.add(new ImgPreview(bnd,this));
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
	public boolean isFiltering(){
		if(fltr==null){
			return false;
		}
		return true;
	}
	
	public ImgRender attach(ImgFilter filter){
		if(fltr!=null){
			//if we already had filter, just reset it again!!!
			fltr = null;
			return this;
		}
		fltr = filter;
		if(filter!=null){
			filter.state.set(ImgFilter.STA_REDY);
		}			
		return this;
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
	
	private static FilterExecIJ fltrExecIJ = new FilterExecIJ();

	private static FilterSnap fltrSnap = new FilterSnap();
}
