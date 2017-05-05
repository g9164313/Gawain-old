package prj.scada;

import javafx.scene.Node;
import narl.itrc.DevTTY;

/**
 * Fatek PLC Controller
 * @author qq
 *
 */
public class DevFatek extends DevTTY {

	public DevFatek(){		
	}
	
	public DevFatek(String name){
		connect(name);
	} 
	
	/**
	 * connect TTY port and prepare to send commands.<p>
	 * @param name - device path name
	 * @return TRUE-success,<p> FALSE-fail to open TTY port
	 */
	public boolean connect(String name){
		if(open(name,"19200,8n1")<=0L){
			return false;
		}
		return true;
	}
	
	/**
	 * just close TTY device
	 */
	public void disconnect(){
		close();
	}
	//--------------------------------//
	
	@Override
	protected Node eventLayout() {
		return null;
	}	
}
