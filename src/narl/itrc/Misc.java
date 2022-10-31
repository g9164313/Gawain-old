package narl.itrc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Window;

public class Misc {
	
	public static boolean isUnix() {
		return System.getProperty("os.name").toLowerCase().contains("win")==false;
	}
	public static String getHomePath() {
		String path;
		if(Misc.isUnix()==true) {
			path = System.getenv("HOME");
			if(path==null) {
				return ".";
			}
		}else {
			path = System.getenv("HOMEPATH");
			if(path==null) {
				return "C:"+File.separatorChar;
			}else{
				path = "C:"+path;
			}
		}
		return path+File.separatorChar;
	}
	
	/**
	 * just show messages, it is like 'stdout'
	 * @param fmt - pass to 'String.printf()' 
	 * @param arg - pass to 'String.printf()' 
	 */
	public static void logv(String fmt,Object... arg){
		synchronized (System.out) { System.out.print(String.format('\21'+fmt+"\20\r\n", arg)); }
	}
	/**
	 * just show messages, it is like 'stdout'
	 * @param fmt - pass to 'String.printf()' 
	 * @param arg - pass to 'String.printf()' 
	 */
	public static void logw(String fmt,Object... arg){
		synchronized (System.out) { System.out.print(String.format('\22'+fmt+"\20\r\n", arg)); }
	}
	/**
	 * just show messages, it is like 'stderr'
	 * @param fmt - pass to 'String.printf()' 
	 * @param arg - pass to 'String.printf()' 
	 */
	public static void loge(String fmt,Object... arg){
		synchronized (System.out) { System.out.print(String.format('\23'+fmt+"\20\r\n", arg)); }
	}	
	//----------------------------------------//

	/*public static final KeyCombination shortcut_save = KeyCombination.keyCombination("Ctrl+S");
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
	}*/

	private static final SimpleDateFormat date_name = new SimpleDateFormat("yyyyMMdd-HHmm");
	/**
	 * Use this function to generate file name.<p>
	 * @return - the string type of file name.<p>
	 */
	public static String getDateName() {
		return date_name.format(new Timestamp(System.currentTimeMillis())); 
	}
	
	private static final SimpleDateFormat day_name = new SimpleDateFormat("MMdd-HHmm");	
	public static String getDayName() {
		return day_name.format(new Timestamp(System.currentTimeMillis())); 
	}
	private static final SimpleDateFormat tick_name = new SimpleDateFormat("HHmmss");	
	public static String getTickName() {
		return tick_name.format(new Timestamp(System.currentTimeMillis())); 
	}
	
	/**
	 * list all files(include sub-directory).<p>
	 * @param path - directory node
	 * @param travel - try to travel sub-directory
	 * @param allAppx - appendix or extension (ex:.jpg;.dll;.png...)
	 * @param allList - the result of list, output value
	 */
	public static void listFiles(
		final File path,
		final boolean travel,
		final String allAppx,
		final ArrayList<File> allList
	) {
		File[] lst;
		if(allAppx.length()==0) {
			lst = path.listFiles();
		}else {
			lst = path.listFiles(new FilterFile(allAppx));
		}
		if(lst==null) {
			return;
		}
		for(File fs:lst){			
			if(fs.isDirectory()==true && travel==true) {
				listFiles(fs,travel,allAppx,allList);
			}else if(fs.isFile()==true) {
				allList.add(fs);
			}
		}
	}
	private static class FilterFile implements FilenameFilter{
		private String[] appx;
		private boolean excu;//exclude flag
		public FilterFile(String allAppx) {
			excu = false;
			if(allAppx.charAt(0)=='^') {
				excu = true;
				allAppx = allAppx.substring(1);
			}
			appx = allAppx.split(";");
		}
		@Override
		public boolean accept(File dir, String name) {
			name = name.toLowerCase();
			for(String txt:appx) {
				txt = txt.toLowerCase();
				if(name.endsWith(txt)==true) {
					return (excu==true)?(false):(true);
				}
			}					
			return (excu==true)?(true):(false);
		}
	};
	//----------------------------------------//
	
