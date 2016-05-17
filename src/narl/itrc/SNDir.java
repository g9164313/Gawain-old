package narl.itrc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

/**
 * When put files in this directory, it will append a count number(Serial Number) to this file name
 * @author qq
 *
 */
public class SNDir extends File {

	private static final long serialVersionUID = -2654865502562489786L;

	/**
	 * append a count number(Serial Number) to file name in this directory
	 * @param pathname - the directory path, if non-existing, it will be made
	 * @param pattern - exchange the percentage(%) character with serial-number
	 */
	public SNDir(String pathname,String pattern) {
		super(pathname);
		if(exists()==false){ 
			mkdirs();
		}
		setPattern(pattern,nameTxt);
		genSNVal();
	}
	
	/**
	 * this array mean prefix and post-fix
	 */
	private String[] nameTxt = {null,null};
	/**
	 * this variable keeps serial number, it is one-base
	 */
	private int nameSN = 1;
	
	private FilenameFilter nameFilter = new FilenameFilter(){
		@Override
		public boolean accept(File dir, String name) {
			if(nameTxt[0]==null){
				return true;
			}
			return name.matches(nameTxt[0]+"\\d*"+nameTxt[1]);
		}
	};
	
	private void setPattern(String pattern,String[] part){
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

	/**
	 * create a serial number
	 * @return - serial number
	 */
	public int genSNVal(){
		if(nameTxt[0]==null){
			return nameSN;
		}
    	String[] lst = list(nameFilter);    	
    	if(lst.length==0){
    		nameSN = 1;
    	}else{
    		Arrays.sort(lst);
    		String txt = lst[lst.length-1];
    		int hd = nameTxt[0].length();
    		int ta = txt.length() - nameTxt[0].length() - nameTxt[1].length();
    		txt = txt.substring(hd,ta);
    		try{
    			nameSN = Integer.valueOf(txt) + 1;
    		}catch(NumberFormatException e){
    			return -1;
    		}
    	}
		return nameSN;
	}

	/**
	 * create new file name with serial number
	 * @return - file name
	 */
	public String genSNTxt(){
		if(nameTxt[0]==null){
			return "unknow.bin";
		}		
		String txt="";
		File fs=null;
		for(;;){
			txt = String.format(
				"%s%c%s%08d%s",
				getAbsolutePath(),
				File.separatorChar,
				nameTxt[0],nameSN,nameTxt[1]			
			);
			fs = new File(txt);
			if(fs.exists()==false){
				break;
			}
			nameSN++;
		}
		return txt;
	}
	
	/**
	 * get the latest file name with serial number
	 * @return - file name
	 */
	public String getSNTxt(){
		if(nameTxt[0]==null){
			return "unknow.bin";
		}
		return getSNTxt(nameTxt[0]);
	}
	
	/**
	 * get the latest file name,but the prefix exchange with parameter - "part"
	 * @param part0 - new prefix name
	 * @return - file name
	 */
	public String getSNTxt(String part0){
		if(nameTxt[1]==null){
			return "unknow.bin";
		}
		return String.format(
			"%s%c%s%08d%s",
			getAbsolutePath(),
			File.separatorChar,
			part0, nameSN, nameTxt[1]			
		);
	}
	//------------------------------------//
	//below lines may be deprecated :-(
	
	public String getFullName(String name){
		return getPath()+File.separatorChar+name;
	}
	
	public String[] getListName(String pattern){
		String[] lst = null;
		if(pattern==null){
			lst = list(nameFilter);
		}else{
			String[] part = {null,null};
			setPattern(pattern,part);
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
		setPattern(pattern,part);
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




