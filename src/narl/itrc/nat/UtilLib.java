package narl.itrc.nat;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;

import narl.itrc.Gawain;
import narl.itrc.Misc;

class UtilLib {

	public static void checkProperty(final String attr){
		
		boolean isFile = attr.contains("FILE");

		for(int i=0; i>=0 ;i++){
			
			String val = null;
			if(i==0){
				val = Gawain.getSetting().getProperty(attr,"");
			}else{
				val = Gawain.getSetting().getProperty(attr+i,"");
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
				loadLibraryFile(lst);
			}else{
				loadLibraryPath(lst);
			}			
		}
	}
	
	private static void loadLibraryFile(String[] lstName){
		LinkedList<Token> lstTkn = new LinkedList<Token>();
		for(String name:lstName){
			Token tkn = new Token(name);
			if(tkn.isValid()==false){
				continue;
			}			
			add_to_enviroment(tkn.getDirPath());
			lstTkn.add(tkn);
		}
		//load_from_list(lstTkn);
	}

	private static void loadLibraryPath(String[] lstPath){		
		for(String name:lstPath){			
			loadLibraryPath(name);			
		}
	}
	
	private static void loadLibraryPath(String path){
		if(path.length()==0){
			//updateProgress(0, 0);
			return;
		}
		LinkedList<Token> lstTkn = new LinkedList<Token>();
		list_all_files(path, lstTkn);
		//load_from_list(lstTkn);
	}
	
	
	
	public static LinkedList<Token> flatten(){
		
		LinkedList<Token> lst = new LinkedList<Token>();
		
		//STEP.1: prepare(copy) the build-in library
		
		//STEP.2: load library in the default directory
		list_all_files(Gawain.pathSock,lst);
		return lst;
	}
	
	/**
	 * List all files including sub-directory
	 * @param rootName - path
	 * @param lst - result
	 */
	private static void list_all_files(String rootName, LinkedList<Token> lst) {
		
		File dir = new File(rootName);
		if(dir.exists()==false){
			return;
		}
		add_to_enviroment(rootName);
		
	    File[] lstFS = dir.listFiles();
	    for (File fs : lstFS) {
	        if(fs.isFile()==true){
	        	Token tkn = new Token(fs.getAbsolutePath());
	        	if(tkn.isValid()==true){
	        		lst.add(tkn);
	        	}
	        }else if(fs.isDirectory()==true){
	        	list_all_files(fs.getAbsolutePath(), lst);
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
}
