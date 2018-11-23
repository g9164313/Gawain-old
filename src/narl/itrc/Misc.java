package narl.itrc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.sun.glass.ui.Application;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.stage.Window;
import javafx.util.Duration;

public class Misc {

	/**
	 * just show messages, it is like 'stdout'
	 * @param fmt - pass through 'printf()' 
	 * @param arg - pass through 'printf()'
	 */
	public static void logv(String fmt,Object... arg){
		System.out.print(String.format('\25'+fmt+"\n", arg));
	}
	/**
	 * just show messages, it is like 'stdout'
	 * @param fmt - pass through 'printf()' 
	 * @param arg - pass through 'printf()'
	 */
	public static void logw(String fmt,Object... arg){
		System.out.print(String.format('\26'+fmt+"\n", arg));
	}
	/**
	 * just show messages, it is like 'stderr'
	 * @param fmt - pass through 'printf()' 
	 * @param arg - pass through 'printf()'
	 */
	public static void loge(String fmt,Object... arg){
		System.out.print(String.format('\27'+fmt+"\n", arg));
	}

	public final static String TXT_UNKNOW = "？？？";
	
	public static native long realloc(long ptr,long len);
	public static native long free(long ptr);//always return '0'
	
	//Should we deprecate this function???
	/*public static Thread tskCheck(Thread tsk,Class<?> clazz,Object... parm){
		if(tsk!=null){
			while(tsk.isAlive()==true){
				delay(100);
				logv("wait task- %s",clazz.toString());
			}
		}
		try{
			int cnt = parm.length;
			if(cnt==0){					
				tsk = (Thread) clazz.newInstance();
			}else{
				Class<?>[] type = new Class<?>[cnt];
				for(int i=0; i<cnt; i++){
					type[i] = parm[i].getClass();
				}
				tsk = (Thread) clazz.getConstructor(type).newInstance(parm);
			}				
		} catch (
			InstantiationException | 
			IllegalAccessException | 
			IllegalArgumentException | 
			InvocationTargetException | 
			NoSuchMethodException | 
			SecurityException e
		) {
			e.printStackTrace();
		}
		tsk.start();
		return tsk;
	}*/
	//--------------------------//

	public static final KeyCombination shortcut_save = KeyCombination.keyCombination("Ctrl+S");
	public static final KeyCombination shortcut_load = KeyCombination.keyCombination("Ctrl+L");
	public static final KeyCombination shortcut_edit = KeyCombination.keyCombination("Ctrl+E");
	
	public static void selectTxt(ComboBox<String> box,String txt){
		ObservableList<String> lst = box.getItems();
		int cnt = lst.size();
		for(int i=0; i<cnt; i++){
			if(lst.get(i).equalsIgnoreCase(txt)==true){
				box.getSelectionModel().select(i);
				return;
			}
		}
		box.getItems().add(txt);
		box.getSelectionModel().select(cnt);
	}
	
