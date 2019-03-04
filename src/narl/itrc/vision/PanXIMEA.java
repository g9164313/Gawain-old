package narl.itrc.vision;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXSlider;

import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import narl.itrc.PanBase;

public class PanXIMEA extends PanBase {

	private CamXIMEA dev = null;
	
	public PanXIMEA(CamXIMEA device){
		dev = device;
	}
	
	private JFXCheckBox chkAE;
	private JFXSlider barExpose;
	//private JFXTextField boxExpose;

	private void refresh_param(){
		
		if(dev.getParamInt(XI_PRM_AEAG)!=0){
			chkAE.setSelected(true);
		}else{
			chkAE.setSelected(false);
		}
		
		int max,min,inc;
		min = dev.getParamInt(XI_PRM_EXPOSURE+XI_PRM_INFO_MIN);//microseconds		
		max = dev.getParamInt(XI_PRM_EXPOSURE+XI_PRM_INFO_MAX);//microseconds
		inc = dev.getParamInt(XI_PRM_EXPOSURE+XI_PRM_INFO_INCREMENT);
		barExpose.setMin(min);
		barExpose.setMax(max);
		barExpose.setMajorTickUnit(inc);
		barExpose.setMinorTickCount(0);
	}
	
	@Override
	public Node eventLayout(PanBase pan) {
		
		chkAE = new JFXCheckBox("Automatic exposure/gain");
		chkAE.setOnAction(event->{
			if(chkAE.isSelected()==true){
				dev.setParamInt(XI_PRM_AEAG,1);
			}else{
				dev.setParamInt(XI_PRM_AEAG,0);
			}
		});
		
		barExpose = new JFXSlider();
		//barExpose.setPrefWidth(137);
				
		GridPane root = new GridPane();
		root.getStyleClass().add("grid-medium");
		root.add(chkAE, 0,0, 5,1);
		root.add(barExpose, 0,1, 5,1);
		
		GridPane.setHgrow(barExpose, Priority.ALWAYS);
		
		stage().setOnShown(e->{
			if(dev.isReady()==false){
				return;
			}		
			refresh_param();//refresh all parameter~~~~~
		});
		return root;
	}

	//append syntax
	public static final String  XI_PRM_INFO_SETTABLE  =":settable"          ;// Is parameter settable(xiSetParamInt(handler, XI_PRM_ABC XI_PRM_INFO_SETTABLE, param_value);)
	public static final String  XI_PRM_INFO_MIN       =":min"               ;// Parameter minimum
	public static final String  XI_PRM_INFO_MAX       =":max"               ;// Parameter maximum
	public static final String  XI_PRM_INFO_INCREMENT =":inc"               ;// Parameter increment
	public static final String  XI_PRM_INFO           =":info"              ;// Parameter value
	public static final String  XI_PRMM_DIRECT_UPDATE =":direct_update"     ;// Parameter modifier for direct update without stopping the streaming. E.g. XI_PRM_EXPOSURE XI_PRMM_DIRECT_UPDATE can be used with this modifier

