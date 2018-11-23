package narl.itrc;

public class DevModbus {

	private long ptrCntx = 0L;//it is same as NULL point
	
	private int rtuAddr = 1;//default ID is '1'

	public DevModbus(){
	}
	
	/**
	 * Create object and connect device immediately
	 * @param path - device name and connection attributes
	 */
	public DevModbus(String path){
		open(path);
	}
	//---------------------//
	
	
	/**
	 * Choose which type connection, format is below:
	 * RTU:[device name],[baud-rate],[8n1],[index]
	 * TCP:[IP address],[port]
	 * @param name - attributes for connection type
	 * @return pointer
	 */
	public long open(String name){
		
		ptrCntx = 0L;//reset pointer!!!
		rtuAddr = 1;//reset RTU-address
		
		name = name.toUpperCase();
		if(name.length()<=4){
			Misc.loge("Wrong format - '"+name+"'");
			return ptrCntx; 
		}
		String[] val = name.substring(4).split(",");

		//native code will overwrite 'ptrCntx' variable!!!
		if(name.startsWith("RTU:")==true){
			//format: RTU:[device name],9600,8n1,1
			int  baud = 9600;
			int  d_bit= 8;
			char p_bit='N';
			int  s_bit= 1;
			try{
				if(val.length>=2){
					baud  = Integer.valueOf(val[1]);
				}
				if(val.length>=3){
					d_bit = val[2].charAt(0)-'0';
					p_bit = val[2].charAt(1);//parity-bit
					s_bit = val[2].charAt(2)-'0';
				}
				if(val.length>=4){
					rtuAddr = Integer.valueOf(val[3]);
				}else{
					rtuAddr = 1;//default address
				}
			}catch(NumberFormatException e){
				return ptrCntx;
			}
			openRtu(val[0],baud,d_bit,p_bit,s_bit);
			
		}else if(name.startsWith("TCP:")==true){
			//format: TCP:[IP位置],[port]
			int port = 502;
			try{
				if(val.length>=2){
					port = Integer.valueOf(val[1]);
				}
			}catch(NumberFormatException e){
				return ptrCntx;
			}
			openTcp(val[0],port);
			
		}else{
			Misc.loge("fail to parse - '"+name+"'");
		}
		return ptrCntx;
	}

	public boolean isValid(){
		return (ptrCntx==0)?(false):(true);
	}
	
	public void setValue(int idx, int val){
		final short[] tmp = { (short)val };
		write(idx,tmp);
	}
	public int getValue(int idx){
		final short[] tmp = { 0 };
		readR(idx,tmp);
		return ((int)tmp[0])&0xFFFF;
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
