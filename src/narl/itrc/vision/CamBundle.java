package narl.itrc.vision;

import com.sun.glass.ui.Application;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.util.Duration;

public abstract class CamBundle extends ImgPreview {

	public CamBundle(){
	}
	//--------------------------------//

	/**
	 * If value is -1, 0 or 1, the picture keep the same size.<p>
	 * Positive value means 'enlarge' or 'Zoom-In'.<p>
	 * Negative value means 'shrink' or Zoom-Out'.<p>
	 * This variable is also changed by preview panel.<p> 
	 * This will be read in native code.<p>
	 */
	//private int zoomScale = 1;
	
	//--------------------------------//
	
	/**
	 * Point to a context for whatever devices.<p>
	 * This pointer also shows whether bundle is ready.<p>
	 * !!! This variable is changed by native code !!! <p>
	 */
	protected long ptrCntx = 0;
	
	/**
	 * When camera retrieve image(Mat), this variable show the image(Mat) size.<p>
	 */
	protected int bufSizeW = 0;
	
	/**
	 * When camera retrieve image(Mat), this variable show the image(Mat) size.<p>
	 */
	protected int bufSizeH = 0;

	/**
	 * When camera retrieve image(Mat), this variable show the image(Mat) type.<p>
	 * This value is same as what OpenCV defines.<p>
	 */
	protected int bufCvFmt= 0;
	
	/**
	 * prepare and initialize camera, the instance will be hold in 'ptrCntx'.<p>
	 */
	public abstract void setup();
	
	/**
	 * just fetch image from camera, 'imgBuff' will be re-assign image data.<p>
	 */
	public abstract void fetch();
	
	/**
	 * close camera and release context.<p>
	 */
	public abstract void close();

	/**
	 * check whether bundle is valid.<p>
	 * It also means device is ready.<p> 
	 * @return TRUE or FALSE
	 */
	public boolean isReady(){
		return (ptrCntx==0)?(false):(true);
	}

	public int[] getMatInfo(){
		final int[] info = {0, 0, 0};
		info[0] = bufSizeW;
		info[1] = bufSizeH;
		info[2] = bufCvFmt;
		return info;
	}
	
	protected void setupCallback(){
		//Misc.logv("setup-callback");
		return;
	}
		
	private int countFrame=0;
	private long countTick=0, rateTick=0;
	
	protected void fetchCallback(
		long   ptrMat,
		byte[] outBuf,		
		int width, 
		int height
	){
		long tick = System.currentTimeMillis();
		if(countFrame==0){
			countTick = tick;
		}
		countFrame+=1;
		
		//adjust the speed of frame rate~~~~
		long diff = tick - rateTick;
		if(diff>=100L){
			refresh(outBuf,width,height);
			rateTick = tick;
		}
		
		if(countFrame>=10){
			countTick = tick - countTick;
			if(Application.isEventThread()==true){
				updateFPS.run();
			}else{
				Application.invokeAndWait(updateFPS);
			}
		}
	}
	private Runnable updateFPS = new Runnable(){
		@Override
		public void run() {
			int val = (countFrame*1000)/(int)countTick;
			propFPS.set(val);
			//Misc.logv("FPS=%d",val);
			countFrame = 0;//reset for next turn~~~~
		}
	};	
	//----------------------------------//
	
	private Timeline render1 = new Timeline();
	
	public void timeRender(double ms){
		if(ms<0){
			render1.pause();
			return;
		}	
		ObservableList<KeyFrame> lstKF = render1.getKeyFrames();	
		lstKF.clear();
		lstKF.add(new KeyFrame(
			Duration.millis(ms),
			event->{
				if(ctrlPlay==false){
					return;
				}				
				fetch();
			}
		));		
		render1.setCycleCount(Timeline.INDEFINITE);
		render1.play();
		ctrlPlay = true;//force to play first frames~~~~
	}	
	//----------------------------------//	
}





