package narl.itrc;

import narl.itrc.camsetting.PanAny;
import narl.itrc.camsetting.PanVFW;
import javafx.scene.Parent;

public class CamVidcap extends CamBundle {

	public CamVidcap() {
	}

	public CamVidcap(String config) {
		super(config);
	}

	private static final int 
		CAP_ANY = 0, // autodetect
		CAP_VFW = 200, // platform native,V4L, V4L2
		CAP_FIREWARE = 300, // IEEE 1394 drivers
		CAP_QT = 500, // QuickTime
		CAP_UNICAP = 600, // Unicap drivers
		CAP_DSHOW = 700, // DirectShow (via videoInput)
		CAP_PVAPI = 800, // PvAPI, Prosilica GigE SDK
		CAP_OPENNI = 900, // OpenNI (for Kinect)
		CAP_OPENNI_ASUS = 910, // OpenNI (for Asus Xtion)
		CAP_XIAPI = 1100, // XIMEA Camera API
		CAP_AVFOUNDATION = 1200, // AVFoundation framework for iOS (OS X-Lion will have the same API)
		CAP_GIGANETIX = 1300, // Smartek Giganetix GigEVisionSDK
		CAP_MSMF = 1400, // Microsoft Media Foundation (via videoInput)
		CAP_WINRT = 1410, // Microsoft Windows Runtime using Media Foundation
		CAP_INTELPERC = 1500, // Intel Perceptual Computing SDK
		CAP_OPENNI2 = 1600, // OpenNI2 (for Kinect)
		CAP_OPENNI2_ASUS = 1610, // OpenNI2 (for Asus Xtion and Occipital Structure sensors)
		CAP_GPHOTO2 = 1700, // gPhoto2 connection
		CAP_GSTREAMER = 1800, // GStreamer
		CAP_FFMPEG = 1900, // FFMPEG
		CAP_IMAGES = 2000; // OpenCV Image Sequence (e.g. img_%02d.jpg)

	private int name2value(String name){
		int val = CAP_ANY;//default domain~~
		if (
			name.equalsIgnoreCase("IMAGE") == true ||
			name.equalsIgnoreCase("FILE") == true
		) {
			val = CAP_IMAGES;//image sequence~~~~
		}else if (
			name.equalsIgnoreCase("VFW") == true || 
			name.equalsIgnoreCase("V4L") == true || 
			name.equalsIgnoreCase("V4L2") == true
		) {			
			val = CAP_VFW;			
		} else if (
			name.equalsIgnoreCase("FIREWARE") == true || 
			name.equalsIgnoreCase("1394") == true
		) {			
			val = CAP_FIREWARE;			
		} else if (name.equalsIgnoreCase("QT") == true) {			
			val = CAP_QT;			
		} else if (name.equalsIgnoreCase("UNICAP") == true) {			
			val = CAP_UNICAP;			
		} else if (name.equalsIgnoreCase("PVAPI") == true) {
			val = CAP_PVAPI;
		}else if ( name.equalsIgnoreCase("GIGE") == true ) {
			val = CAP_GIGANETIX;
		} else if (
			name.equalsIgnoreCase("XIMEA") == true || 
			name.equalsIgnoreCase("XAPI") == true
		) {
			val = CAP_XIAPI;		
		}
		return val;
	}

	/**
	 * The type of camera source
	 */
	private int capDomain = CAP_ANY;
	
	/**
	 * Camera index
	 */
	private int capIndex = 0;
	
	/**
	 * This variable is only for CAP_IMAGES, name must have leading zero!!!
	 */
	private String capConfig = Misc.pathRoot + "img1_%03d.png";
		
	private native void implSetup(CamBundle cam);
	private native void implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);

	public native boolean setProp(CamBundle cam,int id, double val);
	public native double getProp(CamBundle cam,int id);

	public int getIndex(){ return capIndex; }
	
	@Override
	public void setup() {
		// e.g: "winrt:0", open the first camera via WinRT
		String[] args = txtConfig.split(":");
		try {
			capDomain= CAP_ANY;
			capIndex = 0;
			switch(args.length){
			default:
			case 3:				
				capDomain= name2value(args[0].trim());
				capIndex = Integer.valueOf(args[1].trim());
				capConfig= args[2].trim();
				break;
			case 2:
				capDomain= name2value(args[0].trim());
				capIndex = Integer.valueOf(args[1].trim());
				break;
			case 1:
				capIndex = Integer.valueOf(args[0].trim());
				break;
			}
		} catch (NumberFormatException e) {
			Misc.loge("Wrong configure - " + txtConfig);
		}
		implSetup(this);
	}

	@Override
	public void fetch() {
		implFetch(this);
	}

	@Override
	public void close() {
		implClose(this);
	}
	// -----------------------//

	/*private Parent getPanel0() {
		GridPane root = new GridPane();
		root.getStyleClass().add("grid-small");
		
		final int[] opt = {
			CAP_PROP_FRAME_WIDTH,
			CAP_PROP_FRAME_HEIGHT,
			CAP_PROP_EXPOSURE,
			CAP_PROP_FORMAT
		};
		final String[] txt = {
			"影像寬",
			"影像高",
			"曝光值"
		};
		final BoxIntValue[] box = {
			new BoxIntValue(txt[0]),
			new BoxIntValue(txt[1]),
			new BoxIntValue(txt[2])
		};
		
		for(int i=0; i<opt.length; i++){
			int val = (int)getProp(CamVidcap.this,opt[i]);
			box[i].setInteger(val);
			box[i].setEvent(event->{
				setProp(
					CamVidcap.this,
					opt[0],
					box[0].propValue.get()
				);
			});
		}

		root.addRow(0,new Label(txt[0]), box[0]);
		root.addRow(1,new Label(txt[1]), box[1]);
		root.addRow(2,new Label(txt[2]), box[2]);
		return root;
	}*/

	@Override
	public Parent genPanelSetting(PanBase pan) {
		switch (capDomain) {
		default:
		case CAP_ANY: return new PanAny(pan,this);
		case CAP_VFW: return new PanVFW(pan,this);			
		}
	}
}
