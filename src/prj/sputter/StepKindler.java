package prj.sputter;

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

public class StepKindler extends StepAnalysis {
	
	private DevSPIK2k spk;
	
	public StepKindler(
		final DevSQM160 dev1,
		final DevDCG100 dev2, 
		final DevSPIK2k dev3
	){
		sqm = dev1;
		dcg = dev2;
		spk = dev3;
		log_text[0] = msg1;
		log_text[1] = msg2;
		set(op_1,op_2,
			op_3,
			op_4,op_5,
			op_6,op_save,op_7
		);
	}
	
	private final static String init_text = "高壓設定";
	
	public final static String TAG_RAMP = "爬升";
	public final static String TAG_CLEAN = "清洗";
	public final static String TAG_TIMER = "計時";
	public final static String TAG_JOULE = "焦耳";
	
	final JFXRadioButton[] rad = {
		new JFXRadioButton ("定額"),
		new JFXRadioButton ("定時"),
		new JFXRadioButton ("定量"),
		new JFXRadioButton ("功率(W)"),
		new JFXRadioButton ("電壓(V)"),
		new JFXRadioButton ("電流(A)"),		
	};
	private final Label[] txt = {
		new Label("爬升"), 
		new Label("輸出"),  
		new Label(TAG_CLEAN),
	};
	private final TextField[] arg = {
		new TextField("5"),
		new TextField("100"),		
		new TextField("5:00"),
	};	
	private Label msg1 = new Label(init_text);
	private Label msg2 = new Label();
	
	public final TextField boxValue = arg[1]; 
	
	final Runnable op_1 = ()->{
		//close shutter~~~
		log_indx = 0;
		final String _txt = "關閉擋板";
		msg1.setText(_txt);
		msg2.setText("");
		next_work();
		waiting_async();
		sqm.asyncBreakIn(()->{
			try {
				sqm.exec("S");
				Thread.sleep(250);
				sqm.exec("T");
				Thread.sleep(250);
				if(sqm.exec("U0").charAt(0)=='A') {
					Misc.logv(_txt);
					next_work();
				}else{
					next_abort();
					Application.invokeLater(()->PanBase.notifyError("失敗", "無法控制擋板!!"));
				}
			} catch (InterruptedException e) {
				next_abort();
				Application.invokeLater(()->PanBase.notifyError("", "內部錯誤!!"));
			}
		});
	};
	final Runnable op_2 = ()->{		
		msg1.setText("等待檔板");
		msg2.setText(String.format(
			"%s",
			Misc.tick2text(waiting_time(2000),true)
		));
	};

	final Runnable op_3 = ()->{
		final String _txt = "啟動 H-Pin";
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
		final String _txt = "高壓設定";
		msg1.setText(_txt);
		msg2.setText("");
		Misc.logv(_txt);
		
		final String[] lst_cmd = {
			"",//SPR=[millisecond]
			"",//CHT=C,CT,CJ
			"",//CT-->SPT, CJ-->SPJ
			"",//CHL=A,V,W
			"",//SPW,SPV,SPA
			"",//others...
			"TRG",//trigger and fire!!
		};
		
		lst_cmd[0] = String.format(
			"SPR=%d",
			Misc.time2tick(arg[0].getText().trim())
		);//unit is millisecond	
		if(rad[0].isSelected()==true) {
			lst_cmd[1] = "CHT=C";
			lst_cmd[2] = "";//skip~~~
		}else if(rad[1].isSelected()==true) {
			lst_cmd[1] = "CHT=CT";
			lst_cmd[2] = String.format(
				"SPT=%d",
				Misc.time2tick(arg[2].getText().trim())
			);//unit is millisecond
		}else if(rad[2].isSelected()==true) {
			int val = Integer.valueOf(arg[2].getText().trim());
			lst_cmd[1] = "CHT=CJ";
			lst_cmd[2] = "SPJ="+val;
			lst_cmd[5] = "TJR=1";//Joules resolution--> 0:coarse, 1:fine
			//lst_cmd[6] = "TMM=1";//Ramp Watts in Joules mode --> 0:included, 1:excluded
		}
		int val = Integer.valueOf(arg[1].getText().trim());
		if(rad[3].isSelected()==true) {
			lst_cmd[3] = "CHL=W";
			lst_cmd[4] = "SPW="+val;
		}else if(rad[4].isSelected()==true) {
			lst_cmd[3] = "CHL=V";
			lst_cmd[4] = "SPV="+val;
		}else if(rad[5].isSelected()==true) {
			lst_cmd[3] = "CHL=A";
			lst_cmd[4] = "SPA="+val;
		}
		waiting_async();			
		dcg.asyncBreakIn(()->{
			for(String cmd:lst_cmd) {
				if(setting(cmd)==false) { return; }
			}
			Misc.logv("！！Fire！！");			
			next_work();
		});
		//rad[0].setDisable(true);
		//rad[0].setStyle("-fx-opacity: 1");
	};
	final Runnable op_5 = ()->{
		msg1.setText(TAG_RAMP);
		msg2.setText(String.format(
			"%s",
			Misc.tick2text(waiting_tick(arg[0].getText()),true)
		));
		record_info(TAG_RAMP);		
	};
	final Runnable op_6 = ()->{
		String _txt = "";
		String _inf = "";
		if(rad[0].isSelected()==true) {
			//constant-output mode			
			_txt = TAG_CLEAN;
			_inf = Misc.tick2text(waiting_tick(arg[2].getText()),true);
		}else if(rad[1].isSelected()==true) {
			//run-time shutdown mode
			_txt = TAG_TIMER;
			_inf = Misc.tick2text(waiting_tick(arg[2].getText()),true);
		}else if(rad[2].isSelected()==true) {
			//Joule mode
			_txt = TAG_JOULE;
			//TODO:check how much Joule has been generated.
			//_inf = Misc.tick2text(remain_tick(true),true);
		}		
		msg1.setText(_txt);
		msg2.setText(String.format("%s",_inf));
		record_info(_txt);
	};
	final Runnable op_7 = ()->{
		msg1.setText(init_text);
		msg2.setText("");
		Misc.logv(init_text+"結束");
		next_work();
	};
	
