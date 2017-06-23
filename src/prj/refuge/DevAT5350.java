package prj.refuge;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
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
			attr = Gawain.prop.getProperty("DevAT5350","/dev/ttyS0,9600,8n1");
		}
		open(attr);
	}
	
	public void disconnect(){
		close();
	}
	//--------------------------------//
	
	private StringProperty lastResponse = new SimpleStringProperty();

	private void exec(final String cmd){
		exec(null,cmd);
	}
	
	private Task<Void> tskFetch = null;
	
	private void exec(		
		final EventHandler<WorkerStateEvent> hook,
		String... buf
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
				for(int i=0; i<buf.length; i++){
					String cmd = buf[i];
					if(cmd.charAt(cmd.length()-1)!='\n'){
						cmd = cmd + '\n';
					}
					writeTxt(cmd,DELAY);
					if(buf[i].lastIndexOf('?')>=0){
						//we have response~~~
						buf[i] = readTxt('\n','\n',DELAY);
					}else{
						//device just echo the message~~~~
						buf[i] = readTxt('\n',DELAY);
					}					
					Misc.logv("AT5350> %s",buf[i]);
				}
				return null;
			}
		};
		if(hook!=null){
			tskFetch.setOnSucceeded(hook);
		}
		new Thread(tskFetch,"AT5350-fetch").start();
	}
	
	private int sampleCount = 20;
	private int sampleECount= 10;//second
	private int filterPeriod= 60;//second
	
	@Override
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
		
		final JFXComboBox<String> cmb = new JFXComboBox<String>();
		
		final GridPane root = new GridPane();//show all sensor
		root.getStyleClass().add("grid-medium");

		root.addRow(0, new Label("取樣個數")       ,boxSampleCount);
		root.addRow(1, new Label("取樣週期（sec）"),boxSampleECount);
		root.addRow(2, new Label("Filter（sec）")  ,boxFilterPeriod);
		root.add(new Label("使用濾波器")     , 0, 3);
		root.add(new Label("測量形式")       , 0, 4);
		root.add(new Label("測量範圍")       , 0, 5);			
		root.add(new Label("Damper（0.01%）"), 0, 6);
		root.add(new Label("溫度因子"), 0, 7);
		root.add(new Label("壓力因子"), 0, 8);
		root.add(new Label("高壓電（Volt）") , 0, 9);
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-one-dir");
		final Button btnTest = PanBase.genButton2("測試連線",null);
		btnTest.setOnAction(event->{
			exec("*IDN?");
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
		final Button btnLoad = PanBase.genButton2("讀取參數",null);
		btnLoad.setOnAction(event->{
			
		});
		final Button btnSave = PanBase.genButton2("儲存參數",null);
		btnSave.setOnAction(event->{
			exec(null,
				"CONF?",
				":CURR:FILT:VAL?",
				":CURR:DAMP:VAL?",
				":FACT:TEMP?",
				":FACT:PRES?",
				":HVOL:VAL?"
			);
		});
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
			exec(null,":CORR:AUTO");
		});
		final Button btnMeas = PanBase.genButton2("測量數據",null);
		btnMeas.setOnAction(event->{
			
		});
		lay1.getChildren().addAll(
			btnTest,
			btnLoad,
			btnSave,
			btnVolt,
			btnComp,
			btnMeas
		);
		
		root.add(new Separator(Orientation.VERTICAL), 2, 0, 1, 10);
		root.add(lay1, 3, 0, 4, 10);
		return root;
	}
}
