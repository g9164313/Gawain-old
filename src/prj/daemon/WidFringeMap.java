package prj.daemon;

import java.util.ArrayList;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.StrokeType;
import narl.itrc.Misc;
import narl.itrc.WidImageView;

public class WidFringeMap extends WidImageView {

	public WidFringeMap(){
		super();
		init();
	}

	public WidFringeMap(int width, int height){
		super(width, height);
		init();
	}
	
	private ArrayList<Group> lstFringe = new ArrayList<Group>();
	
	private void addFringe(){
		//one group is one fringe
		//the first node is poly-line, the other is dot~~~		
		Polyline py = new Polyline();
		py.setStroke(Color.FORESTGREEN);
		py.autosize();
		py.setStrokeWidth(1);
		
		Group grp = new Group();
		grp.getChildren().add(py);
		
		getOverlay().add(grp);
	}
	
	private void addFringeDot(double locaX, double locaY){
		int idx = getOverlay().size() - 1;
		if(idx<0){
			return;
		}
		
		Circle dot = new Circle(locaX,locaY,3);
		dot.setStroke(Color.FORESTGREEN);
		dot.setFill(Color.TRANSPARENT);
		dot.setStrokeWidth(1);
		
		ObservableList<Node> lst = ((Group)getOverlay().get(idx)).getChildren();
		lst.add(dot);
		
		if(lst.size()==3){
			//create the first segment~~~ Polyline's bug??
			Polyline py = (Polyline)lst.get(0);
			Circle c1 = (Circle)lst.get(1);
			Circle c2 = (Circle)lst.get(2);
			py.getPoints().addAll(
				c1.getCenterX(),c1.getCenterY(),
				c2.getCenterX(),c2.getCenterY()
			);		
		}else if(lst.size()>3){
			//append the next point~~~
			((Polyline)lst.get(0)).getPoints().addAll(locaX,locaY);
		}
	}
	
	private void subLastFringe(){
		int idx = getOverlay().size() - 1;
		if(idx<0){
			return;
		}
		getOverlay().remove(idx);
	}
	
	private void subLastFringeDot(){
		int idxFng = getOverlay().size() - 1;
		if(idxFng<0){
			return;
		}
		
		Group gp = (Group)getOverlay().get(idxFng);
		
		Polyline py = (Polyline)gp.getChildren().get(0);
		
		int idxDot = gp.getChildren().size() - 1;
		if(idxDot<=1){
			//remove the last fringe~~~~
			getOverlay().remove(idxFng);
			return;
		}
		gp.getChildren().remove(idxDot);
		
		py.getPoints().remove(idxDot*2-2, idxDot*2);	
	}
	
	private void init(){

		setOnMouseClicked(e->{
			
		});
		setOnKeyPressed(event->{
			final KeyCode cc = event.getCode();

			if(cc==KeyCode.Q){

			}else if(cc==KeyCode.A && event.isControlDown()==true){				
				addFringe();
				
			}else if(cc==KeyCode.A){
				addFringeDot(
					getCursorX(),
					getCursorY()
				);
				
			}else if(cc==KeyCode.S && event.isControlDown()==true){
				subLastFringe();
				
			}else if(cc==KeyCode.S){
				subLastFringeDot();
				
			}else if(hotkeyUndo.match(event)==true){
				
			}
		});
	}
	
	private static final KeyCombination hotkeyUndo= new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_ANY);
	

	@Override
	public void eventChangeScale(int scale) {
		
	}
}
