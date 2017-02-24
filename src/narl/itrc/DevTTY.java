package narl.itrc;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;

public class DevTTY extends DevBase {

	private final String TXT_NONE  = "--------";
	private final String TXT_UNKNOW= "????";
	
	public DevTTY(){
	}
	
	public DevTTY(String txt){		
		open(txt);
	}

	@Override
	protected Node eventLayout(){
		return null;
	}

	@Override
	void eventShutdown() {
		close();
	}
	//---------------------//
	
	/**
	 * File descriptor or handler to serial-port.<p>
	 * If the value is zero, it means we got fail~~.<p> 
	 * In Unix, it is a integer number.<p>
	 * In fxxking windows, it is pointer(handle).<p>
	 */
	private long handle = 0L;//this is provided by native code
	
	public boolean isLive(){
		if(handle==0){
			return false;
		}
		return true;
	}
	
	private String infoName = TXT_UNKNOW;
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
			Misc.logv("connect to "+txt);
		}
	}

	/**
	 * close terminal!!!
	 */
	public void close(){
		if(handle==0L){
			return;
		}
		implClose();
	}
	//-----------------------//
	
	/**
	 * Read byte data from terminal-port.<p>
	 * This is blocking method!!!.<p>
	 * @return context data
	 */
	public byte[] readBuf(){
		return implRead();
	}
	
	/**
	 * Read text from terminal-port.<p>
	 * This is blocking method!!!.<p>
	 * @return NULL - if no data<p>
	 * Text - what we read.<p>
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
	
	/**
	 * Read text from terminal-port and stopping after encountering tail part.<p>
	 * This is blocking method!!!.<p>
	 * @return NULL - if no data<p>
	 * Text - what we read has tail part.<p>
	 */
	public String readTxt(String tail){
		String txt = "";
		final int FAIL_MAX = 10;
		int fail_cnt = FAIL_MAX;
		for(;fail_cnt>0;){
			String tmp = readTxt();
			if(tmp==null){
				fail_cnt--;				
				continue;
			}
			txt = txt + tmp;
			fail_cnt = FAIL_MAX;//reset this number~~~
			if(txt.endsWith(tail)==true){
				int len = txt.length()-tail.length();
				txt = txt.substring(0,len);
				break;
			}			
		}
		return txt;		
	}
	
	/**
	 * Write byate data via terminal-port.<p>
	 * @param buf - context data
	 */
	public void writeBuf(byte[] buf){
		implWrite(buf);
	}
	
	/**
	 * Write character data via terminal-port.<p>
	 * @param buf
	 */
	public void writeTxt(char ch){
		byte[] buf = { (byte)ch };
		implWrite(buf);
	}
	
	/**
	 * Write text via terminal-port.<p>
	 * @param txt - context data
	 */
	public void writeTxt(String txt){
		if(txt.length()==0){
			return;
		}
		byte[] buf = txt.getBytes(Charset.forName("UTF-8"));
		implWrite(buf);
	}
	
	/**
	 * write text via terminal-port, then wait.<p>
	 * @param txt - context
	 * @param ms - delay millisecond
	 */
	public void writeTxt(String txt,int ms){
		writeTxt(txt);
		Misc.delay(ms);
	}
	//-----------------------//
	
	public String fetch(String cmd,String tail){
		writeTxt(cmd);		
		return readTxt(tail);
	}
	
	private Task<String> tskFetch = null;
	
	public void fetch(
			final String command,
			final String tail,
			EventHandler<ActionEvent> value
	){
		String[] commands = {command};
		fetch(commands,tail,value);
	}
	
	public void fetch(
		final String[] commands,
		final String tail,
		EventHandler<ActionEvent> value
	){
		if(tskFetch!=null){
			if(tskFetch.isRunning()==true){
				Misc.logw("DevTTY-fetch is running");
				return;
			}
		}
		tskFetch = new Task<String>(){
			@Override
			protected String call() throws Exception {
				String txt = "";
				for(String cmd:commands){
					txt = txt + "\t" + fetch(cmd,tail);
					Misc.delay(10);
				}
				return txt.substring(1);
			}
		};
		tskFetch.setOnSucceeded(event->{
			value.handle(new ActionEvent(tskFetch.valueProperty().get(),null));
		});
		new Thread(tskFetch,"DevTTY-fetch").start();
	}
	//-----------------------//
	
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
