package narl.itrc;

import java.io.ByteArrayInputStream;

import javafx.scene.Node;
import javafx.scene.image.Image;

public abstract class CamBundle implements Gawain.EventHook {

	public CamBundle(){
		Gawain.hook(this);
	}
	
	public CamBundle(String txt){
		Gawain.hook(this);
		txtConfig= txt;		
	}
	
	@Override
	public void shutdown() {
		close();
	}	
	//-------------------------//
	
	/**
	 * the pointer to a context for whatever devices.<p>
	 * this pointer show whether device is ready.<p>
	 */
	private long ptrCntx = 0;
	
	/**
	 * the pointer to OpenCV Mat
	 */
	private long ptrMatx = 0;
	
	/**
	 * configuration, the meaning of value is dependent on devices
	 */
	public String txtConfig = "";

	/**
	 * prepare and initialize camera, the instance will be keep in 'ptrCntx'
	 * @param txtConfig - pass configuration to camera. 
	 *   The definition is dependent on camera type. 
	 *   When no configuration, it must be "zero length string"....
	 */
	public abstract void setup(String txtConfig);
	
	public void setup(){
		setup(txtConfig);
	}
	
	/**
	 * just fetch image from camera
	 */
	public abstract void fetch();
	
	/**
	 * close camera and release everything.<p>
	 * remember 'ptrCntx' will be overwrite.<p>
	 */
	public abstract void close();
	
	/**
	 * generate a panel to control camera options
	 * @return a panel, it will be one part of TabPane
	 */
	public abstract Node genPanelSetting();
	
	public void syncSetup(){
		setup(txtConfig);
	}
	
	private Thread thdSetup;
	public void asynSetup(String txtConfig){
		if(thdSetup!=null){
			if(thdSetup.isAlive()==true){
				return;
			}
		}		
		thdSetup = new Thread(new Runnable(){
			@Override
			public void run() {
				setup(txtConfig);
			}
		},"cam-setup");
	}
	
	public boolean isReady(){
		return (ptrCntx==0)?(false):(true);
	}

	public Image getImage(){
		if(ptrMatx==0){
			return null;
		}
		byte[] dat = getData();
		if(dat==null){
			return null;
		}
		return new Image(new ByteArrayInputStream(dat));
	}
	private native byte[] getData();
	
	public native void saveImage(String name);
}


