package narl.itrc;

import java.util.ArrayList;
import java.util.Optional;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

public abstract class DevTTY extends DevBase {

	protected Optional<SerialPort> port = Optional.empty();
	
	@Override
	public void open() {
		final String prop = Gawain.prop().getProperty(TAG, "");
		if(prop.length()==0) {
			Misc.logw("No default tty name...");
			return;
		}
		open(prop);
		afterOpen();
	}

	@Override
	public void close() {
		if(port.isPresent()==false) {
			return;
		}
		beforeClose();
		try {
			port.get().closePort();			
		} catch (SerialPortException e) {				
			e.printStackTrace();
		}
		port = Optional.empty();
	}

	@Override
	public boolean isLive() {
		if(port.isPresent()==false) {
			return false;
		}
		return port.get().isOpened();
	}

	public void open(final String name) {
		if(port.isPresent()==true) {
			return;
		}
		try {			
			final tty_name tty = new tty_name(name);			
			final SerialPort dev = new SerialPort(tty.path);
			dev.openPort();
			dev.setParams(
				tty.baudrate,
				tty.databit,
				tty.stopbit,
				tty.parity
			);			
			port = Optional.of(dev);			
		} catch (SerialPortException e) {
			e.printStackTrace();
		}
	}
		
	public abstract void afterOpen();
	public abstract void beforeClose();
	
	private static class tty_name {
		
		public String path = "";
		public int baudrate= SerialPort.BAUDRATE_9600;
		public int databit = SerialPort.DATABITS_8;
		public int stopbit = SerialPort.STOPBITS_1;
		public int parity  = SerialPort.PARITY_NONE;
		/*
		 * Open tty device and parser name text.<p>
		 * Format is "[device name/path]:[baud rate],[data bits][mask][stop bit]".<p>
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
		 */
		public tty_name(final String txt) {
			if(txt.matches("^[\\/\\w]{3,}[:,][\\d]{3,}[,][5678][noems][123]")==false) {
				Misc.loge("invalid tty name pattern: %s", txt);
				return;
			}			
			String[] val = txt.split("[:,]");
			if(val.length!=3) {
				return;
			}
			
			path = val[0];//let jssc decide whether name is valid~~~

			int baud = Integer.valueOf(val[1]);
			switch(baud){
			case    300: baudrate = SerialPort.BAUDRATE_300; break;
			case    600: baudrate = SerialPort.BAUDRATE_600; break;
			case   1200: baudrate = SerialPort.BAUDRATE_1200; break;
			case   2400: baudrate = SerialPort.BAUDRATE_2400; break;
			case   4800: baudrate = SerialPort.BAUDRATE_4800; break;
			case   9600: baudrate = SerialPort.BAUDRATE_9600; break;
			case  14400: baudrate = SerialPort.BAUDRATE_14400; break;
			case  19200: baudrate = SerialPort.BAUDRATE_19200; break;
			case  38400: baudrate = SerialPort.BAUDRATE_38400; break;
			case  57600: baudrate = SerialPort.BAUDRATE_57600; break;
			case 115200: baudrate = SerialPort.BAUDRATE_115200; break;
			case 128000: baudrate = SerialPort.BAUDRATE_128000; break;
			case 256000: baudrate = SerialPort.BAUDRATE_256000; break;
			default:
				baudrate = baud;
				Misc.loge("invalid baudrate value: %d", baud);
				break;
			}
						
			final char[] attr = val[2].toLowerCase().toCharArray();
			switch(attr[0]) {
			case '5': databit = SerialPort.DATABITS_5; break;
			case '6': databit = SerialPort.DATABITS_6; break;
			case '7': databit = SerialPort.DATABITS_7; break;
			case '8': databit = SerialPort.DATABITS_8; break;
			}
			switch(attr[1]) {
			case 'n': parity = SerialPort.PARITY_NONE; break;
			case 'o': parity = SerialPort.PARITY_ODD;  break;
			case 'e': parity = SerialPort.PARITY_EVEN; break;
			case 'm': parity = SerialPort.PARITY_MARK; break;
			case 's': parity = SerialPort.PARITY_SPACE;break;
			}
			switch(attr[2]) {
			case '1': stopbit = SerialPort.STOPBITS_1; break;
			case '2': stopbit = SerialPort.STOPBITS_2; break;
			case '3': stopbit = SerialPort.STOPBITS_1_5; break;
			}
		}
	};
	//-----------------------------------------
	
