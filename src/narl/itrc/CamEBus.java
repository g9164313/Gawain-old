package narl.itrc;

import javafx.scene.Node;

public class CamEBus extends CamBundle {

	public CamEBus(){
	}
	
	private native void implSetup(CamBundle cam,String txtConfig);
	private native long implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);
	
	@Override
	public void setup(String txtConfig) {
		implSetup(
			CamEBus.this,
			txtConfig
		);
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
	public Node genPanelSetting() {
		return null;
	}
}
