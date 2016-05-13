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
	
	public CamBundle getBundle(){
		return bund;
	}
	
	public interface Filter{
		/**
		 * this invoked by controller(GUI thread)<p>
		 * user can prepare data or check any required objects
		 * @param rnd - the object of image-render
		 * @return 
		 * 	true - we done<p>
		 * 	false- ready, go to next stage<p> 
		 */
		abstract boolean initData(ImgRender rnd);
		/**
		 * this invoked by render-thread<p>
		 * user can process or cook data here<p>
		 * @param bnd - camera bundle
		 * @return 
		 * 	true - we done<p>
		 * 	false- ready, go to next turn<p> 
		 */
		abstract boolean cookData(CamBundle bnd);//this invoked by render-thread
		/**
		 * this invoked by GUI thread<p>
		 * user can show charts or change the state of widget here<p>
		 * @param bnd - camera bundle
		 * @return 
		 * 	true - we done<p>
		 * 	false- ready, go to next turn<p> 
		 */
		abstract boolean showData(CamBundle bnd);//this invoked by GUI thread
	}
	private Filter fltrObj = null;
	
	private void eventFilter(){
		if(fltrObj==null){
			return;
		}
		boolean done = fltrObj.cookData(bund);
		final Runnable eventShow = new Runnable(){
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
		Application.invokeAndWait(eventShow);
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
	
	private Image buff = null;
	public Image getBuffer(){ 
		return buff;
	}
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
