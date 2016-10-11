package narl.itrc;

import javafx.scene.Parent;

public class CamFlyCapture extends CamBundle {

	/**
	 * user can assign camera by serial-number or index.<p>
	 * @param conf - default is the index of camera<p> 
	 *     "1" - the first camera, one-based.<p> 
	 *     "s:12345" - match serial number.<p>
	 */
	public CamFlyCapture(String conf){
		super(conf);
	}
	
	/**
	 * just get the first camera
	 */
	public CamFlyCapture(){
		super("");
	}
	
	private native void implSetup(CamBundle cam,int index,boolean isSeral);
	private native void implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);
	
	private native void implShowCtrl(CamBundle cam);
	
	@Override
	public void setup() {
		int indx = 0;
		boolean flag = false;
		
		String conf = txtConfig;
		if(conf.length()!=0){
			if(conf.startsWith("s:")){
				flag = true;
				conf = conf.substring(2);
				try{
					indx = Integer.valueOf(conf);//this is serial-number
				}catch(NumberFormatException e){
					indx = 0;
					flag = false;
				}
			}else{
				//index is zero-based!!!
				try{
					indx = Integer.valueOf(txtConfig) - 1;
					if(indx<0){
						indx = 0;//reset it~~
					}
				}catch(NumberFormatException e){
					indx = 0;
				}
			}
		}
		
		implSetup(this,indx,flag);
	}

	@Override
	public void fetch() {
		implFetch(this);
	}

	@Override
	public void close() {
		implClose(this);
	}

	private long ptrDlgCtrl = 0;
	
	@Override
	public Parent genPanelSetting(PanBase pan) {
		implShowCtrl(this);
		return null;
	}

}
