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

	public void open(DevTTY tty) {
		port = tty.port;//we share the same line!!!!
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
			Misc.loge("[%s]", TAG, e.getMessage());
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
	//--------------------------------------------//
	
	protected static final byte STX = 0x02;
	protected static final byte ETX = 0x03;
	protected static final byte EOT = 0x04;//end of transmission
	protected static final byte ENQ = 0x05;//enquiry
	protected static final byte ACK = 0x06;
	protected static final byte NAK = 0x15;
	
	protected static final byte LF = 0x0A;//enquiry
	protected static final byte CL = 0x0C;//enquiry
	protected static final byte CR = 0x0D;//enquiry
	
	protected static final byte DLE = 0x10;//跳出資料通訊
	protected static final byte PER = 0x25;//I guess, device denied host answer

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
		byte[] buf;
		if(values!=null) {
			//provide data~~~
			buf = new byte[10+count*2+3];
		}else {
			//read data~~~
			buf = new byte[10+3];
		}
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
	
	/**
	 * Sender  (device or host): STX,___,XXXXX,DLE,ETX,BCC,___<p>
	 * Receiver(device or host): ___,DLE,_____,___,___,___,DLE<p>
	 * @param dev - serial port
	 * @return
	 */
	protected byte[] protocol_3964R_listen(
		final SerialPort dev,
		int data_size
	){
		//-------------bracket--------------//
		try {
			byte cc = dev.readBytes(1)[0];
			if(cc!=STX) {
				Misc.loge("[3964R_listen]: no STX(x%02X)",((int)cc)&0xFF);
				block_sleep_sec(1);
				return null;
			}
			dev.writeByte(DLE);//ready to listen something~~~~
		} catch (SerialPortException e) {
			Misc.loge("[3964R_listen]: tty broken");
			return null;
		}
		//-------------bracket--------------//
		byte[] head=null, info=null, data=null, tail=null;
		try {
			head = dev.readBytes(4);			
			if(head[3]=='D'){
				info = dev.readBytes(6);
				data_size = byte2int(info[2],info[3]);//re-assign!!!
				//Token  : 00, 00
				//command: AD or ED
				//address: 2 byte
				//count  : 2 byte
				//reserve: 0xFF, 0xFF
				if(head[2]=='A'){
					data = dev.readBytes(data_size*2);
				}else if(head[2]=='E'){
					data = null;
				}else{
					Misc.loge("[3964R_listen]: either AD nor ED package");
					Misc.dump_byte(head);
					block_sleep_sec(1);
					return null;
				}
			}else if(
				head[0]==0 && head[1]==0 &&
				head[2]==0 && head[3]==0
			){
				//Token  : 00, 00, 00
				//Error  : 00
				if(data_size>0) {
					data = dev.readBytes(data_size*2);
				}else{
					data = null;
				}
			}else{
				Misc.loge("[3964R_listen]: unknown head");
				Misc.dump_byte(head);
				block_sleep_sec(1);
				return null;
			}
			
			tail = dev.readBytes(3);
			if(tail[0]!=DLE || tail[1]!=ETX) {
				ArrayList<Byte> buf = new ArrayList<Byte>();
				buf.add(tail[0]);
				buf.add(tail[1]);
				buf.add(tail[2]);
				for(;;){
					buf.add(dev.readBytes(1,50)[0]);
					final int tt = buf.size() - 1;
					if(buf.get(tt-2)==DLE && buf.get(tt-1)==ETX) {
						break;
					}
				}
				tail = Misc.list2byte(buf);
			}			
		} catch (SerialPortTimeoutException e) {
			Misc.loge("[3964R_listen]: tail-DLE timeout");
		} catch (SerialPortException e) {
			Misc.loge("[3964R_listen]: tty broken");
			return null;
		}
		//-------------bracket--------------//
		try {
			dev.writeByte(DLE);
		} catch (SerialPortException e) {
			Misc.loge("[3964R_listen]: tty broken");
		}
		return Misc.chainBytes(head,info,data,tail);
	}
	
	/**
	 * Sender  (device or host): ___,DLE,_____,___,___,___,DLE<p>
	 * Receiver(device or host): STX,___,XXXXX,DLE,ETX,BCC,___<p>
	 * @param dev - serial port
	 * @return
	 */
	protected int protocol_3964R_express(
		final SerialPort dev,
		final byte[] pkg
	){
		if(pkg==null) { return 0; }
		//-------------bracket--------------//
		try {
			dev.writeByte(STX);
			byte cc = dev.readBytes(1)[0];
			if(cc==PER) {				
				dev.purgePort(SerialPort.PURGE_RXCLEAR|SerialPort.PURGE_TXCLEAR);
				Misc.loge("[3964R_express]: %%purge%%");
				return -1;
			}else if(cc!=DLE) {
				Misc.loge("[3964R_express]: no head-DLE(x%02X)",(int)cc);
				return -2;
			}
		} catch (SerialPortException e) {
			Misc.loge("[3964R_express]: tty broken");
			return -10;
		}
		//-------------bracket--------------//
		try {
			dev.writeBytes(pkg);
		} catch (SerialPortException e) {
			Misc.loge("[3964R_express]: tty broken");
			return -10;
		}
		//-------------bracket--------------//
		try {
			byte cc = dev.readBytes(1,1000)[0];
			if(cc!=DLE) {
				Misc.loge("[3964R_express]: no tail-DLE");
				dev.purgePort(SerialPort.PURGE_TXABORT|SerialPort.PURGE_RXABORT);
				block_sleep_sec(1);
				return -4;
			}
		} catch (SerialPortTimeoutException e) {
			Misc.loge("[3964R_express]: tail-DLE timeout");
			return -5;
		} catch (SerialPortException e) {
			Misc.loge("[3964R_express]: tty broken");
			return -10;
		}
		return 0;
	}
	
	protected static int byte2int(final byte aa, final byte bb) {
		final int _a = (int)aa;
		final int _b = (int)bb;
		return ((_a&0x00FF)<<8) | (_b&0x00FF);
	}	
	//--------------------------------------------//
	
	/* implement AE BUS protocol.<p>
	 * Format:<p>
	 * <Header><Command>[Option length]<Data1>...<Checksum>
	 * Header ==> bit7~3: address, bit2~1:length.<p>
	 * Parameter is little-endian.<p>
	 * */	
	protected byte[] AE_bus2(
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
		
		//start to send payload!!!
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
			byte rr;
			rr = dev.readBytes(1)[0];
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
				Misc.logw("[%s] AE_Bus: invalid-checksum x%02X!=x%02X",TAG,rr,chks);
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
