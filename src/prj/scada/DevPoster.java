package prj.scada;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;

import narl.itrc.DevBase;
import narl.itrc.Misc;

/**
 * Someone wants to save data by using HTTP post method.<p>
 * @author qq
 *
 */
public class DevPoster extends DevBase {
	
	private String addr = "";
	
	public DevPoster(final String address){
		TAG = "HTTP-poster";
		addr= address;
	}
	
	public final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(1000);
	
	private void looper() throws InterruptedException, IOException {
		if(addr.length()==0){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
			}
			return;
		}
		
		String msg = queue.take();
		String ans = "";
		URL url = new URL(addr);			
		HttpURLConnection con = (HttpURLConnection)url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type","application/json; charset=UTF-8");
        con.setRequestProperty("Accept", "application/json");
        con.setConnectTimeout(3000);
        con.setDoInput(true);
        con.setDoOutput(true);
        //send text to server
        PrintWriter ost = new PrintWriter(con.getOutputStream());
        ost.write(msg);
        ost.close();
        //get response
        Scanner ist = new Scanner(con.getInputStream());        
        while(ist.hasNext()){
        	ans = ans + ist.nextLine();
        }
        ist.close();
        Misc.logv("POST=%s", ans);
	}
	
	@Override
	public void open() {
		final String id = "looper";
		addState(id, ()->{
			try{
				looper();
			} catch (IOException | InterruptedException e) {
				Misc.loge("[%s] %s",TAG, e.getMessage());
				//nextState("");
			}
		});
		playFlow(id);
	}

	@Override
	public void close() {
		stopFlow();
	}

	@Override
	public boolean isLive() {
		return isFlowing();
	}
}
