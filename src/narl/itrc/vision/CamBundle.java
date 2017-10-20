package narl.itrc.vision;

import com.sun.glass.ui.Application;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Orientation;
import javafx.scene.control.ScrollBar;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import narl.itrc.Misc;

public abstract class CamBundle extends StackPane {

	public CamBundle(){
		init();
	}
	
	public CamBundle(int zoomWidth, int zoomHeight){
		zoomSizeW = zoomWidth;
		zoomSizeH = zoomHeight;
		init();
	}
	//--------------------------------//
	
	private final int Min_Zoom_Width = 640;	
	private final int Min_Zoom_Height= 480;
	
	/**
	 * This will be used in native code.<p>
	 */
	@SuppressWarnings("unused") private int zoomLocaX = 0;
	
	/**
	 * This will be used in native code.<p>
	 */
	@SuppressWarnings("unused") private int zoomLocaY = 0;
	
	private int zoomSizeW = Min_Zoom_Width;
	
	private int zoomSizeH = Min_Zoom_Height;
	
	/**
	 * This will be used in native code.<p>
	 */
	@SuppressWarnings("unused") private int zoomScale = 1;
	
	private ImageView zoomView = new ImageView();
	
	private WritableImage zoomImage;
		
	private ScrollBar zoomLoca[] = {
		new ScrollBar(),
		new ScrollBar()
	};
	
	private void init(){
		
		zoomImage = new WritableImage(zoomSizeW,zoomSizeH);
		
		zoomView.setImage(zoomImage);

		final AnchorPane overlay = new AnchorPane();
		overlay.getChildren().add(create_control_item());
		overlay.getChildren().addAll(init_zoom_loca());
				
		//setStyle("-fx-background-color: skyblue;");
		setMinSize(Min_Zoom_Width, Min_Zoom_Height);
		
		//widthProperty().addListener((obs, oldVal, newVal)->{
			//int val = newVal.intValue();
			//zoomInfo[2] = val;
			//screenView.setFitWidth(val);
		//});		
		//heightProperty().addListener((obs, oldVal, newVal)->{
			//int val = newVal.intValue();
			//screenSize[3] = val;
			//screenView.setFitHeight(val);
		//});
		
		setOnMouseMoved(event->{
			
		});
		setOnMouseClicked(event->{
			
			MouseButton btn = event.getButton();
			
			boolean flag = overlay.visibleProperty().get();
			
			if(btn==MouseButton.PRIMARY){
				//Misc.logv("left-click");
				if(flag==false){
					return;
				}
			}else if(btn==MouseButton.SECONDARY){
				//Misc.logv("right-click");
				//show cursor???
				//overlay.setVisible(!flag);
			}
		});
		
		getChildren().addAll(zoomView,overlay);
	}
	
	private Pane create_control_item(){
		GridPane lay = new GridPane();
		lay.setStyle(
			"-fx-background-color: palegreen;"+
			"-fx-padding: 13;"+
			"-fx-spacing: 7;"+
			"-fx-background-radius: 10;"+		
			"-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"
		);
		lay.setMinSize(48, 200);		
		AnchorPane.setTopAnchor(lay, 13.);
		AnchorPane.setLeftAnchor(lay,13.);
		return lay;
	}
	
	private ScrollBar[] init_zoom_loca(){
				
		zoomLoca[0].setOrientation(Orientation.HORIZONTAL);
		zoomLoca[0].setMin(0.f);
		zoomLoca[0].setValue(0.);
		zoomLoca[0].setMax(0.);
		zoomLoca[0].valueProperty().addListener((obv,oldVal,newVal)->{
			zoomLocaX = newVal.intValue();
			//TODO: update screen
		});
				
		zoomLoca[1].setOrientation(Orientation.VERTICAL);
		zoomLoca[1].setMin(0.f);
		zoomLoca[1].setValue(0.);
		zoomLoca[1].setMax(0.);
		zoomLoca[1].valueProperty().addListener((obv,oldVal,newVal)->{
			zoomLocaY = newVal.intValue();
			//TODO: update screen
		});
		
		AnchorPane.setLeftAnchor(zoomLoca[0], 0.);
		AnchorPane.setRightAnchor(zoomLoca[0],16.);
		AnchorPane.setBottomAnchor(zoomLoca[0],0.);
		
		AnchorPane.setTopAnchor(zoomLoca[1], 0.);
		AnchorPane.setBottomAnchor(zoomLoca[1],16.);
		AnchorPane.setRightAnchor(zoomLoca[1],0.);
		
		return zoomLoca;
	}
	
	/*	HBox lay = new HBox();
	lay.setStyle(
		"-fx-background-color: palegreen; "+
		"-fx-padding: 13;"+
		"-fx-spacing: 7; "+
		"-fx-background-radius: 10; "+		
		"-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"		
	);
	 */
	//--------------------------------//
	
	/**
	 * Point to a context for whatever devices.<p>
	 * This pointer also shows whether bundle is ready.<p>
	 * !!! This variable is changed by native code !!! <p>
	 */
	private long ptrCntx = 0;
	
	private int bufSizeW = 0;
	
	private int bufSizeH = 0;

	private int bufCvFmt= 0;
	
	/**
	 * check whether bundle is valid.<p>
	 * It also means device is ready.<p> 
	 * @return TRUE or FALSE
	 */
	public boolean isReady(){
		return (ptrCntx==0)?(false):(true);
	}

	/**
	 * prepare and initialize camera, the instance will be hold in 'ptrCntx'.<p>
	 */
	public abstract void setup();
	
	/**
	 * just fetch image from camera, 'imgBuff' will be re-assign image data.<p>
	 */
	public abstract void fetch();
	
	/**
	 * close camera and release context.<p>
	 */
	public abstract void close();

	protected void setupBuffer(int cvtype, int width,int height){
		//PixelWriter wr = bufImage.getPixelWriter();
		//wr.setPixels(0, 0, 100, 100, com.sun.prism.PixelFormat.BYTE_ALPHA, buffer, scanlineStride);		
	}
	
	protected IntegerProperty propFPS = new SimpleIntegerProperty(0);
	private int countFrame=0, countTick;
	private Runnable updateFPS = new Runnable(){
		@Override
		public void run() {
			int val = (countFrame*1000)/countTick;
			propFPS.set(val);
			Misc.logv("FPS=%d",val);
			countFrame = 0;//reset for next turn~~~~
		}
	};
	
	protected void fetchCallback(byte[] buffImage, int width, int height){
		if(zoomImage==null){
			return;
		}
		zoomImage.getPixelWriter().setPixels(
			0, 0, 
			width, height, 
			PixelFormat.getByteRgbInstance(), 
			buffImage, 
			0, width*3
		);
		
		if(countFrame==0){
			countTick = (int)System.currentTimeMillis();
		}		
		countFrame+=1;
		if(countFrame>=10){
			countTick = (int)System.currentTimeMillis() - countTick;
			if(Application.isEventThread()==true){
				updateFPS.run();
			}else{
				Application.invokeAndWait(updateFPS);
			}
		}
	}
}





