package prj.daemon;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import narl.itrc.DevBase;
import narl.itrc.DevTTY;
import narl.itrc.Misc;

/**
 * Tektronix, TDS30xx Series Oscilloscopes.<p>
 * Implement RS-232 communication.
 * @author qq
 *
 */
public class DevTDS3000 extends DevTTY {

	public DevTDS3000() {
		TAG = "TDS3000";
	}

	@Override
	public void afterOpen() {
		addState(STG_INIT,()->state_initial()).
		addState(STG_IDLE,()->state_idleness()).
		addState(STG_ACQU,()->state_acquire());
		playFlow(STG_INIT);
	}
	@Override
	public void beforeClose() {
	}
	
	private static final String STG_IDLE = "idle";
	private static final String STG_INIT = "initial";
	private static final String STG_ACQU = "acquire";
	
	public final BooleanProperty[] ESR = {
		new SimpleBooleanProperty(),//bit-0 in standard Event Status Register
		new SimpleBooleanProperty(),//bit-1 in standard Event Status Register
		new SimpleBooleanProperty(),//etc.....
		new SimpleBooleanProperty(),
		new SimpleBooleanProperty(),
		new SimpleBooleanProperty(),
		new SimpleBooleanProperty(),
		new SimpleBooleanProperty(),
	};
	
	private void state_idleness() {
		final String esr_txt = talk("*ESR?");
		try {			
			//Misc.logv("[%s] ESR=%s", TAG, esr_txt);
			final int val = Integer.valueOf(esr_txt);
			final boolean[] evr_val = new boolean[8];
			for(int i=0; i<evr_val.length; i++) {
				evr_val[i] = ((val & (1<<i))!=0);
			}
			Application.invokeLater(()->{
				for(int i=0; i<evr_val.length; i++) {
					ESR[i].set(evr_val[i]);
				}
			});
		}catch(NumberFormatException e) {
			Misc.loge("[%s] ggyy %s", TAG, e.getMessage());
			return;
		}
		block_sleep_msec(500);
		nextState(STG_IDLE);
	}
	private void state_initial() {
		String ans;
		ans = talk("*IDN?");
		ans = talk("DESE?");
		ans = talk("DATA:ENCDG?");
		ans = talk("WFMPRE?");
		ans = talk("ACQUIRE?");
		//ans = talk("DISPLAY:INTENSITY:WAVEFORM 80");
		nextState(STG_IDLE);
	}
	private void state_acquire() {
		Misc.logv("[%s] acquire", TAG);
		String ans;
		ans = talk("SELECT:CH1 ON");
		//ans = talk("HORIZONTAL:RECORDLENGTH 500");
		//ans = talk("HORIZONTAL:RECORDLENGTH?");
		//int cnt = Integer.valueOf(ans);
		//float[] val = new float[cnt];
		
		ans = talk("DATA?");
		
		ans = talk("DATA:SOURCE?");		
		ans = talk("DATA:WIDTH?");
		ans = talk("DATA:ENCDG?");
		ans = talk("DATA:START?");
		ans = talk("DATA:STOP?");
		ans = talk("CURVE?");
		
		ans = talk("ACQUIRE:MODE SAMPLE");
		ans = talk("ACQUIRE:STOPAFTER SEQUENCE");
		ans = talk("ACQUIRE:STATE RUN");
		//ans = talk("MEASUREMENT:IMMED:TYPE AMPLITUDE");
		//ans = talk("MEASUREMENT:IMMED:SOURCE CH1");
		ans = talk("*WAI");
		
		//ans = talk("MEASUREMENT:MEAS1:VAL?");
		
		ans = talk("DATA:STOP?");
		
		nextState(STG_IDLE);
	}

	private final String eof = "\n";
	
	private String talk(String cmd) {
		return talk(cmd,'1','R');
	}
	private String talk(
		String cmd, 
		final char binary_width,
		final char binary_order				
	) {
		final int timeout = 3000;
		if(port.isPresent()==false) {
			return "";
		}
		final SerialPort dev = port.get();
		if(cmd.endsWith(eof)==false) {
			cmd = cmd + eof;
		}		
		String ans = "";
		try {
			dev.writeString(cmd);
			if(cmd.endsWith("?"+eof)==false) {
				return ans;
			}
			for(;;){					
				char cc = (char)dev.readBytes(1,timeout)[0];
				if(cc=='#') {
					ans = talk_block(dev,timeout,binary_width,binary_order);
				}else {
					ans = ans + cc;
				}					
				if(ans.endsWith(eof)==true) {
					ans = ans.substring(0, ans.length()-eof.length());
					break;
				}
			}			
		} catch (SerialPortException e) {
			Misc.logw("[%s] %s", TAG, e.getMessage());
		} catch (SerialPortTimeoutException e) {
			Misc.logw("[%s] %s", TAG, e.getMessage());
		}		
		return ans;
	}
	private String talk_block(
		final SerialPort dev,
		final int timeout,
		final char binary_width,
		final char binary_order
	) throws SerialPortException, SerialPortTimeoutException {
		//Special case~~~, block mode, data is in binary format,
		//Well sign indicate syntax: #<x><yyy><data>
		//<x> is the number of <y>
		//Ex x is '3', then y is 512 or 999, or 321.
		String ans = "";
		char cc = (char)dev.readBytes(1,timeout)[0];
		int cnt = (int)((cc) - 0x30);
		for(int i=0; i<cnt; i++) {
			cc = (char)dev.readBytes(1,timeout)[0];
			ans = ans + cc;
		}
		cnt = Integer.valueOf(ans);
		ans = "";//reset buffer~~~~
		for(int i=0; i<cnt; i++) {
			switch(binary_width) {
			default:
			case '1':
				cc = (char)dev.readBytes(1,timeout)[0];						
				break;
			case '2':
				byte[] bb = dev.readBytes(2,timeout);
				switch(binary_order) {
				default:
				case 'R':
					cc = (char)(bb[0]<<8 | (bb[1] & 0x0FF));  
					break;
				case 'S':
					cc = (char)(bb[1]<<8 | (bb[0] & 0x0FF));
					break;
				}
				break;
			}
			ans = ans + cc;
		}
		return ans;
	}
		
