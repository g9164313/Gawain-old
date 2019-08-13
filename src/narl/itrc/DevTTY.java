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
	
	private Thread thrRead;
	
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
			thrRead = new Thread(runLooper,TAG);
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
	
	//Charset.forName("UTF-8");//Do we need this object??
	
	private final StringBuffer stream = new StringBuffer(512);
	
	public class Action extends DevBase.Act {
		
		private String w_data = null;
		private String r_head = null;
		private String r_tail = null;
		private ReadBack hook = null;
		
		public Action writeData(final String txt) {
			w_data = txt;
			return this;
		}
		public Action readHead(final String txt) {
			r_head = txt;
			return this;
		}
		public Action readTail(final String txt) {
			r_tail = txt;
			return this;
		}
		public Action setHook(final ReadBack callback) {
			hook = callback;
			return this;
		}
		
		private void callback() {
			if(hook==null) {
				return;
			}
			final String txt = stream.toString();
			hook.callback(this,txt);
			stream_clear();
		}
	};
	
	public interface ReadBack {
		void callback(final Act action, final String txt);
	};

	public interface Hook {
		void looper(byte[] buf, int cnt);
	};
	
	private Hook peek=null;
	
	public void setPeek(final Hook callback) {
		peek = callback;
	}
	
	private void stream_clear() {
		stream.delete(0, stream.length());
	}
	
	private void stream_buf(
		final byte[] buf, 
		final int cnt
	) {
		if(cnt<=0) {
			return;
		}
		int len = stream.length();
		int cap = stream.capacity();
		if((len+cnt)>=cap) {
			stream.delete(0, cnt);
		}
		for(int i=0; i<cnt; i++) {
			stream.append(((char)buf[i]));
		}
		if(peek!=null) {
			peek.looper(buf, cnt);
		}
		//debug_buf(buf,cnt);
	}
	
	private void debug_buf(
		final byte[] buf, 
		final int cnt
	) {
		String txt="";
		for(int i=0; i<cnt; i++) {
			txt = txt + ((char)buf[i]);
		}
		Misc.logv("R-->%s", txt);
	}
	
	private final Runnable runLooper = new Runnable() {
		@Override
		public void run() {
			int cnt = 0;
			byte[] buf = new byte[16];
			do {
				Action act = (Action) action.poll();
				if(act!=null) {
					stream_clear();
					writeTxt(act.w_data);
					//Misc.logv("W-->%s", act.w_data);
					int idx = 0;
					boolean hasHead=(act.r_head==null)?(true):(false);
					boolean hasTail=(act.r_tail==null)?(true):(false);
					do {
						if(hasHead==true && hasTail==true) {
							act.callback();
							break;
						}
						cnt = implRead(buf,0);
						stream_buf(buf,cnt);
						if(act.r_head!=null) {
							idx = stream.indexOf(act.r_head);
							if(idx>=0) {
								stream.delete(idx, idx+act.r_head.length());
								hasHead = true;
							}
						}
						if(act.r_tail!=null) {
							idx = stream.indexOf(act.r_tail);
							if(idx>=0) {
								idx += act.r_tail.length();
								stream.delete(idx,stream.length());
								hasTail = true;
							}
						}
					}while(Gawain.isExit()==false && handle!=0L);
					check_loop(act);
				}else {
					cnt = implRead(buf,0);
					stream_buf(buf,cnt);
				}
			}while(Gawain.isExit()==false && handle!=0L);
			Misc.logv("%s done",TAG);
		}
	};
	//------------------------------------//
	
	/**
	 * It is same as readTxt(), but no head pattern.<p>
	 * Just for convenience.<p>
	 * @param tail - text start with, it can be null
	 * @param hook - callback
	 */
	public void readTxt(
		final String tail,
		final ReadBack hook
	) {
		readTxt(null,tail,hook);
	}
	
	/**
	 * Wait stream which had some pattern.<p>
	 * @param head - text start with, it can be null
	 * @param tail - text end with, it can be null
	 * @param hook - callback
	 */
	public void readTxt(
		final String head, 
		final String tail,
		final ReadBack hook
	) {
		final Action act = new Action();
		act.r_head = head;
		act.r_tail = tail;
		act.hook = hook;
		take(act);
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
		implWrite(txt.getBytes());
	}
	
	/**
	 * Write byte data via terminal-port.<p>
	 * @param buf - context data
	 */
	public void writeByte(byte... buf){
		implWrite(buf);
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
