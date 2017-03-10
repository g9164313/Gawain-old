package prj.daemon;

import java.math.BigDecimal;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.sun.glass.ui.Application;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.PanTTY;

/**
 * This implements command set for Keyence LK G5000 Series.<p>
 * This controller attaches proximity sensor.<p>
 * Attention!!, this code only support RS-232 protocol.<p>
 * @author qq
 *
 */
public class DevLK_G5000 extends DevTTY {

	private static final String DEFAULT_PORT_ATTR = "115200,8n1";
	
	public DevLK_G5000(){		
	}
	
	public DevLK_G5000(String path){
		connect(path+","+DEFAULT_PORT_ATTR);
	}

	public void connect(){
		if(open()==0L){
			return;
		}
		init_type();
	}
	
	public void connect(String path){
		if(open(path+","+DEFAULT_PORT_ATTR)==0L){
			return;
		}
		init_type();
	}
	
	private void init_type(){
		String resp;
		resp = exec_cmd("Q0");//switch to the communication mode
		resp = exec_cmd("DR");//get the index of laser header 
		if(resp.length()!=0){
			String[] arg = resp.split(",");
			for(int i=0; i<2; i++){
				out[i].setIndex(arg[i]);
				out[i].getTypeResolution();
			}
			//Do we need to generate a sequence index line ???
		}		
		resp = exec_cmd("R0");//switch to the general mode
	}
	
	private String exec_cmd(final String cmd){
		final String tail = "\r";
		String resp = fetch(cmd+tail,tail);
		if(resp.startsWith("ER")==true){
			//show error message~~~
			Misc.loge("ERROR COMMAND: %s", resp);
			return "";
		}else if(resp.startsWith(cmd)==true){
			resp = resp.substring(cmd.length());
			if(resp.startsWith(",")==true){
				resp = resp.substring(1);
			}
		}
		return resp;
	}
	
	/**
	 * set device value. Negative number is invalid (error happened).<p>
	 * @param cmd - any command
	 * @return
	 */
	private int exec_get_int(final String cmd){
		String val = exec_cmd(cmd);
		if(val.length()==0){
			return -1;
		}
		try{
			return Integer.valueOf(val);
		}catch(NumberFormatException e){
			Misc.loge("ERROR COMMAND:"+val);
			return -1;
		}
	}
	
	public void disconnect(){
		if(Application.isEventThread()==true){
			watcher.pause();
		}else{
			Application.invokeAndWait(()->{watcher.pause();});
		}
		close();
	}
	
	/**
	 * get a physical value (including number and unit).<p>
	 * @param i - the index of laser header, only 2 header in one controller
	 * @return the text of physical value
	 */
	public String getPhyValue(int i){
		if(i>=out.length){
			return "0mm";
		}		
		return out[i].value.toString()+out[i].term;
	}
	//-----------------------//

	private WidHeader[] out = {
		new WidHeader(0),
		new WidHeader(1)
	};
	
