package narl.itrc;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class ImgRender {
	
	private int[] size = {800,600};//dimension
	
	private CamBundle[] lstBundle = null;

	public ImgRender(int width,int height){
		size[0] = width;
		size[1] = height;
	}

	public ImgRender(int width,int height,CamBundle... list){
		size[0] = width;
		size[1] = height;
		setBundle(list);
	}
	
	public ImgRender setBundle(CamBundle... list){
		lstBundle = list;
		lstImage = new Image[list.length]; 
		return this;
	}
	//----------------------//
	
	private Task<Integer> looper;
	
	private LinkedBlockingQueue<Filter> lstFilter = new LinkedBlockingQueue<Filter>();

	private Filter curFilter = null;
	
	private void loopInit(){
		for(CamBundle bnd:lstBundle){
			if(bnd==null){
				continue;
			}
			if(bnd.isReady()==true){
				continue;
			}
			bnd.setup();
		}
	}

	private void loopBody(){
		curFilter = lstFilter.peek();
		for(int i=0; i<lstBundle.length; i++){
			CamBundle bnd = lstBundle[i];
			if(bnd==null){
				continue;//user can give a null bundle, it is valid.
			}
			bnd.fetch();
			lstImage[i] = bnd.getImage();			
		}
		filterCookData();
		Application.invokeAndWait(eventShow);
		filterCheckDone();
	}
	
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
		int pos = name.lastIndexOf(File.separatorChar);
		if(pos<0){
			//set default path~~~
			snapName[0] = "."+File.separatorChar;			
		}else{
			//trim path~~~
			snapName[0] = name.substring(0,pos);
			name = name.substring(pos+1);
		}
		pos = name.lastIndexOf('.');
		if(pos<0){
			return this;
		}		
		snapName[1] = name.substring(0,pos);
		snapName[2] = name.substring(pos);		
		lstFilter.add(fltrSnap);		
		return this;
	}
	//----------------------//

	public interface Filter{
		/**
		 * this invoked by working-thread.<p>
		 * user can process or cook data here.<p>
		 * @param bnd - camera bundle
		 * @return true - we done, take off.<p> false- keep this in queue.<p>
		 */
		abstract void cookData(CamBundle[] bnd);//this invoked by render-thread
		
		/**
		 * this invoked by GUI-thread.<p>
		 * user can show charts or change the state of widget here.<p>
		 * @param bnd - camera bundle
		 * @return true - we done, take off.<p> false- keep this in queue.<p>
		 */
		abstract void showData(CamBundle[] bnd);//this invoked by GUI thread
		
		abstract boolean isDone();
	};
	
	private void filterCookData(){
		if(curFilter!=null){
			curFilter.cookData(lstBundle);
		}
	}
	
	private void filterShowData(){
		if(curFilter!=null){
			curFilter.showData(lstBundle);
		}
	}
	
	private void filterCheckDone(){
		if(curFilter!=null){
			if(curFilter.isDone()==true){
				lstFilter.remove(curFilter);
			}
		}
	}
	
	/**
	 * This is file name for filter 'snap'.<p> 
	 * It means "path", "prefix" and "postfix"(.jpg, .png, .gif, etc).<p>
	 */
	private static String[] snapName = {"","",""};
	private static int snapIndx = 0;
	
	private static Filter fltrSnap = new Filter(){
		@Override
		public void cookData(CamBundle[] list) {
			snapIndx++;
			for(int i=0; i<list.length; i++){
				list[i].saveImage(String.format(
					"%s%s%d-%03d%s",
					snapName[0],snapName[1],
					(i+1),snapIndx,
					snapName[2]
				));
			}		
		}
		@Override
		public void showData(CamBundle[] list) {
			PanBase.msgBox.notifyInfo("Render",String.format(
				"儲存影像(%d) %s",
				snapIndx,
				snapName[1]
			));
		}
		@Override
		public boolean isDone() {
			return true;
		}
	};
	//----------------------//
		
	private Image[] lstImage = null;
	
	private final Runnable eventShow = new Runnable(){
		@Override
		public void run() {
			for(int i=0; i<lstScreen.size(); i++){
				if(i>=lstImage.length){
					return;
				}
				if(lstImage[i]==null){
					continue;
				}
				lstScreen.get(i).setImage(lstImage[i]);
			}
			filterShowData();
		}
	};
	//----------------------//
	
	private ArrayList<ImageView> lstScreen = new ArrayList<ImageView>();

	public Pane genPreview(String title){
		ImageView screen = new ImageView();
		screen.setFitWidth(size[0]);
		screen.setFitHeight(size[1]);
		lstScreen.add(screen);
		return PanBase.decorate(title,screen);
	}
}
