package narl.itrc;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import javafx.concurrent.Task;


public class DevTTY {

	public DevTTY(){
	}

	public DevTTY(String path){	
		pathName = path;
	}

	/**
	 * For Unix, it is a number(File descriptor).<p>
	 * For Windows, it is pointer(handle).<p>
	 * This variable will be updated by native code.<p>
	 */
	private long handle = 0L;//this is provided by native code

	public long getHandle(){
		return handle;
	}
	
	/**
	 * Device path name, it is like "/dev/ttyS0,9600,8n1".<p>
	 * In windows, it will be like "\\.\COM1,9600,8n1".<p>
	 */
	private String pathName = "";
	
	public void setPathName(String path){
		pathName = path;
	}
	
	public String getPathName(){
		return pathName;
	}
	
	public boolean isOpen(){
		return (handle==0L)?(false):(true);
	}
	//-----------------------//
	
	/**
	 * open tty device and parser path name.<p>
	 * @param path - device path name, like "/dev/ttyS0,19200,8n1"
	 * @return true - success, false - something is wrong
	 */
	public boolean open(String path){
		pathName = path;//reset path name~~~
		return open();
	}
	
	/**
	 * Open tty device and parser path name.<p>
	 * Format is "[device name],[baud rate],[data bit][mask][stop bit]".<p>
	 * Mask type:<p>
	 *   'n' mean "none".<p>
	 *   'o' mean "odd".<p>
	 *   'e' mean "event".<p>
	 *   'm' mean "mark".<p>
	 *   's' mean "space".<p>
	 * @return true - success, false - something is wrong
	 */
	public boolean open(){		
		handle = 0L;//reset this~~~
		if(pathName==null){
			return false;
		}else if(pathName.length()==0){
			return false;
		}
		//check path name is valid~~~
		String[] attr = pathName.split(",");
		if(attr.length!=3){
			return false;
		}		
		try{
			String name = attr[0];			
			
			int baud = Integer.valueOf(attr[1]);			
			
			char[] vals = attr[2].toCharArray();
			if(vals.length<3){
				return false;
			}
			
			implOpen(name, baud, vals[0], vals[1], vals[2], '?');
			
		}catch(NumberFormatException e){
			return false;
		}		
		return isOpen();
	}

	/**
	 * close tty device.<p>
	 */
	public void close(){
		if(handle==0L){
			return;
		}
		stream.cancel();
		implClose();
	}
	//-----------------------//
	
	public byte readByte1(){
		final byte[] buf = {0};
		implRead(buf,0);
		return buf[0];
	}
	
	public Byte readByteOne(){
		final byte[] buf = {0};
		if(implRead(buf,0)==0L){
			return null;
		}
		return buf[0];
	}
	
	public int readBuff(byte[] buf){
		return (int)implRead(buf,0);
	}
	
	public byte[] readBuff(){
		return readBuff(1024);
	}
	
	public byte[] readBuff(final int maxSize){
		final byte[] buf = new byte[maxSize];
		int len = (int)implRead(buf,0);
		if(len==0){
			return null;
		}
		return Arrays.copyOf(buf, len);
	}
	
	public byte[] readByte(byte end){
		return readByte(end, -1);
	}
	
	public byte[] readByte(byte end, int ms){
		ArrayList<Byte> lst = new ArrayList<Byte>();		
		for(;;){
			Byte cc = readByteOne();
			if(cc==null){
				continue;
			}
			if(cc==end){
				break;
			}
			if(lst!=null){
				lst.add(cc);
				if(ms>0){
					Misc.delay(ms);
				}
			}			
		}
		byte[] result = new byte[lst.size()];
		for(int i=0; i<lst.size(); i++){
			result[i] = lst.get(i);
		}
		return result;
	}
	
	public byte[] readByte(byte beg, byte end){
		return readByte(beg, end, -1);
	}
	
	public byte[] readByte(byte beg, byte end, int ms){
		for(;;){
			Byte cc = readByte1();
			if(cc==beg){
				return readByte(end,ms);
			}		
		}
	}
	
	/**
	 * Read just one character<p>
	 * This is blocking method!!!.<p>
	 * @return - one character or null when error happened~~
	 */
	public Character readChar(){
		final byte[] buf = {0};
		if(implRead(buf,0)==0L){
			return null;
		}//example for char value: '\u0000' ~ '\uffff'
		return (char)(buf[0]);
	}

