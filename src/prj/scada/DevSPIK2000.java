package prj.scada;

import java.util.Arrays;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import narl.itrc.DevTTY;
import narl.itrc.Misc;

/**
 * SPIK2000 is a high voltage pulse generator.<p>
 * This code is for device communication.<p>
 * Although manual say RS-232 protocol is RK512, it not real.<p>
 * I guess someone change the internal controller.<p>
 * Reference Document is "s7300_cp341_manual.pdf".<p>
 * @author qq
 *
 */
public class DevSPIK2000 extends DevTTY {

	private static final byte STX = 0x02;
	private static final byte DLE = 0x10;
	private static final byte ETX = 0x03;
	private static final byte NAK = 0x15;
	private static final byte EM_ = 0x25;//end of medium
	
	public DevSPIK2000(){
		init_property_addr();
	}
	
	public DevSPIK2000(String pathName){
		super(pathName);
		init_property_addr();
	}
		
	public void link(){
		link(null);
	}
	
	public void link(String pathName){
		if(open(pathName)==false){
			return;
		}
		lstToken.clear();
		if(looper!=null){
			if(looper.isDone()==false){	
				Misc.logw("Device is linked.");
				return;				
			}
		}
		looper = new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				
				while(looper.isCancelled()==false){
					
					Token tkn = lstToken.take();

					switch(tkn.get_msg()){
					case MSG_EXIT:
						return null;
					case MSG_GATHER:
						proc_read_data(tkn);
						break;
					case MSG_MODIFY:
						proc_send_data(tkn);
						break;
					case MSG_MEASURE:
						proc_read_meas();
						//lstToken.add(tkn);//for next turn~~~~
						break;
					case MSG_UPDATE://update status, error flag and measurement
						proc_read_meas();
						proc_read_data(tkn);
						if(autoUpdate==true){
							lstToken.add(tkn);//for next turn~~~~
						}
						break;
					}
				}				
				return null;
			}
		};
		Thread th = new Thread(looper,"Dev-SPIK2000");
		th.setDaemon(true);
		th.start();
	}
		
	public void unlink(){
		lstToken.add(new Token(MSG_EXIT));
		if(looper!=null){
			if(looper.isDone()==false){				
				looper.cancel();				
			}
			looper = null;
		}
		close();
	}
	
	private Task<?> looper = null;
	
	public boolean autoUpdate = false;
	
	private static final int MSG_MASK   = 0xFF000000;
	private static final int MSG_EXIT   = 0x00000000;
	private static final int MSG_MEASURE= 0x81000000;
	private static final int MSG_GATHER = 0x82000000;
	private static final int MSG_MODIFY = 0x83000000;
	private static final int MSG_UPDATE = 0x11000000;
	
	private class Token implements Delayed {
		private int msg;		
		private byte[] buf;
		private int arg = 0;
		public Token(int message){
			msg = message;
			buf = null;
		}
		public Token(int mesg, int addr, int size){
			this(mesg,addr,size,0);
		}
		public Token(int mesg, int addr, int size, int v_arg){
			msg = mesg | ((addr&0xFFF)<<12) | (size&0xFFF);
			buf = null;
			arg = v_arg;
		}
		public int get_msg(){
			return msg & MSG_MASK;
		}
		public int get_addr(){
			return (msg & 0x00FFF000)>>12;
		}
		public int get_size(){
			return (msg & 0x00000FFF);
		}
		public void prepare_ad_header(){
			final int addr = get_addr();
			final int size = get_size();
			buf = new byte[10+size*2];
			buf[2] = 0x41; 
			buf[3] = 0x44;
			buf[4] = (byte)((addr&0xFF00)>>8);
			buf[5] = (byte)((addr&0x00FF));
			buf[6] = (byte)((size&0xFF00)>>8);
			buf[7] = (byte)((size&0x00FF));
			buf[8] = (byte)(0xFF);
			buf[9] = (byte)(0xFF);
		}
		public byte[] prepare_ed_header(){
			final int addr = get_addr();
			final int size = get_size();
			final byte[] buf = new byte[10];
			buf[2] = 0x45; 
			buf[3] = 0x44;
			buf[4] = (byte)((addr&0xFF00)>>8);
			buf[5] = (byte)((addr&0x00FF));
			buf[6] = (byte)((size&0xFF00)>>8);
			buf[7] = (byte)((size&0x00FF));
			buf[8] = (byte)(0xFF);
			buf[9] = (byte)(0xFF);
			return buf;
		}
		
		@Override
		public int compareTo(Delayed o) {
			return 0;
		}
		@Override
		public long getDelay(TimeUnit unit) {
			switch(get_msg()){
			case MSG_MEASURE:
			case MSG_UPDATE:
				return unit.toSeconds(1);
			}
			return 0;
		}
	};
	private DelayQueue<Token> lstToken = new DelayQueue<Token>();
	
	public DevSPIK2000 measure(){
		lstToken.add(new Token(MSG_MEASURE));
		return this;
	}
	
	public DevSPIK2000 update(){
		lstToken.add(new Token(MSG_UPDATE, 0, 4));
		return this;
	}

	public DevSPIK2000 getRegister(){
		return getRegister(4, 15);
	}
	
	public DevSPIK2000 getRegister(int addr, int size){
		lstToken.add(new Token(MSG_GATHER, addr, size));
		return this;
	}
	
	public DevSPIK2000 setRegister(){
		lstToken.add(new Token(MSG_MODIFY, 4, 15));
		return this;
	}
	
	public DevSPIK2000 setRegister(int addr, int size){
		lstToken.add(new Token(MSG_MODIFY, addr, size));
		return this;
	}
	
	public DevSPIK2000 setRegister(int addr, int size, int v_arg){
		lstToken.add(new Token(MSG_MODIFY, addr, size, v_arg));
		return this;
	}
	
	public DevSPIK2000 Ready(boolean enable){
		if(enable==false){
			setRegister(1, 1, 0x01);	
		}else{
			setRegister(1, 1, 0x02);
		}
		return this; 
	}
	
	public DevSPIK2000 Running(boolean enable){
		if(enable==false){
			setRegister(1, 1, 0x01);	
		}else{
			setRegister(1, 1, 0x02);
		}
		return this; 
	}
	public DevSPIK2000 clearErr(){ 
		return setRegister(1, 1, 0x03);
	}
	public DevSPIK2000 Save_CFG(){ 
		return setRegister(1, 1, 0x10); 
	}
	public DevSPIK2000 setDC1(boolean enable){ 
		if(enable==false){
			setRegister(1, 1, 0x20);	
		}else{
			setRegister(1, 1, 0x21);
		}
		return this; 
	}

	public DevSPIK2000 setDC2(boolean enable){ 
		if(enable==false){
			setRegister(1, 1, 0x22);	
		}else{
			setRegister(1, 1, 0x23);
		}
		return this; 
	}

	public void idle(){
		lstToken.clear();
	}
	//-----------------------------------------//
	
	public final IntegerProperty ARC_Count = new SimpleIntegerProperty();
	public final IntegerProperty DC1_V_Act = new SimpleIntegerProperty();
	public final IntegerProperty DC1_I_Act = new SimpleIntegerProperty();
	public final IntegerProperty DC1_P_Act = new SimpleIntegerProperty();
	public final IntegerProperty DC2_V_Act = new SimpleIntegerProperty();
	public final IntegerProperty DC2_I_Act = new SimpleIntegerProperty();
	public final IntegerProperty DC2_P_Act = new SimpleIntegerProperty();
	
	/**
	 * Read device measurement(from address 19, block number = 07).<p>
	 * Values include:
	 *   ARC_Count, <p>
	 *   DC1_V_Act, DC1_I_Act, DC1_P_Act, <p>
	 *   DC2_V_Act, DC2_I_Act, DC2_P_Act, <p>
	 */
	private int proc_read_meas(){

		readBuff();//clear buffer why???
		
		//step.1 - check start code.
		wait_code(STX);
		
		//step.2 - give acknowledgement.
		writeByte(DLE);
		
		//step.3 - gather user data from device.
		final byte[] buf = take_data();
		
		//step.4 - give acknowledgement, again.
		writeByte(NAK);//Should we need this??
		
		final Runnable event = new Runnable(){
			@Override
			public void run() {
				ARC_Count.set(get_word_value(buf, 5));
				DC1_V_Act.set(get_word_value(buf, 6));
				DC1_I_Act.set(get_word_value(buf, 7));
				DC1_P_Act.set(get_word_value(buf, 8));
				DC2_V_Act.set(get_word_value(buf, 9));
				DC2_I_Act.set(get_word_value(buf,10));
				DC2_P_Act.set(get_word_value(buf,11));
			}
		};
		Application.invokeAndWait(event);
		
		return get_checksum(buf);
	}
	
	private static final String MODE_BIPOLAR = "Bipolar";
	private static final String MODE_UNI_NEG = "Unipolar－";
	private static final String MODE_UNI_POS = "Bipolar＋";
	private static final String MODE_DC_NEG = "DC－";
	private static final String MODE_DC_POS = "DC＋";
	public static final String[] ModeText ={
		MODE_BIPOLAR,
		MODE_UNI_NEG,
		MODE_UNI_POS,
		MODE_DC_NEG,
		MODE_DC_POS
	};
	public final StringProperty  Mode_Operation = new SimpleStringProperty(MODE_BIPOLAR);
	
	public final BooleanProperty Mode_Multiplex = new SimpleBooleanProperty();
	
	public final BooleanProperty State_Error    = new SimpleBooleanProperty();
	public final BooleanProperty State_Running  = new SimpleBooleanProperty();
	public final BooleanProperty State_Ready    = new SimpleBooleanProperty();
	public final BooleanProperty State_Arc_Delay= new SimpleBooleanProperty();
	public final BooleanProperty State_DC1_On   = new SimpleBooleanProperty();
	public final BooleanProperty State_DC2_On   = new SimpleBooleanProperty();
	public final BooleanProperty State_CFG_Saved= new SimpleBooleanProperty();
	public final BooleanProperty State_Save_CFG = new SimpleBooleanProperty();
	
	public final BooleanProperty Error_Driver1L = new SimpleBooleanProperty();
	public final BooleanProperty Error_Driver1R = new SimpleBooleanProperty();
	public final BooleanProperty Error_Driver2L = new SimpleBooleanProperty();
	public final BooleanProperty Error_Driver2R = new SimpleBooleanProperty();
	public final BooleanProperty Error_Arc_Pos  = new SimpleBooleanProperty();
	public final BooleanProperty Error_Arc_Neg  = new SimpleBooleanProperty();
	public final BooleanProperty Error_Arc_Over = new SimpleBooleanProperty();
	public final BooleanProperty Error_Rack_Temp= new SimpleBooleanProperty();
	public final BooleanProperty Error_HeatSink1= new SimpleBooleanProperty();
	public final BooleanProperty Error_HeatSink2= new SimpleBooleanProperty();
	public final BooleanProperty Error_Interlock= new SimpleBooleanProperty();
	public final BooleanProperty Error_DC1      = new SimpleBooleanProperty();
	public final BooleanProperty Error_DC2      = new SimpleBooleanProperty();
	public final BooleanProperty Error_Config   = new SimpleBooleanProperty();
	public final BooleanProperty Error_Address  = new SimpleBooleanProperty();
	public final BooleanProperty Error_Watchdog = new SimpleBooleanProperty();
	
	//below properties are read and set by user
	public final IntegerProperty Reg_Puls_Pos = new SimpleIntegerProperty();
	public final IntegerProperty Reg_Paus_Pos = new SimpleIntegerProperty();
	public final IntegerProperty Reg_Puls_Neg = new SimpleIntegerProperty();
	public final IntegerProperty Reg_Paus_Neg = new SimpleIntegerProperty();
	public final IntegerProperty Reg_ARC_Pos  = new SimpleIntegerProperty();
	public final IntegerProperty Reg_ARC_Neg  = new SimpleIntegerProperty();
	public final IntegerProperty Reg_ARC_Delay= new SimpleIntegerProperty();
	public final IntegerProperty Reg_ARC_Overflow = new SimpleIntegerProperty();
	public final IntegerProperty Reg_ARC_Interval = new SimpleIntegerProperty();
	public final IntegerProperty Reg_DC1_Volt= new SimpleIntegerProperty();
	public final IntegerProperty Reg_DC1_Amp = new SimpleIntegerProperty();
	public final IntegerProperty Reg_DC1_Pow = new SimpleIntegerProperty();
	public final IntegerProperty Reg_DC2_Volt= new SimpleIntegerProperty();
	public final IntegerProperty Reg_DC2_Amp = new SimpleIntegerProperty();
	public final IntegerProperty Reg_DC2_Pow = new SimpleIntegerProperty();

	private void fetch_property(final Token tkn){
		final Runnable event = new Runnable(){			
			@Override
			public void run() {
				int addr = tkn.get_addr();
				int size = tkn.get_size();
				for(int off=0; off<size; off++){
					int idx = 5 + off;
					switch(addr+off){
					case 0: set_word_value(tkn.buf, idx, tkn.arg); break;
					case 1: set_word_value(tkn.buf, idx, tkn.arg); break;
					case 4: set_word_value(tkn.buf, idx, Reg_Puls_Pos.get()); break;
					case 5:	set_word_value(tkn.buf, idx, Reg_Paus_Pos.get()); break;
					case 6:	set_word_value(tkn.buf, idx, Reg_Puls_Neg.get()); break;
					case 7:	set_word_value(tkn.buf, idx, Reg_Paus_Neg.get()); break;
					case 8:	set_word_value(tkn.buf, idx, Reg_ARC_Pos.get()); break;
					case 9: set_word_value(tkn.buf, idx, Reg_ARC_Neg.get()); break;
					case 10:set_word_value(tkn.buf, idx, Reg_ARC_Delay.get());	break;
					case 11:set_word_value(tkn.buf, idx, Reg_ARC_Overflow.get()); break;
					case 12:set_word_value(tkn.buf, idx, Reg_ARC_Interval.get()); break;
					case 13:set_word_value(tkn.buf, idx, Reg_DC1_Volt.get()); break;
					case 14:set_word_value(tkn.buf, idx, Reg_DC1_Amp.get()); break;
					case 15:set_word_value(tkn.buf, idx, Reg_DC1_Pow.get()); break;
					case 16:set_word_value(tkn.buf, idx, Reg_DC2_Volt.get()); break;
					case 17:set_word_value(tkn.buf, idx, Reg_DC2_Amp.get()); break;
					case 18:set_word_value(tkn.buf, idx, Reg_DC2_Pow.get()); break;
					}
				}
			}
		};
		Application.invokeAndWait(event);
	}
	
	private int update_property(final Token tkn){
		final int addr = tkn.get_addr();
		final int size = tkn.get_size();
		int idx = 4 + size*2 + 3 - 1;// 4 byte dummy, DLE, ETX, and checksum. 
		if(tkn.buf.length<=idx){
			return -11;
		}
		int cc1 = ((int)tkn.buf[idx]) & 0xFF;
		int cc2 = checksum(tkn.buf, 0, idx);
		if(cc1!=cc2){
			return -12;
		}
		final Runnable event = new Runnable(){
			private void update_mode(int val){
				switch(val&0x7){
				case 1: Mode_Operation.set(MODE_BIPOLAR); break;
				case 2: Mode_Operation.set(MODE_UNI_NEG); break;
				case 3: Mode_Operation.set(MODE_UNI_POS); break;
				case 4: Mode_Operation.set(MODE_DC_NEG ); break;
				case 5: Mode_Operation.set(MODE_DC_POS ); break;
				}
			}
			private void update_state(int val){
				check_bool(val,0x0001, State_Error);
				check_bool(val,0x0002, State_Running);
				check_bool(val,0x0004, State_Ready);
				check_bool(val,0x0008, State_Arc_Delay);
				check_bool(val,0x0040, State_DC1_On);
				check_bool(val,0x0080, State_DC2_On);
				check_bool(val,0x0100, State_CFG_Saved);
				check_bool(val,0x0200, State_Save_CFG);
			}
			private void update_error(int val){
				check_bool(val,0x0001, Error_Driver1L);
				check_bool(val,0x0002, Error_Driver1R);
				check_bool(val,0x0004, Error_Driver2L);
				check_bool(val,0x0008, Error_Driver2R);
				check_bool(val,0x0010, Error_Arc_Pos);
				check_bool(val,0x0020, Error_Arc_Neg);
				check_bool(val,0x0040, Error_Arc_Over);
				check_bool(val,0x0080, Error_Rack_Temp);
				check_bool(val,0x0100, Error_HeatSink1);
				check_bool(val,0x0200, Error_HeatSink2);
				check_bool(val,0x0400, Error_Interlock);
				check_bool(val,0x0800, Error_DC1);
				check_bool(val,0x1000, Error_DC2);
				check_bool(val,0x2000, Error_Config);
				check_bool(val,0x4000, Error_Address);
				check_bool(val,0x8000, Error_Watchdog);
			}
			private void check_bool(int val, int mask, BooleanProperty flag){
				if((val&mask)!=0){ 
					flag.set(true); 
				}else{ 
					flag.set(false);
				}
			}
			@Override
			public void run() {

				for(int off=0; off<size; off++){
					int val = get_word_value(tkn.buf, 2+off);
					switch(addr+off){
					case  0:
						update_mode(val);
						break;
					case  1:
						update_state(val);
						break;
					//case  2://COM port state, Read and Set(R/S)
					//	break;
					case  3:
						update_error(val);
						break;
					case  4:					
						Reg_Puls_Pos.set(val);
						break;
					case  5:
						Reg_Paus_Pos.set(val);
						break;
					case  6:
						Reg_Puls_Neg.set(val);
						break;
					case  7:
						Reg_Paus_Neg.set(val);
						break;
					case  8:
						Reg_ARC_Pos.set(val);
						break;
					case  9:
						Reg_ARC_Neg.set(val);
						break;
					case 10:
						Reg_ARC_Delay.set(val);
						break;
					case 11:
						Reg_ARC_Overflow.set(val);
						break;
					case 12:
						Reg_ARC_Interval.set(val);
						break;
					case 13:
						Reg_DC1_Volt.set(val);
						break;
					case 14:
						Reg_DC1_Amp.set(val);
						break;
					case 15:
						Reg_DC1_Pow.set(val);
						break;
					case 16:
						Reg_DC2_Volt.set(val);
						break;
					case 17:
						Reg_DC2_Amp.set(val);
						break;
					case 18:
						Reg_DC2_Pow.set(val);
						break;
					}
				}
			}
		};
		Application.invokeAndWait(event);
		return 0;
	}
	
	/**
	 * Request register values from device.<p> 
	 * @param addr - register address
	 * @param size - the length of requested data package 
	 * @return
	 */
	private int proc_read_data(Token tkn){
		
		readBuff();//clear buffer why???
		
		//step.1 - give start code.
		writeByte(STX);
		
		//step.2 - wait acknowledge
		wait_code(DLE);
		
		//step.3 - give device RK512 header.
		writeBuff(pack_data(tkn.prepare_ed_header()));
		
		//step.4 - take response
		wait_code(STX);

		//step.5 - give device command, finally gather response.
		writeByte(DLE);
		tkn.buf = take_data();
		
		//step.6 - update property according buffer.
		return update_property(tkn);
	}
	
	/**
	 * Send data to device
	 * @param data - command or setting
	 */
	private int proc_send_data(final Token tkn){
		
		//step.0 - prepare data and buffer
		if(tkn.buf==null){
			tkn.prepare_ad_header();
			fetch_property(tkn);
		}

		readBuff();//clear buffer why???
		
		//step.1 - give start code.
		writeByte(STX);
		
		//step.2 - wait acknowledge
		wait_code(DLE);
		
		//step.3 - dump user data to device
		writeBuff(pack_data(tkn.buf));

		//step.5 - wait acknowledge
		Byte b1 = readByteOne();
		Byte b2 = readByteOne();
		int res = 0;
		res = (b1.intValue() & 0x00FF) << 8;
		res = (b2.intValue() & 0x00FF);

		Misc.logv(
			"[SPIK2000] send data, addr=%d, size=%d, result=0x%04X",
			tkn.get_addr(),
			tkn.get_size(),
			res
		);
		return res;
	}

	/**
	 * Gather data from device.<p>
	 * The tail of data include DLE, ETX and BCC character.<p>
	 * @return the value of block check sum
	 */
	private byte[] take_data(){
		final byte[] buf = readBuff();
		int head = 0;
		int tail = buf.length-2;
		while(tail>=0){
			if(buf[tail]==DLE && buf[tail+1]==ETX){
				while(buf[head]==STX){
					head+=1;
				}
				return Arrays.copyOfRange(buf, head, tail+3);
			}
			tail-=1;
		}
		return buf;
	}
	
	private void wait_code(final byte code){
		for(;;){
			Byte res = readByteOne();
			if(res!=null){
				byte bb = res.byteValue();
				if(bb==code){
					return;
				}else if(bb==EM_){
					writeByte(STX);
				}	
			}
			Misc.delay(200);
		}
	}

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
	
	private byte[] pack_data(final byte[] data){
		int cnt = data.length;
		byte[] pack = Arrays.copyOf(data, cnt+3);
		pack[cnt+0] = DLE;
		pack[cnt+1] = ETX;
		pack[cnt+2] = (byte)checksum(pack,0,cnt+2);		
		return pack;
	}

	private int get_word_value(byte[] buf, int idx){
		idx = idx * 2;
		int cnt = buf.length;
		if(cnt<=0 || cnt<=(idx+1)){
			return -1;
		}
		int val = 0;
		val = (((int)buf[idx+0]) & 0xFF) << 8;
		val = (((int)buf[idx+1]) & 0xFF) | val;
		return val;
	}
	
	private void set_word_value(byte[] buf, int idx, int val){
		idx = idx * 2;
		int cnt = buf.length;
		if(cnt<=0 || cnt<=(idx+1)){
			return;
		}
		buf[idx+0] = (byte)((val & 0xFF00)>>8);
		buf[idx+1] = (byte)((val & 0x00FF)   );
	}
	
	private int get_checksum(byte[] buf){
		return buf[buf.length-1];
	}
	
	private void init_property_addr(){
		Mode_Operation.addListener( (a,b,c)->{
			if(c.equals(MODE_BIPOLAR)==true){ 
				setRegister(0,1,1);  
			}else if(c.equals(MODE_UNI_NEG)==true){
				setRegister(0,1,2); 
			}else if(c.equals(MODE_UNI_POS)==true){
				setRegister(0,1,3); 
			}else if(c.equals(MODE_DC_NEG)==true){
				setRegister(0,1,4); 
			}else if(c.equals(MODE_DC_POS)==true){
				setRegister(0,1,5); 
			}
		});
		Reg_Puls_Pos.addListener( (a,b,c)->{ setRegister(4,1); } );
		Reg_Paus_Pos.addListener( (a,b,c)->{ setRegister(5,1); } );		
		Reg_Puls_Neg.addListener( (a,b,c)->{ setRegister(6,1); } );		
		Reg_Paus_Neg.addListener( (a,b,c)->{ setRegister(7,1); } );		
		Reg_ARC_Pos.addListener ( (a,b,c)->{ setRegister(8,1); } );		
		Reg_ARC_Neg.addListener ( (a,b,c)->{ setRegister(9,1); } );
		Reg_ARC_Delay.addListener   ( (a,b,c)->{ setRegister(10,1); } );	
		Reg_ARC_Overflow.addListener( (a,b,c)->{ setRegister(11,1); } );		
		Reg_ARC_Interval.addListener( (a,b,c)->{ setRegister(12,1); } );		
		Reg_DC1_Volt.addListener( (a,b,c)->{ setRegister(13,1); } );		
		Reg_DC1_Amp.addListener ( (a,b,c)->{ setRegister(14,1); } );		
		Reg_DC1_Pow.addListener ( (a,b,c)->{ setRegister(15,1); } );		
		Reg_DC2_Volt.addListener( (a,b,c)->{ setRegister(16,1); } );		
		Reg_DC2_Amp.addListener ( (a,b,c)->{ setRegister(17,1); } );
		Reg_DC2_Pow.addListener ( (a,b,c)->{ setRegister(18,1); } );	
	}
	
}
