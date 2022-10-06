package prj.sputter;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.sun.glass.ui.Application;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.Misc;

/**
 * ADAM-4055 16 channel Isolated Digital I/O module.<p>
 * 8 channel for input(including dry and wet wiring).<p>
 * 8 channel for output(including dry and wet wiring).<p>
 * @author qq
 *
 */
public class DevAdam4055 extends DevAdam {

	public DevAdam4055(final int address) {
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

	private SimpleBooleanProperty[] flg_o = {
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
	};
	public final ReadOnlyBooleanProperty[] DO = {
		flg_o[0], flg_o[1], flg_o[2], flg_o[3],
		flg_o[4], flg_o[5], flg_o[6], flg_o[7],
	};
	
	private SimpleBooleanProperty[] flg_i = {
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
	};
	public final ReadOnlyBooleanProperty[] DI = {
		flg_i[0], flg_i[1], flg_i[2], flg_i[3],
		flg_i[4], flg_i[5], flg_i[6], flg_i[7],
	};
	
	void state_monitor() {
		nextState(STG_MONT);
		if(port.isPresent()==false) {
			nextState("");//we have no port, just go into idle~~~~
			return;
		}
		try {
			refresh_flag(flg_o,flg_i);
			TimeUnit.MILLISECONDS.sleep(25);
		} catch (InterruptedException e) {
		}
	}
	//--------------------------------------------

	public void asyncSetLevel(final int idx,final boolean flg) {		
		if(idx>=8 || idx<0) { 
			Misc.logw("[%s] invalid index:%d",TAG,idx);
			return; 
		}	
		asyncBreakIn(()->set_flag(idx,flg));
	}
	
	/**
	 * Assign Pin value again, if pin no changed, assign null.<p>
	 * Null will give pin a previous setting value.<p>
	 * @param flg - reassign pin value
	 */
	public void asyncSetAllLevel(final Boolean... flg) {
		final boolean[] _flg = override_flag(DO,flg);
		asyncBreakIn(()->assign_val(flag2val(_flg)));
	}	
	//--------------------------------------------
	
	public static Pane genPanel(final DevAdam4055 dev) {
		
		final Pin[] chk_do = {
			new Pin(dev.flg_o,0), new Pin(dev.flg_o,1), new Pin(dev.flg_o,2), new Pin(dev.flg_o,3),
			new Pin(dev.flg_o,4), new Pin(dev.flg_o,5), new Pin(dev.flg_o,6), new Pin(dev.flg_o,7),
		};
		final Pin[] chk_di = {
			new Pin(dev.flg_i,0), new Pin(dev.flg_i,1), new Pin(dev.flg_i,2), new Pin(dev.flg_i,3),
			new Pin(dev.flg_i,4), new Pin(dev.flg_i,5), new Pin(dev.flg_i,6), new Pin(dev.flg_i,7),
		};
		
		for(Pin pin:chk_do) {
			pin.setOnMouseClicked(e->{
				final boolean flg = pin.getValue();
				final String txt = (flg)?("關閉（set to low）"):("開啟（set to high）");				
				Alert dia = new Alert(AlertType.CONFIRMATION,txt);
				Optional<ButtonType> opt = dia.showAndWait();
				if(opt.isPresent()==true) {
					dev.asyncSetLevel(pin.idx, !flg);
				}				
			});
		}
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, new Label()   , new Label("DI"), new Label("DO"));
		lay.addColumn(1, new Label("0"), chk_di[0]      , chk_do[0]      );
		lay.addColumn(2, new Label("1"), chk_di[1]      , chk_do[1]      );
		lay.addColumn(3, new Label("2"), chk_di[2]      , chk_do[2]      );
		lay.addColumn(4, new Label("3"), chk_di[3]      , chk_do[3]      );
		lay.addColumn(5, new Label("4"), chk_di[4]      , chk_do[4]      );
		lay.addColumn(6, new Label("5"), chk_di[5]      , chk_do[5]      );
		lay.addColumn(7, new Label("6"), chk_di[6]      , chk_do[6]      );
		lay.addColumn(8, new Label("7"), chk_di[7]      , chk_do[7]      );
		return lay;
	}
}
