package narl.itrc.vision;

public class CamEBus extends CamBundle {

	public CamEBus(){
	}
	
	private native void implSetup(CamBundle cam,String txtConfig);
	private native long implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);
	
	@Override
	public void setup() {
		//implSetup(CamEBus.this);
	}

	@Override
	public void fetch() {
		implFetch(this);
	}

	@Override
	public void close() {
		implClose(this);
	}
}