	public static void delay(long millisec){
		try {
			TimeUnit.MILLISECONDS.sleep(millisec);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void delay_sec(long second){
		try {
			TimeUnit.SECONDS.sleep(second);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//----------------------------------------//

	private static int    chkSerialIndx = 0;
	private static String chkSerialName = "";
	
	public static String checkSerial(String fullName){
		if(chkSerialName.equalsIgnoreCase(fullName)==false){
			chkSerialName = fullName;
			chkSerialIndx = 0; //reset it~~~
		}
		int pos = fullName.lastIndexOf(File.separatorChar);
		if(pos<0){
			Misc.logw("[imwriteX] invalid path-->"+fullName);
			return "";
		}
		String name = fullName.substring(pos+1);
		String path = fullName.substring(0,pos);		
		File dir = new File(path); 
		if(dir.isDirectory()==false){
			logw("[imwriteX] invalid directory-->"+path);
			return fullName;
		}
		pos = name.lastIndexOf('.');
		if(pos<0){
			logw("[imwriteX] invalid name-->"+name);
			return fullName;
		}
		final String prex = name.substring(0,pos);
		final String appx = name.substring(pos);		
		do{
			fullName = String.format(
				"%s%c%s-%04d%s",
				path,
				File.separatorChar,
				prex,
				chkSerialIndx,
				appx
			);
			chkSerialIndx++;
			dir = new File(fullName);
		}while(dir.exists()==true);
		return fullName;
	}
	
	public static String modify_appx(String name,String appx){
		int pos = name.lastIndexOf('.');
		if(pos<0){
			return name;
		}
		return name.substring(0,pos)+"."+appx;
	}
	
	public static ImageView getResIcon(String name){
		return getResIcon("/narl/itrc/res/icon",name);
	}
	public static ImageView getResIcon(String pkg, String name){
		return new ImageView(getResImage(pkg, name));
	}
	public static Image getResImage(String name){
		return getResImage("/narl/itrc/res/icon",name);
	}
	public static Image getResImage(String pkg, String name){
		return new Image(Gawain.class.getResourceAsStream(pkg+"/"+name));
	}

	/*public static FileChooser genChooseImage(){
		final FileChooser.ExtensionFilter exts[] = {
			new FileChooser.ExtensionFilter("PNG","*.png"),
			new FileChooser.ExtensionFilter("TIF","*.tif","*.tiff"),
			new FileChooser.ExtensionFilter("PGM","*.pgm"),
			new FileChooser.ExtensionFilter("JPG","*.jpg","*.jpeg"),
			new FileChooser.ExtensionFilter("GIF","*.gif"),
			new FileChooser.ExtensionFilter("BMP","*.bmp"),
			new FileChooser.ExtensionFilter("All File","*.*")			
		};
		FileChooser chs = new FileChooser();
		chs.setTitle("開啟圖檔");
		chs.setInitialDirectory(dirSock);
		chs.getExtensionFilters().addAll(exts);
		return chs;
	}
	
	public static DirectoryChooser genChooseDir(){
		DirectoryChooser chs = new DirectoryChooser();
		chs.setTitle("選取資料夾");
		chs.setInitialDirectory(dirSock);
		return chs;
	}*/
	
	public static Window getParent(ActionEvent event){
		return ((Node)event.getTarget()).getScene().getWindow();
	}
	//----------------------------------------//

	public static String hex2txt(byte[] hex){
		final char[] sign = {
			'0','1','2','3',
			'4','5','6','7',
			'8','9','A','B',
			'C','D','E','F'
		};		
		String txt = "";
		for(int i=0; i<hex.length; i++){
			int h0 = (hex[i]&0xF0)>>4;
			int h1 = (hex[i]&0x0F);
			txt = txt + sign[h0] + sign[h1];
		}
		return txt;
	}
	
	public static byte[] txt2hex(String txt){
		int len = txt.length();
		byte[] hex = new byte[len/2];
		for(int i=0; i<len; i+=2){
			hex[i/2] = Integer.valueOf(txt.substring(i,i+2),16).byteValue();
		}
		return hex;
	}
	
	/**
	 * It is same as "Integer.valueOf()", but this function accepts leading zero.<p>
	 * @param txt - the string of integer value(including leading zero)
	 * @return integer value
	 */
	public static int txt2int(String txt){
		char[] cc = txt.toCharArray();
		int val = 0;
		for(int i=0; i<cc.length; i++){
			if(cc[i]<'0' || '9'<cc[i]){
				Misc.logw("fail to parse (%s)", txt);
				return 0;//drop this text, it is invalid number~~~~
			}
			val = val + (int)(cc[i] - '0') * ((int)Math.pow(10, cc.length-i-1));
		}
		return val;
	}
	//----------------------------------------//
		
	private static SimpleDateFormat fmtTime = new SimpleDateFormat ("hh:mm:ss");	
	public static String getTimeTxt(){
		return fmtTime.format(new Date(System.currentTimeMillis()));
	}
	
	private static SimpleDateFormat fmtDate = new SimpleDateFormat ("yyyy.MM.dd");
	public static String getDateTxt(){
		return fmtDate.format(new Date(System.currentTimeMillis()));
	}
	
	public static String[] trim2phy(String txt){
		final String[] res = {null,null};
		res[0] = "";
		res[1] = "";
		txt = txt.replaceAll("\\s","");//trim space!!!
		char[] list = txt.toCharArray();
		for(char cc:list){
			if( (('0'<=cc&&cc<='9')||cc=='.'||cc=='+'||cc=='-')==false){
				res[1] = res[1] + cc;
			}else{
				res[0] = res[0] + cc;
			}
		}
		return res;
	}
	
	private static final String REX_LOCA="[(]\\p{Digit}+[,]\\p{Digit}+[)]";
	private static final String REX_SIZE="\\p{Digit}+[x]\\p{Digit}+";
	/**
	 * Parse location information, the format is '({digi},{digi})'. <p>
	 * Example: '(70,5)' or '(100,050)' 
	 * @param txt
	 * @param pos 
	 * @return
	 */
	public static boolean trimLoca(String txt,int[] pos){
		//example: (200,300) or (10,5)...
		if(txt.matches(REX_LOCA)==false){
			return false;
		}
		txt = txt.replace('(',' ').replace(')',' ').trim();
		trim2Val(txt,pos,',',0);
		return true;
	}

	/**
	 * Parse rectangle information, the format is '({digi},{digi})@{digi}x{digi}'. <p>
	 * Example: '(70,5)@30x10' or '(100,050)@100x100' 
	 * @param txt
	 * @param pos
	 * @return
	 */
	public static boolean trimRect(String txt,int[] pos){
		//example: (200,300)@3x3 or (10,5)@100x100...
		if(txt.matches(REX_LOCA+"[@]"+REX_SIZE)==false){
			return false;
		}
		int idx = txt.indexOf("@");
		String pxt = txt.substring(0,idx);
		pxt = pxt.replace('(',' ').replace(')',' ').trim();
		trim2Val(pxt,pos,',',0);
		trim2Val(txt.substring(idx+1),pos,'x',2);		
		return true;
	}
	
	/**
	 * parse text which contains geometry information.
	 * @param txt - format {x}_{y}@{width}x{height}
	 * @param out - change text to integer
	 * @return
	 */
	public static boolean parseGeomInfo(String txt,int[] out){
		//String[] aa = txt.split("@");
		//if(aa[0].matches("\\d+[_]\\d+")==false){
		//	return false;
		//}
		String[] inf = txt.split("@");
		String[] loca = inf[0].split("_");
		String[] size = inf[1].split("x");
		out[0] = txt2int(loca[0]);
		out[1] = txt2int(loca[1]);
		out[2] = txt2int(size[0]);
		out[3] = txt2int(size[1]);
		return true;
	}
	
	/**
	 * parse text which contains geometry information.<p>
	 * It is convenient function....
	 * @param txt - format {x}_{y}@{width}x{height}
	 * @return integer array
	 */
	public static int[] parseGeomInfo(String txt){
		int[] res = new int[4];
		parseGeomInfo(txt,res);
		return res;
	}
	
	private static void trim2Val(String txt,int[] buf,char sp,int off){
		int idx = txt.indexOf(sp);
		buf[off+0] = txt2int(txt.substring(0,idx));
		buf[off+1] = txt2int(txt.substring(idx+1));
	}
	
	public static String trimPath(String txt){
		int pos = txt.lastIndexOf(File.separatorChar);
		if(pos<0){
			return txt;
		}
		return txt.substring(pos+1);
	}
	
	public static String trimName(String txt){
		int pos = txt.lastIndexOf(File.separatorChar);
		if(pos<0){
			return txt;
		}
		return txt.substring(0,pos);
	}
	
	public static String trimAppx(String txt){
		int pos = txt.lastIndexOf(".");
		if(pos<0){
			return txt;
		}
		return txt.substring(0,pos);
	}
	
	public static String trimPathAppx(String txt){
		int beg = txt.lastIndexOf(File.separatorChar)+1;
		if(beg<0){
			beg=0;
		}
		int end = txt.lastIndexOf(".");
		if(end<0){
			end = txt.length();
		}
		return txt.substring(beg,end);
	}	
	//--------------------------//
	
	/**
	 * execute a command. Remember this is 'blocking' function
	 * @param cmd - command line and arguments
	 * @return
	 */
	public static String exec(String... cmd){
		String txt = "";
		try {
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectOutput();
			pb.redirectError();
			pb.directory(Gawain.dirRoot);
			
			Process pc = pb.start();
			pc.waitFor();
			byte[] buf = new byte[2048];
			pc.getInputStream().read(buf);
			for(byte bb:buf){			
				if(bb==0){
					break;
				}
				txt = txt + (char)bb;
			}
			//try standard error stream~~~
			if(txt.length()==0){				
				pc.getErrorStream().read(buf);
				for(byte bb:buf){							
					if(bb==0){
						break;
					}
					txt = txt + (char)bb;
				}
			}
			pc.destroy();
		} catch (IOException e) {
			e.printStackTrace();
			txt = "[ERROR]: "+e.getMessage();
		} catch (InterruptedException e) {
			txt = "[ERROR]: "+e.getMessage();
		}		
		return txt;
	}
	
	/**
	 * It is just a macro or template function...
	 * @param tsk - lamda function
	 */
	public static void invoke(EventHandler<ActionEvent> tsk){
		if(Application.isEventThread()==true){
			tsk.handle(null);
		}else{
			final Runnable node = new Runnable(){
				@Override
				public void run() {
					tsk.handle(null);
				}
			};
			Application.invokeAndWait(node);
		}
	}
		
	/**
	 * It is just a macro or template function...
	 * First delay with few seconds, then task run~~~
	 * @param msec - delay milli-second
	 * @param tsk - lamda function
	 */
	public static void invoke(int msec, EventHandler<ActionEvent> tsk){
		if(Application.isEventThread()==true){
			final Timeline timer = new Timeline(new KeyFrame(
				Duration.millis(msec),
				eventAfter->{
					tsk.handle(null);
				}
			));			
			timer.setCycleCount(1);
			timer.play();			
		}else{
			final Runnable node = new Runnable(){
				@Override
				public void run() {
					tsk.handle(null);
				}
			};
			delay(msec);
			Application.invokeAndWait(node);
		}
	}
	//--------------------------//
	//I don't know how to set up category for below lines
	
	public static native byte[] screenshot2dib(int[] info);
	
	public static native byte[] screenshot2bmp(int[] info);
	
	public static native byte[] screenshot2png(int[] info);
	
	public static native void deleteScreenshot(byte[] data);
	
	public static native void sendMouseClick(int mx, int my);
	
	public static native void sendKeyboardText(String text);
	
	public static native void getCursorPos(int[] pos);
	
	public static native void imwrite(String name,byte[] data, int width, int height, int[] roi);
	
	public static double hypot(double[] pa,double[] pb){
		return Math.hypot(pa[0]-pb[0], pa[1]-pb[1]);
	}
	
	public static float hypot(int[] pa,int[] pb){
		return (float) Math.hypot(pa[0]-pb[0], pa[1]-pb[1]);
	}
	
	public static int hypotInt(int[] pa,int[] pb){
		return Math.round((float)Math.hypot(pa[0]-pb[0], pa[1]-pb[1]));
	}
	
	public static float[] int2float(int[] src){
		float[] dst = new float[src.length];
		for(int i=0; i<src.length; i++){
			dst[i] = Math.round(src[i]);
		}
		return dst;
	}
	
	public static Double[] int2double(Integer[] src){
		Double[] dst = new Double[src.length];
		for(int i=0; i<src.length; i++){
			if(src[i]==null){
				dst[i] = null;
				continue;
			}
			dst[i] = src[i].doubleValue();
		}
		return dst;
	}
		
	public static int[] double2int(double[] src){
		int[] dst = new int[src.length];
		for(int i=0; i<src.length; i++){
			dst[i] = (int)(Math.round(src[i]));
		}
		return dst;
	}

	public static long[] double2long(double[] src){
		long[] dst = new long[src.length];
		for(int i=0; i<src.length; i++){
			dst[i] = Math.round(src[i]);
		}
		return dst;
	}
}



