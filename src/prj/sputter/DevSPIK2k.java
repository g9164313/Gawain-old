package prj.sputter;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
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
public class DevSPIK2k extends DevTTY {

	public DevSPIK2k() {
		TAG = "SPIK2k";
	}	
	@Override
	public void afterOpen() {
		addState("listen", listen);
		addState("looper", looper);
		//playFlow("init");
		playFlow("looper");
	}
	@Override
	public void beforeClose() {
	}
	//----------------------------------//

	//0* Mode: 
	//1* State: 
	//3* Error: 
	//4* Ton +: 2-32000us, duration of the pulse
	//5* Toff+: 2-32000us, duration of the pause
	//6* Ton -: 2-32000us, duration of the pulse
	//7* Toff-: 2-32000us, duration of the pause
	public final IntegerProperty ARC_count = new SimpleIntegerProperty();//0-10000
	public final IntegerProperty DC1_V_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC1_I_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC1_P_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_V_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_I_Act = new SimpleIntegerProperty();//0-4000
	public final IntegerProperty DC2_P_Act = new SimpleIntegerProperty();//0-4000
	
	private Runnable listen = ()->{
		final SerialPort dev = port.get();
		if(dev.isOpened()==false) {
			stopFlow();
			return;
		}
		try {
			byte[] buf = protocol_3964R_listen(50);
			if(buf.length==0) {
				//something error happened, listen again!!!!
				nextState("listen");
				return;
			}
			//it should AD packet
			int[] val = RK512_unpack_AD(buf);			
			if(val.length>=8 && val[0]==19) {
				Application.invokeLater(()->{
					ARC_count.set(val[1]);
					DC1_V_Act.set(val[2]);
					DC1_I_Act.set(val[3]);
					DC1_P_Act.set(val[4]);
					DC2_V_Act.set(val[5]);
					DC2_I_Act.set(val[6]);
					DC2_P_Act.set(val[7]);						
				});
			}			
			nextState("listen");			
		}catch(SerialPortTimeoutException e1) {
			//device no response, check whether host has works.
			//Misc.loge("[%s] listen - %s", TAG, e1.getMessage());
			nextState("looper");
		} catch (SerialPortException e2) {			
			Misc.loge("[%s] listen - %s", TAG, e2.getMessage());
			nextState("listen");
		}
	};
	
	public static interface TokenNotify {
		void token_notify(final Token tkn);
	};
	public static class Token {
		public final int address;
		public final int count;
		public int[] values;//如果是 null, 就是讀取資料
		public byte[] response;
		TokenNotify event;
		Token(final int addr, final int cnt){
			address= addr;
			count  = cnt;
			values = null;
		}
		Token(final int addr, final int[] val){
			address= addr;
			count  = val.length;
			values = val;
		}
		void unpack() {
			if(values!=null) {
				return;
			}
			values = new int[count];
			for(int i=0; i<count; i++) {
				byte aa = response[4+i*2];
				byte bb = response[5+i*2];
				values[i] = (((int)aa)&0x00FF)<<8 | bb;
			}
		}
	};
		
	private final AtomicReference<Token> ref_tkn = new AtomicReference<Token>();
	
	private Runnable looper = ()->{
		final SerialPort dev = port.get();
		if(dev.isOpened()==false) {
			stopFlow();
			return;
		}
		final Token tkn = ref_tkn.get();
		if(tkn!=null) {
			Misc.loge("[%s] looper - transmit",TAG);
			if(protocol_3964R_reveal(tkn.address, tkn.count, tkn.values)==true) {
				try {
					tkn.response = protocol_3964R_listen(-1);
				} catch (SerialPortTimeoutException | SerialPortException e) {
					Misc.loge("[%s] looper - %s", TAG, e.getMessage());
					nextState("listen");
					return;
				}
				tkn.unpack();
			}			
			if(tkn.event!=null) {
				Application.invokeLater(()->tkn.event.token_notify(tkn));
			}
			ref_tkn.set(null);//for nest turn~~~~
		}
		nextState("listen");
	};
	
