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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
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
	
	private final static String STG_INIT = "init";
	private final static String STG_MONT = "monitor";
	
	@Override
	public void afterOpen() {
		addState(STG_INIT, ()->state_initial());
		addState(STG_MONT, ()->state_monitor());
		playFlow(STG_INIT);
	}

	@Override
	public void beforeClose() {
	}
	//--------------------------------------------
	
	void state_initial() {
		nextState(STG_MONT);
	}
	
	void state_monitor() {
		if(port.isPresent()==false) {
			nextState("");//we have no port, just go into idle~~~~
			return;
		}
		try {
			final String ans = exec("$"+AA+"6");
			if(ans.matches("![0123456789ABCDEF]{4}?00")==true) {
				final boolean[] bit_o = int2flag(ans.substring(1, 3));
				final boolean[] bit_i = int2flag(ans.substring(3, 5));				
				Application.invokeLater(()->{
					for(int i=0; i<8; i++) {
						sta[i].set(bit_o[i]);
					}
					for(int i=8; i<16; i++) {
						sta[i].set(bit_i[i-8]);
					}
				});
			}else {
				Misc.logw("[%s] invalid: $AA6 --> %s", TAG, ans);
			}
			TimeUnit.MILLISECONDS.sleep(100);
		} catch (InterruptedException e) {
		}
		nextState(STG_MONT);
	}
	//--------------------------------------------

	private SimpleBooleanProperty[] sta = {
		//state for output relay
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		//state for input relay
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
		new SimpleBooleanProperty(), new SimpleBooleanProperty(),
	};

	public final ReadOnlyBooleanProperty[] DO = {
		sta[0],sta[1],sta[2],sta[3],
		sta[4],sta[5],sta[6],sta[7],
	};
	public final ReadOnlyBooleanProperty[] DI = {
		sta[ 8],sta[ 9],sta[10],sta[11],
		sta[12],sta[13],sta[14],sta[15],
	};
	
	public void asyncSetLevel(final int idx,final boolean flg) {		
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
			Misc.loge("[%s] fail to set output(%d): %s", TAG, idx, ans);
		});
	}
	
	public void asyncSetLevelAll(final Boolean... flg) {
		asyncBreakIn(()->{
			final int cnt = flg.length % 8;
			int val = 0;
			for(int i=0; i<cnt; i++) {
				if(flg==null) {
					continue;
				}
				if(flg[i]==true) {
					val = val | (1<<i);
				}				
			}
			final String ans = exec("#"+AA+"00"+String.format("%02X", val));
			if(ans.charAt(0)=='>') {
				return;//action is success!!!!
			}
			Misc.loge("[%s] fail to set output: %s(0x%02X)", TAG, ans, val);
		});
	}
	//--------------------------------------------
	

	public static Pane genPanel(final DevAdam4055 dev) {
		
		final Pin[] chk_do = {
			new Pin(dev.sta,0), new Pin(dev.sta,1), new Pin(dev.sta,2), new Pin(dev.sta,3),
			new Pin(dev.sta,4), new Pin(dev.sta,5), new Pin(dev.sta,6), new Pin(dev.sta,7),
		};
		final Pin[] chk_di = {
			new Pin(dev.sta,8), new Pin(dev.sta,9), new Pin(dev.sta,10), new Pin(dev.sta,11),
			new Pin(dev.sta,12), new Pin(dev.sta,13), new Pin(dev.sta,14), new Pin(dev.sta,15),
		};
		
		for(Pin pin:chk_do) {
			pin.setOnMouseClicked(e->{
				final boolean flg = pin.getValue();
				final String txt = (flg)?("關閉（set to low）"):("開啟（set to high）");				
				Alert dia = new Alert(AlertType.CONFIRMATION,txt);
				Optional<ButtonType> opt = dia.showAndWait();
				if(opt.isPresent()==true) {
					dev.asyncSetLevel(pin.cid, !flg);
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
