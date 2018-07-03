package narl.itrc;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;


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
	 * Open tty device and parser path name.<p>
	 * Format is "[baud rate], [data bit][mask][stop bit]".<p>
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
		String[] sets = pathName.split(",");
		if(sets.length!=3){
			return false;
		}		
		try{
			String name = sets[0];			
			
			int baud = Integer.valueOf(sets[1]);			
			
			char[] attr = sets[2].toCharArray();
			if(attr.length<3){
				return false;
			}
			
			implOpen(name, baud, attr[0], attr[1], attr[2], '?');
			
		}catch(NumberFormatException e){
			return false;
		}		
		return true;
	}
	
	/**
	 * open tty device and parser path name.<p>
	 * @param path - device path name, like "/dev/ttyS0,19200,8n1"
	 * @return true - success, false - something is wrong
	 */
	public boolean open(String path){
		pathName = path;
		return open();
	}
	
	/**
	 * close tty device.<p>
	 */
	public void close(){
		if(handle==0L){
			return;
		}
		implClose();
	}
	//-----------------------//
	
	public Byte readOneByte(){
		final byte[] buf = {0};
		if(implRead(buf)==0L){
			return null;
		}
		return buf[0];
	}
	
	public int readByte(byte[] buf){
		return (int)implRead(buf);
	}
	
	public byte[] readByte(){
		return readByte(512);
	}
	
	public byte[] readByte(int maxSize){
		final byte[] buf = new byte[maxSize];
		int len = (int)implRead(buf);
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
			Byte cc = readOneByte();
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
			Byte cc = readOneByte();
			if(cc==null){
				continue;
			}
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
		if(implRead(buf)==0L){
			return null;
		}
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
	public String readTxt(int bufSize){
		final byte[] buf = new byte[bufSize];		
		if(implRead(buf)==0L){
			return null;
		}
		try {
			return new String(buf,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();			
		}
		return "";
	}
	
	public String readTxt(char end){
		return readTxt(end, -1);
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
	//------------------------------------//
	
	/**
	 * Write byte data via terminal-port.<p>
	 * @param buf - context data
	 */
	public void writeByte(byte cc){
		final byte[] buf = { cc };
		implWrite(buf);
	}
	
	/**
	 * Write byte data via terminal-port.<p>
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

	private native long implRead(byte[] buf);
	
	private native void implWrite(byte[] buf);
	
	private native void implClose();
}
