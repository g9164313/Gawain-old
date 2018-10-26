package prj.scada;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import narl.itrc.ColorHue;

public class ImgSpectrum extends Region {
	
	private Canvas img = new Canvas();
	
	private WritableImage buf = new WritableImage(10, 10);
	
	//new WritableImage(SAMPLE,M_HEIGHT);
	
	public ImgSpectrum() {		
		//setStyle("-fx-border-width:4; -fx-border-color:black;");
		img.minHeight(600);
		//hook event, binding property is meaningless 
		widthProperty().addListener(event->{
			img.setWidth(widthProperty().get());
			refresh();
		});
		heightProperty().addListener(event->{
			img.setHeight(heightProperty().get());
			refresh();
		});
		getChildren().add(img);
	}
	
	public ImgSpectrum setAxis( 
	){
		
		return this;
	}
	
	public ImgSpectrum setFreq(float[] vals, float min, float max, int count){
		
		int depth = vals.length;
		
		if(
			(((int)buf.getWidth() )!=count) ||
			(((int)buf.getHeight())!=depth)
		){
			buf = new WritableImage(count, depth);
		}
		//rolling image~~~~
		PixelReader rd = buf.getPixelReader();
 		PixelWriter wt = buf.getPixelWriter();
 		for(int xx=count-2; xx>=0; --xx){			
 			wt.setPixels(
 				xx+1, 0, 
 				1   , depth, 
 				rd, 
 				xx  , 0
 			);
 		}
 		//draw first line in image~~~		
 		for(int yy=0; yy<vals.length; yy++){
 			int idx = (int)((vals[yy]/(max-min))*255.f);
 			wt.setArgb(0, depth-yy-1, ColorHue.mapOct[idx]);
 		}
		return this;
	}
	
	public ImgSpectrum refresh(){
		double ww = img.getWidth();
        double hh = img.getHeight();
        GraphicsContext gc = img.getGraphicsContext2D();
        gc.clearRect(0, 0, ww, hh);
        //gc.setStroke(Color.RED);
        //gc.strokeLine(0, 0, ww, hh);
        //gc.strokeLine(0, hh, ww, 0);
        gc.drawImage(buf, 0, 0, ww, hh);
		return this;
	} 
}
