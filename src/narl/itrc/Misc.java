package narl.itrc;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

public class Misc {

	private static String log_msg(String pfx,String fmt,Object... arg){
		long tick = System.currentTimeMillis();
		return String.format("[%tH:%tM:%tS.%tL]%s",tick,tick,tick,tick,pfx)+String.format(fmt+"\n", arg);
	}
	
	/**
	 * just show messages, it is like 'stdout'
	 * @param fmt - pass through 'printf()' 
	 * @param arg - pass through 'printf()'
	 */
	public static void logv(String fmt,Object... arg){
		String txt = log_msg("[VERBOSE] ",fmt,arg);
		System.out.print(txt);
	}

	/**
	 * just show messages, it is like 'stdout'
	 * @param fmt - pass through 'printf()' 
	 * @param arg - pass through 'printf()'
	 */
	public static void logw(String fmt,Object... arg){
		String txt = log_msg("[WARN   ] ",fmt,arg);
		System.out.print(txt);
	}
	
	/**
	 * just show messages, it is like 'stderr'
	 * @param fmt - pass through 'printf()' 
	 * @param arg - pass through 'printf()'
	 */
	public static void loge(String fmt,Object... arg){
		String txt = log_msg("[ERROR  ] ",fmt,arg);
		System.err.print(txt);
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
		long t2, t1 = System.currentTimeMillis();
		do{
			t2 = System.currentTimeMillis();
		}while((t2-t1)<millisec);
	}
	//----------------------------------------//
	
	//private static final double l0_254 = 1./25.4;
	private static final double l1_254 = 10./25.4;
	private static final double l3_254 = 1000./25.4;
	private static final double l4_254 = 1e4/25.4;
	private static final double l6_254 = 1e6/25.4;
	private static final double D5_254 = 2.54e-5;
	
	public static final String[] unitLength ={
		"mil","cm","inch","m"
	};
	private static final double[][] ratioLength = {
		//destination ||  source --> mil, cm, inch, m
		{  1.0000 , l4_254, 1000.0, l6_254 },
		{  0.00254, 1.0000, 2.5400, 100.00 },
		{  0.00100, l1_254, 1.0000, l3_254 },
		{  D5_254 , 0.0100, 0.0254, 1.0000 },
	};
	
	
	private static final double BASE_60_1 = 60.;
	private static final double BASE_1_60 = 1./60.;
	private static final double BASE_36_1 = 3600.;
	private static final double BASE_1_36 = 1./3600.;
	
	public static final String[] unitTime ={
		"s","sec","min","hr"
	};	
	private static final double[][] ratioTime = {
		//destination ||  source --> second,minute,hour
		{1.       ,1.       ,BASE_60_1,BASE_36_1},
		{1.       ,1.       ,BASE_60_1,BASE_36_1},
		{BASE_60_1,BASE_1_60,1        ,BASE_60_1},
		{BASE_1_36,BASE_1_36,BASE_60_1,1.       },
	};
	
	public static final String[][] unitAll = {
		unitLength,unitTime
	};//the order is important~~~
	
	public static final char[] scaleSig = {
		'p','μ','u','m','k','M','G'
	};//the order is important~~~
	private static final double[] scaleVal = {
		1e-9,1e-6,1e-6,1e-3,1e3,1e6,1e9
	};//the order is important~~~
	
	private static double findScale(String unit){
		if(unit.length()==1){
			return 1.;//it must be no scale signature
		}
		if(unit.equalsIgnoreCase("mil")==true){
			return 1.;//skip this unit, it shouldn't append scale signature
		}
		char ss = unit.charAt(0);
		for(int i=0; i<scaleSig.length; i++){
			if(scaleSig[i]==ss){
				return scaleVal[i];
			}
		}
		return 1.;
	}
	
	private static double findRatio(String srcUnit,String dstUnit){
		if(srcUnit.equalsIgnoreCase(dstUnit)==true){
			return 1.;
		}
		for(int k=0; k<unitAll.length; k++){
			String[] unit = unitAll[k];
			for(int i=0; i<unit.length; i++){
				if(unit[i].equalsIgnoreCase(srcUnit)==true){
					for(int j=0; j<unit.length; j++){
						if(unit[j].equalsIgnoreCase(dstUnit)==true){
							switch(k){
							case 0:
								return ratioLength[j][i];
							case 1:
								return ratioTime[j][i];
							}
						}
					}
				}
			}
		}
		return 0.;//??? what is going on ???
	}

	public static String[] phySplit(String txt){
		txt = txt.replaceAll("\\s+", "");		
		int i = 0;
		for(i=0; i<txt.length(); i++){
			char cc = txt.charAt(i);
			if( !(('0'<=cc && cc<='9') || cc=='.') ){
				break;
			}
		}
		String[] arg = new String[2];
		arg[0] = txt.substring(0, i);
		arg[1] = txt.substring(i);
		return arg;
	}
	
