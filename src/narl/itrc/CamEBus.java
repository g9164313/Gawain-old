package narl.itrc;

import javafx.scene.Parent;

public class CamEBus extends CamBundle {

	public CamEBus(){
	}
	
	class PanSetting extends PanOption {
		public PanSetting(){			
		}
		@Override
		public void slider2value(String name, int newValue) {
		}
		@Override
		public void boxcheck2value(String name, boolean newValue) {
		}
		@Override
		public void boxcombo2value(String name, int newValue, String newTitle) {
		}
		@Override
		public void boxinteger2value(String name, int newValue) {
		}
		@Override
		public Parent rootLayout() {
			return null;
		}
	};
	private PanSetting pan = null;

	@Override
	public PanBase getPanelSetting() {
		return pan;
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
}
