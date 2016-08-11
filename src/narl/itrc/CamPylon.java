package narl.itrc;

import javafx.scene.Parent;

public class CamPylon extends CamBundle {

	public CamPylon(){
	}
	
	private native long getExposure(CamBundle cam,long[] inf);//current,minimum,maximum,increment
	private native void setExposure(CamBundle cam,long val);
	
	private native long getGain(CamBundle cam,long[] inf);//current,minimum,maximum,increment
	private native void setGain(CamBundle cam,long val);
		
	private native void implSetup(CamBundle cam,String configName);
	private native long implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);
	
	@Override
	public void setup() {
		implSetup(
			CamPylon.this,
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

	/*class PanSetting extends PanOption {
		private final String t1 = "曝光值";
		private final String t2 = "增益值";
		public PanSetting(){
			long[] info = {0L,0L,0L,0L};
			getExposure(CamPylon.this,info);
			addBoxInteger(t1,
				(int)info[1],
				(int)info[2],
				(int)info[0]
			);
			getGain(CamPylon.this,info);
			addBoxInteger(t2,
				(int)info[1],
				(int)info[2],
				(int)info[0]
			);
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
			if(name.equals(t1)==true){
				setExposure(CamPylon.this,newValue);
			}else if(name.equals(t2)==true){
				setGain(CamPylon.this,newValue);
			}
		}
		@Override
		public Parent rootLayout() {
			return null;
		}
	};*/
	@Override
	public Parent genPanelSetting(PanBase pan) {
		return null;
	}
}
