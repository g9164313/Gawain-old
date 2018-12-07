package prj.refuge;

import java.util.ArrayList;

import com.jfoenix.controls.JFXTextField;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * This is model for AT5350, electrometer measurement unit.
 * When transmitting command, we must send one character one bye one, at least 10 msec. 
 * @author qq
 *
 */
public class DevAT5350 extends DevTTY {

	private final int DELAY = 10;//unit is ms~~~
	
	public DevAT5350(){
	}
	
	public void connect(String attr){
		if(attr.length()==0){
			attr = Gawain.getSetting().getProperty("DevAT5350","/dev/ttyS0,9600,8n1");
		}
		open(attr);
	}
	
	public void disconnect(){
		close();
	}
	//--------------------------------//

	private String exec(String cmd){
		final String[] buf = { cmd };
		exec(null,cmd);
		return buf[0];
	}
	
	private Task<Void> tskFetch = null;
	
	private void exec(		
		final EventHandler<WorkerStateEvent> hook,
		final String... buf
	){
		if(tskFetch!=null){
			if(tskFetch.isRunning()==true){
				Misc.logw("AT5350 忙碌中");
				return;
			}
		}
		tskFetch = new Task<Void>(){
			@Override
			protected Void call() throws Exception {				
				return _exec_tsk(buf);
			}
		};
		if(hook!=null){
			tskFetch.setOnSucceeded(hook);
		}
		new Thread(tskFetch,"AT5350-fetch").start();
	}
	
	private Void _exec_tsk(final String... buf){
		for(int i=0; i<buf.length; i++){
			if(buf[i]==null){
				continue;
			}
			//check tail character again~~~
			Misc.logv("AT5350) << %s",buf[i]);
			String cmd = buf[i];
			if(cmd.charAt(cmd.length()-1)!='\n'){
				cmd = cmd + '\n';
			}
			writeTxt(cmd,DELAY);
			//try to get response
			if(buf[i].lastIndexOf('?')>=0){
				//device have response~~~
				buf[i] = readTxt('\n','\n',DELAY);
			}else{
				//device just echo the message~~~~
				buf[i] = readTxt('\n', DELAY);
			}					
			Misc.logv("AT5350) >> %s",buf[i]);
		}
		return null;
	}

	//private final boolean simFlag = false;
	
	/**
	 * Start to measurement, this is blocking procedure.<p>
	 * Don't call it with GUI-thread.
	 * @param temp - temperature(degree)
	 * @param press - atmospheric pressure
	 * @return
	 */
	public String syncMeasure(final String temp,final String press){
		/*if(simFlag==true){
			String txt = "\"-1.8313E-10 Sv/min #150\","+
					"\"+5.4938E-10 Sv/min #200\",\"-1.8313E-10 Sv/min #250\","+
					"\"-1.8313E-10 Sv/min #300\",\"+9.1563E-10 Sv/min #350\"";
			split_result(txt);
			return txt;
		}*/
		int _ecount = sampleECount * 10;
		int _period = filterPeriod * 10;
		int _trigger= sampleCount + filterPeriod/sampleECount + 1;
		final String[] buf1 = {
			(temp ==null)?(null):(":FACT:TEMP "+temp),
			(press==null)?(null):(":FACT:PRES "+press),
			":DRAT:FILT ON",
			":DRAT:FILT:VAL "+_period,
			"TRIG:ECO "+_ecount,
			"TRIG:COUN "+_trigger,
			"INIT"
		};
		_exec_tsk(buf1);
		Misc.logv("wait AT5350...");
		
		//waiting few seconds, then get measurement.
		int sec = _trigger*sampleECount+5;
		Misc.delay(sec*1000);
		
		final String[] buf2 = {
			"FETC:ARR? "+sampleCount
		};
		_exec_tsk(buf2);
		split_result(buf2[0]);		
		return buf2[0];
	}

	public void syncCompensate(){
		Misc.logv("AT5350) 補償開始");
		_exec_tsk(":CORR:AUTO");//when device done, it will give us echo~~~
		Misc.logv("AT5350) 補償結束");
	}
	//--------------------------------//
	
	public static class TabResultItem {
		public final StringProperty  index = new SimpleStringProperty("？？？");
		public final StringProperty  value = new SimpleStringProperty("？？？");
		public StringProperty indexProperty() { return index; }
		//public String getIndex() { return index.get(); }
		public StringProperty valueProperty() { return value; }
		//public String getValue() { return value.get(); }
	}
	
	private class TabResult extends HBox {
		
		private String txtAvg, txtDev, txtMin, txtMax;
		
