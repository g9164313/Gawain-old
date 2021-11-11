package narl.itrc.init;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import narl.itrc.Gawain;
import narl.itrc.Misc;

public class ProcTest implements Runnable {
	
	@Override
	public void run() {
		try {
			
			Process pp = new ProcessBuilder("udisksctl","monitor")
				.directory(Gawain.dirRoot)
				.start();
			new Thread("looper").start();

			Scanner ss = new Scanner(pp.getInputStream());
			int cnt = 0;
			while(pp.isAlive()==true) {
				if(ss.hasNextLine()==false) {
					sleep(500);
					continue;
				}
				String txt = ss.nextLine();
				cnt+=1;
				if(cnt<3) {
					sleep(500);
					continue;
				}
				//parse text~~~
				
				//16:30:58.026: Added /org/freedesktop/UDisks2/block_devices/sdg
				//Operation:          drive-eject
				
				//Misc.logv(txt);
				if(txt.matches("^\\d\\d[:]\\d\\d[:].+Added[\\s]+/org/freedesktop/UDisks2/block_devices/sd\\w")==true) {
					//user insert USB storage					
					Misc.logv("insert USB storage");
				}else if(txt.matches("^[\\s]{2,}Operation[:][\\s]{2,}drive-eject.*")==true) {
					//user remove USB storage
					Misc.logv("remove USB storage");
				}
				
				if(cnt<0) { cnt=3; }//we reach to limit of integer~~~
			}
			ss.close();
			pp.destroy();
		} catch (IOException e) {
			
		}
	}
	
	public static Thread launch() {
		Thread obj = new Thread(new ProcTest(),"qqq");
		obj.setDaemon(true);
		obj.start();
		return obj;
	}

	private void sleep(final long val) {
		try {
			TimeUnit.MILLISECONDS.sleep(val);
		} catch (InterruptedException e) {
		}
	}
}
