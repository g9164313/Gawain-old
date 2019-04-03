package prj.refuge;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import narl.itrc.Misc;

public class ImgPreviewX extends ScrollPane {	
	
	protected IntegerProperty propFPS = new SimpleIntegerProperty(0);
	protected StringProperty propSize = new SimpleStringProperty(Misc.TXT_UNKNOW);
	protected StringProperty propLoca = new SimpleStringProperty(Misc.TXT_UNKNOW);
	protected StringProperty propPixv = new SimpleStringProperty(Misc.TXT_UNKNOW);

	private WritableImage buf;
	
	private final ImageView vew = new ImageView();

	public ImgPreviewX(){
		this(640,480);
	}
	
	public ImgPreviewX(int width, int height){
		
		//for(AnchorPane pan:overlay){
			//pan.setStyle("-fx-border-color: mediumorchid; -fx-border-width: 4px;");//for debug
		//	pan.setMaxSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
		//	pan.setMinSize(Control.USE_PREF_SIZE, Control.USE_PREF_SIZE);
		//	pan.setPrefSize(width, height);			
		//}
		
		
		final StackPane lay = new StackPane();  
		lay.getChildren().addAll(
			vew,
			new AnchorPane(),
			new AnchorPane()
		);
		lay.setPrefSize(width,height);
		//lay0.setStyle("-fx-border-color: olive; -fx-border-width: 4px;");//for debug
		
		setContent(lay);
		setStyle("-fx-background: ghostwhite;");
		//setMinViewportWidth(width);
		//setMinViewportHeight(height);
		//setFitToWidth(true);
		//setFitToHeight(true);
		setHbarPolicy(ScrollBarPolicy.ALWAYS);
		setVbarPolicy(ScrollBarPolicy.ALWAYS);		
	}
	//------------------------------------//

	/**
	 * information for image size, pixel location and value.<p>
	 */
	private int geom[] = {
		0/* image width  */, 
		0/* image height */,
		1/* image scale  */,
		0/* cursor-X */, 
		0/* cursor-Y */,		
	};
	
	/**
	 * the current cursor point to the value of pixel.<p>
	 * The data sequence is Red, Green, Blue, Alpha.<p>
	 */
	private final int[] pixv = {0, 0, 0, 0,};
	
	public int getImgWidth(){
		return geom[0]; 
	}
	
	public int getImgHeight(){
		return geom[1]; 
	}
	
	public int getCursorX(){
		return geom[3]; 
	}
	
	public int getCursorY(){
		return geom[4]; 
	}
	
	/*public void snapData(String name){
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
	}*/
	
	
	/**
	 * show image from file. This method must be invoked by GUI-event.<p> 
	 * @param name - file name or full-path
	 */
	public void setImage(String name){
		File fs = new File(name);
		if(fs.exists()==false){
			return;
		}
		Image img = new Image(fs.toURI().toString());		
		vew.setImage(img);
		geom[0] = (int)img.getWidth();
		geom[1] = (int)img.getHeight();
	}
	
	/**
	 * show image from file. This method must be invoked by GUI-event.<p> 
	 * @param file - byte array for image file.(support .png, .jpg, .bmp)
	 */
	public void setImage(byte[] file){
		Image img = file2image(file,vew);
		geom[0] = (int)img.getWidth();
		geom[1] = (int)img.getHeight();
	}

	public static Image file2image(byte[] file, ImageView view){
		Image img = null;
		try {
			img = SwingFXUtils.toFXImage(
				ImageIO.read(new ByteArrayInputStream(file)),
				null
			);
			view.setImage(img);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return img;
	}
	
	public void refresh(
		final byte[] data, 
		final int width, 
		final int height
	){
		if(buf==null){
			create_buff(width,height);
		}
		if( 
			width !=((int)buf.getWidth() ) ||
			height!=((int)buf.getHeight())
		){
			create_buff(width,height);
		}
		//this object can be accessed by non-event thread
		buf.getPixelWriter().setPixels(
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
			geom[0] = width;
			geom[1] = height;
			propSize.set(String.format("%4dx%4d",width,height));
			
			//lay.setPrefSize(width,height);
			//for(AnchorPane pan:overlay){
			//	pan.setPrefSize(width, height);
			//}
			//if(imgView!=null){
			//	lay.getChildren().remove(imgView);
			//}
			buf = new WritableImage(width, height);
			//imgView = new ImageView(imgBuff);
			//imgView.setCache(true);
			//StackPane.setAlignment(zoomView, Pos.TOP_LEFT);
			//lay.getChildren().add(imgView);
			//imgView.toBack();
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
