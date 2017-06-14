package prj.scada;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

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
		if(open(name,"57600,7e1")<=0L){
			return false;
		}
		return true;
	}
	
	/**
	 * Connect device with default property setting
	 * @return
	 */
	public boolean connect(){
		String name = Gawain.prop.getProperty("DevFatek", null);
		if(name==null){
			return false;
		}
		if(open(name)<=0L){
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
	private static final int  LEN = 8;
	private static final int  HDR = 5;
	
	public static final int VAL_DISABLE = 1;
	public static final int VAL_ENABLE  = 2;
	public static final int VAL_SET     = 3;
	public static final int VAL_RESET   = 4;
	
	private final byte[] cmd_buf = new byte[LEN];
	
	public byte[] getSysInfo(int idx){
		wrapper(idx,0x40,cmd_buf);
		writeBuf(cmd_buf);
		return readBuf();
	}
	
	public byte[] getSysAll(int idx){
		wrapper(idx,0x53,cmd_buf);
		writeBuf(cmd_buf);
		return readBuf();
	}
	
	public String testEcho(int idx,String txt){
		byte[] buf = new byte[LEN+txt.length()];
		pave_txt(txt,buf,HDR);
		wrapper(idx,0x4E,buf);
		writeBuf(cmd_buf);
		return readMsg(STX,ETX);
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
			
			pave_txt(name,buf,HDR+1);
			
		}else{
			
			cmd = 0x45;
			buf = new byte[LEN+2+name.length()+argv.length];
			
			buf[HDR+0] = (byte)(0x30+((argv.length & 0xF0)>>4));
			buf[HDR+1] = (byte)(0x30+((argv.length & 0x0F)   ));
			
			int off = pave_txt(name,buf,HDR+2);
			
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
		
		pave_txt(name,buf,HDR+2);
		
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
		
		int off = pave_txt(name,buf,HDR+2);
		
		for(int i=0; i<argv.length; i++, off+=d_size){
			int data = argv[i];
			if(d_size==4){
				buf[off+0] = val2hex(data,12);
				buf[off+1] = val2hex(data, 8);
				buf[off+2] = val2hex(data, 4);
				buf[off+3] = val2hex(data, 0);
			}else{
				buf[off+0] = val2hex(data,28);
				buf[off+1] = val2hex(data,24);
				buf[off+2] = val2hex(data,20);
				buf[off+3] = val2hex(data,16);
				buf[off+4] = val2hex(data,12);
				buf[off+5] = val2hex(data, 8);
				buf[off+6] = val2hex(data, 4);
				buf[off+7] = val2hex(data, 0);
			}
		}
		
		wrapper(idx,0x47,buf);
	}
	
	public void getRegister(int idx,String name,int cnt){
		
		byte[] buf = new byte[LEN+2+name.length()];
		
		buf[HDR+0] = (byte)(0x30+((cnt & 0xF0)>>4));
		buf[HDR+1] = (byte)(0x30+((cnt & 0x0F)   ));
		
		pave_txt(name,buf,HDR+2);
		
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
		
		for(int i=0, off=HDR+2; i<count; i++){
			
			String name = (String)argv[i+0];
			int    data = (int   )argv[i+1];
			
			off = pave_txt(name,buf,off);
			
			int size = name.length();
			if(size<=5){
				buf[off+0] = (byte)(0x30+(data&0xFF));
				off+=1;
			}else if(size==6){
				buf[off+0] = val2hex(data,12);
				buf[off+1] = val2hex(data, 8);
				buf[off+2] = val2hex(data, 4);
				buf[off+3] = val2hex(data, 0);
				off+=4;
			}else if(size>=7){
				buf[off+0] = val2hex(data,28);
				buf[off+1] = val2hex(data,24);
				buf[off+2] = val2hex(data,20);
				buf[off+3] = val2hex(data,16);
				buf[off+4] = val2hex(data,12);
				buf[off+5] = val2hex(data, 8);
				buf[off+6] = val2hex(data, 4);
				buf[off+7] = val2hex(data, 0);
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
		
		for(int i=0,off=HDR+2; i<cnt; i++){
			off = pave_txt(name[i],buf,off);
		}
				
		wrapper(idx,0x48,buf);
	}
	
	private int pave_txt(String txt,byte[] buf,int off){
		int len = txt.length();
		for(int i=0; i<len; i++){
			buf[off+i] = (byte)(txt.charAt(i));
		}
		return off+len;
	}
	
	private void wrapper(int idx,int cmd,byte[] buf){
		
		int ed = buf.length-1;
		buf[ 0] = STX;
		buf[ 1] = val2hex(idx,4);
		buf[ 2] = val2hex(idx,0);
		buf[ 3] = val2hex(cmd,4);
		buf[ 4] = val2hex(cmd,0);
		buf[ed] = ETX;
		
		int chk = 0;
		for(int i=0; i<ed-2;i++){
			chk = chk + buf[i];
		}
		buf[ed-2] = val2hex(chk,4);
		buf[ed-1] = val2hex(chk,0);
	}
	
	private byte val2hex(int val, int bit){
		int msk = (0x0000000F)<<bit;
		val = val & msk;
		val = val >> bit;
		if(0<=val && val<=9){
			val = val + 0x30;
		}else{
			val = val + 0x37;//'A'~'F'
		}
		return (byte)val;
	}
	
	private int hex2val(byte hex){
		int val = 0;
		if(0x30<=hex && hex<=0x39){
			val = val - 0x30; 
		}else{
			val = val - 0x37;
		}
		return val;
	}
	
	private int hex2val(byte hexH,byte hexL){
		return 16*hex2val(hexH) + hex2val(hexL);
	}
	//--------------------------------//
	
	@Override
	protected Node eventLayout(PanBase pan) {
		
		final String TXT_UNKNOW = "？？？";
		
		HBox lay1 = new HBox();
		lay1.getStyleClass().add("hbox-medium");
		
		GridPane lay2 = new GridPane();//show all sensor
		lay2.getStyleClass().add("grid-medium");
		
		Label[] txtInfo = {
			new Label(TXT_UNKNOW), new Label(TXT_UNKNOW),
			new Label(TXT_UNKNOW), new Label(TXT_UNKNOW),
			new Label(TXT_UNKNOW), new Label(TXT_UNKNOW),
			new Label(TXT_UNKNOW), new Label(TXT_UNKNOW),
			new Label(TXT_UNKNOW), new Label(TXT_UNKNOW),
			new Label(TXT_UNKNOW), new Label(TXT_UNKNOW),
			new Label(TXT_UNKNOW), new Label(TXT_UNKNOW),
			new Label(TXT_UNKNOW), new Label(TXT_UNKNOW),
			new Label(TXT_UNKNOW), new Label(TXT_UNKNOW),
			new Label(TXT_UNKNOW), new Label(TXT_UNKNOW),
			new Label(TXT_UNKNOW),
		};//see manual, the sequence is important!!!!
		
		lay2.add(new Label("狀態1"     ), 0, 0); lay2.add(txtInfo[ 0], 1, 0);		
		lay2.add(new Label("狀態2"     ), 0, 1); lay2.add(txtInfo[ 5], 1, 1); 
		lay2.add(new Label("ＩＤ"      ), 0, 2); lay2.add(txtInfo[ 4], 1, 2); 
		lay2.add(new Label("階梯圖"    ), 0, 3); lay2.add(txtInfo[ 1], 1, 3); 
		lay2.add(new Label("階梯大小"  ), 0, 4); lay2.add(txtInfo[ 9], 1, 4); 
		lay2.add(new Label("看門狗"    ), 0, 5); lay2.add(txtInfo[ 3], 1, 5); 
		lay2.add(new Label("主機類型"  ), 0, 6); lay2.add(txtInfo[ 6], 1, 6); 
		lay2.add(new Label("Ｉ／Ｏ個數"), 0, 7); lay2.add(txtInfo[ 7], 1, 7); 
		lay2.add(new Label("版本"      ), 0, 8); lay2.add(txtInfo[ 8], 1, 8); 
		lay2.add(new Label("使用記憶體"), 0, 9); lay2.add(txtInfo[ 2], 1, 9); 
		
		lay2.add(new Label("計時器"    ), 3, 0); lay2.add(txtInfo[19], 4, 0);
		lay2.add(new Label("記數器"    ), 3, 1); lay2.add(txtInfo[20], 4, 1);
		lay2.add(new Label("Ｄ－輸入"  ), 3, 2); lay2.add(txtInfo[10], 4, 2);
		lay2.add(new Label("Ｄ－輸出"  ), 3, 3); lay2.add(txtInfo[11], 4, 3);
		lay2.add(new Label("Ｉ－暫存器"), 3, 4); lay2.add(txtInfo[12], 4, 4);
		lay2.add(new Label("Ｏ－暫存器"), 3, 5); lay2.add(txtInfo[13], 4, 5);
		lay2.add(new Label("Ｒ－暫存器"), 3, 6); lay2.add(txtInfo[17], 4, 6);
		lay2.add(new Label("Ｄ－暫存器"), 3, 7); lay2.add(txtInfo[18], 4, 7); 
		lay2.add(new Label("Ｍ－繼電器"), 3, 8); lay2.add(txtInfo[14], 4, 8);
		lay2.add(new Label("Ｌ－繼電器"), 3, 9); lay2.add(txtInfo[16], 4, 9);
		lay2.add(new Label("Ｓ－繼電器"), 3,10); lay2.add(txtInfo[15], 4,10);
		
		lay2.add(new Separator(Orientation.VERTICAL), 2, 0, 1, 10);
		lay2.add(new Separator(Orientation.VERTICAL), 5, 0, 1, 11);
		
		VBox lay3 = new VBox();
		lay3.getStyleClass().add("vbox-medium");
		
		Button btnSysAll = PanBase.genButton2("更新狀態",null);
		lay3.getChildren().addAll(btnSysAll);
		
		lay1.getChildren().addAll(lay2,lay3);
		return lay1;
	}	
}
