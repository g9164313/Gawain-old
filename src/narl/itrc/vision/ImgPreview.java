package narl.itrc.vision;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import narl.itrc.Misc;

public class ImgPreview extends ScrollPane {

	public boolean ctrlPlay = false;

	/**
	 * information for image size, pixel location and value.<p>
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
		
	protected IntegerProperty propFPS = new SimpleIntegerProperty(0);
	protected StringProperty propSize = new SimpleStringProperty(Misc.TXT_UNKNOW);
	protected StringProperty propLoca = new SimpleStringProperty(Misc.TXT_UNKNOW);
	protected StringProperty propPixv = new SimpleStringProperty(Misc.TXT_UNKNOW);

	protected WritableImage imgBuff;
	
	protected ImageView imgView;
	
	protected AnchorPane overlay[] = { 
		new AnchorPane(),
		new AnchorPane()
	};
	private StackPane layGroup = new StackPane(); 

	public ImgPreview(){
		this(640,480);
	}
	
	public ImgPreview(int width, int height){
		
		for(AnchorPane pan:overlay){
			//pan.setStyle("-fx-border-color: mediumorchid; -fx-border-width: 4px;");//for debug
			pan.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
			pan.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
			pan.setPrefSize(width, height);			
		}
		
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
	}
	//------------------------------------//
	
	public ObservableList<Node> getOverlayList(){
		return overlay[0].getChildren();
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
	
	public void snapData(String name){
		snapData(name,null);
	}
	public void snapData(String name, final int[] roi){
		if(imgView==null){
			return;
		}		
		try {
			Image img = null;
			if(roi==null){
				img = imgView.getImage();
			}else{
				img = new WritableImage(
					imgView.getImage().getPixelReader(),
					roi[0], roi[1],
					roi[2], roi[3]
				);
			}
			ImageIO.write(
				SwingFXUtils.fromFXImage(img,null), 
				"png", 
				new File(name)
			);
			Misc.logv("snap data~~");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] getBgraData(){
		return getBgraData(null);
	}
	
	public byte[] getBgraData(final int[] roi){
		byte[] buff = null;
		if(imgView==null){
			return buff;
		}
		Image img = imgView.getImage();
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
	
	public void refresh(final InputStream stm){		
		Misc.invoke(event->{
			if(imgView==null){
				imgView = new ImageView();
				layGroup.getChildren().add(imgView);
				imgView.toBack();
			}
			Image img = new Image(stm);
			imgView.setImage(img);
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
}
