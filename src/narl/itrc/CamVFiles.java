package narl.itrc;

import javafx.scene.Node;
import javafx.scene.control.Label;

public class CamVFiles extends CamBundle {

	public CamVFiles(){		
	}

	@Override
	public PanBase getPanelSetting() {
		return null;
	}

	public native void mapOverlay(CamBundle cam);//copy data to overlay layer
	public native void updateInfo(CamBundle cam);//copy data to overlay layer
	
	@Override
	public void setup(int idx, String txtConfig) {
		if(txtConfig==null){
			//create dummy layers...
			setMatx(0,Misc.imCreate(640,480,CvType.CV_8UC3));
		}else{
			setMatx(0,Misc.imRead(txtConfig));
		}
		updateInfo(this);
		updateOptEnbl(true);//it always success!!!
		updateMsgLast("open virtual file");
	}

	@Override
	public void fetch() {
		updateInfo(this);
		mapOverlay(this);
	}

	@Override
	public void close() {
		//release all data!!!
		for(int i=0; i<PTR_SIZE; i++){
			long ptr = getMatx(i);
			if(ptr!=0){
				Misc.imRelease(ptr);
			}
			setMatx(i,0L);
		}
		updateOptEnbl(false);//it always success!!!
		updateMsgLast("close virtual file");
	}
}
