package prj.scada;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import narl.itrc.DevTTY;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * INFICON SQM-160 Multi-Film Rate/Thickness Monitor
 * Default port setting are "19200,8n1"
 * @author qq
 *
 */
public class DevSQM160 extends DevTTY {

	/**
	 * we can get information about thickness, rate, frequency for each sensor. <p>
	 */
	private class TypeSensor {
		public BooleanProperty propEnable = new SimpleBooleanProperty(true);
		public StringProperty propThick = new SimpleStringProperty("0");
		public StringProperty propRate = new SimpleStringProperty("0");
		public StringProperty propFreq = new SimpleStringProperty("0");
	};

	private class TypeFilm {
		
		public int number = 0; //it is just an index.
		public StringProperty  name = new SimpleStringProperty("???");//name, only 8-character
		public DoubleProperty  density = new SimpleDoubleProperty(0.50);//0.50~99.99 g/cm³
		public IntegerProperty tooling = new SimpleIntegerProperty(33); //10 to 399, unit is '%'
		public FloatProperty  z_ratio = new SimpleFloatProperty(0.1f); //0.10 to 0.999
		public DoubleProperty  final_thick= new SimpleDoubleProperty(0.000);  //0.000 to 9999.000 kÅ
		public DoubleProperty  setpoint1  = new SimpleDoubleProperty(0.000);//0.000 to 9999.000 kÅ
		public StringProperty  setpoint2  = new SimpleStringProperty("00:00");//00:00 to 99:59, time format, mm:ss
		public byte use_sensor = 0x00;//one-bit present one sensor, LSB bit-0 is sensor.1
		
		public TypeFilm(){			
			for(int i=0; i<sensor.length; i++){
				sensor[i] = new TypeSensor();
			}
			update_sensor();
		}

		private TypeSensor[] sensor = new TypeSensor[6];
		
		public void update_sensor(){
			for(int i=0; i<sensor.length; i++){
				int flag = (use_sensor & (0x01<<i));
				if(flag!=0){
					sensor[i].propEnable.set(true);
				}else{
					sensor[i].propEnable.set(false);
				}
			}
		}
	};
	private TypeFilm film = new TypeFilm();

	private class TypeSystem {
		//below lines are system-1 parameter
		/**
		 * 0.10 to 2.00 second, timebase
		 */
		public DoubleProperty timebase = new SimpleDoubleProperty(0.1);		
		/**
		 * '0' indicate simulation is OFF.
		 */
		public BooleanProperty propSimulate = new SimpleBooleanProperty(false);
		
		/**
		 * 0 to 3, indicating rate or thickness unit.
		 */
		public int mode_display = 0;
		/**
		 * The rate unit for sensor.It can be Å/s, nm/s, Hz, μg/cm²/s.<p>
		 */
		public StringProperty propUnitRate = new SimpleStringProperty("A/s");
		/**
		 * The thickness unit for sensor.It can be kÅ, μm, Hz, μg/cm².<p>
		 */
		public StringProperty propUnitThick = new SimpleStringProperty("kÅ");
				
		private void update_display(){
			switch(mode_display){
			case 0:
				propUnitRate.set("Å/s");
				propUnitThick.set("kÅ");
				break;
			case 1:
				propUnitRate.set("nm/s");
				propUnitThick.set("μm");
				break;
			case 2:
				propUnitRate.set("Hz");
				propUnitThick.set("Hz");
				break;
			case 3:
				propUnitRate.set("μg/cm²/s");
				propUnitThick.set("μg/cm²");
				break;
			}
		}
		
		/**
		 * 0 indicates low resolution(0.1Å/s), 1 indicates high display (0.01Å/s)
		 */
		public BooleanProperty propResolution = new SimpleBooleanProperty(false);
		
		/**
		 * 1 to 20, ?? readings
		 */
		public IntegerProperty rate_filter = new SimpleIntegerProperty(1);
		/**
		 * 10 to 399 (%), crystal tooling for each sensor.
		 */
		public IntegerProperty[] tooling = {
			new SimpleIntegerProperty(10),
			new SimpleIntegerProperty(10),
			new SimpleIntegerProperty(10),
			new SimpleIntegerProperty(10),
			new SimpleIntegerProperty(10),
			new SimpleIntegerProperty(10),
			new SimpleIntegerProperty(10)
		};		
		//below lines are system-2 parameter
		public DoubleProperty[] freq = {
			new SimpleDoubleProperty(1),
			new SimpleDoubleProperty(6.400)
		};//range is 1.000 to 6.400 MHz, min and max.
		
