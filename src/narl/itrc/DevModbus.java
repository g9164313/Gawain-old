package narl.itrc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Support Modbus device, provide a looper to update or modify registers.
 * @author qq
 *
 */
@SuppressWarnings("restriction")
public class DevModbus extends DevBase {

	public DevModbus(){
		TAG = "modbus-dev";
	}
	
	//below variables will be accessed by native code
	private String rtuName = "";
	private int    rtuBaud = 9600;
	private byte   rtuData = '8';
	private byte   rtuMask = 'n';
	private byte   rtuStop = '1';

	private String tcpName = "";	
	private int    tcpPort = 502;
	
	private long handle= 0L;
	
	private short slave= 0;//MODBUS_BROADCAST_ADDRESS

	private final static String STG_IGNITE = "ignite";
	private final static String STG_LOOPER = "looper";
	
	
	/**
	 * Choose which type connection, format is below:
	 * RTU:[device name],[baud-rate],[8n1]
	 * TCP:[IP address]#[port]
	 */
	@Override
	public void open() {
		final String path = Gawain.prop().getProperty(TAG, "");
		if(path.length()==0) {
			Misc.logw("No default tty name...");
			return;
		}
		open(path);
	}
	public void open(final String path) {
		if(path.matches("^[rR][tT][uU]:[\\/\\w]+,\\d+,[78][neoNEO][12]")==true) {			
			String[] col = path.substring(4).split(",");	
			if(col.length>=1) {
				rtuName = col[0];
			}
			if(col.length>=2) {
				rtuBaud = Integer.valueOf(col[1]);
			}
			if(col.length>=3) {
				rtuData = (byte)col[2].charAt(0);
				//LibModbus use upper-case mask sign!!!
				rtuMask = (byte)Character.toUpperCase(col[2].charAt(1));
				rtuStop = (byte)col[2].charAt(2);
			}
			implOpenRtu();
			
		}else if(path.matches("^[tT][cC][pP]:\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}[#]?[\\d]*")==true) {		
			
			String[] col = path.substring(4).split("#");
			if(col.length>=1) {
				tcpName = col[0];
			}
			if(col.length>=2) {
				tcpPort = Integer.valueOf(col[1]);
			}
			implOpenTcp();
			
		}else {
			handle = 0L;
		}
		if(cells.size()!=0 && handle>0L) {
			playLoop();
		}
	}

	public void playLoop() {
		if(isFlowing()==true) {
			return;
		}
		addState(STG_IGNITE,()->ignite());
		addState(STG_LOOPER,()->looper());
		playFlow(STG_IGNITE);
	}

	@Override
	public void close() {		
		if(handle==0L) {
			return;
		}
		if(cells.size()!=0) {
			stopFlow();
		}
		implClose();
	}

	@Override
	public boolean isLive() {
		return (handle==0L)?(false):(true);
	}
	//-----------------------
	
	private class MirrorCell {
		final int    offset;
		final char   t_type;
		final Object target;
		MirrorCell(
			final int off,
			final char typ,
			final Object obj
		) {
			offset = off;
			t_type = Character.toUpperCase(typ);
			target = obj;
		}
		void update_number(final short[] blkdata) {
			switch(t_type) {			
			case 'B':
			case 'b': 
				update_boolean(blkdata); 
				break;
			case 'I':
			case 'i': 
				update_integer(blkdata);
				break;
			case 'S': 
				update_short(blkdata);
				break;
			case 'F':
				update_float(blkdata); 
				break;
			}
		}
		private void update_boolean(final short[] blkdata) {
			int o1 = offset >>4;
			int m1 = offset &0xF;//mask
			int v1 = blkdata[o1];
			if(o1>=blkdata.length) {
				return;
			}
			v1 = v1 & (1<<m1);
			if(t_type=='B') {
				((BooleanProperty)target).set(v1!=0);
			}else if(t_type=='b'){
				((AtomicBoolean)target).set(v1!=0);
			}			
		}
		private void update_integer(final short[] blkdata) {
			if(offset>=blkdata.length) {
				return;
			}
			int v = blkdata[offset];
			if(t_type=='I') {
				((IntegerProperty)target).set(v);
			}else if(t_type=='i'){
				((AtomicInteger)target).set(v);
			}			
		}
		private void update_short(final short[] blkdata) {
			if(offset>=blkdata.length) {
				return;
			}
			int v2 = blkdata[offset];				
			((IntegerProperty)target).set(v2&0x0000FFFF);		
		}
		private void update_float(final short[] blkdata) {
			if((offset+1)>=blkdata.length) {
				return;
			}
			byte[] bb = {
				(byte)((blkdata[offset+1] & 0xFF00)>>8),
				(byte)((blkdata[offset+1] & 0x00FF)>>0),
				(byte)((blkdata[offset+0] & 0xFF00)>>8),
				(byte)((blkdata[offset+0] & 0x00FF)>>0),
			};
			float v = ByteBuffer.wrap(bb).getFloat();
			((FloatProperty)target).set(v);
				
		}
	}	
	private class RecallCell {
		final int  slaveId;//MODBUS_BROADCAST_ADDRESS=0, default=-1
		final char func_id;		
		final int  address;
		final short[] blkdata;  
	