		@SuppressWarnings("unchecked")
		public TabResult(){
			
			statistics();			
			
			TableView<TabResultItem> tab = new TableView<TabResultItem>();		
			TableColumn<TabResultItem,String> col1 = new TableColumn<TabResultItem,String>("#編號");
			col1.setCellValueFactory(new PropertyValueFactory<TabResultItem,String>("index"));
			TableColumn<TabResultItem,String> col2 = new TableColumn<TabResultItem,String>("數值");
			col2.setPrefWidth(100);
			col2.setCellValueFactory(new PropertyValueFactory<TabResultItem,String>("value"));
			tab.setPrefWidth(200);
			tab.getColumns().addAll(col1,col2);

			for(int i=0; i<measValue.size(); i++){
				TabResultItem itm = new TabResultItem();
				itm.index.set(""+measIndex.get(i));
				itm.value.set(""+measValue.get(i));
				tab.getItems().add(itm);
			}
			
			GridPane lay2 = new GridPane();
			lay2.getStyleClass().add("grid-medium");
			lay2.addRow(0, new Label("單位"),  new Label(measUnit));
			lay2.add(new Separator(), 0, 1, 2, 1);
			lay2.addRow(2, new Label("最小"),  new Label(txtMin));
			lay2.addRow(3, new Label("最大"),  new Label(txtMax));
			lay2.add(new Separator(), 0, 4, 2, 1);
			lay2.addRow(5, new Label("平均"),  new Label(txtAvg));
			lay2.addRow(6, new Label("標準差"),new Label(txtDev));
			
			getStyleClass().add("hbox-small");
			getChildren().addAll(tab,lay2);
		}
		private void statistics(){
			ArrayList<Double> lst = new ArrayList<Double>();
			for(String txt:measValue){
				try{
					lst.add(Double.valueOf(txt));
				}catch(NumberFormatException e){
					Misc.logw("Wrong Format --> %s", txt);
				}
			}
			if(lst.size()==0){
				txtMin = txtMax = txtAvg = txtDev = "???";
				return;
			}
			double cnt = lst.size();			
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			double avg = 0., dev = 0.;
			for(double val:lst){
				if(val>max){
					max = val;
				}
				if(val<min){
					min = val;
				}
				avg = avg + val;
			}
			avg = avg / cnt;
			for(double val:lst){
				dev = dev + Math.pow((val-avg),2);
			}
			dev = Math.sqrt(dev/(cnt-1));
			txtMin = String.format("%E",min);
			txtMax = String.format("%E",max);
			txtAvg = String.format("%E",avg);
			txtDev = String.format("%E",dev);
		}
	};
	
	private class MeasOption extends GridPane {
		public String temp = null;
		public String press = null;
		public MeasOption(){
			getStyleClass().add("grid-medium");
			JFXTextField boxTemp = new JFXTextField();
			boxTemp.textProperty().addListener(event->{
				try{
					temp = boxTemp.getText();
					Integer.valueOf(temp);					
				}catch(NumberFormatException e){
					temp = null;
				}			
			});
			boxTemp.setPrefWidth(70);			
			JFXTextField boxPress= new JFXTextField();
			boxPress.textProperty().addListener(event->{
				try{
					press = boxPress.getText();
					Integer.valueOf(boxPress.getText());					
				}catch(NumberFormatException e){
					press = null;
				}			
			});
			boxPress.setPrefWidth(70);
			addRow(0, new Label("溫度"),  boxTemp);
			addRow(1, new Label("壓力"),  boxPress);
		}
	};
	
	public String measUnit = "";
	public ArrayList<String> measValue = new ArrayList<String>(); 
	public ArrayList<Integer> measIndex = new ArrayList<Integer>();
	public double measAverage= 0.;//scalet is micro
	public double measStddev = 0.;
	
	private void split_result(String txt){
		
		measUnit = "";
		measValue.clear();
		measIndex.clear();
		measAverage = measStddev = 0.;
		
		txt = txt.replaceAll("\"", "");
		String[] lst = txt.split(",");
		
		ArrayList<Double> lstVal = new  ArrayList<Double>();
		
		for(String itm:lst){
			String[] arg = itm.split("\\s");
			if(arg.length<3){
				Misc.loge("Invalid Response - %s", itm);
				continue;
			}
			
			arg[0] = arg[0].trim();
			measValue.add(arg[0].trim());
			if(measUnit.length()==0){
				measUnit = arg[1].trim();
			}
			try{
				arg[2] = arg[2].trim().substring(1);
				measIndex.add(Integer.valueOf(arg[2]));
				lstVal.add(Double.valueOf(arg[0]));
			}catch(NumberFormatException e){
				Misc.logw("Wrong format: %s ",itm);
			}
		}
		
		double scale_u = Math.pow(10, 6);
		double cnt = lstVal.size();
		for(double v:lstVal){
			measAverage = measAverage + v*scale_u;
		}
		measAverage = measAverage / cnt;
		for(double v:lstVal){
			v = v * scale_u;
			measStddev = measStddev + (v-measAverage)*(v-measAverage);
		}
		measStddev = Math.sqrt(measStddev/(cnt-1.));
	}

	
	private int sampleCount = 20;
	private int sampleECount= 10;//second
	private int filterPeriod= 60;//second
	
