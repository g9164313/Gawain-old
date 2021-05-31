package prj.sputter;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.sun.glass.ui.Application;

import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class StepKindler extends StepExtender {
		
	private final static String action_name = "高壓設定";
	public final static String TAG_KINDLE = "點火";
	
	public StepKindler(){
		set_mesg(action_name);		
		set(op_1,op_2,
			op_3,op_4,op_6
		);
	}
	//rad[0].setDisable(true);
	//rad[0].setStyle("-fx-opacity: 1");
	
	private static final String LEVEL_VOLT = "輸出電壓";
	private static final String LEVEL_AMPR = "輸出電流";
	private static final String LEVEL_WATT = "輸出功率";	
	private HBox lay_root = new HBox();
	
	private class PanAction extends GridPane {
		Label[] txt = {
			new Label(LEVEL_WATT), 
			new Label("爬升時間"), 
			new Label("穩定時間")
		};
		TextField[] arg = {
			new TextField("100") ,//regulator value
			new TextField("5")   ,//ramp time
			new TextField("30"),//stable time
		};
		PanAction(){
			txt[0].setOnMouseClicked(e->choose_level());
			for(TextField box:arg){
				box.setMaxWidth(80);
			}			
			getStyleClass().addAll("box-pad");			
			add(new Separator(Orientation.VERTICAL), 1, 0, 1, 4);
			addColumn(2,txt);
			addColumn(3,arg);
		}
		private void choose_level() {
			final ToggleGroup grp = new ToggleGroup();
			final RadioButton[] opt = {
				new RadioButton(LEVEL_VOLT),
				new RadioButton(LEVEL_AMPR),
				new RadioButton(LEVEL_WATT),
			};
			for(RadioButton obj:opt) { obj.setToggleGroup(grp); }
			grp.selectToggle(opt[0]);
			final VBox lay = new VBox(opt);
			lay.getStyleClass().addAll("box-pad");	
			final String chl = txt[0].getText();
			if(chl.equals(LEVEL_VOLT)==true) {
				grp.selectToggle(opt[0]);
			}else if(chl.equals(LEVEL_AMPR)==true) {
				grp.selectToggle(opt[1]);
			}else if(chl.equals(LEVEL_WATT)==true) {
				grp.selectToggle(opt[2]);
			}
			
			final Dialog<String> dia = new Dialog<>();
			dia.setTitle("選取模式");
			dia.setHeaderText("DCG100 調節模式");
			dia.getDialogPane().getButtonTypes().addAll(
				ButtonType.OK,
				ButtonType.CANCEL
			);
			dia.setResultConverter(btn->{
				if(btn==ButtonType.OK) {
					return ((RadioButton)grp.getSelectedToggle()).getText();
				}
				return null;
			});
			dia.getDialogPane().setContent(lay);
			Optional<String> res = dia.showAndWait();
			if(res.isPresent()==true) {
				txt[0].setText(res.get());
			}
		}
		public long get_all_tick() { 
			long tick = 0L;
			tick += Misc.time2tick(arg[1].getText().trim());
			tick += Misc.time2tick(arg[2].getText().trim());
			return tick;
		}
		public void get_constant_command(final ArrayList<String> cmd) {
			cmd.add(String.format("SPW=%d",Integer.valueOf(arg[0].getText().trim())));
			cmd.add(String.format("SPR=%d",Misc.time2tick(arg[1].getText().trim())));//unit is millisecond
			cmd.add("CHL=W");
			cmd.add("CHT=C");
		}
		public void get_sequence_command(
			final int seq_id,
			final ArrayList<String> cmd
		) {				
			final String chl = txt[0].getText();
			int val = Integer.valueOf(arg[0].getText());
			final int tk1 = Integer.valueOf(arg[1].getText())*1000;
			final int tk2 = Integer.valueOf(arg[2].getText())*1000;
			if(chl.equals(LEVEL_VOLT)==true) {
				val = val * 10;
				cmd.add(String.format("SQL%d=V",seq_id));
			}else if(chl.equals(LEVEL_AMPR)==true) {
				val = val * 100;
				cmd.add(String.format("SQL%d=A",seq_id));
			}else if(chl.equals(LEVEL_WATT)==true) {
				cmd.add(String.format("SQL%d=W",seq_id));
			}
			cmd.add(String.format("SQN%d=2",seq_id));
			cmd.add(String.format("SQP%d0-%d=%d",seq_id,val,tk1));
			cmd.add(String.format("SQP%d1-%d=%d",seq_id,val,tk2));
		}
	};
	
	final Runnable op_1 = ()->{
		//close shutter~~~
		final String tag = "關閉擋板";
		set_mesg(tag);
		waiting_async();
		sqm.shutter_and_zeros(false,()->{
			Misc.logv(tag);
			next_step();
		}, ()->{
			Misc.logv(tag+"失敗");
			abort_step();
			Application.invokeLater(()->PanBase.notifyError("失敗", "無法控制擋板!!"));
		});
	};
	final Runnable op_2 = ()->{
		final String tag = "啟動 H-Pin";
		set_mesg(tag);
		waiting_async();
		spk.asyncBreakIn(()->{
			spk.set_register(1, 2);//high-pin
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
			Misc.logv(tag);
			next_step();
		});
	};
	
	/*
	DCG100 command example:
	SPR=[millisecond]
	SPT=???  #CT mode
	SPJ=???  #CJ mode
	SPW,SPV,SPA
	CHL=W,V,A
	CHT=C,CT,CJ
	TRG
	*/
	private long tick_start = -1L;
	private long tick_total = -1L; //unit is millisecond.
	final Runnable op_3 = ()->{
		final String tag = "啟動 DCG";
		set_mesg(tag);
		ObservableList<Node> pan = lay_root.getChildren();		
		final ArrayList<String> cmd = new ArrayList<String>();
		if(pan.size()<=2) {
			//constant mode
			PanAction act = (PanAction)pan.get(1);			
			tick_total = act.get_all_tick();
			act.get_constant_command(cmd);
		}else{
			//sequence mode
			int cnt = pan.size();
			for(int i=1; i<cnt; i++) {
				cmd.add(String.format("SQC%d=%d",i-1,i));
			}
			cmd.add(String.format("SQC%d=FF",cnt-1));			
			for(int i=1; i<pan.size(); i++) {
				PanAction act = (PanAction)pan.get(i);
				tick_total += act.get_all_tick();
				act.get_sequence_command(i-1,cmd);
			}
			cmd.add("SQU=0");
			cmd.add("CHT=S");
		}
		waiting_async();
		dcg.asyncBreakIn(()->{
			cmd.add("TRG");
			String[] _txt = cmd.toArray(new String[1]);
			exec(_txt);
			String flat = "";
			for(String v:_txt) {
				flat = flat + v + ", ";
			}
			Misc.logv("[DCG] : %s",flat);
			tick_start = System.currentTimeMillis();
			next_step();
		});
	};
	
	final Runnable op_4 = ()->{
		long tick = System.currentTimeMillis() - tick_start;
		print_info(TAG_KINDLE);		
		set_mesg(TAG_KINDLE,
			String.format(
				"倒數:%s/%s",
				Misc.tick2text(tick,false),
				Misc.tick2text(tick_total,false)
			)
		);
		if(tick<tick_total) {
			hold_step();
		}else {
			next_step();
		}
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
		try {
			TimeUnit.MILLISECONDS.sleep(100);
		} catch (Exception e) {
			//for next command~~~
		}
		return res;		
	}

	@Override
	public Node getContent(){
		GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad");
		lay0.addColumn(0, msg);
		lay_root.getChildren().addAll(lay0,new PanAction());
		return lay_root;
	}
	@Override
	public void eventEdit() {
		final Alert dia = new Alert(AlertType.CONFIRMATION);
		dia.setTitle("新增/刪除");
		dia.setHeaderText("新增或刪除調節步驟");
		dia.setContentText("");
		final ButtonType btn_add = new ButtonType("新增");
		final ButtonType btn_del = new ButtonType("刪除");
		final ButtonType btn_xxx = new ButtonType("取消",ButtonData.CANCEL_CLOSE);
		dia.getButtonTypes().setAll(btn_add,btn_del,btn_xxx);
		Optional<ButtonType> res = dia.showAndWait();
		ObservableList<Node> lst = lay_root.getChildren();
		if(res.get()==btn_add) {
			if(lst.size()<10) {
				lay_root.getChildren().add(new PanAction());
			}
		}else if(res.get()==btn_del) {			
			if(lst.size()>2) {
				lst.remove(lst.size()-1);
			}
		}
	}
	
	private static final String TAG0 = "option";
	private static final String TAG1 = "ramp";
	private static final String TAG2 = "value";
	private static final String TAG3 = "time";
	
	@Override
	public String flatten() {
		//trick, replace time format.
		//EX: mm:ss --> mm#ss
		ObservableList<Node> pan = lay_root.getChildren();
		PanAction act = (PanAction)pan.get(1);
		return String.format(
			"%s:%d-%d, %s:%s, %s:%s, %s:%s",
			TAG0, 0, 0,
			TAG1, act.arg[1].getText().trim().replace(':','.'),
			TAG2, act.arg[0].getText().trim(),
			TAG3, act.arg[2].getText().trim().replace(':','.')
		);
	}
	@Override
	public void expand(String txt) {
		if(txt.matches("([^:,\\p{Space}]+[:]\\p{ASCII}*[,\\s]?)+")==false){
			Misc.loge("pasing fail-->%s",txt);
			return;
		}
		ObservableList<Node> pan = lay_root.getChildren();
		PanAction act = (PanAction)pan.get(1);
		//trick, replace time format.
		//EX: mm#ss --> mm:ss
		String[] col = txt.split(":|,");
		for(int i=0; i<col.length; i+=2){
			final String tag = col[i+0].trim();
			final String val = col[i+1].trim();
			if(tag.equals(TAG1)==true){
				act.arg[1].setText(val.replace('.',':'));
			}else if(tag.equals(TAG2)==true){
				act.arg[0].setText(val);
			}else if(tag.equals(TAG3)==true){
				act.arg[2].setText(val.replace('.',':'));
			}
		}
	}
}
