package narl.itrc;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import com.sun.glass.ui.Application;

public class DevGrabber implements 
	Gawain.EventHook, Runnable
{
	//public static final int SRC_NUL_DIR = 0x1;
	//public static final int SRC_NUL_BIN = 0x2;
	public static final int SRC_NUL_MEM = 0x3;
	public static final int SRC_DEV_VIDCAP= 0x80001;
	public static final int SRC_DEV_EBUS  = 0x80002;
	
	public interface EventHook {
		void notifyImage(long pDat,long pLay);
	} 
	protected EventHook devHook = null;
		
	public DevGrabber(){
		Gawain.hook(this);
		init("grabber",SRC_NUL_MEM,null);//default~~~~
	}
	public DevGrabber(String name){		
		Gawain.hook(this);
		init(name,SRC_NUL_MEM,null);
	}
	public DevGrabber(int type){
		Gawain.hook(this);
		init("grabber",type,null);
	}
	public DevGrabber(EventHook event){
		Gawain.hook(this);
		init("grabber",SRC_NUL_MEM,event);
	}
	public DevGrabber(String name,int type){		
		Gawain.hook(this);
		init(name,type,null);
	}
	public DevGrabber(String name,int type,EventHook event){		
		Gawain.hook(this);
		init(name,type,event);
	}
	
	private void init(String name,int type,EventHook event){
		wndName=name;
		devType=type;
		devHook=event;
		optPreview.addListener(eventPreview);
		optEnable.addListener(eventEnable);
	}
	
	private ChangeListener<Boolean> eventPreview = new ChangeListener<Boolean>(){
		@Override
		public void changed(
			ObservableValue<? extends Boolean> observable,
			Boolean oldValue, Boolean newValue
		) {
			if(newValue==false){
				//we don't need preview so, close it~~~
				Misc.destroyWindow(wndName);
			}
		}
	};
	
	private ChangeListener<Boolean> eventEnable = new ChangeListener<Boolean>(){
		@Override
		public void changed(
			ObservableValue<? extends Boolean> observable,
			Boolean oldValue, Boolean newValue
		) {
			if(newValue==true){
				launch();
			}else{
				finish();
			}
			//Misc.logv("event#"+oldValue+"-->"+newValue);
		}
	};
	
	protected void eventShutdown(){ }
	
	@Override
	public void shutdown() {
		finish();//this will be invoked when application is closed~~~
		eventShutdown();
	}
	
	//The variables below line is modified by native code 
	public int     devType =SRC_NUL_MEM;
	public String  wndName ="grabber";
	public String  cfgName ="";//some device support configure file
	public String  lastMsg ="";	
	public String  pipeMsg ="";
	private long frameIdx;

	public long    optTapeIndx=0L;//count how many images in this tape
	public long    optTapeSize=0L;//the maximum count of image data
	public String  optTapeName="";//prefix of tape name
	public String  optTapeAppx="";//post-fix of tape name
	public String  optTapeInfo="";//appendix for tape
	public double  optFPS=0.;
	
	public SimpleBooleanProperty optPreview= new SimpleBooleanProperty(false);
	public boolean optExit = true;
	public SimpleBooleanProperty optEnable= new SimpleBooleanProperty(false);
	
	private Thread looper=null;	
	@Override
	public void run() {		
		frameIdx=0;
		graberStart();
		switch(devType){
		//case SRC_NUL_DIR://Do we need to continues this??
		//	break;
		//case SRC_NUL_BIN://Do we need to continues this??
		//	break;
		case SRC_NUL_MEM:
			looperNulMem(wndName);
			break;
		case SRC_DEV_VIDCAP: 
			looperVidcap(0); 
			break;
		case SRC_DEV_EBUS:
			looperEBus(0);
			break;
		}		
		Misc.destroyWindow(wndName);
		Misc.logv("[%s] lastMessage=%s",wndName,lastMsg);
	}
	private native void looperNulMem(String name);
	private native void looperVidcap(int vid);
	private native void looperEBus(int vid);
	//-------------------------//
	
	protected void graberStart(){ return; }//user can override this~~~
	protected void graberImage(long pDat,long pLay){ return; }//user can override this~~~
	
	private long ptrOverlay;
	private final Runnable renderEvent = new Runnable(){
		@Override
		public void run() {
			if(optExit==true){
				return;
			}
			Misc.renderWindow(wndName,ptrOverlay);
		}
	};
	private void looperCallback(long pDat,long pLay) throws InterruptedException{
		//this will be invoked by the native looper~~~~
		//Misc.logv("fps=%.1f",optFPS);
		if(optExit==true){
			return;
		}
		if(devHook!=null){
			devHook.notifyImage(pDat,pLay);
		}else{
			graberImage(pDat,pLay);			
		}
		if(optPreview.get()==true){
			ptrOverlay = pLay;
			Application.invokeAndWait(renderEvent);			
		}
	}
	//-------------------------//
	
	public void launch(){
		if(isLive()==true){
			return;
		}
		lastMsg="";//reset it~~~
		optExit=false;
		looper = new Thread(this,wndName);
		looper.start();		
	}
	public void finish(){
		if(looper==null){
			return;
		}		
		synchronized(looper){
			//trick~~~~~
			optExit=true;
			Misc.delay(100);
			looper.interrupt();
			looper = null;				
		}
	}
	
	public boolean isHardware(){
		int flag = devType & 0x80000;
		if(flag==0){
			return false;
		}
		return true;
	}
	
	public boolean isLive(){
		if(looper!=null){
			return looper.isAlive();
		}
		return false;
	}
	
	public boolean isTaping(){
		if(optTapeSize!=0){
			return true;
		}
		return false;
	}
	public void tapeFile(String name){
		_tape(name,1);
	}
	public void tapeFile(String name,long cnt){
		_tape(name,cnt);
	}
	public void tapeBinary(String name){
		_tape(name,-1);
	}
	public void tapeBinary(String name,long cnt){
		_tape(name,cnt);
	}
	private void _tape(String txt,long cnt){
		if(isLive()==false){
			Misc.logw("Grabber is death");
			return;
		}
		if(txt.length()==0){
			//no name, so we just stop recording~~~
			optTapeSize = 0;
			Misc.delay(100);
			return;
		}		
		int pos = txt.lastIndexOf('.');		
		optTapeName = txt.substring(0,pos);
		optTapeAppx = txt.substring(pos);
		if(optTapeAppx.equalsIgnoreCase(".bin")==true){
			optTapeInfo = optTapeName+".txt";
		}
		optTapeIndx = 0;
		optTapeSize = cnt;//finally,kick it~~~
	}
	
	public void fileFWD(){
		if(isHardware()==true||isLive()==false){
			return;
		}
		frameIdx++;
		looper.interrupt();
	}
	
	public void fileBAK(){
		if(isHardware()==true||isLive()==false){
			return;
		}
		frameIdx--;
		if(frameIdx<0){
			frameIdx = 0;
		}
		looper.interrupt();
	}
}
