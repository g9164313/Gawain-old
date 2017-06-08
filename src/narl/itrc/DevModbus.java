package narl.itrc;

public class DevModbus extends DevBase {

	private long ptrCntx = 0L;//it is same as NULL point
	
	private int rtuAddr = -1;

	public DevModbus(){
	}
	
	/**
	 * Create object and connect device immediately
	 * @param path - device name and connection attributes
	 */
	public DevModbus(String path){
		open(path);
	}
	
	@Override
	void eventShutdown() {
		close();		
	}
	//---------------------//
	
	public long open(String name){
		
		String[] val = name.trim().split(",");		
		val[0] = val[0].toLowerCase();
		if(val.length<=1){
			return 0;
		}
		
		//'ptrCntx' will be overwrote by native code!!!
		
		if(val[0].startsWith("rtu")==true){
			
			//format: RTU,[device name],9600,8n1,1
			
			if(val.length<4){
				return 0;
			}
			int  baud = 9600;
			int  d_bit= 8;
			char p_bit='N';
			int  s_bit= 1;
			try{
				if(val.length>=3){
					baud  = Integer.valueOf(val[2]);
				}
				if(val.length>=4){
					d_bit = val[3].charAt(0)-'0';
					p_bit = val[3].charAt(1);//parity-bit
					s_bit = val[3].charAt(2)-'0';
				}
				if(val.length>=5){
					rtuAddr = Integer.valueOf(val[4]);
				}else{
					rtuAddr = 1;//default address
				}
			}catch(NumberFormatException e){
				return 0;
			}
			openRtu(val[1],baud,d_bit,p_bit,s_bit);
			
		}else if(val[0].startsWith("tcp")==true){
			
			//format: TCP,[IP位置],[port]
			int port = 502;
			try{
				if(val.length>=3){
					port = Integer.valueOf(val[2]);
				}
				rtuAddr = -1;//we don't need this~~~
			}catch(NumberFormatException e){
				return -1;
			}
			openTcp(val[1],port);
			
		}else{
			Misc.logv("fail to parse - '"+name+"'");
		}
		return ptrCntx;
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
	
	public native void openTcp(
		String ipaddr,
		int port
	);
	
	public native void readH(int idx,short buf[]);//holding(input) register
	public native void readR(int idx,short buf[]);//register
	public native void write(int idx,short buf[]);//register
	
	public native void close();
}
