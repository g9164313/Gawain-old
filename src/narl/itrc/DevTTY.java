package narl.itrc;

import java.nio.charset.Charset;

import java.util.concurrent.ArrayBlockingQueue;


public class DevTTY {

	public DevTTY(){
	}

	public DevTTY(String path){	
		setPathName(path);
	}
	
	protected String TAG = "tty-stream";

	/**
	 * For Unix, it is a number(File descriptor).<p>
	 * For Windows, it is pointer(handle).<p>
	 * This variable will be updated by native code.<p>
	 */
	private long handle = 0L;//this is provided by native code

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
		//prepare reading task~~~
		boolean flag = isOpen();
		if(flag==true) {
			thrRead = new Thread(runRead,TAG);
			thrRead.setDaemon(true);
			thrRead.start();
		}else {
			thrRead = null;
		}
		return flag;
	}

	/**
	 * close tty device.<p>
	 */
	public void close(){
		if(handle==0L){
			return;
		}
		implClose();
		if(thrRead!=null) {
			int max_iter = 5;
			while(thrRead.isAlive()==true && max_iter>0){
				thrRead.interrupt();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					break;
				}
				max_iter-=1;
			};
		}		
		thrRead = null;
	}
	//-----------------------//
	
	private final ArrayBlockingQueue<Byte> stream = new ArrayBlockingQueue<Byte>(1024);
		
	public interface Hook {
		void looper(byte[] buf, int len);
	};
	
	private Thread thrRead;
	
	private Hook peek=null;
	
	public void setPeek(final Hook callback) {
		peek = callback;
	}
	
	private final Runnable runRead = new Runnable() {
		@Override
		public void run() {
			byte[] buf = new byte[16];
			try {
				do {
					int cnt = implRead(buf,0);
					int rem = stream.remainingCapacity();
					if(rem<cnt) {
						//clear something for new data~~
						rem = cnt - rem;
						for(int i=0; i<rem; i++) {
							stream.take();
						}						
					}
					for(int i=0; i<cnt; i++) {
						stream.offer(buf[i]);
					}
					if(cnt>0 && peek!=null) {
						peek.looper(buf, cnt);
					}
				}while(Gawain.isExit()==false && handle!=0L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Misc.logv("%s done",TAG);
		}
	};
	
	/**
	 * Draw all data in stream.<p>
	 * It means that queue will be empty.<p> 
	 * @return  text
	 */
	public String readTxt(final int waitMillSecond){
		String txt = "";
		try {
			do {
				if(waitMillSecond>0) {
					Thread.sleep(waitMillSecond);
				}
				if(stream.peek()==null) {
					break;
				}
				txt = txt + (char)stream.take().byteValue();
			}while(thrRead.isAlive()==true);
		} catch (InterruptedException e) {
			//e.printStackTrace();
			//Misc.logw("stop %s",TAG);
		}
		return txt;
	}
	
	public String readTxt(String tail) {
		return readTxt(null,tail);
	}
	
	public String readTxt(String head, String tail) {
		String txt = "";
		try {
			do {
				if(head==null && tail==null) {
					break;
				}
				txt = txt + (char)stream.take().byteValue();
				if(head!=null) {
					int idx = txt.indexOf(head);
					if(idx>=0) {						
						txt = txt.substring(idx);
						head= null;
					}
				}
				if(tail!=null) {
					if(txt.endsWith(tail)==true) {
						tail = null;
					}
				}
			}while(thrRead.isAlive()==true);
		} catch (InterruptedException e) {
			//e.printStackTrace();
			Misc.logw("stop tty-reading");
		}		
		return txt;
	}
	
	public static String buff2text(
		final byte[] buf, 
		final int len
	) {
		String txt = "";
		for(int i=0; i<len; i++) {
			txt = txt + ((char)buf[i]);
		}
		return txt;
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
	public void writeByte(byte... buf){
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
	 * Write text and read data from tty-device.<p>
	 * It is lazy function to get data.<p>
	 * Disadvantage is that thread is frequently created.<p>
	 */
	public void fetchTxt(
		final String writeTxt,
		final String readHead,
		final String readTail
	) {
		
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
