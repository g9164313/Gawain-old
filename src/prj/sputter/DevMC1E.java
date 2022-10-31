package prj.sputter;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import narl.itrc.DevBase;
import narl.itrc.Gawain;
import narl.itrc.Misc;

/**
 * Implement MC protocol(1E frame).<p>
 * The MC protocol for the Ethernet adapter is a subset of A compatible 1E frames.<p>
 * '1E frame' may mean 'the first generate Ethernet communication'.<p>
 * MELSOFT(GX Developer else...) or HMI maybe use this to connect PLC controller.<p>
 * This protocol is used by MITSUBISHI ,PLC controller(FC3U).<p>
 */
public class DevMC1E extends DevBase {

	public DevMC1E(final String tag) {
		TAG = tag;
	}
	//--------------------------------------//
	
	private final static String STG_IGNITE = "ignite";
	private final static String STG_LOOPER = "looper";
	
	private boolean ASCII_code = false;//ASCII or binary code....
	
	private String host ="";
	private int port = -1;	
	private Socket ss = null;
	private int monitor = 10;//monitoring timer, default~~~
	
	protected void ignite() {
		try {
			ss = new Socket(host,port);
			exec_get_name();
			nextState(STG_LOOPER);
		} catch (UnknownHostException e) {
			Misc.loge("[%s][ignite] %s", TAG, e.getMessage());
			nextState("");
		} catch (IOException e) {
			Misc.loge("[%s][ignite] %s", TAG, e.getMessage());
			nextState("");
		}		
	}

	protected void looper() {
		if(mirror.size()==0) {
			block_sleep_msec(3000);
			return;
		}
		for(Node node:mirror) {
			
			block_sleep_msec(monitor+10);
		}
	}
		
	//In MITSUBISHI PLC, 
	//'device' means a node for memory-map.<p>
	//'point' means bit.<p>
	//Bit device(1-point): X,Y,M,S,T(contact),C(contact)
	//Word device(16-point): T(value),C(value),D,R		
	private class Node {
		final char name;//could be X, Y, M, S, T, C, D, R, t(value), c(value)
		final int addr, size;
		//final byte[]//device code and number~~~~
		Node(final String txt){
			if(txt.matches("[XYMSTCDRtc]\\d{1,4}+([-]\\d{1,4}+)?+")==false) {
				Misc.logw("[%s] Node: invalid --> \'%s\'", TAG, txt);
				name = ' '; addr = size = -1;
				return;
			}
			char dd = txt.charAt(0);			
			String[] col = txt.substring(1).split("-");
			int aa = Integer.parseInt(col[0]);
			int bb = 1 + aa;
			if(col.length==2) {
				bb = 1 + Integer.parseInt(col[1]);
				if(bb<aa) {
					Misc.logw("[%s] Node: wrong size --> \'%s\'", TAG, txt);
					bb = 1 + aa;
				}
			}
			if(check_model()==false) {
				Misc.logw("[%s] Node: no support --> \'%s\'", TAG, txt);
				name = ' '; addr = size = -1;
				return;
			}
			name = dd;
			addr = aa;
			size = bb - aa;
			
		}
		
		
		boolean check_model() {
			//TODO:~~~~
			return true;
		}
	};
	private ArrayList<Node> mirror = new ArrayList<Node>();
	
	/**
	 * create memory-map-node for PLC device.<p>
	 * list could be 'X1', 'Y10~15'...
	 * @param lst - list for parsing
	 * @return
	 */
	public DevMC1E map(final String... lst) {
		for(String txt:lst) {
			Node nn = new Node(txt);
			if(nn.name==' ') {
				continue;
			}
			mirror.add(nn);
		}
		
		return this;
	}


	private static final Map<Character, byte[]> dev_code = new HashMap<Character,byte[]>() {
		private static final long serialVersionUID = 1447453213611037350L;
	{
		put('D',new byte[] {0x44, 0x20});
		put('R',new byte[] {0x52, 0x20});
		put('t',new byte[] {0x54, 0x4E});
		put('T',new byte[] {0x54, 0x53});
		put('c',new byte[] {0x43, 0x4E});
		put('C',new byte[] {0x43, 0x53});
		put('X',new byte[] {0x58, 0x20});
		put('Y',new byte[] {0x59, 0x20});
		put('M',new byte[] {0x4D, 0x20});
		put('S',new byte[] {0x53, 0x20});
	}};
	
