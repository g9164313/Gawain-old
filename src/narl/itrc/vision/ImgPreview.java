package narl.itrc.vision;

import com.sun.glass.ui.Application;

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
import narl.itrc.Misc;

public class ImgPreview extends ScrollPane {

	public boolean ctrlPlay = false;

	/**
	 * information for image size, pixel location and value.<p>
	 */
	private int infoGeom[] = {
		0/* image width */, 
		0/* image height*/,
		1/* image scale */,
		0/* cursor-X */, 
		0/* cursor-Y */, 
		0, 0, 0,/* pixel value - Red, Green, Blue */
		0
	};
		
	protected IntegerProperty propFPS = new SimpleIntegerProperty(0);
	
	private StringProperty propSize = new SimpleStringProperty(Misc.TXT_UNKNOW);
	
	private StringProperty propLoca = new SimpleStringProperty(Misc.TXT_UNKNOW);
	
	private StringProperty propPixv = new SimpleStringProperty(Misc.TXT_UNKNOW);
	
	private WritableImage imgBuff;
	
	private ImageView imgView;
	
	private AnchorPane overlay[] = { 
		new AnchorPane(),
		new AnchorPane()
	};
	
	private StackPane layGroup = new StackPane(); 
	
	private GridPane layCtrl = create_ctrl_pane();
	
	public ImgPreview(){
		init(640,480);
	}
	
	public ImgPreview(int width, int height){
		init(width,height);
	}
	//------------------------------------//
	
	private EventHandler<MouseEvent> eventCursorMove = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			double offset[] ={
				event.getX() + getHvalue() * getMinViewportWidth(),
				event.getY() + getVvalue() * getMinViewportHeight()
			};
			
			//update cursor location
			infoGeom[3] = (int)offset[0];
			infoGeom[4] = (int)offset[1];
			//how to get information after resizing image view???
			/*if( 2<=infoGeom[2] ){			    
				infoGeom[3] /= infoGeom[2];
				infoGeom[4] /= infoGeom[2];
			}else if( infoGeom[2]<=-2 ){
				infoGeom[3] /= infoGeom[2];
				infoGeom[4] /= infoGeom[2];
			}*/
			propLoca.set(String.format(
				"(%4d,%4d)",
				infoGeom[3],infoGeom[4]
			));

