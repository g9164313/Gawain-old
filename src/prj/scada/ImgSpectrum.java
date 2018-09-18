package prj.scada;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import narl.itrc.Misc;

public class ImgSpectrum extends Region {
	
	private Canvas img = new Canvas();
	
	private WritableImage buf = null;
	
	//new WritableImage(SAMPLE,M_HEIGHT);
	
	public ImgSpectrum() {		
		//setStyle("-fx-border-width:4; -fx-border-color:black;");
		
		//hook event, binding property is useless 
		widthProperty().addListener(event->{
			img.setWidth(widthProperty().get());
			redraw();
		});
		heightProperty().addListener(event->{
			img.setHeight(heightProperty().get());
			redraw();
		});
		getChildren().add(img);
	}
	
	//public ImgSpectrum set
	
	private void redraw(){
		 double width = img.getWidth();
         double height = img.getHeight();

         GraphicsContext gc = img.getGraphicsContext2D();
         gc.clearRect(0, 0, width, height);

         gc.setStroke(Color.RED);
         gc.strokeLine(0, 0, width, height);
         gc.strokeLine(0, height, width, 0);		
	}
}
