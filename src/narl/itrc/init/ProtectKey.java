package narl.itrc.init;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.jar.JarFile;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import narl.itrc.Gawain;
import narl.itrc.Misc;

public class ProtectKey {
	
	private static void liceWrite(byte[] dat){
		if(Gawain.jarName.length()==0){
			return;//no!!! we don't have a jar file~~
		}
		try {
			RandomAccessFile fs = new RandomAccessFile(Gawain.jarName,"rw");	
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
		if(Gawain.jarName.length()==0){
			return false;//no!!! we don't have a jar file~~
		}
		try {
			BasicFileAttributes attr = Files.readAttributes(
				Paths.get(Gawain.jarName),
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
		
		if(Gawain.jarName.length()==0){
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
			final JarFile jj = new JarFile(Gawain.jarName);
			String txt = jj.getComment();
			if(txt==null){				
				if(isBorn()==false){
					Misc.loge("It is Fail,This is not a birthday!!");
					System.exit(-204);
				}
				//first,bind a license~~
				txt = Gawain.prop().getProperty("LICENCE","1");//default is one day~~~
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
