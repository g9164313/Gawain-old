package narl.itrc.init;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import narl.itrc.Gawain;
import narl.itrc.Misc;

public class Loader {
	
	public static void start() {
		//check dependency~~~
		PanSplash.updateMessage("載入原生碼...");
		check_socket_dir();
		check_dependency();
		//awaken_terminal();
		PanSplash.updateMessage("等待主畫面...");
		PanSplash.updateProgress(-1,-1);
	}
	
	private static void check_socket_dir() {
		PanSplash.updateMessage("準備資源... ");
		final File fs = new File(Gawain.pathSock);
		if(fs.exists()==false) {
			//In fact, logger will create this directory
			if(fs.mkdirs()==false) {
				PanSplash.updateMessage("致命錯誤，無法建立資料夾！！");
				System.exit(-1);
			}
		}
		
		//step.1: scan resource files in package
		final String appx = "^.class;.css";
		final URL url = Loader.class.getResource("/narl/itrc/res");
		if(url==null) {
			PanSplash.updateMessage("致命內部URL錯誤！！");
			System.exit(-2);
		}
		ArrayList<File> lstRes = new ArrayList<File>();
		Misc.listFiles(
			new File(url.getPath()),false, 
			appx, lstRes
		);
		//step.2: list files in sock directory
		ArrayList<File> lstSock = new ArrayList<File>();
		Misc.listFiles(
			Gawain.dirSock,false,
			appx, lstSock
		);
		//step.3: check whether we need to copy it
		for(int i=0; i<lstRes.size(); i++) {
			PanSplash.updateProgress(i+1, lstRes.size());
			File rr = lstRes.get(i);
			boolean found = false;
			for(int j=0; j<lstSock.size(); j++) {
				File ss = lstSock.get(j);
				if(rr.getName().equals(ss.getName())==true) {
					found = true;
					lstSock.remove(j);
					break;
				}
			}
			if(found==false) {
				//How to create directory structure??
				//How to the compression file???
				try {
					Files.copy(
						rr.toPath(), 
						new File(Gawain.pathSock+rr.getName()).toPath()
					);
				} catch (IOException e) {					
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void check_dependency(){
		
		LinkedList<LibFile> lst = new LinkedList<LibFile>();
		
		util_list_all(Gawain.pathSock,lst);
		for(String key:Gawain.prop().stringPropertyNames()) {
			if(key.matches("LIB_PATH[\\d]*")==false) {
				continue;
			}
			String path = Gawain.prop().getProperty(key);
			PanSplash.updateMessage("列舉: "+path);
			util_list_all(path,lst);
		}
				
		int barCur=0, barMax=lst.size();
		PanSplash.updateProgress(0, barMax);

		while(lst.isEmpty()==false){
			LibFile fs = lst.pollFirst();
			try {
				System.loadLibrary(fs.getLibName());
				String name = fs.getName();
				Misc.logv("load "+name);
				barCur+=1L;
				PanSplash.updateMessage("載入依賴："+name);
				PanSplash.updateProgress(barCur, barMax);				
			} catch (UnsatisfiedLinkError e1) {
				fs.fail+=1;
				if(fs.fail>(barMax-barCur)){
					//we can't solve the dependency problem, so drop it out.
					PanSplash.updateMessage("拋棄："+fs.getName());//fs.getAbsolutePath()
					break;
				}else{
					//this file have the dependency problem, deal with it later.
					lst.addLast(fs);
					PanSplash.updateMessage("重排："+fs.getName());
				}				
			}			
		}
	}
	//---------------------------------//
	/*
	private static void check_property(final String attr){
		boolean isFile = attr.contains("FILE");
		for(int i=0; i>=0 ;i++){
			String val = null;
			if(i==0){
				val = Gawain.prop().getProperty(attr,"");
			}else{
				val = Gawain.prop().getProperty(attr+i,"");
			}			
			if(val.length()==0){
				if(i==0){
					//skip the first library path, user can count it from '1'
					continue;
				}else{
					break;
				}
			}
			val = val.replace(' ', ';').replace(',', ';');
			String[] lst = val.split(";");
			if(isFile==true){
				load_lib_file(lst);
			}else{
				load_lib_path(lst);
			}			
		}
	}
	
	private static void load_lib_file(String[] lstName){
		LinkedList<LibFile> lstTkn = new LinkedList<LibFile>();
		for(String name:lstName){
			LibFile tkn = new LibFile(name);
			if(tkn.isValid()==false){
				continue;
			}			
			add_to_enviroment(tkn.getDirPath());
			lstTkn.add(tkn);
		}
		//load_from_list(lstTkn);
	}
	private static void load_lib_path(String[] lstPath){		
		for(String name:lstPath){			
			load_lib_path(name);			
		}
	}
	private static void load_lib_path(String path){
		if(path.length()==0){
			//updateProgress(0, 0);
			return;
		}
		LinkedList<LibFile> lstTkn = new LinkedList<LibFile>();
		util_list_all(path, lstTkn);
		//load_from_list(lstTkn);
	}
	*/
	
	/**
	 * List all files including sub-directory
	 * @param rootName - path
	 * @param lst - result
	 */
	public static void util_list_all(String rootName, LinkedList<LibFile> lst) {
		
		File dir = new File(rootName);
		if(dir.exists()==false){
			return;
		}
		add_to_enviroment(rootName);
		
	    File[] lstFS = dir.listFiles();
	    for (File fs : lstFS) {
	        if(fs.isFile()==true){
	        	LibFile tkn = new LibFile(fs.getAbsolutePath());
	        	if(tkn.isValid()==true){
	        		lst.add(tkn);
	        	}
	        }else if(fs.isDirectory()==true){
	        	util_list_all(fs.getAbsolutePath(), lst);
	        }
	    }
	}
	
	/**
	 * Add the specified path to the java library path
	 * 
	 * @param name - the path to add
	 * @throws Exception
	 */
	private static void add_to_enviroment(final String name) {
		try {
			Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
			usrPathsField.setAccessible(true);
			// get array of paths
			final String[] paths = (String[]) usrPathsField.get(null);
			// check if the path to add is already present
			for (String path : paths) {
				if (path.equals(name)) {
					return;
				}
			}
			// add the new path
			final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
			newPaths[newPaths.length - 1] = name;
			usrPathsField.set(null, newPaths);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {			
			Misc.loge("fail to add path(%s)", name);
		}		
	}
	//---------------------------------//
	
	private static class LibFile extends File {
		private static final long serialVersionUID = -1208089454397724000L;
		/**
		 * there is difference about suffix.
		 */
		private final String suffix = (Gawain.isPOSIX==true)?(".so"):(".dll");  
		public int fail = 0;
		public LibFile(String pathname) {
			super(pathname);
		}
		public String getLibName(){
			String name = getName();
			int pos = name.indexOf(suffix);		
			if(pos<0){
				return name;
			}
			name = name.substring(0,pos);
			if(Gawain.isPOSIX==true){
				if(name.startsWith("lib")==true){
					name = name.substring(3);
				}
			}
			return name;
		}
		/*public String getDirPath(){
			String name = getPath();
			int pos = name.lastIndexOf(File.separatorChar);
			if(pos<0){
				return name;
			}
			return name.substring(0, pos);
		}*/
		public boolean isValid(){
			if(isFile()==false){
				return false;
			}
			String name = getName();	
			if(name.endsWith(suffix)==true){	        		
	    		if(Gawain.isPOSIX==true){
	    			if(name.startsWith("lib")==true){
	    				return true;
	    			}
	    		}else{
	    			return true;
	    		}
	    	}
			return false;
		}		
	}
		
	/*private void awaken_terminal() {
		updateMessage("確認外部命令");	
		updateProgress(-1.,-1.);
		for(Entry<Object, Object> e : Gawain.prop().entrySet()){
			final String key = e.getKey().toString();
			if(key.matches("^TERM_\\p{Alnum}+")==false) {
				continue;
			}
			final String tag = key.split("_")[1];
			final String val = e.getValue().toString();
			updateMessage("載入："+tag);	
			Terminal.getInstance().exec(tag,val);
		}
		//test~~~
		Misc.logv(">>"+Terminal.getInstance().exec("/usr/bin/python3","-c","print(4+4)"));
	}*/
}
