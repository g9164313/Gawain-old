package prj.sputter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
		USE_ASCII = true;//default option~~~
		if(arg.length>=2) {
			final String arg1 = arg[1].toLowerCase();
			if(arg1.startsWith("asc")==true) {
				USE_ASCII = true;
			}else if(arg1.startsWith("bin")==true) {
				USE_ASCII = false;
			}
		}
		if(inet.length!=2) {
			Misc.logw("[%s][open] invalid path - %s", TAG, path);
			return;
		}
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
		if(ss==null) { return; }
		try {
			ss.close();
			ss = null;
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
	//--------------------------------------//
	
	private final static String STG_IGNITE = "ignite";
	private final static String STG_LOOPER = "looper";
	
	private boolean USE_ASCII = true;//ASCII or binary code....	
	private String host ="";
	private int port = 5556;	
	private int monitor_t = 10;//monitoring timer, default~~~
	
	@SuppressWarnings("unused")
	private String name = "";//model name
	
	private Socket ss = null;
	
	protected void ignite() {
		try {
			ss = new Socket(host,port);			
			//name = exec_get_name();
			//testing!!!
			exec_write("Y000",true,true,false,false);
			
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
		try {
			TimeUnit.MILLISECONDS.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		/*Application.invokeLater(()->{
			for(int i=0; i<node.prop.length; i++) {
				Object obj = node.prop[i];
				if(node.is_bit()==true) {
					SimpleBooleanProperty prop = (SimpleBooleanProperty)obj;
					prop.set(val[i]!=0);
				}else if(node.is_val()==true) {
					SimpleIntegerProperty prop = (SimpleIntegerProperty)obj;
					prop.set(val[i]);
				}
			}
		});*/
	}
	//---------------------------------------
	
	private class MCPack {
		final int shead;
		final int pc_no;
		int s_code = 0;//same as subheader, but Bit7 is'1'
		int c_code = 0;//complete or abnormal code
		
		MCPack(
			final int subheader,
			final int pc_number
		){
			shead = subheader;
			pc_no = pc_number;
		}
		MCPack(
			final int subheader
		){
			this(subheader,0xFF);
		}
		
		int d_code = -1;//device head(code)
		int d_numb = -1;//device name(number)
		int d_size = 0;//device points
		/* In MITSUBISHI PLC,
		 * 'device' means a node for memory-map.<p>
		 * 'point' means bit.<p>
		 * Bit device(1-point): X,Y,M,S,T(contact),C(contact) <p>
		 * Word device(16-point): t(value),c(value),D,R <p>
		 */
		MCPack askDevice(final String memo) {
			if(memo.matches("[XYMSTCDRtc]\\d{1,4}+([-]\\d{1,4}+)?+")==false) {
				Misc.logv("[%s] invalid text for device code and number", memo);
				return this;
			}
			String[] addr = memo.substring(1).split("-");
			if(addr.length==1) {
				d_numb = Integer.parseInt(addr[0]);
				if(d_size==0) {
					d_size = 1;//override. be careful device values!!!
				}				
			}else {
				final int d_num2 = Integer.parseInt(addr[1]);
				d_numb = Integer.parseInt(addr[0]);				
				d_size = d_num2 - d_numb + 1;
			}
			
			switch(memo.charAt(0)) {
			case 'd':
			case 'D': 
				d_code = 0x4420;
				if(d_numb>=8000) { d_numb += 0x1F40; }
				break;
			case 'r':
			case 'R': d_code = 0x5220; break;
			case 't': d_code = 0x544E; break;//value(timer, 16-bit)
			case 'c': //value(counter, 16-bit)
				d_code = 0x434E;
				if(d_numb>=200) { d_numb += 0x00C8; }
				break;
			
			case 'T': d_code = 0x5453; break;//contact (timer)
			case 'C': //contact (counter)
				d_code = 0x4353;
				if(d_numb>=200) { d_numb += 0x00C8; }
				break;
			case 'X': d_code = 0x5820; break;
			case 'Y': d_code = 0x5920; break;
			case 'M': 
				d_code = 0x4D20;
				if(d_numb>=8000) { d_numb += 0x1F40; }
				break;
			case 'S': d_code = 0x5320; break;		
			}			
			return this;
		}
		
		int[] d_vals = null;
		MCPack askDevice(final String memo,final int... vals) {
			if(memo.matches("[XYMSTCDRtc]\\d{1,4}+")==false) {
				Misc.logv("[%s] invalid text for device values", memo);
				return this;
			}
			d_vals = vals;
			d_size = vals.length;
			return askDevice(memo); 
		}
		MCPack askDevice(final String memo,final boolean... vals) {
			int[] _v = new int[vals.length];
			for(int i=0; i<vals.length; i++) {
				_v[i] = (vals[i]==true)?(1):(0);
			}
			return askDevice(memo, _v); 
		}
		
		byte[] gen_head() {
			if(USE_ASCII==true) {
				return String.format(
					"%02X%02X%04X", 
					(shead&0xFF), 
					(pc_no&0xFF), 
					(monitor_t&0xFFFF)
				).getBytes();
			}else {
				return new byte[] {
					Misc.maskInt0(shead),
					Misc.maskInt0(pc_no),
					Misc.maskInt0(monitor_t),
					Misc.maskInt1(monitor_t)
				};
			}
		}
		byte[] gen_ctxt() {
			if(d_size==0) {
				return null;
			}
			if(USE_ASCII==true) {
				return String.format(
					"%04X%08X%02X00", 
					(d_code&0xFFFF), 
					(d_numb&0xFFFF), 
					(d_size&0xFF)
				).getBytes();
			}else {
				return new byte[] {
					Misc.maskInt0(d_numb),
					Misc.maskInt1(d_numb),
					Misc.maskInt2(d_numb),
					Misc.maskInt3(d_numb),
					Misc.maskInt0(d_code),
					Misc.maskInt1(d_code),
					Misc.maskInt0(d_size),
					0
				};
			}
		}
		byte[] gen_vals() {
			if(d_vals==null) {
				return null;
			}
			byte[] buff = null;	
			switch(shead) {
			case 0x02://write in bit(02H)
				if(USE_ASCII==true) {
					for(int i=0; i<d_size; i++) {
						buff = Misc.chainBytes(
							buff,
							String.format("%1X", d_vals[i]).getBytes()
						);
					}
				}else {
					buff = new byte[(d_size*4)/8];
					for(int i=0; i<d_size; i+=2) {
						final int aa = (d_vals[i*2+0])&0x0F;
						final int bb = (d_vals[i*2+1])&0x0F;
						buff[i/2] = (byte)((aa<<4)|bb);
					}
				}
				break;
			case 0x03://write in word(03H)
				if(USE_ASCII==true) {
					for(int i=0; i<d_size; i++) {
						buff = Misc.chainBytes(
							buff,
							String.format("%04X", d_vals[i]).getBytes()
						);
					}
				}else {
					buff = new byte[d_size*2];
					for(int i=0; i<d_size; i++) {
						buff[i*2+0] = Misc.maskInt0(d_vals[i]);
						buff[i*2+1] = Misc.maskInt1(d_vals[i]);
					}
				}
				break;
			case 0x04://test in bit(random write??)
				break;
			default:
				return null;
			}
			return buff;
		}
		
		void sendPayload(OutputStream ss) throws IOException {
			final byte[] head = gen_head();
			final byte[] ctxt = gen_ctxt();
			final byte[] vals = gen_vals();
			ss.write(Misc.chainBytes(head,ctxt,vals));
		}
		
		void get_complete(InputStream ss) throws IOException {
			byte[] buff;
			if(USE_ASCII==true) {
				buff = new byte[4];
				ss.read(buff);
				s_code = Integer.valueOf(""+(char)(buff[0])+(char)(buff[1]), 16);
				c_code = Integer.valueOf(""+(char)(buff[2])+(char)(buff[3]), 16);				
			}else {
				buff = new byte[2];
				ss.read(buff);
				s_code = Misc.byte2int(buff[0]);
				c_code = Misc.byte2int(buff[1]);
			}
		}		
		void get_abnormal(InputStream ss) throws IOException {
			byte[] buff;
			//Changed complete-code to abnormal-code
			if(USE_ASCII==true) {
				buff = new byte[4];
				ss.read(buff);
				c_code = Integer.valueOf(""+(char)(buff[0])+(char)(buff[1]), 16);				
			}else {
				buff = new byte[2];
				ss.read(buff);
				c_code = Misc.byte2int(buff[0]);
			}
		}

		byte[] reply = null;
		void recvReply(InputStream ss) throws IOException {
			get_complete(ss);
			if(c_code!=0) {
				get_abnormal(ss);
				return;
			}
			int count = 0;
			switch(s_code) {
			case 0x80+0x00://read in bits, four-bits as 1-value
				count = (d_size*4)/8;
				break;
			case 0x80+0x01://read in word, one-bits as 1-value
				count = (d_size*16)/8;
				break;
			case 0x80+0x15://get CPU name
				count = 2;
				break;
			}
			if(count==0) { 
				reply = null;
				return;
			}
			if(USE_ASCII==true) {
				reply = new byte[count*2];
			}else {
				reply = new byte[count];
			}
			ss.read(reply);
		}
		
		int reply2int(int offset) {
			if(reply==null) {
				Misc.logv("[%s][reply2int] no reply buffer", TAG);
				return -1;
			}
			if(USE_ASCII==true) {
				if((offset*2+1)>=reply.length) {
					Misc.logv("[%s][reply2int] outbound!!", TAG);
					return -1;
				}				
				return Integer.valueOf(""+
					(char)(reply[offset*2+0])+
					(char)(reply[offset*2+1]), 16
				);
			}else {
				if(offset>=reply.length) {
					Misc.logv("[%s][reply2int] outbound!!", TAG);
					return -1;
				}
				return (int)(reply[offset]);
			}
		}
	};
	
	/**
	 * transmit message and receive response.<p>
	 * @return message response
	 */
	private MCPack transmit(MCPack pack) {	
		try {
			pack.sendPayload(ss.getOutputStream());
			pack.recvReply(ss.getInputStream());
		} catch (IOException e) {
			Misc.loge("[%s][transmit] %s", TAG, e.getMessage());	
		}
		return pack;
	}
	//--------------------------------------------------
	
	private int exec_read(final String txt) {
		MCPack mp = transmit(new MCPack(0x00).askDevice(""));
		return mp.c_code;
	}
	
	private int exec_write(final String txt,final boolean... flg) {
		return transmit(new MCPack(0x02).askDevice(txt,flg)).c_code;
	}
	
	private int exec_run(final boolean flg) {
		return transmit(new MCPack((flg==true)?(0x13):(0x14))).c_code;	
	}
	
	private String exec_get_name() {
		MCPack mp = transmit(new MCPack(0x15));
		final int code = mp.reply2int(0);
		switch(code) {
		case 0xF5: return "FX3S";
		case 0xF4: return "FX3G/FX3GC";
		case 0xF3: return "FX3U/FX3UC";
		}
		return String.format("%02XH", code);
	}

	@SuppressWarnings("unused")
	private void exec_loopback() {
		final byte[] ctxt = {
			4,
			0x55, (byte)0xAA, 0x55, (byte)0xAA
		};
		
		Misc.loge("[%s][exec_loopback] %s", TAG);
	}
}