		public IntegerProperty[]   rate = {
			new SimpleIntegerProperty(-99),
			new SimpleIntegerProperty(999)
		};//range is -99 to 999 Å/s, min and max.
		
		public DoubleProperty[] thick= {
			new SimpleDoubleProperty(0.),
			new SimpleDoubleProperty(9999.)
		};//range is 0.000 to 9999 kÅ, min and max
		
		public BooleanProperty propEtch = new SimpleBooleanProperty(false);//'0' indicate Etch mode is OFF.
	};
	private TypeSystem system = new TypeSystem();//this structure includes all system parameters
		
	public DevSQM160(String path) {
		this();
		connect(path);
	}

	public DevSQM160() {
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
		//dig up information~~~
		return (open(path)>0L)?(true):(false);
	}
	
	/**
	 * just close TTY device
	 */
	public void disconnect(){
		close();
	}

	/**
	 * keep the last command which user send~~~
	 */
	private StringProperty propLastCommand = new SimpleStringProperty("");
	
	/**
	 * keep the last command status~~~
	 */
	private StringProperty propLastStatus = new SimpleStringProperty("");
	
	/**
	 * keep the last message from device~~~
	 */
	private StringProperty propLastMessage = new SimpleStringProperty("");
	
	/**
	 * Implement SQM-160 communications protocol.<p>
	 * Exclude 'sync' character, length and CRC.<p>
	 * It will synchronize fetch response packet.<p>
	 * @param cmd - just command, excluding 'sync' character, length and CRC
	 * @return
	 */
	public String exec(String cmd) {
		send_command(cmd);
		update_response(have_response());		
		return propLastMessage.get();
	}

	private Task<String> tskExec = null;
	/**
	 * Implement SQM-160 communications protocol.<p>
	 * It is same as "exec", but response is taken by another thread.<p>
	 * Just an a-synchronize response process.<p>
	 * @param cmd - just command, excluding 'sync' character, length and CRC
	 * @param event - callback event
	 * @return
	 */
	public void exec(
		String cmd,
		EventHandler<ActionEvent> event
	) {
		send_command(cmd);
		
		if(tskExec!=null){
			if(tskExec.isRunning()==true){
				Misc.logw("DevSQM160-exec is running");
				return;
			}
		}		
		tskExec = new Task<String>(){
			@Override
			protected String call() throws Exception {
			
				return null;
			}
		};
		tskExec.setOnSucceeded(e->{
			update_response(tskExec.valueProperty().get());
			event.handle(new ActionEvent(propLastMessage.get(),null));
		});
		new Thread(tskExec,"DevSQM160-exec").start();
	}
	
	private void send_command(String cmd){
		
		char tkn = (char) (cmd.length() + 34);
		
		cmd = "!" + tkn + cmd;
		
		short val = calcCrc(cmd.toCharArray());
		
		char crc1 = (char)(crcLow(val));
		
		char crc2 = (char)(crcHigh(val));
		
		cmd = cmd + crc1  + crc2;
		
		propLastCommand.set(cmd);
		
		writeTxt(cmd);
	}
	
	private String have_response(){
		char tkn;
		short val1,val2;
		
		//first, wait 'Sync' character
		do{
			tkn = readChar();
		}while(tkn!='!');
		
		//second byte is the length of response message.
		tkn = readChar();
		val2 = (short)(tkn-34);
		String resp = "";
		for(val1=0; val1<val2; val1++){
			tkn = readChar();
			resp = resp + tkn;
		}
		
		//get the final CRC code
		tkn = readChar();			
		val1 = (short) (val1 + (int)tkn - 34);
		tkn = readChar();
		val2 = (short) (val1 + (int)tkn - 34);
		val2 = (short)(val2 << 8);
		
		val1 = (short)(val2 | val1);
		val2 = calcCrc(resp.toCharArray());
		
		if(val1!=val2){
			//how to deal with this condition????
		}
		return resp;
	}
	
