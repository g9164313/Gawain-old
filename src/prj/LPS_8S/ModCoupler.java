package prj.LPS_8S;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import narl.itrc.DevModbus;
import narl.itrc.Misc;
import narl.itrc.PadTouch;


public class ModCoupler extends DevModbus {

	/**
	 * h8000       - digital input
	 * h8001~h8004 - analog  input
	 * i8005       - digital output
	 * i8006~i8009 - analog  output
	 */
	private final IntegerProperty din;
	private final IntegerProperty[] ain= {null,null,null,null};
	
	private final int DINN_ADDR = 8000;
	//private final int AINN_ADDR = 8001;
	private final int DOUT_ADDR1= 8005;
	private final int DOUT_ADDR2= 8006;
	private final int AOUT_ADDR = 8007;//4-channel
	
	//IVT2050-313BL
	
	private final BooleanProperty[] iflag = new BooleanProperty[8];
	
	public final BooleanProperty flgMasterLocate;
	public final BooleanProperty flgMasterUnLock;
	
	public final IntegerProperty ARM_MVOLT_UP;
	public final IntegerProperty ARM_MVOLT_DW;
	public final IntegerProperty FD_Q20C_AOUT;
	
	public final FloatProperty ARM_PRESS_UP = new SimpleFloatProperty();//unit is MPa
	public final FloatProperty ARM_PRESS_DW = new SimpleFloatProperty();//unit is MPa
	
	public Runnable working_press = null;
	public Runnable working_float = null;
	public Runnable emerged_press = null;
	public Runnable emerged_float = null;
	
	public ModCoupler() {
		
		mapAddress("h8000~8004");
		
		for(int i=0; i<iflag.length; i++) {
			iflag[i] = new SimpleBooleanProperty();
		}
		din = mapInteger(DINN_ADDR);
		din.addListener((obv,oldVal,newVal)->{
			int prv = oldVal.intValue();
			int cur = newVal.intValue();
			for(int i=0; i<iflag.length; i++) {
				iflag[i].set((cur&(1<<i))!=0);
			}
			//remember to swap!!!
			detect_edge(prv,cur,1,act_arm_up,act_arm_dw);
			detect_edge(prv,cur,4,emerged_press,emerged_float);
			detect_edge(prv,cur,5,working_press,working_float);
		});
		
		flgMasterUnLock = iflag[0];//卡榫退出
		flgMasterLocate = iflag[2];//卡榫定點
		
		ARM_MVOLT_DW = ain[0] = mapInteger(8001);
		ARM_MVOLT_UP = ain[1] = mapInteger(8002);
		FD_Q20C_AOUT = ain[2] = mapInteger(8003);
		ain[3] = mapInteger(8004);
		
		ARM_PRESS_DW.bind(ARM_MVOLT_DW.divide(1000f).multiply(0.1992).subtract(0.1694f));
		ARM_PRESS_UP.bind(ARM_MVOLT_DW.divide(1000f).multiply(0.1726).subtract(0.1239f)); 
	}
	
	private JFXToggleButton[] tgl_dout;
	private Label[] txt_aout;
		
	public ModInsider ibus = null;
	
	@Override
	protected void ignite() {
		//before looping, insure setting~~~
		
		//final short[] dv1 = new short[1];
		//final short[] dv2 = new short[1];
		//implReadI(DOUT_ADDR1,dv1);
		int dout1 = readReg('I',DOUT_ADDR1);
		int dout2 = readReg('I',DOUT_ADDR2);
		final boolean[] d_flg = {
			(dout1&0x1)!=0,
			(dout1&0x2)!=0,
			(dout1&0x4)!=0,
			(dout1&0x8)!=0,
			(dout2&0x1)!=0,
		};

		//when system boost, arm is always up~~~~
		act_arm_up.run();
		
		final short[] a_val = new short[4];
		implReadI(AOUT_ADDR, a_val);
		
		//writeSet(DOUT_ADDR2,0);//卡榫退出
				
		super.ignite();//goto next stage~~~~
	}
	
	private boolean first_edge = false;
	
	private void detect_edge(
		final int oldVal,
		final int newVal,
		final int bit_mask,
		final Runnable act_press,
		final Runnable act_float
	) {
		if(first_edge==false) {
			first_edge = true;
			return;
		}
		int ov = (oldVal & (1<<bit_mask)) >> bit_mask;
		int nv = (newVal & (1<<bit_mask)) >> bit_mask;
		// a接點 或 b接點
		if(ov==0 && nv==1) {
			//Misc.logv("signal rise!!");
			if(act_press!=null) { act_press.run(); }
		}else if(ov==1 && nv==0) {
			//Misc.logv("signal fall!!");
			if(act_float!=null) { act_float.run(); }
		}
	}
	
	public final AtomicInteger ArmDownDelay = new AtomicInteger(500);
	