	//basic
	public static final String XI_PRM_EXPOSURE                        ="exposure"                ;// Exposure time in microseconds 
	public static final String XI_PRM_EXPOSURE_BURST_COUNT            ="exposure_burst_count"    ;// Sets the number of times of exposure in one frame. 
	public static final String XI_PRM_GAIN_SELECTOR                   ="gain_selector"           ;// Gain selector for parameter Gain allows to select different type of gains. XI_GAIN_SELECTOR_TYPE
	public static final String XI_PRM_GAIN                            ="gain"                    ;// Gain in dB 
	public static final String XI_PRM_DOWNSAMPLING                    ="downsampling"            ;// Change image resolution by binning or skipping. XI_DOWNSAMPLING_VALUE
	public static final String XI_PRM_DOWNSAMPLING_TYPE               ="downsampling_type"       ;// Change image downsampling type. XI_DOWNSAMPLING_TYPE
	public static final String XI_PRM_BINNING_SELECTOR                ="binning_selector"        ;// Binning engine selector. XI_BIN_SELECTOR
	public static final String XI_PRM_BINNING_VERTICAL                ="binning_vertical"        ;// Vertical Binning - number of vertical photo-sensitive cells to combine together. 
	public static final String XI_PRM_BINNING_HORIZONTAL              ="binning_horizontal"      ;// Horizontal Binning - number of horizontal photo-sensitive cells to combine together. 
	public static final String XI_PRM_BINNING_PATTERN                 ="binning_pattern"         ;// Binning pattern type. XI_BIN_PATTERN
	public static final String XI_PRM_DECIMATION_SELECTOR             ="decimation_selector"     ;// Decimation engine selector. XI_DEC_SELECTOR
	public static final String XI_PRM_DECIMATION_VERTICAL             ="decimation_vertical"     ;// Vertical Decimation - vertical sub-sampling of the image - reduces the vertical resolution of the image by the specified vertical decimation factor. 
	public static final String XI_PRM_DECIMATION_HORIZONTAL           ="decimation_horizontal"   ;// Horizontal Decimation - horizontal sub-sampling of the image - reduces the horizontal resolution of the image by the specified vertical decimation factor. 
	public static final String XI_PRM_DECIMATION_PATTERN              ="decimation_pattern"      ;// Decimation pattern type. XI_DEC_PATTERN
	public static final String XI_PRM_TEST_PATTERN_GENERATOR_SELECTOR ="test_pattern_generator_selector";// Selects which test pattern generator is controlled by the TestPattern feature. XI_TEST_PATTERN_GENERATOR
	public static final String XI_PRM_TEST_PATTERN                    ="test_pattern"            ;// Selects which test pattern type is generated by the selected generator. XI_TEST_PATTERN
	public static final String XI_PRM_IMAGE_DATA_FORMAT               ="imgdataformat"           ;// Output data format. XI_IMG_FORMAT
	public static final String XI_PRM_SHUTTER_TYPE                    ="shutter_type"            ;// Change sensor shutter type(CMOS sensor). XI_SHUTTER_TYPE
	public static final String XI_PRM_SENSOR_TAPS                     ="sensor_taps"             ;// Number of taps XI_SENSOR_TAP_CNT
	public static final String XI_PRM_AEAG                            ="aeag"                    ;// Automatic exposure/gain 
	public static final String XI_PRM_AEAG_ROI_OFFSET_X               ="aeag_roi_offset_x"       ;// Automatic exposure/gain ROI offset X 
	public static final String XI_PRM_AEAG_ROI_OFFSET_Y               ="aeag_roi_offset_y"       ;// Automatic exposure/gain ROI offset Y 
	public static final String XI_PRM_AEAG_ROI_WIDTH                  ="aeag_roi_width"          ;// Automatic exposure/gain ROI Width 
	public static final String XI_PRM_AEAG_ROI_HEIGHT                 ="aeag_roi_height"         ;// Automatic exposure/gain ROI Height 
	public static final String XI_PRM_BPC                             ="bpc"                     ;// Correction of bad pixels 
	public static final String XI_PRM_AUTO_WB                         ="auto_wb"                 ;// Automatic white balance 
	public static final String XI_PRM_MANUAL_WB                       ="manual_wb"               ;// Calculates White Balance(xiGetImage function must be called) 
	public static final String XI_PRM_WB_KR                           ="wb_kr"                   ;// White balance red coefficient 
	public static final String XI_PRM_WB_KG                           ="wb_kg"                   ;// White balance green coefficient 
	public static final String XI_PRM_WB_KB                           ="wb_kb"                   ;// White balance blue coefficient 
	public static final String XI_PRM_WIDTH                           ="width"                   ;// Width of the Image provided by the device (in pixels). 
	public static final String XI_PRM_HEIGHT                          ="height"                  ;// Height of the Image provided by the device (in pixels). 
	public static final String XI_PRM_OFFSET_X                        ="offsetX"                 ;// Horizontal offset from the origin to the area of interest (in pixels). 
	public static final String XI_PRM_OFFSET_Y                        ="offsetY"                 ;// Vertical offset from the origin to the area of interest (in pixels). 
	public static final String XI_PRM_REGION_SELECTOR                 ="region_selector"         ;// Selects Region in Multiple ROI which parameters are set by width, height, ... ,region mode 
	public static final String XI_PRM_REGION_MODE                     ="region_mode"             ;// Activates/deactivates Region selected by Region Selector 
	public static final String XI_PRM_HORIZONTAL_FLIP                 ="horizontal_flip"         ;// Horizontal flip enable 
	public static final String XI_PRM_VERTICAL_FLIP                   ="vertical_flip"           ;// Vertical flip enable 
	public static final String XI_PRM_FFC                             ="ffc"                     ;// Image flat field correction 
	public static final String XI_PRM_FFC_FLAT_FIELD_FILE_NAME        ="ffc_flat_field_file_name";// Set name of file to be applied for FFC processor. 
	public static final String XI_PRM_FFC_DARK_FIELD_FILE_NAME        ="ffc_dark_field_file_name";// Set name of file to be applied for FFC processor. 
}
