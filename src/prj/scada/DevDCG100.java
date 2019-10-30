package prj.scada;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.sun.glass.ui.Application;

import javafx.animation.AnimationTimer;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import narl.itrc.DevBase;
import narl.itrc.DevTTY;
import narl.itrc.Misc;

/**
 * DCG Dual 5kW
 * DC Plasma Generator
 * Support RS-232 interface
 * @author qq
 * 
 */
/**
 * @author qq
 *
 */
public class DevDCG100 extends DevTTY {
	
	public DevDCG100(){
		TAG = "DevDCG-stream";
	}

	public DevDCG100(String path_name){
		this();
		setPathName(path_name);
	}
	
	@Override
	protected void afterLoop() {
		//asyncFetch("REP\r" ,(cmd,txt)->response(cmd,txt));
		exec(true,"REME\r",null);
		exec(true,"REM1\r",null);
		exec(true,"SPV\r",(txt)->{
			//regulation value - volt
			String _txt = trim_result(txt);
			_txt = _txt.substring(0,_txt.length()-1);
			float val = Float.valueOf(_txt) * 10f;
			v_spv.set((int)val);
		});
		exec(true,"SPA\r",(txt)->{
			//regulation value - amps
			String _txt = trim_result(txt);
			_txt = _txt.substring(0,_txt.length()-1);
			float val = Float.valueOf(_txt) * 100f;
			v_spa.set((int)val);
		});
		exec(true,"SPW\r",(txt)->{
			//regulation value - watt
			String _txt = trim_result(txt);
			_txt = _txt.substring(0,_txt.length()-1);
			float val = Float.valueOf(_txt);
			v_spw.set((int)val);
		});
		exec(true,"SPR\r",(txt)->{
			//ramp-time
			String _txt = trim_result(txt);
			_txt = _txt.substring(0,_txt.length()-1);
			float val = Float.valueOf(_txt);
			v_spr.set((int)val);
		});
		exec(true,"SPT\r",(txt)->{
			//run-time shutdown
			//unit is second
			String _txt = trim_result(txt);
			float val = Float.valueOf(_txt) * 1000f;
			v_spt.set((int)val);
		});
		exec(true,"SPJ\r",(txt)->{
			//joules shutdown
			String _txt = trim_result(txt);
			float val = Float.valueOf(_txt);
			v_spj.set((int)val);
		});
		exec(true,"CHL\r",(txt)->{
			//regulation mode - Amps, Volts, Watt
			v_chl = trim_result(txt).charAt(0);
		});
		exec(true,"CHT\r",(txt)->{
			//control mode & shutdown time
			//constant, sequence mode
			//joules or run-time shutdown
			v_cht = trim_result(txt);
		});
	}
	
	private final long period = 1000L; 
	private long pre_loop_tick = System.currentTimeMillis();

	@Override
	protected void doLoop(DevBase dev) {
		long cur_loop_tick = System.currentTimeMillis();
		if((cur_loop_tick-pre_loop_tick)<period) {
			return;
		}
		//monitor data~~~
		txt2prop(fetchTxt("MVV\r"), volt);
		txt2prop(fetchTxt("MVA\r"), amps);
		txt2prop(fetchTxt("MVW\r"), watt);
		txt2prop(fetchTxt("MVJ\r"), joul);
		pre_loop_tick = System.currentTimeMillis();
	}
	
	protected void exec(
		final String cmd
	) {
		exec(false,cmd,null);
	}
	protected void exec(
		final String cmd,
		final ReadBack gui_event
	) {
		exec(false,cmd,gui_event);
	}
	protected void exec(
		final boolean silent,
		final String cmd,		
		final ReadBack gui_event		
	) {
		doingNow((act)->{
			String txt = fetchTxt(cmd);			
			if(txt.contains("?")==true || txt.contains("*")==false) {
				//fail to execute command, show dialog or pass it!!!
				Misc.loge("[X] %s-->%s",cmd,txt);
				if(silent==true) {
					return;
				}
				Application.invokeAndWait(()->{
					final Alert diag = new Alert(AlertType.ERROR);
					diag.setTitle("錯誤！！");
					diag.setHeaderText("指令沒有回應:"+cmd+")"+txt);
					diag.showAndWait();
				});
			}else {
				if(gui_event!=null) {
					Application.invokeAndWait(()->gui_event.callback(txt));
				}
			}
		});
	}
	
