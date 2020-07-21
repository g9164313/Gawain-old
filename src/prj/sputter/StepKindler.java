package prj.sputter;

import com.jfoenix.controls.JFXCheckBox;
import com.sun.glass.ui.Application;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.Stepper;

public class StepKindler extends Stepper {

	private DevSQM160 sqm;
	private DevDCG100 dcg;
	private DevSPIK2k spk;
	
	public StepKindler(
		final DevSQM160 dev1,
		final DevDCG100 dev2, 
		final DevSPIK2k dev3
	){
		sqm = dev1;
		dcg = dev2;
		spk = dev3;
		set(op_1,op_2,
			op_3,
			op_4,op_5,
			op_6,op_7
		);
	}
	
	private final String init_text = "點火程序";
	
	private Label msg1 = new Label(init_text);
	private Label msg2 = new Label();
	
	private JFXCheckBox chk_done = new JFXCheckBox("清洗後結束");
	
	private TextField[] args = {
		new TextField("100"),//DCG power
		new TextField("1:00"),//clean time
	};
	
	final Runnable op_1 = ()->{
		//close shutter~~~
		final String _txt = "關閉擋板";
		msg1.setText(_txt);
		msg2.setText("");
		Misc.logv(_txt);
		waiting_async();
		sqm.asyncBreakIn(()->{
			if(sqm.exec("U0").charAt(0)=='A') {
				next.set(LEAD);
			}else{
				next.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法控制擋板!!"));
			}
		});
	};
	final Runnable op_2 = ()->{		
		msg1.setText("等待檔板");
		msg2.setText(String.format(
			"%s",
			Misc.tick2time(waiting_time(5000),true)
		));
	};

	final Runnable op_3 = ()->{
		final String _txt = "設定H-Pin";
		msg1.setText(_txt);
		msg2.setText("");
		waiting_async();
		spk.asyncBreakIn(()->{
			spk.set_register(1, 2);//high-pin
			next.set(LEAD);
			Misc.logv(_txt);
		});
	};
	
	final Runnable op_4 = ()->{
		//set power for plasma~~
		final String _txt = "功率設定";
		msg1.setText(_txt);
		msg2.setText("");
		waiting_async();
		final int w1 = Integer.valueOf(args[0].getText().trim());		
		dcg.asyncBreakIn(()->{
			if(dcg.exec("SPW="+w1).endsWith("*")==false) {				
				next.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法設定功率!!"));
				Misc.logv(_txt+"失敗");
				return;
			}
			if(dcg.exec("TRG").endsWith("*")==false) {				
				next.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法點燃!!"));
				Misc.logv(_txt+"失敗");
				return;
			}
			next.set(1);
			Misc.logv(_txt);
		});
	};
	final Runnable op_5 = ()->{		
		//Check power reach the goal
		final int target = Integer.valueOf(args[0].getText().trim());
		
		final float volt = dcg.volt.get();
		final float amps = dcg.amps.get();		
		final int watt = (int)dcg.watt.get();
		final float rate = sqm.rate[0].get();
		final String unit_rate = sqm.unitRate.get();
		final float high = sqm.thick[0].get();
		final String unit_high = sqm.unitThick.get();
		
		final String _txt = "輸出中";
		msg1.setText(_txt);
		msg2.setText(String.format("%3d Watt",watt));
		Misc.logv(
			"%s: %.2f V, %.2f A, %d W, %.3f %s, %.3f %s",
			_txt, 
			volt, amps, watt,
			rate, unit_rate,
			high, unit_high
		);
		if(Math.abs(target-watt)>2){
			next.set(HOLD);
		}else{
			next.set(LEAD);
		}	
	};
	
	final Runnable op_6 = ()->{
		//wait for clean shutter
		final String _txt = "清洗中";
		
		final float volt = dcg.volt.get();
		final float amps = dcg.amps.get();		
		final int watt = (int)dcg.watt.get();
		final float rate = sqm.rate[0].get();
		final String unit_rate = sqm.unitRate.get();
		final float high = sqm.thick[0].get();
		final String unit_high = sqm.unitThick.get();
		
		msg1.setText(_txt);
		msg2.setText(String.format(
			"%s",
			Misc.tick2time(waiting_time(args[1].getText()),true)
		));
		Misc.logv(
			"%s: %.2f V, %.2f A, %d W, %.3f %s, %.3f %s",
			_txt, 
			volt, amps, watt,
			rate, unit_rate,
			high, unit_high
		);
	};
	final Runnable op_7 = ()->{
		msg1.setText(init_text);
		msg2.setText("");		
		Misc.logv("結束"+init_text);
		if(chk_done.isSelected()==true) {
			waiting_async();
			dcg.asyncBreakIn(()->{
				if(dcg.exec("OFF").endsWith("*")==false) {
					Misc.loge("DCG100 無法關閉");
				}
				next_work();
			});
		}else {
			next_work();
		}
	};
	
	@Override
	public Node getContent(){
		msg1.setPrefWidth(150);
		msg2.setPrefWidth(150);
		for(TextField box:args){
			box.setMaxWidth(80);
		}
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.add(msg1, 0, 0);
		lay.add(msg2, 0, 1);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);	
		lay.addColumn(2,new Label("輸出功率"), args[0]);
		lay.add(new Separator(Orientation.VERTICAL), 3, 0, 1, 3);
		lay.addColumn(4,new Label("清洗時間"), args[1], chk_done);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	
	private static final String TAG0 = "power";
	private static final String TAG1 = "clean";
	@Override
	public String flatten() {
		//trick, replace time format.
		//EX: mm:ss --> mm#ss
		return String.format(
			"%s:%s,  %s:%s, ",
			TAG0, args[0].getText().trim(),
			TAG1, args[1].getText().trim().replace(':','#')
		);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\s]+[:][^:,]+[,]?[\\s]*)+")==false){
			Misc.loge("pasing fail");
			return;
		}
		//trick, replace time format.
		//EX: mm#ss --> mm:ss
		String[] arg = txt.split(":|,");
		for(int i=0; i<arg.length; i+=2){
			final String tag = arg[i+0].trim();
			final String val = arg[i+1].trim();
			if(tag.equals(TAG0)==true){
				args[0].setText(val);
			}else if(tag.equals(TAG1)==true){
				args[1].setText(val.replace('#',':'));
			}
		}
	}
}
