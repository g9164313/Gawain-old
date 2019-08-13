package prj.scada;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;

import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.Misc;

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
			writeTxt("");
		}
		return flag;
	}
	

	private Task<Void> tskMonitor;
	
	public void trigger(final boolean flag) {
		if(flag==true) {
			if(tskMonitor!=null) {
				if(tskMonitor.isDone()==false) {
					return;
				}
			}
			writeTxt("TRG\n");
			tskMonitor = new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					do {
						if(tskMonitor.isDone()==true) {
							break;
						}
						Misc.delay(1000);
						Misc.logv("ggyy");
					}while(Gawain.isExit()==false);
					Misc.logv("done!!");
					return null;
				}
			};
			Thread thr = new Thread(tskMonitor,"DCG-monitor");
			thr.setDaemon(true);
			thr.start();
		}else {
			writeTxt("OFF\n");
			tskMonitor.cancel();
		}
	}
	
	//-------------------------//
	
	public static Pane genPanel(final DevDCG100 dev) {
		
		//final TextField[] box = new TextField[2];
		
		final Label[] txt = new Label[4];
		for(int i=0; i<4; i++) {
			txt[i] = new Label("？？？？");
			txt[i].setMaxWidth(Double.MAX_VALUE);			
			GridPane.setFillWidth(txt[i], true);
		}
		
		final JFXCheckBox[] chk = new JFXCheckBox[4];
		for(int i=0; i<chk.length; i++) {
			chk[i] = new JFXCheckBox();
			chk[i].setMaxWidth(Double.MAX_VALUE);
			GridPane.setFillWidth(chk[i], true);
		}
		chk[0].setText("電壓：");
		chk[1].setText("電流：");
		chk[2].setText("瓦特：");
		chk[3].setText("焦耳：");
		
		chk[2].setSelected(true);//default show reading-value
		
		final JFXToggleButton tgl = new JFXToggleButton();
		tgl.setText("DC 開關");
		tgl.setOnAction(e->dev.trigger(tgl.isSelected()));
		
		tgl.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(tgl, true);
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("ground-pad");
		lay.addRow(0, chk[0], txt[0]);
		lay.addRow(1, chk[1], txt[1]);
		lay.addRow(2, chk[2], txt[2]);
		lay.addRow(3, chk[3], txt[3]);
		lay.add(tgl, 0, 4, 3, 1);
		return lay;
	}
}
