package narl.itrc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.stage.Stage;
import narl.itrc.init.Loader;
import narl.itrc.init.LogStream;
import narl.itrc.init.PanSplash;


public class Gawain extends Application {
	
	private static final Properties propCache = new Properties();
	private static final String propName = "conf.properties";
	
	private static void propPrepare(final String[] argv){
		String name = getRootPath()+propName;
		for(int i=0; i<argv.length; i++) {
			if(argv[i].startsWith("conf-path=")==true) {
				String[] val = argv[i].split("=");
				name = val[1] + File.separatorChar + propName;
				break;
			}
		}
		try {
			InputStream stm=null;			
			File fs = new File(name);			
			if(fs.exists()==false){
				stm = Gawain.class.getResourceAsStream("/narl/itrc/res/"+propName);
			}else{
				stm = new FileInputStream(fs);
			}
			propCache.load(stm);
			stm.close();			
		} catch (IOException e) {
			e.printStackTrace();
		}
		//override the parameter in the configure file....
		for(int i=0; i<argv.length; i++) {
			if(argv[i].matches("[\\p{Print}&&[^=]]+[=][\\p{Print}&&[^=]]+")==false) {
				break;
			}
			String[] val = argv[i].split("=");
			propCache.setProperty(val[0], val[1]);
		}
	}
	
	public static Properties prop(){
		return propCache;
	}
	
	public static boolean propFlag(final String option) {
		String val = propCache.getProperty(option, "");
		if(val.length()==0) {
			return false;
		}
		val = val.trim().toLowerCase();
		if(val.charAt(0)=='t') {
			//true or 't' is okay~~
			return true;
		}
		return false;
	}
	
	private static void propRestore(){
		try {
			File fs = new File(getRootPath()+propName);
			if(fs.exists()==false){
				return;
			}
			//cache the old data
			ArrayList<String> lst =new ArrayList<String>();
			BufferedReader rd = new BufferedReader(new FileReader(fs));
			while(rd.ready()==true){
				String txt = rd.readLine();
				if(txt==null){
					break;
				}
				lst.add(txt);
			}
			rd.close();
			//check all options again~~~
			for(int i=0; i<lst.size(); i++){
				String txt = lst.get(i).trim();
				if(txt.length()==0 || txt.charAt(0)=='#'){
					continue;
				}
				String[] col = txt.split("=");				
				if(col.length<=1){
					continue;
				}
				col[0] = col[0].trim();
				String val = propCache
						.getProperty(col[0],null)
						.replace("\\", "\\\\");
				if(val==null){
					continue;//is this possible???
				}
				lst.set(i, col[0]+"="+val);
				propCache.remove(col[0]);
			}
			//add the new options...
			for(Object key:propCache.keySet()){
				String val = ((String)propCache.get(key))
					.replace("\\", "\\\\");
				lst.add(String.format("%s = %s", (String)key, (String)val));
			}
			//dump data and option!!!
			BufferedWriter wr = new BufferedWriter(new FileWriter(fs));
			for(String txt:lst){				
				wr.write(txt+"\r\n");
			}
			wr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	//--------------------------------------------//
	
	public static PanBase mainPanel = null;
	
	@Override
	public void start(Stage stg) throws Exception {
		long tick = System.currentTimeMillis();
		try {
			final String name = Gawain.prop().getProperty("LAUNCH","");
			mainPanel = (PanBase)Class.forName(name)
				//.getConstructor()
				//.newInstance();
				.getConstructor(Stage.class)
				.newInstance(stg);
			mainPanel.initLayout();
			mainPanel.appear();
		} catch (
			InstantiationException | 
			IllegalAccessException | 
			IllegalArgumentException | 
			InvocationTargetException | 
			NoSuchMethodException |
			SecurityException | 
			ClassNotFoundException e
		) {			
			Misc.loge("啟動失敗!!\n");
			e.printStackTrace();
			mainPanel = LogStream.getInstance().showConsole(stg);
		}
		tick = System.currentTimeMillis()-tick;		
		Misc.logv("啟動時間: %dms",tick);	
		PanSplash.done();
	}	

	public static void main(String[] args) {		
		propPrepare(args);		
		PanSplash.spawn(args);		
		Loader.start();
		LogStream.getInstance();
		//liceBind();
		launch(args);
		//End of Application, notify all devices down~~~
		DevBase.shutdown.set(true);
		//restore property and waiting daemon thread		
		if(jarName.length()!=0){
			//This is the jar file, try to wait release resource.
			try {
				//let daemon thread have chance to escape its loop.
				Misc.logv("waiting to shutdown...");
				System.gc();
				TimeUnit.SECONDS.sleep(1);
			}catch(InterruptedException e) { 
			}
		}
		LogStream.getInstance().close();
		propRestore();
	}
	//--------------------------------------------//
	//In this section, we initialize all global variables~~
	public static final String sheet = Gawain.class.
		getResource("res/styles.css").
		toExternalForm();		
	public static final String jarName;
	public static final File dirRoot;
	
	public static String getRootPath() {
		return dirRoot.getAbsolutePath()+File.separatorChar;
	}
	public static String getSockPath() {
		//old path, in home directory, named ".gawain"
		String path = Misc.getHomePath() + ".gawain" + File.separatorChar;
		if(new File(path).exists()==false) {
			return getRootPath();
		}
		return path;
	}
	public static File getSockFile() {
		//old path, in home directory, named ".gawain"
		final String path = Misc.getHomePath() + ".gawain" + File.separatorChar;
		final File fs = new File(path);
		if(fs.exists()==false) {
			return fs;
		}
		return dirRoot;
	}
	
	static {
		URI uri = null;
		try {
			uri = Gawain.class.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.toURI();
		} catch (URISyntaxException e) {
			System.exit(-1);
		}
		//check debug or release mode.
		//In debug mode, use home directory as root path
		//In release mode, use current directory as root path
		File fs = new File(uri);
		String path;
		if(fs.isFile()==true) {
			//yes, release mode, code is executed from jar file
			jarName = fs.getAbsolutePath();
			path = new File(".").getAbsolutePath();
		}else {
			//Oh, debug mode, code is executed from binary-class directory
			jarName = "";
			path = Misc.getHomePath() + ".gawain" + File.separatorChar;
			if(new File(path).exists()==false){
				if(fs.mkdirs()==false){
					System.err.printf("we can't create sock-->%s!!\n",path);
					System.exit(-2);
				}
			}
		}
		dirRoot = new File(path);
	}
}