	private String trim_result(String txt) {
		int idx = txt.indexOf(0x0D);
		if(idx>0) {
			txt = txt.substring(idx+1);
		}else {
			return "";
		}
		idx = txt.lastIndexOf('*');
		if(idx>0) {
			txt = txt.substring(0,idx);
		}else {
			return "";
		}
		return txt.trim();
	}
	private void txt2prop(
		final String txt,
		final FloatProperty prop 			
	) {
		try {
			String _txt = trim_result(txt);
			if(_txt.length()==0) {
				return;
			}
			float _val = Float.valueOf(_txt);
			if(Application.isEventThread()==true) {
				prop.set(_val);
			}else {
				Application.invokeAndWait(()->prop.set(_val));
			}
		}catch(NumberFormatException e) {
			Misc.loge("[Wrong Fomrat] %s", txt);
		}
	}
	/*private void txt2prop(
		final String txt,
		final IntegerProperty prop 			
	) {
		try {
			String _txt = trim_result(txt);
			if(_txt.length()==0) {
				return;
			}
			int _val = Integer.valueOf(_txt);
			if(Application.isEventThread()==true) {
				prop.set(_val);
			}else {
				Application.invokeAndWait(()->prop.set(_val));
			}
		}catch(NumberFormatException e) {
			Misc.loge("[Wrong Fomrat] %s", txt);
		}
	}*/
	//-------------------------//
	
	public final FloatProperty volt = new SimpleFloatProperty(0.f);
	public final FloatProperty amps = new SimpleFloatProperty(0.f);
	public final FloatProperty watt = new SimpleFloatProperty(0.f);
	public final FloatProperty joul = new SimpleFloatProperty(0.f);

	private final IntegerProperty v_spv = new SimpleIntegerProperty();//unit is 0.1 volt
	private final IntegerProperty v_spa = new SimpleIntegerProperty();//unit is 0.01 amp
	private final IntegerProperty v_spw = new SimpleIntegerProperty();//unit is 1 watts
	private final IntegerProperty v_spr = new SimpleIntegerProperty();//ramp-time (ms)
	private final IntegerProperty v_spt = new SimpleIntegerProperty();//run-time shutdown (ms)
	private final IntegerProperty v_spj = new SimpleIntegerProperty();//joules shutdown (J or kJ)

	private char v_chl = 0;
	private String v_cht = "";
	
	private static void set_value(
		final DevDCG100 dev,
		final String cmd,
		final String title,
		final String text,
		final float scale,
		final IntegerProperty prop
	) {
		final TextInputDialog diag = new TextInputDialog();
		diag.setTitle(title);
		diag.setContentText(text);
		Optional<String> rest = diag.showAndWait();
		if(rest.isPresent()==true) {
			try {
				int val = (int)(Float.valueOf(rest.get()) * scale);
				dev.exec(
					String.format("%s=%d\r",cmd,val), 
					(txt)->prop.set(val)
				);
			}catch(NumberFormatException exp) {				
			}
		}
	}
	