	private void update_response(String resp){
		switch(resp.charAt(0)){
		case 'A': propLastStatus.set("Normal response"); break;
		case 'C': propLastStatus.set("Invalid command - "+propLastCommand.get()); break;
		case 'D': propLastStatus.set("Problem with data command"); break; 
		}
		propLastMessage.set(resp.substring(1));
	}
	
	private short calcCrc(char[] str) {
		short crc = 0;
		short tmpCRC;
		int length = 1 + str[1] - 34;
		if (length > 0) {
			crc = (short) 0x3fff;
			for (int jx = 1; jx <= length; jx++) {
				crc = (short) (crc ^ (short) str[jx]);
				for (int ix = 0; ix < 8; ix++) {
					tmpCRC = crc;
					crc = (short) (crc >> 1);
					if ((tmpCRC & 0x1) == 1) {
						crc = (short) (crc ^ 0x2001);
					}
				}
				crc = (short) (crc & 0x3fff);
			}
		}
		return crc;
	}

	private byte crcLow(short crc) {
		byte val = (byte) ((crc & 0x7f) + 34);
		return val;
	}
	
	private byte crcHigh(short crc) {
		byte val = (byte) (((crc >> 7) & 0x7f) + 34);
		return val;
	}
	//--------------------------------//

	/**
	 * Film name, it must be changed manually~~~~
	 */
	private SimpleStringProperty propNameFilm = new SimpleStringProperty("???");
	
	/**
	 * We can read a average thickness value from device.<p>
	 */
	private SimpleStringProperty propAvgThick = new SimpleStringProperty("0");

	/**
	 * We can read a average rate value from device.<p>
	 */
	private SimpleStringProperty propAvgRate = new SimpleStringProperty("0");
	
	private final String TXT_SETTING1 = "設定薄膜";
	private final String TXT_SETTING2 = "設定裝置";	
	private final int SPIN_SIZE = 120;
	private final int TBOX_SIZE = 80;
	
	private class PanSetFilm extends PanBase {
		
		public PanSetFilm(){
			propTitle.set(TXT_SETTING1);
		}
		
		@Override
		public Node eventLayout() {
			GridPane root = new GridPane();
			root.getStyleClass().add("grid-medium");
			
			JFXComboBox<Integer> cmbNumber = new JFXComboBox<Integer>();
			cmbNumber.setPrefWidth(SPIN_SIZE);
			for(int i=1; i<=99; i++){
				cmbNumber.getItems().add(i);
			}
			cmbNumber.setOnAction(event->{
				//change 
			});
			
			JFXTextField boxName = new JFXTextField();
			boxName.setPrefWidth(TBOX_SIZE);
			boxName.textProperty().bindBidirectional(film.name);
			
			Spinner<Number> spnDensity = new Spinner<Number>(0.5, 99.99, 1.);
			spnDensity.setPrefWidth(SPIN_SIZE);
			spnDensity.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnDensity.getValueFactory().valueProperty().bindBidirectional(film.density);

			Spinner<Number> spnTooling = new Spinner<Number>(10, 399, 1);
			spnTooling.setPrefWidth(SPIN_SIZE);
			spnTooling.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnTooling.getValueFactory().valueProperty().bindBidirectional(film.tooling);
			
			Spinner<Float> spnZRatio = PanBase.genSpinnerFloat(0.1f, 9.999f, 0.001f, film.z_ratio);
			spnZRatio.setPrefWidth(SPIN_SIZE);
			spnZRatio.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);

			Spinner<Number> spnFinalThick = new Spinner<Number>(0., 9999., 1.);
			spnFinalThick.setPrefWidth(SPIN_SIZE);
			spnFinalThick.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnFinalThick.getValueFactory().valueProperty().bindBidirectional(film.final_thick);
			
			Spinner<Number> spnSetpoint1 = new Spinner<Number>(0., 9999., 1.);
			spnSetpoint1.setPrefWidth(SPIN_SIZE);
			spnSetpoint1.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnSetpoint1.getValueFactory().valueProperty().bindBidirectional(film.setpoint1);
			
			JFXTextField boxSetpoint2 = new JFXTextField();
			boxSetpoint2.setPrefWidth(TBOX_SIZE);
			boxSetpoint2.textProperty().bindBidirectional(film.setpoint2);
			//TODO: boxSetpoint2.getValueFactory().valueProperty().bindBidirectional(film.setpoint2);
			
