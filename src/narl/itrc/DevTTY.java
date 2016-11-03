package narl.itrc;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javafx.beans.property.SimpleStringProperty;

public class DevTTY implements Gawain.EventHook {

	private final String TXT_NONE  = "--------";
	private final String TXT_UNKNOW= "????";
	
	public DevTTY(){
		Gawain.hook(this);
		//how to open a default terminal?
	}
	
	public DevTTY(String txt){		
		Gawain.hook(this);
		open(txt);
	}
	
	@Override
	public void release() {
		close();
	}
	
	@Override
	public void shutdown() {
		close();
	}
	
	private long handle = 0L;
	private boolean sync0= true;//This is tri_state variable, default is block mode
	private boolean sync1= true;//This is tri_state variable
	
	public SimpleStringProperty ctrlName = new SimpleStringProperty(TXT_NONE);
	
	public String getCtrlName(){
		return ctrlName.get();
	}
	
	public void setName(String txt){
		open(txt);
	}
	
	public void setSync(boolean flag){
		sync0 = flag;
	}
	
	public boolean isLive(){
		if(handle==0){
			return false;
		}
		return true;
	}
	
	private String infoName=TXT_UNKNOW;
	private int  infoBaud = -1;
	private char infoData = '?';
	private char infoPart = '?';
	private char infoStop = '?';
	
	public String getName(){
		return infoName;
	}
	public String getBaud(){
		return String.valueOf(infoBaud);
	}
	public String getDataBit(){
		return String.valueOf(infoData);
	}
	public String getParity(){
		return String.valueOf(infoPart);
	}
	public String getStopBit(){
		return String.valueOf(infoStop);
	}
	private void resetInfo(){
		ctrlName.setValue(TXT_NONE);
		infoName = TXT_UNKNOW;
		infoBaud = -1;
		infoData = '?';
		infoPart = '?';
		infoStop = '?';
	}
	
	/**
	 * Parse the control statement like "/dev/ttyS0,9600,8n1".<p>
	 * The control statement is composed of baud-rate,data-bit,parity,stop-bit.<p>
	 * Parity can be 'n'(none),'o'(odd),'e'(event),'m'(mask) and 's'(space).<p>
	 * @param txt - control statement.
	 */
	public void open(String txt){
		//reset the previous connection!!!
		resetInfo();
		close();
		String[] arg = txt.trim().split(",");
		//check we have 3 arguments at least
		if(arg.length<3){
			Misc.logw("fail to connect "+txt);
			return;
		}
		//check the fist argument is device name
		infoName = arg[0];
		if(Misc.isPOSIX()==true){
			if(new File(infoName).exists()==false){
				Misc.logw("No device --> "+txt);
				return;
			}
		}else{
			//how to check whether fxxking windows system has terminal
		}
		//check the second argument is integer
		try{
			infoBaud = Integer.valueOf(arg[1]);
		}catch(NumberFormatException e){
			Misc.loge("error baud : "+arg[1]);
			return;
		}
		char[] ctrl = arg[2].toCharArray();
		infoData = ctrl[0];
		infoPart = ctrl[1];
		infoStop = ctrl[2];
		implOpen(
			infoName,
			infoBaud,
			infoData,
			infoPart,
			infoStop,
			'?'
		);		
		if(handle!=0){ 
			ctrlName.setValue(txt);//we success!!!
			Misc.logv("connect to "+txt);
		}
	}

	public byte[] readBuf(){
		return implRead();
	}
	
	public byte[] readBuf(boolean sync){
		sync0 = sync;
		return readBuf();
	}
		
	public void writeBuf(byte[] buf){
		implWrite(buf);
	}
	
	/**
	 * Read text from terminal.<p>
	 * @return NULL - if no data<p>
	 * Text - what we got.<p>
	 */
	public String readTxt(){
		byte[] buf = readBuf();
		if(buf==null){
			return null;
		}
		try {
			return new String(buf,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();			
		}
		return null;
	}
	
	public String readTxt(boolean sync){
		sync0 = sync;
		return readTxt();
	}

	public void writeTxt(String txt){
		if(txt.length()==0){
			return;
		}
		byte[] buf = txt.getBytes(Charset.forName("UTF-8"));
		implWrite(buf);
	}
	
	public void writeTxt(String txt,int ms){
		writeTxt(txt);
		Misc.delay(ms);
	}
	
	public void writeTxt(char txt){
		byte[] buf = { (byte)txt };
		implWrite(buf);
	}
	//-----------------------//
	
	public String fetch(String tail){
		
		String txt = "";
		
		final int FAIL_MAX = 10;
		int failCnt = FAIL_MAX;
		int t_len = tail.length();
		for(;failCnt>0;){
			String tmp = readTxt();
			if(tmp==null){
				failCnt--;				
				continue;
			}
			txt = txt + tmp;
			failCnt = FAIL_MAX;//reset this number~~~
			if(txt.endsWith(tail)==true){
				int len = txt.length() - t_len;
				txt = txt.substring(0,len);
				break;
			}			
		}
		return txt;
	}
	
	public String fetch(String cmd,String tail){
		writeTxt(cmd);		
		return fetch(tail);
	}
	
	/**
	 * Disconnect terminal!!!
	 */
	public void close(){
		if(handle==0L){
			return;
		}
		implClose();
	}
	
	private native void implOpen(
		String name,
		int  baud_rate,
		char data_bit,
		char parity,
		char stop_bit,
		char flow_mode
	);

	private native byte[] implRead();
	
	private native void implWrite(byte[] buf);
	
	private native void implClose();
}
