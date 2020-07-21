package prj.sputter;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRadioButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PadTouch;

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
		readTimeout = 100;
	}

	public DevDCG100(String path_name){
		this();
		setPathName(path_name);
	}
	
	private final static String STG_INIT = "init";
	private final static String STG_MONT = "monitor";
	
	private void state_init() {
		String ans;
		//ans = exec("REP");
		//Misc.logv(ans);
		v_spr = cook(exec("SPR"),"5.000");
		v_spv = cook(exec("SPV"),"100.0V");
		v_spa = cook(exec("SPA"),"0.01A");
		v_spw = cook(exec("SPW"),"32W");
		v_spt = cook(exec("SPT"),"30.000");
		v_spj = cook(exec("SPJ"),"0");
		ans = cook(exec("CHL"),"W");
		if(ans.length()==1) {
			v_chl = ans.charAt(0);
		}
		v_cht = cook(exec("CHT"),"C");
		//change to HOST control
		//ans = exec("REM1");
		//change to remote control
		ans = exec("REME");
		if(ans.contains("*")==true) {			
			nextState(STG_MONT);
			Application.invokeLater(()->isRemote.set(true));
		}else {
			nextState("");//idle!!!
		}
	}	
	private void state_monitor() {
		try { 
			Thread.sleep(500); 
		}catch(InterruptedException e) { 
			return;
		}
		measurement();
	}
	private void measurement() {
		final String[] val = {"","","",""};
		val[0] = cook(exec("MVV"),"");
		val[1] = cook(exec("MVA"),"");
		val[2] = cook(exec("MVW"),"");
		val[3] = cook(exec("MVJ"),"");
		Application.invokeLater(()->{
			txt2prop(val[0], volt);
			txt2prop(val[1], amps);
			txt2prop(val[2], watt);
			txt2prop(val[3], joul);
		});
		//Misc.logv("V=%s A=%s W=%s J=%s", 
		//	val[0], val[1], val[2], val[3]);
	}
	protected void afterOpen() {
		addState(STG_INIT, ()->state_init()).
		addState(STG_MONT, ()->state_monitor());
		playFlow(STG_INIT);
	}
	
	public final BooleanProperty isRemote = new SimpleBooleanProperty(false);
	
	public final FloatProperty volt = new SimpleFloatProperty(0.f);
	public final FloatProperty amps = new SimpleFloatProperty(0.f);
	public final FloatProperty watt = new SimpleFloatProperty(0.f);
	public final FloatProperty joul = new SimpleFloatProperty(0.f);
		
	public String exec(String txt) {
		if(txt.endsWith("\r")==false) {
			txt = txt + "\r";
		}
		//writeTxt(txt);
		writeTxtDelay(10,txt);
		
		txt = ""; //clear command~~~
		int ans = 0;
		int cnt = 0;
		do{
			ans = readByte();
			if(cnt>=50){
				return "?";
			}
			if(ans==0){
				cnt+=1;
				continue;
			}
			if((ans&0x80)!=0){
				cnt+=1;
				continue;
			}
			txt = txt + (char)ans;
		}while(!(ans=='*' || ans=='?'));
		txt = txt
			.replace("\r\n", "\n")
			.replace("\n\r", "\n")
			.trim();
		return txt;
	}
	private String cook(
		final String responseText,
		final String defaultValue
	) {
		int len = responseText.length();
		if(len==0) {
			return "";
		}
		if(responseText.charAt(len-1)!='*') {
			return defaultValue;
		}
		int pos = responseText.indexOf('\n');
		if(pos<0) {
			return defaultValue;
		}
		return responseText.substring(pos,len-1).trim();
	}
	private void txt2prop(
		final String txt,
		final FloatProperty prop,
		final float scale
	) {
		if(txt.length()==0) {
			return;
		}
		try {
			float val = Float.valueOf(txt);
			val = val * scale;
			prop.set(val);
		}catch(NumberFormatException e) {
			Misc.loge("[%s]:wrong format--> %s", TAG, txt);
		}
	}
	private void txt2prop(
		final String txt,
		final FloatProperty prop
	) {
		txt2prop(txt,prop,1f);
	}
	
	public void asyncExec(final String cmd) {asyncBreakIn(()->{
		final String res = exec(cmd);
		if(res.endsWith("*")==true) {
			return;
		}
		Application.invokeAndWait(()->{
			Alert alt = new Alert(AlertType.ERROR);
			alt.setTitle("!!錯誤的回應!!");
			alt.setHeaderText(cmd+":"+res);
			alt.setContentText(null);
			alt.showAndWait();
		});
	});}	
	public void asyncExec(
		final String cmd,
		final String val
	) {
		asyncExec(cmd+"="+val);
	}	
	//-------------------------//
	
	//regulation mode - Amps('A'), Volts('V'), Watt('W')
	private char v_chl = 'W';
	
	//control mode & shutdown time
	//Constant('C'), Sequence mode('S'),
	//Joules mode,  Time-shutdown mode
	private String v_cht = "C";
	
	private String v_spr = "";//爬升時間(sec <--> mm:ss)
	private String v_spv = "";//額定電壓
	private String v_spa = "";//額定電流
	private String v_spw = "";//額定功率
	private String v_spt = "";//輸出時間(sec <--> mm:ss)
	private String v_spj = "";//輸出焦耳
	
	private static String set_value(
		final DevDCG100 dev,
		final String title,
		final String value,
		final String cmd,
		final String fmt,
		final float scale
	) {
		PadTouch pad = new PadTouch(title,value,'f');
		Optional<String> val = pad.showAndWait();
		if(val.isPresent()==false) {
			return value;
		}
		String _val = val.get();
		String _mod = String.format(
			fmt,
			Float.valueOf(_val) * scale
		);
		dev.asyncExec(cmd, _mod);
		return _val;
	}
	private static String set_value(
		final DevDCG100 dev,
		final String title,
		final String value,
		final String cmd
	) {
		PadTouch pad = new PadTouch(title,value,'n');
		Optional<String> val = pad.showAndWait();
		if(val.isPresent()==false) {
			return value;
		}
		String _val = val.get();
		dev.asyncExec(cmd, _val);
		return _val;
	}	
	private static String set_millisec(
		final DevDCG100 dev,
		final String value,
		final String cmd
	) {
		PadTouch pad = new PadTouch("時間(mm:ss)",'c');
		Optional<String> val = pad.showAndWait();
		if(val.isPresent()==false) {
			return value;
		}
		//change value to millisecond
		String _val = val.get();
		dev.asyncExec(
			cmd, 
			PadTouch.toMillsec(_val)
		);
		return _val;
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
		rad[0].setOnAction(e->dev.asyncExec("CHL=V"));
		rad[1].setOnAction(e->dev.asyncExec("CHL=A"));
		rad[2].setOnAction(e->dev.asyncExec("CHL=W"));
		rad[3].setOnAction(e->dev.asyncExec("CHT=C"));
		rad[4].setOnAction(e->dev.asyncExec("CHT=CT"));
		rad[5].setOnAction(e->dev.asyncExec("CHT=CJ"));
		//-------------------------------------//
		
		final Label[] txt = new Label[6];
		final JFXButton[] btn = new JFXButton[8];
		
		for(int i=0; i<btn.length; i++) {
			btn[i] = new JFXButton(); 			
			btn[i].setMaxWidth(Double.MAX_VALUE);
		}
		btn[0].setText("爬升時間");
		btn[0].setOnAction(e->{
			dev.v_spr = set_millisec(dev,dev.v_spr,"SPR");
			txt[0].setText(dev.v_spr);
		});

		btn[1].setText("額定電壓(V)");
		btn[1].visibleProperty().bind(rad[0].selectedProperty());
		btn[1].setOnAction(e->{
			dev.v_spv = set_value(dev,"額定電壓(V)",dev.v_spv,"SPV","%.1f",10.f);
			txt[1].setText(dev.v_spv);
		});

		btn[2].setText("額定電流(A)");
		btn[2].visibleProperty().bind(rad[1].selectedProperty());
		btn[2].setOnAction(e->{
			dev.v_spa = set_value(dev,"額定電壓(A)",dev.v_spa,"SPA","%.2f",100.f);
			txt[2].setText(dev.v_spa);
		});

		btn[3].setText("額定功率(W)");
		btn[3].visibleProperty().bind(rad[2].selectedProperty());
		btn[3].setOnAction(e->{
			dev.v_spw = set_value(dev,"額定功率(W)",dev.v_spw,"SPW");
			txt[3].setText(dev.v_spw);
		});

		btn[4].setText("輸出時間");
		btn[4].visibleProperty().bind(rad[4].selectedProperty());
		btn[0].setOnAction(e->{
			dev.v_spt = set_millisec(dev,dev.v_spr,"SPT");
			txt[4].setText(dev.v_spt);
		});

		btn[5].setText("輸出焦耳(J)");		
		btn[5].visibleProperty().bind(rad[5].selectedProperty());
		btn[5].setOnAction(e->{
			dev.v_spj = set_value(dev,"輸出焦耳(J)",dev.v_spj,"SPJ");
			txt[5].setText(dev.v_spt);
		});

		btn[6].setText("ON");		
		btn[6].getStyleClass().add("btn-raised-1");
		btn[6].setOnAction(e->dev.asyncExec("TRG"));

		btn[7].setText("OFF");		
		btn[7].getStyleClass().add("btn-raised-2");
		btn[7].setOnAction(e->dev.asyncExec("OFF"));
				
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
			EventHandler<?> hh = btn[i].getOnAction();
			txt[i].setOnMouseClicked(e->hh.handle(null));
		}
		txt[1].visibleProperty().bind(rad[0].selectedProperty());
		txt[2].visibleProperty().bind(rad[1].selectedProperty());
		txt[3].visibleProperty().bind(rad[2].selectedProperty());		
		txt[4].visibleProperty().bind(rad[4].selectedProperty());
		txt[5].visibleProperty().bind(rad[5].selectedProperty());
		//-------------------------------------//
		
		dev.isRemote.addListener((obv,oldVal,newVal)->{
			txt[0].setText(dev.v_spr);//SPR
			txt[1].setText(dev.v_spv);//SPV
			txt[2].setText(dev.v_spa);//SPA
			txt[3].setText(dev.v_spw);//SPW
			txt[4].setText(dev.v_spt);//SPT
			txt[5].setText(dev.v_spj);//SPJ
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
		
		final VBox lay0 = new VBox();
		lay0.disableProperty().bind(dev.isRemote.not());
		lay0.getStyleClass().addAll("box-pad");
		lay0.getChildren().addAll(
			lay1,
			new Separator(),
			lay2,
			new Separator(),
			btn[6],
			btn[7]
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