			for(int i=0; i<film.sensor.length; i++){
				JFXCheckBox chkSensor = new JFXCheckBox("Sensor-"+i);
				chkSensor.selectedProperty().bindBidirectional(film.sensor[i].propEnable);
				root.add(chkSensor, 3, i+1);
			}
			
			Label[] txtTitle = {
				new Label("編號/名稱"),
				new Label("Density（g/cm³）"),
				new Label("Tooling（%）"),
				new Label("Z-Ratio"),
				new Label("Final Thickness"),				
				new Label("最終厚度（kÅ）"),
				new Label("最終時間（mm:ss）"),
			};
			
			root.add(txtTitle[0]  , 0, 0);
			root.add(cmbNumber    , 1, 0);
			root.add(boxName      , 2, 0, 2, 1);
			root.add(txtTitle[1]  , 0, 1);
			root.add(spnDensity   , 1, 1);
			root.add(txtTitle[2]  , 0, 2);
			root.add(spnTooling   , 1, 2);
			root.add(txtTitle[3]  , 0, 3);
			root.add(spnZRatio    , 1, 3);
			root.add(txtTitle[4]  , 0, 4);
			root.add(spnFinalThick, 1, 4);			
			root.add(txtTitle[5]  , 0, 5);
			root.add(spnSetpoint1 , 1, 5);			
			root.add(txtTitle[6]  , 0, 6);
			root.add(boxSetpoint2 , 1, 6);			
			root.add(new Separator(Orientation.VERTICAL), 2, 1, 1, 6);
			
			bottom_ctrl(root);			
			return root;
		}

