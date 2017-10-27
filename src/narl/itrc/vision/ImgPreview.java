package narl.itrc.vision;

import com.sun.glass.ui.Application;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import narl.itrc.Misc;

public class ImgPreview extends StackPane {

	public boolean ctrlPlay = true;
	
	protected IntegerProperty propFPS = new SimpleIntegerProperty(0);
	
	/**
	 * pixel location and value.<p>
	 * Format is [<p> 
	 *   image width, image height, <p> 
	 *   cursor-X,  cursor-Y, <p> 
	 *   Red, Green Blue <p> 
	 * ] <p>
	 */
	private int infoGeom[] = {
		0, 0, 
		0, 0, 
		0, 0, 0,
		0
	};
	
	private StringProperty propLoca = new SimpleStringProperty(Misc.TXT_UNKNOW);
	
	private StringProperty propPixv = new SimpleStringProperty(Misc.TXT_UNKNOW);
	
	private StringProperty propSize = new SimpleStringProperty(Misc.TXT_UNKNOW);
	
	private WritableImage zoomBuff;
	
	private ImageView zoomView;
	
	private AnchorPane overlay[] = { 
		new AnchorPane(),
		new AnchorPane()
	};
	
	private StackPane lay0 = new StackPane();
	
	private ScrollPane lay1 = new ScrollPane();
	
	private AnchorPane lay2 = new AnchorPane();
	
	private GridPane layCtrl = create_ctrl_pane();
	
	public ImgPreview(){
		init(640,480);
	}
	//------------------------------------//
	
	private EventHandler<MouseEvent> eventCursorMove = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			//double hv = lay1.getHvalue();
			//double vv = lay1.getVvalue();
			//Misc.logv("scroll=%.1f,%.1f", hv,vv);
			double offset[] ={0, 0};//top-left distance between image view and scroll panel
			if(zoomView!=null){
				offset[0] = zoomView.getLayoutX();
				offset[1] = zoomView.getLayoutY();
				//Misc.logv("scroll=%.1f,%.1f", x_off, y_off);
			}
			
			int cx = (int)(event.getX()-offset[0]);
			int cy = (int)(event.getY()-offset[1]);

