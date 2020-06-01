package prj.scada;

import java.util.Optional;

import com.sun.glass.ui.Application;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;
import narl.itrc.PadTouch;
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
		set(
			op_1, op_2, op_3, 
			op_5, op_6, 
			op_7, op_8
		);
		//StringProperty[] dat = sqm.filmData;
		//args[0].setText(dat[1].get());
		//args[1].setText(dat[2].get());
		//args[2].setText(dat[3].get());
	}
	
	private final static String init_txt = "厚度監控";
	private Label msg1 = new Label(init_txt);
	private Label msg2 = new Label("");
	
	private TextField[] args = {
		new TextField(),//density
		new TextField(),//tooling
		new TextField(),//z-factor
		new TextField(),//final thickness
		new TextField(),//thickness set-point
		new TextField(),//Time set-point
		new TextField(),//sensor number
	};
	
	long tick_beg = -1L, tick_end = -1L;
	
	final Runnable op_1 = ()->{
		//set film data and final thickness
		waiting_async();
		msg1.setText("設定參數");
		msg2.setText("");
		final String[] vals = {
			args[0].getText().trim(),//density
			args[1].getText().trim(),//tooling
			args[2].getText().trim(),//z-factor
			args[3].getText().trim(),//final thickness
			args[4].getText().trim(),//thickness set-point(kA)
			args[5].getText().trim(),//time set-point (mm:ss)
			args[6].getText().trim(),//active sensor
		};
		sqm.asyncBreakIn(()->{
			//reset film data, include its tooling and final thick
			String cmd = String.format(
				"A%C%s %s %s %s %s %s %s %s",
				(char)(1+48), "TEMPFILM", 
				vals[0], vals[1], vals[2], vals[3], 
				vals[4], vals[5], vals[6]
			);
			if(sqm.exec(cmd).charAt(0)!='A') {
				result.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法設定薄膜參數!!"));
			}
			result.set(NEXT);
		});
	};

	final Runnable op_2 = ()->{
		waiting_async();
		msg1.setText("選取薄膜");
		msg2.setText("");
		tick_beg = tick_end = -1L;
		sqm.asyncBreakIn(()->{
			if(sqm.exec("D1").charAt(0)!='A') {
				result.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法使用薄膜參數!!"));
				return;
			}
			result.set(NEXT);
		});
	};
	final Runnable op_3 = ()->{
		//open shutter
		waiting_async();
		msg1.setText("開啟擋板");
		msg2.setText("");
		sqm.asyncBreakIn(()->{
			//reset film data, include its tooling and final thick
			if(sqm.exec("S").charAt(0)!='A'){
				Misc.logw("[SQM160] 無法重設厚度，速率.");
			}
			if(sqm.exec("T").charAt(0)!='A'){
				Misc.logw("[SQM160] 無法重設時間.");
			}
			if(sqm.exec("U1").charAt(0)=='A') {
				result.set(NEXT);
				tick_beg = System.currentTimeMillis();
			}else{
				result.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法開啟擋板!!"));
			}
		});
	};
	
	final Runnable op_5 = ()->{
		//monitor shutter
		msg1.setText("監控中");
		msg2.setText(String.format(
			"%6.3f%s",
			sqm.thick[0].get(), sqm.unitThick.get()
		));
		if(sqm.shutter.get()==false){
			result.set(NEXT);
			tick_end = System.currentTimeMillis();
		}else{
			result.set(HOLD);
		}
	};
	
	final Runnable op_6 = ()->{
		//extinguish plasma
		waiting_async();
		msg1.setText("關閉高壓");
		msg2.setText("");
		dcg.asyncBreakIn(()->{
			if(dcg.exec("OFF").endsWith("*")==false) {
				result.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法點燃!!"));
				return;
			}
			result.set(NEXT);
		});
	};
	
	final Runnable op_7 = ()->{
		int vv = (int)dcg.volt.get();
		int ww = (int)dcg.watt.get();
		if(vv>=30 && ww>=1){
			result.set(HOLD);
		}else{
			result.set(NEXT);
		}
		msg1.setText("放電中");
		msg2.setText(String.format("%3dV %3dW",vv,ww));
	};
	
	final Runnable op_8 = ()->{
		msg1.setText("濺鍍時間:");
		msg2.setText(Misc.tick2time(tick_end-tick_beg,true));
	};
	
	
	private void select_film(){
		PadTouch pad = new PadTouch("薄膜編號:",'N');
		Optional<String> opt = pad.showAndWait();
		if(opt.isPresent()==false) {
			return;
		}
		final char id = (char)(48+Integer.valueOf(opt.get()));
		sqm.asyncBreakIn(()->{
			String txt = sqm.exec(String.format("A%c?", id));
			if(txt.charAt(0)=='A'){
				//restore film data~~~
				Application.invokeAndWait(()->{
					String[] val = sqm.parse_a_value(txt);
					for(int i=0; i<args.length; i++){
						args[i].setText(val[i+1]);
					}
					return;
				});
			}else{
				//show message~~~
				Application.invokeAndWait(()->PanBase.notifyError("錯誤","無法讀取薄膜資料!!"));
			}				
		});
	}
	
	@Override
	public Node getContent(){
		
		msg1.setPrefWidth(150);
		msg2.setPrefWidth(150);
		for(TextField box:args){
			box.setMaxWidth(80);
		}
		
		Button btn = new Button("選取參數");		
		btn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		btn.setOnAction(e->select_film());
		//GridPane.setHgrow(btn, Priority.ALWAYS);
		//GridPane.setVgrow(btn, Priority.ALWAYS);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, msg1, msg2);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
		lay.addColumn(2, 
			new Label("密度"), args[0], 
			new Label("Z 因子"), args[2]
		);
		lay.addColumn(3, 
			new Label("Tooling"), args[1],
			new Label("感測器編號"), args[6]
		);
		lay.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 4);
		lay.addColumn(5, new Label("最終厚度(kÅ)"), args[3]);
		lay.add(btn, 5, 2, 1, 2);
		
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	
	private static final String TAG0 = "density";
	private static final String TAG1 = "tooling";
	private static final String TAG2 = "z-factor";
	private static final String TAG3 = "final-thick";
	private static final String TAG4 = "set-thick";
	private static final String TAG5 = "set-time";
	private static final String TAG6 = "sensor-bit";
	@Override
	public String flatten() {
		return String.format(
			"%s:%s,  %s:%s,  %s:%s,  %s:%s,  %s:%s", 
			TAG0, args[0].getText(),
			TAG2, args[2].getText(),
			TAG1, args[1].getText(),
			TAG6, args[6].getText(),
			TAG3, args[3].getText()
		);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\s]+[:][^:,]+[,]?[\\s]*)+")==false){
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
				args[5].setText(val);
			}else if(tag.equals(TAG6)==true){
				args[6].setText(val);
			}
		}
	}
}
