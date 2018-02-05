package narl.itrc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;


public class WidImageView extends StackPane {

	public WidImageView(){
		this(640,480);
	}
	
	public WidImageView(int width, int height){
		
		infoGeom[0] = width;
		infoGeom[1] = height;
		
		view.setSmooth(false);
		ova2.setSmooth(false);
		
		//ova1.setStyle("-fx-border-color: chocolate; -fx-border-width: 4px;");//DEBUG!!!
		ova1.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
		ova1.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
		ova1.prefWidthProperty().bind (view.fitWidthProperty());
		ova1.prefHeightProperty().bind(view.fitHeightProperty());
		
		final StackPane grp = new StackPane();
		grp.getChildren().addAll(view, ova2, ova1);
		
		root.setContent(grp);
		root.setStyle("-fx-background: lightskyblue;");
		root.setMinViewportWidth (infoGeom[0]);
		root.setMinViewportHeight(infoGeom[1]);
		root.setFitToWidth(true);
		root.setFitToHeight(true);
		root.setHbarPolicy(ScrollBarPolicy.ALWAYS);
		root.setVbarPolicy(ScrollBarPolicy.ALWAYS);
		
		getChildren().addAll(root);
		
		/*setOnKeyPressed(e->{
			if(hotkeyScaleDown.match(e)==true){				
				change_scale('-');
			}else if(
				hotkeyScaleUp1.match(e)==true ||
				hotkeyScaleUp2.match(e)==true
			){				
				change_scale('+');
			}else if(hotkeyLoadImage.match(e)==true){
				
			}else if(hotkeySaveImage.match(e)==true){
			
			}else if(hotkeySnapImage.match(e)==true){
				//Misc.logv("vvalue=%.1f , %d, %d",
				//	(getVvalue()),
				//	(int)(lay2.getHeight()),
				//	(int)(getHeight())
				//);
			}else{
				return;
			}		
			e.consume();
		});*/
		
		//hvalueProperty().addListener((obs,oldValue,newValue)->{});
		//vvalueProperty().addListener((obs,oldValue,newValue)->{});	
	}
	
	/**
	 * information for image size, pixel location and value.<p>
	 * Attention!! the coordinate systems of cursor is same as overlay panel!!!
	 */
	protected int infoGeom[] = {
		0/* image width */, 
		0/* image height*/,
		1/* image scale */,
		0/* cursor-X */, 
		0/* cursor-Y */, 
		0, 0, 0,/* pixel value - Red, Green, Blue */
		0
	};

	protected ImageView  view = new ImageView();
	protected ImageView  ova2 = new ImageView();
	protected AnchorPane ova1 = new AnchorPane();
	protected ScrollPane root = new ScrollPane();
	
	protected StringProperty propSize = new SimpleStringProperty(Misc.TXT_UNKNOW);	
	
	protected WritableImage getOverlayView(){
		return (WritableImage)ova2.getImage();
	}
	
	protected ObservableList<Node> getOverlayList(){
		return ova1.getChildren();
	}
	
	public int getDataWidth(){
		return infoGeom[0]; 
	}
	
	public int getDataHeight(){
		return infoGeom[1]; 
	}
	
	public int getCursorX(){
		return infoGeom[3]; 
	}
	
	public int getCursorY(){
		return infoGeom[4]; 
	}
		
	public WidImageView setClickEvent(EventHandler<ActionEvent> hook){		
		view.setOnMouseClicked(event->{
			infoGeom[3] = (int)event.getX();
			infoGeom[4] = (int)event.getY();
			hook.handle(null);
		});//is this right??
		return this;
	}
		
	public Node addCross(final int[] roi){
		return addCross(
			roi[0]+roi[2]/2, 
			roi[1]+roi[3]/2,
			Math.min(roi[2], roi[3])
		);
	}
	public Node addCross(int x, int y, int size){
		x = x - infoGeom[0]/2;//why we need this offset ??
		y = y - infoGeom[1]/2;//why we need this offset ??
		final Line[] part = {
			new Line(x-size/2, y       , x+size/2, y       ), 
			new Line(x       , y-size/2, x       , y+size/2)
		};
		part[0].setStroke(Color.CRIMSON);
		part[0].setStrokeWidth(3);
		part[1].setStroke(Color.CRIMSON);
		part[1].setStrokeWidth(3);
		final Group itm = new Group();
		itm.getChildren().addAll(part);
		Misc.invoke(event->{
			ova1.getChildren().add(itm);
		});
		return itm;
	}
	
	public Node addCircle(final int[] roi){
		return addCircle(
			roi[0]+roi[2]/2, 
			roi[1]+roi[3]/2,
			Math.min(roi[2], roi[3])
		);
	}
	public Node addCircle(int xx, int yy, int rad){
		xx = xx - infoGeom[0]/2;//why we need this offset ??
		yy = yy - infoGeom[1]/2;//why we need this offset ??
		final Circle itm = new Circle(xx,yy,rad);
		itm.setFill(Color.TRANSPARENT);
		itm.setStroke(Color.CRIMSON);
		itm.setStrokeWidth(3);
		Misc.invoke(event->{
			//AnchorPane.setLeftAnchor(itm,(double)roi[0]);
			//AnchorPane.setTopAnchor (itm,(double)roi[1]);
			ova1.getChildren().add(itm);			
		});
		return itm;
	}
	
