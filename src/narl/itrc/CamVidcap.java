package narl.itrc;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class CamVidcap extends CamBundle {

	public CamVidcap() {
	}

	public CamVidcap(String config) {
		super(config);
	}

	private static final int CAP_ANY = 0, // autodetect
			CAP_VFW = 200, // platform native,V4L, V4L2
			CAP_FIREWARE = 300, // IEEE 1394 drivers
			CAP_QT = 500, // QuickTime
			CAP_UNICAP = 600, // Unicap drivers
			CAP_DSHOW = 700, // DirectShow (via videoInput)
			CAP_PVAPI = 800, // PvAPI, Prosilica GigE SDK
			CAP_OPENNI = 900, // OpenNI (for Kinect)
			CAP_OPENNI_ASUS = 910, // OpenNI (for Asus Xtion)
			CAP_XIAPI = 1100, // XIMEA Camera API
			CAP_AVFOUNDATION = 1200, // AVFoundation framework for iOS (OS X
										// Lion will have the same API)
			CAP_GIGANETIX = 1300, // Smartek Giganetix GigEVisionSDK
			CAP_MSMF = 1400, // Microsoft Media Foundation (via videoInput)
			CAP_WINRT = 1410, // Microsoft Windows Runtime using Media
								// Foundation
			CAP_INTELPERC = 1500, // Intel Perceptual Computing SDK
			CAP_OPENNI2 = 1600, // OpenNI2 (for Kinect)
			CAP_OPENNI2_ASUS = 1610, // OpenNI2 (for Asus Xtion and Occipital
										// Structure sensors)
			CAP_GPHOTO2 = 1700, // gPhoto2 connection
			CAP_GSTREAMER = 1800, // GStreamer
			CAP_FFMPEG = 1900, // FFMPEG
			CAP_IMAGES = 2000; // OpenCV Image Sequence (e.g. img_%02d.jpg)

	private int capDomain = CAP_ANY;
	private int capIndex = 0;

	private native void implSetup(CamBundle cam);

	private native void implFetch(CamBundle cam);

	private native void implClose(CamBundle cam);

	private native boolean setProp(CamBundle cam,int id, double val);

	private native double getProp(CamBundle cam,int id);

	@Override
	public void setup(String txtConfig) {
		// e.g: "winrt:0", open the first camera via WinRT
		String[] args = txtConfig.split(":");
		try {
			capDomain = CAP_ANY;
			if (args.length == 1) {
				capIndex = Integer.valueOf(args[0].trim());
			} else if (args.length == 2) {
				args[0] = args[0].trim();
				args[1] = args[1].trim();
				if (args[0].equalsIgnoreCase("VFW") == true
						|| args[0].equalsIgnoreCase("V4L") == true
						|| args[0].equalsIgnoreCase("V4L2") == true) {
					capDomain = CAP_VFW;
				} else if (args[0].equalsIgnoreCase("FIREWARE") == true
						|| args[0].equalsIgnoreCase("1394") == true) {
					capDomain = CAP_FIREWARE;
				} else if (args[0].equalsIgnoreCase("QT") == true) {
					capDomain = CAP_QT;
				} else if (args[0].equalsIgnoreCase("UNICAP") == true) {
					capDomain = CAP_UNICAP;
				} else if (args[0].equalsIgnoreCase("PVAPI") == true) {
					capDomain = CAP_PVAPI;
				} else if (args[0].equalsIgnoreCase("XIMEA") == true
						|| args[0].equalsIgnoreCase("XAPI") == true) {
					capDomain = CAP_XIAPI;
				}
				capIndex = Integer.valueOf(args[1]);
			} else {
				capIndex = 0;
			}
		} catch (NumberFormatException e) {
			Misc.loge("Wrong configure - " + txtConfig);
			return;
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

	private static final int 
		CAP_PROP_POS_MSEC = 0, 
		CAP_PROP_POS_FRAMES = 1,
		CAP_PROP_POS_AVI_RATIO = 2, 
		CAP_PROP_FRAME_WIDTH = 3,
		CAP_PROP_FRAME_HEIGHT = 4, 
		CAP_PROP_FPS = 5, 
		CAP_PROP_FOURCC = 6,
		CAP_PROP_FRAME_COUNT = 7, 
		CAP_PROP_FORMAT = 8, 
		CAP_PROP_MODE = 9,
		CAP_PROP_BRIGHTNESS = 10, 
		CAP_PROP_CONTRAST = 11,
		CAP_PROP_SATURATION = 12, 
		CAP_PROP_HUE = 13, 
		CAP_PROP_GAIN = 14,
		CAP_PROP_EXPOSURE = 15, 
		CAP_PROP_CONVERT_RGB = 16,
		CAP_PROP_WHITE_BALANCE_BLUE_U = 17, 
		CAP_PROP_RECTIFICATION = 18,
		CAP_PROP_MONOCHROME = 19, 
		CAP_PROP_SHARPNESS = 20,
		CAP_PROP_AUTO_EXPOSURE = 21, 
		CAP_PROP_GAMMA = 22,
		CAP_PROP_TEMPERATURE = 23, 
		CAP_PROP_TRIGGER = 24,
		CAP_PROP_TRIGGER_DELAY = 25, 
		CAP_PROP_WHITE_BALANCE_RED_V = 26,
		CAP_PROP_ZOOM = 27, 
		CAP_PROP_FOCUS = 28, 
		CAP_PROP_GUID = 29,
		CAP_PROP_ISO_SPEED = 30, 
		CAP_PROP_BACKLIGHT = 32,
		CAP_PROP_PAN = 33, 
		CAP_PROP_TILT = 34, 
		CAP_PROP_ROLL = 35,
		CAP_PROP_IRIS = 36, 
		CAP_PROP_SETTINGS = 37,
		CAP_PROP_BUFFERSIZE = 38, 
		CAP_PROP_AUTOFOCUS = 39;

	private Parent getPanel0() {
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
			/*box[i].setEvent(event->{
				setProp(
					CamVidcap.this,
					opt[0],
					box[0].propValue.get()
				);
			});*/
		}

		root.addRow(0,new Label(txt[0]), box[0]);
		root.addRow(1,new Label(txt[1]), box[1]);
		root.addRow(2,new Label(txt[2]), box[2]);
		return root;
	}

	@Override
	public Parent genPanelSetting() {
		switch (capDomain) {
		}
		return getPanel0();
	}
}
