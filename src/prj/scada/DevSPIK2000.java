package prj.scada;


import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXToggleButton;

import javafx.beans.property.BooleanProperty;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import narl.itrc.DevBase;
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
	}
	public DevSPIK2000(String path_name){
		this();
		setPathName(path_name);
	}
	@Override
	protected void doLoop(DevBase dev) {
		//do nothing
	}
	//----------------------------------//
	
	private static final byte STX = 0x02;
	private static final byte ETX = 0x03;
	private static final byte DLE = 0x10;//跳出資料通訊
	
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
		buf[2] = (byte)tkn2; 
		buf[3] = (byte)tkn3;
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
		int[] val = new int[size];
		int cnt = size * 2;
		byte[] ans;
		if(cnt>=6) {
			ans = new byte[4+cnt+3];
		}else {
			ans = new byte[4+6+3];
		}
		//step.1 - give start code and waiting.
		writeByte(STX);
		while(readByte()!=DLE) {
			Misc.delay(100);
		}
		//step.2 - give device RK512 header.
		pack(ans,'E','D',addr,size,0);
		writeByte(ans,0,13);
		//step.3 - wait for receiver
		readBytePurge(ans,0,2);//DLE,STX
		if(ans[0]!=DLE) {
			return val;
		}
		//step.4 - give echo
		writeByte(DLE);
		//step.5 - get frame:
		//	token(3byte), error-code(), 
		//	data package(size*2),
		//	DLE, ETX, CRC
		readBytePurge(ans,0,(4+cnt+3));
		//collect data and value~~~
		for(int i=0; i<size; i++){
			int aa = (int)(ans[4+i*2+0]);
			int bb = (int)(ans[4+i*2+1]);
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
		//prepare RK512, Data
		byte[] ans = new byte[10+size*2+3];
		for(int i=0; i<size; i++) {
			ans[10+i*2+0] = (byte)((vals[i] & 0xFF00)>>8);
			ans[10+i*2+1] = (byte)((vals[i] & 0x00FF)   );
		}
		pack(ans,'A','D',addr,size,size*2);
		//step.1 - give start code and waiting.
		writeByte(STX);
		while(readByte()!=DLE) {
			Misc.delay(10);
		}
		//step.2 - give device RK512 header.
		writeByte(ans,0,ans.length);
		//step.3 - wait for receiver
		readBytePurge(ans,0,2);//DLE,STX
		if(ans[0]!=DLE) {
			return;
		}
		//step.4 - give echo
		writeByte(DLE);
		readBytePurge(ans,0,7);
	}

	private interface GetValues {
		void getValues(int[] val);
	}
	
	public void getRegister(
		final int addr, 
		final int size,
		final GetValues callback
	) {
		if(action.size()>=1) {return;}
		doing(0,(act)->{
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
		if(action.size()>=1) {return;}
		doing(0,(act)->set_register(addr,vals));
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
		
//		final Alert altFormat = new Alert(AlertType.ERROR);
//		altFormat.setTitle("錯誤！！");
//		altFormat.setHeaderText("錯誤的資料格式");
		
//		final TextInputDialog diaTime = new TextInputDialog();
		
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
		/*txt[0].setText("\u22c4Ton＋：");
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
		});*/
		final JFXButton btn1 = new JFXButton("ggyy");
		btn1.setGraphic(Misc.getIconView("pen.png"));
		btn1.setMaxWidth(Double.MAX_VALUE);

		txt[2].setText("\u22c4Toff＋：");
		txt[2].setOnMouseClicked(e->{
			//diaTime.setTitle("設定 Toff＋");
			//diaTime.setContentText("時間(us)");
			//Optional<String> res = diaTime.showAndWait();
			//if(res.isPresent()) {
			//	try {
			//		int val = Integer.valueOf(res.get());
			//		dev.setRegister(5,val);
			//		dev.ToffPos.set(val);
			//	}catch(NumberFormatException exp) {
					//altFormat.showAndWait();
			//	}
			//}
		});

		txt[4].setText("\u22c4Ton－：");
		txt[4].setOnMouseClicked(e->{
		});
		
		txt[6].setText("\u22c4Toff－：");
		txt[6].setOnMouseClicked(e->{
		});
		
		
		final ImageView[] img = {
			Misc.getIconView("check.png"),
			Misc.getIconView("check.png"),
			Misc.getIconView("check.png")
		};

		final ToggleButton[] tgl = {
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton()
		};
		tgl[0].setText("DC-1");
		tgl[0].setOnAction(e->{			
			if(tgl[0].isSelected()==true) {
				dev.setRegister(1,0x21);//DC-1 on
			}else {
				dev.setRegister(1,0x20);//DC-1 off
			}
		});
		tgl[1].setText("DC-2");
		tgl[1].setOnAction(e->{
			if(tgl[1].isSelected()==true) {
				dev.setRegister(1,0x22);//DC-2 on
			}else {
				dev.setRegister(1,0x23);//DC-2 off
			}
		});
		tgl[2].setText("Run");
		tgl[2].setOnAction(e->{
			if(tgl[2].isSelected()==true) {
				dev.setRegister(1,2);//running-on
			}else {
				dev.setRegister(1,1);//running-off
			}
		});
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.add(cmbMode, 0, 0, 2, 1);
		lay.addRow(1, btn1, txt[1]);
		lay.addRow(2, txt[4], txt[5]);
		lay.addRow(3, txt[2], txt[3]);
		lay.addRow(4, txt[6], txt[7]);
		lay.addRow(5, tgl[0], img[0]);
		lay.addRow(6, tgl[1], img[1]);
		lay.addRow(7, tgl[2], img[2]);
		//lay.add(btn1, 0, 8, 2, 1);
		//lay.add(btn2, 2, 8, 2, 1);
		
		/*dev.doing(500,(act)->{
			int[] val = dev.get_register(0,8);
			//Misc.logv("^[%d, %d, %d, %d]", val[4], val[5], val[6], val[7]);
			Application.invokeAndWait(()->{
				dev.flgMod.set(val[0]&0x07);
				dev.flgMLX.set(((val[0]&0x10)==0)?(false):(true));
				dev.flgRun.set(((val[1]&0x01)==0)?(false):(true));
				dev.flgDC1.set(((val[1]&0x40)==0)?(false):(true));
				dev.flgDC2.set(((val[1]&0x80)==0)?(false):(true));
				dev.flgErr.set(val[3]);
				dev.Ton_Pos.set(val[4]);
				dev.ToffPos.set(val[5]);
				dev.Ton_Neg.set(val[6]);
				dev.ToffNeg.set(val[7]);
			});
		});*/
		return lay;
	}
}
