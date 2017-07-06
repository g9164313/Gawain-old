package prj.scada;

import java.text.Format;
import java.text.NumberFormat;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.util.converter.BooleanStringConverter;
import javafx.util.converter.FormatStringConverter;
import javafx.util.converter.NumberStringConverter;
import narl.itrc.BoxLogger;
import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.JBoxValInteger;
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
		//Finally, retrieve feedback
		byte[] info = readPackBuck(STX,ETX);
		event_last_error(info[HDR-1]);
		event_inf( hex2val(info[HDR+0],info[HDR+1]) );		
		return info;
	}
	
	public byte[] getSysAll(int idx){
		wrapper(idx,0x53,cmd_buf);
		writeBuf(cmd_buf);
		//Finally, retrieve feedback
		byte[] info = readPackBuck(STX,ETX);
		event_last_error(info[HDR-1]);
		event_inf( hex2val(info[HDR+0], info[HDR+1]) );
		event_typ( hex2val(info[HDR+2], info[HDR+3]) );
		event_nio( info[HDR+4], info[HDR+5] );
		event_ver( info[HDR+6], info[HDR+7] );
		event_var(
			hex2val(info[HDR+ 8], info[HDR+ 9], info[HDR+10], info[HDR+11]),
			hex2val(info[HDR+12], info[HDR+13], info[HDR+14], info[HDR+15]),
			hex2val(info[HDR+16], info[HDR+17], info[HDR+18], info[HDR+19]),
			hex2val(info[HDR+20], info[HDR+21], info[HDR+22], info[HDR+23]),
			hex2val(info[HDR+24], info[HDR+25], info[HDR+26], info[HDR+27]),
			hex2val(info[HDR+28], info[HDR+29], info[HDR+30], info[HDR+31]),
			hex2val(info[HDR+32], info[HDR+33], info[HDR+34], info[HDR+35]),
			hex2val(info[HDR+36], info[HDR+37], info[HDR+38], info[HDR+39]),
			hex2val(info[HDR+40], info[HDR+41], info[HDR+42], info[HDR+43]),
			hex2val(info[HDR+44], info[HDR+45], info[HDR+46], info[HDR+47]),
			hex2val(info[HDR+48], info[HDR+49], info[HDR+50], info[HDR+51]),
			hex2val(info[HDR+52], info[HDR+53], info[HDR+54], info[HDR+55])
		);
		return info;
	}
	
	public String testEcho(int idx,String txt){
		byte[] buf = new byte[LEN+txt.length()];
		pave_txt(txt,buf,HDR);
		wrapper(idx,0x4E,buf);
		writeBuf(buf);
		return byte2txt(readPackBuck(STX,ETX));
	}
	
	public void makeSwitch(int idx,boolean flag){
		byte[] buf = new byte[LEN+1];
		buf[HDR+0] = (byte)((flag)?('1'):('0'));
		wrapper(idx,0x41,buf);
		writeBuf(buf);
		//Finally, retrieve feedback
		buf = readPackBuck(STX,ETX);
		event_last_error(buf[HDR-1]);
	}
	
	public void setNode(int idx,String name,int argv){
		byte[] buf = new byte[LEN+1+name.length()];		
		buf[HDR+0] = val2hex_L(argv);
		pave_txt(name,buf,HDR+1);
		wrapper(idx,0x42,buf);
		writeBuf(buf);
		//Finally, retrieve feedback
		buf = readPackBuck(STX,ETX);
		event_last_error(buf[HDR-1]);
	}
	
	public void setNode(int idx,String name,int... argv){
		byte[] buf = new byte[LEN+2+name.length()+argv.length];
		int cnt = argv.length;
		buf[HDR+0] = val2hex_H(cnt);
		buf[HDR+1] = val2hex_L(cnt);	
		int off = pave_txt(name,buf,HDR+2);		
		for(int i=0; i<argv.length; i++){
			buf[off+i] = val2hex_L(argv[i]);
		}	
		wrapper(idx,0x45,buf);
		writeBuf(buf);
		//Finally, retrieve feedback
		buf = readPackBuck(STX,ETX);
		event_last_error(buf[HDR-1]);
	}
	
	public int getNodeEnable(int idx,String name){
		int[] tmp = {0};
		tmp = getNodeEnable(idx,name,1);
		return tmp[0];
	}
	
	public int[] getNodeEnable(int idx,String name,int cnt){
		return get_node(idx,0x43,name,cnt);
	}
		
	public int[] getNodeStatus(int idx,String name,int cnt){		
		return get_node(idx,0x44,name,cnt);
	}
		
	private int[] get_node(int idx,int cmd,String name,int cnt){
		byte[] buf = new byte[LEN+2+name.length()];
		buf[HDR+0] = val2hex_H(cnt);
		buf[HDR+1] = val2hex_L(cnt);
		pave_txt(name,buf,HDR+2);	
		wrapper(idx,cmd,buf);
		writeBuf(buf);
		//Finally, retrieve feedback
		buf = readPackBuck(STX,ETX);
		event_last_error(buf[HDR-1]);
		int[] resp = new int[cnt];
		for(int i=0; i<cnt; i++){
			resp[i] = val2hex_L(buf[HDR+1+i]);
		}
		return resp;
	}
	
	public int setRegister(int idx,String name,int... argv){
		int d_size = 0;
		if(name.length()>=7){
			d_size = 8;
		}else{
			d_size = 4;
		}
		byte[] buf = new byte[LEN+2+name.length()+argv.length*d_size];
		int cnt = argv.length;
		buf[HDR+0] = val2hex_H(cnt);
		buf[HDR+1] = val2hex_L(cnt);
		int off = pave_txt(name,buf,HDR+2);
		for(int i=0; i<cnt; i++, off+=d_size){
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
		writeBuf(buf);
		//Finally, retrieve feedback
		buf = readPackBuck(STX,ETX);
		event_last_error(buf[HDR-1]);
		return (char)(buf[HDR-1]);
	}
	
	public int[] getRegister(int idx,String name,int cnt){
		int d_size = (name.length()>=7)?(8):(4);
		byte[] buf = new byte[LEN+2+name.length()];
		buf[HDR+0] = val2hex_H(cnt);
		buf[HDR+1] = val2hex_L(cnt);
		pave_txt(name,buf,HDR+2);	
		wrapper(idx,0x46,buf);
		writeBuf(buf);
		//Finally, retrieve feedback
		buf = readPackBuck(STX,ETX);
		event_last_error(buf[HDR-1]);
		int[] resp = new int[cnt];
		for(int i=0, j=HDR; i<cnt; i++, j+=d_size){
			if(d_size==4){
				resp[i] = hex2val(
					buf[j+0], buf[j+1], buf[j+2], buf[j+3]
				);
			}else{
				resp[i] = hex2val(
					buf[j+0], buf[j+1], buf[j+2], buf[j+3],
					buf[j+4], buf[j+5], buf[j+6], buf[j+7]
				);
			}
		}
		return resp;
	}
	
	public char set(int idx,Object... argv){
		if(argv.length%2!=0){
			Misc.loge("the number of arguments must be even.");
			return '?';
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
				buf[off+0] = val2hex(data, 0);
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
		writeBuf(buf);
		//Finally, retrieve feedback
		buf = readPackBuck(STX,ETX);
		event_last_error(buf[HDR-1]);
		return (char)(buf[HDR-1]);
	}
	
	public int[] get(int idx,String... name){
		int cnt = name.length;
		int total = 0;
		for(int i=0; i<cnt; i++){
			total = total + name[i].length(); 
		}
		byte[] buf = new byte[LEN+2+total];
		buf[HDR+0] = val2hex_H(cnt);
		buf[HDR+1] = val2hex_L(cnt);
		for(int i=0,off=HDR+2; i<cnt; i++){
			off = pave_txt(name[i],buf,off);
		}		
		wrapper(idx,0x48,buf);
		writeBuf(buf);
		//Finally, retrieve feedback
		buf = readPackBuck(STX,ETX);
		if(buf==null){
			return null;
		}
		event_last_error(buf[HDR-1]);
		int[] resp = new int[cnt];
		for(int i=0, off=HDR; i<cnt; i++){
			int size = name[i].length();
			if(size<=5){
				resp[i] = hex2val(buf[off]);
				off+=1;
			}else if(size==6){
				resp[i] = hex2val(
					buf[off+0], buf[off+1], buf[off+2], buf[off+3]
				);				
				off+=4;
			}else if(size>=7){
				resp[i] = hex2val(
					buf[off+0], buf[off+1], buf[off+2], buf[off+3],
					buf[off+4], buf[off+5], buf[off+6], buf[off+7]
				);
				off+=8;
			}
		}
		return resp;
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
	
	private byte val2hex_H(int val){
		return val2hex(val,4);
	}
	
	private byte val2hex_L(int val){
		return val2hex(val,0);
	}
		
	private byte val2hex(int val, int shift){
		val = (val >> shift) & 0x0000000F;
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
			val = hex - 0x30; 
		}else{
			val = hex - 0x37;
		}
		return val;
	}

	private int hex2val(byte... hex){
		int val = 0;
		int pow = (int)Math.pow(16,hex.length-1);
		for(int i=0; i<hex.length;  i++){
			val = val + (pow)*(hex2val(hex[i]));
			pow = pow / 16;
		}
		return val;
	}
	
	private String byte2txt(byte[] buf){
		String txt = "";
		int cnt = buf.length;
		if(cnt==0){
			return txt;
		}
		for(int i=0; i<cnt; i++){
			txt = txt + (char)(buf[i]);
		}
		return txt;
	}	
	//--------------------------------//
	
	private final String TXT_UNKNOW = "？？？";
	
	private BooleanProperty[] staInfo = {
		new SimpleBooleanProperty(false), //B0: Run or Stop
		new SimpleBooleanProperty(false), //B1: Battery
		new SimpleBooleanProperty(false), //B2: Checksum
		new SimpleBooleanProperty(false), //B3: Memory pack
		new SimpleBooleanProperty(false), //B4: WDT timeout
		new SimpleBooleanProperty(false), //B5: Use ID or not
		new SimpleBooleanProperty(false), //B6: Emergency
	};
	
	private StringProperty staTyp = new SimpleStringProperty(TXT_UNKNOW);//machine type
	
	private StringProperty staNIO = new SimpleStringProperty(TXT_UNKNOW);//the number of IO node
	
	private StringProperty staVer = new SimpleStringProperty(TXT_UNKNOW);//OS version
	
	private IntegerProperty[] staVar = {
		new SimpleIntegerProperty(), //Ladder Size
		new SimpleIntegerProperty(), //Discrete Input(?)
		new SimpleIntegerProperty(), //Discrete Output(?)
		new SimpleIntegerProperty(), //Register Input
		new SimpleIntegerProperty(), //Register Output
		new SimpleIntegerProperty(), //M(?) relay
		new SimpleIntegerProperty(), //Step relay
		new SimpleIntegerProperty(), //L(?) relay
		new SimpleIntegerProperty(), //R Register
		new SimpleIntegerProperty(), //D Register
		new SimpleIntegerProperty(), //Timer
		new SimpleIntegerProperty(), //Counter
	};

	private void event_inf(final int val){
		final Runnable event = new Runnable(){
			@Override
			public void run() {
				staInfo[0].set( ((val & 0b0000_0001)!=0)?(true):(false) );
				staInfo[1].set( ((val & 0b0000_0010)!=0)?(true):(false) );
				staInfo[2].set( ((val & 0b0000_0100)!=0)?(true):(false) );
				staInfo[3].set( ((val & 0b0000_1000)!=0)?(true):(false) );
				staInfo[4].set( ((val & 0b0001_0000)!=0)?(true):(false) );
				staInfo[5].set( ((val & 0b0010_0000)!=0)?(true):(false) );
				staInfo[6].set( ((val & 0b0100_0000)!=0)?(true):(false) );
			}			
		};
		if(Application.isEventThread()==true){
			event.run();
		}else{
			Application.invokeAndWait(event);
		}
	}
	
	private void event_typ(final int val){
		final Runnable event = new Runnable(){
			@Override
			public void run() {
				switch(val){
				case 0: staTyp.set("MA"); break;
				case 1: staTyp.set("MC"); break;
				default: staTyp.set(TXT_UNKNOW); break;
				}
			}			
		};
		if(Application.isEventThread()==true){
			event.run();
		}else{
			Application.invokeAndWait(event);
		}
	}
	
	private void event_nio(final byte val_H, final byte val_L){
		final Runnable event = new Runnable(){
			final char[] digi = {'０','１','２','３','４','５','６','７','８','９'};
			@Override
			public void run() {
				char a = digi[hex2val(val_H)];
				char b = digi[hex2val(val_L)];
				staNIO.set("ｘ"+a+b);
			}			
		};
		if(Application.isEventThread()==true){
			event.run();
		}else{
			Application.invokeAndWait(event);
		}
	}
	
	private void event_ver(final byte val_H, final byte val_L){
		final Runnable event = new Runnable(){
			final char[] digi = {'０','１','２','３','４','５','６','７','８','９'};
			@Override
			public void run() {
				char a = digi[hex2val(val_H)];
				char b = digi[hex2val(val_L)];
				staVer.set("Ｖ"+a+"."+b+"ｘ");
			}			
		};
		if(Application.isEventThread()==true){
			event.run();
		}else{
			Application.invokeAndWait(event);
		}
	}
	
	private void event_var(final int... val){
		final Runnable event = new Runnable(){
			@Override
			public void run() {
				for(int i=0; i<val.length; i++){
					staVar[i].set(val[i]);
				}
			}			
		};
		if(Application.isEventThread()==true){
			event.run();
		}else{
			Application.invokeAndWait(event);
		}
	}
	
	private StringProperty lastError = new SimpleStringProperty("");//error code After last command
	
	private void event_last_error(final byte code){
		final Runnable event = new Runnable(){
			@Override
			public void run() {
				switch(code){
				case '0': lastError.set("通訊正常"); break;
				case '2': lastError.set("不合法的數值表示"); break;
				case '4': lastError.set("不合法的命令格式"); break;
				case '5': lastError.set("階梯圖偵錯碼不符合，無法啟動"); break;
				case '6': lastError.set("非法ID，無法啟動"); break;
				case '7': lastError.set("語法錯誤，無法啟動"); break;
				case '9': lastError.set("由階梯圖無法啟動"); break;
				case 'A': lastError.set("非法地址"); break;
				default: lastError.set(String.format("%s-(%c)",TXT_UNKNOW,(char)(code))); break;
				}
			}			
		};
		if(Application.isEventThread()==true){
			event.run();
		}else{
			Application.invokeAndWait(event);
		}
	}
	
	private Node layout_sys_state(){
		
		HBox lay1 = new HBox();
		lay1.getStyleClass().add("hbox-medium");
		
		GridPane lay2 = new GridPane();//show all sensor
		lay2.getStyleClass().add("grid-medium");
		
		Label[] txtInfo = new Label[23];
		for(int i=0; i<txtInfo.length; i++){
			txtInfo[i] = new Label();
			txtInfo[i].setPrefWidth(65);
		}
		
		txtInfo[ 0].textProperty().bind(staInfo[0].asString());//B0: Run or Stop
		txtInfo[ 1].textProperty().bind(staInfo[1].asString());//B1: Battery
		txtInfo[ 2].textProperty().bind(staInfo[2].asString());//B2: Checksum
		txtInfo[ 3].textProperty().bind(staInfo[3].asString());//B3: Memory pack
		txtInfo[ 4].textProperty().bind(staInfo[4].asString());//B4: WDT timeout
		txtInfo[ 5].textProperty().bind(staInfo[5].asString());//B5: Use ID or not
		txtInfo[ 6].textProperty().bind(staInfo[6].asString());//B6: Emergency
		
		txtInfo[ 7].textProperty().bind(staTyp);//machine type		
		txtInfo[ 8].textProperty().bind(staNIO);//the number of IO node		
		txtInfo[ 9].textProperty().bind(staVer);//OS version
		
		txtInfo[10].textProperty().bind(staVar[ 0].asString());//Ladder Size
		txtInfo[11].textProperty().bind(staVar[ 1].asString("%X"));//Discrete Input(?)
		txtInfo[12].textProperty().bind(staVar[ 2].asString("%X"));//Discrete Output(?)
		txtInfo[13].textProperty().bind(staVar[ 3].asString("%X"));//Input Register 
		txtInfo[14].textProperty().bind(staVar[ 4].asString("%X"));//Output Register 
		txtInfo[15].textProperty().bind(staVar[ 5].asString("%X"));//M(?) relay
		txtInfo[16].textProperty().bind(staVar[ 6].asString("%X"));//Step relay
		txtInfo[17].textProperty().bind(staVar[ 7].asString("%X"));//L(?) relay
		txtInfo[18].textProperty().bind(staVar[ 8].asString("%X"));//R Register
		txtInfo[19].textProperty().bind(staVar[ 9].asString("%X"));//D Register
		txtInfo[20].textProperty().bind(staVar[10].asString("%X"));//Timer
		txtInfo[21].textProperty().bind(staVar[11].asString("%X"));//Counter
		
		lay2.add(new Label("主機類型"  ), 0, 0); lay2.add(txtInfo[ 7], 1, 0); 
		lay2.add(new Label("Ｉ／Ｏ個數"), 0, 1); lay2.add(txtInfo[ 8], 1, 1); 
		lay2.add(new Label("ＯＳ版本"  ), 0, 2); lay2.add(txtInfo[ 9], 1, 2);
		lay2.add(new Label("主機運行"  ), 0, 3); lay2.add(txtInfo[ 0], 1, 3);
		lay2.add(new Label("緊急狀況"  ), 0, 4); lay2.add(txtInfo[ 6], 1, 4);
		lay2.add(new Label("電池用量"  ), 0, 5); lay2.add(txtInfo[ 1], 1, 5);
		lay2.add(new Label("ＩＤ設置"  ), 0, 6); lay2.add(txtInfo[ 5], 1, 6); 		
		lay2.add(new Label("看門狗"    ), 0, 7); lay2.add(txtInfo[ 4], 1, 7);		
		lay2.add(new Label("記憶體使用"), 0, 8); lay2.add(txtInfo[ 3], 1, 8);
		lay2.add(new Label("階梯圖合法"), 0, 9); lay2.add(txtInfo[ 2], 1, 9); 
		lay2.add(new Label("階梯圖大小"), 0,10); lay2.add(txtInfo[10], 1,10); 
		
		lay2.add(new Label("Ｄ－輸入"  ), 3, 0); lay2.add(txtInfo[11], 4, 0);
		lay2.add(new Label("Ｄ－輸出"  ), 3, 1); lay2.add(txtInfo[12], 4, 1);
		lay2.add(new Label("Ｉ－暫存器"), 3, 2); lay2.add(txtInfo[13], 4, 2);
		lay2.add(new Label("Ｏ－暫存器"), 3, 3); lay2.add(txtInfo[14], 4, 3);
		lay2.add(new Label("Ｒ－暫存器"), 3, 4); lay2.add(txtInfo[18], 4, 4);
		lay2.add(new Label("Ｄ－暫存器"), 3, 5); lay2.add(txtInfo[19], 4, 5); 
		lay2.add(new Label("Ｍ－繼電器"), 3, 6); lay2.add(txtInfo[15], 4, 6);
		lay2.add(new Label("Ｌ－繼電器"), 3, 7); lay2.add(txtInfo[17], 4, 7);
		lay2.add(new Label("Ｓ－繼電器"), 3, 8); lay2.add(txtInfo[16], 4, 8);
		lay2.add(new Label("計時器"    ), 3, 9); lay2.add(txtInfo[20], 4, 9);
		lay2.add(new Label("記數器"    ), 3,10); lay2.add(txtInfo[21], 4,10);
		
		lay2.add(new Separator(Orientation.VERTICAL), 2, 0, 1, 11);
		lay2.add(new Separator(Orientation.VERTICAL), 5, 0, 1, 11);
		
		VBox lay3 = new VBox();
		lay3.getStyleClass().add("vbox-medium-space");
		Button btnSysAll = PanBase.genButton2("更新",null);
		btnSysAll.setOnAction(e->{
			getSysAll(0x01);
		});
		Button btnSwitchOn = PanBase.genButton2("啟動",null);
		btnSwitchOn.setOnAction(e->{
			makeSwitch(0x01,true);
			getSysInfo(0x01);
		});
		Button btnSwitchOff = PanBase.genButton2("關閉",null);
		btnSwitchOff.setOnAction(e->{
			makeSwitch(0x01,false);
			getSysInfo(0x01);
		});
		lay3.getChildren().addAll(btnSysAll,btnSwitchOn,btnSwitchOff);
		
		lay1.getChildren().addAll(lay2,lay3);
		return lay1;
	}
	
	/**
	 * this present the value of node and register
	 * @author qq
	 *
	 */
	public static class CompValue {
		private final StringProperty  name  = new SimpleStringProperty();
		private final StringProperty  status= new SimpleStringProperty("");
		private final BooleanProperty enable= new SimpleBooleanProperty(false);
				
		public CompValue(String txt){
			name.set(txt);
		}
		
		public StringProperty nameProperty() { return name; }
		public String getName() { return name.get(); }
		public void setName(String txt) { name.set(txt); }
		
		public StringProperty statusProperty() { return status; }
		public String getStatus() { return status.get(); }
		public void setStatus(String txt) { status.set(txt); }
		
		public BooleanProperty enableProperty() { return enable; }
		public boolean getEnable() { return enable.get(); }
		public void setEnable(boolean txt) { enable.set(txt); }
	} ;
	
	@SuppressWarnings("unchecked")
	private Node layout_watch_node(){
		
		HBox lay1 = new HBox();
		lay1.getStyleClass().add("hbox-medium-space");
		
		final TableView<CompValue> tabNode = new TableView<CompValue>();
		tabNode.setEditable(true);

		TableColumn<CompValue,String> col1 = new TableColumn<CompValue,String>("位址");
		col1.setCellValueFactory(new PropertyValueFactory<CompValue,String>("name"));
		col1.setCellFactory(TextFieldTableCell.forTableColumn());
		
		TableColumn<CompValue,String> col2 = new TableColumn<CompValue,String>("內容");
		col2.setCellValueFactory(new PropertyValueFactory<CompValue,String>("status"));
		col2.setCellFactory(TextFieldTableCell.forTableColumn());
		
		TableColumn<CompValue,Boolean> col3 = new TableColumn<CompValue,Boolean>("致能");
		col3.setCellValueFactory(new PropertyValueFactory<CompValue,Boolean>("enable"));
		col3.setCellFactory(CheckBoxTableCell.forTableColumn(col3));
		
		tabNode.setPrefWidth(300);
		tabNode.getColumns().addAll(col1,col2,col3);
		
		final JFXComboBox<String> cmbToken = new JFXComboBox<String>();
		cmbToken.getItems().addAll(
			"X"  ,"Y"  ,"M"  ,"S"  ,"T"  ,"C"  ,
			"WX" ,"WY" ,"WM" ,"WS" ,"WT" ,"WC" ,
			"DWX","DWY","DWM","DWS","DWT","DWC",
			"RT" ,"RC" ,"R"  ,"D"  ,"F"
		);
		cmbToken.getSelectionModel().select(0);
		
		final JFXTextField boxAddr = new JFXTextField();
		boxAddr.setPrefWidth(70);
		boxAddr.setText("0");
		
		final Button btnAdd = PanBase.genButton2("新增節點",null);
		btnAdd.setOnAction(e->{
			String token = cmbToken.getSelectionModel().getSelectedItem();
			int addr = (int)(Integer.valueOf(boxAddr.getText()));
			String name = null;
			if(
				token.equalsIgnoreCase("R")==true || 
				token.equalsIgnoreCase("D")==true || 
				token.equalsIgnoreCase("F")==true 
			){
				name = String.format("%s%05d",token,addr);
			}else{
				name = String.format("%s%04d",token,addr);
			}			
			CompValue cv = new CompValue(name);
			tabNode.getItems().add(cv);
			//for next register or node~~~~
			addr++;
			boxAddr.setText(""+addr);
		});		
		final Button btnDel = PanBase.genButton2("刪除節點",null);
		btnDel.setOnAction(e->{
			tabNode.getSelectionModel().getSelectedItems().forEach(obj->{
				tabNode.getItems().remove(obj);
			});
		});
		
		final Button btnLoad = PanBase.genButton2("讀取節點",null);
		btnLoad.setOnAction(e->{
			int cnt = tabNode.getItems().size();
			String[] arg = new String[cnt];
			for(int i=0; i<cnt; i++){
				arg[i] = tabNode.getItems().get(i).getName();
			}
			int[] val = get(1,arg);
			if(val==null){
				Misc.loge("內部錯誤：無法讀取通訊埠");
				return;
			}
			for(int i=0; i<cnt; i++){
				CompValue com = tabNode.getItems().get(i);
				String name = com.getName();
				if(name.charAt(0)=='D'){
					com.setStatus(String.format("%08X", val[i]));
				}else if(
					name.charAt(0)=='W' ||
					name.charAt(0)=='R' ||
					name.charAt(0)=='D' ||
					name.charAt(0)=='F'
				){
					com.setStatus(String.format("%04X", val[i]));
				}else{
					com.setStatus(String.format("%02X", val[i]));
				}				
			}
		});
		final Button btnSave = PanBase.genButton2("更新節點",null);
		btnSave.setOnAction(e->{
		});

		final Button btnLoadEN = PanBase.genButton2("讀取致能",null);
		final Button btnSaveEN = PanBase.genButton2("更新致能",null);
		
		GridPane lay3 = new GridPane();
		lay3.getStyleClass().add("grid-medium");
		lay3.addRow(0, new Label("名稱"), cmbToken);
		lay3.addRow(1, new Label("位置"), boxAddr);
		lay3.add(btnAdd , 0, 2, 3, 1);
		lay3.add(btnDel , 0, 3, 3, 1);
		lay3.add(btnLoad, 0, 4, 3, 1);
		lay3.add(btnSave, 0, 5, 3, 1);
		lay3.add(btnLoadEN, 0, 6, 3, 1);
		lay3.add(btnSaveEN, 0, 7, 3, 1);
		
		lay1.getChildren().addAll(tabNode,lay3);
		return lay1;
	}

	private IntegerProperty[] iREG = null;//Device will map register to input analogy signal from address 'R3840'
	
	private Node layout_watch_rio(){
		
		final JBoxValInteger boxSize = new JBoxValInteger(10);
		final Button btnReset = new Button("重設");

		iREG = new IntegerProperty[boxSize.get()];
		for(int i=0; i<iREG.length; i++){
			iREG[i] = new SimpleIntegerProperty();
		}
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small-space");
		
		final HBox lay2 = new HBox();
		lay2.getStyleClass().add("hbox-medium-space");		
		lay2.getChildren().addAll(
			new Label("數目："),boxSize,
			new Label("格式："),btnReset
		);
		
		final GridPane lay3 = new GridPane();
		lay3.getStyleClass().add("grid-medium");
		
		btnReset.setOnAction(e->{
			lay3.getChildren().clear();
			for(int i=0, addr=3840; i<boxSize.get(); i++, addr++){
				Label txt = new Label("R"+addr);				
				lay3.add(txt, 0, i);
				txt = new Label();				
				txt.textProperty().bind(iREG[i].asString("%06d"));
				lay3.add(txt, 1, i);
			}
		});
		btnReset.getOnAction().handle(null);
		
		lay1.getChildren().addAll(lay2,lay3);		
		return lay1;
	}
	
	public static class RegValue {
		private final StringProperty  name = new SimpleStringProperty();
		private final IntegerProperty value= new SimpleIntegerProperty();
		
		public RegValue(String txt){
			name.set(txt);
		}
		
		public String getName() { return name.get(); }
		public void setName(String txt) { name.set(txt); }
		
		public int getValue() { return value.get(); }
		public void setValue(int txt) { value.set(txt); }
	};
	
	@Override
	protected Node eventLayout(PanBase pan) {
		
		VBox lay1 = new VBox();

		JFXTabPane lay2 = new JFXTabPane();
		
		Tab tab1 = new Tab();
		tab1.setText("系統資訊");
		tab1.setContent(layout_sys_state());
		
		Tab tab2 = new Tab();
		tab2.setText("節點監控");
		tab2.setContent(layout_watch_node());
		
		Tab tab3 = new Tab();
		tab3.setText("類比監控");
		tab3.setContent(layout_watch_rio());
		
		lay2.getTabs().addAll(tab1,tab2,tab3);
		
		Label txtLast = new Label();
		txtLast.setPrefWidth(200);
		txtLast.textProperty().bind(lastError);
		
		lay1.getChildren().addAll(lay2,txtLast);
		return lay1;
	}	
}
