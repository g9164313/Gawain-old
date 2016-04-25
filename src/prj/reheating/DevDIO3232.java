package prj.reheating;

import com.jfoenix.controls.JFXCheckBox;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import narl.itrc.Gawain;

public class DevDIO3232 implements Gawain.EventHook {
	
	//this implements the stupid functions for DIO3232 (JS Automation)
	//input port:0~3, output port:4-7, they are 8-bit 'PCI port'
	//so the range 'bit' is 0~31, the range of 'port' is 0~7
	
	class PanSetting extends GridPane implements EventHandler<ActionEvent>{
		
		private final int PORT_MAX = 8;
		private final int BITS_MAX = 8;
		
		private JFXCheckBox[][] chkBit;
		
		private Label txtInfo = new Label();
		
		PanSetting(){
			init_box();
			getStyleClass().add("grid-small");
			Label txt;
			txt = new Label("bit7");
			txt.setAlignment(Pos.CENTER);
			add(txt,1,0);
			txt = new Label("bit0");
			txt.setAlignment(Pos.CENTER);
			add(txt,8,0);
			add(new Label("I-PORT(0):"),0,1);
			add(new Label("I-PORT(1):"),0,2);
			add(new Label("I-PORT(2):"),0,3);
			add(new Label("I-PORT(3):"),0,4);
			add(new Label("O-PORT(4):"),0,5);
			add(new Label("O-PORT(5):"),0,6);
			add(new Label("O-PORT(6):"),0,7);
			add(new Label("O-PORT(7):"),0,8);
			for(int port=0; port<PORT_MAX; port++){
				for(int bits=0; bits<BITS_MAX; bits++){
					add(chkBit[port][bits],BITS_MAX-bits,port+1);
					chkBit[port][bits].setAlignment(Pos.CENTER);
				}
			}
			txtInfo.setText("");
			add(txtInfo,0,9,8,1);
		}
		
		private void init_box(){
			chkBit = new JFXCheckBox[PORT_MAX][];
			
			for(int port=0; port<PORT_MAX; port++){
				chkBit[port] = new JFXCheckBox[BITS_MAX];
				for(int bits=0; bits<BITS_MAX; bits++){
					chkBit[port][bits] = new JFXCheckBox();
					if(port>=4){
						int idx = (port<<4)+bits;
						chkBit[port][bits].setUserData(idx);
						chkBit[port][bits].setOnAction(this);
					}
				}
			}
		}
		
		@Override
		public void handle(ActionEvent event) {
			if(cid<0){
				return;
			}
			JFXCheckBox box = (JFXCheckBox)event.getSource();
			int idx = (int)box.getUserData();
			int port = (idx & 0xF0)>>4;
			int bits = (idx & 0x0F);
			writeOBit(
				(port-4)*PORT_MAX+bits,
				chkBit[port][bits].selectedProperty().get()
			);
		}
		
		public void updateAllBox(){
			updateBox(0,PORT_MAX);
		}
		
		public void updateInputBox(){
			updateBox(0,4);
		}
		
		public void updateBox(int start,int end){
			if(cid<0){
				return;
			}
			if(txtInfo.getText().length()==0){				
				txtInfo.setText(String.format("CID=%d, VID=0x%X, ADDR=0x%X",cid,vid,addr));//update information once~~~
			}
			for(int port=0; port<PORT_MAX; port++){
				int val = getPort(port);
				for(int bits=0; bits<BITS_MAX; bits++){
					int v = (val & (1<<bits));
					if(v==0){
						chkBit[port][bits].selectedProperty().set(false);
					}else{
						chkBit[port][bits].selectedProperty().set(true);
					}
				}
			}
		}
	};
	
	public PanSetting panel = new PanSetting();
	
	private int cid = -1;
	private int vid = -1;//update by native code
	private int addr= -1;//update by native code
	private int sta = 0;//update by native code
	
	public DevDIO3232(){		
	}
	
	public DevDIO3232(int id){
		open(id);
	}

	public void open(int id){
		cid = id;
		open();
	}
	
	@Override
	public void shutdown() {
		close();
	}
	
	public native void open();
	public native boolean readIBit(int bit);
	public native boolean readOBit(int bit);
	public native void writeOBit(int bit,boolean val);
	public native int getPort(int port);
	public native void setPort(int port,int val);
	public native void close();
}
