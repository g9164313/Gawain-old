package prj.daemon;

import java.util.ArrayList;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.PanBase;
import narl.itrc.PanTTY;
import prj.scada.DevSPIK2000;

public class PanTestUnit1 extends PanBase {

	public PanTestUnit1(){		
	}
	
	//private DevSPIK2000 dev = new DevSPIK2000();
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final TextArea txt1 = new TextArea();

		final TextArea txt2 = new TextArea();
		
		final Button btn1 = PanBase.genButton2("TTY 設定", null);
		btn1.setMaxWidth(Double.MAX_VALUE);
		btn1.setOnAction(event->{
			//PanTTY.popup(dev);
		});

		final Button btn2 = PanBase.genButton2("Recv", null);
		btn2.setMaxWidth(Double.MAX_VALUE);
		btn2.setOnAction(event1->{
			txt1.clear();
		});
		
		final Button btn3 = PanBase.genButton2("Send", null);
		btn3.setMaxWidth(Double.MAX_VALUE);
		btn3.setOnAction(event->{
			byte[] buf = cook_hex_text(txt2.getText());
		});
		
		final Button btn4 = PanBase.genButton2("Read", null);
		btn4.setMaxWidth(Double.MAX_VALUE);
		btn4.setOnAction(event->{
			txt1.clear();
			String txt = "";
			byte[] buf = null;//dev.readBuff();
			if(buf==null){
				txt1.setText("No data!!");
				return;
			}
			for(int i=0; i<buf.length; i++){
				txt = txt + String.format("%02X ", ((int)buf[i] & 0xFF));
				if(i%4==0 && i!=0){
					txt = txt + "\n";
				}
			}
			txt1.setText(txt);
		});
		
		final Button btn5 = PanBase.genButton2("Writ", null);
		btn5.setMaxWidth(Double.MAX_VALUE);
		btn5.setOnAction(event->{
			byte[] buf = cook_hex_text(txt2.getText());
			//dev.writeByte(buf);
		});
		
		final HBox lay2 = new HBox();
		HBox.setHgrow(txt1, Priority.ALWAYS);
		HBox.setHgrow(txt2, Priority.ALWAYS);
		lay2.getStyleClass().add("hbox-small");
		lay2.getChildren().addAll(txt1, txt2);
		
		final VBox lay3 = new VBox();
		lay3.getStyleClass().add("vbox-small");
		lay3.getChildren().addAll(btn1, btn2, btn3, btn4, btn5);
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		//lay0.setLeft(layAdvn);
		lay0.setCenter(lay2);
		lay0.setRight(lay3);		
		return lay0;
	}

	private byte[] cook_hex_text(String txt){
		txt = txt.replace('\n', ' ');
		String[] val = txt.split("\\s");
		ArrayList<Byte> lst = new ArrayList<Byte>();
		for(int i=0; i<val.length; i++){
			try{
				int v = Integer.valueOf(val[i],16);
				if(val[i].length()>=3){					
					lst.add((byte)((v&0xFF00)>>8));
					lst.add((byte)((v&0x00FF)   ));
				}else if(val[i].length()==2){
					lst.add((byte)v);
				}				
			}catch(NumberFormatException e){
			}			
		}
		byte[] buf = new byte[lst.size()];
		for(int i=0; i<buf.length; i++){
			buf[i] = lst.get(i).byteValue();
		}
		return buf;
	}
	
	@Override
	public void eventShown(PanBase self) {
		//dev.link("\\\\.\\COM2,19200,8n1");
	}

	@Override
	public void eventClose(PanBase self) {
		//dev.unlink();
	}
}
