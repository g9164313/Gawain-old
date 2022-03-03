package prj.sputter.action;

import javafx.scene.control.Label;
import narl.itrc.Misc;
import narl.itrc.Stepper;
import prj.sputter.DevCouple;
import prj.sputter.DevDCG100;
import prj.sputter.DevSPIK2k;
import prj.sputter.DevSQM160;
import prj.sputter.LayLogger;

public abstract class Bumper extends Stepper {

	public static DevCouple coup;
	public static DevDCG100 dcg1;	
	public static DevSPIK2k spik;
	public static DevSQM160 sqm1;
	public static LayLogger logg;
	
	protected Label[] msg = {
		new Label(), new Label(), new Label(),
	};
		
	protected void set_mesg(final String... txt) {
		for(int i=0; i<msg.length; i++) {
			if(i>=txt.length) {
				msg[i].setText("");
			}else {
				msg[i].setText(txt[i]);
			}
		}
	}
	
	protected void print_info(final String TAG) {
		
		final float volt = dcg1.volt.get();		
		final float amps = dcg1.amps.get();				
		final int   watt = (int)dcg1.watt.get();
		
		final float rate = sqm1.meanRate.get();
		final String unit1 = sqm1.unitRate.get();
		
		final float high = sqm1.meanThick.get();
		final String unit2 = sqm1.unitThick.get();
		
		final float mfc1 = coup.PV_FlowAr.get();
		final float mfc2 = coup.PV_FlowN2.get();
		final float mfc3 = coup.PV_FlowO2.get();
		
		Misc.logv(
			"%s: %.3f V, %.3f A, %d W, "+
			"%.3f sccm, %.3f sccm, %.3f sccm, "+
			"%.3f %s, %.3f %s",
			TAG, 
			volt, amps, watt,
			mfc1, mfc2, mfc3,
			rate, unit1, high, unit2
		);
	}
}