			//pick up pixel value
			if(
				(0<=infoGeom[3] && infoGeom[3]<infoGeom[0]) &&
				(0<=infoGeom[4] && infoGeom[4]<infoGeom[1]) &&
				imgBuff!=null
			){
				int val = imgBuff.getPixelReader().getArgb(infoGeom[3], infoGeom[4]);
				infoGeom[5] = (val&0xFF0000)>>16;
				infoGeom[6] = (val&0x00FF00)>>8;
				infoGeom[7] = (val&0x0000FF);
				propPixv.set(String.format(
					"%03d,%03d,%03d",
					infoGeom[5],infoGeom[6],infoGeom[7]
				));
			}else{
				propPixv.set(Misc.TXT_UNKNOW);
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
		overlay[1].getChildren().add(layCtrl);
		
		//lay0.setStyle("-fx-border-color: olive; -fx-border-width: 4px;");//for debug
		layGroup.getChildren().addAll(overlay);
		layGroup.setPrefSize(width,height);

		//lay1.setStyle("-fx-border-color: blueviolet; -fx-border-width: 4px;");//for debug
		setContent(layGroup);
		setStyle("-fx-background: lightskyblue;");
		setMinViewportWidth(width);
		setMinViewportHeight(height);
		setFitToWidth(true);
		setFitToHeight(true);
		setHbarPolicy(ScrollBarPolicy.ALWAYS);
		setVbarPolicy(ScrollBarPolicy.ALWAYS);
		setOnMouseMoved(eventCursorMove);
		setOnMouseClicked(eventCursorClick);
		hvalueProperty().addListener((obs,oldValue,newValue)->{
			AnchorPane.setLeftAnchor(
				layCtrl, 
				32. + newValue.doubleValue() * getMinViewportWidth()
			);
		});
		vvalueProperty().addListener((obs,oldValue,newValue)->{
			AnchorPane.setTopAnchor (
				layCtrl, 
				32. + newValue.doubleValue() * getMinViewportHeight()
			);
		});
		//setEventDispatcher(lay2.getEventDispatcher());
		
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
		
		Label txtSize = new Label();
		txtSize.textProperty().bind(propSize);
		
		final Label txtScale=new Label();
		change_scale(txtScale);
		
		Label txtLoca = new Label();
		txtLoca.textProperty().bind(propLoca);
		
		Label txtPixV = new Label();
		txtPixV.textProperty().bind(propPixv);
		
		Label txtFps = new Label();
		txtFps.textProperty().bind(propFPS.asString());
		
		Button btnPlay = new Button();
		btnPlay.setMaxWidth(Double.MAX_VALUE);		
		btnPlay.setOnAction(e->{
			ctrlPlay = !ctrlPlay;
			set_ctrl_title(btnPlay);
		});
		set_ctrl_title(btnPlay);

		Button btnZoomIn = new Button("Zoom+");
		btnZoomIn.setMaxWidth(Double.MAX_VALUE);
		btnZoomIn.setOnAction(e->{
			infoGeom[2]++;
			if(infoGeom[2]==0){
				infoGeom[2] = 2;//-1, 0, 1 are all same~~~~
			}
			change_scale(txtScale);
			
		});

		Button btnZoomOut = new Button("Zoom-");
		btnZoomOut.setMaxWidth(Double.MAX_VALUE);
		btnZoomOut.setOnAction(e->{
			infoGeom[2]--;
			if(infoGeom[2]==0){
				infoGeom[2] = -2;//-1, 0, 1 are all same~~~~
			}
			change_scale(txtScale);
			
		});
		
		Button btnSnapBuf = new Button("Snap");
		btnSnapBuf.setMaxWidth(Double.MAX_VALUE);
		btnSnapBuf.setOnAction(e->{
			Misc.logv("btnSnap");
		});
		
		GridPane lay = new GridPane();		
		lay.add(btnPlay   , 0, 0, 3, 1);
		lay.add(btnZoomIn , 0, 1, 3, 1);
		lay.add(btnZoomOut, 0, 2, 3, 1);
		lay.add(btnSnapBuf, 0, 4, 3, 1);		
		lay.addRow(5, new Label("尺寸："), txtSize);
		lay.addRow(6, new Label("比例："), txtScale);
		lay.addRow(7, new Label("位置："), txtLoca);
		lay.addRow(8, new Label("像素："), txtPixV);
		lay.addRow(9, new Label("FPS:"  ), txtFps);
		lay.add(new Separator(), 0, 10, 3, 1);
		
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
		
		AnchorPane.setTopAnchor (lay, 32.);
		AnchorPane.setLeftAnchor(lay, 32.);
		return lay;
	}
	
	private void change_scale(Label txt){
		if(imgView==null){
			return;
		}
		if( 2<=infoGeom[2] ){
			txt.setText("+"+infoGeom[2]);
			imgView.setFitWidth (infoGeom[0]*infoGeom[2]);
			imgView.setFitHeight(infoGeom[1]*infoGeom[2]);
		}else if(infoGeom[2]==-1 || infoGeom[2]==0 || infoGeom[2]==1){
			txt.setText("x1");
			imgView.setFitWidth (infoGeom[0]);
			imgView.setFitHeight(infoGeom[1]);
		}else if( infoGeom[2]<=-2 ){
			txt.setText(""+infoGeom[2]);
			imgView.setFitWidth (-infoGeom[0]/infoGeom[2]);
			imgView.setFitHeight(-infoGeom[1]/infoGeom[2]);
		}		
	}
	
	private void set_ctrl_title(final Button btn){
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
		if(imgBuff==null){
			init_buff(width,height);
		}
		if( 
			width !=((int)imgBuff.getWidth() ) ||
			height!=((int)imgBuff.getHeight())
		){
			init_buff(width,height);
		}
		imgBuff.getPixelWriter().setPixels(
			0, 0, 
			width, height, 
			PixelFormat.getByteRgbInstance(), 
			data, 
			0, width*3
		);
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
				
				layGroup.setPrefSize(width,height);
				for(AnchorPane pan:overlay){
					pan.setPrefSize(width, height);
				}
				if(imgView!=null){
					layGroup.getChildren().remove(imgView);
				}
				imgBuff = new WritableImage(width, height);
				imgView = new ImageView(imgBuff);
				imgView.setCache(true);
				//StackPane.setAlignment(zoomView, Pos.TOP_LEFT);
				layGroup.getChildren().add(imgView);
				imgView.toBack();
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
