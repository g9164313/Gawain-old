package narl.itrc.nat;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedList;

import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import narl.itrc.Gawain;
import narl.itrc.Misc;

public class Loader extends Task<Integer>{
	
	private class Token extends File {		

		private static final long serialVersionUID = -4492630432053041430L;
		
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
	};
	
	@Override
	protected Integer call() throws Exception {

		//check all path in property file, if not, just jump out~~~
		checkProperty("LIB_PATH");		
		checkProperty("LIB_FILE");
		
		//load library in the default directory
		//loadLibrary(Gawain.pathRoot);
		loadLibraryPath(Gawain.pathSock);
		return 0;
	}

	private void checkProperty(final String attr){
		
		boolean isFile = (attr.contains("FILE")==true)?(true):(false);

		for(int i=0; i>=0 ;i++){
			
			String val = null;
			if(i==0){
				val = Gawain.prop.getProperty(attr,"");
			}else{
				val = Gawain.prop.getProperty(attr+i,"");
			}			
			if(val.length()==0){
				if(i==0){
					//skip the first library path, use can count it from '1'
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
	
	private void loadLibraryFile(String[] lstName){
		
		LinkedList<Token> lstTkn = new LinkedList<Token>();
		
		for(String name:lstName){
			
			Token tkn = new Token(name);
			
			if(tkn.isValid()==false){
				continue;
			}			
			
			addLibraryPath(tkn.getDirPath());
			
			lstTkn.add(tkn);
		}
		
		load_from_list(lstTkn);
	}

	private void loadLibraryPath(String[] lstName){		
		for(String name:lstName){			
			loadLibraryPath(name);			
		}
	}
	
	private void loadLibraryPath(String path){
		
		if(path.length()==0){
			updateProgress(0, 0);
			return;
		}
		
		LinkedList<Token> lstTkn = new LinkedList<Token>();
		
		listAllFileInSub(path, lstTkn);
		
		load_from_list(lstTkn);
	}
	
	/**
	 * Indicate whether we need to print message....
	 */
	private static boolean debug = false;
	
	private void load_from_list(LinkedList<Token> lstTkn){
		
		long barCur=0L,  barMax=lstTkn.size();
		
		updateProgress(0, barMax);
		
		if(barMax==0L){
			return;
		}
		
		String msg = null;
		
		while(lstTkn.isEmpty()==false){
			
			Token tkn = lstTkn.pollFirst();
			
			try {
				
				System.loadLibrary(tkn.getLibName());
				
				msg = "載入 "+tkn.getName();
				updateMessage(msg);
				if(debug==true){
					System.out.println(msg);
				}

				barCur+=1L;
				
			} catch (UnsatisfiedLinkError e1) {
				
				tkn.fail+=1;

				if(tkn.fail>=(barMax-barCur)){
					//we can't solve the dependency problem, so drop it out.
					msg = "拋棄 "+tkn.getAbsolutePath();
					updateMessage(msg);
					if(debug==true){
						System.out.println(msg);
					}
					break;
				}else{
					//this file have the dependency problem, deal with it later.
					lstTkn.addLast(tkn);
					if(debug==true){
						System.out.println("重排 "+tkn.getName());
					}
				}				
			}
			
			updateProgress(barCur, barMax);
		}
	}
	
	
	/**
	 * there is difference about suffix.
	 */
	private final String suffix = (Gawain.isPOSIX==true)?(".so"):(".dll");  
	
	/**
	 * List all files including sub-directory
	 * @param dirName - path
	 * @param lstResult - result
	 */
	private void listAllFileInSub(String dirName, LinkedList<Token> lstResult) {
		
		File dir = new File(dirName);
		
		if(dir.exists()==false){
			return;
		}
		
		addLibraryPath(dirName);
		
	    File[] lstFS = dir.listFiles();
	    
	    for (File fs : lstFS) {
	    	
	        if(fs.isFile()==true){
	        	
	        	Token tkn = new Token(fs.getAbsolutePath());
	        	if(tkn.isValid()==true){
	        		lstResult.add(tkn);
	        	}
	        	
	        }else if(fs.isDirectory()==true){
	        	
	        	listAllFileInSub(fs.getAbsolutePath(), lstResult);
	        }
	    }
	}
	
	/**
	 * Adds the specified path to the java library path
	 * 
	 * @param pathToAdd - the path to add
	 * @throws Exception
	 */
	private void addLibraryPath(final String pathToAdd) {
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
			Misc.loge("fail to add path(%s)", pathToAdd);
		}		
	}
	
	private Parent layout(Stage stg){
		
		VBox lay0 = new VBox();
		
		lay0.getStyleClass().add("splash-board");
		
		ImageView img = Misc.getIcon("logo.jpg");
		
		ProgressBar bar = new ProgressBar();
		bar.prefWidthProperty().bind(stg.widthProperty().subtract(3));
		bar.progressProperty().bind(this.progressProperty());
		
		lay0.getChildren().addAll(img,bar);
		
		return lay0;
	}
	
	public void standby(){
		
		String val = Gawain.prop.getProperty("LIB_DEBUG","false");
		if(val.toLowerCase().endsWith("true")==true){
			debug = true;
		}
		
		final Stage stg = new Stage(StageStyle.UNIFIED);
		
		stg.initModality(Modality.WINDOW_MODAL);
		
		stg.initStyle(StageStyle.TRANSPARENT);
		
		stg.setResizable(false);
		
		stg.centerOnScreen();
		
		Scene scn = new Scene(layout(stg));
		scn.getStylesheets().add(Gawain.class.getResource("res/styles.css").toExternalForm());
		
		stg.setScene(scn);

		setOnSucceeded(event->{
			stg.close();
		});
		
		stg.setOnShown(event->{			
			new Thread(Loader.this,"Loader").start();
		});
		stg.showAndWait();
	}
}