	public static ImageView getIconView(String name){
		return getResView("/narl/itrc/res/icon",name);
	}
	public static Image getIconImage(String name){
		return getResImage("/narl/itrc/res/icon",name);
	}
	public static ImageView getPicView(String name){
		return getResView("/narl/itrc/res/pic",name);
	}
	public static Image getPicImage(String name){
		return getResImage("/narl/itrc/res/pic",name);
	}
	public static ImageView getResView(String pkg, String name){
		return new ImageView(getResImage(pkg,name));
	}
	public static Image getResImage(String pkg, String name){
		return new Image(Gawain.class.getResourceAsStream(pkg+"/"+name));
	}
	public static Node addBorder(final Node obj) {
		obj.getStyleClass().add("box-border");
		return obj;
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

	/**
	 * parse text of Python dictionary to java object.<p>
	 * @param txt
	 * @return
	 */
	public static HashMap<String,String> txt2dict(String txt){
		HashMap<String,String> map = new HashMap<String,String>();		
		txt = txt.replace('{', ' ').replace('}', ' ').trim();
		String[] row = txt.split(",\\s*");
		for(String itm:row) {
			String[] col = itm.split(":\\s*");
			String kk = col[0].replace('\'', ' ').trim();
			String vv = col[1].trim();
			map.put(kk, vv);
		}
		return map;
	}
	
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
	
	public static int hex2int(final String txt) {
		return Integer.valueOf(txt,16).byteValue();
	}
	public static int hex2int(
		final byte... buf
	) {
		return hex2int(new String(buf));
	}
	public static int hex2int(
		final byte[] buf,
		final int off,
		int size
	) {	
		if((off+size)>buf.length) {
			size = buf.length - off;
		}
		byte[] txt = new byte[size];
		for(int i=off; i<size; i++) {
			txt[i-off] = buf[i];
		}
		return hex2int(new String(txt));
	}
	
	/**
	 * It's same as "Integer.valueOf()", but this function accepts leading zero.<p>
	 * @param txt - the string of integer value(including leading zero)
	 * @return integer value
	 */
	public static Integer txt2Int(String txt){
		if(txt.length()==0) { return null; }
		try {
			txt = txt.replace("\\s","").trim();
			//remove leading zeros~~~
			while(txt.charAt(0)=='0' && txt.length()>1){
				txt = txt.substring(1);
			}		
			return Integer.decode(txt);
		}catch(NumberFormatException e) {
			Misc.loge(e.getMessage());
		}
		return null;
	}
	public static int txt2int(final String txt,final int def) {
		final Integer v = txt2Int(txt);
		if(v==null) { return def; }
		return v.intValue();
	}
	public static int txt2int(final String txt) {
		return txt2int(txt,0);
	}
	
	public static Float txt2Float(String txt) {
		if(txt.length()==0) { return null; }
		try {
			txt = txt.replace("\\s","").trim();			
			return Float.parseFloat(txt);
		}catch(NumberFormatException e) {
			Misc.loge("invalid float format:"+txt);
			return null;
		}
	}
	public static float txt2float(final String txt,final float def) {
		final Float v = txt2Float(txt);
		if(v==null) { return def; }
		return v.floatValue();
	}
	public static float txt2float(final String txt) {
		return txt2float(txt,0f);
	}

	public static Object txt2num(String txt) {
		if(txt.length()==0) { return null; }
		txt = txt.replace("\\s","").trim();
		try {
			return Integer.decode(txt);
		}catch(NumberFormatException e) {			
		}
		try {			
			return new Float(Float.parseFloat(txt));		
		}catch(NumberFormatException e) {			
		}		
		return null;
	}
	
	public static int txt2bit_val(String txt,final int offset) {		
		String[] col = txt.replace("\\s", "").split(",");
		int val = 0;
		for(String v:col) {
			if(v.matches("\\d+")==true) {
				final int pa = Integer.parseInt(v) + offset;
				if(pa<0) {
					continue;
				}
				val = val | (1<<pa);
			}else if(v.matches("\\d+[-|~]\\d+")) {
				String[] arg = v.split("[-|~]");
				final int pa = Integer.parseInt(arg[0]) + offset;
				final int pb = Integer.parseInt(arg[1]) + offset;
				if(pa<0 || pb<0 || pa>pb) {
					continue;
				}
				for(int i=pa; i<=pb; i++) {
					val = val | (1<<i);
				}
			}
		}		
		return val;
	}
	
	/**
	 * change tick(number value) to text.<p>
	 * @param tick - unit is millisecond.
	 * @param readable
	 * @return stamp text (時:分:秒)
	 */
	public static String tick2text(
		long tick, 
		final boolean readable,
		final int columns
	){
		if(tick<0) { tick = 0; }
		long sec = tick / 1000L;
		long min = sec / 60; sec = sec % 60;
		long hour= min / 60; min = min % 60;
		String txt = "";
		if(hour!=0){
			if(readable){
				txt = String.format(
					"%2d時%2d分%2d秒", 
					hour, min, sec
				);
			}else{
				txt = String.format(
					"%02d:%02d:%02d", 
					hour, min, sec
				);
			}
		}else if(min!=0){
			if(readable){
				txt = String.format("%2d分%2d秒", min, sec);
			}else{
				switch(columns) {
				default:txt = String.format("%02d:%02d", min, sec); break;
				case 2: txt = String.format("%02d:%02d", min, sec); break;
				case 3: txt = String.format("00:%02d:%02d", min, sec); break;
				}
			}
		}else{
			if(readable){
				txt = String.format("%2d秒", sec);
			}else{
				switch(columns) {
				default: txt= String.format("%02d", sec); break;
				case 2: txt = String.format("00:%02d", sec); break;
				case 3: txt = String.format("00:00:%02d", sec); break;
				}
			}
		}
		return txt;
	}
	public static String tick2text(final long tick){
		return tick2text(tick,false,-1);
	}
	public static String tick2text(final long tick,final boolean readable){
		return tick2text(tick,readable,-1);
	}
	/**
	 * change text to millisecond.<p>
	 * Text format is hh:mm:ss.<p>
	 * @param time - readable time value
	 * @return tick, unit is millisecond
	 */
	public static long text2tick(final String time){
		if(time.length()==0) {
			return 0L;
		}
		final long[] scale = {1L, 60L, 3600L};
		String[] arg = time.replace("\\s", "").split(":");
		int size = (arg.length>=3)?(3):(arg.length);
		long tick = 0L;
		for(int i=0; i<size; i++){
			//remove leading zeros~~~
			String txt = arg[i].replaceFirst("^0+(?!$)","");
			long val = (txt.length()==0)?
				(0L):
				(Integer.valueOf(txt)
			);
			val = val * 1000L * scale[size-1-i];
			tick += val;
		}
		return tick;
	}
	
	/**
	 * Un-escape a string that contains standard Java escape sequences.
	 * <ul>
	 * <li><strong>\b \f \n \r \t \" \'</strong> :
	 * BS, FF, NL, CR, TAB, double and single quote.</li>
	 * <li><strong>\\xXX</strong> : Hexadecimal specification (0x00 - 0xFF).</li>
	 * <li><strong>\\uXXXX</strong> : Hexadecimal based Unicode character.</li>
	 * </ul>
	 * @return The translated string.
	 */
	public static String unescape(String st) {
	 
	    StringBuilder sb = new StringBuilder(st.length());
	 
	    for (int i = 0; i < st.length(); i++) {
	        char ch = st.charAt(i);
	        if (ch == '\\') {
	            char nextChar = (i == st.length() - 1) ? '\\' : st.charAt(i + 1);
	            int code = 0;
	            switch (nextChar) {
	            case '\\':
	                ch = '\\';
	                break;
	            case 'b':
	                ch = '\b';
	                break;
	            case 'f':
	                ch = '\f';
	                break;
	            case 'n':
	                ch = '\n';
	                break;
	            case 'r':
	                ch = '\r';
	                break;
	            case 't':
	                ch = '\t';
	                break;
	            case '\"':
	                ch = '\"';
	                break;
	            case '\'':
	                ch = '\'';
	                break;
	            case 'x':
	                if (i >= st.length() - 3) {
	                    ch = 'x';
	                    break;
	                }
	                code = Integer.parseInt("" + 
	                	st.charAt(i + 2) + st.charAt(i + 3), 16
	                );
	                sb.append(Character.toChars(code));
	                i += 3;
	                continue;
	            case 'u':
	                if (i >= st.length() - 5) {
	                    ch = 'u';
	                    break;
	                }
	                code = Integer.parseInt("" + 
	                	st.charAt(i + 2) + st.charAt(i + 3) + 
	                	st.charAt(i + 4) + st.charAt(i + 5), 16
	                );
	                sb.append(Character.toChars(code));
	                i += 5;
	                continue;
	            }
	            i++;
	        }
	        sb.append(ch);
	    }
	    return sb.toString();
	}
	//----------------------------------------//
	
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
	//------------------------------------------

	public static byte[] list2byte(final ArrayList<Byte> src) {
		final int cnt = src.size();
		byte[] dst = new byte[cnt];
		for(int i=0; i<cnt; i++) {
			dst[i] = src.get(i).byteValue();
		}
		return dst;
	}
	
	public static int[] list2int(final ArrayList<Integer> src) {
		final int cnt = src.size();
		int[] dst = new int[cnt];
		for(int i=0; i<cnt; i++) {
			dst[i] = src.get(i).intValue();
		}
		return dst;
	}

	public static float[] list2float(final ArrayList<?> src) {		
		final int cnt = src.size();
		float[] dst = new float[cnt];
		if(cnt==0) {
			return dst;
		}
		Class<?> clzz = src.get(0).getClass();
		if(clzz==Float.class) {
			for(int i=0; i<cnt; i++) {				
				dst[i] = ((Float)src.get(i)).floatValue();
			}
		}else if(clzz==Double.class) {
			for(int i=0; i<cnt; i++) {				
				dst[i] = ((Double)src.get(i)).floatValue();
			}
		}			
		return dst;
	}
	public static double[] list2double(final ArrayList<?> src) {	
		final int cnt = src.size();
		double[] dst = new double[cnt];
		if(cnt==0) {
			return dst;
		}
		Class<?> clzz = src.get(0).getClass();
		if(clzz==Float.class) {
			for(int i=0; i<cnt; i++) {				
				dst[i] = ((Float)src.get(i)).doubleValue();
			}
		}else if(clzz==Double.class) {
			for(int i=0; i<cnt; i++) {				
				dst[i] = ((Double)src.get(i)).doubleValue();
			}
		}			
		return dst;
	}
	//----------------------------------------//
	
	public static void dump_byte(final byte[] buf) {
		try {
			final String name = Gawain.getRootPath()+"pack_"+UUID.randomUUID().toString()+".bin";
			FileOutputStream fs = new FileOutputStream(name);
			fs.write(buf);
			fs.close();
			Misc.loge("[Dump] %s", name);
		}catch(SecurityException | IOException e) {			
			Misc.loge("[Dump] %s", e.getMessage());
		}
	}
	
	public static void asynSerialize2file(		
		final Object obj,
		final String name
	) {
		new Thread(
			()->serialize2file(obj,name),
			"serial2file"
		).start();
	}
	
	public static void serialize2file(		
		final Object obj,
		final String name
	) {
		serialize2file(obj,new File(name));
	}
	public static Object deserializeFile(
		final String name
	) {
		return deserializeFile(new File(name));
	}

	public static void serialize2file(
		final Object obj,
		final File fs		
	) {
		try {
			FileOutputStream fid = new FileOutputStream(fs);
			ObjectOutputStream stm = new ObjectOutputStream(fid);
			stm.writeObject(obj);
			stm.close();
			fid.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static Object deserializeFile(
		final File fs
	) {
		Object obj = null;
		if(fs.exists()==false || fs.isFile()==false) {
			return obj;
		}
		try {
			FileInputStream fid = new FileInputStream(fs);
			ObjectInputStream stm = new ObjectInputStream(fid);
			obj = stm.readObject();
			stm.close();
			fid.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return obj;
	}

	@SuppressWarnings("unchecked")
	public static class BiMap<K,V> extends HashMap<K,V> {
		private static final long serialVersionUID = 2076368437439402996L;
		
		public final HashMap<V,K> dict = new HashMap<V,K>();
		
		public BiMap<K,V> init(Object... args){
			final int len = args.length - args.length%2;
			for(int i=0; i<len; i+=2) {				
				K k = (K)args[i+0];
				V v = (V)args[i+1];
				put(k, v);
				dict.put(v, k);
			}
			return this;
		}
		
		public K bi_get(V value) {
			return dict.get(value);
		}		
		public V bi_put(K key, V value) {
			V v = super.put(key, value);
			if(v!=null) {
				dict.remove(v);
			}			
			dict.put(value, key);
			return v;
		}
		public V bi_putIfAbsent(K key, V value) {
			V v = super.putIfAbsent(key, value);
			if(v==null) {
				dict.put(value, key);
			}
			return v;
		}		
	};
	//-------------------------------------

	public static byte[] chainBytes(final byte[]... lst) {
		ArrayList<Byte> dst = new ArrayList<Byte>();
		for(byte[] src:lst) {
			if(src==null) {
				continue;
			}
			for(int i=0; i<src.length; i++) {
				dst.add(src[i]);
			}
		}
		return Misc.list2byte(dst);
	}
	//-------------------------------------
	
	public static Object[] calculate_prefix(final float value) {
		
		String[] col = String.format("%.3E", value).split("E");
		
		final int exp = Integer.valueOf(col[1]);
		
		String prefix;
		float fix = 1f;
		final int div = Math.abs(exp) / 3;
		if(exp>=0) {
			switch(div) {
			default:
			case 5: prefix="P"; fix=1e-15f; break;
			case 4: prefix="T"; fix=1e-12f; break;
			case 3: prefix="G"; fix=1e-9f ; break;
			case 2: prefix="M"; fix=1e-6f ; break;
			case 1: prefix="k"; fix=1e-3f ; break;
			case 0: prefix="" ; break;
			}				
		}else {
			switch(div) {
			case 0: prefix="" ; break;
			case 1: prefix="m"; fix=1e3f ; break;
			case 2: prefix="μ"; fix=1e6f ; break;
			case 3: prefix="n"; fix=1e9f ; break;
			case 4: prefix="p"; fix=1e12f; break;
			default:
			case 5: prefix="f"; fix=1e15f; break;
			}
		}
		
		return new Object[] {value*fix,prefix};
	}
	
	public static class MetricPrefix extends SimpleStringProperty {
		
		public String postfix = "";
		
		public MetricPrefix(
			final ObservableValue<Number> prop,
			final String unit
		) {
			postfix = unit;
			event.changed(prop, null, prop.getValue());
			prop.addListener(event);						
		}
		
		private ChangeListener<Number> event = (obv,oldVal,newVal)->{
			
			final Object[] arg = calculate_prefix(newVal.floatValue());
			
			final float  preval = (float)arg[0];
			final String prefix = (String)arg[1];
			
			String full_text;
			if(prefix.length()==0) {
				full_text = String.format("%.3f%s%s", preval, prefix, postfix);
			}else {
				full_text = String.format("%.1f%s%s", preval, prefix, postfix);
			}
			set(full_text);
		};		
	}
	//-------------------------------------
}



