package narl.itrc;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;

public class DevTTY extends DevBase {

	public DevTTY(){
	}
	
	public DevTTY(String path_addr){	
		open(path_addr);
	}

	@Override
	protected Node eventLayout(PanBase pan){
		return new PanTTY(this);
	}

	/**
	 * This is special event, device want to do something before shutdown~~~
	 */
	protected void eventTurnOff(){		
	}

	@Override
	public void eventShutdown() {
		//stopTaskMonitor();
		eventTurnOff();
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
	
	public boolean isOpen(){
		return (handle==0L)?(false):(true);
	}
	
	private final String TXT_UNKNOW_NAME = "？";
	private final char   TXT_UNKNOW_ATTR = '？';
	
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
	public boolean setInfoPathAttr(String path_attr){
		
		resetInfoPath();
		
		String[] arg = path_attr.trim().split(",");
		//check we have 3 arguments at least
		if(arg.length<3){
			Misc.loge("fail to connect "+path_attr);
			return false;
		}
		
		//check the fist argument,it is device name		
		if(Gawain.isPOSIX==true){
			if(new File(arg[0]).exists()==false){
				Misc.loge("Unknown device --> "+path_attr);
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
	 * if path have no control statement, it will append the second argument.<p>
	 * @param path_attr - device name, or full name
	 * @param attr - default control statement
	 * @return
	 */
	public long open(String path_attr,String attr){
		if(path_attr.contains(",")==false){
			path_attr = path_attr + ","+attr; //add default attribute setting.
		}		
		return open(path_attr);
	}
	
	/**
	 * open TTY and start to communication.<p>
	 * the argument path must be full name. it means path includes device name and control statement.<p>
	 * @param path_attr - full name, including device name and control statement, ex:/dev/ttyS0,9600,8n1.
	 */
	public long open(String path_attr){
		if(setInfoPathAttr(path_attr)==false){
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
	 * Like readByte(), but it will return null!!!
	 * @return - null or byte value
	 */
	public Byte readOneByte(){
		final byte[] buf = implRead(1);
		if(buf==null){
			return null;
		}
		return buf[0];
	}
	
	/**
	 * Read just one byte data<p>
	 * This is blocking method!!!.<p>
	 * @return byte from TTY device
	 */
	public byte readByte(){
		final byte[] buf = implRead(1);
		if(buf==null){
			return (byte)0x00;
		}
		return buf[0];
	}
		
	/**
	 * Read just one character<p>
	 * This is blocking method!!!.<p>
	 * @return character from TTY device
	 */
	public char readChar(){
		return (char)(readByte());
	}
	
	/**
	 * Read byte data from terminal-port.<p>
	 * This is blocking method!!!.<p>
	 * @return context data
	 */
	public byte[] readBuf(){
		return implRead(-1);
	}
	
	public byte[] readPack(byte beg,byte end){
		
		ArrayList<Byte> lst = new ArrayList<Byte>();
		
		boolean flg = false;
		
		long tk2 = System.currentTimeMillis();
		long tk1 = tk2;
		
		while((tk2-tk1)<1000L){
			byte[] buf = implRead(1);
			if(buf==null){
				Misc.delay(50);
				tk2 = System.currentTimeMillis();
				continue;
			}		
			if(buf[0]==beg){
				flg = true;//for next turn~~~~
			}else if(flg==true){
				if(buf[0]==end){
					break;
				}
				lst.add(buf[0]);
			}
			tk2 = tk1;//reset ticker~~~
		}
		return check_out(lst);
	}
	
	/*public byte[] readPackBuck(byte beg,byte end){

		long tk2 = System.currentTimeMillis();
		long tk1 = tk2;
		int idxBeg=-1;//the index of buffer
		int idxEnd= 0;//the index of pool
		
		byte[] pool = new byte[1024];
		
		while((tk2-tk1)<1000L){
			
			byte[] buf = implRead(-1);
			
			if(buf==null){
				Misc.delay(30);
				tk2 = System.currentTimeMillis();
				continue;
			}else{
				tk2 = tk1 = System.currentTimeMillis();
			}
						
			for(int i=0; i<buf.length; i++){
				if(idxBeg<0){					
					if(buf[i]==beg){
						idxBeg = i;
						continue;
					}
				}else{
					if(buf[i]==end){						
						return Arrays.copyOfRange(pool, 0, idxEnd);
					}
					pool[idxEnd] = buf[i];
					idxEnd+=1;
				}
			}
		}		
		return null;
	}*/
	
	private byte[] check_out(ArrayList<Byte> lst){
		int cnt = lst.size();
		if(cnt==0){
			return null;
		}
		byte[] buf = new byte[cnt];
		for(int i=0; i<cnt; i++){
			buf[i] = lst.get(i);
		}
		return buf;
	}

	public String readTxt(char end){
		return readTxt(end,-1);
	}
	
	public String readTxt(char end,int ms){
		String res = "";
		for(;;){
			char cc = readChar();			
			if(cc==end){
				break;
			}			
			res = res + cc;
			if(ms>0){
				Misc.delay(ms);
			}
		}
		return res;
	}
	
	public String readTxt(char beg,char end){
		return readTxt(beg,end,-1);
	}
	
	public String readTxt(char beg,char end,int ms){
		String res = "";
		boolean flg = false;
		long tk2 = System.currentTimeMillis();
		long tk1 = tk2;
		while((tk2-tk1)<1000L){
			char cc = readChar();			
			if(cc==beg){
				flg = true;
			}else if(cc==0){
				tk2 = System.currentTimeMillis();
				continue;
			}else if(flg==true){
				if(cc==end){
					return res;
				}
				res = res + cc;
			}
			tk2 = tk1;
			if(ms>0){
				Misc.delay(ms);
			}
		}
		return res;
	}
	
	/**
	 * Read text from terminal-port.<p>
	 * This is blocking method!!!.<p>
	 * @return NULL - if no data<p> Text - what we read.<p>
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
	 * @return NULL - if no data<p> Text - what we read has tail part.<p>
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
	//------------------------------------//
	
	/**
	 * Write byate data via terminal-port.<p>
	 * @param buf - context data
	 */
	public void writeByte(byte cc){
		final byte[] buf = { cc };
		implWrite(buf);
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
		final byte[] tmp = { (byte)ch };
		implWrite(tmp);
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
	 * It is same as writeTxt(), but send one character one time~~~
	 * Attention, this function is blocking!!! 
	 * @param txt - context data
	 */
	public void writeTxt(String txt, int ms){
		char[] buf = txt.toCharArray();
		for(char cc:buf){
			writeTxt(cc);
			Misc.delay(ms);
		}		
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
