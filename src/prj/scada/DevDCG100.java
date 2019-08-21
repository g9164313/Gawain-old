package prj.scada;

import java.util.Optional;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
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
/**
 * @author qq
 *
 */
public class DevDCG100 extends DevTTY {
	
	public DevDCG100(){
		TAG = "DevDCG-stream";		
		act_mvv.setRepeat(-1).setDelay(100L);
		act_mva.setRepeat(-1).setDelay(100L);
		act_mvw.setRepeat(-1).setDelay(100L);
		act_mvj.setRepeat(-1).setDelay(100L);
	}

	public DevDCG100(String path_name){
		this();
		setPathName(path_name);
	}
	
	@Override
	protected void afterOpen() {
		take(act_reme);
		take(act_read_spr);
		take(act_read_spt);
		take(act_read_spw);
		take(act_read_spa);
		take(act_mvv);
		take(act_mva);
		take(act_mvw);
	}
	
	private String trim_data(String txt) {
		int idx = txt.indexOf(0x0D);
		if(idx>0) {
			txt = txt.substring(idx+1);
		}
		idx = txt.lastIndexOf(0x0D);
		if(idx>0) {
			txt = txt.substring(0,idx);
		}
		return txt.trim();
	}
	
	public final FloatProperty volt = new SimpleFloatProperty(0.f);
	public final FloatProperty amps = new SimpleFloatProperty(0.f);
	public final FloatProperty watt = new SimpleFloatProperty(0.f);
	public final FloatProperty joul = new SimpleFloatProperty(0.f);
	
	private DevTTY.Action act_mvv = new DevTTY.Action()
		.writeData("MVV\n\r")
		.indexOfData("MVV", "\r\n*", (act,txt)->{		
		try {
			txt = trim_data(txt);
			volt.set(Float.valueOf(txt));
		}catch(NumberFormatException e) {			
		}		
	});
	private DevTTY.Action act_mva = new DevTTY.Action()
		.writeData("MVA\n\r")
		.indexOfData("MVA", "\r\n*", (act,txt)->{
		try {
			txt = trim_data(txt);
			amps.set(Float.valueOf(txt));
		}catch(NumberFormatException e) {			
		}		
	});
	private DevTTY.Action act_mvw = new DevTTY.Action()
		.writeData("MVW\n\r")
		.indexOfData("MVW", "\r\n*", (act,txt)->{
		try {
			txt = trim_data(txt);
			watt.set(Float.valueOf(txt));
		}catch(NumberFormatException e) {			
		}		
	});
	private DevTTY.Action act_mvj = new DevTTY.Action()
		.writeData("MVJ\n\r")
		.indexOfData("MVJ", "\r\n*", (act,txt)->{
		try {
			txt = trim_data(txt);
			joul.set(Float.valueOf(txt));
		}catch(NumberFormatException e) {
		}
	});
	
	private DevTTY.Action act_reme = new DevTTY.Action()
		.writeData("REME\n\r")
		.indexOfData("\r\n*", null);
	
	@SuppressWarnings("unused")
	private DevTTY.Action act_remf = new DevTTY.Action()
		.writeData("REMF\n\r")
		.indexOfData("\r\n*", null);
	
	private DevTTY.Action act_trg = new DevTTY.Action()
		.writeData("TRG\n\r")
		.indexOfData("\r\n*", null);
	
	private DevTTY.Action act_off = new DevTTY.Action()
		.writeData("OFF\n\r")
		.indexOfData("\r\n*", null);
	
	public void trigger(final boolean flag) {
		if(flag==true) {
			take(act_trg);
		}else {
			take(act_off);
		}
	}
	
	//value for ramp-time
	public final FloatProperty spr = new SimpleFloatProperty();
	//value for run-time
	public final FloatProperty spt = new SimpleFloatProperty();
	//value for Watts set-point, but it is integer value!!
	public final FloatProperty spw = new SimpleFloatProperty();
	//value for Amps set-point 
	public final FloatProperty spa = new SimpleFloatProperty();
	
