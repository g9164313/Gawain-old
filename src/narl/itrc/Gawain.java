package narl.itrc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarFile;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import narl.itrc.nat.Loader;

public class Gawain extends Application {
	
	public interface EventHook {
		/**
		 * Callback, when pprogram is shutdown.<p>
		 */
		public void shutdown();		
	}
	
	private static ArrayList<EventHook> hook = new ArrayList<EventHook>();
	
	public static void hook(EventHook h){
		if(hook.contains(h)==true){
			return;
		}
		hook.add(h);
	}
	
	private static void hookShutdown(){
		for(EventHook h:hook){
			h.shutdown();
		}
	}
	//--------------------------------------------//
	
	public static Properties prop = new Properties();
	public static final String propName = "conf.properties";
	
	private static void propInit(){
		try {
			File fs = new File(Misc.pathSock+propName);
			InputStream stm=null;
			if(fs.exists()==false){
				stm = Gawain.class.getResourceAsStream("/narl/itrc/res/"+propName);
			}else{
				stm = new FileInputStream(fs);
			}
			prop.load(stm);
			stm.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void propKeep(){
		try {
			File fs = new File(Misc.pathSock+propName);
			FileOutputStream stm=null;
			if(fs.exists()==false){
				//the first dump!!!
				stm = new FileOutputStream(Misc.pathSock+propName);
				prop.store(stm,"");
				return;
			}
			//check whether to restore data~~~
			String txt = prop.getProperty("PROP_RESTORE","false");
			if(txt.equalsIgnoreCase("false")==true){
				return;
			}			
			stm = new FileOutputStream(Misc.pathSock+propName);
			prop.store(stm,"");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	//--------------------------------------------//
	
	private static class Pipe extends OutputStream {
		public PrintStream org;
		public PrintStream fid;
		public Pipe(){			
		}
		@Override
		public void write(int b) throws IOException {
			org.write(b);
			if(fid!=null){
				fid.write(b);
			}
		}
		@Override
		public void flush() throws IOException {
			org.flush();
			if(fid!=null){ 
				fid.flush(); 
			}
		}
		@Override
		public void close() throws IOException {
			org.close();
			if(fid!=null){
				fid.close();
			}
		}
	};
	private static Pipe[] pipe = {
		new Pipe(), 
		new Pipe()
	};
	
	private static void loggerInit(){
		PrintStream fid = null;
		String opt = prop.getProperty("Logger", null);
		if(opt!=null){
			//it may be boolean flag or file path name~~~
			if(opt.equalsIgnoreCase("true")==true || opt.length()==0){
				//default path~~~
				opt = Misc.pathSock + "logger.txt";
			}
			try {
				fid = new PrintStream(opt, "UTF-8");
			} catch (FileNotFoundException e) {
				e.printStackTrace(); fid = null; 
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace(); fid = null;
			}	
		}
		
		pipe[0].org = System.out;
		pipe[0].fid = fid;
		System.setOut(new PrintStream(pipe[0]));
		
		pipe[1].org = System.err;
		pipe[1].fid = fid;
		System.setErr(new PrintStream(pipe[1]));
	}
	//--------------------------------------------//
	
	private static PanBase parent = null;
	
	public static Window getMainWindow(){
		if(parent==null){
			return null;
		}
		return parent.getScene().getWindow();
	}
	
	public static boolean isMainWindow(PanBase pan){
		return pan.equals(parent);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		if(optUnpack==true){
			Misc.logv("unpack done...");
			System.exit(0);
			return;
		}
		new Loader().standby();
		String name = prop.getProperty("LAUNCH","");	
		try {
			Object obj = Class.forName(name).newInstance();
			//$ Demo how to pass arguments~~~
			//Param1Type param1;
			//Class cl = Class.forName(className);
			//Constructor con = cl.getConstructor(Param1Type.class);
			//obj = con.newInstance(param1,param2);
			parent = (PanBase)obj;			
			parent.appear(primaryStage);
			//Misc.logv("啟動 launch [%s]",name);//show the first message for box-logger
		} catch (
			InstantiationException | 
			IllegalAccessException | 
			ClassNotFoundException e
		) {			
			e.printStackTrace();			
		}
	}
	
	public static AtomicBoolean flgStop = new AtomicBoolean(false);
	
	@Override
	public void stop() throws Exception {
		//Do we need to close render looper???
		flgStop.set(true);		
	}
	
	private static boolean optUnpack = false;

	public static void main(String[] argv) {
		flgStop.set(false);
		propInit();
		//parse arguments~~~~
		for(int i=0; i<argv.length; i++){
			if(argv[i].toLowerCase().startsWith("-unpack")==true){
				optUnpack = true;
			}
		}
		loggerInit();
		//liceBind();//check dark license~~~		
		launch(argv);		
		propKeep();
		hookShutdown();
	}
	//--------------------------------------------//
	
	private static void liceWrite(byte[] dat){
		try {
			RandomAccessFile fs = new RandomAccessFile(Misc.fileJar,"rw");	
			final byte[] buf = {0,0,0,0};
			for(long pos = fs.length()-4; pos>0; --pos){				
				fs.seek(pos);
				fs.read(buf,0,4);
				if(
					buf[3]==0x06 && 
					buf[2]==0x05 && 
					buf[1]==0x4b && 
					buf[0]==0x50
				){
					//reach to EOCD
					fs.seek(pos+20);
					short len = (short)(dat.length*2);
					buf[0] = (byte)((len&0x00FF)   );
					buf[1] = (byte)((len&0xFF00)>>8);
					fs.write(buf,0,2);
					fs.write(Misc.hex2txt(dat).getBytes());
					break;
				}
			}						
			fs.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-202);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-203);
		}
	}
		
	private static final String LICE_CIPHER="AES";
	private static final String LICE_SECKEY=Gawain.class.toString();	
	private static float liceDay = 1.f;//unit is days
	private static SecretKeySpec liceKey = null;
	private static Cipher liceCip = null;
		
	private static boolean isBorn(){
		try {
			BasicFileAttributes attr = Files.readAttributes(
				Paths.get(Misc.fileJar),
				BasicFileAttributes.class
			);
			FileTime t1 = attr.lastAccessTime();
			FileTime t2 = attr.lastModifiedTime();
			long diff = t1.toMillis() - t2.toMillis();
			if(diff<60000){
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
		return false;
	}
		
	private static EventHandler<ActionEvent> eventPeekLice = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			//check license key per hour 
			if(liceDay<0.f){
				Misc.logv("License is expired!!!");
				System.exit(0);
				return;
			}			
			liceDay = liceDay - (1.f/24.f);
			String txt = String.format("%.2f",liceDay);			
			try {
				liceCip.init(Cipher.ENCRYPT_MODE,liceKey);
				liceWrite(liceCip.doFinal(txt.getBytes()));
			} catch (InvalidKeyException e) {
				e.printStackTrace();
				System.exit(-11);
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
				System.exit(-12);
			} catch (BadPaddingException e) {
				e.printStackTrace();
				System.exit(-13);
			}
			Misc.logv("check licence - %.2fday",liceDay);
		}
	};
		
	@SuppressWarnings("unused")
	private static void liceBind(){		
		if(Misc.fileJar==null){
			return;//no!!! we don't have a jar file~~
		}
		try {
			liceKey = new SecretKeySpec(
				LICE_SECKEY.getBytes(),
				LICE_CIPHER
			);
			liceCip = Cipher.getInstance(LICE_CIPHER);			
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
			System.exit(-11);
		} catch (NoSuchPaddingException e1) {
			e1.printStackTrace();
			System.exit(-12);
		}
		
		try {			
			final JarFile jj = new JarFile(Misc.fileJar);
			String txt = jj.getComment();
			if(txt==null){				
				if(isBorn()==false){
					Misc.loge("It is Fail,This is not a birthday!!");
					System.exit(-204);
				}
				//first,bind a license~~
				txt = prop.getProperty("LICENCE=","1");//default is one day~~~
				liceDay = Float.valueOf(txt);
				liceCip.init(Cipher.ENCRYPT_MODE,liceKey);
				liceWrite(liceCip.doFinal(txt.getBytes()));
			}else{
				//check whether license is valid~~
				liceCip.init(Cipher.DECRYPT_MODE,liceKey);	
				txt = new String(liceCip.doFinal(Misc.txt2hex(txt)));
				liceDay = Float.valueOf(txt);
				if(liceDay<0.f){
					Misc.logv("License is expired!!!");
					System.exit(0);
				}
			}
			jj.close();
			
			Timeline timer = new Timeline(new KeyFrame(
				Duration.hours(1),
				eventPeekLice
			));
			timer.setCycleCount(Timeline.INDEFINITE);
			timer.play();
		} catch (IOException e) {			
			e.printStackTrace();
			System.exit(-201);
		} catch (InvalidKeyException e) {			
			e.printStackTrace();
			System.exit(-11);
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
			System.exit(-11);
		} catch (BadPaddingException e) {
			e.printStackTrace();
			System.exit(-11);
		} catch (NumberFormatException e){
			//someone want to modify license!!!
			System.exit(0);
		}
	}
	//--------------------------------------------//	
}