			if(cx<infoGeom[0]){
				infoGeom[2] = cx;
			}else{
				infoGeom[2] = -Integer.MAX_VALUE;//reset
			}
			if(cy<infoGeom[1]){
				infoGeom[3] = cy;
			}else{
				infoGeom[3] = -Integer.MAX_VALUE;//reset
			}
			if(infoGeom[2]<0 || infoGeom[3]<0){
				propLoca.set("(??,??)");
			}else{				
				propLoca.set(String.format("(%4d,%4d)",infoGeom[2],infoGeom[3]));
			}			
		}
	};
	
	private EventHandler<MouseEvent> eventCursorClick = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			MouseButton btn = event.getButton();			
			boolean flag = layCtrl.visibleProperty().get();			
			if(btn==MouseButton.PRIMARY){
				//Misc.logv("left-click");
				if(flag==false){
					return;
				}
			}else if(btn==MouseButton.SECONDARY){
				layCtrl.setVisible(!flag);
			}
		}
	};
	
	public ObservableList<Node> getOverlayList(){
		return overlay[0].getChildren();
	}
	//------------------------------------//
	
	private void init(int width, int height){
		
		layCtrl.setVisible(false);//default~~~
		
		//gg = new Rectangle(30., 30.);
		//gg.setFill(Color.TRANSPARENT);
		//gg.setStroke(Color.GREENYELLOW);
		//gg.setStrokeWidth(2);
		//overlay[0].getChildren().add(gg);
		
		for(AnchorPane pan:overlay){
			//pan.setStyle("-fx-border-color: mediumorchid; -fx-border-width: 4px;");//for debug
			pan.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
			pan.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
			pan.setPrefSize(width, height);
			//StackPane.setAlignment(pan, Pos.TOP_LEFT);
			
		}
		//lay0.setStyle("-fx-border-color: olive; -fx-border-width: 4px;");//for debug
		lay0.getChildren().addAll(overlay);
		lay0.setPrefSize(width,height);
		lay0.setOnMouseMoved(eventCursorMove);
		
		//lay1.setStyle("-fx-border-color: blueviolet; -fx-border-width: 4px;");//for debug
		lay1.setContent(lay0);
		lay1.setStyle("-fx-background: lightskyblue;");
		//lay1.setMouseTransparent(true);
		lay1.setMinViewportWidth(width);
		lay1.setMinViewportHeight(height);
		lay1.setFitToWidth(true);
		lay1.setFitToHeight(true);
		lay1.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		lay1.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		
		//lay2.setStyle("-fx-border-color: red; -fx-border-width: 4px;");
		lay2.getChildren().addAll(layCtrl);
		lay2.setPickOnBounds(false);
		lay2.setOnMouseMoved(eventCursorMove);
		lay2.setOnMouseClicked(eventCursorClick);
		
		//setEventDispatcher(lay2.getEventDispatcher());
		getChildren().addAll(lay1,lay2);

		
		/*widthProperty().addListener((obs, oldVal, newVal)->{
			int val = newVal.intValue();
			lay1.setMinViewportWidth(val);
			lay2.setMinWidth(val);
		});		
		heightProperty().addListener((obs, oldVal, newVal)->{
			int val = newVal.intValue();
			lay1.setMinViewportHeight(val);
			lay2.setMinHeight(val);
		});*/
	}

	private GridPane create_ctrl_pane(){
		
		Button btnPlay = new Button();
		btnPlay.setMaxWidth(Double.MAX_VALUE);		
		btnPlay.setOnAction(e->{
			ctrlPlay = !ctrlPlay;
			set_ctrl_play(btnPlay);
		});
		set_ctrl_play(btnPlay);

		Button btnZoomIn = new Button("Zoom+");
		btnZoomIn.setMaxWidth(Double.MAX_VALUE);
		btnZoomIn.setOnAction(e->{
			Misc.logv("btnZoomIn");
		});

		Button btnZoomOut = new Button("Zoom-");
		btnZoomOut.setMaxWidth(Double.MAX_VALUE);
		btnZoomOut.setOnAction(e->{
			Misc.logv("btnZoomOut");
		});
		
		Button btnSnapBuf = new Button("Snap");
		btnSnapBuf.setMaxWidth(Double.MAX_VALUE);
		btnSnapBuf.setOnAction(e->{
			Misc.logv("btnSnap");
		});
		
		Label txtSize = new Label();
		txtSize.textProperty().bind(propSize);
		
		Label txtLoca = new Label();
		txtLoca.textProperty().bind(propLoca);
		
		Label txtPixV = new Label();
		txtPixV.textProperty().bind(propPixv);
		
		Label txtFps = new Label();
		txtFps.textProperty().bind(propFPS.asString());
		
		GridPane lay = new GridPane();		
		lay.add(btnPlay   , 0, 0, 3, 1);
		lay.add(btnZoomIn , 0, 1, 3, 1);
		lay.add(btnZoomOut, 0, 2, 3, 1);
		lay.add(btnSnapBuf, 0, 4, 3, 1);		
		lay.addRow(5, new Label("尺寸："), txtSize);
		lay.addRow(6, new Label("位置："), txtLoca);
		lay.addRow(7, new Label("像素："), txtPixV);
		lay.addRow(8, new Label("FPS:"  ), txtFps);
		lay.add(new Separator(), 0, 9, 3, 1);
		
		lay.setStyle(
			"-fx-vgap: 7px;"+
			"-fx-background-color: gainsboro;"+
			"-fx-padding: 7px;"+
			"-fx-spacing: 7px;"+
			"-fx-background-radius: 10;"+		
			"-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"
		);
		lay.setMinSize(130, 300);
		lay.getChildren().forEach(obj->{
			GridPane.setHgrow(obj, Priority.ALWAYS);
			GridPane.setFillWidth(obj, true);
		});
		
		AnchorPane.setTopAnchor (lay, 17.);
		AnchorPane.setLeftAnchor(lay, 17.);
		//AnchorPane.setRightAnchor(lay, 17.);
		return lay;
	}
	
	private void set_ctrl_play(final Button btn){
		if(ctrlPlay==true){
			btn.setText("pause");
		}else{
			btn.setText("play");
		}
	}
	//------------------------------------//
	
	protected void refresh(
		final byte[] data, 
		final int width, 
		final int height
	){
		if(width<=0 || height<=0 || data.length==0){
			return;
		}
		if(zoomBuff==null){
			init_buff(width,height);
		}
		if( 
			width !=((int)zoomBuff.getWidth() ) ||
			height!=((int)zoomBuff.getHeight())
		){
			init_buff(width,height);
		}
		zoomBuff.getPixelWriter().setPixels(
			0, 0, 
			width, height, 
			PixelFormat.getByteRgbInstance(), 
			data, 
			0, width*3
		);
		
		if(infoGeom[2]>=0 && infoGeom[3]>=0){
			int val = zoomBuff.getPixelReader().getArgb(infoGeom[2], infoGeom[3]);
			infoGeom[4] = (val&0xFF0000)>>16;
			infoGeom[5] = (val&0x00FF00)>>8;
			infoGeom[6] = (val&0x0000FF);
		}
		Misc.invoke(e->{
			if(infoGeom[2]>=0 && infoGeom[3]>=0){
				propPixv.set(String.format(
					"%3d,%3d,%3d",
					infoGeom[4],infoGeom[5],infoGeom[6]
				));
			}else{
				propPixv.set("?,?,?");
			}			
		});
	}
	
	private void init_buff(
		final int width, 
		final int height
	){
		final Runnable _init_buff = new Runnable(){
			@Override
			public void run() {
				infoGeom[0] = width;
				infoGeom[1] = height;
				propSize.set(String.format("%4dx%4d",width,height));
				
				lay0.setPrefSize(width,height);
				for(AnchorPane pan:overlay){
					pan.setPrefSize(width, height);
				}
				if(zoomView!=null){
					lay0.getChildren().remove(zoomView);
				}
				zoomBuff = new WritableImage(width, height);
				zoomView = new ImageView(zoomBuff);	
				//StackPane.setAlignment(zoomView, Pos.TOP_LEFT);
				lay0.getChildren().add(zoomView);
				zoomView.toBack();
			}
		};
		if(Application.isEventThread()==true){
			_init_buff.run();
		}else{
			Application.invokeAndWait(_init_buff);
		}
	}

	/*	HBox lay = new HBox();
	lay.setStyle(
		"-fx-border-color: red;"+
		"-fx-border-width: 4px;"+
		"-fx-background-color: palegreen; "+
		"-fx-padding: 13;"+
		"-fx-spacing: 7; "+
		"-fx-background-radius: 10; "+		
		"-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"		
	);
	 */
}
