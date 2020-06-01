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
			op_3,
			op_4,op_5,
			op_6
		);
	}
	
	private final String init_text = "點火程序";
	
	private Label msg1 = new Label(init_text);
	private Label msg2 = new Label();

	private TextField[] args = {
		new TextField("100"),//DCG power
		new TextField("5:00"),//clean time
	};
	
	final Runnable op_1 = ()->{
		//close shutter~~~
		msg1.setText("關閉擋板");
		msg2.setText("");
		result.set(NEXT);
		waiting_async();
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
		msg1.setText("設定H-Pin");
		msg2.setText("");
		waiting_async();
		spk.asyncBreakIn(()->{
			spk.set_register(1, 2);//high-pin
			result.set(NEXT);
		});
	};
	
	final Runnable op_4 = ()->{
		//set power for plasma~~
		msg1.setText("設定功率");
		msg2.setText("");
		waiting_async();
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
	final Runnable op_5 = ()->{		
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
	
	final Runnable op_6 = ()->{
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
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);	
		lay.addColumn(2,
			new Label("輸出功率"), args[0]
		);
		lay.add(new Separator(Orientation.VERTICAL), 3, 0, 1, 2);
		lay.addColumn(4,
			new Label("清洗時間"), args[1]
		);
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
