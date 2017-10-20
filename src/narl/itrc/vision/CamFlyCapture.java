package narl.itrc.vision;

public class CamFlyCapture extends CamBundle {

	/**
	 * user can assign camera by serial-number or index.<p>
	 * @param conf - default is the index of camera<p> 
	 *     "1" - the first camera, one-based.<p> 
	 *     "s:12345" - match serial number.<p>
	 */
	public CamFlyCapture(String conf){
		//super(conf);
	}
	
	/**
	 * just get the first camera
	 */
	public CamFlyCapture(){
	}
	
	private native void implSetup(CamBundle cam,int index,boolean isSeral);
	private native void implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);
		
	@Override
	public void setup() {
		/*int indx = 0;
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
		
		implSetup(this,indx,flag);*/
	}

	@Override
	public void fetch() {
		implFetch(this);
	}

	@Override
	public void close() {
		implClose(this);
	}

	
	/**
	 * It is same as 'Format7ImageSettings',reference native code.<p>
	 * The first parameter is 'mode' and starts with '0'.<p>
	 * So check whether it is positive or negative to decide applying these setting.<p>
	 */
	private int[] fmt7setting = {-1,0,0,0,0,0,0};
	
	public void setROI(int x, int y, int w, int h){
		fmt7setting[0] = 0;//reset this parameter
		fmt7setting[1] = x;
		fmt7setting[2] = y;
		fmt7setting[3] = w;
		fmt7setting[4] = h;
	}
	
	private long ptrDlgCtrl = 0;
	
	private native void implShowCtrl(CamBundle cam);
}
