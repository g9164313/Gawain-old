package prj.sputter;

import java.util.concurrent.TimeUnit;

import com.jfoenix.controls.JFXRadioButton;
import com.sun.glass.ui.Application;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;

import narl.itrc.Misc;
import narl.itrc.PanBase;

public class StepKindler extends StepExtender {
	
	private final static String action_name = "高壓設定";
	
	public StepKindler(){
		set_mesg(action_name);
		opt_reg[0].setSelected(true);
		set(op_1,op_2,
			op_3,op_4,
			op_6
		);
	}
	
	//rad[0].setDisable(true);
	//rad[0].setStyle("-fx-opacity: 1");
	
	final JFXRadioButton[] opt_reg = {
		new JFXRadioButton ("固定功率(W)"),
		new JFXRadioButton ("固定電壓(V)"),
		new JFXRadioButton ("固定電流(A)"),		
	};
	private final TextField[] box_arg = {
		new TextField("100"), //power value
		new TextField("5"),	  //ramp time
		new TextField("5:00"),//clean time
	};
	
	private final TextField box_power= box_arg[0];
	private final TextField box_ramp = box_arg[1];
	private final TextField box_clean= box_arg[2];

	final Runnable op_1 = ()->{
		//close shutter~~~
		final String _txt = "關閉擋板";
		set_mesg(action_name,_txt);
		next_step();
		waiting_async();
		sqm.shutter_and_zeros(false,()->{
			Misc.logv(_txt);
			next_step();
		}, ()->{
			Misc.logv(_txt+"失敗");
			abort_step();
			Application.invokeLater(()->PanBase.notifyError("失敗", "無法控制擋板!!"));
		});
	};
	final Runnable op_2 = ()->{
		final String _txt = "啟動 H-Pin";
		set_mesg(_txt);
		waiting_async();
		spk.asyncBreakIn(()->{
			spk.set_register(1, 2);//high-pin
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
			Misc.logv(_txt);
			next.set(LEAD);
		});
	};
	
	//DCG100 command example:
	//SPR=[millisecond]
	//CHT=C,CT,CJ
	//SPT=???  #CT mode
	//SPJ=???  #CJ mode
	//CHL=W,V,A
	//SPW,SPV,SPA
	//TRG 
	final Runnable op_3 = ()->{
		set_mesg(TAG_KINDLE);		
		final String SPR_VAL = String.format(
			"SPR=%d",
			Misc.time2tick(box_arg[1].getText().trim())
		);//unit is millisecond
		final String SPW_VAL = String.format(
			"SPW=%d",
			Integer.valueOf(box_arg[0].getText().trim())
		);
		waiting_async();			
		dcg.asyncBreakIn(()->{
			Misc.logv("DCG100: %s, %s", SPR_VAL, SPW_VAL);
			exec(SPR_VAL,
				"CHT=C",
				"CHL=W",
				SPW_VAL,
				"TRG"
			);		
			next_step();
		});
	};
	public final static String TAG_KINDLE = "點火程序";
	final Runnable op_4 = ()->{
		print_info(TAG_KINDLE);
		final long t1 = Misc.time2tick(box_arg[1].getText().trim());
		final long t2 = Misc.time2tick(box_arg[2].getText().trim());
		final long t3 = waiting_time(t1+t2);
		set_mesg(
			"等待中",
			String.format("%s",Misc.tick2text(t3,true))
		);		
	};
	final Runnable op_6 = ()->{
		set_mesg(action_name);
		Misc.logv(action_name+"結束");
		next_step();
	};
	
	private void exec(final String... lst) {
		for(String cmd:lst) {
			if(exec(cmd)==false) { return; }
		}
	}
	private boolean exec(final String cmd) {
		if(cmd==null) {
			return true;
		}
		if(cmd.length()==0) {
			return true;
		}
		boolean res = dcg.exec(cmd).endsWith("*");
		if(res==false) {
			String _txt = cmd + "設定失效!!";
			abort_step();
			Misc.logv(_txt);
			Application.invokeLater(()->PanBase.notifyError("",_txt));
		}
		return res;		
	}
		
	@Override
	public Node getContent(){
		for(TextField box:box_arg){
			box.setMaxWidth(80);
		}
		
		final ToggleGroup grp= new ToggleGroup();
		opt_reg[0].setToggleGroup(grp);
		opt_reg[1].setToggleGroup(grp);
		opt_reg[2].setToggleGroup(grp);
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, msg);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
		lay.addColumn(2,new Label("輸出功率"),new Label("預備時間"),new Label("清洗時間"));
		lay.addColumn(3,box_arg);
		lay.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 4);
		lay.addColumn(5,opt_reg);
		return lay;
	}
	@Override
	public void eventEdit() {
	}
	
	private static final String TAG0 = "option";
	private static final String TAG1 = "ramp";
	private static final String TAG2 = "value";
	private static final String TAG3 = "time";
	
	@Override
	public String flatten() {
		//trick, replace time format.
		//EX: mm:ss --> mm#ss
		int opt_level = 0;
		if(opt_reg[0].isSelected()==true) {
			opt_level = 0;
		}else if(opt_reg[1].isSelected()==true){
			opt_level = 1;
		}else if(opt_reg[2].isSelected()==true){
			opt_level = 2;
		}
		return String.format(
			"%s:%d-%d, %s:%s, %s:%s, %s:%s",
			TAG0, 0, opt_level,
			TAG1, box_ramp.getText().trim().replace(':','.'),
			TAG2, box_power.getText().trim(),
			TAG3, box_clean.getText().trim().replace(':','.')
		);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\p{Space}]+[:]\\p{ASCII}*[,\\s]?)+")==false){
			Misc.loge("pasing fail-->%s",txt);
			return;
		}
		//trick, replace time format.
		//EX: mm#ss --> mm:ss
		String[] col = txt.split(":|,");
		for(int i=0; i<col.length; i+=2){
			final String tag = col[i+0].trim();
			final String val = col[i+1].trim();
			if(tag.equals(TAG1)==true){
				box_ramp.setText(val.replace('.',':'));
			}else if(tag.equals(TAG2)==true){
				box_power.setText(val);
			}else if(tag.equals(TAG3)==true){
				box_clean.setText(val.replace('.',':'));
			}
		}
	}
}