	/**
	 * check the statement is a valid physical value.<p>
	 * A valid physical is composed of numeral and SI unit.
	 * @param txt - text statement
	 * @return true - a valid physical value.<p>
	 *         false- no numeral or invalid SI unit
	 */
	public static boolean isValidPhy(String txt){		
		String[] arg = phySplit(txt);
		//check numeral
		if(arg[0].length()==0){
			return false;
		}
		//check SI unit
		for(int j=0; j<unitAll.length; j++){
			for(int i=0; i<unitAll[j].length; i++){
				if(arg[1].contains(unitAll[j][i])==true){
					return true;
				}
			}
		}
		return false;
	}
	
	public static double phyConvert(
		double srcVal,
		String srcUnit,
		String dstUnit
	) throws NumberFormatException {
		srcUnit = srcUnit.trim();//for safety~~~
		dstUnit = dstUnit.trim();
		if(srcUnit.equals(dstUnit)==true){
			return srcVal;//we don't need convert value~~~~
		}
		double srcScale = findScale(srcUnit);
		if(srcScale!=1.){
			srcUnit = srcUnit.substring(1);
		}
		double dstScale = findScale(dstUnit);
		if(dstScale!=1.){
			dstUnit = dstUnit.substring(1);
		}
		double scale = (srcScale/dstScale);		
		double ratio = findRatio(srcUnit,dstUnit); 
		return srcVal*scale*ratio;
	}
	
	public static double phyConvert(
		String srcValUnit,
		String dstUnit
	) throws NumberFormatException {
		
		double srcVal=1.;
		
		String[] srcTxt = phySplit(srcValUnit);
		srcVal = Double.valueOf(srcTxt[0]);
		
		return phyConvert(srcVal,srcTxt[1],dstUnit);
	}
	
	public static double phyConvertRatio(
		String srcValUnit,
		String dstValUnit
	) throws NumberFormatException {
		
		double srcVal=1.,dstVal=1.;
		
		String[] srcTxt = phySplit(srcValUnit);
		srcVal = Double.valueOf(srcTxt[0]); 

		String[] dstTxt = phySplit(dstValUnit);
		dstVal = Double.valueOf(dstTxt[0]);
			
		srcVal = phyConvert(srcVal,srcTxt[1],dstTxt[1]);
		
		return srcVal/dstVal;
	}
	
	public static String num2scale(double num){
		String txt = String.format("%G",num);
		int pos = txt.indexOf('E');
		if(pos<0){
			return txt;
		}
		double sss = Math.pow(10.,Integer.valueOf(txt.substring(pos+1)));
		int idx = 0;
		for(idx=0; idx<scaleVal.length; idx++){
			if(scaleVal[idx]==sss){
				break;
			}			
		}
		if(idx>=scaleVal.length){
			return txt;
		}
		return txt.substring(0,pos)+scaleSig[idx];
	}	
	
	public static String num2prefix(double num){
		return num2prefix(num,0);
	}
	
	public static String num2prefix(double num,int pow){
		
		String[] txt = String.format("%E",num).split("E");
		
		double val = Double.valueOf(txt[0]);
		
		int prx = Integer.valueOf(txt[1]);
		
		int cnt = Math.abs(prx);
		
		if(cnt==0){			
			return String.format("%."+pow+"f",val);//special case~~~
		}

		final char[] p_prefix = {' ','k','M','G','T','P','E'};	
		final char[] n_prefix = {' ','m','μ','n','p','f','a'};
		char mtrx = ' ';
		if(prx>=0){
			mtrx = p_prefix[cnt/3];
			val = val * Math.pow(10.,(cnt%3));
		}else{
			int scale = 1+(cnt-1)/3;
			mtrx = n_prefix[scale];
			val = num * Math.pow(10.,scale*3);
		}
		if(mtrx!=' '){
			return String.format("%."+pow+"f%c",val,mtrx);
		}
		return String.format("%."+pow+"f",val);
	}
	
	/**
	 * 
	 * @param ms - millsecond
	 * @return
	 */
	public static String num2time(long ms){		
		long ss = ms / 1000L;
		if(ss!=0){
			ms = ms % 1000L;
		}
		long mm = ss / 60L;
		if(mm!=0){
			ss = ss % 60L;
		}
		long hh = mm / 60L;
		if(hh!=0L){
			mm = mm % 60L;
		}
		if(hh!=0){
			return String.format("%d:%d:%d.%d",hh,mm,ss,ms/100L);
		}else if(mm!=0){
			return String.format("%d:%d.%d",mm,ss,ms/100L);
		}else if(ss!=0){
			return String.format("%d.%dsec",ss,ms/10L);
		}
		return String.format("%dms",ms);
	}
	
	public static String insertAdjunct(String txt,char tkn,String adj){
		int pos = txt.lastIndexOf(tkn);
		if(pos<0){
			return txt+adj;
		}
		if(tkn==File.separatorChar){
			txt = txt.substring(0,pos+1)+adj+txt.substring(pos+1);
		}else{
			txt = txt.substring(0,pos)+adj+txt.substring(pos);
		}
		return txt;
	}	
	//----------------------------------------//
	
