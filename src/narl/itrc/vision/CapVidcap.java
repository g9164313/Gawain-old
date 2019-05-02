package narl.itrc.vision;

public class CapVidcap extends Capture {

	private native boolean implSetup();
	private native void implFetch(ImgData img);
	private native void implDone();
	
	private native void setFrameSize(int ww, int hh);
	
	public CapVidcap(){
	}
	
	private int[] frameSize = {0,0};
	
	public CapVidcap(int width, int height){
		frameSize[0] = width;
		frameSize[1] = height;
	}
	
	@Override
	public boolean setup() {
		boolean flag = implSetup();

		return flag;
	}
	@Override
	protected void afterSetup(){
		if(frameSize[0]>0 && frameSize[1]>0){
			setFrameSize(frameSize[0],frameSize[1]);
		}
	}
	
	@Override
	public void fetch(ImgData data) {
		implFetch(data);
		return;
	}
	@Override
	public void done() {
		implDone();
	}
}
