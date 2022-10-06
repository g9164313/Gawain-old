package prj.sputter;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.sun.glass.ui.Application;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.Misc;


/**
 * ADAM-4068 8-channel Relay Output Module.<p>
 * Relay type is a-contact(normal open, working is closed).<p>
 * @author qq
 *
 */
public class DevAdam4068 extends DevAdam {

	public DevAdam4068(int address) {
		TAG= "ADAM4055";
		AA = String.format("%02X", address);
	}
	
	private final static String STG_MONT = "monitor";
	@Override
	public void afterOpen() {
		addState(STG_MONT, ()->state_monitor());
		playFlow(STG_MONT);
	}
	@Override
	public void beforeClose() {
	}
	//--------------------------------------------
	
	/**
	 * True(1) - relay closing.<p>
	 * False(0)- relay opening.<p>
	 */
	private SimpleBooleanProperty[] flg = {
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty()
	};
	public final ReadOnlyBooleanProperty[] Relay = {
		flg[0],flg[1],flg[2],flg[3],
		flg[4],flg[5],flg[6],flg[7],
	};
	
	void state_monitor() {
		nextState(STG_MONT);
		if(port.isPresent()==false) {
			nextState("");//we have no port, just go into idle~~~~
			return;
		}
		try {
			refresh_flag(flg);
			TimeUnit.MILLISECONDS.sleep(100);
		} catch (InterruptedException e) {
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
		asyncBreakIn(()->set_flag(idx,flg));
	}	
	
	public void asyncSetAllRelay(final Boolean... flg) {
		final boolean[] _flg = override_flag(Relay,flg);
		asyncBreakIn(()->assign_val(flag2val(_flg)));
	}
	//--------------------------------------------
	
	public static Pane genPanel(final DevAdam4068 dev) {
		
		final Pin[] chk = {
			new Pin(dev.flg,0), new Pin(dev.flg,1), new Pin(dev.flg,2), new Pin(dev.flg,3),
			new Pin(dev.flg,4), new Pin(dev.flg,5), new Pin(dev.flg,6), new Pin(dev.flg,7),
		};
		
		for(Pin pin:chk) {
			pin.setOnMouseClicked(e->{
				final boolean flg = pin.getValue();				
				Optional<ButtonType> opt = new Alert(
						AlertType.CONFIRMATION,
						(flg)?("確認是否斷路?"):("確認是否導通?")
					).showAndWait();
				if(opt.isPresent()==true) {
					dev.asyncSetRelay(pin.idx, !flg);
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
