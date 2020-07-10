package narl.itrc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import com.sun.glass.ui.Application;

import javafx.event.ActionEvent;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Window;

public class Misc {

	/**
	 * just show messages, it is like 'stdout'
	 * @param fmt - pass to 'String.printf()' 
	 * @param arg - pass to 'String.printf()' 
	 */
	public static void logv(String fmt,Object... arg){
		System.out.print(String.format('\21'+fmt+"\20\r\n", arg));
	}
	/**
	 * just show messages, it is like 'stdout'
	 * @param fmt - pass to 'String.printf()' 
	 * @param arg - pass to 'String.printf()' 
	 */
	public static void logw(String fmt,Object... arg){
		System.out.print(String.format('\22'+fmt+"\20\r\n", arg));
	}
	/**
	 * just show messages, it is like 'stderr'
	 * @param fmt - pass to 'String.printf()' 
	 * @param arg - pass to 'String.printf()' 
	 */
	public static void loge(String fmt,Object... arg){
		System.out.print(String.format('\23'+fmt+"\20\r\n", arg));
	}	
	//--------------------------//

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

	/**
	 * list all files(include sub-directory).<p>
	 * @param path - directory node
	 * @param allAppx - appendix or extension (ex:.jpg;.dll;.png...)
	 * @param allList - the result of list, output value
	 */
	public static void listFiles(
		final File path,
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
			if(fs.isDirectory()==true) {
				listFiles(fs,allAppx,allList);
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
	public static void copyFile(final File src, final File dst) {
		try {
			int len;
			byte[] buf = new byte[1024*512];
			FileInputStream ss = new FileInputStream(src);
			FileOutputStream dd = new FileOutputStream(dst);
			while((len=ss.read(buf))>0){
				dd.write(buf, 0, len);
			}
			dd.close();
			ss.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
	 * It's same as "Integer.valueOf()", but this function accepts leading zero.<p>
	 * @param txt - the string of integer value(including leading zero)
	 * @return integer value
	 */
	public static int txt2int(String txt){
		txt = txt.replace("\\s","").trim();
		if(txt.matches("^[+-]?[\\d]+")==false){
			return 0;
		}
		if(txt.charAt(0)=='+'){
			txt = txt.substring(1);
		}
		while(txt.charAt(0)=='0' && txt.length()>1){
			txt = txt.substring(1);
		}
		return Integer.valueOf(txt);
	}
	
	/**
	 * change tick(number value) to text.<p>
	 * @param tick - unit is millisecond.
	 * @param usePlainText
	 * @return stamp text (時:分:秒)
	 */
	public static String tick2time(final long tick, final boolean usePlainText){
		long sec = tick / 1000L;
		long min = sec / 60; sec = sec % 60;
		long hour= min / 60; min = min % 60;
		String txt = "";
		if(hour!=0){
			if(usePlainText){
				txt = String.format(
					"%2d時 %2d分 %2d秒", 
					hour, min, sec
				);

			}else{
				txt = String.format(
					"%2d:%2d:%2d", 
					hour, min, sec
				);
			}
		}else if(min!=0){
			if(usePlainText){
				txt = String.format("%2d分 %2d秒", min, sec);
			}else{
				txt = String.format("%2d:%2d", min, sec);
			}
		}else{
			if(usePlainText){
				txt = String.format("%2d秒", sec);
			}else{
				txt = String.format("%2d", sec);
			}
		}
		return txt;
	}
	public static String tick2time(final long tick){
		return tick2time(tick,false);
	}
	/**
	 * change text to millisecond.<p>
	 * Text format is hh:mm:ss.<p>
	 * @param time - 
	 * @return tick, unit is millisecond
	 */
	public static long time2tick(final String time){
		final long[] scale = {1L, 60L, 3600L};
		String[] arg = time.replace("\\s", "").split(":");
		int size = (arg.length>=3)?(3):(arg.length);
		long tick = 0L;
		for(int i=0; i<size; i++){
			//remove leading zeros~~~
			String txt = arg[i].replaceFirst("^0+(?!$)","");
			long val = (txt.length()==0)?
				(0L):
				(Integer.valueOf(txt));
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
	//--------------------------//
	
	public static void asynSerialize2file(
		final String file_name,
		final Object source
	) {
		new Thread(
			()->serialize2file(file_name,source),
			"serial2file"
		).start();
	}	
	public static void serialize2file(
		final String file_name,
		final Object source
	) {
		if(file_name.length()==0) {
			return;
		}
		try {
			FileOutputStream fid = new FileOutputStream(file_name);
			ObjectOutputStream stm = new ObjectOutputStream(fid);
			stm.writeObject(source);
			stm.close();
			fid.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	public static Object deserializeFile(
		final String file_name
	) {
		if(file_name.length()==0) {
			return null;
		}
		Object obj = null;
		try {
			FileInputStream fid = new FileInputStream(file_name);
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
	
	
	/**
	 * a wrap for GUI-thread application.
	 */
	public static void invoke(final Runnable work) {
		if(Application.isEventThread()==true) {
			work.run();
		}else {
			Application.invokeAndWait(work);
		}
	}
	
	/**
	 * execute a command. Remember this is 'blocking' function
	 * @param cmd - command line and arguments
	 * @return
	 */
	public static String exec(String... cmd){
		
		File working_path;
		if(cmd[0].contains(File.separator)==true){
			working_path = Gawain.dirRoot;
		}else{
			cmd[0] = Gawain.dirSock + cmd[0];
			working_path = Gawain.dirSock;
		}
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.redirectOutput();//How to use this??
		pb.redirectError();//How to use this??
		pb.directory(working_path);
		
		String txt = "";
		try {	
			Process pc = pb.start();
			pc.waitFor();
			/*byte[] buf = new byte[2048];
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
			}*/
			pc.destroy();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
			Misc.loge("[EXEC]>>", e.getMessage());
		}		
		return txt;
	}
}