	public void asyncSetRegister(TokenNotify event, final int addr, int... val) {
		Token tkn = new Token(addr,val);
		tkn.event = event;//e->{
			//buf[0:2]--> token
			//buf[  3]--> error code
			//buf[  4]--> DLE
			//buf[  5]--> ETX
			//buf[  6]--> checksum
			//byte cc = e.response[0];
		//}; 
		ref_tkn.set(tkn);
	}
	public void asyncGetRegister(
		final TokenNotify event, 
		final int addr, 
		final int count
	) {
		Token token = new Token(addr,count);
		token.event = event;/*tkn->{
			String txt = "addr="+tkn.address+", vals= {\n";
			for(int v:tkn.values) {
				txt = txt + v + ",\n";
			}
			System.out.println(txt+"}\n");
		};*/
		ref_tkn.set(token);
	} 
	
	public void setRunning(boolean flg) {
		asyncSetRegister(null,1,(flg==true)?(2):(1));
	}
	
	//device setting values
	//4* Ton +: 2-32000us, duration of the pulse
	//5* Toff+: 2-32000us, duration of the pause
	//6* Ton -: 2-32000us, duration of the pulse
	//7* Toff-: 2-32000us, duration of the pause
	public void set_Ton_P(final int us) {
		asyncSetRegister(null,4,us);
	}
	public void set_Toff_P(final int us) {
		asyncSetRegister(null,5,us);
	}
	public void set_Ton_N(final int us) {
		asyncSetRegister(null,6,us);
	}
	public void set_Toff_N(final int us) {
		asyncSetRegister(null,7,us);
	}
	
	public void set_T_pos(final int on_us,final int off_us) {
		asyncSetRegister(null,4,on_us,off_us);
	}
	public void set_T_neg(final int on_us,final int off_us) {
		asyncSetRegister(null,6,on_us,off_us);
	}
	
	public void set_pulse(
		final int Ton_P_us,
		final int Toff_P_us,
		final int Ton_N_us,
		final int Toff_N_us
	) {
		asyncSetRegister(null,4,Ton_P_us,Toff_P_us,Ton_N_us,Toff_N_us);
	}
	//---------------------------------//
	
	private static void show_error(Token tkn) {
		if(tkn.response[3]==0) {
			return;
		}
		String txt = String.format(
			"Addr:%d+%d, Error:%d",
			tkn.address,tkn.count,(int)tkn.response[3]
		);
		Misc.logv("[SPIK2000] %s",txt);
		//error code
		final Alert dia = new Alert(AlertType.ERROR);
		dia.setTitle("錯誤！！");
		dia.setHeaderText(txt);
		dia.showAndWait();
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
		
		final TextInputDialog dd1 = new TextInputDialog();
		dd1.setTitle("設定 "+pref);
		dd1.setContentText("時間(us)");		
		Optional<String> res = dd1.showAndWait();
		if(res.isPresent()==false) {
			return;
		}
		
		try {
			int val = Integer.valueOf(res.get());
			dev.asyncSetRegister(tkn->show_error(tkn), addr, val);			
		}catch(NumberFormatException exp) {
			final Alert dia = new Alert(AlertType.ERROR);
			dia.setTitle("錯誤！！");
			dia.setHeaderText("輸入必須為整數");
			dia.showAndWait();
		}
	}
	
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
				dev.asyncSetRegister(
					tkn->show_error(tkn),
					0,
					(int)grp.getSelectedToggle().getUserData()
				);
			});
		}
		
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
		btn[0].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x21));//DC-1 on
		btn[1].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x20));//DC-1 off
		btn[2].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x23));//DC-2 on
		btn[3].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x22));//DC-2 off
		btn[4].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x02));//RUN on
		btn[5].setOnAction(e->dev.asyncSetRegister(tkn->show_error(tkn),1,0x01));//RUB off
		
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

		//final AnimationTimer wait_device = new AnimationTimer() {
		//};
		//-------------------------------------//
		
		JFXButton btn_test = new JFXButton();
		btn_test.setMaxWidth(Double.MAX_VALUE);
		btn_test.getStyleClass().add("btn-raised-1");
		btn_test.setOnAction(e->{
			dev.asyncGetRegister(null,4, 4);
		});
		
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
		lay0.add(lay1, 0, 10, 4, 3);
		lay0.add(btn_test, 0, 14, 4, 1);
		
		return lay0;
	}
}
