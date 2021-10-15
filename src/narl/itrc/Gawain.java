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
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.util.Duration;
import narl.itrc.init.Loader;
import narl.itrc.init.LogStream;
import narl.itrc.init.PanSplash;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class Gawain extends Application {
	
	private static final Properties propCache = new Properties();
	private static final String propName = "conf.properties";
	
	private static void propPrepare(final String[] args){
		try {
			InputStream stm=null;
			File fs = new File(pathSock+propName);			
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
			File fs = new File(pathSock+propName);
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
			e.printStackTrace();
			Misc.loge("啟動失敗!!");
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
		//End of Application
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
		
	public static final boolean isPOSIX;
	public static final String pathRoot;//Working directory.
	public static final String pathHome;//User home directory.
	public static final String pathSock;//Data or setting directory.
		
	public static final File dirRoot;//Working directory.
	public static final File dirSock;//Data or setting directory.
	
	public static final String jarName;
	
	static {
		String txt = null;
		//check operation system....
		txt = System.getProperty("os.name").toLowerCase();
		if(txt.indexOf("win")>=0){
			isPOSIX = false;
		}else{
			isPOSIX = true;
		}
		//check working path
		pathRoot= new File(".").getAbsolutePath() + File.separatorChar;
		//check home path, user store data in this directory.
		if(isPOSIX==true){
			txt = System.getenv("HOME");
		}else{
			txt = System.getenv("HOMEPATH");
			txt = "C:"+txt;
		}
		if(txt==null){
			txt = ".";
		}
		pathHome = txt + File.separatorChar;
		//check whether self is a jar file or not~~~		
		//cascade sock directory.
		pathSock = pathHome + ".gawain" + File.separatorChar;
		
		dirRoot = new File(pathRoot);
		dirSock = new File(pathSock);
		
		if(dirSock.exists()==false){
			if(dirSock.mkdirs()==false){
				System.err.printf("we can't create sock-->%s!!\n",pathSock);
				System.exit(-2);
			}
		}
		try {
			File fs = new File(
				Gawain.class.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.toURI()
			);
			if(fs.isFile()==true) {
				txt = fs.getAbsolutePath();
			}else {
				txt = "";
			}
		} catch (URISyntaxException e) {
			txt = "";
		}
		if(txt.length()>0) {
			jarName = txt;
		}else {
			jarName = "";
		}
	}
}




