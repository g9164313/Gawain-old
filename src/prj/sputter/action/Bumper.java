package prj.sputter.action;

import java.util.concurrent.TimeUnit;

import com.sun.glass.ui.Application;

import javafx.scene.control.Label;
import narl.itrc.Misc;
import narl.itrc.PanBase;
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
		for(int i=1; i<msg.length; i++) {
			if(i>=txt.length) {
				msg[i].setText("");
			}else {
				msg[i].setText(txt[i]);
			}
		}
		String inf = "["+txt[0]+"],";
		for(int i=1; i<txt.length; i++) {
			if(txt[i].length()==0) {
				continue;
			}
			inf = inf + txt[i] + ",";
		}
		Misc.logv(inf);
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
	//-------------------------------//
	
	protected final Runnable close_shutter = ()->{		
		final String tag = "關閉擋板";
		set_mesg(tag);
		wait_async();
		sqm1.shutter_and_zeros(false,()->{
			Misc.logv(tag);
			notify_async();
		}, ()->{
			Misc.logv(tag+"失敗");
			abort_step();
			Application.invokeLater(()->PanBase.notifyError("失敗", "無法控制擋板!!"));
		});
	};
	
	protected int t_on_pos = -1;
	protected int t_off_pos= -1;
	protected int t_on_neg = -1;
	protected int t_off_neg= -1;
	
	protected final Runnable spik_get_pulse = ()->{
		final String tag = "設定脈衝";
		set_mesg(tag);
		wait_async();
		spik.asyncGetRegister(tkn->{
			t_on_pos = tkn.values[0];
			t_off_pos= tkn.values[1];
			t_on_neg = tkn.values[2];
			t_off_neg= tkn.values[3];
			notify_async();
		}, 4, 4);
		hold_step();
	};
	
	protected final Runnable spik_apply_pulse = ()->{
		final String tag = "設定脈衝";
		set_mesg(tag);
		wait_async();
		spik.asyncSetRegister(tkn->{		
			notify_async();
		}, 4, t_on_pos, t_off_pos, t_on_neg, t_off_neg);
		hold_step();
	};
	
	protected final Runnable spik_running = ()->{
		final String tag = "啟動 H-Pin";
		set_mesg(tag);
		wait_async();
		spik.asyncSetRegister(tkn->{			
			notify_async();
		}, 1, 2);
		hold_step();
	};
	
	protected int dcg_power = -1;
	protected final Runnable turn_on = ()->{
		final String tag = "啟動 DCG";
		final int T_RISE = 3000;//3 sec
		final int T_STABLE = 60000*3;//3 min
		set_mesg(tag);
		wait_async();
		dcg1.asyncBreakIn(()->{
			if(dcg_power>0) {
				dcg1.exec("CHL=W");
				dcg1.exec("SPW="+dcg_power);
				dcg1.exec("SPR="+T_RISE);//unit is millisecond
			}
			{
				dcg1.exec("TRG");
				block_delay(T_RISE+T_STABLE);
			}
			notify_async();
		});
	};
	
	protected final Runnable turn_off = ()->{
		set_mesg("關閉高壓");
		wait_async();		
		dcg1.asyncBreakIn(()->{
			if(dcg1.exec("OFF").endsWith("*")==false) {
				abort_step();
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法關閉!!"));
			}else {
				notify_async();
			}
		});
	};
	
	protected final Runnable waiting = ()->{
		int vv = (int)dcg1.volt.get();
		int ww = (int)dcg1.watt.get();
		if(vv>=30 && ww>=1){
			hold_step();
		}else{
			next_step();
		}
		set_mesg(
			"放電中",
			String.format("%3dV %3dW",vv,ww)
		);
	};
	
	private void block_delay(int msec) {
		try {
			TimeUnit.MILLISECONDS.sleep(msec);
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}
	}
}
