package narl.itrc;

import javafx.scene.Node;

public class CamEBus extends CamBundle {

	public CamEBus(){
	}
	
	private native void implSetup(CamBundle cam,int id,String configName);
	private native long implFetch(CamBundle cam,int id);
	private native void implClose(CamBundle cam);
	
	@Override
	public void setup(int idx, String configName) {
		implSetup(
			CamEBus.this,
			idx,
			configName
		);
	}

	@Override
	public void fetch() {
		implFetch(this,0);
	}

	@Override
	public void close() {
		implClose(this);
	}

	@Override
	public Node getPanSetting() {
		// TODO Auto-generated method stub
		return null;
	}
}
