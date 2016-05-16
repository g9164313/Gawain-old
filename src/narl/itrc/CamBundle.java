package narl.itrc;

import java.io.ByteArrayInputStream;

import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.image.Image;

public abstract class CamBundle implements Gawain.EventHook {

	public CamBundle(){
		Gawain.hook(this);
		roiTmp[0] = roiTmp[1] = roiTmp[2] = roiTmp[3] = -1;//don't draw it~~~
	}

	@Override
	public void shutdown() {
		close();
	}	
	//-------------------------//
	
	//these will 
	public static final int ROI_SIZE = 4;
	public static final int ROI_COLS = 6;
	//[channel:4bit][stroke:16bit][shape:8bit]
	public static final int ROI_TYPE_NONE = 0x000000;
	public static final int ROI_TYPE_PIN  = 0x000001;
	public static final int ROI_TYPE_RECT = 0x000002;
	public static final int ROI_TYPE_CIRC = 0x000003;
	
	private int infoType,infoWidth,infoHeight;//update by native code, type value is same as OpenCV
	
	private int[]   roiTmp = new int[2*2];//current and diagonal position~~ 
	/**
	 * How to treat ROI structure<p>
	 * Each column means [type(4-bit),x,y,width,height,???]<p>
	 * Type is bit-base<p>
	 * bit0~7:shape, bit8~15:stroke size, bit16~19:which channel<p>
	 */
	private int[]   roiPos = new int[ROI_SIZE*ROI_COLS];//
	/**
	 * keep the pixel value or the statistics value of ROI<p>
	 * When type is Pin, it mean four channel pixel value<p>
	 * Otherwise it means [average,deviation,minimum,maximum,mode,???]<p>
	 */
	private float[] roiVal = new float[ROI_SIZE*ROI_COLS];
	
	public native void markData();//this code are implemented in "utils_cv.cpp" 
	
	public int getType(){ return infoType; }
	public int getWidth(){ return infoWidth; }
	public int getHeight(){ return infoHeight; }

	public void stickPin(boolean fistPin,double pos_x, double pos_y){
		roiTmp[0] = roiTmp[1] = -1;
		if(0<=pos_x && pos_x<infoWidth){			
			roiTmp[0] = (int)pos_x;
		}
		if(0<=pos_y && pos_y<infoHeight){ 
			roiTmp[1] = (int)pos_y;
		}
		if(fistPin==true){
			roiTmp[2] = roiTmp[0];
			roiTmp[3] = roiTmp[1];			
		}
	}
	
	public void fixPin(int idx,double pos_x, double pos_y){
		//it is a special case
		roiPos[idx*ROI_COLS + 0] = ROI_TYPE_PIN;
		roiPos[idx*ROI_COLS + 1] = (int)pos_x;
		roiPos[idx*ROI_COLS + 2] = (int)pos_y;
		roiPos[idx*ROI_COLS + 3] = 1;
		roiPos[idx*ROI_COLS + 4] = 1;
	}
	
	public void fixROI(int idx,int type){
		int lf,rh,tp,bm;
		if(roiTmp[0]<roiTmp[2]){
			lf = roiTmp[0]; 
			rh = roiTmp[2];
		}else{
			lf = roiTmp[2]; 
			rh = roiTmp[0];
		}
		if(roiTmp[1]<roiTmp[3]){
			tp = roiTmp[1]; 
			bm = roiTmp[3];
		}else{
			tp = roiTmp[3]; 
			bm = roiTmp[1];
		}
		roiPos[idx*ROI_COLS + 0] = type;
		roiPos[idx*ROI_COLS + 1] = lf;
		roiPos[idx*ROI_COLS + 2] = tp;
		roiPos[idx*ROI_COLS + 3] = rh - lf;
		roiPos[idx*ROI_COLS + 4] = bm - tp;
		//Misc.logv("ROI%d=(%d,%d)@%dx%d",roiIdx,lf,tp,rh - lf,bm - tp);
		roiTmp[0] = roiTmp[1] = roiTmp[2] = roiTmp[3] = -1;
	}
	
	public void delMark(int idx){
		roiPos[idx*ROI_COLS + 0] = ROI_TYPE_NONE;
	}

	public boolean getPin(int idx,int[] pos){
		if(idx>=ROI_SIZE){
			return false;
		}
		if(roiPos[idx*ROI_COLS + 0]!=ROI_TYPE_PIN){
			return false;
		}
		pos[0] = roiPos[idx*ROI_COLS + 1];
		pos[1] = roiPos[idx*ROI_COLS + 2];
		return true;
	}
	
	public boolean getROI(int idx,int[] pos){
		if(idx>=ROI_SIZE){
			return false;
		}
		if(
			roiPos[idx*ROI_COLS + 0]==ROI_TYPE_NONE ||
			roiPos[idx*ROI_COLS + 0]==ROI_TYPE_PIN
		){
			return false;
		}
		pos[0] = roiPos[idx*ROI_COLS + 1];
		pos[1] = roiPos[idx*ROI_COLS + 2];
		pos[2] = roiPos[idx*ROI_COLS + 3];
		pos[3] = roiPos[idx*ROI_COLS + 4];
		return true;
	}
	
