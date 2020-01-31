package prj.scada;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import javafx.scene.layout.VBox;
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
		TAG = "DCG-100";
		readTimeout = 500;
	}

	public DevDCG100(String path_name){
		this();
		setPathName(path_name);
	}
	
	private final static String STG_INIT = "init";
	private final static String STG_MONT = "monitor";
	private final static String STG_WAIT = "purge";
	
	private void state_init() {
		//exec("REP\r" ,(txt)->Misc.logv(txt));
		exec("SPV \r",(txt)->{
			//regulation value - volt (value+unit)
			txt = txt.substring(0,txt.length()-1);
			txt_float2prop(txt,10,v_spv);
		});
		exec("SPA \r",(txt)->{
			//regulation value - amps (value+unit)
			txt = txt.substring(0,txt.length()-1);
			txt_float2prop(txt,100,v_spa);
		});
		exec("SPW \r",(txt)->{
			//regulation value - watt (value+unit)
			txt = txt.substring(0,txt.length()-1);
			txt_int2prop(txt,1,v_spw);
		});
		exec("SPR \r",(txt)->{
			//ramp-time
			//unit is milliseconds
			//report unit is second
			txt_float2prop(txt,1000,v_spr);
		});
		exec("SPT \r",(txt)->{
			//run-time shutdown
			//unit is milliseconds
			//report unit is second
			txt_float2prop(txt,1000,v_spt);
		});
		exec("SPJ \r",(txt)->{
			//joules shutdown
			txt_int2prop(txt,1,v_spj);
		});
		exec("CHL \r",(txt)->{
			//regulation mode - Amps, Volts, Watt
			v_chl = trim_txt(txt).charAt(0);
		});
		exec("CHT \r",(txt)->{
			//control mode & shutdown time
			//constant, sequence mode
			//joules or run-time shutdown
			v_cht = trim_txt(txt);
		});
		exec("REM1\r",(txt)->{});
		exec("REME\r",(txt)->{Application.invokeAndWait(()->{
			isRemote.set(true);
			nextState.set(STG_MONT);
		});});
		nextState.set(null);
	}	
	private void state_monitor() {
		try { Thread.sleep(100); } catch (InterruptedException e) { return;	}
		exec("MVV \r",(txt)->txt_float2prop(txt,1f,volt));
		
		try { Thread.sleep(100); } catch (InterruptedException e) { return;	}
		exec("MVA \r",(txt)->txt_float2prop(txt,1f,amps));
		
		try { Thread.sleep(100); } catch (InterruptedException e) { return;	}
		exec("MVW \r",(txt)->txt_float2prop(txt,1f,watt));
		
		try { Thread.sleep(100); } catch (InterruptedException e) { return;	}
		exec("MVJ \r",(txt)->txt_float2prop(txt,1f,joul));
	}
	private int flagWait = 0;
	private void state_wait() {
		Misc.logv("waiting...");
		Application.invokeAndWait(()->{
			flagWait = v_spr.get();
		});
		try { 
			Thread.sleep(flagWait*5); 
		} catch (InterruptedException e) { 
			return;
		}
		Misc.logv("monitor again...");
		nextState.set(STG_MONT);
	}
	@Override
	protected void afterOpen() {
		setupState0(STG_INIT, ()->state_init()).
		setupStateX(STG_MONT, ()->state_monitor());
		setupStateX(STG_WAIT, ()->state_wait());
		playFlow();
	}
		
	private void exec(
		final String cmd,
		final ReadBack hook
	) {
		writeTxt(cmd);
		String txt = readTxt(readTimeout*2);
		//callback, notify user~~~
		if(hook==null) {
			return;
		}
		if(txt.length()==0) {
			Misc.loge("Timeout!! %s", cmd);
			return;
		}
		txt = trim_txt(txt);
		hook.callback(txt);
	}
	
	private String trim_txt(String txt) {
		int idx = txt.indexOf(0x0A);
		if(idx>0) {
			txt = txt.substring(idx+1);
		}else {
			return txt;
		}
		idx = txt.lastIndexOf('*');
		if(idx>0) {
			txt = txt.substring(0,idx);
		}else {
			return txt;
		}
		return txt.trim();
	}
	
	private void txt_float2prop(
		final String txt,
		final float scale,
		final FloatProperty prop		
	) {
		try {
			float val = Float.valueOf(txt) * scale;
			if(Application.isEventThread()==true) {
				prop.set(val);
			}else {
				Application.invokeAndWait(()->prop.set(val));
			}
		}catch(NumberFormatException e) {
			Misc.loge("[DCG-100]:wrong value-->%s", txt);
		}
	}
	private void txt_float2prop(
		final String txt,
		final int scale,
		final IntegerProperty prop		
	) {
		try {
			int val = (int)(Float.valueOf(txt) * (float)scale);
			if(Application.isEventThread()==true) {
				prop.set(val);
			}else {
				Application.invokeAndWait(()->prop.set(val));
			}
		}catch(NumberFormatException e) {
			Misc.loge("[DCG-100]:wrong value-->%s", txt);
		}
	}

	private void txt_int2prop(
		final String txt,
		final int scale,
		final IntegerProperty prop		
	) {
		try {
			int val = Integer.valueOf(txt) * scale;
			if(Application.isEventThread()==true) {
				prop.set((int)val);
			}else {
				Application.invokeAndWait(()->prop.set(val));
			}
		}catch(NumberFormatException e) {
			Misc.loge("[Wrong Fomrat] %s", txt);
		}
	}
	
	public void asyncExec(
		final String cmd,
		final ReadBack hook
	) {
		breakIn(()->exec(cmd,hook));
	}
	public void asyncTrigger() {
		breakIn(()->exec("TRG \r",null));
	}
	public void asyncTurnOff() {
		breakIn(()->exec("OFF \r",null));
	}
	//-------------------------//
	
	public final BooleanProperty isRemote = new SimpleBooleanProperty(false);
	
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
	
	public static void exec_gui(
		final DevDCG100 dev,
		final String cmd
	) {
		dev.breakIn(()->dev.exec(cmd,(txt)->{

			if(txt.contains("*")==true) {
				//very special condition~~~
				if(cmd.contains("TRG")==true) {
					dev.nextState.set(STG_WAIT);
				}
				return;
			}
			//command fail!!! notify user~~
			final String _cmd = cmd.replace("\r", "");
			final String _txt = txt.replace("\r", "");
			Application.invokeAndWait(()->{				
				final Alert diag = new Alert(AlertType.ERROR);
				diag.setTitle("錯誤！！");
				diag.setHeaderText("指令("+_cmd+")無回應:"+_txt);
				diag.showAndWait();
			});
		}));
	}
	
	private static void set_value(
		final DevDCG100 dev,
		final String cmd,
		final String title,
		final String text,
		final int scale,
		final IntegerProperty prop
	) {
		final TextInputDialog diag = new TextInputDialog();
		diag.setTitle(title);
		diag.setContentText(text);
		Optional<String> result = diag.showAndWait();
		if(result.isPresent()==false) {
			return;
		}
		try {
			int val = (int)(Float.valueOf(result.get()) * (float)scale);
			prop.set(val);
			exec_gui(dev,String.format("%s=%d\r",cmd,val));
		}catch(NumberFormatException exp) {				
		}
	}
	
	public static Pane genPanel(final DevDCG100 dev) {
		
		final ToggleGroup grp1= new ToggleGroup();
		final ToggleGroup grp2= new ToggleGroup();
		
		final JFXRadioButton[] rad = {
			new JFXRadioButton ("電壓"),
			new JFXRadioButton ("電流"),
			new JFXRadioButton ("功率"),
			new JFXRadioButton ("固定"),
			new JFXRadioButton ("間歇"),
			new JFXRadioButton ("焦耳"),
		};
		for(JFXRadioButton obj:rad) {
			obj.setMaxWidth(Double.MAX_VALUE);
			GridPane.setHgrow(obj,Priority.ALWAYS);
		}
		rad[0].setToggleGroup(grp1);
		rad[1].setToggleGroup(grp1);
		rad[2].setToggleGroup(grp1);
		rad[3].setToggleGroup(grp2);
		rad[4].setToggleGroup(grp2);
		rad[5].setToggleGroup(grp2);
		
		rad[0].setOnAction(e->exec_gui(dev,"CHL=V\r"));
		rad[1].setOnAction(e->exec_gui(dev,"CHL=A\r"));
		rad[2].setOnAction(e->exec_gui(dev,"CHL=W\r"));
		rad[3].setOnAction(e->exec_gui(dev,"CHT=C\r"));
		rad[4].setOnAction(e->exec_gui(dev,"CHT=CT\r"));
		rad[5].setOnAction(e->exec_gui(dev,"CHT=CJ\r"));
		//-------------------------------------//
				
		final JFXButton[] btn = new JFXButton[9];
		for(int i=0; i<btn.length; i++) {
			btn[i] = new JFXButton(); 			
			btn[i].setMaxWidth(Double.MAX_VALUE);
		}
		
		btn[0].setText("預備時間");
		btn[0].setOnAction(e->set_value(
			dev,"SPR",
			"Ramp-Time","預備訊號時間(ms)",
			1, dev.v_spt
		));
		
		btn[1].setText("額定電壓");
		btn[1].visibleProperty().bind(rad[0].selectedProperty());
		btn[1].setOnAction(e->set_value(
			dev,"SPV",
			"額定電壓","",
			10, dev.v_spv
		));
		btn[2].setText("額定電流");
		btn[2].visibleProperty().bind(rad[1].selectedProperty());
		btn[2].setOnAction(e->set_value(
			dev,"SPA",
			"額定電流","",
			100, dev.v_spa
		));
		btn[3].setText("額定功率");
		btn[3].visibleProperty().bind(rad[2].selectedProperty());
		btn[3].setOnAction(e->set_value(
			dev,"SPW",
			"額定功率","",
			1, dev.v_spw
		));
				
		btn[4].setText("間歇時間");
		btn[4].visibleProperty().bind(rad[4].selectedProperty());
		btn[4].setOnAction(e->set_value(
			dev,"SPT",
			"Run-Time","維持輸出時間(ms)",
			1, dev.v_spt
		));
		
		btn[5].setText("額定焦耳");		
		btn[5].visibleProperty().bind(rad[5].selectedProperty());
		btn[5].setOnAction(e->set_value(
			dev,"SPJ",
			"Joules-Shutdown","維持總輸出功率(Joules)",
			1, dev.v_spj
		));
		
		btn[6].setText("ON");		
		btn[6].getStyleClass().add("btn-raised-1");
		btn[6].setOnAction(e->exec_gui(dev,"TRG\r"));
		
		btn[7].setText("OFF");		
		btn[7].getStyleClass().add("btn-raised-1");
		btn[7].setOnAction(e->exec_gui(dev,"OFF\r"));
		
		btn[8].setText("REM-");	
		btn[8].getStyleClass().add("btn-raised-2");		
		btn[8].setOnAction(e->{
			if(dev.isRemote.get()==true) {
				exec_gui(dev,"REMF\r");//change to local
			}else {
				exec_gui(dev,"REME\r");//change to remote
			}
		});
		
		final Label[] txt = new Label[6];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
		}
		txt[1].visibleProperty().bind(rad[0].selectedProperty());
		txt[2].visibleProperty().bind(rad[1].selectedProperty());
		txt[3].visibleProperty().bind(rad[2].selectedProperty());		
		txt[4].visibleProperty().bind(rad[4].selectedProperty());
		txt[5].visibleProperty().bind(rad[5].selectedProperty());
		
		txt[0].textProperty().bind(dev.v_spr.asString("%d ms"));//ramp-time
		txt[0].setOnMouseClicked(e->btn[0].getOnAction().handle(null));
		
		txt[1].textProperty().bind(dev.v_spv.multiply(0.1f).asString("%.1f V"));
		txt[1].setOnMouseClicked(e->btn[1].getOnAction().handle(null));
		
		txt[2].textProperty().bind(dev.v_spa.multiply(0.01f).asString("%.2f A"));
		txt[2].setOnMouseClicked(e->btn[2].getOnAction().handle(null));
		
		txt[3].textProperty().bind(dev.v_spw.asString("%d W"));
		txt[3].setOnMouseClicked(e->btn[3].getOnAction().handle(null));
		
		txt[4].textProperty().bind(dev.v_spt.asString("%d ms"));//run-time shutdown
		txt[4].setOnMouseClicked(e->btn[4].getOnAction().handle(null));
		
		txt[5].textProperty().bind(dev.v_spj.asString("%d kJ"));//joules shutdown
		txt[5].setOnMouseClicked(e->btn[5].getOnAction().handle(null));
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad-inner");
		lay1.addColumn(0, new Label("調變訊號"), rad[0], rad[1], rad[2]);
		lay1.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
		lay1.addColumn(2, new Label("輸出模式"), rad[3], rad[4], rad[5]);

		final GridPane lay2 = new GridPane();
		lay2.getStyleClass().addAll("box-pad-inner");
		lay2.add(btn[0], 0, 0, 1, 1); lay2.add(txt[0], 1, 0, 1, 1);
		lay2.add(btn[1], 0, 1, 1, 1); lay2.add(txt[1], 1, 1, 1, 1);
		lay2.add(btn[2], 0, 1, 1, 1); lay2.add(txt[2], 1, 1, 1, 1);
		lay2.add(btn[3], 0, 1, 1, 1); lay2.add(txt[3], 1, 1, 1, 1);
		lay2.add(btn[4], 0, 2, 1, 1); lay2.add(txt[4], 1, 2, 1, 1);
		lay2.add(btn[5], 0, 2, 1, 1); lay2.add(txt[5], 1, 2, 1, 1);

		lay1.setDisable(true);
		lay2.setDisable(true);
		btn[6].setDisable(true);
		btn[7].setDisable(true);
		
		dev.isRemote.addListener((obv,oldVal,newVal)->{
			
			lay1.setDisable(!newVal);
			lay2.setDisable(!newVal);
			btn[6].setDisable(!newVal);
			btn[7].setDisable(!newVal);
			
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
		});
		
		final VBox lay0 = new VBox();
		lay0.getStyleClass().addAll("box-pad");
		lay0.getChildren().addAll(
			lay1,
			new Separator(),
			lay2,
			new Separator(),
			btn[6],btn[7],btn[8]
		);
		return lay0;
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