	public Node addMark(final int[] roi){
		int xx = roi[0] - infoGeom[0]/2;//why we need this offset ??
		int yy = roi[1] - infoGeom[1]/2;//why we need this offset ??
		final Rectangle itm = new Rectangle(
			xx,yy,
			roi[2],roi[3]
		);
		itm.setFill(Color.TRANSPARENT);
		itm.setStroke(Color.CRIMSON);
		itm.setStrokeWidth(3);
		Misc.invoke(event->{
			//AnchorPane.setLeftAnchor(itm,(double)roi[0]);
			//AnchorPane.setTopAnchor (itm,(double)roi[1]);
			ova1.getChildren().add(itm);			
		});		
		return itm;
	}
	
	public void delMark(final Node itm){
		Misc.invoke(event->{
			ova1.getChildren().remove(itm);
		});
	}
	public void clearOverlay(){
		Misc.invoke(event->{
			ova1.getChildren().clear();
		});
	}
	//-----------------------------------//

	public void loadImageFile(String name){
		loadImage(new Image("file:"+name));
	}
	
	public void loadImageFile(File fs){
		if(fs.isFile()==true){
			return;
		}
		FileInputStream stm;
		try {
			stm = new FileInputStream(fs);
			loadImage(new Image(stm));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void loadImage(Image img){
		
		infoGeom[0] = (int)img.getWidth();
		infoGeom[1] = (int)img.getHeight();
		
		view.setImage(img);		
		view.setFitWidth (infoGeom[0]);
		view.setFitHeight(infoGeom[1]);		
		
		ova2.setImage(new WritableImage(infoGeom[0], infoGeom[1]));
		ova2.setFitWidth (infoGeom[0]);
		ova2.setFitHeight(infoGeom[1]);	

		setFocused(true);
		
		propSize.setValue(String.format(
			"%dx%d",
			infoGeom[0],infoGeom[1]
		));
	}
	//-----------------------------------//
	
	public void snapData(){
		snapData(Misc.pathSock+"snap.png",null);
	}
	public void snapData(String name){
		snapData(name,null);
	}
	public void snapData(String name, final int[] roi){		
		try {
			Image img = null;
			if(roi==null){
				img = view.getImage();
			}else{
				img = new WritableImage(
					view.getImage().getPixelReader(),
					roi[0], roi[1],
					roi[2], roi[3]
				);
			}
			ImageIO.write(
				SwingFXUtils.fromFXImage(img,null), 
				"png", 
				new File(name)
			);
			//how to show notify??
			//Misc.logv("snap data~~");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] getBgraData(){
		return getBgraData(null);
	}
	
	public byte[] getBgraData(final int[] roi){
		byte[] buff = null;
		Image img = view.getImage();
		int xx,yy,ww,hh;		
		if(roi==null){
			xx = yy = 0;
			ww = (int)img.getWidth();
			hh = (int)img.getHeight();
		}else{
			xx = roi[0];
			yy = roi[1];
			ww = roi[2];
			hh = roi[3];
		}		
		buff = new byte[ww*hh*4];
		img.getPixelReader().getPixels(
			xx, yy, 
			ww, hh,
			WritablePixelFormat.getByteBgraInstance(),
			buff, 
			0, (int)img.getWidth()*4
		);
		return buff;
	}
	
	protected WritableImage imgBuff;
	
	public void refresh(final InputStream stm){		
		Misc.invoke(event->{
			Image img = new Image(stm);
			view.setImage(img);
			infoGeom[0] = (int)img.getWidth();
			infoGeom[1] = (int)img.getHeight();
		});		
	}
	
	public void refresh(
		final byte[] data, 
		final int width, 
		final int height
	){
		if(width<=0 || height<=0 || data.length==0){
			return;
		}		
		if(imgBuff==null){
			create_buff(width,height);
		}
		if( 
			width !=((int)imgBuff.getWidth() ) ||
			height!=((int)imgBuff.getHeight())
		){
			create_buff(width,height);
		}
		//this object can be accessed by non-event thread
		imgBuff.getPixelWriter().setPixels(
			0, 0, 
			width, height, 
			PixelFormat.getByteRgbInstance(), 
			data, 
			0, width*3
		);
	}
	
	private void create_buff(
		final int width, 
		final int height
	){
		Misc.invoke(event->{
			infoGeom[0] = width;
			infoGeom[1] = height;
			propSize.set(String.format("%4dx%4d",width,height));
			
			setPrefSize(width,height);
			ova1.setPrefSize(width, height);
			if(view!=null){
				getChildren().remove(view);
			}
			imgBuff = new WritableImage(width, height);
			view = new ImageView(imgBuff);
			view.setCache(true);
			//StackPane.setAlignment(zoomView, Pos.TOP_LEFT);
			getChildren().add(view);
			view.toBack();
		});
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
	); */
	
	/*private static final KeyCombination hotkeyScaleDown= new KeyCodeCombination(KeyCode.MINUS , KeyCombination.CONTROL_ANY);
	private static final KeyCombination hotkeyScaleUp1 = new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_ANY);
	private static final KeyCombination hotkeyScaleUp2 = new KeyCodeCombination(KeyCode.ADD   , KeyCombination.CONTROL_ANY);
	private static final KeyCombination hotkeyLoadImage= new KeyCodeCombination(KeyCode.L   , KeyCombination.CONTROL_ANY);
	private static final KeyCombination hotkeySaveImage= new KeyCodeCombination(KeyCode.S   , KeyCombination.CONTROL_ANY);
	private static final KeyCombination hotkeySnapImage= new KeyCodeCombination(KeyCode.D   , KeyCombination.CONTROL_ANY);*/
}
