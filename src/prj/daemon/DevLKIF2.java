package prj.daemon;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import narl.itrc.DevBase;

/**
 * Support KEYENCE LK-IF library.<p>
 * Connect LK-Navigator to get measurement from USB cable.<p>
 * @author qq
 *
 */
public class DevLKIF2 extends DevBase {

	public DevLKIF2() {	
		TAG = "LK-IF";
		for(int i=0; i<txtValue.length; i++) {
			txtValue[i] = new SimpleStringProperty("???");
		}
	}
	
	private static final String STA_LOOPER = "looper";
	
	@Override
	public void open() {
		clear_payload();
		if(openUSB()!=0) {
			return;
		}
		if(head==null) {
			return;
		}
		addState(STA_LOOPER,()->looper());
		playFlow(STA_LOOPER); 
	}

	@Override
	public void close() {
		stopFlow();
		closeDev();
		clear_payload();
	}

	@Override
	public boolean isLive() {
		return (head==null)?(true):(false);
	}

	private void looper() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			return;
		}
		
		measure(rest,meas);
		
		for(int i=0; i<rest.length; i++) {
			String txt = "???";
			switch(rest[i]) {
			case '@':
				int typ = outs[i][20];
				int vid = outs[i][19];
				txt = String.format(
					pttn[typ][vid],
					meas[i],
					unit[typ][vid]
				);
				break;
			case '+': txt = "**超過上限**"; break;
			case '-': txt = "**超過下限**"; break;
			case 'w': txt = "--等待量測--"; break;
			case 'a': txt = "!!警報!!"; break;
			case 'x': txt = "!!錯誤!!"; break;
			}
			ref_txt[i] = txt;
		}
		Application.invokeAndWait(()->{
			for(int i=0; i<rest.length; i++) {
				txtValue[i].set(ref_txt[i]);
			}
		});
	}
	
	private final String[] pttn[] = {
		//Displacement
		{"%.2f%s","%.3f%s","%.4f%s","%.5f%s",  "%.1f%s","%.2f%s","%.3f%s"},
		//Speed
		{"%.1f%s","%.2f%s","%.3f%s",  "%.1f%s","%.2f%s","%.3f%s","%.4f%s"},
		//Acceleration
		{"%.1f%s","%.2f%s","%.3f%s",  "%.1f%s","%.2f%s","%.3f%s","%.4f%s"},
	};
	private final String[] unit[] = {
		//Displacement
		{"mm","mm","mm","mm",  "μm","μm","μm"},
		//Speed
		{"m/s","m/s","m/s", "mm/s","mm/s","mm/s","mm/s"},
		//Acceleration
		{"km/s²","km/s²","km/s²", "m/s²","m/s²","m/s²","m/s²"},
	};
	
	/**
	 * General information for all head and output.<p>
	 */
	private int[] info = null;
	/**
	 * head - prober or analyst.<p>
	 * All values are enumeration in C.<p>
	 */
	private int[][] head = null;
	/**
	 * out - it means value displayed from head.<p>
	 * All values are enumeration in C.<p>
	 */
	private int[][] outs = null;	
	
	/**
	 * measurement result:<p>
	 * @: valid data. <p>
	 * +: over range at positive (+) side. <p>
	 * -: over range at negative (-) side. <p>
	 * w: waiting comparator result. <p>
	 * a: alarm
	 * x: error, invalid value
	 * array index is mapped to output index.<p>
	 * EX: 'rest[0]' means output-0 result.<p>
	 */
	private char[] rest = null;
	/**
	 * measurement value.<p>
	 * array index is mapped to output index.<p>
	 * EX: 'meas[0]' means output-0 measurement.<p>
	 */
	private float[] meas = null;
	
	/**
	 * This is invoked by 'openUSB' for preparing buffer.<p>
	 */
	private void preparePayload(int cntHead, int cntOuts) {
		info = new int[4];
		if(cntHead!=0) {
			head = new int[cntHead][];
			for(int i=0; i<cntHead; i++) {
				head[i] = new int[12];
			}
		}
		if(cntOuts!=0) {
			outs = new int[cntOuts][];
			for(int i=0; i<cntOuts; i++) {
				outs[i] = new int[21];
			}
			rest = new char[cntOuts];
			meas = new float[cntOuts];
		}
	}
	
	private void clear_payload() {
		info = null;
		head = null;
		outs = null;
		
		rest = null;
		meas = null;
	}
	
	public void resetValue(final int outID) {asyncBreakIn(()->{
		implResetValue(outID);
		nextState(STA_LOOPER);
	});}

	/**
	 * get measurement result. The character means: <p>
	 * @: valid data. <p>
	 * +: over range at positive (+) side. <p>
	 * -: over range at negative (-) side. <p>
	 * w: waiting comparator result. <p>
	 * a: alarm
	 * x: error, invalid value
	 * @param id
	 * @return
	 */
	public char getResult(final int id) {
		if(rest==null) {
			return 'x';
		}else if(id>=rest.length) {
			return 'x';
		}
		return rest[id];
	}
	public float getValue(final int id) {
		if(meas==null) {
			return 0f;
		}else if(id>=meas.length) {
			return 0f;
		}
		return meas[id];
	}
	public String getUnit(final int id) {
		if(outs==null) {
			return "";
		}else if(id>=outs.length) {
			return "";
		}
		int typ = outs[id][20];
		int vid = outs[id][19];
		return unit[typ][vid];
	}
	
	private native void getInfo(int[] info);
	private native void setInfo(int[] info);
	
	private native void getHead(int id, int[] head);
	private native void setHead(int id, int[] head);
	
	private native void getOuts(int id, int[] head);
	private native void setOuts(int id, int[] head);
	
	private native void measure(char[] attr, float[] meas);
	
	private native void implResetValue(int outID);
	
	private native int openUSB();
	private native void closeDev();
	//---------------------
	
	private final static int MAX_OUT = 12;
	
	private final String[] ref_txt = new String[MAX_OUT];
	
	private final StringProperty[] txtValue = new StringProperty[MAX_OUT];	
	
	public static Pane genPanel(final DevLKIF2 dev, final int outID) {
		
		final JFXButton[] btn = {
			new JFXButton("歸零"),
			new JFXButton("設定"),
		};
		for(int i=0; i<btn.length; i++) {
			btn[i].setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			GridPane.setHgrow(btn[i], Priority.ALWAYS);
			GridPane.setVgrow(btn[i], Priority.ALWAYS);
		}
		btn[0].setOnAction(e->dev.resetValue(outID));
		
		final Font ft = new Font(43);
		final Label txt = new Label();
		txt.setMinSize(250, 48);
		txt.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		txt.setFont(ft);
		txt.setAlignment(Pos.CENTER_RIGHT);
		txt.textProperty().bind(dev.txtValue[outID]);		
		GridPane.setHgrow(txt, Priority.ALWAYS);
		GridPane.setVgrow(txt, Priority.ALWAYS);
		
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().add("box-border");
		lay0.add(btn[0], 0, 0, 1, 1);
		lay0.add(btn[1], 0, 1, 1, 1);
		lay0.add(txt, 1, 0, 1, 2);
		return lay0;
	}
	
	public static Pane genPanelMulti(final DevLKIF2 dev, final int... outID) {
		int cnt = outID.length;
		if(cnt>=MAX_OUT) {
			cnt = MAX_OUT;
		}
		final VBox lay0 = new VBox();
		lay0.getStyleClass().add("box-pad-inner");
		for(int i=0; i<cnt; i++) {
			final Pane lay = genPanel(dev,i);
			lay0.getChildren().add(lay);
		}
		return lay0;
	}
}
