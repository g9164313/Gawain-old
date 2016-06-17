package narl.itrc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.jar.JarFile;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import narl.itrc.nat.Loader;

public class Gawain extends Application {
	
	public interface EventHook {		
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
		//release resource~~~
		for(EventHook h:hook){
			h.shutdown();
		}
	}
	//--------------------------------------------//
	
	public static Properties prop = new Properties();
	public static final String propName = "conf.properties";
	
	private static void propInit(){
		try {
			File fs = new File(Misc.pathTemp+propName);
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
	
	private static void propSave(){
		try {
			File fs = new File(Misc.pathTemp+propName);
			FileOutputStream stm=null;
			if(fs.exists()==false){
				//the first dump!!!
				stm = new FileOutputStream(Misc.pathTemp+propName);
				prop.store(stm,"");
				return;
			}
			//check whether to restore data~~~
			String txt = prop.getProperty("PROP_RESTORE","false");
			if(txt.equalsIgnoreCase("false")==true){
				return;
			}			
			stm = new FileOutputStream(Misc.pathTemp+propName);
			prop.store(stm,"");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
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
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		new Loader().popup();		
		if(optUnpack==true){
			Misc.logv("unpack done...");
			System.exit(0);
			return;
		}
		
		String name = prop.getProperty("LAUNCH","");	
		try {
			Object obj = Class.forName(name).newInstance();
			//$ Demo how to pass arguments~~~
			//Param1Type param1;
			//Class cl = Class.forName(className);
			//Constructor con = cl.getConstructor(Param1Type.class);
			//obj = con.newInstance(param1,param2);
			PanBase pan = (PanBase)obj;			
			pan.appear(primaryStage);			
		} catch (
			InstantiationException | 
			IllegalAccessException | 
			ClassNotFoundException e
		) {			
			e.printStackTrace();			
		}
	}
	
	@Override
	public void stop() throws Exception {
		PanBase.msgBox.stop();
	}
	 
	private static boolean optUnpack = false;
	
	public static void main(String[] argv) {
		propInit();
		
		//parse arguments~~~~
		for(int i=0; i<argv.length; i++){
			if(argv[i].toLowerCase().startsWith("-unpack")==true){
				optUnpack = true;
			}
		}
		//liceBind();//check dark license~~~
		
		launch(argv);
		
		propSave();
		
		hookShutdown();
	}
}




