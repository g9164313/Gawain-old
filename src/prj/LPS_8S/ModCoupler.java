package prj.LPS_8S;

import java.util.Optional;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
	private final int DOUT_ADDR1 = 8005;
	private final int DOUT_ADDR2 = 8006;
	private final int AOUT_ADDR = 8007;//4-channel
	
	//IVT2050-313BL
	
	private final BooleanProperty[] iflag = new BooleanProperty[8];
	
	public final BooleanProperty flgMasterLock;
	public final BooleanProperty flgMasterUnLock;
	
	public final IntegerProperty PRESS_UP;
	public final IntegerProperty PRESS_DW;
	public final IntegerProperty FLUX_VAL;
	
	public Runnable working_press = null;
	public Runnable working_float = null;
	public Runnable emerged_press = null;
	public Runnable emerged_float = null;
	
	public ModCoupler() {
		
		mapAddress("h8000","h8001~8004");
		
		for(int i=0; i<iflag.length; i++) {
			iflag[i] = new SimpleBooleanProperty();
		}
		din = holdingRegister(DINN_ADDR);
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
		
		flgMasterUnLock= iflag[0];
		flgMasterLock = iflag[2];
		
		PRESS_DW = ain[0] = holdingRegister(8001);
		PRESS_UP = ain[1] = holdingRegister(8002);
		FLUX_VAL = ain[2] = holdingRegister(8003);
		ain[3] = holdingRegister(8004);
	}
	
	private JFXToggleButton[] tgl_dout;
	private Label[] txt_aout;
	
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

		writeVal(AOUT_ADDR+0, (int)(1000));
		writeVal(AOUT_ADDR+1, (int)(1000));
		
		final short[] a_val = new short[4];
		implReadI(AOUT_ADDR, a_val);
		
		writeBit1(DOUT_ADDR2,0);//卡榫退出

		arm_dw_value = readReg('I',AOUT_ADDR+0);
		
		Application.invokeAndWait(()->{
			for(int i=0; i<d_flg.length; i++) {
				tgl_dout[i].setSelected(d_flg[i]);
			}
			for(int i=0; i<a_val.length; i++) {
				int _v = ((int)a_val[i])&0xFFFF;
				txt_aout[i].setText(String.format(
					"%.2f",((float)_v)/1000f
				));
			}
		});
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
			Misc.logv("signal rise!!");
			if(act_press!=null) { act_press.run(); }
		}else if(ov==1 && nv==0) {
			Misc.logv("signal fall!!");
			if(act_float!=null) { act_float.run(); }
		}
	}
	
	private int arm_dw_value = 0; 
	private Runnable act_arm_up = ()->{
		arm_dw_value = readReg('I',AOUT_ADDR+0);
		writeVal(AOUT_ADDR+0, (int)(0));//close value
	};
	private Runnable act_arm_dw = ()->{
		Misc.logv("PRESS_DW=%d",arm_dw_value);
		writeVal(AOUT_ADDR+0, arm_dw_value);
	};
	
	public void toggleSlurry(final boolean flg) {
		tgl_dout[0].setSelected(flg);
		tgl_dout[0].getOnAction().handle(null);
		tgl_dout[3].setSelected(!flg);
		tgl_dout[3].getOnAction().handle(null);
	}
	public void toggleHeater(final boolean flg) {
		tgl_dout[1].setSelected(flg);
		tgl_dout[1].getOnAction().handle(null);
	}
	public void toggleAlarm(final boolean flg) {
		tgl_dout[2].setSelected(flg);
		tgl_dout[2].getOnAction().handle(null);
	}
	public void toggleLatch(final boolean lock) {
		tgl_dout[4].setSelected(!lock);
		tgl_dout[4].getOnAction().handle(null);
	}
	public void armPressDw(final float volt) {
		change_aout(0,volt);
	}
	public void armPressUp(final float volt) {
		change_aout(1,volt);
	}
	private void change_aout(final int id, final float volt) {
		txt_aout[id].setText(String.format("%.2f",volt));
		final int val =  (int)(volt*1000.f);
		if(id==0) {
			arm_dw_value = val;
		}
		asyncWriteVal(AOUT_ADDR+id, val);
	}
	
	public Node gen_console() {
		
		final JFXCheckBox[] chk = {
			new JFXCheckBox("退卡磁簧"),
			new JFXCheckBox("上提軸"),
			new JFXCheckBox("卡榫磁簧"),
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
		
		final JFXToggleButton[] tgl = {
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton(),
		};
		tgl_dout = tgl;
		tgl[0].setText("冷卻水");//冷卻水-->止水汽缸-->
		tgl[1].setText("加熱器");
		tgl[2].setText("警示燈");
		tgl[3].setText("止水汽缸");
		tgl[4].setText("卡榫退出");
		
		tgl[0].setOnAction(e->out_pin(tgl[0],0));
		tgl[1].setOnAction(e->out_pin(tgl[1],1));
		tgl[2].setOnAction(e->out_pin(tgl[2],2));
		tgl[3].setOnAction(e->out_pin(tgl[3],3));
		tgl[4].setOnAction(e->out_pin(tgl[4],0x80000));
		
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
		lay3.add(new Label("輸入訊號"), 0, 0, 2, 1);
		lay3.addColumn(0,
			new Label("AI1:"),
			new Label("AI2:"),
			new Label("AI3:"),
			new Label("AI4:")
		);
		lay3.addColumn(1,t_ain);
		
		final GridPane lay4 = new GridPane();
		lay4.getStyleClass().addAll("box-pad-inner");
		lay4.add(new Label("輸出訊號"), 0, 0, 2, 1);
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
		lay2.getChildren().addAll(lay3);
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().addAll("box-pad");
		lay1.getChildren().add(new Label("輸出接點"));
		lay1.getChildren().addAll(tgl);
		lay1.getChildren().addAll(lay4);
		
		final HBox lay0 = new HBox(lay2,lay1);
		lay0.getStyleClass().addAll("box-pad","box-border");
		return lay0;
	}
	
	private void out_pin(
		final JFXToggleButton tgl,
		int bit
	) {
		int addr = DOUT_ADDR1;
		if((bit&0x80000)!=0) {
			addr = DOUT_ADDR2;
			bit = bit & 0xFFFF;
		}
		if(tgl.isSelected()==true) {
			asyncWriteBit1(addr, bit);
		}else {
			asyncWriteBit0(addr, bit);
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

