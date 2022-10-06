package prj.sputter;

import java.util.concurrent.TimeUnit;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TextSize;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import narl.itrc.Misc;

/**
 * ADAM4117: 8 analogy-input channel 
 * @author qq
 *
 */
public class DevAdam4x17 extends DevAdam {

	public DevAdam4x17(final int address) {
		TAG= "ADAM4x17";
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
	//--------------------------------
	
	void state_initial() {
		get_configuration();
		init_range_type(ain);
		for(Channel ch:ain) {
			get_range_type(ch);
		}	
		nextState(STG_MONT);
	}
	
	void state_monitor() {
		if(port.isPresent()==false) {
			nextState("");//we have no port, just go into idle~~~~
			return;
		}
		try {
			read_all_analog_input();
			TimeUnit.MILLISECONDS.sleep(25);
		} catch (InterruptedException e) {
		}
		nextState(STG_MONT);
	}
	//--------------------------------
	
	//there are 8 analogy-input channel in ADAM4117
	public final Channel[] ain = {
		new Channel(0), new Channel(1), new Channel(2), new Channel(3),
		new Channel(4), new Channel(5), new Channel(6), new Channel(7),
	};
		
	private void read_all_analog_input() {
		final String ans = exec("#"+AA);
		if(ans.startsWith("?")==true) {
			Misc.logw("[%s] unable read all inputs", TAG);
			return;
		}
		//response message is like below:
		//>+00.000+00.000+00.000+00.000+00.000+00.000+00.000+00.000
		for(Channel ch:ain) {
			final int off = 1 + 7 * ch.id;
			if((off+7)>ans.length()) {				
				//What is going on??
				Misc.loge("invalid - %s", ans);
				break;
			}
			final String txt = ans.substring(off, off+7);
			final float val = Misc.txt2float(txt);
			Misc.invokeLater(()->{
				ch.txt.setValue(txt);
				ch.val.setValue(val);
			});
		}
	}

	@SuppressWarnings("unused")
	private void set_addr_baud(final int addr, final int baud) {
		final String pv_cc = CC_baud_rate.get(baud);
		if(pv_cc==null) { return; }
		CC = pv_cc;
		set_configuration(String.format("%02X", addr%256));
	}	
	@SuppressWarnings("unused")
	private void set_baud_rate(final int baud) {
		final String pv_cc = CC_baud_rate.get(baud);
		if(pv_cc==null) { return; }
		CC = pv_cc;
		set_configuration(null);
	}
	
	//-------------------------------------------
	
	public static Pane genPanel(final DevAdam4x17 dev) {
		
		final Tile[] gag = new Tile[8];
		
		for(DevAdam4x17.Channel ch:dev.ain) {

			Tile tile = TileBuilder.create()
				.skinType(SkinType.SPARK_LINE)
				.textSize(TextSize.BIGGER)
				.decimals(3)
				.autoScale(false)//影響最大，最小值的 property binding
				.build();
			
			tile.titleProperty().bind(ch.title);
			tile.valueProperty().bind(ch.val);
			tile.minValueProperty().bind(ch.min);
			tile.maxValueProperty().bind(ch.max);
			tile.unitProperty().bind(ch.unit);

			tile.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			GridPane.setHgrow(tile, Priority.ALWAYS);
			GridPane.setVgrow(tile, Priority.ALWAYS);
			
			gag[ch.id] = tile;
		}

		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addRow(0, gag[0], gag[1], gag[2], gag[3]);
		lay.addRow(1, gag[4], gag[5], gag[6], gag[7]);
		return lay;
	}	
}
