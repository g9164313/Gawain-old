package narl.itrc.vision;

public class CapVidcap implements DevCamera.Capture {

	private long context;
	
	@Override
	public boolean setup() {
		boolean flag = implSetup();
		setFrameSize(640,480);
		return flag;
	}
	@Override
	public void fetch(DevCamera cam) {
		implFetch(cam);
	}
	@Override
	public void done() {
		implDone();
	}	
	private native boolean implSetup();
	private native void implFetch(DevCamera cam);
	private native void implDone();
	
	private native void setFrameSize(int ww, int hh);
}
