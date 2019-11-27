package prj.daemon;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.DevBase;

/**
 * Support KEYENCE LK-IF library.<p>
 * Connect LK-Navigator to get measurement from USB cable.<p>
 * @author qq
 *
 */
public class DevLKIF2 extends DevBase {

	protected String TAG = "LK-IF";
	
	public DevLKIF2() {	
	}
	
	@Override
	public void open() {
		clear_payload();
		if(openUSB()!=0) {
			return;
		}
		setupState0("looper",looper);
		//playFlow(); 
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
	 * updated by native code.<p>
	 */
	/**
	 * measurement value:<p>
	 * updated by native code.<p>
	 */
 
	private final Runnable looper = new Runnable() {
		@Override
		public void run() {
		}
	};
	
	/**
	 * This is invoked by 'openUSB' for preparing buffer.<p>
	 */
	private void preparePayload(int cntHead, int cntOuts) {
		
		head = new int[cntHead][];
		for(int i=0; i<cntHead; i++) {
			head[i] = new int[12];
			getHead(i,head[i]);
		}
		
		outs = new int[cntOuts][];
		for(int i=0; i<cntOuts; i++) {
			outs[i] = new int[8];
			getOuts(i,outs[i]);
		}
	}
	
	private void clear_payload() {
		info = null;
		head = null;
		outs = null;
	}
	
	private native void getInfo(int[] info);
	private native void setInfo(int[] info);
	
	private native void getHead(int id, int[] head);
	private native void setHead(int id, int[] head);
	
	private native void getOuts(int id, int[] head);
	private native void setOuts(int id, int[] head);
	
	private native void measure();
	
	private native int openUSB();
	private native void closeDev();
	//---------------------
	
	public static Pane genPanel(final DevLKIF2 dev) {
		
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad");
		return lay0;
	}
}
