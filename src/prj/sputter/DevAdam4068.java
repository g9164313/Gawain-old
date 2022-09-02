package prj.sputter;

import java.util.Optional;

import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.Misc;


/**
 * ADAM-4068 8 channel Relay module.<p>
 * Relay type is a-contact(normal open, working is closed).<p>
 * @author qq
 *
 */
public class DevAdam4068 extends DevAdam {

	public DevAdam4068(int address) {
		TAG= "ADAM4055";
		AA = String.format("%02X", address);
	}
	
	private final static String STG_INIT = "init";
	
	@Override
	public void afterOpen() {
		addState(STG_INIT, ()->state_initial());
		playFlow(STG_INIT);
	}

	@Override
	public void beforeClose() {
	}
	//--------------------------------------------
	
	/**
	 * True(1) - relay closing.<p>
	 * False(0)- relay opening.<p>
	 */
	private SimpleBooleanProperty[] sta = {
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty()
	};
	
	void state_initial() {
		final String ans = exec("$"+AA+"6");
		if(ans.matches("![0123456789ABCDEF]{4}?00")==true) {
			final boolean[] bit_o = int2flag(ans.substring(1, 3));			
			Application.invokeLater(()->{
				for(int i=0; i<8; i++) {
					sta[i].set(bit_o[i]);
				}
			});
		}else {
			Misc.logw("[%s] invalid: $AA6 --> %s", TAG, ans);
		}
	}
	
	/**
	 * Type-a context, true for closing, false for opening<p>
	 * @param ch - Relay index
	 * @param flg - type-a contact
	 */
	public void asyncSetRelay(int idx, boolean flg) {
		if(idx>=8 || idx<0) { 
			Misc.logw("[%s] invalid index:%d",TAG,idx);
			return; 
		}
		asyncBreakIn(()->{
			final String val = (flg)?("01"):("00");
			final String ans = exec("#"+AA+"1"+idx+val);
			if(ans.charAt(0)=='>') {
				return;//action is success!!!!
			}
			Misc.loge("[%s] fail to set relay(%d): %s", TAG, idx, ans);
	});}	
	
	public void asyncSetRelayAll(Boolean... arg) {asyncBreakIn(()->{
		final int cnt = arg.length % 8;
		int val = 0;
		for(int i=0; i<cnt; i++) {
			if(arg[i]==null) {
				continue;
			}
			if(arg[i]==true) {
				val = val | (1<<i);
			}				
		}
		final String ans = exec("#"+AA+"00"+String.format("%02X", val));
		if(ans.charAt(0)=='>') {
			return;//action is success!!!!
		}
		Misc.loge("[%s] fail to set all relay: %s(0x%02X)", TAG, ans, val);
	});}
	//--------------------------------------------
	
	public static Pane genPanel(final DevAdam4068 dev) {
		
		final Pin[] chk = {
			new Pin(dev.sta,0), new Pin(dev.sta,1), new Pin(dev.sta,2), new Pin(dev.sta,3),
			new Pin(dev.sta,4), new Pin(dev.sta,5), new Pin(dev.sta,6), new Pin(dev.sta,7),
		};
		
		for(Pin pin:chk) {
			pin.setOnMouseClicked(e->{
				final boolean flg = pin.getValue();
				final String txt = (flg)?("斷路?"):("導通?");				
				Alert dia = new Alert(AlertType.CONFIRMATION,txt);
				Optional<ButtonType> opt = dia.showAndWait();
				if(opt.isPresent()==true) {
					dev.asyncSetRelay(pin.cid, !flg);
				}				
			});
		}
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addRow(0,
			new Label("RL0"), chk[0], 
			new Label("RL1"), chk[1], 
			new Label("RL2"), chk[2],
			new Label("RL3"), chk[3]
		);
		lay.addRow(1,
			new Label("RL4"), chk[4], 
			new Label("RL5"), chk[5], 
			new Label("RL6"), chk[6],
			new Label("RL7"), chk[7]
		);
		return lay;
	}	
}
