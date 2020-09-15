package narl.itrc.init;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Optional;

import narl.itrc.Gawain;


/**
 * Manager for terminal program.<p>
 * @author qq
 *
 */
public class Terminal {
		
	public final HashMap<String,Process> pool = new HashMap<String,Process>();
	
	private Terminal exec(
		final String tag,
		final String cmd
	) {		
		final ProcessBuilder buld = new ProcessBuilder(cmd); 		
		buld.directory(Gawain.dirSock);		
		try {
			pool.put(tag, buld.start());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	public String prompt(
		final String tag,
		final String txt
	) {	
		Process proc;
		try {
			proc = Runtime.getRuntime().exec("/usr/bin/python3");
			InputStream std1 = proc.getInputStream();
			int cnt;
			do {
				cnt = std1.available();
			}while(cnt>=0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		return "";
	}
	
	//-----------------------------//
	
	/**
	 * execute a command and get standard-output.<p>
	 * Remember this is 'blocking' function.<p>
	 * @param cmd - command line and arguments
	 * @return standard output
	 */
	public static String exec(final String... cmd){		
		String txt = "";
		try {
			Process proc = new ProcessBuilder(cmd)
				.directory(Gawain.dirSock)
				.start();
			proc.waitFor();
			InputStream stdin = proc.getInputStream();
			final byte[] buf = new byte[1024];
			while(stdin.available()!=0) {
				int len = stdin.read(buf);
				txt = txt + new String(buf,0,len);
			}
			proc.destroy();
		} catch (InterruptedException | IOException e) {
			e.printStackTrace();
		}		
		return txt;
	}

	private static Optional<Terminal> self = Optional.empty();
	
	public static Terminal getInstance() {
		if(self.isPresent()==false){
			self = Optional.of(new Terminal());
		}
		return self.get();
	}
}
