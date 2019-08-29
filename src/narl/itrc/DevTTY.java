package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;

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
	
	public boolean isLive(){
		if(Gawain.isExit()==true) {
			return false;
		}
		if(handle==0L) {
			return false;
		}
		return true;
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
	
	protected boolean asynMode = true;
	
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
			implOpen(
				name, baud, 
				vals[0], vals[1], vals[2], 
				'?'
			);			
		}catch(NumberFormatException e){
			return false;
		}
		//prepare reading task~~~
		boolean flag = isLive();
		if(flag==true) {
			if(asynMode==true) {
				looper_start();
			}
			afterOpen();
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
		looper_stop();
		afterClose();
		implClose();
	}

	protected void afterOpen() { }
	protected void afterClose() { }
	//-----------------------//

	public class Action extends DevBase.Act {
		
		private String w_data = "";
		private String[] r_data = {"", ""};
		//private String r_match = null;
		private ReadBack hook = null;
		private boolean backByUI = true;
		
		public Action() {
			setWork(act->{				
				flush_stream();
				writeTxt(w_data);
				index_reading(this);
				check_repeat(act);
			});
		}
		
		public Action writeData(final String data) {
			w_data = data;
			return this;
		}
		public Action indexOfData(
			final String tail,
			final ReadBack callback
		) {
			return indexOfData("", tail, callback);
		}
		public Action indexOfData(
			final String head, 
			final String tail,
			final ReadBack callback
		) {
			r_data[0] = head;
			r_data[1] = tail;
			hook = callback;
			return this;
		}
		
		public Action backbyUI(boolean flag){
			backByUI = flag;
			return this;
		}
		
		private void callback() {
			if(hook==null) {
				return;
			}
			if(backByUI==false) {
				hook.callback(
					this,
					stream.toString()
				);
			}else {
				Application.invokeAndWait(()->{
					hook.callback(
						this,
						stream.toString()
					);
				});
			}
			flush_stream();
		}
	};
	
	public interface ReadBack {
		void callback(final Action action, final String txt);
	};

	public interface Peek {
		void looper(byte[] buf, int cnt);
	};
	
	private Peek peek=null;
	
	public void setPeek(final Peek callback) {
		peek = callback;
	}
	
	protected final StringBuffer stream = new StringBuffer(512);
	
	protected String flush_stream() {
		String txt = stream.toString();
		stream.delete(0, stream.length());
		return txt;
	}
	
	private void buff_stream(
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
	
	@Override
	protected void wait_act(Task<?> looper) {
		byte[] buf = new byte[32];
		int cnt = implRead(buf,0,-1);
		buff_stream(buf,cnt);
	}
	//------------------------------------//
	
	private void index_reading(final Action act) {
		
		int idx=0, off=0, try_count = 10;
		boolean hasHead=(act.r_data[0].length()==0)?(true):(false);
		boolean hasTail=(act.r_data[1].length()==0)?(true):(false);
		
		byte[] buf = new byte[32];
		
		while(isLive()==true){
			
			if(try_count<0) {
				Misc.logw(
					"[%s] fail to indexof(%s,%s)", 
					TAG, act.r_data[0], act.r_data[1]
				);
				break;
			}
			
			int cnt = implRead(buf,0,-1);
			
			buff_stream(buf,cnt);
			
			if(cnt<=0) {
				try_count-=1;
			}
			
			if(act.r_data[0].length()!=0) {				
				idx = stream.lastIndexOf(act.r_data[0]);
				if(idx>=0) {					
					stream.delete(0, idx);
					hasHead = true;
				}
			}
			if(act.r_data[1].length()!=0) {
				idx = stream.lastIndexOf(act.r_data[1]);
				if(idx>=0) {					
					off = stream.length();
					stream.delete(
						idx+act.r_data[1].length(), 
						off
					);
					hasTail = true;
				}
			}			
			
			if(hasHead==true && hasTail==true) {
				act.callback();
				break;
			}
		}
	}
	
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
		readTxt("",tail,hook);
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
		take(new Action().indexOfData(head, tail, hook));
	}
	
	/**
	 * Fetch text by writing data.<p>
	 * @param data - write command
	 * @param head - reading text which match head
	 * @param tail - reading text which match head
	 * @param hook - callback function
	 */
	public void fetchTxt(
		final String data,
		final String head, 
		final String tail,
		final ReadBack hook
	) {
		take(new Action()
			.writeData(data)
			.indexOfData(head, tail, hook)
		);
	}
	
	/**
	 * Fetch text by writing data.<p>
	 * Just match tail data.<p>
	 * @param data - write command
	 * @param tail - reading text which match head
	 * @param hook - callback function
	 */
	public void fetchTxt(
		final String data,
		final String tail,
		final ReadBack hook
	) {
		take(new Action()
			.writeData(data)
			.indexOfData("", tail, hook)
		);
	}
	
	public byte readByte() {
		byte[] buf = {0};
		do {
			int cnt = implRead(buf,0,1);
			if(cnt>0) {
				break;
			}
			Misc.delay(3);
		}while(isLive()==true);
		return buf[0];
	}	
	
	public void readBuff(byte[] buf, int len) {
		int off = 0;
		if(len<0) {
			len = buf.length;
		}
		do {
			int cnt = implRead(buf,off,len);
			if(cnt>0) {
				off = off + cnt;
				len = len - cnt;
				continue;
			}else if(cnt==0) {
				break;
			}
			Misc.delay(3);
		}while(isLive()==true);
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

	public void writeByte(byte[] buf, int off, int len){
		implWrite(buf,off,len);
	}
	
	public void writeByte(byte[] buf, int len){
		implWrite(buf,0,len);
	}
	
	/**
	 * Write byte data via terminal-port.<p>
	 * @param buf - context data
	 */
	public void writeByte(byte... buf){
		implWrite(buf,0,-1);
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

	private native int implRead(byte[] buf, int off, int cnt);
	
	private native int implWrite(byte[] buf, int off, int cnt);
	
	private native void implClose();
}
