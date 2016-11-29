package narl.itrc;

import java.io.ByteArrayInputStream;

import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;

public abstract class CamBundle {

	public CamBundle(){
	}
	
	public CamBundle(String txt){
		txtConfig= txt;		
	}	
	//-------------------------//
	
	/**
	 * configuration, the meaning of value is dependent on devices.<p>
	 * Meaning of this variable is depend on device.<p> 
	 */
	public String txtConfig = "";

	/**
	 * The pointer to a context for whatever devices.<p>
	 * this pointer show whether device is ready.<p>
	 */
	private long ptrCntx = 0;
	
	/**
	 * The pointer to memory buffer created by malloc().<p>
	 * This pointer can be zero, it is dependent on device.<p>
	 */
	private long ptrBuff = 0;
	
	/**
	 * How to interpret buffer geometry.<p>
	 * It can be changed when each fetch routine.<p>
	 */
	private int bufType = 0;
	
	/**
	 * How to interpret buffer size - width.<p>
	 * It can be changed when each fetch routine.<p>
	 */
	private int bufSizeW = 0;
	
	/**
	 * How to interpret buffer size - height.<p>
	 * It can be changed when each fetch routine.<p>
	 */
	private int bufSizeH = 0;
	
	/**
	 * 'Image file' for 'ptrBuff'，data is encoded by compressor.<p>
	 * Format may be PNG, JPG or TIFF.<p>
	 */
	private byte[] imgBuff = null;
	
	/**
	 * It is also a PNG image file. Overlay with grabbed image.<p>
	 */
	private byte[] imgInfo = null;

	/**
	 * prepare and initialize camera, the instance will be keep in 'ptrCntx'.<p>
	 */
	public abstract void setup();
	
	/**
	 * Overload function, it will override configuration again.<p>
	 * @param txtConfig - pass configuration to camera. 
	 */
	public void setup(String txtConfig){
		if(txtConfig!=null){
			this.txtConfig = txtConfig;
		}		
		setup();
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
	 * @return a panel, it will be the part of control layout
	 */
	public abstract Parent genPanelSetting(PanBase pan);

	public Image getImgBuff(){ 
		return get_image(imgBuff); 
	}
	
	public Image getImgInfo(){
		return get_image(imgInfo); 
	}
	
	private Image get_image(byte[] arr){
		if(arr==null){
			return null;
		}
		return new Image(new ByteArrayInputStream(arr));
	}
	
	public void clearImgInfo(){		
		imgInfo = null;
	}
	
	public void showPanel(){		
		new PanBase("相機設定"){
			@Override
			public Parent layout() {
				Parent root = genPanelSetting(this);
				if(root==null){
					return new Label("不支援");
				}
				return root;
			}
		}.appear();
	}
	
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
	
	public native void saveImage(String name);
	
	public native void saveImageROI(String name,int[] roi);
}