	private DevTTY.Action act_read_spr = new DevTTY.Action()
		.writeData("SPR\n\r")
		.indexOfData("\r\n*", (act,txt)->{
			txt = trim_data(txt);
			spr.set(Float.valueOf(txt));
		});
	private DevTTY.Action act_read_spt = new DevTTY.Action()
		.writeData("SPT\n\r")
		.indexOfData("\r\n*", (act,txt)->{
			txt = trim_data(txt);
			spt.set(Float.valueOf(txt));
		});
	private DevTTY.Action act_read_spw = new DevTTY.Action()
		.writeData("SPW\n\r")
		.indexOfData("\r\n*", (act,txt)->{
			txt = trim_data(txt);
			txt = txt.substring(0, txt.length()-1);
			spw.set(Integer.valueOf(txt));
		});
	private DevTTY.Action act_read_spa = new DevTTY.Action()
		.writeData("SPA\n\r")
		.indexOfData("\r\n*", (act,txt)->{
			txt = trim_data(txt);
			txt = txt.substring(0, txt.length()-1);
			spa.set(Float.valueOf(txt));
		});
	
	public void setRampTime(final float val) {
		set_value("SPR",val,true);
	}
	public void setRunTime(final float val) {
		set_value("SPT",val,true);
	}
	private void setPointWatt(final int val) {
		set_value("SPW",(float)val, false);
	}
	private void setPointAmps(final float val) {
		set_value("SPA",val*100.f, false);
	}
	private void set_value(
		final String cmd, 
		final float val,
		final boolean deci_point
	) {
		final String ex_cmd = (deci_point==true)?
			(String.format("%s=%.3f\n\r",cmd, val)):
			(String.format("%s=%.0f\n\r",cmd, val));
		take(new DevTTY.Action()
			.writeData(ex_cmd)
			.indexOfData("\r\n*",(act,txt)->{
				if(cmd.startsWith("SPR")==true) {
					spr.set(val);
				}else if(cmd.startsWith("SPT")==true) {
					spt.set(val);
				}else if(cmd.startsWith("SPW")==true) {
					spw.set(val);
				}else if(cmd.startsWith("SPA")==true) {
					spa.set(val*0.01f);
				}
			})
		);
	}
	//-------------------------//
	