	private boolean setting(final String cmd) {
		if(cmd==null) {
			return true;
		}
		if(cmd.length()==0) {
			return true;
		}
		boolean res = dcg.exec(cmd).endsWith("*");
		if(res==false) {
			String _txt = cmd + "設定失效!!";
			next_abort();
			Misc.logv(_txt);
			Application.invokeLater(()->PanBase.notifyError("",_txt));
		}
		return res;		
	}
		
	@Override
	public Node getContent(){
		msg1.setPrefWidth(150);
		msg2.setPrefWidth(150);
		for(TextField box:arg){
			box.setMaxWidth(80);
		}
		
		final ToggleGroup grp1= new ToggleGroup();
		final ToggleGroup grp2= new ToggleGroup();
		rad[0].setToggleGroup(grp1);
		rad[1].setToggleGroup(grp1);
		rad[2].setToggleGroup(grp1);
		rad[3].setToggleGroup(grp2);
		rad[4].setToggleGroup(grp2);
		rad[5].setToggleGroup(grp2);
		
		rad[0].setSelected(true);
		rad[3].setSelected(true);
		
		rad[0].setOnAction(e->txt[2].setText(TAG_CLEAN));
		rad[1].setOnAction(e->txt[2].setText(TAG_TIMER));
		rad[2].setOnAction(e->txt[2].setText(TAG_JOULE));
		
		GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0, msg1, msg2);
		lay.add(new Separator(Orientation.VERTICAL), 1, 0, 1, 3);	
		lay.addColumn(2,rad[0],rad[1],rad[2]);
		lay.add(new Separator(Orientation.VERTICAL), 3, 0, 1, 3);
		lay.addColumn(4,rad[3],rad[4],rad[5]);
		lay.add(new Separator(Orientation.VERTICAL), 5, 0, 1, 3);
		lay.addColumn(6,txt[0],txt[1],txt[2]);
		lay.addColumn(7,arg[0],arg[1],arg[2]);
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
		int opt_type=-1, opt_level=-1;
		if(rad[0].isSelected()==true) {
			opt_type = 0;
		}else if(rad[1].isSelected()==true){
			opt_type = 1;
		}else if(rad[2].isSelected()==true){
			opt_type = 2;
		}
		if(rad[3].isSelected()==true) {
			opt_level = 3;
		}else if(rad[4].isSelected()==true){
			opt_level = 4;
		}else if(rad[5].isSelected()==true){
			opt_level = 5;
		}
		return String.format(
			"%s:%d-%d, %s:%s, %s:%s, %s:%s",
			TAG0, opt_type, opt_level,
			TAG1, arg[0].getText().trim().replace(':','.'),
			TAG2, arg[1].getText().trim(),
			TAG3, arg[2].getText().trim().replace(':','.')
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
		String[] col = txt.split(":|,");
		for(int i=0; i<col.length; i+=2){
			final String tag = col[i+0].trim();
			final String val = col[i+1].trim();
			if(tag.equals(TAG0)==true){
				String[] _v = val.split("-");
				int opt_typ = Integer.valueOf(_v[0]);
				int opt_lev = Integer.valueOf(_v[1]);
				rad[opt_typ].setSelected(true);
				rad[opt_typ].getOnAction().handle(null);
				rad[opt_lev].setSelected(true);
			}else if(tag.equals(TAG1)==true){
				arg[0].setText(val.replace('.',':'));
			}else if(tag.equals(TAG2)==true){
				arg[1].setText(val);
			}else if(tag.equals(TAG3)==true){
				arg[2].setText(val.replace('.',':'));
			}
		}
	}
}
