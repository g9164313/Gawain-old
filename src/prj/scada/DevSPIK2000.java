package prj.scada;

import java.util.Arrays;
import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PanTTY;

/**
 * SPIK2000 is a high voltage pulse generator.<p>
 * This code is for device communication.<p>
 * Although manual say RS-232 protocol is RK512, it not real.<p>
 * I guess someone change the internal controller.<p>
 * Reference Document is "s7300_cp341_manual.pdf".<p>
 * @author qq
 *
 */
public class DevSPIK2000 extends DevTTY {

	public DevSPIK2000() {
		TAG = "DevSPIK2000-stream";
		asynMode = false;
	}
	
	public DevSPIK2000(String path_name){
		this();
		setPathName(path_name);
	}
	
	@Override
	protected void afterOpen() {
		getRegister(4,4,(val)->{
			TonPos.set(val[0]);
			ToffPos.set(val[1]);
			TonNeg.set(val[2]);
			ToffNeg.set(val[3]);
		});
	}
	
	private static final byte STX = 0x02;
	private static final byte DLE = 0x10;
	private static final byte ETX = 0x03;

	private boolean wait_code(byte code) {
		int max_try = 100;
		do{
			if(readByte()==code) {
				return true;
			}
			Misc.delay(5);
			max_try-=1;
		}while(isLive()==true && max_try>0);
		return false;
	}
	
	private int checksum(
		final byte[] data, 
		final int start, 
		final int length
	){
		int bcc = 0x00;		
		for(int i=start; i<length; i++){
			bcc = bcc ^ ((int)data[i] & 0xFF);
		}
		return bcc;
	}
	
	private byte[] pack(
		final byte[] buf,
		final char tkn2, 
		final char tkn3,
		int off
	) {
		//RK512 header
		buf[0] = 0x00; 
		buf[1] = 0x00;
		buf[2] = (byte)tkn2;//0x45;//'E' 
		buf[3] = (byte)tkn3;//0x44;//'D'
		buf[4] = (byte)((tskAddr&0xFF00)>>8);
		buf[5] = (byte)((tskAddr&0x00FF));
		buf[6] = (byte)((tskSize&0xFF00)>>8);
		buf[7] = (byte)((tskSize&0x00FF));
		buf[8] = (byte)(0xFF);
		buf[9] = (byte)(0xFF);
		//3964R tail
		off += 10;						
		buf[off+0] = DLE;
		buf[off+1] = ETX;
		buf[off+2] = (byte)checksum(buf,0,off+2);
		return buf;
	}
	
	private Runnable runRead = new Runnable() {
		@Override
		public void run() {
			//trick!! read byte one by one!!
			byte[] ans = new byte[4+tskSize*2+3];
			//step.1 - give start code.
			writeByte(STX);
			//step.2 - wait acknowledge
			if(wait_code(DLE)==false) {
				Misc.logw("[%s] no start code", TAG);
				return;
			}
			//step.3 - give device RK512 header.
			writeByte(pack(ans,'E','D',0));
			//step.4 - wait for receiver
			ans[0] = readByte();//DLE or %%
			ans[1] = readByte();//STX
			//if(ans[0]!=DLE || ans[1]!=STX) {
			//	readBuff(ans,-1);//clear stream, trick!!				
			//	Misc.logw("[%s] invalid answer", TAG);
			//	return;
			//}
			//step.5 - give echo
			writeByte(DLE);
			//step.6 - get response, token(3byte), error-code
			ans[0] = readByte();
			ans[1] = readByte();
			ans[2] = readByte();
			ans[3] = readByte();
			if(ans[3]==0x00) {
				//try to get data!!!
				for(int i=0; i<tskSize*2; i++) {
					ans[4+i]  = readByte();
				}				
			}else {
				Misc.logw("[%s] token has error code(%d)", TAG, ans[3]);
			}
			int off = ans.length-3;
			ans[off+0] = readByte();//DLE
			ans[off+1] = readByte();//ETX
			ans[off+2] = readByte();//CRC
			//step.7 - end of communication
			writeByte(DLE);
			//check CRC and give response to UI
			int crc = checksum(ans,0,ans.length-1);
			if(crc!=ans[off+2]) {
				Misc.logv("invalid CRC");
				return;
			}
			//invoke callback
			if(hook==null) {
				return;
			}
			args = new int[tskSize];
			for(int i=0; i<tskSize; i++){
				int aa = (int)(ans[4+i*2+0]);
				int bb = (int)(ans[4+i*2+1]);
				aa = (aa<<8) & 0xFF00;
				bb = (bb   ) & 0x00FF;
				args[i] = aa | bb;
			}
			Application.invokeAndWait(()->hook.readBack(args));
			hook = null;//reset it again
		}
	};
	
