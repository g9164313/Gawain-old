package prj.scada;

import java.util.ArrayList;

import javafx.scene.Node;
import narl.itrc.DevTTY;
import narl.itrc.Misc;

/**
 * Fatek PLC Controller
 * @author qq
 *
 */
public class DevFatek extends DevTTY {

	public DevFatek(){		
	}
	
	public DevFatek(String name){
		connect(name);
	} 
	
	/**
	 * connect TTY port and prepare to send commands.<p>
	 * @param name - device path name
	 * @return TRUE-success,<p> FALSE-fail to open TTY port
	 */
	public boolean connect(String name){
		if(open(name,"19200,8n1")<=0L){
			return false;
		}
		return true;
	}
	
	/**
	 * just close TTY device
	 */
	public void disconnect(){
		close();
	}
	//--------------------------------//
	
	private static final byte STX = 0x02;
	private static final byte ETX = 0x03;
	private static final int LEN = 8;
	private static final int HDR = 5;
	
	public static final int VAL_DISABLE = 1;
	public static final int VAL_ENABLE  = 2;
	public static final int VAL_SET     = 3;
	public static final int VAL_RESET   = 4;
	
	public void getStatus(int idx){
		
		byte[] buf = new byte[LEN];
		
		wrapper(idx,0x40,buf);
	}
	
	public void makeSwitch(int idx,boolean flag){
		
		byte[] buf = new byte[LEN+1];
		
		buf[HDR+0] = (byte)((flag)?('1'):('0'));
		
		wrapper(idx,0x41,buf);
	}
	
	public void setNode(int idx,String name,int... argv){
		
		byte[] buf = null;		
		int cmd;
		
		if(argv.length==1){
			
			cmd = 0x42;
			buf = new byte[LEN+1+name.length()];
			
			buf[HDR+0] = (byte)(0x30+argv[0]);
			
			pave_txt(name,buf,1);
			
		}else{
			
			cmd = 0x45;
			buf = new byte[LEN+2+name.length()+argv.length];
			
			buf[HDR+0] = (byte)(0x30+((argv.length & 0xF0)>>4));
			buf[HDR+1] = (byte)(0x30+((argv.length & 0x0F)   ));
			
			int off = pave_txt(name,buf,2);
			
			for(int i=0; i<argv.length; i++){
				buf[off+i] = (byte)(0x30+(argv[i]));
			}
		}
		wrapper(idx,cmd,buf);
	}
	
	public void getNodeSwitch(int idx,String name,int cnt){
		
		get_node(idx,0x43,name,cnt);
	}
		
	public void getNodeStatus(int idx,String name,int cnt){
				
		get_node(idx,0x44,name,cnt);
	}
	
	private void get_node(int idx,int cmd,String name,int cnt){
		
		byte[] buf = new byte[LEN+2+name.length()];
		
		buf[HDR+0] = (byte)(0x30+((cnt & 0xF0)>>4));
		buf[HDR+1] = (byte)(0x30+((cnt & 0x0F)   ));
		
		pave_txt(name,buf,2);
		
		wrapper(idx,cmd,buf);
	}
	
	public void setRegister(int idx,String name,int... argv){
		
		int d_size = 0;
		if(name.length()>=7){
			d_size = 8;
		}else{
			d_size = 4;
		}
		
		byte[] buf = new byte[LEN+2+name.length()+argv.length*d_size];
		
		buf[HDR+0] = (byte)(0x30+((argv.length & 0xF0)>>4));
		buf[HDR+1] = (byte)(0x30+((argv.length & 0x0F)   ));
		
		int off = pave_txt(name,buf,2);
		
		for(int i=0; i<argv.length; i++, off+=d_size){
			int data = argv[i];
			if(d_size==4){
				buf[off+0] = (byte)(0x30+((data&0xF000)>>12));
				buf[off+1] = (byte)(0x30+((data&0x0F00)>>8 ));
				buf[off+2] = (byte)(0x30+((data&0x00F0)>>4 ));
				buf[off+3] = (byte)(0x30+((data&0x000F)    ));
			}else{
				buf[off+0] = (byte)(0x30+((data&0xF0000000)>>28));
				buf[off+1] = (byte)(0x30+((data&0x0F000000)>>24));
				buf[off+2] = (byte)(0x30+((data&0x00F00000)>>20));
				buf[off+3] = (byte)(0x30+((data&0x000F0000)>>16));
				buf[off+4] = (byte)(0x30+((data&0x0000F000)>>12));
				buf[off+5] = (byte)(0x30+((data&0x00000F00)>>8 ));
				buf[off+6] = (byte)(0x30+((data&0x000000F0)>>4 ));
				buf[off+7] = (byte)(0x30+((data&0x0000000F)    ));
			}
		}		
		wrapper(idx,0x47,buf);
	}
	