	private static final byte STX = 0x02;
	private static final byte ETX = 0x03;
	private static final byte DLE = 0x10;//跳出資料通訊
	
	private static final byte ACK = 0x06;
	private static final byte NAK = 0x15;
	
	private int checksum(
		final byte[] data, 
		final int start, 
		final int length
	){
		int bcc = 0x00;		
		for(int i=start; i<length; i++){
			bcc = bcc ^ ((int)data[i] & 0xFF);
		}
		return bcc;
	}
	
	protected byte[] RK512_package(
		final int address,
		int count,
		int... values
	){
		if(count<=0 && values!=null) {
			count = values.length;
		}
		byte[] buf = new byte[10+count*2+3];
		//RK512 header
		buf[0] = 0x00;//Token 
		buf[1] = 0x00;//Token
		if(values!=null) {
			buf[2] = (byte)'A';//Command 
			buf[3] = (byte)'D';//Command
		}else {
			buf[2] = (byte)'E';//Command 
			buf[3] = (byte)'D';//Command
		}
		buf[4] = (byte)((address&0xFF00)>>8);
		buf[5] = (byte)((address&0x00FF)>>0);
		buf[6] = (byte)((count&0xFF00)>>8);
		buf[7] = (byte)((count&0x00FF)>>0);
		buf[8] = (byte)(0xFF);//Reserver
		buf[9] = (byte)(0xFF);//Reserver
		if(values!=null) {
			for(int i=0; i<count; i++) {
				final int v = values[i];
				buf[10+i*2] = (byte)((v&0xFF00)>>8);
				buf[11+i*2] = (byte)((v&0x00FF)>>0);
			}
		}		
		buf[buf.length-3] = DLE;
		buf[buf.length-2] = ETX;
		buf[buf.length-1] = (byte)checksum(buf,0,buf.length-1);
		return buf;
	}
	
	protected int[] RK512_unpack_AD(final byte[] buf){
		final int[] empty = new int[] {-1};
		if(buf.length<=10) {
			Misc.dump_byte(buf);
			Misc.logw("[%s] invalid AD packet",TAG);
			return empty;
		}
		if(buf[0]!=0 || buf[1]!=0 || buf[2]!='A' || buf[3]!='D'){
			Misc.dump_byte(buf);
			Misc.logw("[%s] invalid AD packet",TAG);
			return empty;
		}
		final int size = ((((int)buf[6])&0xFF)<<8) + (((int)buf[7])&0xFF);
		int[] val = new int[1+size];//first is address~~~
		val[0] =  ((((int)buf[4])&0xFF)<<8) + (((int)buf[5])&0xFF);
		for(int i=0; i<size; i++){
			val[1+i] = ((((int)buf[10+i*2+0])&0xFF)<<8) + (((int)buf[11+i*2])&0xFF);			
		}
		return val;
	}
	