	public static final String pathRoot= check_root();
		
	public static final String pathSock= check_path(true);
	
	public static final String pathHome= check_path(false);
	
	public static final String fileJar = check_jar();
	
	/**
	 * The path where we start.
	 */
	public static final File dirRoot = new File(pathRoot);
	
	/**
	 * The path where we keep files or data.
	 */
	public static final File dirSock = new File(pathSock);
	
	public static final File dirHome = new File(pathHome);
	
	private static String check_root(){
		
		String url = "."+File.separator;		
		try {
			url = Misc.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return url;
		}
		url = url.replace('/',File.separatorChar);
		
		//System.out.printf("URL==>%s\n",url);//just for debug~~~
		final String pre1 = "jar:file:";
		final String pre3 = "file:";				
		if(url.startsWith(pre1)==true){
			url = url.substring(pre1.length());
			int pos = url.lastIndexOf(File.separatorChar);
			if(pos>0){
				url = url.substring(0,pos);
			}
		}else if(url.startsWith(pre3)==true){
			url = url.substring(pre3.length());//EX: "file:/xxx/xxx/xxx.jar"
			final String post1 = "bin"+File.separatorChar;
			final String post2 = ".jar";
			int len;
			if(url.endsWith(post1)==true){
				//trick!!!!
				len = url.length()-post1.length();
				url = url.substring(0,len);
			}else if(url.endsWith(post2)==true){
				len = url.lastIndexOf(File.separator);
				url = url.substring(0,len+1);
			}
		}else{
			url = new File(".").getAbsolutePath();
			url = url.substring(0,url.length()-1);
		}
		//System.out.printf("ROOT==>%s",url);//just for debug~~~
		return url;
	}
	
	private static String check_path(boolean isSock){		
		String path = null;
		String os_name = System.getProperty("os.name").toLowerCase();
		if(os_name.contains("win")==true){
			if(isSock==true){
				//Any other system variable name??
				path = System.getenv("HOMEPATH");
				if(path==null){
					Misc.loge("fail to get $HOME");
				}else{
					path = "C:"+path+File.separatorChar+".gawain"+File.separatorChar;
				}				
			}else{
				path = System.getenv("TEMP")+File.separatorChar;
			}
		}else{
			path = System.getenv("HOME");
			if(path==null){
				Misc.loge("fail to get $HOME");
				if(isSock==true){
					path = pathRoot+File.separatorChar+".gawain"+File.separatorChar;
				}else{
					return "."+File.separatorChar;
				}
			}else{
				if(isSock==true){
					path = path+File.separatorChar+".gawain"+File.separatorChar;
				}else{
					return path+File.separatorChar;
				}
			}
		}
		File dir = new File(path);
		if(dir.exists()==false){
			if(dir.mkdirs()==false){
				System.err.printf("we can't create a temp-->%s!!\n",path);
				System.exit(-2);
			}
		}
		return path;
	}
	
	private static String check_jar(){
		
		File[] lst = new File(".").listFiles();
		for(File fs:lst){
			if(fs.getName().indexOf(".jar")<=0){
				continue;
			}
			try {
				@SuppressWarnings("resource")
				final JarFile jj = new JarFile(fs);
				final Enumeration<JarEntry> lstEE = jj.entries();
				while(lstEE.hasMoreElements()==true){
					JarEntry ee = lstEE.nextElement();
					if(ee.getName().indexOf(Gawain.propName)>=0){
						return jj.getName();
					}
				}				
			} catch (IOException e) {
				Misc.logv("can't open %s",fs.getName());
				continue;
			}
		}
		return null;
	}

	public static String checkSeparator(String txt){
		int len = txt.length();
		if(txt.charAt(len-1)==File.separatorChar){
			return txt;
		}
		return txt + File.separatorChar;
	}
	
	public static boolean isFileExist(String path){
		return new File(path).isFile();
	}
	
	public static boolean isPOSIX(){
		String name = System.getProperty("os.name").toLowerCase();
		if(name.indexOf("linux")>=0){
			return true;
		}
		return false;
	}
	
	public static String getOSName(){
		String name = System.getProperty("os.name").toLowerCase();
		if(name.indexOf("win")>=0){
			return "win";
		}else if(name.indexOf("linux")>=0){
			return "linux";
		}
		return "unknow";
	}
	//------------------------------------------------//
	
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
	
	public static ImageView getIcon(String name){
		return new ImageView(getResImage("/narl/itrc/res/icon",name));
	}
	
	public static Image getResImage(String pkg,String name){
		return new Image(Gawain.class.getResourceAsStream(pkg+"/"+name));
	}
	
	public static Image getFileImage(String name){
		File fs = new File(name);
		if(fs.isFile()==false){
			return null;
		}		
		return new Image(fs.toURI().toString());
	}

	public static FileChooser genChooseImage(){
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
	}
	
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



