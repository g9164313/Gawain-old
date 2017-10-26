package narl.itrc.vision;

public class CamVidcap extends CamBundle {

	public CamVidcap() {
	}

	public CamVidcap(String name) {
		if(
			name.endsWith(".avi")==true || 
			name.endsWith(".mp4")==true 
		){
			capDomain = CAP_FFMPEG;
		}else{
			capDomain = CAP_IMAGES;
		}
		capIndex = 0;
	}
	//-----------------------//
	

	/**
	 * The type of camera source
	 */
	private int capDomain = CAP_ANY;
	
	/**
	 * Camera index
	 */
	private int capIndex = 0;
	
	/**
	 * This variable is used only for CAP_IMAGES or CAP_FFMPEG
	 */
	private String seqName = null;
	
	private native void implSetup(CamBundle cam);
	private native void implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);

	public native boolean setProp(CamBundle cam,int id, double val);
	public native double  getProp(CamBundle cam,int id);

	public int getIndex(){ return capIndex; }
	
	@Override
	public void setup() {
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
	//-----------------------//
	
	private static final int 
		CAP_ANY = 0, // auto-detect
		CAP_VFW = 200, // platform native, V4L, V4L2
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

	private static int name2value(String name){
		int val = CAP_ANY;//default domain~~
		if (
			name.equalsIgnoreCase("IMAGES") == true ||
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
}
