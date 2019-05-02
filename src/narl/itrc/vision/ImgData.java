package narl.itrc.vision;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * Pack OpenCV information
 * @author qq
 *
 */
public class ImgData {
	
	public int snap = 1;
	
	private int cvWidth = 0;	
	private int cvHeight= 0;		
	private int cvType  = 0;
	private byte[] pool=null;//pure image data
	private byte[] mior=null;//mirror data
	private byte[] over=null;//overlay view
	
	private WritableImage[] view = { null, null };
	
	public final byte[] mesg = new byte[1024];//pipe response message
	
	public boolean isValid(){
		if(
			pool==null ||
			cvWidth ==0||
			cvHeight==0||
			cvType==0
		){
			return false;
		}
		return true;
	}
	
	
	public void clear(int count){
		snap = count;
		pool = null;
		mior = null;
		over = null;
	}
	
	/**
	 * this function is invoked by native code
	 * @param total - image data size
	 * @param width - image width
	 * @param height- image height
	 */
	private void requestPool(int total, int width, int height){
		pool = new byte[snap * total];//raw data
		mior = new byte[width * height * 3];//format is RGB
		over = new byte[width * height * 4];//format is ARGB
		view[0] = new WritableImage(width,height);
		view[1] = new WritableImage(width,height);
	}
	
	public Image[] getImage(){
		reflector(pool, mior);
		view[0].getPixelWriter().setPixels(
			0, 0, 
			cvWidth, cvHeight, 
			PixelFormat.getByteRgbInstance(),
			mior, 
			0, cvWidth*3
		);//mirror for data in pool
		view[1].getPixelWriter().setPixels(
			0, 0, 
			cvWidth, cvHeight, 
			PixelFormat.getByteBgraInstance(),
			over, 
			0, cvWidth*4
		);//mirror for data in overlay
		return view;
	}
	
	private native void reflector(byte[] src, byte[] dst);
}