		ArrayList<MirrorCell> prop = new ArrayList<>();
		
		RecallCell(
			final int sid,
			final char fid,
			final int addr,
			final int size
		) {			
			slaveId = sid;
			func_id = fid;
			address = addr;			
			blkdata = new short[size];
		}
		void fecth_data() {
			if(slaveId>=0) {
				implSlaveID(slaveId);
			}
			switch(func_id) {
			case 'C':
				//coils, function code = 1
				implReadC(address,blkdata);
				break;
			case 'S':
				//input status, function code = 2
				implReadS(address,blkdata);
				break;
			case 'R':
			case 'I':
				//input register, function code = 4
				implReadI(address,blkdata);
				break;	
			case 'H':
				//holding register, function code = 3
				implReadH(address,blkdata);
				break;
			}
		}
		void update_property() {
			for(MirrorCell mc:prop) {
				mc.update_number(blkdata);
			}
		}
	};
	
	private ArrayList<RecallCell> cells = new ArrayList<>();
	
	/**
	 * 	User can override this to prepare something.<p>
	 * 	Remember this is not called by GUI-thread.<p>
	 */
	protected void ignite() {
		nextState(STG_LOOPER);
	}
	
	protected int looperDelay = 50;
	
	protected void looper() {
		if(isLive()==false || cells.size()==0) {
			block_sleep_msec(500);
			return;
		}
		for(RecallCell cc:cells) {
			cc.fecth_data();
			if(looperDelay>0) {
				block_sleep_msec(looperDelay);
			}
		}
		Application.invokeLater(()->{
			for(RecallCell cc:cells) {
				cc.update_property();
			}
		});
	}
	
	/**
	 * mapping register address.<p>
	 * first character is register type, it can be C, S, I, H<p>
	 * C - coil register
	 * S - input status
	 * H - holding register
	 * I - input register
	 * @param radix - address value radix
	 * @param address - register address, ex:H303, I12A 
	 * @return
	 */
	public DevModbus mapAddress(
		final int radix,
		final int sid,
		final String... address
	) {
		String pattn;
		if(radix==16) {
			pattn = "[CSHI][0123456789ABCDEF]+([-~][0123456789ABCDEF]+)?";
		}else {
			pattn = "[CSHI]\\d+([-~]\\d+)?";
		}		
		for(String txt:address) {
			txt = txt.toUpperCase();
			if(txt.matches(pattn)==false) {
				continue;
			}else if(txt.length()==0){
				continue;
			}
			char fid = txt.toUpperCase().charAt(0);
			int addr = 0;
			int size = 1;
			String[] col = txt.substring(1).split("[-~]");
			if(col.length>=1) {
				addr = Integer.valueOf(col[0],radix);
			}
			if(col.length>=2) {
				size = Integer.valueOf(col[1],radix) - addr + 1;
				if(size<=0) {
					return this;
				}
			}
			cells.add(new RecallCell(sid,fid,addr,size));
		}
		return this;
	}
	/**
	 * convenience function for 'mapAddress(radix,address)'.<p>
	 * address base is decimal.<p>
	 * mapping register address.<p>
	 * first character is register type, it can be C, H, I.<p>
	 * C - coil register
	 * H - holding register
	 * I/R - input register
	 * @param address
	 * @return
	 */
	public DevModbus mapAddress(final String... address) {
		return mapAddress(10, -1,address);
	}
	public DevModbus mapAddress(final int sid,final String... address) {
		return mapAddress(10,sid,address);
	}
	public DevModbus mapAddress10(final String... address) {
		return mapAddress(address);
	}
	public DevModbus mapAddress10(final int sid,final String... address) {
		return mapAddress(sid,address);
	}
	/**
	 * convenience function for 'mapAddress(radix,address)'.<p>
	 * address base is decimal.<p>
	 * mapping register address.<p>
	 * first character is register type, it can be C, H, I.<p>
	 * C - coil register
	 * H - holding register
	 * I/R - input register
	 * @param address
	 * @return
	 */
	public DevModbus mapAddress16(final String... address) {
		return mapAddress(16, -1,address);
	}
	public DevModbus mapAddress16(final int sid,final String... address) {
		return mapAddress(16,sid,address);
	}
	
