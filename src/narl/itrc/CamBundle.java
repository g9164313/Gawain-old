package narl.itrc;

import java.io.ByteArrayInputStream;

import com.sun.glass.ui.Application;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public abstract class CamBundle implements Gawain.EventHook {

	public CamBundle(){
		Gawain.hook(this);		
	}

	@Override
	public void shutdown() {
		close();
	}
	
	public void showPanel(Window owner){
		PanBase pan = getPanelSetting();
		if(pan==null){
			return;
		}
		Stage stg = new Stage(StageStyle.UNIFIED);
		stg.initModality(Modality.NONE); 
		stg.setResizable(false);
		stg.centerOnScreen();
		stg.setTitle("設定相機參數");
		stg.initOwner(owner);
		pan.appear(stg);
	}
	public abstract PanBase getPanelSetting();

	private Thread thrSetup;
	public void asynSetup(int idx, String configName){
		if(thrSetup!=null){
			if(thrSetup.isAlive()==true){
				return;
			}
		}		
		thrSetup = new Thread(new Runnable(){
			@Override
			public void run() {
				setup(idx,configName);
			}
		},"cam-setup");
	}
	
	public static final int ROI_COLS = 6;
	public static final int ROI_SIZE = 4;
	public static final int ROI_TYPE_NONE  = 0;
	public static final int ROI_TYPE_RECT  = 1;
	public static final int ROI_TYPE_CIRCLE= 2;
	
	private int infoType,infoWidth,infoHeight;//update by native code, type value is same as OpenCV
	private int[] curPos={0,0, -1,-1};//cursor and tick~~~
	private float[] curVal={0.f,0.f,0.f,0.f};//update by native code, support 4-channels	
	private int[] roiVal = new int[ROI_COLS*ROI_SIZE];//[type(1),left-top(2),right-bottom(2),reserve(1)]	
	
	private long ptrCntx = 0;//point to a container for whatever devices~~~
	private long[] ptrMatx = new long[16];//point to Mat, the first is source layer, the second is	
	public SimpleBooleanProperty optEnbl = new SimpleBooleanProperty(false);
	public SimpleStringProperty msgLast = new SimpleStringProperty("");

	public abstract void setup(int idx,String configName);
	public abstract void fetch();
	public abstract void close();
	
	public void updateOptEnbl(boolean val){		
		if(Application.GetApplication()==null){
			//This happened when application closes looper,
			//but we still need to update the information 
			optEnbl.set(val);
		}else{
			//callback by native instance~~
			final Runnable event = new Runnable(){
				@Override
				public void run() {
					optEnbl.set(val);
				}
			}; 
			Application.invokeAndWait(event);
		}		
	}
	
	public void updateMsgLast(String txt){		
		if(Application.GetApplication()==null){
			//This happened when application closes looper,
			//but we still need to update the information 
			msgLast.set(txt);
		}else{
			//callback by native instance~~
			final Runnable event = new Runnable(){
				@Override
				public void run() {
					msgLast.set(txt);
				}
			};
			Application.invokeAndWait(event);
		}
	}
	
	public void setCursor(double pos_x, double pos_y){
		if(0<=pos_x && pos_x<infoWidth){
			curPos[0] = (int)pos_x;
		}
		if(0<=pos_y && pos_y<infoHeight){
			curPos[1] = (int)pos_y;
		}
	}
	
	public void setTick0(){
		curPos[2] = curPos[0];
		curPos[3] = curPos[1];
	}
	
	public void setTick1(int roiIdx,int roiType){
		int lf,rh,tp,bm;
		if(curPos[0]<curPos[2]){
			lf = curPos[0]; 
			rh = curPos[2];
		}else{
			lf = curPos[2]; 
			rh = curPos[0];
		}
		if(curPos[1]<curPos[3]){
			tp = curPos[1]; 
			bm = curPos[3];
		}else{
			tp = curPos[3]; 
			bm = curPos[1];
		}
		roiVal[roiIdx*ROI_COLS + 0] = roiType;
		roiVal[roiIdx*ROI_COLS + 1] = lf;
		roiVal[roiIdx*ROI_COLS + 2] = tp;
		roiVal[roiIdx*ROI_COLS + 3] = rh - lf;
		roiVal[roiIdx*ROI_COLS + 4] = bm - tp;
		curPos[2] = curPos[3] = -1;//reset tick~~
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
	
	private native byte[] getData(long ptr);
	
	public native void markData();
}