	private EventHandler<ActionEvent> eventWatcher = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			//how to set index number???
			String val = exec_cmd("MM,110000000000");
			if(val.length()==0){
				return;
			}
			String[] arg = val.split(",");
			out[0].setValue(arg[0]);
			out[1].setValue(arg[1]);
		}
	};
	
	private Timeline watcher = new Timeline(new KeyFrame(
		Duration.millis(100),
		eventWatcher
	));
		
	private JFXCheckBox chkAllOut, chkWatcher;
	
	@Override
	protected Node eventLayout(){
		
		GridPane root = new GridPane();
		root.getStyleClass().add("grid-medium");
				
		chkAllOut = new JFXCheckBox("全部");
		chkAllOut.disableProperty().bind(isAlive().not());
		
		chkWatcher = new JFXCheckBox("即時");		
		chkWatcher.disableProperty().bind(isAlive().not());
		chkWatcher.setSelected(true);
		chkWatcher.setOnAction(event->{
			if(chkWatcher.isSelected()==true){
				watcher.play();
			}else{
				watcher.pause();
			}
		});
				
		//bind global controller to laser out-header
		for(int i=0; i<out.length; i++){
			out[i].disableProperty().bind(isAlive().not());
			out[i].btnMeasure.disableProperty().bind(chkWatcher.selectedProperty());
		}

		Button btnComPort = PanBase.genButton0("通訊埠："+getName(),"");
		btnComPort.setOnAction(event->{
			root.setDisable(true);
			setInfoAttr(DEFAULT_PORT_ATTR);
			PanTTY.showSetting(
				DevLK_G5000.this,
				eventOpen->{
					connect();
					if(chkWatcher.isSelected()==true){
						watcher.play();
					}else{
						watcher.pause();
					}					
					btnComPort.setText("通訊埠："+getName());					
				},
				eventClose->{
					disconnect();
					watcher.pause();
					btnComPort.setText("通訊埠：----");
				}
			);
			root.setDisable(false);
		});
		
		root.addRow(1, btnComPort, chkAllOut, chkWatcher);
		root.add(new Separator(), 0, 2, 6, 1);
		root.add(out[0], 0, 3, 5, 1);
		root.add(out[1], 0, 4, 5, 1);
		
		//finally check timer is running~~~
		watcher.setCycleCount(Timeline.INDEFINITE);		
		if(isAlive().get()==true){
			watcher.play();
		}
		return root;
	}
	//-----------------------------------------//
	
	/**
	 * This widget is used for displaying measured value or setting parameter.<p>
	 * In manual, this widget is actually referenced to "OUT".<p>
	 * @author qq
	 *
	 */
	private class WidHeader extends GridPane {
		
		public int index=0, type=0, unit=0;
		
		public BigDecimal value = BigDecimal.ZERO; 	
		public String term = "?";
		
		public StringProperty propValue = new SimpleStringProperty("********");		
				
		public WidHeader(final int idx){
			index = idx;
			init_layout();
		}

		public void setIndex(String idx){
			setIndex(Integer.valueOf(idx));
		}
		
		public void setIndex(final int idx){
			index = idx;
			if(Application.isEventThread()==true){
				txtIndex.setText("編號："+index);				
			}else{
				Application.invokeLater(()->{
					txtIndex.setText("編號："+index);
				});
			}
		}

		/**
		 * get the measurement type and unit from laser header
		 * Attention, this 'unit' means 'digital' <p>
		 * @param type - <p>
		 *   0: displacement, 1: velocity, 2: acceleration
		 * @param unit - <p>
		 *   displacement - 0: 0.01mm, 1: 0.001mm, 2: 0.0001μm, 3: 0.00001mm,  4: 0.1μm, 5: 0.01μm, 6: 0.001μm <p>
		 *   velocity - 0: 100mm/s, 1: 10mm/s, 2: 1mm/s, 3: 0.1mm/s, 4: 0.01mm/s, 5: 0.001mm/s, 6: 0.0001mm/s <p>
		 *   acceleration - 0: 100mm/s², 1: 10mm/s², 2: 1mm/s², 3: 0.1mm/s², 4: 0.01mm/s², 5: 0.001mm/s², 6: 0.0001mm/s² <p>
		 */
		public void getTypeResolution(){
			type = exec_get_int(String.format("SR,OI,%02d", index));
			unit = exec_get_int(String.format("SR,OG,%02d", index));
			reset_scale_term();
		}

		public void setResolution(final int unit){
			String txt = exec_cmd(String.format("SW,OG,%02d,%d", index, unit));
			if(txt.length()==0){
				return;//ignore invalid value
			}
			this.unit = unit;//update again~~~~
			reset_scale_term();
		}
		
		private void reset_scale_term(){
			switch(type){
			case 0://displacement
				if(unit<=3){
					term="mm";
				}else{
					term="μm";
				}
				break;
			case 1://velocity
				if(unit<=3){
					term="mm/s";
				}else{
					term="μm/s";
				}
				break;
			case 2://acceleration
				if(unit<=3){
					term="mm/s²";
				}else{
					term="μm/s²";
				}
				break;			
			}
		}
		
		public void setValue(final String txt){
			value = value.multiply(BigDecimal.ZERO);//reset old value
			if(txt.contains("FFFF")==true){
				propValue.set("!! Limit !!");
				return;
			}else if(txt.contains("XXXX")==true){
				propValue.set("--------");
				return;
			}
			value = new BigDecimal(txt);//assign a new number~~~
			String res = value.toString()+" "+term;
			if(Application.isEventThread()==true){
				propValue.set(res);
			}else{
				Application.invokeAndWait(()->{ 
					propValue.set(res); 
				});
			}
		}
		
		private Label txtIndex;		
		public Button btnMeasure;

		private void init_layout(){
			
			getStyleClass().add("grid-medium");
			
			txtIndex = new Label();
			txtIndex.setText("編號："+index);
			
			Label txtValue = new Label();
			txtValue.textProperty().bind(propValue);
			txtValue.setPrefWidth(180);
			txtValue.setAlignment(Pos.BASELINE_RIGHT);
			txtValue.setMaxHeight(Double.MAX_VALUE);
			txtValue.setStyle(
				"-fx-font-size: 23px;"+
				"-fx-border-color: #455a64;"+
				"-fx-label-padding: 7,0,0,7;"		
			);
			
			final double btn_size = 87.;
			
			Button btnSetting = PanBase.genButton1("<設定>","");
			btnSetting.setPrefWidth(btn_size);
			btnSetting.setOnAction(event->{
				watcher.pause();
				exec_cmd("Q0");
				new PanSetting(this).popup(null,null,null);				
				exec_cmd("R0");
				if(chkWatcher.isSelected()==true){
					watcher.play();
				}
			});

			btnMeasure = PanBase.genButton2("測量","");			
			btnMeasure.setPrefWidth(btn_size);
			btnMeasure.setOnAction(event->{
				String resp;
				if(chkAllOut.isSelected()==true){
					eventWatcher.handle(event);//TODO: hard-code
				}else{
					resp = exec_cmd(String.format("MS,%02d",index));
					if(resp.length()==0){
						return;
					}					
					setValue(resp);
				}
			});
			
			Button btnReset = PanBase.genButton3("重設","");
			btnReset.setPrefWidth(btn_size);
			btnReset.setOnAction(event->{
				watcher.pause();
				if(chkAllOut.isSelected()==true){
					exec_cmd("DM,110000000000");//TODO: hard-code
				}else{
					exec_cmd(String.format("DS,%02d",index));
				}
				if(chkWatcher.isSelected()==true){
					watcher.play();
				}
			});
			
			JFXCheckBox chkAutoZero = new JFXCheckBox("Auto-Zero");
			
			JFXCheckBox chkTiming = new JFXCheckBox("Timing");
			
			GridPane.setHgrow(btnSetting, Priority.ALWAYS);
			GridPane.setHgrow(btnMeasure, Priority.ALWAYS);
			GridPane.setHgrow(btnReset, Priority.ALWAYS);
			
			add(txtIndex, 0, 0, 1, 1);
			add(btnSetting, 0, 1, 1, 1);
			
			add(txtValue, 1, 0, 1, 2);
			
			add(btnMeasure, 2, 0, 1, 1);
			add(btnReset, 2, 1, 1, 1);
			
			add(chkAutoZero, 3, 0, 1, 1);
			add(chkTiming, 3, 1, 1, 1);
			
			add(new Separator(), 0, 3, 4, 1);
		}
	};
	//-----------------------------------------//
	
	private class PanSetting extends PanBase {

		private WidHeader header;
		
		public PanSetting(WidHeader h){
			header = h;
		}

		@Override
		protected void eventShowing(WindowEvent e){
			Misc.logv("dialog is showing");
		}
		
		@Override
		public Node eventLayout() {
			
			GridPane root = new GridPane();
			root.getStyleClass().add("grid-medium");

			JFXComboBox<String> cmbResolution = new JFXComboBox<String>();
			switch(header.type){
			case 0://displacement				
				cmbResolution.getItems().addAll("0.01mm", "0.001mm", "0.0001mm", "0.00001mm",  "0.1μm", "0.01μm", "0.001μm");
				break;
			case 1://velocity
				cmbResolution.getItems().addAll("100mm/s", "10mm/s", "1mm/s", "0.1mm/s", "0.01mm/s", "0.001mm/s", "0.0001mm/s");
				break;
			case 2://acceleration
				cmbResolution.getItems().addAll("100mm/s²", "10mm/s²", "1mm/s²", "0.1mm/s²", "0.01mm/s²", "0.001mm/s²", "0.0001mm/s²");
				break;
			}
			cmbResolution.getSelectionModel().select(header.unit);//update again,At this time, it must be event thread~~~
			cmbResolution.setOnAction(event->{
				header.setResolution(cmbResolution.getSelectionModel().getSelectedIndex());
			});

			root.addRow(0, new Label("位數："), cmbResolution);			
			return root;
		}
	}
}
