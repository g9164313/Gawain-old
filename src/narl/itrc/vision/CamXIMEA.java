package narl.itrc.vision;

public class CamXIMEA extends CamBundle {

	public CamXIMEA(){		
	}
	
	public CamXIMEA(String conf){
		super(conf);
	}

	private native void implSetup(CamBundle cam);
	private native void implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);
	
	private native int implSetParamInt(CamBundle cam,String prm,int val);
	private native int implSetParamFloat(CamBundle cam,String prm,float val);
	private native int implSetParamString(CamBundle cam,String prm,String val);

	private native int implGetParamInt(CamBundle cam,String prm);
	private native float implGetParamFloat(CamBundle cam,String prm);
	private native String implGetParamString(CamBundle cam,String prm);

	public int setParamInt(String prm,int val){
		return implSetParamInt(this,prm,val);
	}
	
	public int setParamFloat(String prm,float val){
		return implSetParamFloat(this,prm,val);
	}
	
	public int setParamString(String prm,String val){
		return implSetParamString(this,prm,val);
	}
	
	public int getParamInt(String prm){
		return implGetParamInt(this,prm);
	}
	
	public float getParamFloat(String prm){
		return implGetParamFloat(this,prm);
	}
	
	public String getParamString(String prm){
		return implGetParamString(this,prm);
	}
	
	/**
	 * Device serial number.
	 */
	public String infoSN = null;
	/**
	 * Device name.
	 */
	public String infoName = null;
	/**
	 * Device instance path in operating system.
	 */
	public String infoPathInst = null;
	/**
	 * Device location path in operating system.<p>
	 * It should reflect the connection position.<p>
	 */
	public String infoPathLoca = null;
	/**
	 * Device type (1394, USB2.0, CURRERA…..).
	 */
	public String infoType = null;
	
	@Override
	public void setup() {
		implSetup(this);
	}

	@Override
	public void fetch() {
		implFetch(this);
	}

	@Override
	public long bulk(long addr) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public void close() {
		implClose(this);
	}

	private PanXIMEA pan = new PanXIMEA(this);
	
	@Override
	public void showSetting(ImgPreview1 prv) {
		pan.appear();
	}
}