	private static final Map<Character, byte[]> mod_numb = new HashMap<Character,byte[]>() {
		private static final long serialVersionUID = -7736329965805542873L;
	{
		
	}};
	//---------------------------------------
	
	/**
	 * send message and receive response.<p>
	 * @return message response
	 */
	private void send(
		final int subheader,
		final int pc_number,
		final int monitoning,
		final byte[] content
	) {		
		try {
			byte[] payload;
			if(ASCII_code==true) {
				//use ASCII code
				String txt = String.format(
					"%02X%02X%04X", 
					(subheader&0xFF), 
					(pc_number&0xFF), 
					(monitoning&0xFFFF)
				);
				for(int i=0; i<content.length; i++) {
					txt = txt + String.format("%02X", content[i]);
				}
				payload = txt.getBytes();				
			}else {
				//use binary code
				byte[] mesg = new byte[] {
					(byte)(subheader&0xFF),
					(byte)(pc_number&0xFF),
					//long2byte(pc_number,0),//Little-endian
					//long2byte(pc_number,8)
				};
				payload = Misc.chainBytes(mesg,content);
			}
			ss.getOutputStream().write(payload);
		} catch (IOException e) {
			Misc.loge("[%s][send] %s", TAG, e.getMessage());	
		}
	}
	

	private class Recv {
		byte[] head = null;//sub-header & complete code (abnormal code)
		byte[] ctxt = null;//response~~~
		
		int subheader() {
			if(head==null) { return -1; }
			return (ASCII_code==true)?(
				Misc.hex2int(head[0],head[1])
			):(
				-1//TODO:byte2long(head[0],0)
			);
		}
		int complete() {
			if(head==null) { return -1; }
			return (ASCII_code==true)?(
				Misc.hex2int(head[2],head[3])
			):(
				-1//TODO:byte2long(head[1],0)
			);
		}
		int abnormal_code() {
			if(ctxt==null) { 
				return -1; 
			}
			if(ctxt.length==4 && ASCII_code==true) {
				return Misc.hex2int(ctxt[0],ctxt[1],ctxt[2],ctxt[3]);
			}
			if(ctxt.length==2 && ASCII_code==false) {
				//TODO:return byte2long(ctxt[0],8) | byte2long(ctxt[1],0);
			}
			return -1;
		}
		Recv putHeader(final Socket ss,final int count) throws IOException {
			//sub-header, complete-code
			head = new byte[count];
			ss.getInputStream().read(head);
			if(complete()!=0) {
				//abnormal code~~~
				ctxt = new byte[count];
				ss.getInputStream().read(ctxt);
				Misc.loge(
					"[%s] abnormal:(%02X, %02X, %04X)!!", 
					TAG, subheader(), complete(), abnormal_code()
				);				
			}
			return this;
		}
		Recv putContext(final Socket ss,final int count) throws IOException {
			if(count<=0) { return this; }
			ctxt = new byte[count];
			ss.getInputStream().read(ctxt);
			return this;
		}
		
		int get_u8(final int off) {			
			return buf2int(ctxt,off,1);
		}
		int get_u16(final int off) {
			return buf2int(ctxt,off,2);
		}
		//int get_u32(final int off) {
		//	return buf2int(ctxt,off,4);
		//}
	};
	
	private Recv recv(int count) {
		Recv recv = new Recv();
		try {
			if(count<=0) { count=0; }
			if(ASCII_code==true) {
				count *= 2;
				recv.putHeader(ss,4);
			}else {
				recv.putHeader(ss,2);
			}
			if(recv.complete()!=0) {
				return recv;
			}
			recv.putContext(ss, count);
		} catch (IOException e) {
			Misc.loge("[%s][send] %s", TAG, e.getMessage());	
		}
		return recv;
	}
	
