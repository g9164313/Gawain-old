package narl.itrc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

public class SNDir extends File {

	private static final long serialVersionUID = -2654865502562489786L;

	public SNDir(String pathname) {
		super(pathname);
		if(exists()==false){ mkdirs(); }
		//String txt = getAbsolutePath();
	}

	public SNDir(String pathname,String pattern) {
		super(pathname);
		if(exists()==false){ mkdirs(); }
		setPattern(pattern);
		genSNValue();
	}
	
	private String[] defPart = {null,null};
	private int defSN = 0;//file index - zero base
	
	private FilenameFilter defFilter = new FilenameFilter(){
		@Override
		public boolean accept(File dir, String name) {
			if(defPart[0]==null){
				return true;
			}
			return name.matches(defPart[0]+"\\d*"+defPart[1]);
		}
	};
	
	private void set_pattern(String pattern,String[] part){
		if(pattern==null){
			return;
		}
		int pos = pattern.lastIndexOf('%');
		if(pos<0){
			return;
		}
		part[0] = pattern.substring(0,pos);
		part[1] = pattern.substring(pos+1);
	}
	
	public void setPattern(String pattern){
		set_pattern(pattern,defPart);
	}
	
	public int genSNValue(){
		if(defPart[0]==null){
			return defSN;
		}
    	String[] lst = list(defFilter);    	
    	if(lst.length==0){
    		defSN = 0;
    	}else{
    		Arrays.sort(lst);
    		String txt = lst[lst.length-1];
    		int hd = defPart[0].length();
    		int ta = txt.length() - defPart[0].length() - defPart[1].length();
    		txt = txt.substring(hd,ta);
    		try{
    			defSN = Integer.valueOf(txt);
    		}catch(NumberFormatException e){
    			return -1;
    		}
    	}
    	defSN++;
		return defSN;
	}

	public String genSNName(){
		if(defPart[0]==null){
			return "";
		}		
		String txt="";
		File fs=null;
		do{
			txt = String.format(
				"%s%c%s%08d%s",
				getAbsolutePath(),
				File.separatorChar,
				defPart[0], defSN++, defPart[1]			
			);
			fs = new File(txt);
		}while(fs.exists()==true);
		return txt;
	}
	
	public String getSNName(String part0,int off){
		if(defPart[0]==null){
			return "";
		}
		return String.format(
			"%s%c%s%08d%s",
			getAbsolutePath(),
			File.separatorChar,
			part0, defSN+off, defPart[1]			
		);
	}
	
	public String getFullName(String name){
		return getPath()+File.separator+name;
	}
	
	public String[] getListName(String pattern){
		String[] lst = null;
		if(pattern==null){
			lst = list(defFilter);
		}else{
			String[] part = {null,null};
			set_pattern(pattern,part);
			final FilenameFilter filter = new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name) {
					if(part[0]==null){
						return true;
					}
					return name.matches(part[0]+"\\d*"+part[1]);
				}
			};
			lst = list(filter);
		}
		Arrays.sort(lst);
		return lst;
	}
	
	public int countFile(String pattern){
		return getListName(pattern).length;
	}
	
	public int countFile(){
		return list().length;		
	}

	public void clearData(String pattern){
		String[] part ={null,null};
		set_pattern(pattern,part);
		final FilenameFilter filter = new FilenameFilter(){
			@Override
			public boolean accept(File dir, String name) {
				if(part[0]==null){
					return true;
				}
				return name.matches(part[0]+"\\d*"+part[1]);
			}
		};
		for(File fs:listFiles(filter)){
			fs.delete();
		}
	}
	
	public void clearData(){
		for(File fs:listFiles()){
			fs.delete();
		}
	}
}




