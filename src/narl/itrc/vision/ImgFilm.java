package narl.itrc.vision;

import java.util.Arrays;

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * Pack OpenCV information
 * @author qq
 *
 */
public class ImgFilm {
	
	private int snap = 1;
	private int cvWidth = 0;	
	private int cvHeight= 0;		
	private int cvType  = 0;
	public byte[] pool=null;//pure image data	
	private byte[] mior=null;//mirror data
	private byte[] over=null;//overlay view
	
	private WritableImage[] view = { null, null };
	
	public Image refMask = null; 
	
	private byte[] bufMask = new byte[0];
	
	public void mirrorMask() {
		if(refMask==null) {
			return;
		}
		int len = cvWidth * cvHeight * 4;
		if(bufMask.length!=len) {
			bufMask = new byte[len];
		}
		refMask.getPixelReader().getPixels(
			0, 0,
			cvWidth, cvHeight,
			PixelFormat.getByteBgraInstance(),
			bufMask, 
			0, cvWidth*4
		);
	}
	
	/**
	 * This variables are accessed by native code.<p>
	 */
	private final int[] refMark = new int[16*4];
	
	public void setMark(final ImgPane vew) {
	
		ImgPane.Mark[] lst = vew.getAllMark();
	
		for(int i=0; i<lst.length; i++) {
		
			ImgPane.Mark mm = lst[i];
		
			if(mm.isUsed==true) {
				refMark[i*4+0] = mm.locaX;
				refMark[i*4+1] = mm.locaY;
				refMark[i*4+2] = mm.sizeW;
				refMark[i*4+3] = mm.sizeH;
			} else {
				refMark[i*4+0] = -1;
				refMark[i*4+1] = -1;
				refMark[i*4+2] = 0;
				refMark[i*4+3] = 0;
			}
		}
	}
	
	public int getWidth() {
		return cvWidth;
	}
	
	public int getHeight() {
		return cvHeight;
	}
	
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
	
	
	public ImgFilm setSnapCount(final int cnt){
		if(snap!=cnt){
			reset(cnt);
		}
		return this;
	}
	
	public void reset(int cnt){
		snap = cnt;
		pool = null;
		mior = null;
		over = null;
	}
	
	/**
	 * this function is invoked by native code.<p>
	 * For native code, overlay will be BGRA.<p>
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
	
	/**
	 * get javafx image object from capture device.<p>
	 * @return - image object
	 */
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
	
	public void mirrorPool() {
		reflector(pool, mior);
	}
	
	public Image[] mirrorImg() {
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
	
	public void clearOverlay() {		
		Arrays.fill(over, (byte)0);		
		view[1].getPixelWriter().setPixels(
			0, 0, 
			cvWidth, cvHeight, 
			PixelFormat.getByteBgraInstance(),
			over, 
			0, cvWidth*4
		);
	}
	
	private native void reflector(byte[] src, byte[] dst);
}