	private Runnable act_arm_up = ()->{
		writeCont(AOUT_ADDR+0, 0);//arm-forward
		writeCont(AOUT_ADDR+1, 500);//arm-backward
	};
	private Runnable act_arm_dw = ()->{			
		writeCont(AOUT_ADDR+0, 8000);//arm-forward
		writeCont(AOUT_ADDR+1, 2000);//arm-backward
		//blocking_delay(500);//angle-1
		//blocking_delay(700);//angle-2
		blocking_delay(ArmDownDelay.get());//angle-2
		//finally~~~
		writeCont(AOUT_ADDR+0, 3000);//arm-forward
		writeCont(AOUT_ADDR+1, 3300);//arm-backward
	};
	
	public void pumpSlurry(final boolean flg) {
		asyncBreakIn(()->{
			if(flg) {
				writeSet(DOUT_ADDR1,3);//-->開水
				//writeSet(DOUT_ADDR1,0);
			}else {
				writeCls(DOUT_ADDR1,3);//-->關水
			}
		});
	}
	public void heatSlurry(final boolean flg) {
		asyncBreakIn(()->{
			if(flg) {
				writeSet(DOUT_ADDR1,1);
			}else {
				writeCls(DOUT_ADDR1,1);
			}
		});
	}
	public void alarm(final boolean flg) {
		asyncBreakIn(()->{
			if(flg) {
				writeSet(DOUT_ADDR1,2);
			}else {
				writeCls(DOUT_ADDR1,2);
			}
		});
	}
	
	public void armForce(final float volt) {
		final int val =  (int)(volt*1000.f);
		this.asyncBreakIn(()->{
			writeVals(AOUT_ADDR+0, val);//press-down
			writeVals(AOUT_ADDR+1, val);//press-up
		});
	}
	private void change_aout(final int id, final float volt) {
		txt_aout[id].setText(String.format("%.2f",volt));
		final int val =  (int)(volt*1000.f);
		asyncWriteVals(AOUT_ADDR+id, val);
	}
	
	final AtomicBoolean flagStep = new AtomicBoolean(false);
		
	public void servoMove(
		final int ID,
		final boolean flag
	) {
		final int bit = (ID==ModInsider.ID_PRESS)?(1):(0);
		flagStep.set(flag);
		if(flag==false) {
			return;
		}
		asyncBreakIn(()->{	
			while(flagStep.get()==true) {
				writeSet(DOUT_ADDR2,bit);
				blocking_delay(25);
				writeCls(DOUT_ADDR2,bit);
				blocking_delay(25);
			}
		});
	};

	public void LockMasterMotor(
		final boolean lock,
		final Runnable afterEvent
	) {asyncBreakIn(()->{
		if(lock==true) {
			//rotate master motor and wait~~~~
			//writeSet(DOUT_ADDR1, 3);
		}else {
			//writeCls(DOUT_ADDR1, 3);
		}		
		Application.invokeAndWait(afterEvent);
	});}
	
	public void action_working(final JFXToggleButton tgl) {
		if(tgl.isSelected()==true) {
			asyncBreakIn(()->{
				writeSet(DOUT_ADDR2,2);//LOP
				blocking_delay(500);
				writeSet(DOUT_ADDR2,3);//ST1
			});	
		}else {
			asyncBreakIn(()->{
				writeCls(DOUT_ADDR2,3);//ST1
				blocking_delay(5000);
				writeCls(DOUT_ADDR2,2);//LOP
			});	
		}
	}
	
