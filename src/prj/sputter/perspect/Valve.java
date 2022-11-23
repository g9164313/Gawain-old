package prj.sputter.perspect;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeType;

public class Valve extends PElement {

	
	public Valve(final PDir dir) {
		super(dir);
		
		Circle s1 = new Circle(PIPE_SIZE, Color.TRANSPARENT);
		s1.setStroke(Color.BLACK);
		s1.setStrokeWidth(3.);
		s1.setStrokeType(StrokeType.OUTSIDE);
		
		Shape s2 = shape_cross();
		//s2.setVisible(false);
		
		Shape s3 = shape_check();
		s3.setVisible(false);
		
		add(new StackPane(s1,s2,s3),1,1);
	}
	
	protected GridPane info = null;
	private int info_row = 0;
	
	public Valve addInfo(
		final String name,
		final Label text
	) {
		if(info==null) {
			info = new GridPane();
			info.getStyleClass().add("box-pad");
			
			GridPane.setVgrow(info, Priority.ALWAYS);
			GridPane.setHgrow(info, Priority.ALWAYS);
			
			switch(dir) {
			case HORI:
				getChildren().remove(bm);
				add(info, 0, 2, 3, 1);
				break; 
			case VERT: 
				getChildren().remove(rh);
				add(info, 2, 0, 1, 3);
				break;
			//------------------------
			case LF_TP:
			case TP_LF:
			case LF_BM:
			case BM_LF:
				getChildren().remove(rh);
				add(info, 2, 0, 1, 3);
				break;
			case RH_TP:
			case TP_RH:
			case RH_BM:
			case BM_RH:
				getChildren().remove(lf);
				add(info, 0, 0, 1, 3);
				break;
			//------------------------
			case HORI_TP:
				getChildren().remove(bm);
				add(info, 0, 2, 3, 1);
				break;
			case HORI_BM:
				getChildren().remove(tp);
				add(info, 0, 0, 3, 1);
				break;
				
			case VERT_LF:
				getChildren().remove(rh);
				add(info, 2, 0, 1, 3);
				break;
			case VERT_RH:
				getChildren().remove(lf);
				add(info, 0, 0, 1, 3);
				break;
			//------------------------
			default: break;
			}
		}
		info.addRow(
			info_row++, 
			new Label(name), new Label("："), 
			text
		);
		return this;
	}
	public Valve addInfo(
		final String name,
		final StringProperty text
	) {
		Label _txt =new Label();
		_txt.textProperty().bind(text);
		return addInfo(name,_txt);
	}
	public Valve addInfo(
		final String name,
		final String text
	) {
		return addInfo(name,new Label(text));
	}
	
	
	private Shape shape_cross() {
		final double rad = PIPE_SIZE*0.73;
		Shape s1 = new Line(-rad,-rad, rad, rad);
		Shape s2 = new Line(-rad, rad, rad,-rad);
		s1.setStrokeWidth(1.9);
		s2.setStrokeWidth(1.9);		
		return Shape.union(s1, s2);
	}
	
	private Shape shape_check() {
		Shape s1 = new Line(-PIPE_SIZE*0.6,-PIPE_SIZE*0.7, 0, PIPE_SIZE);
		Shape s2 = new Line( PIPE_SIZE*0.6,-PIPE_SIZE*0.7, 0, PIPE_SIZE);
		s1.setStrokeWidth(1.9);
		s2.setStrokeWidth(1.9);		
		return Shape.union(s1, s2);
	}	
}
