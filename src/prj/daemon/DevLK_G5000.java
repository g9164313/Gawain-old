package prj.daemon;

import com.jfoenix.controls.JFXCheckBox;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.DevTTY;
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

	private static final String DEFAULT_PORT_ATTR = ",115200,8n1";
	
	public DevLK_G5000(){		
	}
	
	public DevLK_G5000(String path){
		super(path+DEFAULT_PORT_ATTR);
	}
	
	public void connect(String path){
		if(open(path+DEFAULT_PORT_ATTR)==0L){
			return;
		}
		init_param();
	}
	
	private void init_param(){
		
	}
	
	public void disconnect(){
		close();
	}
	//-----------------------//

	/**
	 * This widget is used for displaying measured value or setting parameter.<p>
	 * In manual, this widget is actually referenced to "OUT".<p>
	 * @author qq
	 *
	 */
	private class WidHeader extends GridPane{
		
		private int index;
		
		public WidHeader(int idx){
			index = idx;
			init_layout();
		}
		
		public StringProperty propValue = new SimpleStringProperty("********");
		
		private void init_layout(){
			
			getStyleClass().add("grid-medium");
			
			Label txtIndex = new Label("編號："+index);
			
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
			
			Button btnSetting = PanBase.genButton1("設定","");
			btnSetting.setAlignment(Pos.BASELINE_LEFT);
			btnSetting.setPrefWidth(btn_size);
			
			Button btnMeasure = PanBase.genButton2("測量","");
			btnMeasure.setPrefWidth(btn_size);

			Button btnReset = PanBase.genButton3("重設","");
			btnReset.setPrefWidth(btn_size);

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
	
	private WidHeader[] out;
	
	@Override
	protected Node eventLayout(){
		
		GridPane root = new GridPane();
		root.getStyleClass().add("grid-medium");
		
		final VBox lay0 = new VBox();
		lay0.disableProperty().bind(isAlive().not());
		
		ComboBox<String> cmbPort = PanTTY.genPortCombo(null,this);
		cmbPort.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
		
		Button btnPort = PanTTY.genPortButton(
			null, this, 
			event->{ 				
				lay0.getChildren().clear();
			}, 
			event->{
				init_param();
				out = new WidHeader[2];
				for(int i=0; i<out.length; i++){
					out[i] = new WidHeader(i+1);//one-base number
				}
				lay0.getChildren().addAll(out);
			}, 
			event->{
				String path = cmbPort.getSelectionModel().getSelectedItem()+DEFAULT_PORT_ATTR;
				DevLK_G5000.this.setInfoPath(path);
			}
		);
		
		JFXCheckBox chkAllOut = new JFXCheckBox("全部");
		chkAllOut.disableProperty().bind(isAlive().not());
		
		JFXCheckBox chkOpt2 = new JFXCheckBox("option2");
		chkOpt2.disableProperty().bind(isAlive().not());
		
		JFXCheckBox chkOpt3 = new JFXCheckBox("option3");
		chkOpt3.disableProperty().bind(isAlive().not());
		

		root.add(cmbPort, 0, 0, 4, 1);
		root.add(btnPort, 5, 0, 1, 1);
		
		root.addRow(1, chkAllOut, chkOpt2, chkOpt3);
		root.add(new Separator(), 0, 2, 6, 1);
		root.add(lay0, 0, 4, 6, 1);
		return root;
	}
}
