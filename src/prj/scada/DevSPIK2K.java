package prj.scada;

import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import narl.itrc.DevTTY;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

/**
 * SPIK-2000A, plasma generator.<p>
 * It is just a "pulse" generator, with a huge bias voltage.<p>
 * This device use DK3964R protocol(SuperCOM?), it may come from SIEMENS........ 
 * @author qq
 *
 */
public class DevSPIK2K extends DevTTY {

	public DevSPIK2K(){		
	}
	
	/**
	 * connect TTY port and prepare to send commands.<p>
	 * @param path - device name or full name
	 * @return TRUE-success,<p> FALSE-fail to open TTY port
	 */
	public boolean connect(String path){
		return (open(path)>0L)?(true):(false);
	}
	
	public boolean connect(){
		String path = Gawain.prop.getProperty("DevSPIK2K", null);
		if(path==null){
			return false;
		}
		return connect(path);
	}
	
	@Override	
	protected void eventTurnOff(){
		if(core!=null){
			core.cancel();
		}
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
	//private int[] variable = new int[50]; 
	
	/**
	 * Address: 02
	 * Range: 0:Serial1, 1:Serial2, 2:HMS-module, 3:Dualport-RAM 
	 */

	//public IntegerProperty meas_DC2_P_Act  = new SimpleIntegerProperty();
	
	//private static final byte STX = 0x02;
	//private static final byte ETX = 0x03;
	//private static final byte DLE = 0x10;
	//private static final byte NAK = 0x15;
	//private static final byte REV = (byte)0xFF;
	
	public void getVariable(int addr,int cnt){
		/*final byte[] buf = { 
			0x00, 0x00,
			'E', 'D',
			0x00, 0x13,
			0x00, 0x07,
			REV, REV,
			DLE, ETX,
			(byte) 0xA5
		};
		writeBuf(buf);
		//writeByte(DLE);
		byte[] res = readBuf();		
		//writeByte(DLE);*/
		return;
	}
	
	public int getVariable(int addr){
		int res = 0;;
		
		return res;
	}
	
	/*private void waitFor(byte tkn){
		Byte res=null;
		for(;;){
			res = readOneByte();
			if(res!=null){
				if(res==tkn){
					break;
				}
			}			
		}
	}*/
	
	private class TskMonitor extends Task<Long>{
		private long tick = 0;
		@Override
		protected Long call() throws Exception {
			
			updateValue(++tick);
			return 0L;
		}
	};
	private TskMonitor core; 
	
	public void startMonitor(){
		core = new TskMonitor();
		new Thread(core,"SPIK-monitor").start();
	}

	@Override
	protected Node eventLayout(PanBase pan) {		
		GridPane root = new GridPane();//show all sensor
		root.getStyleClass().add("grid-medium");
		
		return root;
	}
	
	/*private GridPane gen_inform(){
		
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
	}*/
	
	/*private class PanSetting extends PanBase {
		
		public PanSetting(){
			propTitle.set("SPIK-2000 設定畫面");
		}

		@Override
		protected void eventShown(WindowEvent e){
			//refresh R/S register~~~
		}
		
		@Override
		public Node eventLayout(PanBase pan) {
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
	private PanSetting panSetting = new PanSetting();*/
}
