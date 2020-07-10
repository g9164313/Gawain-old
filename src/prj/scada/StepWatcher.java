package prj.scada;

import com.sun.glass.ui.Application;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.Stepper;

public class StepWatcher extends Stepper {

	private DevSQM160 sqm;
	private DevDCG100 dcg;
		
	public StepWatcher(
		final DevSQM160 dev1,
		final DevDCG100 dev2 
	){
		sqm = dev1;
		dcg = dev2;
		set(op_1,op_2,op_3,op_4,op_5,op_6);
	}
	
	private final static String init_txt = "厚度監控";
	private Label msg1 = new Label(init_txt);
	private Label msg2 = new Label("");

	private Label msg3 = new Label("");
	private Label msg4 = new Label("");
	private Label msg5 = new Label("");
	
	long tick_beg = -1L, tick_end = -1L;
	
	final Runnable op_1 = ()->{
		//open shutter
		waiting_async();
		msg1.setText("歸零");
		msg2.setText("");
		sqm.asyncBreakIn(()->{
			//reset film data, include its tooling and final thick
			/*while(sqm.exec("U1").charAt(0)!='A'){
				Misc.logw("[SQM160] 無法開啟擋板.");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			while(sqm.exec("S").charAt(0)!='A'){
				Misc.logw("[SQM160] 無法重設厚度，速率.");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			while(sqm.exec("T").charAt(0)!='A'){
				Misc.logw("[SQM160] 無法重設時間.");
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}*/
			try {
				sqm.exec("S");
				Thread.sleep(250);
				sqm.exec("T");
				Thread.sleep(250);
				sqm.exec("U1");
			} catch (InterruptedException e) {
			}
			tick_beg = System.currentTimeMillis();
			next.set(LEAD);
		});
	};
	final Runnable op_2 = ()->{
		msg1.setText("等待檔板");
		msg2.setText(String.format(
			"%s",
			Misc.tick2time(waiting(5000),true)
		));
	};
	final Runnable op_3 = ()->{
		//monitor shutter
		tick_end = System.currentTimeMillis();
		
		msg1.setText("監控中");
		msg2.setText("");
		
		msg3.setText(String.format(
			"%5.3f%s",
			sqm.rate[0].get(), sqm.unitRate.get()
		));
		msg4.setText(String.format(
			"%5.3f%s",
			sqm.thick[0].get(), sqm.unitThick.get()
		));	
		msg5.setText(Misc.tick2time(tick_end-tick_beg,true));
			
		if(sqm.shutter.get()==false){
			next.set(LEAD);
		}else{
			next.set(HOLD);
		}
	};
	final Runnable op_4 = ()->{
		//extinguish plasma		
		msg1.setText("關閉高壓");
		msg2.setText("");
		
		waiting_async();
		dcg.asyncBreakIn(()->{
			if(dcg.exec("OFF").endsWith("*")==false) {
				next.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法點燃!!"));
				return;
			}
			next.set(LEAD);
		});
	};
	
	final Runnable op_5 = ()->{
		int vv = (int)dcg.volt.get();
		int ww = (int)dcg.watt.get();
		if(vv>=30 && ww>=1){
			next.set(HOLD);
		}else{
			next.set(LEAD);
		}
		msg1.setText("放電中");
		msg2.setText(String.format("%3dV %3dW",vv,ww));
	};
	
	final Runnable op_6 = ()->{
		msg1.setText(init_txt);
		msg2.setText("");
		
		msg5.setText(Misc.tick2time(tick_end-tick_beg,true));
	};
	
	@Override
	public Node getContent(){
		msg1.setPrefWidth(150);
		msg2.setPrefWidth(150);
		
		msg3.setMinWidth(100);
		msg4.setMinWidth(100);
		msg4.setMinWidth(100);
		
		//msg2.textProperty().bind(sqm.thick[0].get());
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, msg1);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
		lay.addColumn(2,new Label("速率"),msg3);
		lay.addColumn(3,new Label("厚度"),msg4);
		lay.addColumn(4,new Label("時間"),msg5);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return "";
	}
	@Override
	public void expand(String txt) {
	}
}