	public static Pane genPanel(final DevDCG100 dev) {
		
		//final TextField[] box = new TextField[2];
		
		final DevTTY.Action[] act_m = {
			dev.act_mvv, 
			dev.act_mva, 
			dev.act_mvw, 
			dev.act_mvj
		};
		final JFXCheckBox[] chk = new JFXCheckBox[4];
		for(int i=0; i<chk.length; i++) {
			chk[i] = new JFXCheckBox();
			chk[i].setUserData(act_m[i]);
			chk[i].setOnAction(e->{
				JFXCheckBox obj = (JFXCheckBox)e.getSource();
				DevTTY.Action act = (DevTTY.Action)obj.getUserData();
				if(obj.isSelected()==true) {
					dev.take(act);
				}else {
					dev.abort(act);
				}
			});
			chk[i].setMaxWidth(Double.MAX_VALUE);
			GridPane.setFillWidth(chk[i], true);			
		}
		chk[0].setText("電壓(V)：");chk[0].setSelected(true);
		chk[1].setText("電流(A)：");chk[1].setSelected(true);
		chk[2].setText("瓦特(W)：");chk[2].setSelected(true);
		chk[3].setText("焦耳(J)：");

		
		final Label[] txt = new Label[12];
		for(int i=0; i<txt.length; i++) {
			txt[i] = new Label();
			//txt[i].setStyle("-fx-font-size: 1.7em;");
			txt[i].setMaxWidth(Double.MAX_VALUE);	
			GridPane.setFillWidth(txt[i], true);
		}		
		txt[0].textProperty().bind(dev.volt.asString("%.2f"));
		txt[1].textProperty().bind(dev.amps.asString("%.2f"));
		txt[2].textProperty().bind(dev.watt.asString("%.2f"));
		txt[3].textProperty().bind(dev.joul.asString("%.2f"));
		txt[4].setText("\u22c4Ramp：");
		txt[5].textProperty().bind(dev.spr.asString("%.3f sec"));
		txt[6].setText("\u22c4Run Time：");		
		txt[7].textProperty().bind(dev.spt.asString("%.3f sec"));
		txt[8].setText("\u22c4額定功率：");
		txt[9].textProperty().bind(dev.spw.asString("%.0f W"));
		txt[10].setText("\u22c4額定電流：");
		txt[11].textProperty().bind(dev.spa.asString("%.2f A"));

		final Alert altFormat = new Alert(AlertType.ERROR);
		altFormat.setTitle("錯誤！！");
		altFormat.setHeaderText("錯誤的資料格式");
		
		final TextInputDialog diaTime = new TextInputDialog();
		
		txt[4].setOnMouseClicked(e->{
			diaTime.getEditor().setText(String.format("%.3f",dev.spr.getValue()));
			diaTime.setTitle("設定");
			diaTime.setHeaderText("Ramp Time - 預備時間");
			diaTime.setContentText("時間(sec)");
			Optional<String> res = diaTime.showAndWait();
			if(res.isPresent()) {
				try {
					dev.setRampTime(Float.valueOf(res.get()));
				}catch(NumberFormatException exp) {
					altFormat.showAndWait();
				}
			}
		});		
		txt[6].setOnMouseClicked(e->{
			diaTime.getEditor().setText(String.format("%.3f",dev.spt.getValue()));
			diaTime.setTitle("設定");
			diaTime.setHeaderText("Run Time - 執行時間");
			diaTime.setContentText("時間(sec)");
			Optional<String> res = diaTime.showAndWait();
			if(res.isPresent()) {
				try {
					dev.setRunTime(Float.valueOf(res.get()));
				}catch(NumberFormatException exp) {
					altFormat.showAndWait();
				}
			}
		});
		txt[8].setOnMouseClicked(e->{
			diaTime.getEditor().setText(String.format("%.0f",dev.spw.getValue()));
			diaTime.setTitle("設定");
			diaTime.setHeaderText("額定輸出功率");
			diaTime.setContentText("功率(W)");
			Optional<String> res = diaTime.showAndWait();
			if(res.isPresent()) {
				try {
					dev.setPointWatt(Integer.valueOf(res.get()));
				}catch(NumberFormatException exp) {
					altFormat.showAndWait();
				}
			}
		});
		txt[10].setOnMouseClicked(e->{
			diaTime.getEditor().setText(String.format("%.2f",dev.spa.getValue()));
			diaTime.setTitle("設定");
			diaTime.setHeaderText("額定輸出電流");
			diaTime.setContentText("安培(A)");
			Optional<String> res = diaTime.showAndWait();
			if(res.isPresent()) {
				try {
					dev.setPointAmps(Float.valueOf(res.get()));
				}catch(NumberFormatException exp) {
					altFormat.showAndWait();
				}
			}
		});
		
		final JFXToggleButton tgl = new JFXToggleButton();
		tgl.setText("DC 開關");
		tgl.setOnAction(e->{			
			dev.trigger(tgl.isSelected());
		});
		
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("ground-pad");
		lay1.addColumn(0, chk);
		lay1.addColumn(1, txt[0], txt[1], txt[2], txt[3]);
		lay1.addRow(4, txt[4] ,txt[5]);
		lay1.addRow(5, txt[6] ,txt[7]);
		lay1.addRow(6, txt[8] ,txt[9]);
		lay1.addRow(7, txt[10],txt[11]);
		lay1.addRow(8, tgl);
		return lay1;
	}
}