	/**
	 * Read data, and convert it to string type.<p>
	 * Buffer length is default size, 512 byte.<p>
	 * This is blocking method!!!.<p>
	 * @return NULL - if no data<p> Text - what we read.<p>
	 */
	public String readTxt(){
		return readTxt(512);
	}
	
	/**
	 * It is same as readTxt(), the difference is buffer size can be set.<p>
	 * If data contain unsupported character, the result will be a empty string.<p>
	 * This is blocking method!!!.<p>
	 * @param bufSize - buffer size
	 * @return NULL - if no data<p> Text - what we read.<p>
	 */
	public String readTxt(final int bufSize){
		final byte[] buf = new byte[bufSize];
		int len = implRead(buf,0);
		if(len<=0){
			return null;
		}
		return new String(buf).substring(0, len);
	}

	/**
	 * Read data until encountering the tail token
	 * @param end - the tail token.
	 * @param ms - delay millisecond between reading.
	 * @return string data
	 */
	public String readTxt(char end, int ms){
		String result = "";
		for(;;){
			Character cc = readChar();
			if(cc==null){
				continue;
			}
			if(cc==end){
				break;
			}
			if(result!=null){
				result = result + cc;
				if(ms>0){
					Misc.delay(ms);
				}
			}			
		}
		return result;
	}
	
	/**
	 * Read data when encountering the head token, and stop when matching the tail token.<p>
	 * Result is not including the token.<p> 
	 * This is blocking method!!!.<p>
	 * @param beg - the head token for stream. 0 will be ignore.
	 * @param end - the tail token for stream.
	 * @return string data
	 */
	public String readTxt(char beg, char end){
		return readTxt(beg,end,-1);
	}
	
	/**
	 * It is same as readTxt().<p>
	 * This is blocking method!!!.<p>
	 * @param beg - the head token for stream. 0 will be ignore.
	 * @param end - the tail token for stream.
	 * @param ms - delay millisecond between reading.
	 * @return string data
	 */
	public String readTxt(char beg, char end, int ms){
		for(;;){
			Character cc = readChar();
			if(cc==null){
				continue;
			}
			if(cc==beg){
				return readTxt(end,ms);
			}		
		}
	}
		
	private Task<Integer> stream = null;

	private AtomicReference<String> s_text = new AtomicReference<>();
	
	private static final int s_size = 512;
	
	public String getStream(){
		if(stream==null){
			return "";
		}
		return s_text.get();
	}

	public String getStreamTail(
		final String delimiter,
		final int count
	){		
		if(stream==null){
			return "";
		}
		int off = delimiter.length();
		while(stream.isCancelled()==false){			
			String txt = s_text.get();
			int beg = txt.length();
			int end = -1;
			int cnt = 0;
			for(;beg>0;){
				beg = txt.lastIndexOf(delimiter, beg);
				if(end==-1){
					end = beg;//this is tail~~~
				}
				if(beg>=0){
					cnt+=1;
					if(cnt>count){
						s_text.set(txt.substring(end-off));
						return txt.substring(beg+off,end);
					}
					if(beg==0){
						break;
					}else{
						beg-=1;
					}
				}
			}
			Misc.delay(5);
		}
		return "";
	}
	
	public void createStream(){		
		if(stream!=null){
			return;
		}
		s_text.set("");
		stream = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				do{
					if(handle==0L){
						return 0;
					}
					String old = s_text.get();
					if(old.length()>=s_size){
						old = old.substring(s_size/2);
					}
					String txt = readTxt();
					if(txt==null){
						continue;
					}
					s_text.set(old + txt);										
				}while(stream.isCancelled()==false);
				return 1;
			}
		};
		new Thread(stream,"tty-stream").start();
	}	
	//------------------------------------//
	
	/**
	 * Write byte data via terminal-port.<p>
	 * @param buf - context data
	 */
	public void writeByte(byte cc){
		final byte[] buf = { cc };
		implWrite(buf);
	}
	
	public void writeByte(byte... val){
		implWrite(val);
	}
	
	/**
	 * Write byte data via terminal-port.<p>
	 * @param buf - context data
	 */
	public void writeBuff(byte[] buf){
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
		implWrite(txt.getBytes(Charset.forName("UTF-8")));
	}
	
	/**
	 * It is same as writeTxt(), but send characters one by one~~~
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
	
	private native void implOpen(
		String name,
		int  baud_rate,
		char data_bit,
		char parity,
		char stop_bit,
		char flow_mode
	);

	private native int implRead(byte[] buf, int offset);
	
	private native void implWrite(byte[] buf);
	
	private native void implClose();
}
