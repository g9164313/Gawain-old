package prj.scada;

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
			op_3,op_4,op_5,
			op_6,op_7,op_8
		);
	}
	
	private final String init_text = "點火程序";
	
	private Label msg1 = new Label(init_text);
	private Label msg2 = new Label();

	private TextField[] args = {
		new TextField("50"),//T_on +
		new TextField("50"),//T_off+
		new TextField("50"),//T_on -
		new TextField("50"),//T_off-
		new TextField("100"),//DCG power
		new TextField("5:00"),//clean time
	};
	
	final Runnable op_1 = ()->{
		//close shutter~~~
		waiting_async();
		msg1.setText("關閉擋板");
		msg2.setText("");
		sqm.asyncBreakIn(()->{
			if(sqm.exec("U0").charAt(0)=='A') {
				result.set(NEXT);
			}else{
				result.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法控制擋板!!"));
			}
		});
	};
	final Runnable op_2 = ()->{
		msg1.setText("等待中");
		msg2.setText(String.format(
			"%s",
			Misc.tick2time(waiting(3000),true)
		));
	};
	
	final Runnable op_3 = ()->{
		waiting_async();
		msg1.setText("設定Ton-");
		msg2.setText("");
		final int neg_on = Integer.valueOf(args[2].getText().trim());
		spk.asyncBreakIn(()->{
			spk.set_register(6, neg_on );
			result.set(NEXT);
		});
	};
	
	final Runnable op_4 = ()->{
		waiting_async();
		msg1.setText("設定Toff-");
		msg2.setText("");
		final int neg_off= Integer.valueOf(args[3].getText().trim());
		spk.asyncBreakIn(()->{
			spk.set_register(7, neg_off);
			result.set(NEXT);
		});
		
	};
	final Runnable op_5 = ()->{
		waiting_async();
		msg1.setText("設定High-Pin");
		msg2.setText("");
		spk.asyncBreakIn(()->{
			spk.set_register(1, 2);//high-pin
			result.set(NEXT);
		});
	};
	
	final Runnable op_6 = ()->{
		//set power for plasma~~
		waiting_async();
		msg1.setText("設定功率");
		msg2.setText("");
		final int w1 = Integer.valueOf(args[4].getText().trim());		
		dcg.asyncBreakIn(()->{
			if(dcg.exec("SPW="+w1).endsWith("*")==false) {
				result.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法設定功率!!"));
				return;
			}
			if(dcg.exec("TRG").endsWith("*")==false) {
				result.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法點燃!!"));
				return;
			}
			result.set(NEXT);
		});
	};
	final Runnable op_7 = ()->{
		//Check power reach the goal
		final int w1 = Integer.valueOf(args[4].getText().trim());
		final int w2 = (int)dcg.watt.get();
		msg1.setText("輸出中 ");
		msg2.setText(String.format("%3d Watt",w2));
		if(Math.abs(w1-w2)>2){
			result.set(HOLD);
		}else{
			result.set(NEXT);
		}		
	};

	
	final Runnable op_8 = ()->{
		//wait for clean shutter
		long tt = waiting(args[5].getText());
		if(tt>=10){
			msg1.setText("清洗中");
			msg2.setText(String.format(
				"%s",
				Misc.tick2time(tt,true)
			));
		}else{
			msg1.setText(init_text);
			msg2.setText("");
		}
	};
	
	@Override
	protected Node getContent(){
		
		msg1.setPrefWidth(150);
		msg2.setPrefWidth(150);
		for(TextField box:args){
			box.setMaxWidth(90);
		}
				
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.add(msg1, 0, 0);
		lay.add(msg2, 0, 1);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
		lay.addColumn(2, 
			//new Label("Ton +"), arg[0],
			new Label("Ton -"), args[2]
		);
		lay.addColumn(3,
			//new Label("Toff+"), arg[1],
			new Label("Toff-"), args[3]
		);
		lay.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 4);	
		lay.addColumn(5, 
			new Label("輸出功率(Watt)"), args[4]
		);
		lay.addColumn(6, 
			new Label("清洗時間(m:s)"), args[5]
		);
		return lay;
	}
	
	
	private static final String TAG0 = "Ton+";
	private static final String TAG1 = "Toff+";
	private static final String TAG2 = "Ton-";
	private static final String TAG3 = "Toff-";
	private static final String TAG4 = "power";
	private static final String TAG5 = "clean";
	@Override
	public String flatten() {
		return String.format(
			"%s:%s,  %s:%s,  %s:%s,  %s:%s, ", 
			TAG2, args[2].getText().trim(),
			TAG3, args[3].getText().trim(),
			TAG4, args[4].getText().trim(),
			TAG5, args[5].getText().trim().replace(':','#')
		);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\s]+[:][^:,]+[,]?[\\s]*)+")==false){
			Misc.loge("pasing fail");
			return;
		}
		String[] arg = txt.split(":|,");
		for(int i=0; i<arg.length; i+=2){
			final String tag = arg[i+0].trim();
			final String val = arg[i+1].trim();
			if(tag.equals(TAG0)==true){
				args[0].setText(val);
			}else if(tag.equals(TAG1)==true){
				args[1].setText(val);
			}else if(tag.equals(TAG2)==true){
				args[2].setText(val);
			}else if(tag.equals(TAG3)==true){
				args[3].setText(val);
			}else if(tag.equals(TAG4)==true){
				args[4].setText(val);				
			}else if(tag.equals(TAG5)==true){
				args[5].setText(val.replace('#',':'));
			}
		}
	}
}
