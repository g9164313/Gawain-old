package prj.sputter.labor1;

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
import narl.itrc.Misc;
import narl.itrc.PadTouch;
import narl.itrc.PanBase;


public class StepSetFilmOld extends StepCommon {
	
	public StepSetFilmOld(){
		set(op_1,op_2,op_3);
	}
	
	private static final String TAG0 = "density";
	private static final String TAG1 = "tooling";
	private static final String TAG2 = "z-factor";
	private static final String TAG3 = "final-thick";
	private static final String TAG4 = "set-thick";
	private static final String TAG5 = "set-time";
	private static final String TAG6 = "sensor-bit";
	
	public final static String action_name = "薄膜設定";

	private TextField[] args = {
		new TextField(),//film name
		new TextField(),//density
		new TextField(),//tooling
		new TextField(),//z-ratio/factor
		new TextField(),//final thickness
		new TextField("0.000"),//thickness set-point
		new TextField("0"),//Time set-point
		new TextField(),//sensor number
	};
	public final TextField box_density = args[1];
	public final TextField box_tooling = args[2];
	public final TextField box_z_ratio = args[3];
	public final TextField box_f_thick = args[4];
	public final TextField box_s_thick = args[5];
	public final TextField box_s_time  = args[6];
	public final TextField box_sensor  = args[7];
	
	final Runnable op_1 = ()->{
		//set film data and final thickness		
		wait_async();
		final String[] vals = {
			args[1].getText().trim(),//density
			args[2].getText().trim(),//tooling
			args[3].getText().trim(),//z-factor
			args[4].getText().trim(),//final thickness
			args[5].getText().trim(),//thickness set-point(kA)
			args[6].getText().trim(),//time set-point (mm:ss)
			args[7].getText().trim(),//active sensor
		};
		sqm1.asyncBreakIn(()->{
			//reset film data, include its tooling and final thick
			String cmd = String.format(
				"A%C%s %s %s %s %s %s %s %s",
				(char)(1+48), "__TEMP__", 
				vals[0], vals[1], vals[2], vals[3], 
				vals[4], vals[5], vals[6]
			);
			if(sqm1.exec(cmd).charAt(0)!='A') {
				abort_step();
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法設定薄膜參數!!"));
				return;
			}
			notify_async();
		});
	};
	
	final Runnable op_2 = ()->{	
		wait_async();
		show_mesg("切換中");
		sqm1.asyncBreakIn(()->{
			if(sqm1.exec("D1").charAt(0)!='A') {
				abort_step();
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法使用薄膜參數!!"));
				return;
			}
			notify_async();
		});
	};
	
	final Runnable op_3 = ()->{
		show_mesg(action_name);
		float val = Float.valueOf(box_f_thick.getText().trim());//final thickness
		sqm1.maxThick.set(val+0.01f);
		next_step();
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
		sqm1.asyncBreakIn(()->{
			final String txt = sqm1.exec(String.format("A%c?", id));
			if(txt.charAt(0)=='A'){
				//restore film data to GUI box~~~
				Application.invokeAndWait(()->{
					String[] val = sqm1.split_a_text(txt);
					for(int i=0; i<args.length; i++){
						args[i].setText(val[i]);
					}
				});
			}else{
				Application.invokeAndWait(()->PanBase.notifyError("錯誤","無法讀取薄膜資料!!"));
			}				
		});
	}
		
	@Override
	public Node getContent(){
		show_mesg(action_name);
		for(TextField box:args){
			box.setMaxWidth(83);
		}
		
		JFXButton btn = new JFXButton("選取");		
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(e->select_film());
		btn.getStyleClass().add("btn-raised-1");
		//GridPane.setHgrow(btn, Priority.ALWAYS);
		//GridPane.setVgrow(btn, Priority.ALWAYS);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, msg[0], msg[1]);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 2);
		lay.addRow(0,
			new Label("密度"), box_density,
			new Label("Tooling"), box_tooling
		);
		lay.addRow(1,
			new Label("Z 因子"), box_z_ratio,
			new Label("感測器編號"), box_sensor
		);
		lay.add(new Separator(Orientation.VERTICAL), 6, 0, 1, 2);
		lay.addRow(0, new Label("最終厚度(kÅ)"), box_f_thick);
		lay.add(btn, 7, 1, 2, 1);
		
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
			TAG0, args[1].getText(),
			TAG1, args[2].getText(),
			TAG2, args[3].getText(),
			TAG3, args[4].getText(),
			TAG4, args[5].getText(),
			TAG5, args[6].getText(),
			TAG6, args[7].getText()
		);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\p{Space}]+[:]\\p{ASCII}*[,\\s]?)+")==false){
			Misc.loge("pasing fail-->%s",txt);
			return;
		}
		String[] arg = txt.split(":|,");
		for(int i=0; i<arg.length; i+=2){
			String tag = arg[i+0].trim();
			String val = arg[i+1].trim();
			if(tag.equals(TAG0)==true){
				args[1].setText(val);
			}else if(tag.equals(TAG1)==true){
				args[2].setText(val);
			}else if(tag.equals(TAG2)==true){
				args[3].setText(val);
			}else if(tag.equals(TAG3)==true){
				args[4].setText(val);
			}else if(tag.equals(TAG4)==true){
				if(val.length()==0) { val = "0.000"; }
				args[5].setText(val);				
			}else if(tag.equals(TAG5)==true){
				if(val.length()==0) { val = "0"; }
				args[6].setText(val);
			}else if(tag.equals(TAG6)==true){
				args[7].setText(val);
			}
		}
	}
}
