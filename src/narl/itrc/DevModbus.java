package narl.itrc;

import java.util.Hashtable;

public class DevModbus implements Gawain.EventHook {

	private long ptrCntx = 0L;//it is same as NULL point
	
	private final int TYPE_RTU = 0;
	private final int TYPE_TCP = 1;
	
	private int conType = TYPE_RTU;
	private int rtuAddr = -1;
	
	private static Hashtable<String,Long> tblName = new Hashtable<String,Long>();
	
	public DevModbus(){
		Gawain.hook(this);
	}
	
	public DevModbus(String name){
		open(name);
	}
	
	@Override
	public void shutdown() {
		close();
	}
	
	public int open(String name){
		String[] val = name.trim().split(",");
		if(val[0].toLowerCase().startsWith("rtu")==true){
			if(val.length<4){
				return -1;
			}
			conType = TYPE_RTU;
			try{
				val[3] = val[3].toUpperCase();
				String key = val[0]+"-"+val[1];				
				int  baud  = Integer.valueOf(val[2]);				
				int  d_bit = val[3].charAt(0)-'0';
				char parity= val[3].charAt(1);
				int  s_bit = val[3].charAt(2)-'0';
				rtuAddr = Integer.valueOf(val[4]);
				if(tblName.get(key)!=null){
					//we have this context, so just take it~~~
					ptrCntx = tblName.get(key);
					return 0;
				}
				openRtu(val[1],baud,d_bit,parity,s_bit);
				if(ptrCntx!=0){
					//keep this context~~~
					tblName.put(key, ptrCntx);
				}
				return 0;
			}catch(NumberFormatException e){
				return -1;
			}
		}else{
			Misc.logv("fail to parse - '"+name+"'");
		}
		return -2;
	}

	public boolean isValid(){
		return (ptrCntx==0)?(false):(true);
	}
	
	public native void openRtu(
		String name,
		int baud,
		int data_bit,
		char parity,
		int stop_bit
	);
	
	public native void readH(int idx,short buf[]);//holding(input) register
	public native void readR(int idx,short buf[]);//register
	public native void write(int idx,short buf[]);//register
	
	public native void close();
}
