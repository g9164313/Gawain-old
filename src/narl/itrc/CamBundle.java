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
		for(int i=0; i<pinPos.length; i++){ 
			pinPos[i] = -1;
		}
	}

	@Override
	public void shutdown() {
		close();
	}	
	//-------------------------//
	
	public static final int PR_SIZE = 4;
	public static final int PIN_COLS = 4;
	public static final int ROI_COLS = 6;
	public static final int ROI_TYPE_NONE  = 0;
	public static final int ROI_TYPE_RECT  = 1;
	public static final int ROI_TYPE_CIRCLE= 2;
	
	private int infoType,infoWidth,infoHeight;//update by native code, type value is same as OpenCV
	private int[]   pinPos = new int[2*PR_SIZE];//just a Cartesian coordinates
	private float[] pinVal = new float[PIN_COLS*PR_SIZE];//support maximum 4-channel
	private int[]   roiTmp = new int[2*2];//current and diagonal position~~ 
	private int[]   roiPos = new int[ROI_COLS*PR_SIZE];//[type(4-bit),x,y,width,height,reserve]	
	private float[] roiVal = new float[ROI_COLS*PR_SIZE];//[average,deviation,minimum,maximum,mode,???]
	
	public native void markData();//this code are implemented in "utils_cv.cpp" 
	
	public int getType(){ return infoType; }
	public int getWidth(){ return infoWidth; }
	public int getHeight(){ return infoHeight; }
	
	public void getPinPos(int pinIdx,int[] pos){
		if(pinIdx>=PR_SIZE){ 
			pos[0] = pos[1] = -1;
			return;
		}
		pos[0] = pinPos[2*pinIdx+0];
		pos[1] = pinPos[2*pinIdx+1];
	}
	public void setPinPos(int pinIdx,double pos_x, double pos_y){
		if(pinIdx>=PR_SIZE){ 
			return;
		}
		pinPos[2*pinIdx+0] = (int)pos_x;
		pinPos[2*pinIdx+1] = (int)pos_y;
	}
	
	public String getPinVal(int pinIdx){
		if(pinIdx>=PR_SIZE){ 
			return "???";
		}
		int xx = pinPos[2*pinIdx+0];
		int yy = pinPos[2*pinIdx+1];
		if(xx<0 || yy<0){ 
			return ""; 
		}
		int blue = (int)pinVal[PIN_COLS*pinIdx+0];
		int green= (int)pinVal[PIN_COLS*pinIdx+1];
		int red  = (int)pinVal[PIN_COLS*pinIdx+2];
		return String.format(
			"P%d:(%03d,%03d,%03d)",
			pinIdx,red,green,blue
		);
	}
	
	public void setROI(boolean detect,double pos_x, double pos_y){
		if(0<=pos_x && pos_x<infoWidth){			
			roiTmp[0] = (int)pos_x;
		}else{
			roiTmp[0] = 0;
		}
		if(0<=pos_y && pos_y<infoHeight){ 
			roiTmp[1] = (int)pos_y;
		}else{
			roiTmp[1] = 0;
		}
		if(detect==true){
			roiTmp[2] = roiTmp[0];
			roiTmp[3] = roiTmp[1];
		}
	}

	public void fixROI(int roiIdx,int roiType){
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
		roiPos[roiIdx*ROI_COLS + 0] = roiType;
		roiPos[roiIdx*ROI_COLS + 1] = lf;
		roiPos[roiIdx*ROI_COLS + 2] = tp;
		roiPos[roiIdx*ROI_COLS + 3] = rh - lf;
		roiPos[roiIdx*ROI_COLS + 4] = bm - tp;
		//Misc.logv("ROI%d=(%d,%d)@%dx%d",roiIdx,lf,tp,rh - lf,bm - tp);
		roiTmp[0] = roiTmp[1] = roiTmp[2] = roiTmp[3] = -1;
	}
	
	public void delROI(int roiIdx){
		roiPos[roiIdx*ROI_COLS + 0] = ROI_TYPE_NONE;
	}
	//-------------------------//
	
	protected static final int PTR_SIZE = 16;
	
	private long ptrCntx = 0;//point to a container for whatever devices~~~
	private long[] ptrMatx = new long[PTR_SIZE];//point to Mat, the first is source layer, the second is	
	public SimpleBooleanProperty optEnbl = new SimpleBooleanProperty(false);
	public SimpleStringProperty msgLast = new SimpleStringProperty("");

	public native void refreshInf(CamBundle cam);//get type and size from current matrix
	public native void mapOverlay(CamBundle cam);//copy data to overlay layer
	public native void releasePtr(CamBundle cam);//release and delete all pointer~~~
	
	public abstract void setup(int idx,String txtConfig);
	public abstract void fetch();
	public abstract void close();
	
	public abstract Node getPanSetting();
	
	private Thread thrSetup;
	public void asynSetup(int idx,String txtConfig){
		if(thrSetup!=null){
			if(thrSetup.isAlive()==true){
				return;
			}
		}		
		thrSetup = new Thread(new Runnable(){
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


