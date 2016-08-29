package narl.itrc;

import javafx.scene.Parent;

public class CamFlyCapture extends CamBundle {

	
	public CamFlyCapture(){
	}
	
	private native void implSetup(CamBundle cam);
	private native void implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);
	
	@Override
	public void setup() {
		implSetup(this);
	}

	@Override
	public void fetch() {
		implFetch(this);
	}

	@Override
	public void close() {
		implClose(this);
	}

	@Override
	public Parent genPanelSetting(PanBase pan) {
		// TODO Auto-generated method stub
		return null;
	}

}
