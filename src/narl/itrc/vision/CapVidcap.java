package narl.itrc.vision;

public class CapVidcap extends Capture {

	private native boolean implSetup();
	private native void implFetch(ImgFilm img);
	private native void implDone();
	
	private native void setFrameSize(int ww, int hh);
	private native void setProperty(int ctrl, double val);
	private native double getProperty(int ctrl);
	
	public CapVidcap(){
	}
	
	private int[] frameSize = {0,0};
	
	public CapVidcap(int width, int height){
		frameSize[0] = width;
		frameSize[1] = height;
	}
	
	@Override
	public boolean setup() {
		return implSetup();
	}
	@Override
	protected void afterSetup(){
		if(frameSize[0]>0 && frameSize[1]>0){
			setFrameSize(frameSize[0],frameSize[1]);
			//setProperty(10, 0.5);//CAP_PROP_BRIGHTNESS=0.502
			setProperty(11, 0.09);//CAP_PROP_CONTRAST=0.1255
			setProperty(12, 0.14);//CAP_PROP_SATURATION=0.1255
			//setProperty(13, 0.8);//CAP_PROP_HUE=-1
			//setProperty(14, 1);//CAP_PROP_GAIN=0.2510
			//setProperty(15, 0.8);//CAP_PROP_EXPOSURE=0.0797
			setProperty(44, 0.);//CAP_PROP_AUTO_WB=1
			//setProperty(45, 0.8);//CAP_PROP_WB_TEMPERATURE=6148
			//for(int i=43; i<46; i++) {
			//	Misc.logv("VID.prop[%2d] = %.4f", i, getProperty(i));
			//}			
		}
	}
	
	@Override
	public void fetch(ImgFilm data) {
		implFetch(data);
		return;
	}
	@Override
	public void done() {
		implDone();
	}
}