	protected byte[] protocol_3964R_listen(final int timeout_ms) throws 
		SerialPortException, SerialPortTimeoutException 
	{
		final SerialPort dev = port.get();
		if(dev.isOpened()==false) {
			return new byte[0];
		}
		ArrayList<Byte> buf = new ArrayList<Byte>();
		byte[] cc;
		if(timeout_ms>0) {
			cc = dev.readBytes(1,timeout_ms);
		}else {
			cc = dev.readBytes(1);
		}
		if(cc[0]==STX) {				
			dev.writeByte(DLE);//host give conversation
			do {
				//buf[0:2]--> token
				//buf[  3]--> error code
				//buf[...]--> data or nothing
				//buf[ -3]--> DLE
				//buf[ -2]--> ETX
				//buf[ -1]--> checksum
				cc = dev.readBytes(2);
				buf.add(cc[0]);
				buf.add(cc[1]);			
			}while(!(cc[0]==DLE && cc[1]==ETX));
			cc[0] = dev.readBytes(1)[0];
			buf.add(cc[0]);//BBC, last byte is checksum!!!
			dev.writeByte(DLE);//host close conversation
		}else {
			//what else happened!!!!
			Misc.loge("[%s] protocol_3964R_listen - unknown response(%d)",TAG, (int)cc[0]);
		}
		return Misc.list2byte(buf); 
	}	
	
	protected boolean protocol_3964R_reveal(
		final int addr, 
		final int cnt,
		int... values
	) {
		final SerialPort dev = port.get();
		if(dev.isOpened()==false) {
			return false;
		}		
		try {
			//host start to conversation
			dev.writeByte(STX);			
			try {
				//host close conversation
				byte cc = dev.readBytes(1,100)[0];
				if(cc!=DLE) {
					Misc.loge("[%s] protocol_3964R_reveal - invalid respose (%d)", TAG, (int)cc);
					return false;
				}
			} catch (SerialPortTimeoutException e) {
				Misc.loge("[%s] protocol_3964R_reveal - timeout.2", TAG);
				return false;
			}			
			//host send package~~~
			dev.writeBytes(RK512_package(addr,cnt,values));			
			try {
				//host close conversation
				byte cc = dev.readBytes(1,100)[0];
				if(cc!=DLE) {
					Misc.loge("[%s] protocol_3964R_reveal - invalid respose (%d)", TAG, (int)cc);
					return false;
				}
			} catch (SerialPortTimeoutException e) {
				Misc.loge("[%s] protocol_3964R_reveal - timeout.2", TAG);
				return false;
			}			
		} catch (SerialPortException e) {
			Misc.loge("[%s] %s", TAG, e.getMessage());
			return false;
		}		
		return true;
	}
	
	
	/* implement AE BUS protocol.<p>
	 * Format:<p>
	 * <Header><Command>[Option length]<Data1>...<Checksum>
	 * Header ==> bit7~3: address, bit2~1:length.<p>
	 * Parameter is little-endian.<p>
	 * */	
	protected byte[] AE_bus(
		final int address,
		final int command,
		final String hexData
	) {		
		final ArrayList<Byte> hex = new ArrayList<Byte>();
		if(hexData.length()!=0) {
			final String[] hex_data = hexData.trim().split("\\s+");
			for(String txt:hex_data) {
				txt = txt.trim();
				if(txt.matches("^[x]?[0-9A-F]+$")==true) {
					if(txt.charAt(0)=='x') {
						txt = txt.substring(1);
					}
					int val = Integer.valueOf(txt, 16);
					int cnt = txt.length();
					cnt = cnt/2 + cnt%2;
					for(int k=0; k<cnt; k++) {
						hex.add((byte)(val & (0xFF<<(8*k))));  
					}
				}else {
					Misc.logw("[%s] invalid hex: %s (%s)", TAG, txt, hexData);
				}
			}
		}		
		final int hex_count = hex.size();
		
		final byte[] send;		
		if(hex_count<=6) {
			send = new byte[1+1+hex_count+1];
			send[0] = (byte)((address&0x1F)<<3 | hex_count);
			for(int i=0; i<hex_count; i++) {
				send[2+i] = hex.get(i);
			}
		}else {
			send = new byte[1+1+1+hex_count+1];
			send[0] = (byte)((address&0x1F)<<3 | 7);
			send[2] = (byte)((hex.size() - 7) & 0xFF);
			for(int i=0; i<hex_count; i++) {
				send[3+i] = hex.get(i);
			}
		}			
		send[1] = (byte)(command&0xFF);
		
		//checksum - XOR all bytes
		final int last_index = send.length-1;
		for(int i=0; i<last_index; i++) {			
			send[last_index] = (byte)(send[last_index] ^ send[i]);
		}
				
		byte[] recv = new byte[0];
		if(port.isPresent()==false) {
			return recv;
		}
		final SerialPort dev = port.get();
		if(dev.isOpened()==false) {
			return recv;
		}
		try {
			dev.writeBytes(send);
			byte rr = dev.readBytes(1)[0];			
			if(rr==NAK || rr!=ACK) {
				Misc.logw("[%s] Host got NAK or none ACK", TAG);
				block_sleep_msec(25);//slow transmission
				return recv;
			}
			byte chks = 0;
			rr = dev.readBytes(1)[0];//header (5-bit address, 3-bit length)
			chks = (byte)(chks ^ rr);
			int cnt = (rr & 0x7);//decide length~~~						
			if(cnt>=7) {
				rr = dev.readBytes(1)[0];//optional length
				cnt = cnt + (rr&0xFF);
			}
			recv = new byte[cnt];			
			rr = dev.readBytes(1)[0];//command. skip it~~~
			chks = (byte)(chks ^ rr);
			//get all data according previous length~~~
			for(int i=0; i<cnt; i++) {
				rr = dev.readBytes(1)[0];
				chks = (byte)(chks ^ rr);
				recv[i] = rr;
			}
			//get checksum and check valid
			rr = dev.readBytes(1)[0];
			if(rr!=chks) {
				Misc.logw("[%s] checksum x%02X!=%02X",TAG,rr,chks);
			}
			//finally, echo to client~~~
			dev.writeByte(ACK);
			block_sleep_msec(50);//slow transmission
		} catch (SerialPortException e) {
			Misc.loge("[%s] AE_Bus: %s", TAG, e.getMessage());
		}
		return recv;
	}
	
