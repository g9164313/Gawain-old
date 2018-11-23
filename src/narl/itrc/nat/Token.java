package narl.itrc.nat;

import java.io.File;

import narl.itrc.Gawain;

class Token extends File {		
	
	private static final long serialVersionUID = -1208089454397724000L;


	/**
	 * there is difference about suffix.
	 */
	private final String suffix = (Gawain.isPOSIX==true)?(".so"):(".dll");  
	
	
	public int fail = 0;
	
	public Token(String pathname) {
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
	
	public String getDirPath(){
		String name = getPath();
		int pos = name.lastIndexOf(File.separatorChar);
		if(pos<0){
			return name;
		}
		return name.substring(0, pos);
	}
	
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

