package prj.sputter;


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

/**
 * SPIK2000 is a high voltage pulse generator.<p>
 * This code is for device communication.<p>
 * Although manual say RS-232 protocol is RK512, it not real.<p>
 * I guess someone change the internal controller.<p>
 * Reference Document is "s7300_cp341_manual.pdf".<p>
 * @author qq
 *
 */
@SuppressWarnings("restriction")
public class DevSPIK2k extends DevTTY {

	public DevSPIK2k() {
		TAG = "SPIK-2000";
		flowControl = 0x10;//disable all controls
		readTimeout = 500;
	}
	public DevSPIK2k(String path_name){
		this();
		setPathName(path_name);
	}	
	@Override
	protected void afterOpen() {
		addState("init", ()->{
			fst_value1 = get_register(0,8);
			//0* Mode: 
			//1* State: 
			//3* Error: 
			//4* Ton +: 2-32000us, duration of the pulse
			//5* Toff+: 2-32000us, duration of the pause
			//6* Ton -: 2-32000us, duration of the pulse
			//7* Toff-: 2-32000us, duration of the pause			
			nextState("loop");			
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
		addState("loop", ()->{
			//19* ARC_Count: Amount of ARC impulses
			//20* DC1_V_Act: measurement of DC-1 source
			//21* DC1_I_Act: measurement of DC-1 source
			//22* DC1_P_Act: measurement of DC-1 source
			//23* DC2_V_Act: measurement of DC-2 source
			//24* DC2_I_Act: measurement of DC-2 source
			//25* DC2_P_Act: measurement of DC-2 source
			final int[] reg = get_register(19,7);			
			Application.invokeLater(()->{
				ARC_count.set(reg[0]);
				DC1_V_Act.set(reg[1]);
				DC1_I_Act.set(reg[2]);
				DC1_P_Act.set(reg[3]);
				DC2_V_Act.set(reg[4]);
				DC2_I_Act.set(reg[5]);
				DC2_P_Act.set(reg[6]);
			});			
			sleep(500);
		});
		playFlow("init");
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
	
	private byte pack(
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
		return buf[off+2];
	}
	
	private boolean tell_3964R() {
		long t0 = System.currentTimeMillis();
		long dt = 0L;
		writeByte(STX);
		byte cc = 0;
		do{
			cc = readByte();
			if(cc==DLE) {
				return true;
			}
			dt = System.currentTimeMillis() - t0;
		}while(dt<5000);
		return false;
	}
	
	private void wait_3964R(final byte BCC) {
		final byte[] res = {DLE,ETX,BCC};
		writeByte(res,0,3);
		purgeByte(res,0,2);
		//if(res[0]!=DLE || res[1]!=STX) {
		// return; ???
		//}
		writeByte(DLE);
	}
	
	public int[] get_register(
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
		final byte BCC = pack(ans,'E','D',addr,size,0);
		
		//step.1 - give start code and waiting.
		if(tell_3964R()==false) {
			return null;
		}
		//step.2 - give device RK512 header.		
		writeByte(ans,0,13-3);
		wait_3964R(BCC);
		//step.3 - get frame:
		//	token(3byte), error-code(1byte), 
		//	data package(size*2),
		//	DLE, ETX, CRC
		purgeByte(ans,0,(3+1+cnt+3));
		writeByte(DLE);
		//end of talking~~~~
		
		//check whether answer is valid....
		//ans[0:2]--> token
		//ans[3]  --> error code
		//ans[-3] --> DLE(3964R)
		//ans[-2] --> ETX(3954R)
		//ans[-1] --> unknown in document
		if(	ans[3]!=0 ||
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
	
	public boolean set_register(
		final int addr, 
		final int... vals
	) {
		int cnt = vals.length;
		//prepare RK512, Data
		byte[] ans = new byte[10+cnt*2+3];
		for(int i=0; i<cnt; i++) {
			ans[10+i*2+0] = (byte)((vals[i] & 0xFF00)>>8);
			ans[10+i*2+1] = (byte)((vals[i] & 0x00FF)   );
		}
		final byte BCC = pack(ans,'A','D',addr,cnt,cnt*2);
		//step.1 - give start code and waiting.
		if(tell_3964R()==false) {
			return false;
		}
		//step.2 - give device RK512 header.
		writeByte(ans,0,ans.length-3);
		wait_3964R(BCC);
		//step.3 - fetch echo ???
		purgeByte(ans,0,7);
		writeByte(DLE);
		//end of talking~~~~
		return true;
	}

	private interface Callback {
		void call(int[] val);
	}
	
	public void getRegister(
		final Callback event,
		final int addr, 
		final int size		
	) {asyncBreakIn(()->{
		int[] val = get_register(addr,size);
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
	) {asyncBreakIn(()->{		
		boolean flag = set_register(addr,vals);
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
	
	public void setPulse(
		final int Ton_P,
		final int Ton_N,
		final int Toff_P,
		final int Toff_N
	) {
		//4* Ton +: 2-32000us, duration of the pulse
		//5* Toff+: 2-32000us, duration of the pause
		//6* Ton -: 2-32000us, duration of the pulse
		//7* Toff-: 2-32000us, duration of the pause
		int msk = 0;
		if(2<=Ton_P && Ton_P<=3200) {
			msk = msk | 1;
		}
		if(2<=Toff_P && Toff_P<=3200) {
			msk = msk | 2;
		}
		if(2<=Ton_N && Ton_N<=3200) {
			msk = msk | 4;
		}
		if(2<=Toff_N && Toff_N<=3200) {
			msk = msk | 8;
		}
		switch(msk) {
		case  1: set_register(4,Ton_P ); break;
		case  2: set_register(5,Toff_P); break;
		case  4: set_register(6,Ton_N ); break;
		case  8: set_register(7,Toff_N); break;
		
		case  3: set_register(4,Ton_P ,Toff_P); break;
		case  6: set_register(5,Toff_P,Ton_N ); break;
		case 12: set_register(6,Ton_N ,Toff_N); break;
		
		case  7: set_register(4,Ton_P,Toff_P,Ton_N); break;
		case 14: set_register(5,Toff_P,Ton_N,Toff_N); break;
		
		case 15: set_register(4,Ton_P,Toff_P,Ton_N,Toff_N); break;
		
		case 5:
			set_register(4,Ton_P );
			set_register(6,Ton_N );
			break;			
		case 9:
			set_register(4,Ton_P );
			set_register(7,Toff_N);
			break;
		case 10:
			set_register(5,Toff_P);
			set_register(7,Toff_N);
			break;
		case 11:
			set_register(4,Ton_P,Toff_P);
			set_register(7,Toff_N);
			break;
		case 13:
			set_register(4,Ton_P );
			set_register(6,Ton_N,Toff_N);
			break;		
		}
	}
	public void asyncSetPulse(
		final int Ton_P,
		final int Ton_N,
		final int Toff_P,
		final int Toff_N
	) {asyncBreakIn(()->{
		setPulse(Ton_P,Ton_N,Toff_P,Toff_N);
	});}
	
	//---------------------------------//
	
	private int[] fst_value1 = null;//Mode

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
		final DevSPIK2k dev, 
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
	
	public static Pane genPanel(final DevSPIK2k dev) {
		
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
		btn[2].setOnAction(e->dev.setRegister(_v->{},1,0x23));//DC-2 on
		btn[3].setOnAction(e->dev.setRegister(_v->{},1,0x22));//DC-2 off
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
		lay0.addRow(1, rad[1], rad[3]);
		lay0.addRow(2, rad[2], rad[4]);
		lay0.addRow(3, rad[0]);
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
