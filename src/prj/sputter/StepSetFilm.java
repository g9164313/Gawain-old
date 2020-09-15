package prj.sputter;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import narl.itrc.Gawain;
import narl.itrc.PadTouch;
import narl.itrc.PanBase;
import narl.itrc.Stepper;

public class StepSetFilm extends Stepper {

	private DevSQM160 sqm;
	
	public StepSetFilm(
		final DevSQM160 dev
	){
		sqm = dev;
		set(op_1,op_2,op_3);
	}
	
	private static final String TAG0 = "density";
	private static final String TAG1 = "tooling";
	private static final String TAG2 = "z-factor";
	private static final String TAG3 = "final-thick";
	private static final String TAG4 = "set-thick";
	private static final String TAG5 = "set-time";
	private static final String TAG6 = "sensor-bit";
	
	private final static String init_txt = "薄膜設定";
	private Label msg1 = new Label(init_txt);

	private TextField[] args = {
		new TextField(),//density
		new TextField(),//tooling
		new TextField(),//z-factor
		new TextField(),//final thickness
		new TextField(),//thickness set-point
		new TextField(),//Time set-point
		new TextField(),//sensor number
	};
	
	public final TextField boxZFactor = args[2]; 
	
	final Runnable op_1 = ()->{
		//set film data and final thickness		
		waiting_async();
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
				(char)(1+48), "__TEMP__", 
				vals[0], vals[1], vals[2], vals[3], 
				"0.000", "0", vals[6]
			);
			if(sqm.exec(cmd).charAt(0)!='A') {
				next.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法設定薄膜參數!!"));
				return;
			}
			next.set(LEAD);
		});
	};
	
	final Runnable op_2 = ()->{	
		waiting_async();
		msg1.setText("切換中");
		sqm.asyncBreakIn(()->{
			if(sqm.exec("D1").charAt(0)!='A') {
				next.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法使用薄膜參數!!"));
				return;
			}
			next.set(LEAD);
		});
	};
	
	final Runnable op_3 = ()->{
		msg1.setText(init_txt);
		double val = Double.valueOf(args[3].getText().trim());//final thickness
		val = val + 0.01;
		LayGauges.setRateMax(val/2.);
		LayGauges.setThickMax(val);
	};
	
	private void select_film(){
		PadTouch pad;
		Optional<String> opt;
		String combo = Gawain.prop().getProperty("FILM", "");
		if(combo.length()==0) {
			//select by ID
			pad = new PadTouch('N',"薄膜編號:");
			opt = pad.showAndWait();
		}else {
			//select by readable name
			pad = new PadTouch(combo+",ID編號:id");
			opt = pad.showAndWait();
			if(opt.get().equals("id")==true) {
				pad = new PadTouch('N',"薄膜編號:");
				opt = pad.showAndWait();
			}
		}
		if(opt.isPresent()==false) {
			return;
		}		
		active_film(Integer.valueOf(opt.get()));
	}
	private void active_film(final int ID) {
		final char id = (char)(48+ID);
		sqm.asyncBreakIn(()->{
			String txt = sqm.exec(String.format("A%c?", id));
			if(txt.charAt(0)=='A'){
				//restore film data~~~
				Application.invokeAndWait(()->{
					String[] val = sqm.parse_a_value(txt);
					if(val==null){
						return;
					}
					for(int i=0; i<args.length; i++){
						args[i].setText(val[i+1]);
					}
				});
			}else{
				Application.invokeAndWait(()->PanBase.notifyError("錯誤","無法讀取薄膜資料!!"));
			}				
		});
	}
		
	@Override
	public Node getContent(){
		msg1.setPrefWidth(150);
		for(TextField box:args){
			box.setMaxWidth(80);
		}
		
		JFXButton btn = new JFXButton("選取");		
		btn.setMaxSize(80, Double.MAX_VALUE);
		btn.setOnAction(e->select_film());
		btn.getStyleClass().add("btn-raised-1");
		//GridPane.setHgrow(btn, Priority.ALWAYS);
		//GridPane.setVgrow(btn, Priority.ALWAYS);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, msg1);
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
	public void eventEdit(){
		select_film();
	}
	
	@Override
	public String flatten() {
		return String.format(
			"%s:%s,  %s:%s,  %s:%s,  %s:%s,  %s:%s,  %s:%s,  %s:%s", 
			TAG0, args[0].getText(),
			TAG1, args[1].getText(),
			TAG2, args[2].getText(),
			TAG3, args[3].getText(),
			TAG4, args[4].getText(),
			TAG5, args[5].getText(),
			TAG6, args[6].getText()
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
