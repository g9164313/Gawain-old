package narl.itrc;

public class DevTTY extends DevBase {

	public DevTTY(){
		TAG = "tty-stream";
	}

	public DevTTY(String path){	
		this();
		setPathName(path);
	}

	/**
	 * For Unix, it is a number(File descriptor).<p>
	 * For Windows, it is pointer(handle).<p>
	 * This variable will be updated by native code.<p>
	 */
	private long handle = 0L;//this is provided by native code

	
	/**
	 * Hardware/software flow control.<p>
	 * 0: none, TTY default setting.<p>
	 * 1: RTS/CTS 
	 * 2: DTR/DSR
	 */
	protected byte flowControl= 0;
	
	protected int readTimeout = 3000;//milliseconds~~

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
	//-----------------------//
	
	/**
	 * open tty device and parser path name.<p>
	 * @param path - device path name, like "/dev/ttyS0,19200,8n1"
	 * @return true - success, false - something is wrong
	 */
	public void open(String path){
		pathName = path;//reset path name~~~
		open();
	}
	
	/**
	 * Open tty device and parser path name.<p>
	 * Format is "[device name],[baud rate],[data bit][mask][stop bit]".<p>
	 * Data bit:<p>
	 *   7,8 <p>
	 * Mask type:<p>
	 *   'n' mean "none".<p>
	 *   'o' mean "odd".<p>
	 *   'e' mean "event".<p>
	 *   'm' mean "mark".<p>
	 *   's' mean "space".<p>
	 * Stop bit:<p>
	 *   1,2 <p>
	 * @return true - success, false - something is wrong
	 */
	@Override
	public void open() {
		//check path name is valid~~~
		if(pathName.length()==0){
			return;
		}
		String[] attr = pathName.split(",");
		if(attr.length!=3){
			return;
		}
		if(attr[1].matches("^\\d+$")==false) {
			return;
		}
		if(attr[2].matches("^[78][noems][12]")==false) {
			return;
		}
		char[] vals = attr[2].toCharArray();
		implOpen(
			attr[0], 
			Integer.valueOf(attr[1]), 
			vals[0], vals[1], vals[2], 
			'?'
		);
		afterOpen();
	}
	/**
	 * close tty device.<p>
	 */
	@Override
	public void close() {
		implClose();
		afterClose();
		stopFlow();
	}
	@Override
	public boolean isLive(){
		if(handle==0L) {
			return false;
		}
		return true;
	}
	
	protected void afterOpen() {}
	protected void afterClose() {}	
	//-----------------------//

	/**
	 * Access TTY by native code. blocking until one byte.<p>
	 * @return the byte which device gave
	 */
	public byte readByte() {
		byte[] buf = {0};
		implRead(buf,0,1);
		return buf[0];
	}
	/**
	 * Access TTY by native code.<p>
	 * @param buf - data buffer 
	 * @param off - offset in buffer
	 * @param len - data length
	 * @return data in length
	 */
	public int readByte(byte[] buf, int off, int len) {
		return implRead(buf,off,len);
	}
	/**
	 * Alias function, readByte(byte[], int, int).<p>
	 * @param buf
	 * @return
	 */
	public int readByte(byte[] buf) {
		return implRead(buf,0,-1); 
	}
	
	/**
	 * Read data until the end of buffer.<p>
	 * This function is blocking!!.<p>
	 * @param buf - keep data
	 * @param len - maximum length
	 */
	public void purgeByte(byte[] buf, int idx, int len) {
		do {
			int cnt = readByte(buf,idx,len);
			if(cnt>0) {
				idx+=cnt;
				len-=cnt;
			}
		}while(isLive()==true && len>0);
	}
	
	/**
	 * block-reading, thread will be trapped in this function.<p>
	 * In win7, it will stop after 5 second.<p>
	 * In unix, forever???
	 * @return
	 */
	public String readTxt() {
		return readTxt(500);
	}
	
	private static final int TXT_BUF_SIZE = 64;
	
	/**
	 * block-reading, and try to read until TTY timeout.<p>
	 * @param tryCount - count for trying.<p>
	 * @param maxLength - received length.<p>
	 * @return
	 */
	public String readTxt(int msec) {
		final byte[] buf = new byte[TXT_BUF_SIZE];
		String txt = "";
		long t0 = System.currentTimeMillis();
		do {
			int cnt = implRead(buf,0,-1);
			if(cnt>0) {
				txt = txt + new String(buf,0,cnt);
			}			
		}while(isLive()==true && (System.currentTimeMillis()-t0)<msec);
		return txt;
	}
	
	public String readTxt(final String regx) {
		final byte[] buf = new byte[TXT_BUF_SIZE];
		String txt = "";
		do {
			int cnt = implRead(buf,0,-1);
			if(cnt>0) {
				txt = txt + new String(buf,0,cnt);
				if(txt.matches(regx)==true) {
					break;
				}
			}
		}while(isLive()==true);
		return txt;
	}
	
	/**
	 * Access TTY by native code.<p>
	 * @param val - context value
	 */
	public int writeByte(final byte val){
		byte[] buf = {val};
		return implWrite(buf,0,1);
	}
	/**
	 * Alias function, writeByte(byte)
	 * @param val
	 * @return
	 */
	public int writeByte(final int val){
		return writeByte((byte)(val&0xFF));
	}
	/**
	 * Access TTY by native code.<p>
	 * @param buf - context value
	 * @param off - offset from buffer
	 * @param size- buffer size
	 * @return 
	 */
	public int writeByte(final byte[] buf, int off, int size){
		return implWrite(buf,off,size);
	}
	/**
	 * Alias function,  writeByte(byte[], int, int).<p>
	 * @param buf
	 * @return
	 */
	public int writeByte(final byte[] buf){
		return implWrite(buf,0,-1);
	}
	/**
	 * Write text via terminal-port.<p>
	 * @param txt - context data
	 */
	public int writeTxt(final String txt){
		//Charset cc_set = Charset.forName("UTF-8");
		if(txt==null) {
			return 0;
		}
		if(txt.length()==0){
			return 0;
		}
		return implWrite(txt.getBytes(),0,-1);
	}
	
	/**
	 * write byte with delay time.
	 * @param delay - milliseconds
	 * @param txt
	 */
	public void writeDelay(final int millisecond, final String txt) {
		final byte[] buf = txt.getBytes();
		for(int i=0; i<buf.length; i++) {
			implWrite(buf,i,1);
			Misc.delay(millisecond);
		}
	}
	//------------------------------------//
	
	//private final StringBuffer buff = new StringBuffer(512);	
	
	public interface ReadBack {
		void callback(String txt);
	};
	
	public void fetchTxt(
		final String cmd,
		final ReadBack hook
	) {
		writeTxt(cmd);
		hook.callback(readTxt());
	}
	//------------------------------------//
	
	private native void implOpen(
		String name,
		int  baud_rate,
		char data_bit,
		char parity,
		char stop_bit,
		char flow_mode
	);
	private native int implRead(byte[] buf, int off, int cnt);
	private native int implWrite(byte[] buf, int off, int cnt);
	private native void implClose();
}
