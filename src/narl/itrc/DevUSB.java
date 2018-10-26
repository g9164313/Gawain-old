package narl.itrc;

/**
 * Connection wrapper for USB.<p>
 * In Unix, the implement is 'libusb-1.0' ().<p>
 * In Windows, the implement will be UMDF or WinUSB (not sure).<p>
 * Backward compatibility is also essential.<p>
 * This class will list all end-points and let use access them.<p>
 * @author qq
 *
 */
public class DevUSB {

	/**
	 * product and vendor identify for USB device.
	 */
	private int iden = -1;
	
	/**
	 * list of 'output' end-points.<p>
	 * User will write data to these end-points.<p>
	 * one-row list ends-points in same interfaces.<p> 
	 */
	private long[][] end0 = null;
	
	/**
	 * list of 'input' end-points.<p>
	 * User will read data to these end-points.<p>
	 * one-row list ends-points in same interfaces.<p> 
	 */
	private long[][] end1 = null;
	
	/**
	 * device handle for native code.<p>
	 */
	private long handle = 0;
	
	/**
	 * interface array pointer in active configuration
	 */
	private long face = 0;
	
	/**
	 * the number of interfaces in active configuration
	 */
	private int numFace = 0;
	
	private void set_id(int vid, int pid){
		iden = (vid & 0xFFFF);
		iden = iden << 16;
		iden = iden | (pid & 0xFFFF);
	}
	
	public DevUSB(int vid, int pid){
		set_id(vid,pid); 
	}
	
	public DevUSB(){
	}
	
	public void open(int vid, int pid){
		set_id(vid,pid);
		open();
	}
	
	public void open(){
		short vid = (short)((iden&0xFFFF0000)>>16);
		short pid = (short)((iden&0x0000FFFF));
		if(configure(vid,pid)==false){
			Misc.loge("[USB] Fail to connect %04X:%04X", vid, pid);
			return;
		}
		listEndpoint();
	}
	
	public int timeout = 200;
	
	public int write(int id, byte[] buffer){
		return write(id, buffer, -1, timeout);
	}
	
	public int write(int id, byte[] buffer, int length){
		return write(id, buffer, length, timeout);
	}
	
	public int write(int id, byte[] buffer, int length, int timeout){
		int ii = (id & 0xFF00) >> 8;
		int jj = (id & 0x00FF);
		return last_result(bulkTransfer(end0[ii][jj], buffer, length, timeout));
	}
	
	public int read(int id, final byte[] buffer){
		return read(id, buffer, -1, timeout);
	}
	
	public int read(int id, final byte[] buffer, int length){
		return read(id, buffer, length, timeout);
	}
	
	public int read(int id, final byte[] buffer, int length, int timeout){
		int ii = (id & 0xFF00) >> 8;
		int jj = (id & 0x00FF);
		return last_result(bulkTransfer(end1[ii][jj], buffer, length, timeout));
	}
	
	/**
	 * each transfer result will be kept in this variable.<p>
	 * This variable is changed by native-code.<p>
	 */
	private int lastResult = 0;
	
	private int last_result(int pass){
		//see values in 'libusb.h'
		switch(lastResult){
		case  -1: Misc.logv("I/O Error"); break;
		case  -2: Misc.logv("Invalid parameter"); break;
		case  -3: Misc.logv("Access denied"); break;
		case  -4: Misc.logv("No such device"); break;
		case  -5: Misc.logv("Entity not found"); break;
		case  -6: Misc.logv("Resource busy"); break;
		case  -7: Misc.logv("Operation timed out"); break;
		case  -8: Misc.logv("Overflow"); break;
		case  -9: Misc.logv("Pipe error"); break;
		case -10: Misc.logv("System call interrupted"); break;
		case -11: Misc.logv("Insufficient memory"); break;
		case -12: Misc.logv("Operation not supported"); break;
		case -99: Misc.logv("Other error"); break;
		}
		return pass;
	}
	
	public void close(){
		release();
	}	
	
	/**
	 * this method must be invoked for initialized.<p>
	 * @param vid - vendor identify
	 * @param pid - product identify
	 */
	private native boolean configure(short vid, short pid);
	
	private native void listEndpoint();
	
	private native int bulkTransfer(long endpoint, byte[] buffer, int length, int timeout);
	
	private native void release();
}
