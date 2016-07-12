package narl.itrc;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class ImgRender {
	
	private int[] size = {800,600};//dimension
	
	private CamBundle[] lstBundle = null;
	private Image[] buf = null;
	
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
		buf = new Image[list.length];
		return this;
	}
	//----------------------//
	
	private Task<Integer> looper;

	private void loopInit(){
		for(CamBundle bnd:lstBundle){
			if(bnd.isReady()==true){
				continue;
			}
			bnd.setup();
		}
	}
	
	private int showIdx = 0;
	
	private Image showImg = null;
	
	private final Runnable eventShow = new Runnable(){
		@Override
		public void run() {
			try{
				lstScreen.get(showIdx).setImage(showImg);
			}catch(IndexOutOfBoundsException e){
				return;
			}
		}
	};
	
	private void loopBody(){
		for(showIdx=0; showIdx<lstBundle.length; showIdx++){
			CamBundle bnd = lstBundle[showIdx];
			if(bnd==null){
				continue;//user can give a null bundle, it is valid.
			}
			bnd.fetch();
			showImg = bnd.getImage();
			if(showImg==null){
				continue;
			}			
			Application.invokeAndWait(eventShow);
		}
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
				while(looper.isCancelled()==false){
					loopBody();
				}
				Misc.logv("stop...");
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
		while(looper.isDone()==false){
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
