package narl.itrc.nat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;

import javafx.concurrent.Task;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.TskDialog;

public class Loader extends TskDialog {

	public Loader(){
		title = "Tsk-Loader";
	}
	
	@Override
	public int looper(Task<Integer> tsk) {
		String txtConf = Gawain.prop.getProperty("LIB","");
		if(txtConf.length()==0){
			return -1;
		}
		String[] node = txtConf
			.replace(' ', ';')
			.replace(',', ';')
			.split(";");
		if(node[0].length()==0){
			log("沒有設定LIB變數!!");
			return -2;
		}
		addLibraryPath(Misc.pathRoot);//default search path~~~
		addLibraryPath(Misc.pathTemp);
		
		log("複製函式庫...");
		for(int i=0; i<node.length; i++){
			int pos = node[i].indexOf('@');
			if(pos>=0){
				//user indicate a search path~~~~
				String txt = node[i].substring(pos+1);
				node[i] = node[i].substring(0, pos);
				addLibraryPath(txt);
			}else{
				prepare_lib(node[i]);				
			}
			log("複製 "+node[i]);
			setProgress(1+i,node.length);//stepping~~~
			if(tsk.isCancelled()==true){
				log("中止!!");
				return -3;
			}
		}
						
		log("載入函式庫...");
		for(int i=0; i<node.length; i++){
			reload_lib(node[i]);
			setProgress(1+i,node.length);//stepping~~~
			if(tsk.isCancelled()==true){
				log("中止!!");
				return -3;
			}
		}		
		return 1;//we success!!!
	}

	private void reload_lib(String node){
		String flat="", deps="";
		LinkedList<String> lst = null;
		if(node.charAt(0)=='.'){
			node = node.substring(1);
			flat = node;
			lst = new LinkedList<String>();
			lst.add(node);
		}else{
			flat = Gawain.prop.getProperty(node,"").trim();
			String[] tmp;
			if(flat.indexOf(',')>=0){
				tmp = flat.split(",");
			}else{
				tmp = flat.split("\\s");
			}			
			if(tmp[0].length()==0){
				return;
			}
			//check suffix name again!!!
			for(int i=0; i<tmp.length; i++){
				if(Misc.isFxxkMicrosoft()==true){
					if(tmp[i].endsWith(".dll")==true){
						tmp[i] = tmp[i].substring(0,tmp[i].length()-4);
					}
				}else{
					if(tmp[i].startsWith("lib")==true){
						tmp[i] = tmp[i].substring(3);
					}
					if(tmp[i].endsWith(".so")==true){
						tmp[i] = tmp[i].substring(0,tmp[i].length()-3);
					}
				}
			}
			lst = new LinkedList<String>(Arrays.asList(tmp));
		}
		int failMaxium=lst.size();
		int failCount=0;
		while(lst.size()!=0){
			String name = lst.pollFirst();
			try {			
				System.loadLibrary(name);				
				//Handle.sync(bar,max-lst.size());
				if(deps.length()==0){
					deps = name;
				}else{
					deps = deps+ ","+name;
				}
				log("載入 "+name);
			} catch (UnsatisfiedLinkError e1) {
				Misc.logv(e1.getMessage());
				log("相依問題："+name);
				failCount++;
				if(failCount>failMaxium){
					//show how many libraries we can't load~~
					String rem="";
					for(String txt:lst){
						rem = rem + txt +" ";
					}
					Misc.loge("fail to load library:\n"+rem);
					break;
				}				
				lst.addLast(name);				
			} catch (Exception e2){
				log("問題("+name+")："+e2.getMessage());
			}
		}
		//Misc.logv("load modules:\n%s",deps);
		if(deps.equalsIgnoreCase(flat)==false){
			Gawain.prop.setProperty(node,deps);//restore it for next turn~~~
		}
	}
		
	private void prepare_lib(String node){
		String src=null,dst=null;
		String[] lst = null;
		boolean isBase = false;
		if(node.charAt(0)=='.'){
			src = ".";
			dst = Misc.pathTemp;	
			lst = new String[1];
			lst[0] = node.substring(1);
			isBase = true;
		}else{
			src = String.format(
				"%s_%s_%s",
				node,Misc.arch,Misc.getOSName()
			);
			dst = Misc.pathTemp+node;			
			lst = Gawain.prop.getProperty(node,"").replace(' ',',').split(",");
			if(lst[0].length()==0){
				return;
			}
			//if we don't have directory, make it!!!
			File dir = new File(dst);
			if(dir.exists()==false){
				dir.mkdirs();
			}
			addLibraryPath(dst);
		}
		//Handle.sync(bar,0,lst.length);
		//Handle.sync(txt,"複製"+node);
		for(int i=0; i<lst.length; i++){
			export_file(dst,src,lst[i],isBase);
			//Handle.sync(bar,i+1);
		}		
	}
	
	public void export_file(String dst,String src,String name,boolean isBase){
		InputStream ss = null;
		OutputStream dd = null;
		if(Misc.isFxxkMicrosoft()==true){
			if(name.endsWith(".dll")==false){
				name=name+".dll";
			}
		}else{
			if(name.startsWith("lib")==false){
				name="lib"+name;
			}
			if(name.endsWith(".so")==false){
				name=name+".so";
			}
		}
		if(dst.charAt(dst.length()-1)==File.separatorChar){
			dst=dst+name;
		}else{
			dst=dst+File.separator+name;
		}
		//if we have files, don't override it!!!!
		File fs = new File(dst);
		if(fs.exists()==true){
			return;
		}
		if(src.equalsIgnoreCase(".")==true){
			ss = Loader.class.getResourceAsStream(name);
		}else{
			ss = Loader.class.getResourceAsStream(src+"/"+name);
		}
		if(ss==null){
			Misc.loge("fail to get %s/%s from resource!!",src,name);
			System.exit(-1);
		}		
		try {
			int len;
			byte[] buf = new byte[4096];
			dd = new FileOutputStream(dst);
			while((len=ss.read(buf))>0){
				dd.write(buf, 0, len);
			}
			dd.close();
		} catch (FileNotFoundException e) {			
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static boolean isSymlink(File file) {
		File canon;
		if (file.getParent() == null) {
			canon = file;
		} else {
			File canonDir;
			try {
				canonDir = file.getParentFile().getCanonicalFile();
			} catch (IOException e) {
				return false;
			}
			canon = new File(canonDir, file.getName());
		}
		try {
			return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Adds the specified path to the java library path
	 * 
	 * @param pathToAdd
	 *            the path to add
	 * @throws Exception
	 */
	 public static void addLibraryPath(String pathToAdd) {
		Field usrPathsField;
		try {
			usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
			usrPathsField.setAccessible(true);
			// get array of paths
			final String[] paths = (String[]) usrPathsField.get(null);
			// check if the path to add is already present
			for (String path : paths) {
				if (path.equals(pathToAdd)) {
					return;
				}
			}
			// add the new path
			final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
			newPaths[newPaths.length - 1] = pathToAdd;
			usrPathsField.set(null, newPaths);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {			
			e.printStackTrace();
		}		
	}
}




