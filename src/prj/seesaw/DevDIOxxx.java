package prj.seesaw;

public class DevDIOxxx {

	public void open(){ }//All devices must provided this. 'Interface'? It is too tiny....
	
	public boolean readIBit(int bit){ return true; }
	
	public boolean readOBit(int bit){ return true; }
	
	public void writeOBit(int bit,boolean val){ }
	
	public int getPort(int port){ return 0; }
	
	public void setPort(int port,int val){ }
	
	public void close(){ }//All devices must provided this.
}
