package narl.itrc;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;

public class DevTTY extends DevBase {

	public DevTTY(){
	}
	
	public DevTTY(String path){	
		open(path);
	}

	@Override
	protected Node eventLayout(){
		return new PanTTY(this);
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
	
	public long getHandle(){
		return handle;
	}
	
	private final String TXT_UNKNOW_NAME = "?";
	private final char TXT_UNKNOW_ATTR = '?';
	
	private String infoName = TXT_UNKNOW_NAME;
	private int  infoBaud = -1;
	private char infoData = TXT_UNKNOW_ATTR;
	private char infoPart = TXT_UNKNOW_ATTR;
	private char infoStop = TXT_UNKNOW_ATTR;
	
	public String getName(){
		return infoName;
	}
	
	public String getBaud(){
		if(infoBaud<=0){
			return TXT_UNKNOW_NAME;
		}
		return String.valueOf(infoBaud);
	}
	
	public String getData(){
		if(infoData==TXT_UNKNOW_ATTR){
			return TXT_UNKNOW_NAME;
		}
		return String.valueOf(infoData);
	}

	public String getParity(){
		switch(infoPart){
		case 'n': return "none";
		case 'o': return "odd";
		case 'e': return "event";
		case 'm': return "mark";
		case 's': return "space";
		}
		return TXT_UNKNOW_NAME;
	}
	
	public String getStopBit(){
		if(infoData==TXT_UNKNOW_ATTR){
			return TXT_UNKNOW_NAME;
		}
		return String.valueOf(infoStop);
	}
	
	private void resetInfoPath(){
		infoName = TXT_UNKNOW_NAME;
		infoBaud = -1;
		infoData = TXT_UNKNOW_ATTR;
		infoPart = TXT_UNKNOW_ATTR;
		infoStop = TXT_UNKNOW_ATTR;
	}
	
	/**
	 * Parse the control statement like "/dev/ttyS0,9600,8n1".<p>
	 * The control statement is composed of baud-rate,data-bit,parity,stop-bit.<p>
	 * Parity can be 'n'(none),'o'(odd),'e'(event),'m'(mask) and 's'(space).<p>
	 * @param name - control statement.
	 * @return TRUE - valid, FALSE - invalid
	 */
	public boolean setInfoPath(String path){
		
		resetInfoPath();
		
		String[] arg = path.trim().split(",");
		//check we have 3 arguments at least
		if(arg.length<3){
			Misc.loge("fail to connect "+path);
			return false;
		}
		
		//check the fist argument,it is device name		
		if(Misc.isPOSIX()==true){
			if(new File(arg[0]).exists()==false){
				Misc.loge("Unknown device --> "+path);
				return false;
			}
		}else{
			//how to check whether fxxking windows system has terminal
		}
		infoName = arg[0];
		
		//check the second argument is integer
		try{
			infoBaud = Integer.valueOf(arg[1]);
		}catch(NumberFormatException e){
			Misc.loge("error baud : "+arg[1]);
			return false;
		}
		
		//how to check valid below lines???
		char[] ctrl = arg[2].toCharArray();
		infoData = ctrl[0];
		infoPart = ctrl[1];
		infoStop = ctrl[2];
		
		return true;
	}
	
	
	public boolean setInfoAttr(String attr){
		
		String[] arg = attr.trim().split(",");
		//check the second argument is integer
		try{
			infoBaud = Integer.valueOf(arg[0]);
		}catch(NumberFormatException e){
			Misc.loge("error baud : "+arg[0]);
			return false;
		}
				
		//how to check valid below lines???
		char[] ctrl = arg[1].toCharArray();
		infoData = ctrl[0];
		infoPart = ctrl[1];
		infoStop = ctrl[2];
				
		return true;		
	}	
	//---------------------//
	
	/**
	 * open TTY and start to communication.<p>
	 * 
	 * @param path - control statement.
	 */
	public long open(String path){
		if(setInfoPath(path)==false){
			return -1L;
		}
		return open();
	}
	
	/**
	 * open TTY and start to communication~~~
	 * @param txt - control statement.
	 */
	public long open(){
		implOpen(
			infoName,
			infoBaud,
			infoData,
			infoPart,
			infoStop,
			'?'
		);		
		if(handle!=0){
			setAlive(true);
		}else{
			setAlive(false);
		}
		return handle;
	}

	/**
	 * close terminal!!!
	 */
	public void close(){
		if(handle==0L){
			return;
		}
		implClose();
		setAlive(false);
	}
	//-----------------------//
	
	/**
	 * Read just one byte data<p>
	 * This is blocking method!!!.<p>
	 * @return response
	 */
	public byte readByte(){
		byte[] buf = implRead(1);
		return buf[0];
	}
	
	/**
	 * Read byte data from terminal-port.<p>
	 * This is blocking method!!!.<p>
	 * @return context data
	 */
	public byte[] readBuf(){
		return implRead(-1);
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
		
	/**
	 * 
	 * Write command and read back what the device generated.<p>
	 * Attention!!, this is a blocking procedure.<p>
	 * 
	 * @param command - what we write to TTY device
	 * @param tail - the tail of text what we read  
	 * @param value - if it were done, invoke this event.
	 */
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
				String result = "";
				for(String cmd:commands){
					String txt = null;
					while(txt==null){
						txt = fetch(cmd,tail);
					}
					result = result + "\t" + txt;
					Misc.delay(10);
				}
				return result.substring(1);
			}
		};
		tskFetch.setOnSucceeded(event->{
			value.handle(new ActionEvent(tskFetch.valueProperty().get(),null));
		});
		new Thread(tskFetch,"DevTTY-fetch").start();
	}
	
	/**
	 * Write command and read back what the device generated.<p>
	 * Attention!!, this is a blocking procedure.<p>
	 * 
	 * @param command - what we write to TTY device
	 * @param tail - the tail of text what we read  
	 * @param value - if it were done, invoke this event.
	 */
	public void fetch(
		final String command,
		final String tail,
		EventHandler<ActionEvent> value
	){
		String[] commands = {command};
		fetch(commands,tail,value);
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

	private native byte[] implRead(int len);
	
	private native void implWrite(byte[] buf);
	
	private native void implClose();
}
