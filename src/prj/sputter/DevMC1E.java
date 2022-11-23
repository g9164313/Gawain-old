package prj.sputter;

import java.io.IOException;
import java.io.InputStream;
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
	
	private final static String STG_IGNITE = "ignite";
	private final static String STG_LOOPER = "looper";
	
	private boolean USE_BINARY = true;//ASCII or binary code....
	
	private String host ="";
	private int port = -1;	
	private Socket ss = null;
	private int monitor = 10;//monitoring timer, default~~~
	private String name = "";//model name
	
	protected void ignite() {
		try {
			ss = new Socket(host,port);
			name = exec_get_name();
			exec_loopback();//test~~~~
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
			final int[] val = exec_read(node);
			if(val==null) {
				continue;
			}			
			Application.invokeLater(()->{
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
			});
		}
	}
	
	//In MITSUBISHI PLC, 
	//'device' means a node for memory-map.<p>
	//'point' means bit.<p>
	//Bit device(1-point): X,Y,M,S,T(contact),C(contact)
	//Word device(16-point): t(value),c(value),D,R
	//looper will read these nodes according their address and points.
	private class Node {		
		final byte[] pay;
		final char name;
		final int address;
		final int points;//device points
		final Object[] prop;
		Node(final char dev_name, final int dev_address,final int dev_points){
			name   = dev_name;
			address= dev_address;			
			points = dev_points & 0xFF;				
			final int code = name2code(name);
			if(USE_BINARY==true) {
				pay = new byte[] {
					Misc.maskInt0(address),
					Misc.maskInt1(address),
					Misc.maskInt2(address),
					Misc.maskInt3(address),
					Misc.maskInt0(code),
					Misc.maskInt1(code),
					(byte)(points),
					(byte)(0x0)
				};
			}else {
				pay = String.format(
					"%04X%08X%02X00", 
					code, address, points
				).getBytes();		
			}
			prop= prepare_property();
		}
		boolean is_bit() {
			switch(name) {
			case 'X':
			case 'Y':
			case 'M':
			case 'S':
			case 'T'://Timer contact
			case 'C'://Counter contact
				return true;
			}
			return false;
		}
		boolean is_val() {
			switch(name) {
			case 'D':
			case 'R':
			case 'd':
			case 'r':
			case 't'://current value(16-bit)
			case 'c'://current value(16-bit)
				return true;
			}
			return false;
		}
		Object[] prepare_property() {
			Object[] lst = new Object[points];
			for(int i=0; i<lst.length; i++) {
				if(is_bit()==true) {
					lst[i] = new SimpleBooleanProperty();
				}else if(is_val()==true){
					lst[i] = new SimpleIntegerProperty();
				}
			}			
			return lst;
		}
		boolean contain(final int address) {
			if(address<=address && address<(address+points)) {
				return true;
			}
			return false;
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
			Node node = parsing_text(txt);
			if(node!=null) {
				mirror.add(node);
			}			
		}		
		return this;
	}
	public SimpleBooleanProperty bindBool(final int address) {				
		return (SimpleBooleanProperty)find_node(true,address);
	}
	public SimpleIntegerProperty bindInteger(final int address) {
		return (SimpleIntegerProperty)find_node(true,address);
	}
	private Object find_node(final boolean is_bool, final int address) {
		Object dst = null;
		for(Node node:mirror) {
			if(node.contain(address)==true) {
				dst = node.prop[address-node.address];
				break;
			}			
		}
		if(is_bool) {
			if(dst instanceof SimpleBooleanProperty) {
				return dst;
			}
			return new SimpleBooleanProperty();//dummy~~~~
		}else {
			if(dst instanceof SimpleBooleanProperty) {
				return dst;
			}
			return new SimpleIntegerProperty();//dummy~~~~
		}
	}
	
	private int name2code(final char cc) {
		int code = 0;
		switch(cc) {
		case 'd':
		case 'D': code = 0x4420; break;
		case 'r':
		case 'R': code = 0x5220; break;
		case 't': code = 0x544E; break;//current value(16-bit)
		case 'c': code = 0x434E; break;//current value(16-bit)
		
		case 'T': code = 0x5453; break;//contact		
		case 'C': code = 0x4353; break;//contact
		case 'X': code = 0x5820; break;
		case 'Y': code = 0x5920; break;
		case 'M': code = 0x4D20; break;
		case 'S': code = 0x5320; break;		
		}
		return code;
	}
	private Node parsing_text(final String txt) {
		if(txt.matches("[XYMSTCDRtc]\\d{1,4}+([-]\\d{1,4}+)?+")==false) {
			Misc.logw("[%s] Node: invalid --> \'%s\'", TAG, txt);
			return null;
		}
		//Device could be X, Y, M, S, T, C, D, R, t(value), c(value)
		//device type name~~~	
		final char cc = txt.charAt(0);
		//We don't check device number, this number is limited according model name.
		String[] col = txt.substring(1).split("-");
		int aa = Integer.parseInt(col[0]);
		int bb = 1 + aa;
		if(col.length==2) {
			bb = Integer.parseInt(col[1]);
		}		
		int numb = aa;
		if(aa>bb) {
			//swap address~~~
			numb= bb;
			bb  = aa;
		}else if(aa==bb) {
			bb = aa + 1;
		}
		return new Node(cc,numb,bb-aa);
	}
	
	//private static final Map<Character, byte[]> dev_code = new HashMap<Character,byte[]>() {
	//	private static final long serialVersionUID = 1447453213611037350L;
	//{
	//}};
	//---------------------------------------
	
	/**
	 * send message and receive response.<p>
	 * @return message response
	 */
	private byte[] send(
		final int subheader,
		final int pc_number,
		final int monitor_t,
		final byte[] content
	) {		
		try {
			byte[] payload;
			//Phase.1: send command and content
			if(USE_BINARY==true) {
				//use binary code
				byte[] mesg = new byte[] {
					Misc.maskInt0(subheader),
					Misc.maskInt0(pc_number),
					Misc.maskInt0(monitor_t),
					Misc.maskInt1(monitor_t)
				};				
				payload = Misc.chainBytes(mesg,content);
			}else {
				//use ASCII code
				String txt = String.format(
					"%02X%02X%04X", 
					(subheader&0xFF), 
					(pc_number&0xFF), 
					(monitor_t&0xFFFF)
				);
				for(int i=0; i<content.length; i++) {
					txt = txt + String.format("%02X", content[i]);
				}
				payload = txt.getBytes();
			}
			ss.getOutputStream().write(payload);

		} catch (IOException e) {
			Misc.loge("[%s][exec] %s", TAG, e.getMessage());	
		}
		return new byte[0];//dummy, and we fail....
	}
	private byte[] recv(final int size) {		
		try {
			InputStream stm = ss.getInputStream();
			byte[] s_head = null;//sub-header
			byte[] c_code = null;//complete code			
			if(USE_BINARY==true) {
				s_head = new byte[1];
				c_code = new byte[1];
			}else {
				s_head = new byte[2];
				c_code = new byte[2];
			}
			stm.read(s_head);
			stm.read(c_code);
			final int complete = (USE_BINARY==true)?(
				Misc.byte2int(c_code[0])
			):(
				Integer.valueOf(""+(char)(c_code[0])+(char)(c_code[1]), 16)
			);
			if(complete!=0) {
				byte[] a_code;
				if(USE_BINARY==true) {
					a_code = new byte[2];//last byte is dummy, 0					
				}else {
					a_code = new byte[4];//last two byte is dummy, '00'
				}
				stm.read(a_code);
				return Misc.chainBytes(s_head, c_code, a_code);
			}			
			byte[] contxt = null;
			if(size>0) {
				contxt = new byte[(USE_BINARY==true)?(size):(size*2)];
			}
			return Misc.chainBytes(s_head, c_code, contxt);
		} catch (IOException e) {
			Misc.loge("[%s][exec] %s", TAG, e.getMessage());	
		}
		return new byte[0];//dummy, and we fail....
	}
	private int is_complete(final byte[] pkg) {
		int s_head, c_code, a_code;
		if(USE_BINARY==true) {
			s_head = Misc.byte2int(pkg[0]);
			c_code = Misc.byte2int(pkg[1]);
			if(c_code!=0) {
				a_code = Misc.byte2int(pkg[2]);
				Misc.loge(
					"[%s][is_complete] abnormal!! (%02x, %02x, %02x)",
					s_head, c_code, a_code 
				);
				return a_code; 
			}
		}else {
			s_head = Integer.valueOf(""+(char)(pkg[0])+(char)(pkg[1]), 16);
			c_code = Integer.valueOf(""+(char)(pkg[2])+(char)(pkg[3]), 16);
			if(c_code!=0) {				
				a_code = Integer.valueOf(""+(char)(pkg[4])+(char)(pkg[5]), 16);
				Misc.loge(
					"[%s][is_complete] abnormal!! (%02x, %02x, %02x)",
					s_head, c_code, a_code 
				);
				return a_code; 
			}
		}
		return c_code;
	}
	//--------------------------------------------------
	
	private int exec_run(final boolean flg) {
		send(
			(flg==true)?(0x13):(0x14),
			0xFF, monitor,
			null
		);
		return is_complete(recv(-1));		
	}
	
	private int[] exec_read_bit(final Node node) {
		int cnt = 0;
		if(USE_BINARY==true) {
			cnt = node.points * 4;
			cnt = (cnt + cnt%8) / 8;
		}else {
			cnt = node.points;
		}
		send(
			0x00,
			0xFF, monitor,
			node.pay//head device & number of points
		);		
		final byte[] res = recv(cnt);
		if(is_complete(res)!=0) {
			return null;
		}
		final int[] lst = new int[node.points];
		for(int i=0; i<lst.length; i++) {
			if(USE_BINARY==true) {
				lst[i] = (i%2==0)?(
					((int)(res[2+i/2]) & 0xF0)>>4
				):(
					((int)(res[2+i/2]) & 0x0F)>>0
				);
			}else {
				lst[i] = (res[4+i]=='1')?(1):(0);
			}
		}		
		return lst;
	}
	private int[] exec_read_val(final Node node,int sizeof) {
		int cnt = 0;
		if(USE_BINARY==true) {
			cnt = node.points * sizeof;
		}else {
			sizeof = sizeof * 2;
			cnt = node.points * sizeof;
		}
		send(
			0x01,
			0xFF, monitor,
			node.pay//head device & number of points
		);		
		final byte[] res = recv(cnt);
		if(is_complete(res)!=0) {
			return null;
		}
		final int[] lst = new int[node.points];
		for(int i=0; i<lst.length; i++) {
			if(USE_BINARY==true) {
				int val = 0;
				for(int j=0; j<sizeof; j++) {
					int _v = Misc.byte2int(res[2+i*sizeof+j]);
					_v = _v << (j*8);
					val = val | _v;
				}
				lst[i] = val;
			}else {
				String txt = "";
				if(sizeof==2) {
					txt = txt+(char)(res[4+i*sizeof+1]);
					txt = txt+(char)(res[4+i*sizeof+0]);
				}else if(sizeof==4) {
					txt = txt+(char)(res[4+i*sizeof+2]);
					txt = txt+(char)(res[4+i*sizeof+3]);
					txt = txt+(char)(res[4+i*sizeof+1]);
					txt = txt+(char)(res[4+i*sizeof+0]);
				}else {
					Misc.loge("invalid format!!!");
					continue;
				}
				lst[i] = Integer.parseInt(txt, 16);
			}
		}
		return lst;
	}
	private int[] exec_read(final Node node) {
		if(node.is_bit()==true) {
			return exec_read_bit(node);
		}else if(node.is_val()==true) {
			if(node.name=='c' && node.address>=200) {
				return exec_read_val(node,4);
			}
			return exec_read_val(node,2);
		}
		return null;
	}
	
	private int exec_write(final String txt,final int... args) {
		Node node = parsing_text(txt);
		if(node==null) {
			return -10;
		}
		final int cmd = (node.is_bit())?(0x02):(0x03);		
		final byte[] head_dev = node.pay;
		byte[] data_arg = null;
		if(node.is_bit()==true) {
			if(USE_BINARY==true) {
				//one point is 4 bit~~~
				data_arg = new byte[args.length/2];
				for(int i=0; i<args.length; i+=2) {
					final int jj = i*2+0;
					final int kk = i*2+1;
					int vv = 0;
					vv = vv | (args[jj]<<4);
					if(kk<args.length) {
						vv = vv | (args[kk]<<0);
					}					
					data_arg[i/2] = Misc.maskInt0(vv);
				}
			}else {
				data_arg = new byte[args.length];
				for(int i=0; i<args.length; i++) {
					data_arg[i] = (byte)((args[i]>0)?('1'):('0'));
				}
			}
			return -11;
		}else if(node.is_val()==true) {
			if(USE_BINARY==true) {
				final int sizeof;
				if(node.name=='c' && node.address>=200) {
					sizeof = 4;
				}else {
					sizeof = 2;
				}
				data_arg = new byte[args.length*sizeof];
				
				for(int i=0; i<args.length; i++) {
					//ByteBuffer buf = ByteBuffer.allocate(2);
					//buf.putInt(args[i]);
					//buf.array();
					for(int j=0; j<sizeof; j++) {
						int vv = args[i] >> (8*sizeof);
						data_arg[i*sizeof+j] = (byte)(vv & 0xFF);
					}
				}
			}else {
				String txt_arg = "";
				if(node.name=='c' && node.address>=200) {
					for(int i=0; i<args.length; i++) {
						txt_arg = txt_arg + String.format("%04X", (args[i]>>0 )&0xFFFF);
						txt_arg = txt_arg + String.format("%04X", (args[i]>>16)&0xFFFF);
					}
				}else {
					for(int i=0; i<args.length; i++) {
						txt_arg = txt_arg + String.format("%04X", args[i]);
					}
				}
				data_arg = txt_arg.getBytes();
			}
			return -11;
		}
		send(
			cmd,
			0xFF, monitor,
			Misc.chainBytes(head_dev, data_arg)
		);
		return is_complete(recv(-1));
	}
	
	private String exec_get_name() {
		send(0x15,0xFF,monitor,null);
		final byte[] res = recv(2);//0xF3 --> FX3U/FX3UC
		if(is_complete(res)!=0) {
			return "";
		}
		int code = 0;
		if(USE_BINARY==true) {
			code = Misc.byte2int(res[2]);
		}else {
			code = Integer.valueOf(""+((char)res[4])+((char)res[5]));
		}
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
		send(0x16,0xFF,monitor,ctxt);
		final byte[] res = recv(5);//0xF3 --> FX3U/FX3UC
		if(is_complete(res)!=0) {
			return;
		}
		String txt = "";
		if(USE_BINARY==true) {
			for(int i=0; i<res.length; i++) {
				txt = txt + String.format("%02X_", Misc.byte2int(res[i]));
			}
		}else {
			for(int i=0; i<res.length; i+=2) {
				txt = txt + ((char)res[0]) + ((char)res[1]) + '_';
			}
		}
		Misc.loge("[%s][exec_loopback] %s", TAG, txt);
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
	//--------------------------------------------------
	
	
}
