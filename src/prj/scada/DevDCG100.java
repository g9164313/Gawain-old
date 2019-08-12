package prj.scada;

import narl.itrc.DevTTY;

/**
 * DCG Dual 5kW
 * DC Plasma Generator
 * Support RS-232 interface
 * @author qq
 *
 */
public class DevDCG100 extends DevTTY 
	implements DevTTY.Hook 
{
	
	public DevDCG100(){
		TAG = "DevDCG-stream";
		//setHook(this);
	}

	public DevDCG100(String path_name){
		this();
		setPathName(path_name);
	}

	@Override
	public boolean open() {
		boolean flag = super.open();
		if(flag==true) {
			writeTxt("");
		}
		return flag;
	}
	
	
	private String msg = "";
	@Override
	public void looper(byte[] buf, int len) {
		msg = msg + DevTTY.buff2text(buf, len);	
		
	}	
	//-------------------------//
	
	
	
}
