package prj.scada;

import java.util.Optional;

import com.sun.glass.ui.Application;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
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
	
	private final static String init_txt = "薄膜切換";
	private Label msg = new Label(init_txt);
	private Label inf = new Label("編號：1");
	private int idx = 1;
	
	private void update_info(){
		inf.setText(String.format("編號：%2d", idx));
	}
	
	final Runnable op_1 = ()->{
		waiting_async();
		msg.setText("切換中");
		final char id = (char)(48+idx);
		sqm.asyncBreakIn(()->{
			//reset film data, include its tooling and final thick
			String cmd = String.format("D%c", id);
			if(sqm.exec(cmd).charAt(0)!='A') {
				result.set(ABORT);
				Application.invokeLater(()->PanBase.notifyError("失敗", "無法切換薄膜參數!!"));
				return;
			}
			result.set(NEXT);
		});
	};
	
	final Runnable op_2 = ()->{
		waiting_async();
		msg.setText("歸零");
		sqm.asyncBreakIn(()->{
			//reset film data, include its tooling and final thick			
			if(sqm.exec("S").charAt(0)!='A'){
				///Misc.logw("[SQM160] 無法重設厚度，速率.");
			}
			if(sqm.exec("T").charAt(0)!='A'){
				///Misc.logw("[SQM160] 無法重設時間.");
			}
			result.set(NEXT);
		});
	};
	
	final Runnable op_3 = ()->{
		msg.setText(init_txt);
	};
	
	@Override
	protected Node getContent(){
		update_info();
		msg.setPrefWidth(150);		
		HBox lay = new HBox(
			msg, 
			new Separator(Orientation.VERTICAL), 
			inf
		);
		lay.getStyleClass().addAll("box-pad");
		return lay;
	}
	
	@Override
	protected void eventEdit(){
		select_idx();
	}
	
	private void select_idx(){
		PadTouch pad = new PadTouch("薄膜編號",'N');
		Optional<String> opt = pad.showAndWait();
		if(opt.isPresent()==false) {
			return;
		}
		idx = Integer.valueOf(opt.get());
		update_info();
	}
	
	private static final String TAG0 = "fid";
	@Override
	public String flatten() {
		return String.format("%s:%d", TAG0, idx);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\s]+[:][^:,]+[,]?[\\s]*)+")==false){
			return;
		}
		String[] arg = txt.split(":|,");
		idx = Integer.valueOf(arg[1].trim());
		update_info();
	}
}
