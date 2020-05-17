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

	private DevDCG100 dcg;
	private DevSQM160 sqm;
	
	public StepKindler(DevDCG100 dev1, DevSQM160 dev2){
		dcg = dev1;
		sqm = dev2;
		set(op_1,op_2,op_3,op_4,op_5,op_6);
	}
	
	private final String init_text = "點火程序";
	
	private Label msg1 = new Label(init_text);
	private Label msg2 = new Label();
	private TextField arg1 = new TextField("100");
	private TextField arg2 = new TextField("5:00");
	
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
			"剩餘  %s",
			Misc.tick2time(waiting(3000),true)
		));
	};
	
	final Runnable op_3 = ()->{
		//set power for plasma~~
		waiting_async();
		msg1.setText("設定功率");		
		final int val = Integer.valueOf(arg1.getText().trim());		
		dcg.asyncBreakIn(()->{
			if(dcg.exec("SPW="+val).endsWith("*")==false) {
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
	
	final Runnable op_4 = ()->{
		//Check power reach the goal
		final int w1 = Integer.valueOf(arg1.getText().trim());
		final int w2 = (int)dcg.watt.get();
		msg1.setText("輸出中 ");
		msg2.setText(String.format("%3d Watt",w2));
		if(Math.abs(w1-w2)>2){
			result.set(HOLD);
		}else{
			result.set(NEXT);
		}		
	};

	final Runnable op_5 = ()->{
		//wait for clean shutter
		msg1.setText("清洗中");
		msg2.setText(String.format(
			"剩餘  %s",
			Misc.tick2time(waiting(arg2.getText()),true)
		));
	};
	
	final Runnable op_6 = ()->{
		msg1.setText(init_text);
		msg2.setText("");
	};
	
	@Override
	protected Node getContent(){		
		
		msg1.setPrefWidth(100);
		msg2.setPrefWidth(100);
		arg1.setMaxWidth(100);
		arg2.setMaxWidth(100);
		
		//arg1.disableProperty().bind(arg2.selectedProperty().not());		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.add(msg1, 0, 0);
		lay.add(msg2, 0, 1);
		lay.addColumn(2, new Label("輸出功率(Watt)"), arg1);		
		lay.addColumn(4, new Label("清洗時間(mm:ss)"), arg2);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.add(new Separator(Orientation.VERTICAL), 3, 0, 1, 2);		
		return lay;
	}
}