		private void bottom_ctrl(GridPane root){
			final int BTN_SIZE = 100;
			
			Label txtStatus = new Label();
			txtStatus.textProperty().bind(propLastStatus);
			
			Button btn1 = PanBase.genButton3("更新", "sync.png");
			btn1.setPrefWidth(BTN_SIZE);
			btn1.setOnAction(event->{
				
			});
			Button btn2 = PanBase.genButton3("套用", "check.png");
			btn2.setPrefWidth(BTN_SIZE);
			btn2.setOnAction(event->{
				
			});
			
			HBox lay0 = new HBox(btn1,btn2);
			lay0.setAlignment(Pos.BASELINE_RIGHT);
			lay0.getStyleClass().add("hbox-small");
			root.add(txtStatus, 0, 9);
			root.add(lay0, 1, 9, 7, 1);
		}
	};	
	private PanSetFilm panSetting1 = new PanSetFilm();
		
	private class PanSetSystem extends PanBase {

		public PanSetSystem(){
			propTitle.set(TXT_SETTING2);
		}

		@Override
		public Node eventLayout() {
			GridPane root = new GridPane();
			root.getStyleClass().add("grid-medium");			
			section_1(root);
			section_2(root);
			section_3(root);
			bottom_ctrl(root);
			return root;
		}

		private void section_1(GridPane root){
			JFXCheckBox chkSimulation = new JFXCheckBox("模擬");
			chkSimulation.selectedProperty().bindBidirectional(system.propSimulate);
			
			JFXCheckBox chkEtch = new JFXCheckBox("蝕刻模式");
			chkEtch.selectedProperty().bindBidirectional(system.propEtch);
			
			JFXCheckBox chkResolution = new JFXCheckBox("高解析度");
			chkResolution.selectedProperty().bindBidirectional(system.propResolution);
			
			Spinner<Number> spnFilter = new Spinner<Number>(1,20,1);
			spnFilter.setPrefWidth(SPIN_SIZE);
			spnFilter.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnFilter.getValueFactory().valueProperty().bindBidirectional(system.rate_filter);
			
			Spinner<Number> spnTimebase = new Spinner<Number>(0.,99.,1.);
			spnTimebase.setPrefWidth(SPIN_SIZE);
			spnTimebase.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnTimebase.getValueFactory().valueProperty().bindBidirectional(system.timebase);
			
			root.add(chkSimulation,         0, 1, 2, 1);
			root.add(chkEtch      ,         0, 2, 2, 1);
			root.add(chkResolution,         0, 3, 2, 1);
			root.add(new Label("Filter"),   0, 4);
			root.add(spnFilter,             1, 4);
			root.add(new Label("Timebase"), 0, 5);
			root.add(spnTimebase,           1, 5);
			root.add(new Separator(Orientation.VERTICAL), 2, 0, 1, 7);
		}
		
		private void section_2(GridPane root){
			
			Spinner<Number> spnMinThick = new Spinner<Number>(0.,9999.,1.);
			spnMinThick.setPrefWidth(SPIN_SIZE);
			spnMinThick.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnMinThick.getValueFactory().valueProperty().bindBidirectional(system.thick[0]);
			
			Spinner<Number> spnMaxThick = new Spinner<Number>(0.,9999.,1.);
			spnMaxThick.setPrefWidth(SPIN_SIZE);
			spnMaxThick.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnMaxThick.getValueFactory().valueProperty().bindBidirectional(system.thick[1]);
			
			Spinner<Number> spnMinRate = new Spinner<Number>(-99.,999.,1.);
			spnMinRate.setPrefWidth(SPIN_SIZE);
			spnMinRate.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnMinRate.getValueFactory().valueProperty().bindBidirectional(system.rate[0]);
			
			Spinner<Number> spnMaxRate = new Spinner<Number>(-99.,999.,1.);
			spnMaxRate.setPrefWidth(SPIN_SIZE);
			spnMaxRate.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnMaxRate.getValueFactory().valueProperty().bindBidirectional(system.rate[1]);
			
			Spinner<Number> spnMinFreq = new Spinner<Number>(1.,6.4,0.1f);
			spnMinFreq.setPrefWidth(SPIN_SIZE);
			spnMinFreq.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnMinFreq.getValueFactory().valueProperty().bindBidirectional(system.freq[0]);
			
			Spinner<Number> spnMaxFreq = new Spinner<Number>(1.,6.4,0.1f);
			spnMaxFreq.setPrefWidth(SPIN_SIZE);
			spnMaxFreq.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
			spnMaxFreq.getValueFactory().valueProperty().bindBidirectional(system.freq[1]);
			
			Label[] txtTitle = {
				new Label("類比輸出"),
				new Label("最小厚度（kÅ）"),
				new Label("最大厚度（kÅ）"),
				new Label("最小速率（Å/s）"),
				new Label("最大速率（Å/s）"),
				new Label("最小頻率（Hz）"),
				new Label("最大頻率（Hz）"),
			};
			
			root.add(txtTitle[0], 3, 0, 3,1);
			root.add(txtTitle[1], 3, 1);
			root.add(spnMinThick, 4, 1);
			root.add(txtTitle[2], 3, 2);
			root.add(spnMaxThick, 4, 2);
			root.add(txtTitle[3], 3, 3);
			root.add(spnMinRate,  4, 3);
			root.add(txtTitle[4], 3, 4);
			root.add(spnMaxRate,  4, 4);
			root.add(txtTitle[5], 3, 5);
			root.add(spnMinFreq,  4, 5);
			root.add(txtTitle[6], 3, 6);
			root.add(spnMaxFreq,  4, 6);
			root.add(new Separator(Orientation.VERTICAL), 5, 0, 1, 7);
		}
		
		private void section_3(GridPane root){
			root.add(new Label("X Tooling"), 6, 0, 3, 1);
			for(int i=0; i<6; i++){
				Spinner<Integer> spnTool = new Spinner<Integer>(10,399,1);
				spnTool.setPrefWidth(SPIN_SIZE);
				spnTool.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
				root.add(new Label("Crystal-1"), 6, i+1);
				root.add(spnTool, 7, i+1);
			}
		}

		private void bottom_ctrl(GridPane root){
			
			Label txtStatus = new Label();
			txtStatus.textProperty().bind(propLastStatus);
			
			final int BTN_SIZE = 100;
			Button btn1 = PanBase.genButton3("更新", "sync.png");
			btn1.setPrefWidth(BTN_SIZE);
			btn1.setOnAction(event->{
				
			});
			Button btn2 = PanBase.genButton3("套用", "check.png");
			btn2.setPrefWidth(BTN_SIZE);
			btn2.setOnAction(event->{
				
			});
			
			HBox lay0 = new HBox(btn1,btn2);
			lay0.setAlignment(Pos.BASELINE_RIGHT);
			lay0.getStyleClass().add("hbox-small");
			root.add(txtStatus, 0, 9);
			root.add(lay0, 1, 9, 7, 1);
		}
	};
	private PanSetSystem panSetting2 = new PanSetSystem();
	
	@Override
	protected Node eventLayout() {

		final int DISP_SIZE = 53;
		
		GridPane panSensor = new GridPane();//show all sensor
		panSensor.getStyleClass().add("grid-small");
		
		Label txtName1 = new Label("厚度");
		txtName1.setPrefWidth(DISP_SIZE);
		
		Label txtName2 = new Label("速率");
		txtName2.setPrefWidth(DISP_SIZE);
		
		Label txtName3 = new Label("頻率");
		txtName3.setPrefWidth(DISP_SIZE);
		
		panSensor.addRow(0, new Label(), txtName1,txtName2, txtName3 );
		
		for(int i=0; i<film.sensor.length; i++){
			
			Label txtName = new Label(String.format("編號%d)  ", i+1));
			txtName.visibleProperty().bind(film.sensor[i].propEnable);
			
			Label txtVal1 = new Label();
			txtVal1.textProperty().bind(film.sensor[i].propThick);
			txtVal1.visibleProperty().bind(film.sensor[i].propEnable);
			
			Label txtVal2 = new Label();
			txtVal2.textProperty().bind(film.sensor[i].propRate);
			txtVal2.visibleProperty().bind(film.sensor[i].propEnable);
			
			Label txtVal3 = new Label();
			txtVal3.textProperty().bind(film.sensor[i].propFreq);
			txtVal3.visibleProperty().bind(film.sensor[i].propEnable);
			
			panSensor.addRow(i+1, txtName, txtVal1, txtVal2, txtVal3);
		}

		GridPane root = new GridPane();
		root.getStyleClass().add("grid-medium");

		final String DISP_STYLE = "-fx-font-size: 17px;";
		
		Label txtTitleFilm = new Label("薄膜名稱：");
		txtTitleFilm.setStyle(DISP_STYLE);
		txtTitleFilm.textProperty().bind(film.name);
		
		Label txtNameFilm = new Label("");
		txtNameFilm.setStyle(DISP_STYLE);
		txtNameFilm.textProperty().bind(propNameFilm);
		
		Label txtNameThick = new Label("平均厚度：");
		txtNameThick.setStyle(DISP_STYLE);
		
		Label txtAvgThick = new Label();	
		txtAvgThick.textProperty().bind(propAvgThick);
		txtAvgThick.setAlignment(Pos.BASELINE_RIGHT);
		txtAvgThick.setPrefWidth(DISP_SIZE);
		txtAvgThick.setStyle(DISP_STYLE);
		
		Label txtUnitThick = new Label();
		txtUnitThick.textProperty().bind(system.propUnitThick);
		txtUnitThick.setStyle(DISP_STYLE);
		
		Label txtNameRate =new Label("平均速率：");
		txtNameRate.setStyle(DISP_STYLE);
		
		Label txtAvgRate = new Label();		
		txtAvgRate.textProperty().bind(propAvgRate);
		txtAvgRate.setAlignment(Pos.BASELINE_RIGHT);
		txtAvgRate.setPrefWidth(DISP_SIZE);
		txtAvgRate.setStyle(DISP_STYLE);
		
		Label txtUnitRate = new Label();
		txtUnitRate.textProperty().bind(system.propUnitRate);
		txtUnitRate.setStyle(DISP_STYLE);
		
		Button btnSetFilm = PanBase.genButton2(TXT_SETTING1,"");
		btnSetFilm.setMaxWidth(Double.MAX_VALUE);
		btnSetFilm.setOnAction(event->panSetting1.popup());
		
		Button btnSetSystem = PanBase.genButton2(TXT_SETTING2,"");
		btnSetSystem.setMaxWidth(Double.MAX_VALUE);
		btnSetSystem.setOnAction(event->panSetting2.popup());
		
		root.addRow(0, txtTitleFilm, txtNameFilm);
		root.addRow(1, txtNameThick, txtAvgThick, txtUnitThick);
		root.addRow(2, txtNameRate , txtAvgRate , txtUnitRate );
		root.add(btnSetFilm  , 0, 3, 3, 1);
		root.add(btnSetSystem, 0, 4, 3, 1);
		
		root.add(new Separator(Orientation.VERTICAL), 4, 0, 1, 5);
		root.add(panSensor, 5, 0, 6, 6);
		return root;
	}
}