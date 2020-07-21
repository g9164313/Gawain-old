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
		char    type;
		int     address;
		short[] values;
		IntegerProperty[] _values;
		
		//public RCell(final char typ, final int addr) {
		//	this(typ,addr,1);
		//}
		public RCell(final char typ, final int addr, final int size) {
			type = typ;
			address= addr;
			values = new short[size];
			_values= new IntegerProperty[size];
			for(int i=0; i<size; i++) {
				_values[i] = new SimpleIntegerProperty();
			}
		}
		
		void fecth() {
			if(values==null) {
				return;
			}
			switch(type) {
			//coils ??? support ???
			case 'C':
				break;
			case 'R':
			case 'I':
				//input register
				implReadR(address,values);
				break;
			case 'H':
				//holding register
				implReadH(address,values);
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
				_values[i].set(v);
			}
		}
	};
	
	private ArrayList<RCell> mems = new ArrayList<>();
	
	protected void ignite() {
		//user can override this to prepare something.
		//Remember this is not called by GUI-thread.
		nextState(STG_LOOPER);
	}
	
	private void looper() {
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			return;
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
		final String... address
	) {
		for(String txt:address) {
			txt = txt.toUpperCase();
			if(txt.matches("[CHIR]\\d+([-]\\d+)?")==false) {
				continue;
			}else if(txt.length()==0){
				continue;
			}
			char typ = txt.toUpperCase().charAt(0);
			int off = 0;
			int cnt = 1;
			String[] col = txt.substring(1).split("-");
			if(col.length>=1) {
				off = Integer.valueOf(col[0],radix);
			}
			if(col.length>=2) {
				cnt = Integer.valueOf(col[1],radix) - off + 1;
				if(cnt<=0) {
					return this;
				}
			}
			mems.add(new RCell(typ,off,cnt));
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
		return mapAddress(10,address);
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
		return mapAddress(16,address);
	}
	
	private IntegerProperty register(final char type, final int addr) {
		for(RCell reg:mems) {
			int beg = reg.address;
			int end = reg.address + reg.values.length - 1;
			if(beg<=addr && addr<=end && reg.type==type) {				
				int off = addr - beg;
				return reg._values[off];
			}
		}
		return null;
	}
	public IntegerProperty coilRegister(final int address) {
		return register('C',address); 
	}
	public IntegerProperty holdingRegister(final int address) {
		return register('H',address);
	}
	public IntegerProperty inputRegister(final int address) {
		return register('R',address);
	}
	
	public void asyncWriteVal(int addr,int val) {asyncBreakIn(()->writeVal(addr,val));}
	public void asyncWrite_OR(int addr,int val) {asyncBreakIn(()->write_OR(addr,val));}
	public void asyncWriteAND(int addr,int val) {asyncBreakIn(()->writeAND(addr,val));}
	public void asyncWriteXOR(int addr,int val) {asyncBreakIn(()->writeXOR(addr,val));}
	public void asyncWriteBit0(int addr,int bit) {asyncBreakIn(()->writeBit0(addr,bit));}
	public void asyncWriteBit1(int addr,int bit) {asyncBreakIn(()->writeBit1(addr,bit));}
	
	public void writeVal(int addr,int val) {
		short[] buff = { (short)(val&0xFFFF) };
		implWrite(addr,buff);
	}
	public void write_OR(int addr,int val) {
		short[] buff = {0};
		implReadR(addr,buff);
		buff[0] = (short)((buff[0] | val) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeAND(int addr,int val) {
		short[] buff = {0};
		implReadR(addr,buff);
		buff[0] = (short)((buff[0] & val) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeXOR(int addr,int val) {
		short[] buff = {0};
		implReadR(addr,buff);
		buff[0] = (short)((buff[0] ^ val) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeBit0(int addr,int bit) {
		short[] buff = {0};
		implReadR(addr,buff);
		buff[0] = (short)((buff[0] & ~(1<<bit)) & 0xFFFF);
		implWrite(addr,buff);
	}
	public void writeBit1(int addr,int bit) {
		short[] buff = {0};
		implReadR(addr,buff);
		buff[0] = (short)((buff[0] |  (1<<bit)) & 0xFFFF);
		implWrite(addr,buff);
	}
		
	protected native void implOpenRtu();	
	protected native void implOpenTcp();
	protected native void implReadH(int addr,short buff[]);//holding register
	protected native void implReadR(int addr,short buff[]);//input register
	protected native void implWrite(int addr,short buff[]);//write register
	protected native void implClose();
}