	public static Pane genPanel(final DevDCG100 dev) {
		
		final ToggleGroup grp1 = new ToggleGroup();
		final ToggleGroup grp2= new ToggleGroup();

		final JFXRadioButton[] rad = {
			new JFXRadioButton ("電壓"),
			new JFXRadioButton ("電流"),
			new JFXRadioButton ("功率"),
			new JFXRadioButton ("固定"),
			new JFXRadioButton ("間歇"),
			new JFXRadioButton ("焦耳"),
		};
		rad[0].setToggleGroup(grp1);
		rad[1].setToggleGroup(grp1);
		rad[2].setToggleGroup(grp1);
		rad[3].setToggleGroup(grp2);
		rad[4].setToggleGroup(grp2);
		rad[5].setToggleGroup(grp2);
		
		rad[0].setOnAction(e->dev.exec("CHL=V\r"));
		rad[1].setOnAction(e->dev.exec("CHL=A\r"));
		rad[2].setOnAction(e->dev.exec("CHL=W\r"));
		
		rad[3].setOnAction(e->dev.exec("CHT=C\r"));
		rad[4].setOnAction(e->dev.exec("CHT=CT\r"));
		rad[5].setOnAction(e->dev.exec("CHT=CJ\r"));
		
		//-------------------------------------//
				
		final JFXButton[] btn = new JFXButton[8];
		for(int i=0; i<btn.length; i++) {
			btn[i] = new JFXButton(); 			
			btn[i].setMaxWidth(Double.MAX_VALUE);
			GridPane.setHgrow(btn[i], Priority.ALWAYS);
		}
		btn[0].setText("ON");
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setOnAction(e->{
			dev.exec("TRG\r");
		});
		
		btn[1].setText("OFF");
		btn[1].getStyleClass().add("btn-raised-2");
		btn[1].setOnAction(e->{
			dev.exec("OFF\r");
		});
		
		btn[2].setText("預備時間");
		btn[2].setOnAction(e->set_value(
			dev,"SPR",
			"Ramp-Time","預備訊號時間(ms)",
			1f, dev.v_spt
		));
		
		btn[3].setText("額定電壓");
		btn[3].visibleProperty().bind(rad[0].selectedProperty());
		btn[3].setOnAction(e->set_value(
			dev,"SPV",
			"額定電壓","",
			10f, dev.v_spv
		));		
		btn[4].setText("額定電流");
		btn[4].visibleProperty().bind(rad[1].selectedProperty());
		btn[4].setOnAction(e->set_value(
			dev,"SPA",
			"額定電流","",
			100f, dev.v_spa
		));
		btn[5].setText("額定功率");
		btn[5].visibleProperty().bind(rad[2].selectedProperty());
		btn[5].setOnAction(e->set_value(
			dev,"SPW",
			"額定功率","",
			1f, dev.v_spw
		));
				
		btn[6].setText("間歇時間");
		btn[6].visibleProperty().bind(rad[4].selectedProperty());
		btn[6].setOnAction(e->set_value(
			dev,"SPT",
			"Run-Time","維持輸出時間(ms)",
			1f, dev.v_spt
		));
		
		btn[7].setText("額定焦耳");		
		btn[7].visibleProperty().bind(rad[5].selectedProperty());
		btn[7].setOnAction(e->set_value(
			dev,"SPJ",
			"Joules-Shutdown","維持總輸出功率(Joules)",
			1f, dev.v_spj
		));
		
		final Label[] txt = new Label[6];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label("        ");
		}
		txt[1].visibleProperty().bind(rad[0].selectedProperty());
		txt[2].visibleProperty().bind(rad[1].selectedProperty());
		txt[3].visibleProperty().bind(rad[2].selectedProperty());		
		txt[4].visibleProperty().bind(rad[4].selectedProperty());
		txt[5].visibleProperty().bind(rad[5].selectedProperty());
		//-------------------------------------//
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad-inner");
		lay1.add(btn[2], 0, 0, 1, 1); lay1.add(txt[0], 1, 0, 1, 1);
		lay1.add(btn[3], 0, 1, 1, 1); lay1.add(txt[1], 1, 1, 1, 1);
		lay1.add(btn[4], 0, 1, 1, 1); lay1.add(txt[2], 1, 1, 1, 1);
		lay1.add(btn[5], 0, 1, 1, 1); lay1.add(txt[3], 1, 1, 1, 1);
		lay1.add(btn[6], 0, 2, 1, 1); lay1.add(txt[4], 1, 2, 1, 1);
		lay1.add(btn[7], 0, 2, 1, 1); lay1.add(txt[5], 1, 2, 1, 1);
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.add(new Label("調變訊號"), 0, 0);
		lay.add(new Label("輸出模式"), 2, 0);
		lay.add(rad[0], 0, 1);
		lay.add(rad[1], 0, 2);
		lay.add(rad[2], 0, 3);
		lay.add(rad[3], 2, 1);
		lay.add(rad[4], 2, 2);
		lay.add(rad[5], 2, 3);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
		lay.add(new Separator(), 0, 4, 3, 1);
		lay.add(lay1, 0, 5, 3, 3);
		lay.add(new Separator(), 0, 8, 3, 1);
		lay.add(btn[0], 0, 9, 3, 1);
		lay.add(btn[1], 0,10, 3, 1);
		
		//binding!!!
		Application.invokeLater(()->{
			
			final AnimationTimer tim = new AnimationTimer() {
				@Override
				public void handle(long arg0) {
					if(dev.v_chl==0 || dev.v_cht.length()==0) {
						return;
					}
					switch(dev.v_chl) {
					case 'V': rad[0].setSelected(true); break;
					case 'A': rad[1].setSelected(true); break;
					case 'W': rad[2].setSelected(true); break;
					}
					if(dev.v_cht.equalsIgnoreCase("C")==true) {
						rad[3].setSelected(true);
					}else if(dev.v_cht.equalsIgnoreCase("CT")==true) {
						rad[4].setSelected(true);
					}else if(dev.v_cht.equalsIgnoreCase("CJ")==true) {
						rad[5].setSelected(true);
					}
					stop();
				}
			};
			tim.start();
			
			txt[0].textProperty().bind(dev.v_spr.asString("%d ms"));//ramp-time
			
			txt[1].textProperty().bind(dev.v_spv.multiply(0.1f).asString("%.1f V"));
			txt[2].textProperty().bind(dev.v_spa.multiply(0.01f).asString("%.2f A"));
			txt[3].textProperty().bind(dev.v_spw.asString("%d W"));
			
			txt[4].textProperty().bind(dev.v_spt.asString("%d ms"));//run-time shutdown
			txt[5].textProperty().bind(dev.v_spj.asString("%d kJ"));//joules shutdown
		});
		return lay;
	}
}

/**
----DCG-100 System report----

System Total KW:                10

Interface Board Type:     ANALOG_D

PMA Tap Setting:              500V
PMB Tap Setting:              500V

PMA Arc Threshold (Base):      255
PMA Arc Threshold (Prop):      255
PMA Arc-Reset Delay:             1
PMA Max Arc Count:               0

PMB Arc Threshold (Base):      255
PMB Arc Threshold (Prop):      255
PMB Arc-Reset Delay:             1
PMB Max Arc Count:               0

RS422 ADDRESS = 00
.
    NAME    SER_NO      DATE        ON_TIME        UP_TIME  VER

      F8      3283   4/20/04    14600.59.17    36589.34.43  3.3
*
*/