	public void resetMark(){
		
		roiTmp[0] = roiTmp[1] = roiTmp[2] = roiTmp[3] = -1;
		
		for(int idx=0; idx<ROI_SIZE; idx++){
			
			roiPos[idx*ROI_COLS + 0] = 0;
						
			String txt = Gawain.prop.getProperty("imgROI"+idx,"").trim();
			if(txt.length()!=0){
				
				int pos[]={0,0,0,0};
				
				if(Misc.trimPosition(txt,pos)==true){
					
					roiPos[idx*ROI_COLS + 0] = ROI_TYPE_PIN;
					roiPos[idx*ROI_COLS + 1] = pos[0];
					roiPos[idx*ROI_COLS + 2] = pos[1];
					roiPos[idx*ROI_COLS + 3] = 1;
					roiPos[idx*ROI_COLS + 4] = 1;
					
				}else if(Misc.trimRectangle(txt,pos)==true){
					
					roiPos[idx*ROI_COLS + 0] = ROI_TYPE_RECT;
					roiPos[idx*ROI_COLS + 1] = pos[0];
					roiPos[idx*ROI_COLS + 2] = pos[1];
					roiPos[idx*ROI_COLS + 3] = pos[2];
					roiPos[idx*ROI_COLS + 4] = pos[3];
				}
			}			
		}
	}
	//-------------------------//
	
	protected static final int PTR_SIZE = 16;
		
	private long ptrCntx = 0;//point to a container for whatever devices~~~
	private long[] ptrMatx = new long[PTR_SIZE];//point to Mat, the first is source layer, the second is	
	
	public int optIndex = -1;
	public String optConfig = "";
	public SimpleBooleanProperty optEnbl = new SimpleBooleanProperty(false);
	public SimpleStringProperty msgLast = new SimpleStringProperty("");

	/**
	 * get type and size from current matrix.
	 * @param cam - pass self
	 */
	public native void refreshInf(CamBundle cam);
	
	/**
	 * copy data to overlay layer.
	 * @param cam - pass self
	 */
	public native void mapOverlay(CamBundle cam);
	
	/**
	 * release and delete all pointer~~~
	 * @param cam - pass self
	 */
	public native void releasePtr(CamBundle cam);
	
	/**
	 * prepare and initialize camera, the instance will be keep in ptrCntx
	 * @param idx - camera index, -1 mean auto-selection
	 * @param txtConfig - pass configuration to camera. 
	 *   The definition is dependent on camera type. 
	 *   When no configuration, it must be "zero length string"....
	 */
	public abstract void setup(int idx,String txtConfig);
	
	/**
	 * just fetch image from camera
	 */
	public abstract void fetch();
	
	/**
	 * close camera and release everything~~~
	 */
	public abstract void close();
	
	/**
	 * generate a panel to control camera options
	 * @return a panel, it will be one part of TabPane
	 */
	public abstract Node getPanSetting();
	
	public void syncSetup(){
		setup(optIndex,optConfig);
	}
	
	private Thread thdSetup;
	public void asynSetup(int idx,String txtConfig){
		if(thdSetup!=null){
			if(thdSetup.isAlive()==true){
				return;
			}
		}		
		thdSetup = new Thread(new Runnable(){
			@Override
			public void run() {
				setup(idx,txtConfig);
			}
		},"cam-setup");
	}
	
	protected void updateOptEnbl(boolean val){
		if(Application.isEventThread()==false){
			if(Application.GetApplication()==null){
				return;
			}
			Application.invokeAndWait(new Runnable(){
				@Override
				public void run() { 
					optEnbl.set(val);
				}
			});
		}else{
			optEnbl.set(val);
		}
	}
	
	protected void updateMsgLast(String txt){
		if(Application.isEventThread()==false){
			if(Application.GetApplication()==null){
				return;
			}
			Application.invokeAndWait(new Runnable(){
				@Override
				public void run() { 
					msgLast.set(txt);
				}
			});
		}else{
			msgLast.set(txt);
		}
	}
	//-------------------------//

	public long getCntx(){ 
		return ptrCntx;
	}	
	public long getMatSrc(){ 
		return getMatx(0);
	}
	public long getMatOva(){ 
		return getMatx(1);
	}
	public long getMatx(int idx){
		if(idx>=ptrMatx.length){
			return 0;
		}
		return ptrMatx[idx];
	}	
	protected void setMatx(int idx,long ptr){
		if(idx>=ptrMatx.length){ 
			return;
		}
		if(ptrMatx[idx]!=0){
			Misc.imRelease(ptrMatx[idx]);
		}
		ptrMatx[idx] = ptr;
		if(idx==0){
			refreshInf(this);//always update iunformation again~~~
		}
	}
	
	public Image getImage(){
		return getImage(0);
	}
	public Image getImage(int idx){
		if(idx>=ptrMatx.length){
			return null;
		}
		byte[] dat = getData(ptrMatx[idx]);
		if(dat==null){
			return null;
		}
		return new Image(new ByteArrayInputStream(dat));
	}
	private native byte[] getData(long ptr);//this code are implemented in "utils_cv.cpp"
}