	public void getRegister(int idx,String name,int cnt){
		
		byte[] buf = new byte[LEN+2+name.length()];
		
		buf[HDR+0] = (byte)(0x30+((cnt & 0xF0)>>4));
		buf[HDR+1] = (byte)(0x30+((cnt & 0x0F)   ));
		
		pave_txt(name,buf,2);
		
		wrapper(idx,0x46,buf);		
	}
	
	public void set(int idx,Object... argv){
		
		if(argv.length%2!=0){
			Misc.loge("the number of arguments must be even.");
			return;
		}
		
		int total = 0;		
		for(int i=0; i<argv.length; i+=2){
			
			String name = (String)argv[i+0];
			
			int size = name.length();
			
			total = total + size;

			if(size>=7){
				total+=8;
			}else if(size==6){
				total+=4;
			}else{
				total+=1;
			}
		}
		
		int count = argv.length/2; 
		
		byte[] buf = new byte[LEN+2+total];
			
		buf[HDR+0] = (byte)(0x30+((count & 0xF0)>>4));
		buf[HDR+1] = (byte)(0x30+((count & 0x0F)   ));
		
		int off = 2;
		for(int i=0; i<count; i++){
			
			String name = (String)argv[i+0];
			int    data = (int   )argv[i+1];
			
			off = pave_txt(name,buf,off);
			
			int size = name.length();
			if(size<=5){
				buf[off+0] = (byte)(0x30+(data&0xFF));
				off+=1;
			}else if(size==6){
				buf[off+0] = (byte)(0x30+((data&0xF000)>>12));
				buf[off+1] = (byte)(0x30+((data&0x0F00)>>8 ));
				buf[off+2] = (byte)(0x30+((data&0x00F0)>>4 ));
				buf[off+3] = (byte)(0x30+((data&0x000F)    ));
				off+=4;
			}else if(size>=7){
				buf[off+0] = (byte)(0x30+((data&0xF0000000)>>28));
				buf[off+1] = (byte)(0x30+((data&0x0F000000)>>24));
				buf[off+2] = (byte)(0x30+((data&0x00F00000)>>20));
				buf[off+3] = (byte)(0x30+((data&0x000F0000)>>16));
				buf[off+4] = (byte)(0x30+((data&0x0000F000)>>12));
				buf[off+5] = (byte)(0x30+((data&0x00000F00)>>8 ));
				buf[off+6] = (byte)(0x30+((data&0x000000F0)>>4 ));
				buf[off+7] = (byte)(0x30+((data&0x0000000F)    ));
				off+=8;
			}
		}
		
		wrapper(idx,0x49,buf);
	}
	
	public void get(int idx,String... name){
		
		int cnt = name.length;
		
		int total = 0;
		for(int i=0; i<cnt; i++){
			total = total + name[i].length(); 
		}
		
		byte[] buf = new byte[LEN+2+total];
		
		buf[HDR+0] = (byte)(0x30+((cnt & 0xF0)>>4));
		buf[HDR+1] = (byte)(0x30+((cnt & 0x0F)   ));
		
		int off = 2;
		for(int i=0; i<cnt; i++){
			off = pave_txt(name[i],buf,off);
		}
				
		wrapper(idx,0x48,buf);
	}
	
	private int pave_txt(String txt,byte[] buf,int off){
		int j = HDR + off;
		for(int i=0; i<txt.length(); i++,j++){
			buf[j+i] = (byte)(txt.charAt(i));
		}
		return j;
	}
	
	private void wrapper(int idx,int cmd,byte[] buf){
		
		int ed = buf.length-1;
		buf[ 0] = STX;
		buf[ 1] = (byte)(0x30+((idx & 0xF0)>>4));
		buf[ 2] = (byte)(0x30+((idx & 0x0F)   ));
		buf[ 3] = (byte)(0x30+((cmd & 0xF0)>>4));
		buf[ 4] = (byte)(0x30+((cmd & 0x0F)   ));		
		buf[ed] = ETX;
		
		int chk = 0;
		for(int i=0; i<ed-2;i++){
			chk = chk + buf[i];
		}
		buf[ed-2] = (byte)(0x30+((chk & 0xF0)>>4));
		buf[ed-1] = (byte)(0x30+((chk & 0x0F)   ));
	}
	
	@Override
	protected Node eventLayout() {
		return null;
	}	
}