	private int[] take_curve() {
		//talk("DATA:ENCDG RIBINARY");//ASCII, RIBINARY,RPBINARY, SRIBINARY,SRPBINARY
		//talk("DATA:WIDTH 1");
		
		
		
		final String[] var = talk("DATA?").split(";");		
		final String ans = talk(
			"CURVE?",
			var[5].charAt(0),
			var[0].charAt(0) 	
		);
		
		int[] val = new int[0];
		
		if(var[0].equals("ASCII")==true) {
		
		}else if(
			var[0].equals("RIBINARY")==true ||
			var[0].equals("SRIBINARY")==true
		) {
			final char[] buf = ans.toCharArray();
			val = new int[buf.length];
			for(int i=0; i<val.length; i++) {
				val[i] = (int)buf[i];			
			}
		}else if(
			var[0].equals("RPBINARY")==true ||
			var[0].equals("SRPBINARY")==true			
		) {
			final char[] buf = ans.toCharArray();
			val = new int[buf.length];
			for(int i=0; i<val.length; i++) {
				val[i] = ((int)buf[i]) & 0x0FFFF;			
			}
		}
		return val;
	}	
	public void takeCurve(final XYChart<Number,Number> chart) {
		
		chart.getData().clear();
		
		asyncBreakIn(()->{
						
			final int[] val = take_curve();
			
			final XYChart.Series<Number, Number> series = new XYChart.Series<Number, Number>();
			
			for(int i=0; i<val.length; i++) {
				series.getData().add(new XYChart.Data<Number,Number>(i,val[i]));
			}			
			Application.invokeLater(()->{
				chart.getData().add(series);
			});
		});
	}
	
	//-------------------------------------//
	
	public static Node genCtrlPanel(final DevTDS3000 dev) {

		/*final CheckBox[] chk_status = {
			new CheckBox("PON"),
			new CheckBox("URQ"),	
			new CheckBox("CME"),
			new CheckBox("EXE"),
			new CheckBox("DDE"),
			new CheckBox("QYE"),
			new CheckBox("RQC"),
			new CheckBox("OPC"),
		};
		for(int i=0; i<8; i++) {
			chk_status[i].selectedProperty().bind(dev.ESR[7-i]);
			chk_status[i].setDisable(true);
			chk_status[i].setStyle("-fx-opacity: 1.0;");
		}*/
		
		final LineChart<Number,Number> chart = new LineChart<Number,Number>(
			new NumberAxis(),
			new NumberAxis()
		);
		chart.setPrefSize(320,240);

		final JFXButton btn_acquire = new JFXButton("Test");
		btn_acquire.getStyleClass().add("btn-raised-1");
		btn_acquire.setMaxWidth(Double.MAX_VALUE);
		btn_acquire.setOnAction(e->dev.takeCurve(chart));

		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad");
		
		final VBox lay0 = new VBox(
			chart, 
			//new HBox(chk_status), 
			btn_acquire
		);
		lay0.getStyleClass().addAll("box-pad");		
		return lay0;
	}
	
	public static Node genScreenView(final String ip_addr) {		
		final ImageView screen = new ImageView();
		screen.setFitWidth(-1.);
		screen.setFitHeight(-1.);
		screen.minWidth(640);
		screen.minHeight(480);
		//screen.setImage();
		//"http://"+ip_addr+"/Image.png"
		//final Image img = new Image("http://"+ip_addr+"/Image.png");
		//screen.setImage(img);
		
		/*final Timeline timer = new Timeline(new KeyFrame(
			Duration.seconds(3.),
			e->{
				final Image img = new Image("http://"+ip_addr+"/Image.png");
				screen.setImage(img);
			}
		));		
		timer.setCycleCount(Animation.INDEFINITE);
		timer.playFromStart();*/
		return screen;
	}
	
	/*try {
	URL url = new URL("http://10.10.0.30/Image.png");
	InputStream is = url.openStream();
	OutputStream os = new FileOutputStream("ggyy.png");
	final byte[] b = new byte[2048];
	int length;
	while ((length = is.read(b)) != -1) {
		os.write(b, 0, length);
	}
	is.close();
	is.close();
	} catch (MalformedURLException e) {
		e.printStackTrace();
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}*/
}
