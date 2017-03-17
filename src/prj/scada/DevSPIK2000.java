package prj.scada;

import narl.itrc.DevTTY;

/**
 * SPIK-2000A, plasma generator.<p>
 * It is just a "pulse" generator, with a huge bias voltage.<p>
 * This device use DK3964R protocol(SuperCOM?), it may come from SIEMENS........ 
 * @author qq
 *
 */
public class DevSPIK2000 extends DevTTY {

	public DevSPIK2000(){		
	}
	
	/**
	 * connect TTY port and prepare to send commands.<p>
	 * @param path - device name or full name
	 * @return TRUE-success,<p> FALSE-fail to open TTY port
	 */
	public boolean connect(String path){
		if(open(path,"19200,8n1")<=0L){
			return false;
		}
		//dig up information~~~
		return (open(path)>0L)?(true):(false);
	}
	
	/**
	 * just close TTY device
	 */
	public void disconnect(){
		close();
	}
	
}
