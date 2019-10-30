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
		startLoop();
	}
	/**
	 * close tty device.<p>
	 */
	@Override
	public void close() {
		stopLoop();
		implClose();
		
	}
	@Override
	public boolean isLive(){
		if(handle==0L) {
			return false;
		}
		if(Gawain.isExit()==true) {
			return false;
		}
		return true;
	}	
	//-----------------------//

	public interface ReadBack {
		void callback(String txt);
	};
	public interface FetchBack {
		void callback(String cmd, String txt);
	};
	
	protected final StringBuffer buff = new StringBuffer(1024*4);
		
	@Override
	protected void doLoop(DevBase dev) {
		fill_buff();
	}
	private void fill_buff() {
		final byte[] tmp = new byte[512];		
		int cnt = implRead(tmp,0,tmp.length);
		if(cnt<=0) {
			return;
		}
		//TODO: peek buffer feature~~~~
		buff.append((char)tmp[0]);
		int cap = buff.capacity();
		if(buff.length()>=cap) {
			buff.delete(0, cap/2);
		}
	}
	
	protected class FastFetch extends DevBase.Action 
		implements DevBase.Work
	{		
		private short ends;
		private long tick1, count=0L;
		private long expire = 100L;
		private boolean w_flag = false;
		private String w_text = "";
		private FetchBack result;
		
		public FastFetch(
			final int stage,
			final int the_end,
			final int expired,
			final String writing,
			final FetchBack hooker			
		) {				
			stgx = (short)stage;			
			next = (short)-1;			
			hook = this;
			ends = (short)the_end;
			expire = expired;
			w_text = writing;
			result = hooker;			
		}
		public FastFetch(
			final int stage,
			final String writing,
			final FetchBack hooker
		) {				
			this(stage,stage+1,5,writing,hooker);			
		}
		public FastFetch(
			final int stage,
			final int the_end,
			final String writing,
			final FetchBack hooker
		) {				
			this(stage,the_end,5,writing,hooker);			
		}
		
		@Override
		public void doWork(Action act) {
			if(w_flag==false) {
				buff.delete(0, buff.length());//clear buffer~~~
				writeTxt(w_text);
				tick1 = System.currentTimeMillis();
				count = buff.length();
				w_flag = true;
				return;
			}
			long cnt = buff.length();
			if((cnt-count)==0 && count!=0) {
				//stream no growth~~~
				long t2 = System.currentTimeMillis();
				if((t2-tick1)>=expire) {
					act.next = ends;
					if(act.stgx<0) {
						//this action will be hold
						//so reset this fetch action again!!!
						w_flag = false;
					}
					if(result!=null) {
						result.callback(w_text,buff.toString());
					}
					return;
				}
			}else {
				//update count for stream~~~
				tick1 = System.currentTimeMillis();
				count = cnt;
			}
		}
	};
	//------------------------------------//
	
	/**
	 * Access TTY by native code. blocking until one byte.<p>
	 * @return the byte which device gave
	 */
	public byte readByte() {
		byte[] buf = {0};
		int res=0;
		while(isLive()==true && res<=0){
			res = implRead(buf,0,1);
			if(res<=0) {
				Misc.delay(5);
			}
		}
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
	 * Access TTY by native code, but blocking.<p>
	 * @param buf - data buffer 
	 * @param off - offset in buffer
	 * @param len - data length
	 * @return data in length
	 */
	public void readBytePurge(byte[] buf, int off, int len) {
		while(isLive()==true && len>0){
			int cnt = implRead(buf,off,len);
			if(cnt<=0) {
				Misc.delay(10);
				continue;
			}
			off+=cnt;
			len-=cnt;
		}
	}
	/**
	 * Access TTY by native code.<p>
	 * @param val - context value
	 */
	public int writeByte(byte val){
		byte[] buf = {val};
		return implWrite(buf,0,1);
	}
	public int writeByte(int val){
		return writeByte((byte)(val&0xFF));
	}
	/**
	 * Access TTY by native code.<p>
	 * @param buf - context value
	 * @param off - offset from buffer
	 * @param size- buffer size
	 * @return 
	 */
	public int writeByte(byte[] buf, int off, int size){
		return implWrite(buf,off,size);
	}
	
	//private final static Charset cc_set = Charset.forName("UTF-8");
	/**
	 * Write text via terminal-port.<p>
	 * @param txt - context data
	 */
	public void writeTxt(String txt){
		if(txt==null) {
			return;
		}
		if(txt.length()==0){
			return;
		}
		implWrite(txt.getBytes(),0,-1);
	}
	
	protected static final int DEFAULT_FETCH_EXPIRED = 100;
	
	/**
	 * Blocking method.<p>
	 * write text and wait for response in expired time.<p>
	 * @param txt - writing text.<p>
	 * @param exp - waiting time for millisec.<p>
	 * @return data buffer
	 */
	public String fetchTxt(
		final String cmd,		
		final int expire_ms
	) {
		writeTxt(cmd);
		
		String txt = "";
		
		final byte[] tmp = new byte[256];
		
		long t1 = System.currentTimeMillis();
		
		while(Gawain.isExit()==false){
			
			int cnt = implRead(tmp,0,-1);
			
			long t2 = System.currentTimeMillis();
			
			if(cnt>0) {
				txt = txt + new String(tmp,0,cnt);
				t1 = System.currentTimeMillis();
			}else {
				if((t2-t1)>expire_ms) {
					return txt;
				}
				Misc.delay(DEFAULT_FETCH_EXPIRED/4);//wait TTY input buffer
			}
		};
		return txt;
	}
	/**
	 * fetch text with expired time (5ms). 
	 * @param cmd - writing command
	 * @return fetched data
	 */
	public String fetchTxt(
		final String cmd
	) {
		return fetchTxt(cmd, DEFAULT_FETCH_EXPIRED);
	}
	
	public void asyncFetch(
		final String cmd,
		final int expire_ms, 
		final FetchBack result
	) {
		doing(0,0,(act)->{
			String txt = fetchTxt(cmd,expire_ms);
			if(result!=null) {
				result.callback(cmd, txt);
			}
		});
	}
	public void asyncFetch(
		final String cmd,
		final FetchBack result
	) {
		asyncFetch(cmd, DEFAULT_FETCH_EXPIRED, result);
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
