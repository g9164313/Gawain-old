package narl.itrc;

import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.glass.ui.Application;

import javafx.application.Platform;
import javafx.scene.image.Image;

public class ImgRender {
	
	private ImgControl ctrl = null;
	private ImgPreview[] prvw = null;
	private CamBundle[] bund = null;
	private Image[] buff = null;
	
	public ImgRender(){
	}
	
	public ImgRender(ImgControl control){
		ctrl = control;
	}
	
	public ImgRender(ImgPreview... preview){
		prvw = preview;
	}
	
	public ImgRender(ImgControl control,ImgPreview... preview){
		ctrl = control;
		prvw = preview;
	}
	
	public void setPreview(ImgPreview... preview){
		prvw = preview;
	}
	
	public ImgPreview[] getPreview(){
		return prvw;
	}
	
	public CamBundle getBundle(){
		return (bund==null)?(null):(bund[0]);
	}	
	public CamBundle getBundle(int idx){
		if(bund==null){
			return null;
		}
		if(idx>=bund.length){
			return null;
		}
		return bund[idx];
	}
	
	private void bundPrepare(){
		bund = new CamBundle[prvw.length];
		for(int i=0; i<prvw.length; i++){
			bund[i] = prvw[i].bundle;
		}
		buff = new Image[prvw.length];
		for(ImgPreview pp:prvw){
			pp.initScreen(ImgRender.this);
		}
	}
	
	private int bundSetup(){
		int cnt = bund.length;
		for(int i=0; i<bund.length; i++){
			if(bund[i]==null){
				continue;
			}
			bund[i].resetMark();
			bund[i].syncSetup();
			if(bund[i].optEnbl.get()==false){				
				bund[i] = null;//reset this~~~
				cnt--;
			}
		}
		return cnt;
	}

	private void bundFetch(){
		for(int i=0; i<bund.length; i++){
			if(bund[i]==null){
				buff[i] = null;				
			}else{
				bund[i].fetch();
				bund[i].markData();
				buff[i] = bund[i].getImage(1);//show overlay~~
			}
		}
	}
	
	private void bundClose(){
		for(CamBundle b:bund){
			if(b!=null){
				b.close();
			}			
		}
	}
	
	private Runnable looper = new Runnable(){
		@Override
		public void run() {
			if(bund==null){ 
				return;//WTF???
			}				
			//stage.1 - try to open camera~~~
			if(bundSetup()==0){
				return;//no camera!!!
			}
			//reset flag~~~
			wait0.set(false);
			wait1.set(false);
			//stage.2 - continue to grab image from camera			
			do{
				if(ctrl!=null){
					//check whether we need to play video~~~
					if(ctrl.btnPlayer.getState()==false){
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						continue;
					}
				}
				if(wait0.get()==true){
					synchronized(task){ try{
						wait1.set(true);
						task.wait();
						wait1.set(false);
					} catch (InterruptedException e) {						
						e.printStackTrace();
						continue;
					}}
				}
				if(Application.GetApplication()==null){
					break;//Application is dead!!!
				}
				bundFetch();
				for(ImgPreview pp:prvw){
					doAction(pp);
				}
				passFilter();//process image and show data~~~
				Platform.runLater(eventShow);
			}while(isWorking()==true);
		}
	};
		
	private Thread task = null;
	/**
	 *  User must call "cancel()" to release camera!!! 
	 */	
	public void launch(){
		if(isAlive()==true){
			return;
		}
		bundPrepare();
		task = new Thread(looper,"imgRender");
		task.start();
	}

	public void cancel(){
		if(isAlive()==false){
			return;
		}
		Misc.logv("cancel render");
		bundClose();
	}
	
	private AtomicBoolean wait0 = new AtomicBoolean(false);
	private AtomicBoolean wait1 = new AtomicBoolean(false);
	
	public void sleep(){
		wait0.set(true);
		do{
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}while(wait1.get()==false);
	}
	
	public void invoke(){
		wait0.set(false);//for next turn!!!
		synchronized(task){ task.notify(); }
		while(wait1.get()==true){
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean isAlive(){
		if(task==null){
			return false;
		}
		return task.isAlive();
	}
	
	private boolean isWorking(){
		if(Application.GetApplication()==null){						
			return false;//Platform is shutdown
		}
		//First,check all camera are live~
		for(CamBundle b:bund){
			if(b!=null){
				if(b.optEnbl.get()==true){
					return true;
				}
			}			
		}
		//Second, here, we have no camera :-)
		//just check whether thread is alive~~~
		return isAlive();
	}
	
	/**
	 * Really "render" screen and this interface is executed by GUI thread.<p>
	 */
	private Runnable eventShow = new Runnable(){
		@Override
		public void run() {
			for(int i=0; i<prvw.length; i++){
				if(prvw[i]==null){
					continue;
				}
				if(buff[i]==null){
					continue;
				}
				prvw[i].screen.setImage(buff[i]);
			}
		}
	};

	private DirSN dirSnap = new DirSN(Misc.pathTemp,"snap%.png");
	
	private void doAction(ImgPreview pp){
		switch(pp.action.get()){
		case ImgPreview.ACT_SNAP:{
			Misc.imWrite(
				dirSnap.genSNTxt(),
				pp.bundle.getMatSrc()
			);
			/*int[] zone={0,0,0,0};
			if(bundle.getROI(0,zone)==true){
				Misc.imWriteX(
					Misc.pathTemp+"roi.png",
					pp.bundle.getMatSrc(),
					zone
				);
			}*/
			pp.action.set(ImgPreview.ACT_NONE);			
			Application.invokeAndWait(new Runnable(){
				@Override
				public void run() {
					PanBase.msgBox.notifyInfo(
						"Snap",
						"儲存成 "+Misc.trimPath(dirSnap.getSNTxt())
					);
				}
			});
			}break;
		case ImgPreview.ACT_RECD:{
			}break;
		}
	}	
	//---------------------//
	
	public interface Filter{
		/**
		 * this invoked by controller(GUI thread)<p>
		 * user can prepare data or check any required objects
		 * @param rnd - the object of image-render
		 * @return 
		 * 	true - we done<p>
		 * 	false- ready, go to next stage<p> 
		 */
		abstract boolean initData(ImgRender rndr);
		/**
		 * this invoked by render-thread<p>
		 * user can process or cook data here<p>
		 * @param bnd - camera bundle
		 * @return 
		 * 	true - we done<p>
		 * 	false- ready, go to next turn<p> 
		 */
		abstract boolean cookData(CamBundle[] bnd);//this invoked by render-thread
		/**
		 * this invoked by GUI thread<p>
		 * user can show charts or change the state of widget here<p>
		 * @param bnd - camera bundle
		 * @return 
		 * 	true - we done<p>
		 * 	false- ready, go to next turn<p> 
		 */
		abstract boolean showData(CamBundle[] bnd);//this invoked by GUI thread
	}
	private Filter fltrObj = null;
	
	private void passFilter(){
		if(fltrObj==null){
			return;
		}
		boolean done = fltrObj.cookData(bund);
		Application.invokeAndWait(()->{
			if(fltrObj==null){
				return;
			}
			if(fltrObj.showData(bund)==true){
				fltrObj=null;
			}
		});
		if(done==true){
			fltrObj=null;
		}
	}
		
	public void hookFilter(Filter obj){
		//this method must be invoked by GUI thread~~~
		if(fltrObj!=null){
			PanBase.msgBox.notifyWarning("Render","忙碌中...");
			return;
		}
		if(obj.initData(this)==true){
			return;
		}
		fltrObj = obj; 
	}	
}
