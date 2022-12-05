package prj.sputter.diagram;

import javafx.geometry.HPos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

/**
 * Mass flow control used this symbol.<p>
 * @author qq
 *
 */
public class ValveSrc extends PixTile {

	private final String s_txt = 
		"-fx-background-color: white;"+
		"-fx-border-color: black;"+
		"-fx-border-style: solid;"+
		"-fx-border-radius: 13px;"+
		"-fx-border-width: 3px;";
	
	final StackPane cc = new StackPane();
	
	public ValveSrc() {
		super(PixDir.HORI);
		rh.setMinWidth(PIPE_SIZE*2.);
		//rh.setMinWidth(57.);
		//rh.setMinHeight(PIPE_SIZE);
		
		cc.setMinSize(PIPE_SIZE*2., PIPE_SIZE*2.);
		cc.setStyle(s_txt);
		
		getChildren().removeAll(lf,tp);
		add(cc, 1, 1);
		add(shape_handle(), 1, 0);		
		//setGridLinesVisible(true);
	}

	
	
	
	private static Shape shape_handle() {
		final double h_wide = 11.;//handle wide
		final double h_tall = 15.;//handle tall
		final double s_size = 6.;//stroke width
		Shape s1 = new Line(-h_wide,-h_tall, h_wide,-h_tall);		
		Shape s2 = new Line(0.,0,0.,-h_tall);
		s1.setStrokeWidth(s_size);
		s2.setStrokeWidth(s_size);
		Shape s3 = Shape.union(s1, s2);
		GridPane.setHalignment(s3, HPos.CENTER);
		return s3;
	}
		
	public PanInfo info = null;
	
	public PanInfo createInfo() {
		if(info!=null) {
			return info;
		}
		info = new PanInfo();
		cc.getChildren().add(info);
		return info;
	}
}