	private Runnable runWrite = new Runnable() {
		@Override
		public void run() {
			//trick!! read byte one by one!!
			//it contain RK512, Data, 3964R
			byte[] ans = new byte[10+tskSize*2+3];
			//step.1 - write start code
			writeByte(STX);
			//step.2 - wait response
			if(wait_code(DLE)==false) {
				Misc.logw("[%s] no start code", TAG);
				return;
			}
			//step.3 - write data
			for(int i=0; i<tskSize; i++) {
				ans[10+i*2+0] = (byte)((args[i] & 0xFF00)>>8);
				ans[10+i*2+1] = (byte)((args[i] & 0x00FF)   );
			}
			pack(ans,'A','D',tskSize*2);
			writeByte(ans);
			//step.4 - wait for receiver
			ans[0] = readByte();//DLE or %%
			ans[1] = readByte();//STX
			//if(ans[0]!=DLE || ans[1]!=STX) {				
				//Misc.logw("[%s] invalid answer[%x,%x]", TAG, ans[0], ans[1]);
				//return;
			//}
			//step.5 - give echo
			writeByte(DLE);
			//step.6 - get response
			ans[0] = readByte();//token
			ans[1] = readByte();
			ans[2] = readByte();
			ans[3] = readByte();//error-code
			ans[4] = readByte();//DLE
			ans[5] = readByte();//ETX
			ans[6] = readByte();//CRC
			//step.7 - end of communication
			writeByte(DLE);
		}
	};
		
	private Thread tsk = null;
	
	private int tskAddr, tskSize;
	
	private int[] args;//keep writing and reading data~~~
	
	private interface ReadBack {
		void readBack(int[] args);
	};
	private ReadBack hook = null;
	
	private void do_task(
		final int addr, 
		final int size,
		final Runnable runTask
	) {
		if(tsk!=null) {
			if(tsk.isAlive()==true) {
				return;
			}
		}
		tskAddr = addr;
		tskSize = size;
		tsk = new Thread(runTask,TAG);
		tsk.setDaemon(true);
		tsk.start();
	} 
	
	public void getRegister(
		final int addr, 
		final int size,
		final ReadBack callback
	) {
		hook = callback;
		do_task(addr,size,runRead);
	}
	
	public void setRegister(
		final int addr, 
		final int... values
	) {
		args = values;
		do_task(addr,values.length,runWrite);
	}
	//---------------------------------//
	
	public final IntegerProperty TonPos = new SimpleIntegerProperty();
	public final IntegerProperty TonNeg = new SimpleIntegerProperty();
	public final IntegerProperty ToffPos = new SimpleIntegerProperty();
	public final IntegerProperty ToffNeg = new SimpleIntegerProperty();
	
	public static Pane genPanel(final DevSPIK2000 dev) {
		
		final Alert altFormat = new Alert(AlertType.ERROR);
		altFormat.setTitle("錯誤！！");
		altFormat.setHeaderText("錯誤的資料格式");
		
		final TextInputDialog diaTime = new TextInputDialog();
		
		final Label[] txt = new Label[8];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
			txt[i].setMaxWidth(Double.MAX_VALUE);	
			GridPane.setFillWidth(txt[i], true);
		}
		txt[0].setText("\u22c4Ton＋：");
		txt[0].setOnMouseClicked(e->{
			diaTime.setTitle("設定 Ton＋");
			diaTime.setContentText("時間(us)");
			Optional<String> res = diaTime.showAndWait();
			if(res.isPresent()) {
				try {
					dev.setRegister(4, Integer.valueOf(res.get()));
				}catch(NumberFormatException exp) {
					altFormat.showAndWait();
				}
			}
		});
		txt[1].textProperty().bind(dev.TonPos.asString("%d us"));
		
