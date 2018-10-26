package prj.scada;

import java.util.Arrays;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import narl.itrc.DevBase;
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
public class DevSPIK2k extends DevBase {

	private static final String TAG = "Dev-SPIK2000";
	
	private DevTTY conn = new DevTTY();
	
	public DevSPIK2k() {
		super(TAG);
	}

	public DevSPIK2k(String tty) {
		super(TAG);
		conn.setPathName(tty);
	} 
	
	private static final int MSG_MASK   = 0xFF000000;
	private static final int MSG_GET_REG= 0x82000000;
	private static final int MSG_SET_REG= 0x83000000;
	
	protected static class Token extends TokenBase {
		
		private int msg;		
		private byte[] buf;

		public Token(int message){
			msg = message;
			buf = null;
		}
		public Token(int mesg, int addr, int size){
			msg = mesg | ((addr&0xFFF)<<12) | (size&0xFFF);
			buf = null;
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
		public Token prepare_ad_header(){
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
			return this;
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
	};
		
	private static final byte STX = 0x02;
	private static final byte DLE = 0x10;
	private static final byte ETX = 0x03;
	//private static final byte NAK = 0x15;
	private static final byte EM_ = 0x25;//end of medium
	
	@Override
	protected boolean looper(TokenBase obj) {
		
		conn.readBuff();//clear buffer, but why???
		
		Token tkn = (Token)obj;

		switch(tkn.get_msg()){
		case MSG_GET_REG:
			//step.1 - give start code.
			conn.writeByte(STX);			
			//step.2 - wait acknowledge
			wait_code(DLE);			
			//step.3 - give device RK512 header.
			conn.writeBuff(pack_data(tkn.prepare_ed_header()));			
			//step.4 - take response
			wait_code(STX);
			//step.5 - give device command, finally get response.
			conn.writeByte(DLE);
			tkn.buf = take_data();
			break;
		case MSG_SET_REG:
			//step.1 - give start code.
			conn.writeByte(STX);
			//step.2 - wait acknowledge
			wait_code(DLE);
			//step.3 - dump user data to device
			conn.writeBuff(pack_data(tkn.buf));
			//step.5 - wait acknowledge
			conn.readByteOne();
			conn.readByteOne();
			//int res = (b1.intValue() & 0x00FF) << 8;
			//res = res | (b2.intValue() & 0x00FF);
			break;
		}
		if(match_checksum(tkn)==true){
		}
		return true;
	}

	@Override
	protected boolean eventReply(TokenBase obj) {
		
		Token tkn = (Token)obj;
		
		switch(tkn.get_msg()){
		case MSG_GET_REG:
			event_update_property(tkn);
			break;
		case MSG_SET_REG:
			break;
		}
		return true;
	}

	public void link(String tty){
		conn.setPathName(tty);
		link();
	}
	
	@Override
	protected void eventLink() {
		conn.open();
		getRegister();
	}

	@Override
	protected void eventUnlink() {
		conn.close();
	}


	/**
	 * Gather data from device.<p>
	 * The tail of data include DLE, ETX and BCC character.<p>
	 * @return the value of block check sum
	 */
	private byte[] take_data(){
		final byte[] buf = conn.readBuff();
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
			Byte res = conn.readByteOne();
			if(res!=null){
				byte bb = res.byteValue();
				if(bb==code){
					return;
				}else if(bb==EM_){
					conn.writeByte(STX);
				}	
			}
			Misc.delay(200);
		}
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
		
	//private int get_checksum(byte[] buf){
	//	return buf[buf.length-1];
	//}
	
	private boolean match_checksum(final Token tkn){
		final int size = tkn.get_size();
		int idx = 4 + size*2 + 3 - 1;// 4 byte dummy, DLE, ETX, and checksum. 
		if(tkn.buf.length<=idx){
			return false;
		}
		int cc1 = ((int)tkn.buf[idx]) & 0xFF;
		int cc2 = checksum(tkn.buf, 0, idx);
		if(cc1!=cc2){
			return false;
		}
		return true;
	}
	//---------------------------------//
	
	public DevSPIK2k measure(){
		//offer(new Token(MSG_MEASURE));
		return this;
	}
	
	public DevSPIK2k update(){
		//offer(new Token(MSG_UPDATE, 0, 4));
		return this;
	}

	public DevSPIK2k getRegister(){
		return getRegister(4, 15);
	}
	
	public DevSPIK2k getRegister(int addr, int size){
		offer(new Token(MSG_GET_REG, addr, size));
		return this;
	}
	
	public DevSPIK2k setRegister(){
		return setRegister(4,15);
	}
	
	public DevSPIK2k setRegister(int addr, int size){
		Token tkn = new Token(MSG_SET_REG, addr, size);
		tkn.prepare_ad_header();
		event_fetch_property(tkn);
		offer(tkn);
		return this;
	}
	
	public DevSPIK2k setRegister(int addr, int size, int val){
		Token tkn = new Token(MSG_SET_REG, addr, size);
		tkn.prepare_ad_header();
		event_fetch_property(tkn, val);
		offer(tkn);
		return this;
	}
	
	public DevSPIK2k Ready(boolean enable){
		if(enable==false){
			setRegister(1, 1, 0x01);	
		}else{
			setRegister(1, 1, 0x02);
		}
		return this; 
	}
	
	public DevSPIK2k Running(boolean enable){
		if(enable==false){
			setRegister(1, 1, 0x01);	
		}else{
			setRegister(1, 1, 0x02);
		}
		return this; 
	}
	
	public DevSPIK2k clearErr(){ 
		return setRegister(1, 1, 0x03);
	}
	
	public DevSPIK2k Save_CFG(){ 
		return setRegister(1, 1, 0x10); 
	}
	
	public DevSPIK2k setDC1(boolean enable){ 
		if(enable==false){
			setRegister(1, 1, 0x20);	
		}else{
			setRegister(1, 1, 0x21);
		}
		return this; 
	}

	public DevSPIK2k setDC2(boolean enable){ 
		if(enable==false){
			setRegister(1, 1, 0x22);	
		}else{
			setRegister(1, 1, 0x23);
		}
		return this; 
	}
	//---------------------------------//
	
	public final IntegerProperty ARC_Count = new SimpleIntegerProperty();
	public final IntegerProperty DC1_V_Act = new SimpleIntegerProperty();
	public final IntegerProperty DC1_I_Act = new SimpleIntegerProperty();
	public final IntegerProperty DC1_P_Act = new SimpleIntegerProperty();
	public final IntegerProperty DC2_V_Act = new SimpleIntegerProperty();
	public final IntegerProperty DC2_I_Act = new SimpleIntegerProperty();
	public final IntegerProperty DC2_P_Act = new SimpleIntegerProperty();
	
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

	private void event_update_mode(int val){
		switch(val&0x7){
		case 1: Mode_Operation.set(MODE_BIPOLAR); break;
		case 2: Mode_Operation.set(MODE_UNI_NEG); break;
		case 3: Mode_Operation.set(MODE_UNI_POS); break;
		case 4: Mode_Operation.set(MODE_DC_NEG ); break;
		case 5: Mode_Operation.set(MODE_DC_POS ); break;
		}
	}
	private void event_update_state(int val){
		check_bool(val,0x0001, State_Error);
		check_bool(val,0x0002, State_Running);
		check_bool(val,0x0004, State_Ready);
		check_bool(val,0x0008, State_Arc_Delay);
		check_bool(val,0x0040, State_DC1_On);
		check_bool(val,0x0080, State_DC2_On);
		check_bool(val,0x0100, State_CFG_Saved);
		check_bool(val,0x0200, State_Save_CFG);
	}
	private void event_update_error(int val){
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
	
	/**
	 * update data from token to property.<p>
	 * @param tkn - message token for inner looper
	 */
	private void event_update_property(final Token tkn){
		
		final int addr = tkn.get_addr();
		final int size = tkn.get_size();
		
		for(int off=0; off<size; off++){
			int val = get_word_value(tkn.buf, 2+off);
			switch(addr+off){
			case  0: event_update_mode(val);  break;
			case  1: event_update_state(val); break;
			case  3: event_update_error(val); break;
			case  4: Reg_Puls_Pos.set(val); break;
			case  5: Reg_Paus_Pos.set(val);	break;
			case  6: Reg_Puls_Neg.set(val);	break;
			case  7: Reg_Paus_Neg.set(val); break;
			case  8: Reg_ARC_Pos.set(val); break;
			case  9: Reg_ARC_Neg.set(val); break;
			case 10: Reg_ARC_Delay.set(val); break;
			case 11: Reg_ARC_Overflow.set(val); break;
			case 12: Reg_ARC_Interval.set(val); break;
			case 13: Reg_DC1_Volt.set(val);	break;
			case 14: Reg_DC1_Amp.set(val); break;
			case 15: Reg_DC1_Pow.set(val); break;
			case 16: Reg_DC2_Volt.set(val); break;
			case 17: Reg_DC2_Amp.set(val); break;
			case 18: Reg_DC2_Pow.set(val); break;
			}
		}
	}
	
	/**
	 * fetch data from property to token.<p>
	 * @param tkn - message token for inner looper
	 * @param val - when address is equal to 0 or 1.	 
	 */
	private void event_fetch_property(final Token tkn, int... args){
		int addr = tkn.get_addr();
		int size = tkn.get_size();
		for(int off=0; off<size; off++){
			int idx = 5 + off;
			int tmp = 0;
			if(off<args.length && args.length!=0){
				tmp = args[off];
			}else{
				switch(addr+off){
				case  4:tmp = Reg_Puls_Pos.get(); break;
				case  5:tmp = Reg_Paus_Pos.get(); break;
				case  6:tmp = Reg_Puls_Neg.get(); break;
				case  7:tmp = Reg_Paus_Neg.get(); break;
				case  8:tmp = Reg_ARC_Pos.get(); break;
				case  9:tmp = Reg_ARC_Neg.get(); break;
				case 10:tmp = Reg_ARC_Delay.get();	break;
				case 11:tmp = Reg_ARC_Overflow.get(); break;
				case 12:tmp = Reg_ARC_Interval.get(); break;
				case 13:tmp = Reg_DC1_Volt.get(); break;
				case 14:tmp = Reg_DC1_Amp.get(); break;
				case 15:tmp = Reg_DC1_Pow.get(); break;
				case 16:tmp = Reg_DC2_Volt.get(); break;
				case 17:tmp = Reg_DC2_Amp.get(); break;
				case 18:tmp = Reg_DC2_Pow.get(); break;
				}
			}
			set_word_value(tkn.buf, idx, tmp);
		}
	}
}
