package prj.scada;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.DevTTY;

/**
 * DCG Dual 5kW
 * DC Plasma Generator
 * Support RS-232 interface
 * @author qq
 *
 */
public class DevDCG100 extends DevTTY {
	
	public DevDCG100(){
		TAG = "DevDCG-stream";		
		act_mvv.setLoop(true).setDelay(100L);
		act_mva.setLoop(true).setDelay(100L);
		act_mvw.setLoop(true).setDelay(100L);
		act_mvj.setLoop(true).setDelay(100L);
		//setHook(this);
	}

	public DevDCG100(String path_name){
		this();
		setPathName(path_name);
	}

	@Override
	public boolean open() {
		boolean flag = super.open();
		if(flag==true) {
			take(act_mvv);
			take(act_mva);
			take(act_mvw);
		}
		return flag;
	}
	
	private String trim_data(String txt) {
		int idx = txt.indexOf("\n\r");
		if(idx>=0) {
			txt = txt.substring(idx+2);
		}
		idx = txt.indexOf("\r\n");
		if(idx>=0) {
			txt = txt.substring(0,idx);
		}
		txt = txt.trim();
		return txt;
	}
	
	public final FloatProperty volt = new SimpleFloatProperty(0.f);
	public final FloatProperty amps = new SimpleFloatProperty(0.f);
	public final FloatProperty watt = new SimpleFloatProperty(0.f);
	public final FloatProperty joul = new SimpleFloatProperty(0.f);
	
	private DevTTY.Action act_mvv = new DevTTY.Action()
		.writeData("MVV\n\r")
		.readHead("MVV")
		.readTail("\r\n*")
		.setHook((act,txt)->{		
		try {
			txt = trim_data(txt);
			final float val = Float.valueOf(txt);
			Application.invokeAndWait(()->volt.set(val));
		}catch(NumberFormatException e) {			
		}		
	});
	private DevTTY.Action act_mva = new DevTTY.Action()
		.writeData("MVA\n\r")
		.readHead("MVA")
		.readTail("\r\n*")
		.setHook((act,txt)->{
		try {
			txt = trim_data(txt);
			final float val = Float.valueOf(txt);
			Application.invokeAndWait(()->amps.set(val));
		}catch(NumberFormatException e) {			
		}		
	});
	private DevTTY.Action act_mvw = new DevTTY.Action()
		.writeData("MVW\n\r")
		.readHead("MVW")
		.readTail("\r\n*")
		.setHook((act,txt)->{
		try {
			txt = trim_data(txt);
			final float val = Float.valueOf(txt);
			Application.invokeAndWait(()->watt.set(val));
		}catch(NumberFormatException e) {			
		}		
	});
	private DevTTY.Action act_mvj = new DevTTY.Action()
		.writeData("MVJ\n\r")
		.readHead("MVJ")
		.readTail("\r\n*")
		.setHook((act,txt)->{
		try {
			txt = trim_data(txt);
			final float val = Float.valueOf(txt);
			Application.invokeAndWait(()->joul.set(val));
		}catch(NumberFormatException e) {
		}
	});
	
	private DevTTY.Action act_trg = new DevTTY.Action()
		.writeData("TRG\n\r")
		.readTail("\r\n*");
	
	private DevTTY.Action act_off = new DevTTY.Action()
		.writeData("OFF\n\r")
		.readTail("\r\n*");
	
	public void trigger(
		final boolean flag, 
		final int monitor
	) {
		if(flag==true) {
			take(act_trg);
		}else {
			take(act_off);
		}
	}
	//-------------------------//
	
	public static Pane genPanel(final DevDCG100 dev) {
		
		//final TextField[] box = new TextField[2];
		
		final Label[] txt = new Label[4];
		for(int i=0; i<4; i++) {
			txt[i] = new Label();
			txt[i].setMaxWidth(Double.MAX_VALUE);			
			GridPane.setFillWidth(txt[i], true);
		}		
		txt[0].textProperty().bind(dev.volt.asString("%.2f"));
		txt[1].textProperty().bind(dev.amps.asString("%.2f"));
		txt[2].textProperty().bind(dev.watt.asString("%.2f"));
		txt[3].textProperty().bind(dev.joul.asString("%.2f"));
		
		final JFXCheckBox[] chk = new JFXCheckBox[4];
		for(int i=0; i<chk.length; i++) {
			chk[i] = new JFXCheckBox();
			chk[i].setMaxWidth(Double.MAX_VALUE);
			GridPane.setFillWidth(chk[i], true);
		}
		chk[0].setText("電壓(V)：");
		chk[1].setText("電流(A)：");
		chk[2].setText("瓦特(W)：");
		chk[3].setText("焦耳(J)：");
		
		chk[2].setSelected(true);//default show reading-value
		
		final JFXToggleButton tgl = new JFXToggleButton();		
		tgl.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(tgl, true);
		tgl.setText("DC 開關");
		tgl.setOnAction(e->{
			int monitor = 0;
			for(int i=0; i<chk.length; i++) {
				if(chk[i].isSelected()==true) { 
					monitor = monitor | (1<<i);
				}
			}			
			dev.trigger(tgl.isSelected(),monitor);
		});
		
		final JFXButton btnRamp = new JFXButton("Ramp Time");
		btnRamp.getStyleClass().add("btn-raised-2");
		btnRamp.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(btnRamp, true);
		btnRamp.setOnAction(e->{
			
		});
		
		final JFXButton btn = new JFXButton("test");
		btn.getStyleClass().add("btn-raised-2");
		btn.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(btn, true);
		btn.setOnAction(e->{
			//new PanTTY(dev).appear();
		});
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("ground-pad");
		lay.addRow(0, chk[0], txt[0]);
		lay.addRow(1, chk[1], txt[1]);
		lay.addRow(2, chk[2], txt[2]);
		lay.addRow(3, chk[3], txt[3]);
		lay.add(tgl, 0, 4, 3, 1);
		lay.add(btnRamp, 0, 5, 3, 1);
		lay.add(btn, 0, 6, 3, 1);
		return lay;
	}
}
