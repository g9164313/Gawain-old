package prj.scada;


import java.util.Optional;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.DevTTY;
import narl.itrc.Misc;

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
		looper_start();
		take(0,0,(act)->{
			int[] val = get_register(4,4);
			Application.invokeAndWait(()->{
				Ton_Pos.set(val[0]);
				ToffPos.set(val[1]);
				Ton_Neg.set(val[2]);
				ToffNeg.set(val[3]);
			});
		});
		take(-1,1000,(act)->{
			int[] sta = get_register(0,4);
			Application.invokeAndWait(()->{
				flgMod.set(sta[0]&0x07);
				flgMLX.set(((sta[0]&0x10)==0)?(false):(true));
				flgRun.set(((sta[1]&0x01)==0)?(false):(true));
				flgDC1.set(((sta[1]&0x40)==0)?(false):(true));
				flgDC2.set(((sta[1]&0x80)==0)?(false):(true));
				flgErr.set(sta[3]);
			});
		});
	}	
	//----------------------------------//
	
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
		int addr,
		int size,
		int off
	) {
		//RK512 header
		buf[0] = 0x00; 
		buf[1] = 0x00;
		buf[2] = (byte)tkn2;//0x45;//'E' 
		buf[3] = (byte)tkn3;//0x44;//'D'
		buf[4] = (byte)((addr&0xFF00)>>8);
		buf[5] = (byte)((addr&0x00FF));
		buf[6] = (byte)((size&0xFF00)>>8);
		buf[7] = (byte)((size&0x00FF));
		buf[8] = (byte)(0xFF);
		buf[9] = (byte)(0xFF);
		//3964R tail
		off += 10;						
		buf[off+0] = DLE;
		buf[off+1] = ETX;
		buf[off+2] = (byte)checksum(buf,0,off+2);
		return buf;
	}
	
	private int[] get_register(
		final int addr, 
		final int size
	) {
		//trick!! read byte one by one!!
		byte[] ans = new byte[4+6+3+size*2];
		//step.1 - give start code.
		writeByte(STX);
		//step.2 - wait acknowledge
		if(wait_code(DLE)==false) {
			Misc.logw("[%s] no start code", TAG);
			return new int[size];
		}
		//step.3 - give device RK512 header.
		writeByte(pack(ans,'E','D',addr,size,0));
		//step.4 - wait for receiver
		ans[0] = readByte();//DLE or %%
		ans[1] = readByte();//STX
		//step.5 - give echo
		writeByte(DLE);
		//step.6 - get response, token(3byte), error-code
		ans[0] = readByte();
		ans[1] = readByte();
		ans[2] = readByte();
		ans[3] = readByte();
		int off = 0;
		if(ans[3]==0x00) {
			//try to get data!!!
			for(; off<size*2; off++) {
				ans[off]  = readByte();
			}				
		}else {
			Misc.logw("[%s] token has error(%d)", TAG, ans[3]);
		}
		ans[off+0] = readByte();//DLE
		ans[off+1] = readByte();//ETX
		ans[off+2] = readByte();//CRC
		//step.7 - end of communication
		writeByte(DLE);
		//check CRC and give response to UI
		int crc = checksum(ans,0,off+2);
		if(crc!=ans[off+2]) {
			Misc.logv("invalid CRC");
			//return null;
		}
		int[] val = new int[size];
		for(int i=0; i<size; i++){
			int aa = (int)(ans[i*2+0]);
			int bb = (int)(ans[i*2+1]);
			aa = (aa<<8) & 0xFF00;
			bb = (bb   ) & 0x00FF;
			val[i] = aa | bb;
		}
		return val;
	}
	
	private void set_register(
		final int addr, 
		final int... vals
	) {
		int size = vals.length;
		//trick!! read byte one by one!!
		//it contain RK512, Data, 3964R
		byte[] ans = new byte[10+size*2+3];
		//step.1 - write start code
		writeByte(STX);
		//step.2 - wait response
		if(wait_code(DLE)==false) {
			Misc.logw("[%s] no start code", TAG);
			return;
		}
		//step.3 - write data
		for(int i=0; i<size; i++) {
			ans[10+i*2+0] = (byte)((vals[i] & 0xFF00)>>8);
			ans[10+i*2+1] = (byte)((vals[i] & 0x00FF)   );
		}
		writeByte(pack(ans,'A','D', addr, size, size*2));
		//step.4 - wait for receiver
		ans[0] = readByte();//DLE or %%
		ans[1] = readByte();//STX
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
		return;
	}

	private interface GetValues {
		void getValues(int[] val);
	}
	
	public void getRegister(
		final int addr, 
		final int size,
		final GetValues callback
	) {
		if(action.size()>=1) {
			return;
		}
		take(0,0,(act)->{
			int[] val = get_register(addr,size);
			if(callback!=null) {
				callback.getValues(val);
			}
		});
	}
	
	public void setRegister(
		final int addr, 
		final int... vals
	) {
		if(action.size()>=1) {
			return;
		}
		take(0,0,(act)->set_register(addr,vals));
	}
	//---------------------------------//
	
	public final IntegerProperty flgMod = new SimpleIntegerProperty();
	public final BooleanProperty flgMLX = new SimpleBooleanProperty(false);

	public final IntegerProperty flgErr = new SimpleIntegerProperty();	
	public final BooleanProperty flgRun = new SimpleBooleanProperty(false);
	public final BooleanProperty flgDC1 = new SimpleBooleanProperty(false);
	public final BooleanProperty flgDC2 = new SimpleBooleanProperty(false);
	
	public final IntegerProperty Ton_Pos = new SimpleIntegerProperty();
	public final IntegerProperty Ton_Neg = new SimpleIntegerProperty();
	public final IntegerProperty ToffPos = new SimpleIntegerProperty();
	public final IntegerProperty ToffNeg = new SimpleIntegerProperty();
	
	public static Pane genPanel(final DevSPIK2000 dev) {
		
		final Alert altFormat = new Alert(AlertType.ERROR);
		altFormat.setTitle("錯誤！！");
		altFormat.setHeaderText("錯誤的資料格式");
		
		final TextInputDialog diaTime = new TextInputDialog();
		
		final JFXComboBox<String> cmbMode = new JFXComboBox<String>();
		cmbMode.getItems().addAll(
			"Bipolar",
			"Unipolar－","Unipolar＋",
			"DC－", "DC＋"
		);
		cmbMode.getSelectionModel().select(0);
		cmbMode.setOnAction(e->{
			switch(cmbMode.getSelectionModel().getSelectedIndex()) {
			case 0: dev.setRegister(0,1); break;
			case 1: dev.setRegister(0,2); break;
			case 2: dev.setRegister(0,3); break;
			case 3: dev.setRegister(0,4); break;
			case 4: dev.setRegister(0,5); break;
			}
		});
		
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
					int val = Integer.valueOf(res.get());
					dev.setRegister(4, val);
					dev.Ton_Pos.setValue(val);
				}catch(NumberFormatException exp) {
					altFormat.showAndWait();
				}
			}
		});
		txt[1].textProperty().bind(dev.Ton_Pos.asString("%d us"));
		
		txt[2].setText("\u22c4Toff＋：");
		txt[2].setOnMouseClicked(e->{
			diaTime.setTitle("設定 Toff＋");
			diaTime.setContentText("時間(us)");
			Optional<String> res = diaTime.showAndWait();
			if(res.isPresent()) {
				try {
					int val = Integer.valueOf(res.get());
					dev.setRegister(5,val);
					dev.ToffPos.set(val);
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
					int val = Integer.valueOf(res.get());
					dev.setRegister(6, val);
					dev.Ton_Neg.set(val);
				}catch(NumberFormatException exp) {
					altFormat.showAndWait();
				}
			}
		});
		txt[5].textProperty().bind(dev.Ton_Neg.asString("%d us"));
		
		txt[6].setText("\u22c4Toff－：");
		txt[6].setOnMouseClicked(e->{
			diaTime.setTitle("設定 Toff－");
			diaTime.setContentText("時間(us)");
			Optional<String> res = diaTime.showAndWait();
			if(res.isPresent()) {
				try {
					int val = Integer.valueOf(res.get());
					dev.setRegister(7, val);
					dev.ToffNeg.set(val);
				}catch(NumberFormatException exp) {
					altFormat.showAndWait();
				}
			}
		});
		txt[7].textProperty().bind(dev.ToffNeg.asString("%d us"));
		
		final ImageView[] img = {
			Misc.getIconView("check.png"),
			Misc.getIconView("check.png"),
			Misc.getIconView("check.png")
		};
		img[0].visibleProperty().bind(dev.flgRun);
		img[1].visibleProperty().bind(dev.flgDC1);
		img[2].visibleProperty().bind(dev.flgDC2);
		
		final ToggleButton[] tgl = {
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton()
		};
		tgl[0].setText("DC-1");
		tgl[0].setOnAction(e->{			
			if(tgl[0].isSelected()==true) {
				dev.setRegister(1, 0x21);//DC-1 on
			}else {
				dev.setRegister(1, 0x20);//DC-1 off
			}
		});
		tgl[1].setText("DC-2");
		tgl[1].setOnAction(e->{
			if(tgl[1].isSelected()==true) {
				dev.setRegister(1, 0x23);//DC-2 on
			}else {
				dev.setRegister(1, 0x22);//DC-2 on
			}
		});
		tgl[2].setText("Run");
		tgl[2].setOnAction(e->{
			if(tgl[2].isSelected()==true) {
				dev.setRegister(1, 0x02);//running-on
			}else {
				dev.setRegister(1, 0x01);//running-off
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
		lay.add(cmbMode, 0, 0, 2, 1);
		lay.addRow(1, txt[0], txt[1]);
		lay.addRow(2, txt[4], txt[5]);
		lay.addRow(3, txt[2], txt[3]);
		lay.addRow(4, txt[6], txt[7]);
		lay.addRow(5, tgl[0], img[0]);
		lay.addRow(6, tgl[1], img[1]);
		lay.addRow(7, tgl[2], img[2]);
		//lay.add(btn, 0, 7, 4,1);
		return lay;
	}
}
