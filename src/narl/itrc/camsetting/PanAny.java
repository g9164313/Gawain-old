package narl.itrc.camsetting;

import com.jfoenix.controls.JFXTextField;

import narl.itrc.CamVidcap;
import narl.itrc.PanBase;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.stage.WindowEvent;

/**
 * This setting panel is based on OpenCV generic capture property.
 * @author qq
 *
 */
public class PanAny extends ScrollPane implements PanBase.EventHook {

	public PanAny(PanBase pan,CamVidcap device){		
		pan.hook = this;
		dev = device;
		initLayout();
	}
	
	private CamVidcap dev;
	private JFXTextField[] boxProp = new JFXTextField[CAP_PROP_TOTAL_SIZE];
	
	private void initLayout(){
		GridPane lay0 = new GridPane();
		lay0.getStyleClass().add("grid-small");
		lay0.setAlignment(Pos.CENTER);
		for(int i=0; i<boxProp.length; i++){
			String t_idx = String.format("%02d) ");
			Label txt = new Label();
			if(i==NO_SUPPORT_VAL1){
				txt.setText(t_idx+NO_SUPPORT);
				lay0.addRow(i,txt);
			}else{
				txt.setText(t_idx+txtProp[i]);
				boxProp[i] = new JFXTextField();
				boxProp[i].setPrefWidth(100.);
				boxProp[i].setUserData(i);				
				boxProp[i].setOnAction(eventEnter);
				lay0.addRow(i,txt,boxProp[i]);
			}
		}
		setPrefWidth(200);
		setPrefHeight(300);
		setContent(lay0);
	}
	
	private final EventHandler<ActionEvent> eventEnter = 
		new EventHandler<ActionEvent>()
	{
		@Override
		public void handle(ActionEvent event) {
			JFXTextField box = (JFXTextField)event.getSource();
			int idx = (int)box.getUserData();
			try{
				dev.setProp(
					dev, idx,
					Double.valueOf(box.getText())
				);
			}catch(NumberFormatException e){				
			}
			set_text(
				idx,
				dev.getProp(dev, idx)
			);
		}	
	};
	
	private void set_text(int idx,double val){
		//How to check decimal??
		boxProp[idx].setText(String.format("%.3f",val));		
	}
	
	@Override
	public void eventShowing(WindowEvent e) {

	}

	@Override
	public void eventShown(WindowEvent e) {
		for(int i=0; i<boxProp.length; i++){
			if(i==NO_SUPPORT_VAL1){
				continue;
			}
			set_text(i,dev.getProp(dev,i));
		}
	}

	@Override
	public void eventWatch(int cnt) {
	}

	@Override
	public void eventClose(WindowEvent e) {
	}
	
	private static final String NO_SUPPORT = "NO_SUPPORT";
	
	private static final String txtProp[]={
		"POS_MSEC",
		"POS_FRAMES",
		"POS_AVI_RATIO", 
		"FRAME_WIDTH",
		"FRAME_HEIGHT", 
		"FPS", 
		"FOURCC",
		"FRAME_COUNT", 
		"FORMAT", 
		"MODE",
		"BRIGHTNESS ",
		"CONTRAST",
		"SATURATION",
		"HUE",
		"GAIN",
		"EXPOSURE",
		"CONVERT_RGB",
		"WHITE_BALANCE_BLUE_U",
		"RECTIFICATION",
		"MONOCHROME",
		"SHARPNESS",
		"AUTO_EXPOSURE",
		"GAMMA",
		"TEMPERATURE", 
		"TRIGGER",
		"TRIGGER_DELAY", 
		"WHITE_BALANCE_RED_V",
		"ZOOM", 
		"FOCUS", 
		"GUID",
		"ISO_SPEED",
		NO_SUPPORT,
		"BACKLIGHT",
		"PAN",
		"TILT", 
		"ROLL",
		"IRIS", 
		"SETTINGS",
		"BUFFERSIZE", 
		"AUTOFOCUS"
	};
	
	@SuppressWarnings("unused")
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
		NO_SUPPORT_VAL1 = 31, 
		CAP_PROP_BACKLIGHT = 32,
		CAP_PROP_PAN = 33, 
		CAP_PROP_TILT = 34, 
		CAP_PROP_ROLL = 35,
		CAP_PROP_IRIS = 36, 
		CAP_PROP_SETTINGS = 37,
		CAP_PROP_BUFFERSIZE = 38, 
		CAP_PROP_AUTOFOCUS = 39,
		CAP_PROP_TOTAL_SIZE = 40;//this is just a tage, not including in OpenCV
}