		txt[2].setText("\u22c4Toff＋：");
		txt[2].setOnMouseClicked(e->{
			diaTime.setTitle("設定 Toff＋");
			diaTime.setContentText("時間(us)");
			Optional<String> res = diaTime.showAndWait();
			if(res.isPresent()) {
				try {
					dev.setRegister(5, Integer.valueOf(res.get()));
				}catch(NumberFormatException exp) {
					altFormat.showAndWait();
				}
			}
		});
		txt[3].textProperty().bind(dev.ToffPos.asString("%d us"));
		
		txt[4].setText("\u22c4Ton－：");
		txt[4].setOnMouseClicked(e->{
			diaTime.setTitle("設定 Ton－");
			diaTime.setContentText("時間(us)");
			Optional<String> res = diaTime.showAndWait();
			if(res.isPresent()) {
				try {
					dev.setRegister(6, Integer.valueOf(res.get()));
				}catch(NumberFormatException exp) {
					altFormat.showAndWait();
				}
			}
		});
		txt[5].textProperty().bind(dev.TonNeg.asString("%d us"));
		
		txt[6].setText("\u22c4Toff－：");
		txt[6].setOnMouseClicked(e->{
			diaTime.setTitle("設定 Toff－");
			diaTime.setContentText("時間(us)");
			Optional<String> res = diaTime.showAndWait();
			if(res.isPresent()) {
				try {
					dev.setRegister(7, Integer.valueOf(res.get()));
				}catch(NumberFormatException exp) {
					altFormat.showAndWait();
				}
			}
		});
		txt[7].textProperty().bind(dev.ToffNeg.asString("%d us"));
		
		final JFXToggleButton[] tgl = new JFXToggleButton[3];
		for(int i=0; i<tgl.length; i++) {
			tgl[i] = new JFXToggleButton();
			tgl[i].setMaxWidth(Double.MAX_VALUE);
			GridPane.setFillWidth(tgl[i], true);
		}
		tgl[0].setText("Run");
		tgl[0].setOnAction(e->{
			if(tgl[0].isSelected()==true) {
				dev.setRegister(1, 0x02);//running-on
			}else {
				dev.setRegister(1, 0x01);//running-off
			}
		});
		tgl[1].setText("DC-1");
		tgl[1].setOnAction(e->{
			if(tgl[1].isSelected()==true) {
				dev.setRegister(1, 0x21);//DC-1 on
			}else {
				dev.setRegister(1, 0x20);//DC-1 off
			}
		});
		tgl[2].setText("DC-2");
		tgl[2].setOnAction(e->{
			if(tgl[2].isSelected()==true) {
				dev.setRegister(1, 0x23);//DC-1 on
			}else {
				dev.setRegister(1, 0x22);//DC-1 on
			}
		});
		
		final Button btn = new Button("test");
		btn.setOnAction(e->{
			int val = (int)(Math.random()*100.);
			Misc.logv("test=%d",val);
			dev.setRegister(7, val);
			/*dev.getRegister(4, 4, (val)->{
				for(int i=0; i<val.length; i++) {
					Misc.logv("val=%d", val[i]);
				}
			});*/
		});
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("ground-pad");
		lay.addRow(0, txt[0], txt[1]);
		lay.addRow(1, txt[4], txt[5]);
		lay.addRow(2, txt[2], txt[3]);
		lay.addRow(3, txt[6], txt[7]);
		lay.add(tgl[0], 0, 4, 4, 1);
		lay.add(tgl[1], 0, 5, 4, 1);
		lay.add(tgl[2], 0, 6, 4, 1);
		//lay.add(btn, 0, 7, 4,1);
		return lay;
	}
}