	public BooleanProperty mapBoolean(final int address,final int bit) {
		return mapBoolean(-1,address,bit);
	}
	public IntegerProperty mapShort(final int address) {
		return mapShort(01,address);
	}
	public IntegerProperty mapInteger(final int address) {
		return mapInteger(-1,address);
	}
	public FloatProperty mapFloat(final int address) {		
		return mapFloat(-1,address);
	}
	public BooleanProperty mapBoolean(final int slaveId,final int address,final int bit) {
		BooleanProperty prop = new SimpleBooleanProperty();
		add_mirror_cell(slaveId,address,bit,'B',prop);
		return prop;
	}
	public IntegerProperty mapShort(final int slaveId,final int address) {
		IntegerProperty prop = new SimpleIntegerProperty();
		add_mirror_cell(slaveId,address,-1,'S',prop);
		return prop;
	}
	public IntegerProperty mapInteger(final int slaveId,final int address) {
		IntegerProperty prop = new SimpleIntegerProperty();
		add_mirror_cell(slaveId,address,-1,'I',prop);
		return prop;
	}
	public FloatProperty mapFloat(final int slaveId,final int address) {
		FloatProperty prop = new SimpleFloatProperty();
		add_mirror_cell(slaveId,address,-1,'F',prop);		
		return prop;
	}
	/**
	 * 
	 * @param address
	 * @param bit_pos: 0 to 15
	 * @param prop_type
	 * @param property
	 */
	private void add_mirror_cell(
		final int slaveId,
		final int address,
		final int bit_pos,
		final char prop_type,
		final Object property
	) {
		for(RecallCell cc:cells) {
			if(cc.slaveId!=slaveId) {
				continue;
			}
			final int beg = cc.address;
			final int end = cc.address + cc.blkdata.length;
			if(beg<=address && address<end) {
				int off = address - cc.address;
				if(0<=bit_pos && bit_pos<=15) {
					off = (off << 4) | (bit_pos & 0xF); 
				}
				cc.prop.add(new MirrorCell(
					off,
					prop_type,
					property
				));
				return;
			}
		}
	}
	
	//-------------------------------------//
	
