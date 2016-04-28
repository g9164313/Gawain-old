package narl.itrc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Misc {

	public static void logv(String fmt,Object... arg){
		System.out.printf("[VERBOSE] "+fmt+"\n", arg);
	}

	public static void logw(String fmt,Object... arg){
		System.out.printf("[WARN   ] "+fmt+"\n", arg);
	}
	
	public static void loge(String fmt,Object... arg){
		System.err.printf("[ERROR  ] "+fmt+"\n", arg);
	}
		
	public static Thread tskCheck(Thread tsk,Class<?> clazz,Object... parm){
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
	}
	
	public static void tskDelay(long tick){
		try {
			Thread.sleep(tick);
		} catch (InterruptedException e) {			
			logw(
				"fail to sleep --> %s",
				Thread.currentThread().getName()
			);
			delay(tick);
		}
	}
	
	public static void delay(long millisec){
		long t1 = System.currentTimeMillis();
		long t2 = System.currentTimeMillis();
		while((t2-t1)<millisec){
			t2 = System.currentTimeMillis();
		}
	}
		
	private static boolean _verify_digit(String txt,String cc){
		char[] chars = new char[txt.length()];
		txt.getChars(0, chars.length, chars, 0);
		for (int i = 0; i < chars.length; i++) {
			if('0'<=chars[i] && chars[i]<='9'){
				continue;
			}			
			if(cc.indexOf(chars[i])>=0){
				continue;
			}
			return false;
		}
		return true;
	}
	//----------------------------------------//
	
	public static final String unitMm= "mm";
	public static final String unitCm= "cm";
	public static final String unitInch= "inch";
	public static final String unitM = "m";
	private static final String[] unitLength ={
		unitMm,
		unitCm,
		unitInch,
		unitM
	};
	private static final double IM=25.4;
	private static final double MI=1./25.4;
	private static final double[][] ratioLength = {
		//destination ||  source-->mm,cm,inch,m
		{  1.,    10.,      IM,    1e3},
		{ 0.1,     1.,   IM*10,    1e2},
		{MI  , MI*10.,      1., MI*1e3},
		{1e-3,   1e-2, IM*1e-3,     1.},
	};
	
	public static final String unitSec= "sec";
	public static final String unitMin= "min";
	public static final String unitHr = "hr";
	private static final String[] unitTime ={
		unitSec,
		unitMin,
		unitHr
	};	
	private static final double[][] ratioTime = {
		//destination ||  source-->second,minute,hour
		{1.      ,60.   ,3600.   },
		{1./60.  ,1.    ,60.     },
		{1./3600.,1./60 ,1.      },
	};
	
	private static String[][] unitAll = {
		unitTime,unitLength
	};//the order is important~~~
	
	private static char[] scaleSig = {
		'p','μ','u','m','k','M','G'
	};//the order is important~~~
	private static double[] scaleVal = {
		1e-9,1e-6,1e-6,1e-3,1e3,1e6,1e9
	};//the order is important~~~
	
	private static double findRatio(String u1,String u2){
		if(u1.equalsIgnoreCase(u2)==true){
			return 1.;
		}
		int i=0,idx1=0,idx2=0;
		double[][] tbl = null;
		for(i=0; i<unitAll.length; i++){
			idx1 = match_unit(unitAll[i],u1);
			if(idx1<0){
				continue;
			}
			idx2 = match_unit(unitAll[i],u2);
			if(idx2<0){
				return 1.;//WTF???
			}
			break;
		}		
		switch(i){
		case 0: tbl=ratioTime  ; break;
		case 1: tbl=ratioLength; break;
		default:
			return 1.;
		}
		return tbl[idx2][idx1];
	}
	
	private static double findScale(String u1,String u2){
		//'scale' is always positive!!!!
		char cc;
		double s1=1., s2=1.;
		cc = u1.charAt(0);
		for(int i=0; i<scaleSig.length; i++){
			if(cc==scaleSig[i]){
				s1 = scaleVal[i];
				break;
			}			
		}
		cc = u2.charAt(0);
		for(int i=0; i<scaleSig.length; i++){
			if(cc==scaleSig[i]){
				s2 = scaleVal[i];
				break;
			}			
		}
		return s1/s2;
	}
	
	public static double convert(
		double srcVal,
		String srcUnit,
		String dstUnit
	){
		srcUnit = srcUnit.trim();//for safety~~~
		dstUnit = dstUnit.trim();
		if(srcUnit.equals(dstUnit)==true){
			return srcVal;//we don't need convert value~~~~
		}
		double ratio = findRatio(srcUnit,dstUnit);
		double scale = findScale(srcUnit,dstUnit);
		if(ratio<0.){
			return srcVal * scale;//ratio between units must be positive!!!
		}
		return srcVal * ratio * scale;// source * ratio = destination
	}
	
	public static double convertUnit(
		String srcValUnit,
		String dstUnit
	){
		double srcVal=1.;
		try{
			String[] srcTxt = split(srcValUnit);
			srcVal = Double.valueOf(srcTxt[0]); 
			
			dstUnit = dstUnit.trim();
			
			srcVal = convert(srcVal,srcTxt[1],dstUnit);
			
		}catch(NumberFormatException e){
			return 1.;
		}
		return srcVal;
	}
	
	public static double convertRatio(
		String srcValUnit,
		String dstValUnit
	){
		double srcVal=1.,dstVal=1.;
		try{
			String[] srcTxt = split(srcValUnit);
			srcVal = Double.valueOf(srcTxt[0]); 
		
			String[] dstTxt = split(dstValUnit);
			dstVal = Double.valueOf(dstTxt[0]);
			
			srcVal = convert(srcVal,srcTxt[1],dstTxt[1]);
			
		}catch(NumberFormatException e){
			return 1.;
		}
		return srcVal/dstVal;
	}
	
	private static String[] split(String txt){
		txt = txt.replaceAll("\\s+", "");
		String[] seg = {"",""};
		char[] cc = txt.toCharArray();
		int i = 0;
		while(('0'<=cc[i] && cc[i]<='9') || cc[i]=='.'){
			seg[0] = seg[0] + cc[i];
			i++;
		}
		while(i<txt.length()){
			seg[1] = seg[1] + cc[i];
			i++;
		}
		return seg;
	}
	
	private static int match_unit(String[] lst,String u1){
		for(int i=0; i<lst.length; i++){
			if(lst[i].equalsIgnoreCase(u1)==true){
				return i;
			}
		}
		return -1;
	}
	
	public static String[] match_unit(String un,String scale){
		un = un.trim();
		for(int i=0;i<scaleSig.length; i++){
			if(un.charAt(0)==scaleSig[i]){
				un = un.substring(1);
				break;
			}			
		}		
		for(int j=0; j<unitAll.length; j++){
			for(int i=0; i<unitAll[j].length; i++){
				if(unitAll[j][i].equalsIgnoreCase(un)==true){
					return unitAll[j];
				}
			}			
		}
		String[] tmp;
		int cnt = scale.length();
		if(cnt==0){
			tmp = new String[1];
			tmp[0] = un;
		}else{
			tmp = new String[cnt];
			for(int i=0; i<cnt; i++){
				tmp[i] = ""+scale.charAt(i)+un; 
			}
		}		
		return tmp;
	}
	
	public static double matchDenom(String src,String dst){		
		String[] itm = src.split("/");
		if(itm.length!=2){
			Misc.logw("invalid unit=%s",src);
			return 1.;
		}
		src = itm[1].trim();		
		itm = dst.split("/");
		if(itm.length!=2){
			Misc.logw("invalid unit=%s",dst);
			return 1.;
		}
		dst = itm[1].trim();
		return findRatio(src,dst);
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
	
	public static final String arch = check_arch();
	
	public static final String os = check_os();
	
	public static final String pathRoot = check_root();
	
	public static final String pathTemp = check_tmp();
	
	public static final String fileJar = check_jar();
	
	private static String check_arch(){
		String name = System.getProperty("os.arch");
		if(name.indexOf("64")>0){
			return "x64";
		}
		return name;
	}
	
	private static String check_os(){
		String name = System.getProperty("os.name").toLowerCase();
		if(name.indexOf("win")>=0){
			return "win";
		}else if(name.indexOf("linux")>=0){
			return "linux";
		}
		return "unknow";
	}
	
	private static String check_root(){
		String url = "."+File.separator;		
		try {
			url = Misc.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return url;
		}
		url = url.replace('/',File.separatorChar);
		Misc.logv("URL==>%s",url);
		final String pre1 = "jar:file:";
		final String pre3 = "file:";				
		if(url.startsWith(pre1)==true){
			url = url.substring(pre1.length());
			int pos = url.lastIndexOf(File.separatorChar);
			if(pos>0){
				url = url.substring(0,pos);
			}
		}else if(url.startsWith(pre3)==true){
			url = url.substring(pre3.length());
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
		Misc.logv("ROOT==>%s",url);
		return url;
	}
	
	private static String check_tmp(){
		String url = Misc.pathRoot+"gawain"+File.separator;
		File dir = new File(url);
		if(dir.exists()==false){
			if(dir.mkdirs()==false){
				Misc.loge("we can't create a temp-->%s!!",url);
				System.exit(-2);
			}
		}
		return url;
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
				prex,chkSerialIndx,appx
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
	
	public static ImageView getIcon(String iconName){
		return new ImageView(
			new Image(Gawain.class.getResourceAsStream("/narl/itrc/res/"+iconName))
		);
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
		
	public static int[] splitInteger(String txt){
		ArrayList<String> buff = new ArrayList<String>();
		boolean ending=true;
		String digi="";
		char[] cc = txt.toCharArray();
		for(int i=0; i<cc.length; i++){			
			if('0'<=cc[i]&&cc[i]<='9'){
				digi = digi + cc[i];
				ending = false;
				continue;
			}
			if(ending==false){
				buff.add(digi);
				digi = "";//reset it~~~~
			}
			ending = true;
		}
		if(digi.length()!=0){
			buff.add(digi);//final one~~~
		}
		int[] res = new int[buff.size()];
		for(int i=0; i<buff.size(); i++){
			try{
				res[i] = Integer.valueOf(buff.get(i));
			}catch(NumberFormatException e){
				res[i] = 0;
			}
		}
		return res;
	}
	//----------------------------------------//
	
	private static long tick1, tick2;//the unit is millisecond~~
	public static void setTick1(){
		tick1 = System.currentTimeMillis();
	}
	public static void setTick2(){
		tick2 = System.currentTimeMillis();
	}
	public static String getTickTxt(){
		long ms = tick2 - tick1;
		if(ms<0L){
			return "????";
		}
		long ss = ms / 1000L;
		if(ss!=0){
			ms = ms % 1000L;
		}
		long mm = ss / 60L;
		if(mm!=0){
			ms = ms % 60L;
		}
		long hh = mm / 60L;
		if(hh!=0L){
			mm = mm % 60L;
		}
		return String.format("%d:%02d:%02d.%03d",hh,mm,ss,ms);
	}
	
	private static SimpleDateFormat fmtTime = new SimpleDateFormat ("hh:mm:ss");	
	public static String getTimeTxt(){
		return fmtTime.format(new Date(System.currentTimeMillis()));
	}
	
	private static SimpleDateFormat fmtDate = new SimpleDateFormat ("yyyy.MM.dd");
	public static String getDateTxt(){
		return fmtDate.format(new Date(System.currentTimeMillis()));
	}
	
	public static String trimPath(String txt){
		int pos = txt.lastIndexOf(File.separatorChar);
		if(pos<0){
			return txt;
		}
		return txt.substring(pos+1);
	}
	//--------------------------//
	
	public static String syncExec(String... command){
		String txt = "";
		try {
			ProcessBuilder pb = new ProcessBuilder(command);
			pb.redirectOutput();
			pb.redirectError();
			Process p = pb.start();
			p.waitFor();
			byte[] buf = new byte[2048];
			p.getInputStream().read(buf);
			for(byte bb:buf){			
				if(bb==0){
					break;
				}
				txt = txt + (char)bb;
			}
			//try standard error stream~~~
			if(txt.length()==0){				
				p.getErrorStream().read(buf);
				for(byte bb:buf){							
					if(bb==0){
						break;
					}
					txt = txt + (char)bb;
				}
			}
			p.destroy();
		} catch (IOException e) {
			txt = "[ERROR]: "+e.getMessage();
		} catch (InterruptedException e) {
			txt = "[ERROR]: "+e.getMessage();
		}		
		return txt;
	}
	//--------------------------//

	public static native void namedWindow(String name);//this will invoke OpenCV!!
	
	public static native void renderWindow(String name,long ptr);//this will invoke OpenCV!!
	
	public static native void destroyWindow(String name);//this will invoke OpenCV!!
	
	public static native void imwrite(String name,long ptr);//this will invoke OpenCV!!
	
	public static String imwriteX(String fullName,long ptr){		
		fullName = checkSerial(fullName);
		imwrite(fullName,ptr);
		return fullName;
	}
	
	public static native void imread(String name,long ptr);//this will invoke OpenCV!!
}



