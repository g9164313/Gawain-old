package narl.itrc;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class Misc {

	/**
	 * It is same as logger function, but no line feed-back
	 * @param fmt - pass through 'printf()' 
	 * @param arg - pass through 'printf()'
	 */
	public static void printf(String fmt,Object... arg){
		BoxLogger.printAll(fmt, arg);
	}
	
	/**
	 * just show messages, like 'stdout'
	 * @param fmt - pass through 'printf()' 
	 * @param arg - pass through 'printf()'
	 */
	public static void logv(String fmt,Object... arg){
		String txt = log_txt("[VERBOSE] ",fmt,arg);
		System.out.print(txt);
		BoxLogger.printAll(txt);
	}

	/**
	 * just show messages, like 'stdout'
	 * @param fmt - pass through 'printf()' 
	 * @param arg - pass through 'printf()'
	 */
	public static void logw(String fmt,Object... arg){
		String txt = log_txt("[WARN   ] ",fmt,arg);
		System.out.print(txt);
		BoxLogger.printAll(txt);
	}
	
	/**
	 * just show messages, like 'stderr'
	 * @param fmt - pass through 'printf()' 
	 * @param arg - pass through 'printf()'
	 */
	public static void loge(String fmt,Object... arg){
		String txt = log_txt("[ERROR  ] ",fmt,arg);
		System.err.print(txt);
		BoxLogger.printAll(txt);
	}
	
	private static String log_txt(String pfx,String fmt,Object... arg){
		return pfx+String.format(fmt+"\n", arg);
	}
	
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
		long t1 = System.currentTimeMillis();
		long t2 = System.currentTimeMillis();
		while((t2-t1)<millisec){
			t2 = System.currentTimeMillis();
		}
	}
	//----------------------------------------//
	
	private static final double l0_254 = 1./25.4;
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
	
	public static final String pathRoot = check_root();
	
	public static final String pathTemp = check_temp();
	
	public static final String fileJar = check_jar();
	
	/**
	 * The path where we start.
	 */
	public static final File fsPathRoot = new File(pathRoot);
	
	/**
	 * The path where we keep files or data.
	 */
	public static final File fsPathTemp = new File(pathTemp);
	
	/**
	 * Where the jar file is, if no, this will be null
	 */
	public static final File fsFileJar = (fileJar==null)?(null):(new File(fileJar));
	
	private static String check_arch(){
		String name = System.getProperty("os.arch");
		if(name.indexOf("64")>0){
			return "x64";
		}
		return name;
	}
	
	public static boolean isPOSIX(){
		String name = System.getProperty("os.name").toLowerCase();
		if(name.indexOf("linux")>=0){
			return true;
		}
		return false;
	}
	
	public static boolean isFxxkMicrosoft(){
		String name = System.getProperty("os.name").toLowerCase();
		if(name.indexOf("win")>=0){
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
	
	private static String check_temp(){
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
	
	public static ImageView getIcon(String iconName){
		return new ImageView(getImage(iconName));
	}
	
	public static Image getImage(String name){
		return new Image(Gawain.class.getResourceAsStream("/narl/itrc/res/icon/"+name));
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
		chs.setInitialDirectory(fsPathTemp);
		chs.getExtensionFilters().addAll(exts);
		return chs;
	}
	
	public static DirectoryChooser genChooseDir(){
		DirectoryChooser chs = new DirectoryChooser();
		chs.setTitle("選取資料夾");
		chs.setInitialDirectory(fsPathTemp);
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
	
	private static final String REX_POSITION="[(]\\p{Digit}+[,]\\p{Digit}+[)]";
	private static final String REX_SIZE="\\p{Digit}+[x]\\p{Digit}+";
	
	public static boolean trimPosition(String txt,int[] pos){
		//example: (200,300) or (10,5)...
		if(txt.matches(REX_POSITION)==false){
			return false;
		}
		txt = txt.replace('(',' ').replace(')',' ').trim();
		trim2Val(txt,pos,',',0);
		return true;
	}

	public static boolean trimRectangle(String txt,int[] pos){
		//example: (200,300)@3x3 or (10,5)@100x100...
		if(txt.matches(REX_POSITION+"[@]"+REX_SIZE)==false){
			return false;
		}
		int idx = txt.indexOf("@");
		String pxt = txt.substring(0,idx);
		pxt = pxt.replace('(',' ').replace(')',' ').trim();
		trim2Val(pxt,pos,',',0);
		trim2Val(txt.substring(idx+1),pos,'x',2);		
		return true;
	}

	private static void trim2Val(String txt,int[] buf,char sp,int off){
		int idx = txt.indexOf(sp);
		buf[off+0] = Integer.valueOf(txt.substring(0,idx));
		buf[off+1] = Integer.valueOf(txt.substring(idx+1));
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
	
	private static Process procIJ = null;
	
	public static void execIJ(String img_name){
		if(procIJ!=null){
			//just only execute one instance~~~
			if(procIJ.isAlive()==true){
				procIJ.destroy();
				procIJ = null;//for next turn~~~~
				return;
			}			
		}
		String ij_path = Gawain.prop.getProperty("IJ_PATH","");		
		if(ij_path.length()==0){
			PanBase.notifyError(
				"內部資訊",
				"請在 conf.properties 設定 ImageJ 執行路徑"
			);
			return;
		}
		File fs = new File(ij_path);
		try {			
			//How to find 'java' from M$ Windows OS ? 
			ProcessBuilder pb = null;
			if(fs.isDirectory()==true){
				//execute ImageJ from jar file
				pb = new ProcessBuilder(
					"/usr/bin/java","-Xmx1024m","-jar",
					ij_path, img_name
				);
			}else{
				//it is a executed file
				pb = new ProcessBuilder(ij_path,img_name);
			}
			pb.directory(fsPathTemp);
			procIJ = pb.start();
		} catch (Exception e) {
			PanBase.notifyError(
				"內部資訊",
				e.getMessage()
			);
		}
	} 
	//--------------------------//

	//Do we need this ???
	public static native void namedWindow(String name);
	
	public static native void renderWindow(String name,long ptr);
	
	public static native void destroyWindow(String name);
	
	public static native void imWrite(String name,long ptr);//Should we move this to 'CamBundle' ???
	
	public static native void imWriteRoi(String name,long ptr,int[] roi);
	
	public static String imWriteX(String fullName,long ptr){		
		fullName = checkSerial(fullName);
		imWriteX(fullName,ptr,null);
		return fullName;
	}
	
	public static String imWriteX(String fullName,long ptr,int[] roi){		
		fullName = checkSerial(fullName);
		if(roi==null){
			imWrite(fullName,ptr);
		}else{
			imWriteRoi(fullName,ptr,roi);
		}
		return fullName;
	}

	public static native long imRead(String name,int flags);
	
	public static native void imRelease(long ptr);//just release Mat pointer

	public static native long imCreate(int width,int height,int type);//just release Mat pointer
	
	//--------------------------//
	//I don't know how to set up category for below lines
	
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
	
	public static Double[] Int2Double(Integer[] src){
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