	public Node gen_console() {
		
		final JFXCheckBox[] chk = {
			new JFXCheckBox("卡榫退出"),
			new JFXCheckBox("上提壓扣"),
			new JFXCheckBox("卡榫定點"),
			new JFXCheckBox("止水磁簧"),
			new JFXCheckBox("急停壓扣"),
			new JFXCheckBox("加工壓扣"),
			new JFXCheckBox("壓力檢知-1"),
			new JFXCheckBox("壓力檢知-2"),
		};
		for(int i=0; i<iflag.length; i++) {
			chk[i].selectedProperty().bind(iflag[i]);
			chk[i].setDisable(true);
			chk[i].setStyle("-fx-opacity: 1.0;");
		}

		final Label[] t_ain = {
			new Label(), 
			new Label(), 
			new Label(), 
			new Label()
		};
		for(int i=0; i<ain.length; i++) {
			t_ain[i].textProperty().bind(ain[i].divide(1000f).asString("%4.2f"));
		}
		
		//----------------------
		
		final JFXButton btn_stp1 =  new JFXButton("加壓軸寸進");
		btn_stp1.setMaxWidth(Double.MAX_VALUE);
		btn_stp1.getStyleClass().add("btn-raised-1");
		btn_stp1.setOnAction(e->dout_bounce(5));
		
		final JFXButton btn_stp2 =  new JFXButton("擺動軸寸進");
		btn_stp2.setMaxWidth(Double.MAX_VALUE);
		btn_stp2.getStyleClass().add("btn-raised-1");
		btn_stp2.setOnAction(e->dout_bounce(4));

		final JFXToggleButton[] tgl = {
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton()
		};
		tgl_dout = tgl;
		tgl[0].setText("冷卻水");//冷卻水(抽水幫浦)-->止水汽缸-->
		tgl[1].setText("加熱器");
		tgl[2].setText("警示燈");
		tgl[3].setText("止水汽缸");
		tgl[4].setText("同步旋轉");
		
		tgl[0].setOnAction(e->dout_pin(tgl[0],0));
		tgl[1].setOnAction(e->dout_pin(tgl[1],1));
		tgl[2].setOnAction(e->dout_pin(tgl[2],2));
		tgl[3].setOnAction(e->dout_pin(tgl[3],3));
		tgl[4].setOnAction(e->action_working(tgl[4]));
		
		
		final Label[] t_aout = {
			new Label("------"), 
			new Label("------"), 
			new Label("------"), 
			new Label("------")
		};
		txt_aout = t_aout;
		for(int i=0; i<t_aout.length; i++) {
			final int ID = i;
			t_aout[i].setOnMouseClicked(e->{
				final int aid = ID+1;
				final PadTouch pad = new PadTouch('f',"AO"+aid+"(V)");
				Optional<String> opt = pad.showAndWait();			
				if(opt.isPresent()==false) {
					return;
				}
				float val = Float.valueOf(opt.get());
				if(val<0. || 10.<val) {
					return;
				}
				change_aout(ID,val);
			});
		}
		
		final GridPane lay3 = new GridPane();
		lay3.getStyleClass().addAll("box-pad-inner");
		lay3.add(new Label("類比輸入"), 0, 0, 2, 1);
		lay3.addColumn(0,
			new Label("AI1:"),
			new Label("AI2:"),
			new Label("AI3:"),
			new Label("AI4:")
		);
		lay3.addColumn(1,t_ain);
		
		final GridPane lay4 = new GridPane();
		lay4.getStyleClass().addAll("box-pad-inner");
		lay4.add(new Label("類比輸出"), 0, 0, 2, 1);
		lay4.addColumn(0,
			new Label("AO1(下壓):"),
			new Label("AO2(上提):"),
			new Label("AO3:"),
			new Label("AO4:")
		);
		lay4.addColumn(1,t_aout);
		
		final VBox lay2 = new VBox();
		lay2.getStyleClass().addAll("box-pad");
		lay2.getChildren().add(new Label("輸入接點"));
		lay2.getChildren().addAll(chk);

		final VBox lay1 = new VBox();
		lay1.getStyleClass().addAll("box-pad");
		lay1.getChildren().add(new Label("輸出接點"));
		lay1.getChildren().addAll(tgl);
		lay1.getChildren().addAll(btn_stp1);
		lay1.getChildren().addAll(btn_stp2);

		final HBox lay0 = new HBox(lay2,lay1,lay3,lay4);
		lay0.getStyleClass().addAll("box-pad","box-border");
		return lay0;
	}
	
	private void dout_pin(
		final JFXToggleButton tgl,
		int bit
	) {
		int addr = -1;
		if(bit>=4) {
			//bit4-7
			addr = DOUT_ADDR2;
			bit-=4;
		}else {
			//bit0~3
			addr = DOUT_ADDR1;
		}
		if(tgl.isSelected()==true) {
			asyncWriteSet(addr, bit);
		}else {
			asyncWriteCls(addr, bit);
		}		
	}
	public void dout_bounce(final int bit) {
		final int addr = (bit>=4)?(DOUT_ADDR2):(DOUT_ADDR1);
		final int _bit = (bit>=4)?(bit-4):(bit);
		asyncBreakIn(()->{
			writeSet(addr,_bit);
			blocking_delay(50);
			writeCls(addr,_bit);
		});	
	}
	private void blocking_delay(final int msec) {
		try {
			Thread.sleep(msec);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

/**
 * PHOENIX CONTACT coupler:
 * 
 * ETH BK DIO8 DO4 2TX-PAC:
 * 1.1 OUT1  2.1 OUT2
 * 1.2 GND   2.2 GND
 * 1.3 FE    2.3 FE
 * 1.4 OUT3  2.4 OUT4
 * 
 * 1.1 IN1   2.1 IN2
 * 1.2 Um    2.1 Um
 * 1.3 GND   2.2 GND
 * 1.4 IN3   2.3 IN4
 * 
 * 3.1 IN5   4.1 IN6
 * 3.2 Um    4.1 Um
 * 3.3 GND   4.2 GND
 * 3.4 IN7   4.3 IN8
 * -----------------
 * IB IL AI 4-ECO
 * 1.1 IN1   2.1 GND
 * 1.2 IN2   2.1 GND
 * 1.3 IN3   2.2 GND
 * 1.4 IN4   2.3 GND 
 * -----------------
 * IB IL AO 4-ECO
 * 1.1 OUT1(下)  2.1 OUT2(上)
 * 1.2 GND   2.1 GND
 * 1.3 OUT3  2.2 OUT4
 * 1.4 GND   2.3 GND
 * -----------------
 * Um - 24V
 * FE - Function Earth
 */