	/**
	 * convert buffer data into integer.<p>
	 * @param buf - buffer array
	 * @param off - offset in buffer array
	 * @param size - byte length, u8-->1, u16-->2, u32-->4
	 * @return
	 */
	private int buf2int(final byte[] buf, final int off, int size) {
		size = (ASCII_code==true)?(size*2):(size);
		if((off+size)>=buf.length || off<0) {
			return -1;
		}
		if(ASCII_code==true) {
			switch(size) {
			case 2: return Misc.hex2int(buf,off,2);
			case 4: return Misc.hex2int(buf,off,4);
			case 8: return Misc.hex2int(buf,off,8);
			}
		}else {
			//little-endian~~~
			switch(size) {
			case 1: return (int)(pad_buf(buf[off+0],0));
			case 2: return (int)(pad_buf(buf[off+0],0) | pad_buf(buf[off+1],8));
			case 4: return (int)(
				pad_buf(buf[off+0],0 ) | pad_buf(buf[off+1],8 ) | 
				pad_buf(buf[off+2],16) | pad_buf(buf[off+3],24)
			);
			}
		}
		return -1;
	}
	private long pad_buf(final byte buf,final int shift) {
		return (((long)buf)&0xFF)<<shift;
	}
	private byte pad_val(final long val,final int shift) {
		return (byte)((val>>shift)&0xFF);
	}	
	
	private void exec_run(final boolean flg) {
		final int subheader = (flg==true)?(0x13):(0x14);
		send(subheader,0xFF,monitor,null);
		recv(-1);
	}
	
	private String exec_get_name() {
		send(0x15,0xFF,monitor,null);
		Recv rr = recv(2);//0xF3 --> FX3U/FX3UC
		if(rr.complete()!=0) {
			return "";
		}
		final int code = rr.get_u8(0);
		switch(code) {
		case 0xF5: return "FX3S";
		case 0xF4: return "FX3G/FX3GC";
		case 0xF3: return "FX3U/FX3UC";
		}
		return String.format("%XH", code);
	}
	
	private void exec_loopback() {
		final byte[] ctxt = {
			4,
			0x55, (byte)0xAA, 0x55, (byte)0xAA
		};
		send(0x16,0xFF,monitor,ctxt);
		Recv rr = recv(2);//0xF3 --> FX3U/FX3UC
		if(rr.complete()!=0) {
			Misc.loge("[%s] loopback is fail!!", TAG);
		}else {
			if(rr.ctxt.length!=ctxt.length) {
				Misc.loge("[%s] loopback context is wrong!!", TAG);
			}else {
				boolean flg = true;
				for(int i=0; i<ctxt.length; i++) {
					if(rr.ctxt[i]!=ctxt[i]) {
						flg = false;
						
						break;
					}
				}
				if(flg) {
					Misc.loge("[%s] loopback context is match!!", TAG);
				}else {
					Misc.loge("[%s] loopback context is no match!!", TAG);
				}
			}
		}
		;//rr.context[0];
	}	
	//--------------------------------------//
	
	@Override
	public void open() {
		final String path = Gawain.prop().getProperty(TAG, "");
		if(path.length()==0) {
			Misc.logw("[%s][open] no options",TAG);
			return;
		}
		open(path);
	}
	/**
	 * Connection argument format is below:
	 * [Ethernet address]:[port],[ascii/binary]
	 * @param arg
	 */
	public void open(final String path) {
		String[] arg = path.split(",");
		String[] inet = arg[0].split(":");
		try {
			host = inet[0];
			port = Integer.parseInt(inet[1]);
			addState(STG_IGNITE,()->ignite());
			addState(STG_LOOPER,()->looper());
			playFlow(STG_IGNITE);
		}catch(NumberFormatException e) {
			Misc.loge("[%s][open] %s", TAG, e.getMessage());
		}
	}
	
	@Override
	public void close() {
		if(ss==null) {
			return;
		}
		try {
			ss.close();
		} catch (IOException e) {
			Misc.loge("[%s][ignite] %s", TAG, e.getMessage());
		}
	}

	@Override
	public boolean isLive() {
		if(ss==null) {
			return false;
		}
		return ss.isConnected();
	}
}