	protected static int byte2int(
		final boolean big_endian,
		final byte... payload		
	) {
		int val = 0;
		switch(payload.length) {
		case 0:	break;
		case 1:
			val = (payload[0]&0xFF);
			break;
		case 2:
			if(big_endian==true) {
				val = ((payload[0]&0xFF)<<8) | ((payload[1]&0xFF)<<0);
			}else {
				val = ((payload[1]&0xFF)<<8) | ((payload[0]&0xFF)<<0);
			}			
			break;
		case 3:
			if(big_endian==true) {
				val = (
					((payload[0]&0xFF)<<16) | 
					((payload[1]&0xFF)<< 8) | 
					((payload[2]&0xFF)<< 0)
				);
			}else {
				val = (
					((payload[2]&0xFF)<<16) | 
					((payload[1]&0xFF)<< 8) | 
					((payload[0]&0xFF)<< 0)
				);				
			}
			break;
		default:
		case 4:
			if(big_endian==true) {
				val = (
					((payload[0]&0xFF)<<24) | 
					((payload[1]&0xFF)<<16) | 
					((payload[2]&0xFF)<< 8) | 
					((payload[3]&0xFF)<< 0)
				);
			}else {
				val = (
					((payload[3]&0xFF)<<24) | 
					((payload[2]&0xFF)<<16) | 
					((payload[1]&0xFF)<< 8) | 
					((payload[0]&0xFF)<< 0)
				);				
			}
			break;
		}
		return val;
	}
	
	protected void writeBytes(byte[] buf, int off, int len) throws SerialPortException {
		final SerialPort dev = port.get();
		byte[] bb = new byte[len];
		for(int i=0; i<len; i++) {
			int j = off + i;
			if(j>=buf.length) {
				j = j % buf.length;
			}
			bb[i] = buf[j];
		}
		dev.writeBytes(bb);
	}	
}
