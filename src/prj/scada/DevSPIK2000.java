package prj.scada;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.WindowEvent;
import narl.itrc.DevTTY;
import narl.itrc.PanBase;

/**
 * SPIK-2000A, plasma generator.<p>
 * It is just a "pulse" generator, with a huge bias voltage.<p>
 * This device use DK3964R protocol(SuperCOM?), it may come from SIEMENS........ 
 * @author qq
 *
 */
public class DevSPIK2000 extends DevTTY {

	public DevSPIK2000(){		
	}
	
	/**
	 * connect TTY port and prepare to send commands.<p>
	 * @param path - device name or full name
	 * @return TRUE-success,<p> FALSE-fail to open TTY port
	 */
	public boolean connect(String path){
		if(open(path,"19200,8n1")<=0L){
			return false;
		}
		return true;
	}
	
	/**
	 * just close TTY device
	 */
	public void disconnect(){
		close();
	}
	//--------------------------------//
	
	//Below lines are mapping device register,
	//It is like Profibus-DP, the register type may be F/C, R/S, F, M or P. 16-bit word.
	//These states may be:
	//[ F/C ] - status the clear, ??
	//[ R/S ] - read and set ??
	//[ F   ] - status, ??
	//[ M   ] - measurement, just a read-only register??
	//[ P   ] - preserve ??
	
	public StringProperty stat_Operation1 = new SimpleStringProperty();	
	public StringProperty stat_Operation2 = new SimpleStringProperty();
	
	/**
	 * Address --> 02
	 * Range--> 0:Serial1, 1:Serial2, 2:HMS-module, 3:Dualport-RAM 
	 */
	public IntegerProperty prop_ComState = new SimpleIntegerProperty();
	
	public StringProperty stat_Error = new SimpleStringProperty();
	
	public IntegerProperty prop_Puls_Pos  = new SimpleIntegerProperty();
	public IntegerProperty prop_Pause_Pos = new SimpleIntegerProperty();
	public IntegerProperty prop_Puls_Neg  = new SimpleIntegerProperty();
	public IntegerProperty prop_Pause_Neg = new SimpleIntegerProperty();
	
	public IntegerProperty prop_ARC_Level_Pos = new SimpleIntegerProperty();
	public IntegerProperty prop_ARC_Level_Neg = new SimpleIntegerProperty();
	public IntegerProperty prop_ARC_Delay     = new SimpleIntegerProperty();
	public IntegerProperty prop_ARC_Overflow  = new SimpleIntegerProperty();
	public IntegerProperty prop_ARC_Intervall = new SimpleIntegerProperty();
	
	public IntegerProperty prop_DC1_V_Set  = new SimpleIntegerProperty();
	public IntegerProperty prop_DC1_I_Set  = new SimpleIntegerProperty();
	public IntegerProperty prop_DC1_P_Set  = new SimpleIntegerProperty();
	public IntegerProperty prop_DC2_V_Set  = new SimpleIntegerProperty();
	public IntegerProperty prop_DC2_I_Set  = new SimpleIntegerProperty();
	public IntegerProperty prop_DC2_P_Set  = new SimpleIntegerProperty();
	
	public IntegerProperty meas_ARC_count  = new SimpleIntegerProperty();
	public IntegerProperty meas_DC1_V_Act  = new SimpleIntegerProperty();
	public IntegerProperty meas_DC1_I_Act  = new SimpleIntegerProperty();
	public IntegerProperty meas_DC1_P_Act  = new SimpleIntegerProperty();
	public IntegerProperty meas_DC2_V_Act  = new SimpleIntegerProperty();
	public IntegerProperty meas_DC2_I_Act  = new SimpleIntegerProperty();
	public IntegerProperty meas_DC2_P_Act  = new SimpleIntegerProperty();
	
	private final double TXT_SIZE = 77.;
	private final String TXT_DC1 = "--- DC 1 ---";
	private final String TXT_DC2 = "--- DC 2 ---";
	
	@Override
	protected Node eventLayout() {
		
		GridPane root = new GridPane();//show all sensor
		root.getStyleClass().add("grid-medium");
		
		Label val;
		
		val = new Label();
		val.setPrefWidth(TXT_SIZE);
		val.textProperty().bind(stat_Operation1);
		root.add(new Label("Mode："), 0, 1);
		root.add(val, 1, 1);
		
		val = new Label();
		val.setPrefWidth(TXT_SIZE);
		val.textProperty().bind(stat_Operation2);
		root.add(new Label("State："), 0, 2);
		root.add(val, 1, 2);
		
		val = new Label();
		val.setPrefWidth(TXT_SIZE);
		val.textProperty().bind(stat_Error);
		root.add(new Label("Error："), 0, 3);
		root.add(val, 1, 3);
		
		Button btn1 = PanBase.genButton2("設定裝置","");
		btn1.setMaxWidth(Double.MAX_VALUE);
		btn1.setOnAction(e->{ panSetting.popup(); });
		root.add(btn1, 0, 4, 2, 1);
		
		root.add(new Separator(Orientation.VERTICAL), 2, 0, 1, 5);
		root.add(gen_inform(), 3, 0, 5, 5);
		return root;
	}
	