	/**
	 * continue writing until it is successful.
	 * @param s_id - slave identify
	 * @param addr - address
	 * @param val - value
	 */
	public void writeCont_sid(final int s_id,final int addr,final int val) {
		implSlaveID(s_id);
		short[] buff = { (short)(val&0xFFFF) };
		int res = 0;
		do{
			res = implWrite(addr,buff);
			if(res>0) {
				break;
			}
			block_sleep_msec(50);
		}while(true);
	}
	public void writeVals_sid(final int s_id,final int addr,final int... vals) {
		implSlaveID(s_id);
		final short[] buff = new short[vals.length];
		for(int i=0; i<vals.length; i++) {
			buff[i] = (short)(vals[i]&0xFFFF);
		}
		implWrite(addr,buff);
	}
	public void write_OR_sid(final int s_id,final int addr,final int val) {
		implSlaveID(s_id);
		short[] buff = {0};
		implReadI(addr,buff);
		buff[0] = (short)((buff[0] | val) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeAND_sid(final int s_id,final int addr,final int val) {
		implSlaveID(s_id);
		short[] buff = {0};
		implReadI(addr,buff);
		buff[0] = (short)((buff[0] & val) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeXOR_sid(final int s_id,final int addr,final int val) {
		implSlaveID(s_id);
		short[] buff = {0};
		implReadI(addr,buff);
		buff[0] = (short)((buff[0] ^ val) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeCls_sid(final int s_id,final int addr,final int bit) {
		implSlaveID(s_id);
		short[] buff = {0};
		implReadI(addr,buff);
		buff[0] = (short)((buff[0] & ~(1<<bit)) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeSet_sid(final int s_id,final int addr,final int bit) {
		implSlaveID(s_id);
		short[] buff = {0};
		implReadI(addr,buff);
		buff[0] = (short)((buff[0] |  (1<<bit)) & 0xFFFF);
		implWrite(addr,buff);
	}
	
	public void writeCont(final int addr,final int val) {
		writeCont_sid(-1,addr,val);
	}
	public void writeVals(final int addr,final int... vals) {
		writeVals_sid(-1,addr,vals);
	}
	public void write_OR(final int addr,final int val) {
		write_OR_sid(-1,addr,val);
	}
	public void writeAND(final int addr,final int val) {
		writeAND_sid(-1,addr,val);
	}
	public void writeXOR(final int addr,final int val) {
		writeXOR_sid(-1,addr,val);
	}
	public void writeCls(final int addr,final int bit) {
		writeCls_sid(-1,addr,bit);
	}
	public void writeSet(final int addr,final int bit) {
		writeSet_sid(-1,addr,bit);
	}
	
	public void asyncWriteCont_sid(final int s_id,final int addr,final int val) { asyncBreakIn(()->writeCont_sid(s_id,addr,val));}	
	public void asyncWriteVals_sid(final int s_id,final int addr,final int... val) { asyncBreakIn(()->writeVals_sid(s_id,addr,val));}	
	public void asyncWrite_OR_sid (final int s_id,final int addr,final int val) { asyncBreakIn(()->write_OR_sid (s_id,addr,val));}
	public void asyncWriteAND_sid (final int s_id,final int addr,final int val) { asyncBreakIn(()->writeAND_sid (s_id,addr,val));}
	public void asyncWriteXOR_sid (final int s_id,final int addr,final int val) { asyncBreakIn(()->writeXOR_sid (s_id,addr,val));}
	public void asyncWriteCls_sid (final int s_id,final int addr,final int bit) { asyncBreakIn(()->writeCls_sid (s_id,addr,bit));}
	public void asyncWriteSet_sid (final int s_id,final int addr,final int bit) { asyncBreakIn(()->writeSet_sid (s_id,addr,bit));}
	
	public void asyncWriteCont(final int addr,final int val) { asyncWriteCont_sid(-1,addr,val); }
	public void asyncWriteVals(final int addr,final int... val) { asyncWriteVals_sid(-1,addr,val); }	
	public void asyncWrite_OR (final int addr,final int val) { asyncWrite_OR_sid (-1,addr,val); }
	public void asyncWriteAND (final int addr,final int val) { asyncWriteAND_sid (-1,addr,val); }
	public void asyncWriteXOR (final int addr,final int val) { asyncWriteXOR_sid (-1,addr,val); }
	public void asyncWriteCls (final int addr,final int bit) { asyncWriteCls_sid (-1,addr,bit); }
	public void asyncWriteSet (final int addr,final int bit) { asyncWriteSet_sid (-1,addr,bit); }
	
	
	public int readReg(
		final int sid,			
		final char fid,
		final int addr
	) {
		if(sid>=0) {
			implSlaveID(sid);
		}
		short[] val = {0};
		char _fid = Character.toUpperCase(fid);		
		switch(_fid) {
		case 'C':
			//coils, function code = 1
			implReadC(addr,val);
			break;
		case 'S':
			//input status, function code = 2
			implReadS(addr,val);
			break;
		case 'H':
			//holding register, function code = 3
			implReadH(addr,val);
			break;				
		case 'I':
			//input register, function code = 4
			implReadI(addr,val);
			break;
		}
		return (int)val[0];
	}
	public int readReg(
		final char fid,
		final int addr
	) {
		return readReg(-1,fid,addr);
	}
	
	protected native void implOpenRtu();	
	protected native void implOpenTcp();
	protected native void implSlaveID(int slave_id);
	protected native void implReadC(int addr,short buff[]);//coils status(FC=1)
	protected native void implReadS(int addr,short buff[]);//input status(FC=2)	
	protected native void implReadH(int addr,short buff[]);//holding register
	protected native void implReadI(int addr,short buff[]);//input register
	protected native int implWrite(int addr,short buff[]);
	protected native void implClose();
}