	//private int sampleCount = 5;//debug
	//private int sampleECount= 5;//debug
	//private int filterPeriod= 5;//debug
	
	protected Node eventLayout(PanBase pan) {
		
		final JFXTextField boxSampleCount = new JFXTextField(""+sampleCount);		
		boxSampleCount.textProperty().addListener(event->{
			try{
				sampleCount = Integer.valueOf(boxSampleCount.getText());
			}catch(NumberFormatException e){				
			}			
		});
		boxSampleCount.setPrefWidth(70);
				
		final JFXTextField boxSampleECount = new JFXTextField(""+sampleECount);//second
		boxSampleECount.textProperty().addListener(event->{
			try{
				sampleECount = Integer.valueOf(boxSampleECount.getText());
			}catch(NumberFormatException e){				
			}			
		});
		boxSampleECount.setPrefWidth(70);
		
		final JFXTextField boxFilterPeriod= new JFXTextField(""+filterPeriod);//second
		boxFilterPeriod.textProperty().addListener(event->{
			try{
				sampleCount = Integer.valueOf(boxFilterPeriod.getText());
			}catch(NumberFormatException e){				
			}			
		});
		boxFilterPeriod.setPrefWidth(70);
		
		//final JFXComboBox<String> cmb = new JFXComboBox<String>();
		
		final GridPane root = new GridPane();
		root.getStyleClass().add("grid-medium");
		root.addRow(0, new Label("取樣個數")       ,boxSampleCount);
		root.addRow(1, new Label("取樣週期（sec）"),boxSampleECount);
		root.addRow(2, new Label("Filter（sec）")  ,boxFilterPeriod);
		/*root.add(new Label("使用濾波器")     , 0, 3);
		root.add(new Label("測量形式")       , 0, 4);
		root.add(new Label("測量範圍")       , 0, 5);			
		root.add(new Label("Damper（0.01%）"), 0, 6);
		root.add(new Label("溫度因子"), 0, 7);
		root.add(new Label("壓力因子"), 0, 8);
		root.add(new Label("高壓電（Volt）") , 0, 9);*/

		final Button btnTest = PanBase.genButton2("測試連線",null);
		btnTest.setOnAction(event->{
			exec("*IDN?");
		});
		final Button btnAbort = PanBase.genButton2("ABORT!!",null);
		btnAbort.setOnAction(event->{			
			exec("ABOR");
		});
		final Button btnVolt = PanBase.genButton2("高壓",null);
		btnVolt.setOnAction(event->{
			final String[] buf = { ":HVOL?" };
			exec(event1->{
				if(buf[0].contains("1")==true){
					exec(null,":HVOL OFF");
					btnVolt.setText("高壓-關-");
				}else if(buf[0].contains("0")==true){
					exec(null,":HVOL ON");
					btnVolt.setText("高壓-開-");
				}
			},buf);
		});		
		/*final Button btnLoad = PanBase.genButton2("讀取參數",null);
		btnLoad.setOnAction(event->{
			exec(null,
				"CONF?",
				":CURR:FILT:VAL?",
				":CURR:DAMP:VAL?",
				":FACT:TEMP?",
				":FACT:PRES?",
				":HVOL:VAL?"
			);
		});
		final Button btnSave = PanBase.genButton2("儲存參數",null);
		btnSave.setOnAction(event->{
		});*/
		final Button btnComp = PanBase.genButton2("執行補償",null);
		btnComp.setOnAction(event->{			
			root.disableProperty().set(true);//Don't let user touch anything!!!
			Timeline timer = new Timeline(new KeyFrame(
				Duration.minutes(3),
				eventAfter->{
					root.disableProperty().set(false);
				}
			));			
			timer.setCycleCount(1);
			timer.play();
			//device will not reply anything, just put a delay
			exec(null,":CORR:AUTO");
		});
		final Button btnMeas = PanBase.genButton2("測量數據",null);
		btnMeas.setOnAction(event->{
			root.disableProperty().set(true);
			//ask pressure and temperature
			Alert dia = new Alert(AlertType.INFORMATION);
			final MeasOption opt = new MeasOption();
			dia.getDialogPane().setContent(opt);
			dia.showAndWait();
			new Thread(new Runnable(){
				@Override
				public void run() {
					syncMeasure(opt.temp,opt.press);
					Misc.invoke(stpFinal->{
						Alert dia = new Alert(AlertType.INFORMATION);
						dia.getDialogPane().setContent(new TabResult());
						dia.showAndWait();
						root.disableProperty().set(false);
					});
				}
			},"AT5350-measurement").start();			
		});

		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-one-dir");
		lay1.getChildren().addAll(
			btnTest,
			btnAbort,
			btnVolt,
			btnComp,
			btnMeas
		);
		
		root.add(new Separator(Orientation.VERTICAL), 2, 0, 1, 10);
		root.add(lay1, 3, 0, 4, 10);
		return root;
	}
}
