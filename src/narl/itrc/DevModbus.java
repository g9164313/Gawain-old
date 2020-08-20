package narl.itrc;

import java.util.ArrayList;

import com.sun.glass.ui.Application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class DevModbus extends DevBase {

	public DevModbus(){
		TAG = "modbus-dev";
	}
	
	/**
	 * Create object and connect device immediately
	 * @param name - device name and connection attributes
	 */
	public DevModbus(final String name){
		this();
		devPath = name;
	}
	
	/**
	 * Choose which type connection, format is below:
	 * RTU:[device name],[baud-rate],[8n1]
	 * TCP:[IP address]#[port]
	 */
	private String devPath = "";
	
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
	
	@Override
	public void open() {
		if(isLive()==true) {
			return;
		}
		if(devPath.matches("^[rR][tT][uU]:\\w+,\\d+,[78][noems][12]")==true) {
			
			String[] col = devPath.substring(4).split(",");	
			if(col.length>=1) {
				rtuName = col[0];
			}
			if(col.length>=2) {
				rtuBaud = Integer.valueOf(col[1]);
			}
			if(col.length>=3) {
				rtuData = (byte)col[2].charAt(0);
				rtuMask = (byte)col[2].charAt(1);
				rtuStop = (byte)col[2].charAt(2);
			}
			implOpenRtu();
			
		}else if(devPath.matches("^[tT][cC][pP]:\\d{1,3}.\\d{1,3}.\\d{1,3}.\\d{1,3}[#]?[\\d]*")==true) {
			
			String[] col = devPath.substring(4).split("#");
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
		if(mems.size()!=0) {
			addState(STG_IGNITE,()->ignite());
			addState(STG_LOOPER,()->looper());
			playFlow(STG_IGNITE);
		}
	}
	public void open(final String name) {
		devPath = name.split(";|@")[0];
		open();
	}
	
	@Override
	public void close() {		
		if(handle==0L) {
			return;
		}
		if(mems.size()!=0) {
			stopFlow();
		}
		implClose();
	}

	@Override
	public boolean isLive() {
		return (handle==0L)?(false):(true);
	}
	//-----------------------
	
	//register cell
	private class RCell {
		int     slaveId;//MODBUS_BROADCAST_ADDRESS=0
		char    func_id;		
		int     address;
		short[] values;
		IntegerProperty[] v_prop;
			
		public RCell(
			final int sid,
			final char fid,
			final int addr,
			final int size
		) {			
			slaveId= sid;
			func_id = Character.toUpperCase(fid);
			if(func_id=='R') {
				func_id = 'I';
			}
			address= addr;			
			values = new short[size];
			v_prop = new IntegerProperty[size];
			for(int i=0; i<size; i++) {
				v_prop[i] = new SimpleIntegerProperty();
			}
		}
		void fecth() {
			if(values==null) {
				return;
			}
			implSlaveID(slaveId);
			switch(func_id) {
			case 'C':
				//coils, function code = 1
				implReadC(address,values);
				break;
			case 'S':
				//input status, function code = 2
				implReadS(address,values);
				break;
			case 'H':
				//holding register, function code = 3
				implReadH(address,values);
				break;
			case 'I':
			case 'R':
				//input register, function code = 4
				implReadI(address,values);
				break;
			}
		}
		void update_by_gui() {
			if(values==null) {
				return;
			}
			for(int i=0; i<values.length; i++) {
				int v = values[i];
				v = v & 0xFFFF;
				v_prop[i].set(v);
			}
		}
	};
	
	private ArrayList<RCell> mems = new ArrayList<>();
	
	protected void ignite() {
		//user can override this to prepare something.
		//Remember this is not called by GUI-thread.
		nextState(STG_LOOPER);
	}
	
	protected int looperDelay = 50;
	
	private void looper() {
		if(looperDelay>0) {
			try {
				Thread.sleep(looperDelay);
			} catch (InterruptedException e) {
				return;
			}
		}
		if(isLive()==false) {
			return;
		}
		for(RCell reg:mems) {
			reg.fecth();
		}
		Application.invokeAndWait(()->{
			if(mems.size()==0) {
				return;
			}
			for(RCell reg:mems) {
				reg.update_by_gui();
			}
		});
	}
	/**
	 * mapping register address.<p>
	 * first character is register type, it can be C, H, I.<p>
	 * C - coil register
	 * H - holding register
	 * I/R - input register
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
			pattn = "[CSHIR][0123456789ABCDEF]+([-][0123456789ABCDEF]+)?";
		}else {
			pattn = "[CSHIR]\\d+([-]\\d+)?";
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
			String[] col = txt.substring(1).split("-");
			if(col.length>=1) {
				addr = Integer.valueOf(col[0],radix);
			}
			if(col.length>=2) {
				size = Integer.valueOf(col[1],radix) - addr + 1;
				if(size<=0) {
					return this;
				}
			}
			mems.add(new RCell(sid,fid,addr,size));
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
		return mapAddress(10,0,address);
	}
	public DevModbus mapAddress10(final String... address) {
		return mapAddress(address);
	}
	public DevModbus mapAddress(final int sid,final String... address) {
		return mapAddress(10,sid,address);
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
		return mapAddress(16,0,address);
	}
	public DevModbus mapAddress16(final int sid,final String... address) {
		return mapAddress(16,sid,address);
	}
	
	private IntegerProperty get_register(
		final int sid,
		final char fid, 
		final int addr
	) {
		for(RCell reg:mems) {
			int beg = reg.address;
			int end = reg.address + reg.values.length - 1;
			if(
				beg<=addr && 
				addr<=end &&
				reg.slaveId==sid &&
				reg.func_id==fid 
			) {				
				int off = addr - beg;
				return reg.v_prop[off];
			}
		}
		return null;
	}
	public IntegerProperty coilStatus(final int address) {
		return get_register(0,'C',address); 
	}
	public IntegerProperty inputStatus(final int address) {
		return get_register(0,'S',address); 
	}
	public IntegerProperty holdingRegister(final int address) {
		return get_register(0,'H',address);
	}
	public IntegerProperty inputRegister(final int address) {
		return get_register(0,'I',address);
	}
	public IntegerProperty coilStatus(final int slaveId,final int address) {
		return get_register(slaveId,'C',address); 
	}
	public IntegerProperty inputStatus(final int slaveId,final int address) {
		return get_register(slaveId,'S',address); 
	}
	public IntegerProperty holdingRegister(final int slaveId,final int address) {
		return get_register(slaveId,'H',address);
	}
	public IntegerProperty inputRegister(final int slaveId,final int address) {
		return get_register(slaveId,'I',address);
	}
	
	//writing with slave_id~~~
	public void writeVal(final int s_id,final int addr,final int... val) {
		implSlaveID(s_id);
		writeVal(addr,val);
	}
	public void writeVal(final int s_id,final int addr,final int val) {
		implSlaveID(s_id);
		writeVal(addr,val);
	}
	public void write_OR(final int s_id,final int addr,final int val) {
		implSlaveID(s_id);
		write_OR(addr,val);
	}
	public void writeAND(final int s_id,final int addr,final int val) {
		implSlaveID(s_id);
		writeAND(addr,val);
	}
	public void writeXOR(final int s_id,final int addr,final int val) {
		implSlaveID(s_id);
		writeXOR(addr,val);
	}
	public void writeBit0(final int s_id,final int addr,final int bit) {
		implSlaveID(s_id);
		writeBit0(addr,bit);
	}
	public void writeBit1(final int s_id,final int addr,final int bit) {
		implSlaveID(s_id);
		writeBit1(addr,bit);
	}
	public void asyncWriteVal(final int s_id,final int addr,final int val) {asyncBreakIn(()->writeVal(s_id,addr,val));}
	public void asyncWrite_OR(final int s_id,final int addr,final int val) {asyncBreakIn(()->write_OR(s_id,addr,val));}
	public void asyncWriteAND(final int s_id,final int addr,final int val) {asyncBreakIn(()->writeAND(s_id,addr,val));}
	public void asyncWriteXOR(final int s_id,final int addr,final int val) {asyncBreakIn(()->writeXOR(s_id,addr,val));}
	public void asyncWriteBit0(final int s_id,final int addr,final int bit) {asyncBreakIn(()->writeBit0(s_id,addr,bit));}
	public void asyncWriteBit1(final int s_id,final int addr,final int bit) {asyncBreakIn(()->writeBit1(s_id,addr,bit));}
	
	//writing without slave_id~~~
	public void writeVal(final int addr,final int... val) {
		short[] buff = new short[val.length];
		for(int i=0; i<val.length; i++) {
			buff[i] = (short)(val[i]&0xFFFF);
		}
		implWrite(addr,buff);
	}
	public void writeVal(final int addr,final int val) {
		short[] buff = { (short)(val&0xFFFF) };
		implWrite(addr,buff);
	}
	public void write_OR(final int addr,final int val) {
		short[] buff = {0};
		implReadI(addr,buff);
		buff[0] = (short)((buff[0] | val) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeAND(final int addr,final int val) {
		short[] buff = {0};
		implReadI(addr,buff);
		buff[0] = (short)((buff[0] & val) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeXOR(final int addr,final int val) {
		short[] buff = {0};
		implReadI(addr,buff);
		buff[0] = (short)((buff[0] ^ val) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeBit0(final int addr,final int bit) {
		short[] buff = {0};
		implReadI(addr,buff);
		buff[0] = (short)((buff[0] & ~(1<<bit)) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeBit1(final int addr,final int bit) {
		short[] buff = {0};
		implReadI(addr,buff);
		buff[0] = (short)((buff[0] |  (1<<bit)) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void asyncWriteVal(int addr,int val) {asyncBreakIn(()->writeVal(addr,val));}
	public void asyncWrite_OR(int addr,int val) {asyncBreakIn(()->write_OR(addr,val));}
	public void asyncWriteAND(int addr,int val) {asyncBreakIn(()->writeAND(addr,val));}
	public void asyncWriteXOR(int addr,int val) {asyncBreakIn(()->writeXOR(addr,val));}
	public void asyncWriteBit0(int addr,int bit) {asyncBreakIn(()->writeBit0(addr,bit));}
	public void asyncWriteBit1(int addr,int bit) {asyncBreakIn(()->writeBit1(addr,bit));}
	
	
	protected native void implOpenRtu();	
	protected native void implOpenTcp();
	protected native void implSlaveID(int slave_id);
	protected native void implReadC(int addr,short buff[]);//coils status(FC=1)
	protected native void implReadS(int addr,short buff[]);//input status(FC=2)	
	protected native void implReadH(int addr,short buff[]);//holding register
	protected native void implReadI(int addr,short buff[]);//input register
	protected native void implWrite(int addr,short buff[]);
	protected native void implClose();
}