	private GridPane gen_inform(){
		
		GridPane root = new GridPane();//show all sensor
		root.getStyleClass().add("grid-medium");

		Label txtARC = new Label();
		txtARC.textProperty().bind(meas_ARC_count.asString());
		root.add(new Label("ARC："), 0, 0);		
		root.add(txtARC, 1, 0, 4, 1);
		
		Label[] txtMeas = {
			new Label(TXT_DC1), new Label(TXT_DC2),
			new Label(), new Label(),
			new Label(), new Label(),
			new Label(), new Label(),
		};
		for(Label txt:txtMeas){
			txt.setPrefWidth(TXT_SIZE);
			txt.setAlignment(Pos.BASELINE_RIGHT);
		}
		txtMeas[2].textProperty().bind(meas_DC1_V_Act.asString());
		txtMeas[3].textProperty().bind(meas_DC1_I_Act.asString());
		txtMeas[4].textProperty().bind(meas_DC1_P_Act.asString());
		txtMeas[5].textProperty().bind(meas_DC2_V_Act.asString());
		txtMeas[6].textProperty().bind(meas_DC2_I_Act.asString());
		txtMeas[7].textProperty().bind(meas_DC2_P_Act.asString());
		
		root.add(txtMeas[0], 1, 1);
		root.add(txtMeas[1], 2, 1);
		
		root.add(new Label("電壓"), 0, 2);
		root.add(new Label("電流"), 0, 3);
		root.add(new Label("功率"), 0, 4);

		root.add(txtMeas[2], 1, 2);
		root.add(txtMeas[3], 1, 3);
		root.add(txtMeas[4], 1, 4);
		
		root.add(txtMeas[5], 2, 2);
		root.add(txtMeas[6], 2, 3);
		root.add(txtMeas[7], 2, 4);
		return root;
	}
	
	private class PanSetting extends PanBase {
		
		public PanSetting(){
			propTitle.set("SPIK-2000 設定畫面");
		}

		@Override
		protected void eventShown(WindowEvent e){
			//refresh R/S register~~~
		}
		
		@Override
		public Node eventLayout() {
			GridPane root = new GridPane();
			root.getStyleClass().add("grid-medium");
			
			root.addRow(0, new Label(""), new Label("正極＋"), new Label("負極－"));
			root.addRow(1,
				new Label("Pulse（μs）"), 
				genBoxInteger(2,32000,prop_Puls_Pos), genBoxInteger(2,32000,prop_Puls_Neg)
			);
			root.addRow(2,
				new Label("Pause（μs）"), 
				genBoxInteger(2,32000,prop_Pause_Pos), genBoxInteger(2,32000,prop_Pause_Neg)
			);
			root.addRow(3,
				new Label("ARC-Level")  , 
				genBoxInteger(0,4000,prop_ARC_Level_Pos) , genBoxInteger(0,4000,prop_ARC_Level_Neg) 
			);
			
			root.add(new Label("ARC-Delay（μs）"), 0, 4);
			root.add(genBoxInteger(30,10000,prop_ARC_Delay) , 1, 4, 2, 1);
			
			root.add(new Label("ARC-Overflow"), 0, 5);
			root.add(genBoxInteger(1,10000,prop_ARC_Overflow), 1, 5, 2, 1);
			
			root.add(new Separator(), 0, 6, 4, 1);
			
			root.addRow(7, new Label(""), new Label(TXT_DC1), new Label(TXT_DC2));
			root.addRow(8,
				new Label("電壓"), 
				genBoxInteger(0,4000,prop_DC1_V_Set), genBoxInteger(0,4000,prop_DC2_V_Set)
			);
			root.addRow(9, 
				new Label("電流"),
				genBoxInteger(0,4000,prop_DC1_I_Set), genBoxInteger(0,4000,prop_DC2_I_Set)
			);
			root.addRow(10,
				new Label("功率"),
				genBoxInteger(0,4000,prop_DC1_P_Set), genBoxInteger(0,4000,prop_DC2_P_Set)
			);
			
			final int BTN_SIZE = 100;
			Button btn1 = PanBase.genButton3("更新", "sync.png");
			btn1.setPrefWidth(BTN_SIZE);
			btn1.setOnAction(e->{
			});
			
			Button btn2 = PanBase.genButton3("套用", "check.png");
			btn2.setPrefWidth(BTN_SIZE);
			btn2.setOnAction(event->{
				dismiss();
			});
			
			HBox lay0 = new HBox(btn1,btn2);
			lay0.setAlignment(Pos.BASELINE_RIGHT);
			lay0.getStyleClass().add("hbox-small");
			root.add(lay0, 0, 11, 4, 11);			
			return root;
		}
	};
	private PanSetting panSetting = new PanSetting();
}
