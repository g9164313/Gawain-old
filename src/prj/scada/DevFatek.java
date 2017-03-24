package prj.scada;

import javafx.scene.Node;
import narl.itrc.DevTTY;

public class DevFatek extends DevTTY {

	public DevFatek(){
		
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
