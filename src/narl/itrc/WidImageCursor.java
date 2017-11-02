package narl.itrc;

import javafx.scene.Group;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;

public class WidImageCursor extends Group {
	
	//gg.setFill(Color.TRANSPARENT);
	//gg.setStroke(Color.CRIMSON);
	//gg.setStrokeWidth(2);
	//lay2.getChildren().add(gg);
		
	private final int RANGE = 15;
	
	private final int SSize = 1;
	
	private final Color DEF_COLOR = Color.CRIMSON;
	
	private Shape[] lst = {
		//Cross-shape
		//new Line(-RANGE/2, RANGE/2, RANGE/2, RANGE/2),
		//new Line(RANGE/2, -RANGE/2, RANGE/2, RANGE/2),
		//Circle-shape
		new Circle(RANGE/2, RANGE/2, RANGE/2),
		//Shape-Rectangle
		new Rectangle(0,0,RANGE,RANGE),
	};
	
	public WidImageCursor(){
		
		for(Shape itm:lst){
			itm.setFill(Color.TRANSPARENT);
			itm.setStroke(DEF_COLOR);
			itm.setStrokeWidth(SSize);
		}
		
		//for shape, cross~~~
		//((Line)lst[0]).setStrokeLineCap(StrokeLineCap.ROUND);
		//((Line)lst[1]).setStrokeLineCap(StrokeLineCap.ROUND);
		
		getChildren().addAll(lst);
	}
	
	public void setAnchor(double locaX, double locaY){
		Misc.logv("mc=(%d,%d)", (int)locaX,(int)locaY);
		AnchorPane.setLeftAnchor(this,locaX);
		AnchorPane.setTopAnchor (this,locaY);
	}	
}
