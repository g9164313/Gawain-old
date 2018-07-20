package prj.scada;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public class PID_Node extends Canvas {

	private int type;
	
	public PID_Node(int category){
		type = category;		
		//debug~~~
		setWidth(32);
		setHeight(32);
		GraphicsContext gc = getGraphicsContext2D();
		gc.setStroke(Color.AQUAMARINE);
		gc.setLineWidth(7);
		
		gc.setFill(Color.AQUAMARINE);
		gc.fillRect(0, 0, 32, 32);
	}
}
