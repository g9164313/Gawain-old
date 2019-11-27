package prj.scada;


import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

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
		TAG = "SPIK-2000";
		flowControl = 0x10;//disable all controls
		readTimeout = 1000;
	}
	public DevSPIK2000(String path_name){
		this();
		setPathName(path_name);
	}
	@Override
	protected void afterOpen() {
		setupState0("init", ()->{
			fst_value1 = get_register( 0,8);
			fst_value2 = get_register(26,6);
			nextState.set(null);
			Application.invokeAndWait(()->{
				if(fst_value1==null) {
					return;
				}
				switch(fst_value1[0] & 0x7) {
				case 1: tmpRad[0].setSelected(true); break;
				case 2: tmpRad[1].setSelected(true); break;
				case 3: tmpRad[2].setSelected(true); break;
				case 4: tmpRad[3].setSelected(true); break;
				case 5: tmpRad[4].setSelected(true); break;
				}
				tmpTxt[0].setText(String.format("Ton +: %4d", fst_value1[4]));
				tmpTxt[1].setText(String.format("Toff+: %4d", fst_value1[5]));
				tmpTxt[2].setText(String.format("Ton -: %4d", fst_value1[6]));
				tmpTxt[3].setText(String.format("Toff-: %4d", fst_value1[7]));
			});
		});
		playFlow();
	}
	//----------------------------------//
	
	private static final byte STX = 0x02;
	private static final byte ETX = 0x03;
	private static final byte DLE = 0x10;//跳出資料通訊
	//private static final byte X25 = 0x25;//unknown token
	
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
	
	private boolean notify_device() {
		writeByte(STX);
		byte cc = 0;
		do{
			Misc.delay(10);
			cc = readByte();
			if(cc==DLE) {
				return true;
			}
		}while(isLive()==true);
		return false;
	}
	
	private boolean waiting_device() {
		byte[] res = {0,0};
		readByte(res,0,2);
		if(res[0]==DLE && res[1]==STX) {
			writeByte(DLE);
			return true;
		}
		Misc.loge("[SPIK-WAIT] %02X %02X", res[0], res[1]);
		return false;
	}
	
	private int[] get_register(
		final int addr, 
		final int size
	) {
		//trick!! read byte one by one!!
		int cnt = size * 2;
		byte[] ans;
		if(cnt>=6) {
			ans = new byte[4+cnt+3];
		}else {
			ans = new byte[4+6+3];
		}
		//step.1 - give start code and waiting.
		if(notify_device()==false) {
			return null;
		}
		//step.2 - give device RK512 header.
		pack(ans,'E','D',addr,size,0);
		writeByte(ans,0,13);
		//step.3 - wait for receiver
		if(waiting_device()==false) {
			return null;
		}	
		//step.4 - get frame:
		//	token(3byte), error-code(), 
		//	data package(size*2),
		//	DLE, ETX, CRC
		readByte(ans,0,(3+1+cnt+3));		
		writeByte(DLE);
		//end of talking~~~~
		if(	ans[3]!=0||
			ans[3+1+cnt+0]!=DLE ||
			ans[3+1+cnt+1]!=ETX 
		) {
			return null;
		}
		//collect data and value~~~
		int[] val = new int[size];
		for(int i=0; i<size; i++){
			int aa = (int)(ans[4+i*2+0]);
			int bb = (int)(ans[4+i*2+1]);
			aa = (aa<<8) & 0xFF00;
			bb = (bb   ) & 0x00FF;
			val[i] = aa | bb;
		}
		return val;
	}
	
	private boolean set_register(
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
		if(notify_device()==false) {
			return false;
		}
		//step.2 - give device RK512 header.
		writeByte(ans,0,ans.length);
		//step.3 - wait for receiver
		if(waiting_device()==false) {
			return false;
		}
		//step.4 - give echo
		size = readByte(ans,0,7);
		writeByte(DLE);
		//end of talking~~~~		
		if(	size==7 &&
			ans[4]==DLE &&
			ans[5]==ETX 
		) {
			return true;
		}
		Misc.loge(
			"[SPIK-WRITE-ECHO]: %02X %02X %02X %02X %02X %02X %02X",
			ans[0],ans[1],ans[2],ans[3],ans[4],ans[5],ans[6]
		);
		return false;
	}

	private interface Callback {
		void call(int[] val);
	}
	
	public void getRegister(
		final Callback event,
		final int addr, 
		final int size		
	) {interrupt(()->{
		int[] val = get_register(addr,size);
		if(Application.isEventThread()==false) {
			return;
		}
		Application.invokeAndWait(()->{
			if(val==null) {
				show_fail(addr,size);
			}else if(event!=null) {
				event.call(val);
			}
		});
	});}
	
	public void setRegister(
		final Callback event,
		final int addr,
		final int... vals
	) {interrupt(()->{		
		boolean flag = set_register(addr,vals);
		if(event==null) {
			return;
		}
		Application.invokeAndWait(()->{
			if(flag==false) {
				show_fail(addr,vals[0]);
				return;
			}
			if(event!=null) {
				event.call(vals);
			}
		});		
	});}
	//---------------------------------//
	
	private int[] fst_value1 = null;//Mode
	@SuppressWarnings("unused")
	private int[] fst_value2 = null;
	
	public final IntegerProperty ARC_count = new SimpleIntegerProperty();//1-10000
	public final IntegerProperty DC1_V_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC1_I_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC1_P_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_V_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_I_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_P_Act = new SimpleIntegerProperty();//0-4000
	
	private void show_fail(final int addr, final int value) {
		final Alert diag = new Alert(AlertType.ERROR);
		diag.setTitle("錯誤！！");
		diag.setHeaderText(String.format(
			"存取失敗: Addr:%2d, Value:%2d",
			addr,value
		));
		diag.showAndWait();
	}	
	
	private static void set_txt_value(
		final DevSPIK2000 dev, 
		final Label txt, 
		final int addr
	) {
		final int _p = txt.getText().indexOf(':');
		final String pref = txt
			.getText()
			.substring(0,_p);
		
		final TextInputDialog diag = new TextInputDialog();
		diag.setTitle("設定 "+pref);
		diag.setContentText("時間(us)");
		Optional<String> res = diag.showAndWait();
		if(res.isPresent()==false) {
			return;
		}
		try {
			int val = Integer.valueOf(res.get());
			dev.setRegister(_v->{
				txt.setText(String.format("%s: %4d", pref, val));
			}, addr, val);			
		}catch(NumberFormatException exp) {			
		}
	}
	
	private static JFXRadioButton[] tmpRad = null;
	private static Label[] tmpTxt = null;
	
	public static Pane genPanel(final DevSPIK2000 dev) {
		
		final ToggleGroup grp = new ToggleGroup();
		
		final JFXRadioButton[] rad = {
			new JFXRadioButton ("Bipolar"),
			new JFXRadioButton ("Uni-"),
			new JFXRadioButton ("Uni+"),
			new JFXRadioButton ("DC-"),
			new JFXRadioButton ("DC+"),
		};
		for(int i=0; i<rad.length; i++) {
			rad[i].setToggleGroup(grp);
			rad[i].setUserData(i+1);
			rad[i].setOnAction(e->{
				dev.setRegister(
					_v->{},
					0,
					(int)grp.getSelectedToggle().getUserData()
				);
			});
		}
		tmpRad = rad;
		
		final JFXButton[] btn = new JFXButton[6];
		for(int i=0; i<btn.length; i++) {
			btn[i] = new JFXButton();
			btn[i].setMaxWidth(Double.MAX_VALUE);
			if(i%2==0) {
				btn[i].getStyleClass().add("btn-raised-1");
				btn[i].setText("ON ");
			}else {
				btn[i].getStyleClass().add("btn-raised-3");
				btn[i].setText("OFF");
			}
			GridPane.setHgrow(btn[i], Priority.ALWAYS);
		}
		btn[0].setOnAction(e->dev.setRegister(_v->{},1,0x21));//DC-1 on
		btn[1].setOnAction(e->dev.setRegister(_v->{},1,0x20));//DC-1 off
		btn[2].setOnAction(e->dev.setRegister(_v->{},1,0x22));//DC-2 on
		btn[3].setOnAction(e->dev.setRegister(_v->{},1,0x23));//DC-2 off
		btn[4].setOnAction(e->dev.setRegister(_v->{},1,0x02));//RUN on
		btn[5].setOnAction(e->dev.setRegister(_v->{},1,0x01));//RUB off
		
		final Label[] txt = new Label[4];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
			txt[i].setMaxWidth(Double.MAX_VALUE);			
			GridPane.setHgrow(txt[i], Priority.ALWAYS);
		}
		txt[0].setText("Ton +: ");
		txt[0].setOnMouseClicked(e->set_txt_value(dev,txt[0],4));
		txt[1].setText("Toff+: ");
		txt[1].setOnMouseClicked(e->set_txt_value(dev,txt[1],5));
		txt[2].setText("Ton -: ");
		txt[2].setOnMouseClicked(e->set_txt_value(dev,txt[2],6));
		txt[3].setText("Toff-: ");
		txt[3].setOnMouseClicked(e->set_txt_value(dev,txt[3],7));
		tmpTxt = txt;

		//final AnimationTimer wait_device = new AnimationTimer() {
		//};
		//-------------------------------------//
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad-inner");
		lay1.addRow(0, new Label("DC-1"),btn[0], btn[1]);
		lay1.addRow(1, new Label("DC-2"),btn[2], btn[3]);
		lay1.addRow(2, new Label("RUN"), btn[4], btn[5]);

		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad");
		lay0.add(new Label("操作模式"), 0, 0, 4, 1);
		lay0.addRow(1, rad[0], rad[3]);
		lay0.addRow(2, rad[1], rad[4]);
		lay0.addRow(3, rad[2]);
		lay0.add(new Separator(), 0, 4, 4, 1);
		lay0.add(txt[ 0], 0, 5, 4, 1);
		lay0.add(txt[ 1], 0, 6, 4, 1);
		lay0.add(txt[ 2], 0, 7, 4, 1);
		lay0.add(txt[ 3], 0, 8, 4, 1);		
		lay0.add(new Separator(), 0, 9, 4, 1);
		lay0.add(lay1, 0,10, 4, 3);
		return lay0;
	}
}
