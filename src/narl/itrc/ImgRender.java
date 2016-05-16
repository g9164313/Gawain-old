package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;

public class ImgRender{
	
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
	
	private void initBundle(){
		bund = new CamBundle[prvw.length];
		for(int i=0; i<prvw.length; i++){
			bund[i] = prvw[i].bundle;
		}
		buff = new Image[prvw.length];
	}
	
	private Task<Integer> core = null;
	/**
	 *  User must call "stop()" to release camera!!! 
	 */	
	public void launch(){
		if(isWorking()==true){
			return;
		}
		initBundle();
		core = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				if(bund==null){ 
					return 1;//WTF???
				}
				//stage.1 - try to open camera~~~
				if(bundSetup()==0){
					return 2;//no camera!!!
				}
				
				//stage.2 - continue to grab image from camera			
				while(isCancelled()==false){
					if(ctrl!=null){
						//check whether we need to play video~~~
						if(ctrl.btnPlayer.getState()==false){
							Thread.sleep(50);
							continue;
						}
					}
					
					bundFetch();
					if(Application.GetApplication()==null){						
						break;//Platform is shutdown
					}
					//TODO: how to save image???
					eventFilter();//process image and show data~~~
										
					Platform.runLater(eventShow);
					//Application.invokeAndWait(eventShow);
				}
				bundClose();
				return 0;
			}
		};
		core.setOnScheduled(new EventHandler<WorkerStateEvent>(){
			@Override
			public void handle(WorkerStateEvent event) {
				//This is invoked by GUI thread...
				for(ImgPreview pp:prvw){
					pp.initScreen(ImgRender.this);
				}
			}	
		});
		/*core.setOnCancelled(new EventHandler<WorkerStateEvent>(){
			@Override
			public void handle(WorkerStateEvent event) {
				bundClose();
			}
		});*/
		new Thread(core,"imgRender").start();
	}

	public void cancel(){
		if(isWorking()==false){
			return;
		}
		core.cancel();
	}
	
	private int bundSetup(){
		Misc.logv("all-bundles setup");
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
		//Misc.logv("all-bundles fetch");
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
		Misc.logv("all-bundles close");
		for(CamBundle b:bund){
			if(b!=null){
				b.close();
			}
		}
	}
	
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
				//TODO: process ROI handle
				prvw[i].screen.setImage(buff[i]);
			}
		}
	};

	public boolean isWorking(){
		if(core==null){
			return false;
		}
		return core.isRunning();
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
	
	private void eventFilter(){
		if(fltrObj==null){
			return;
		}
		boolean done = fltrObj.cookData(bund);
		final Runnable event = new Runnable(){
			@Override
			public void run() {
				if(fltrObj==null){
					return;
				}
				if(fltrObj.showData(bund)==true){
					fltrObj=null;
				}
			}
		};
		Application.invokeAndWait(event);
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
