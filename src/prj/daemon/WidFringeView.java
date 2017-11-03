package prj.daemon;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import narl.itrc.Misc;
import narl.itrc.WidImageView;

public class WidFringeView extends WidImageView {

	public WidFringeView(){
		super();
		init();
	}

	public WidFringeView(int width, int height){
		super(width, height);
		init();
	}

	protected class CircMask extends Group {
		public CircMask(){
			msk.setStroke(Color.GHOSTWHITE);
			msk.setFill(Color.TRANSPARENT);
			msk.setStrokeWidth(2);
			msk.setVisible(false);
			getChildren().add(msk);			
		}
		public Circle msk = new Circle();
		
		public void addMaskEdge(double locaX, double locaY){
			//add a edge around circle-mask~~~
			final double SIZE=5;
			Rectangle edg = new Rectangle(locaX-SIZE/2.,locaY-SIZE/2.,SIZE,SIZE);
			edg.setStroke(Color.GHOSTWHITE);
			edg.setFill(Color.TRANSPARENT);
			edg.setStrokeWidth(1.3);
			edg.setVisible(true);
			getChildren().add(edg);
		}
		public void delMaskEdge(){
			//always remove the last one~~~
			int idxEdg = getChildren().size() - 1;
			if(idxEdg<=0){				
				return;
			}else if(idxEdg==1){
				msk.setRadius(0.);//reset circle-mask
			}
			getChildren().remove(idxEdg);
		}
		public void showMask(){
			boolean flg = msk.isVisible();
			Rectangle d1,d2;
			double xx=0., yy=0., rad=0.;
			if(flg==false){
				//we will show mask, so calculate circle-mask again!
				switch(getChildren().size()-1){
				case 0:
					Misc.logw("無足夠的資訊");
					return;
				case 1:
					d1 = (Rectangle)getChildren().get(1);
					xx = d1.getX();
					yy = d1.getY();
					rad = 100.;
					break;
				case 2:
					d1 = (Rectangle)getChildren().get(1);
					d2 = (Rectangle)getChildren().get(2);
					xx = d1.getX() - d2.getX();
					yy = d1.getY() - d2.getY();
					rad = Math.sqrt((xx*xx)+(yy*yy));
					xx = ((d1.getX()+d2.getX())/ 2.);
					yy = ((d1.getY()+d2.getY())/ 2.);
					break;
				default:
				case 3:
					int cols = getChildren().size() - 1;
					RealMatrix AA = new Array2DRowRealMatrix(cols,3);
					RealVector BB = new ArrayRealVector(cols);
					for(int i=1; i<getChildren().size(); i++){
						d1 = (Rectangle)getChildren().get(i);
						xx = d1.getX();
						yy = d1.getY();
						AA.setEntry(i-1, 0, xx);
						AA.setEntry(i-1, 1, yy);
						AA.setEntry(i-1, 2, 1.);
						BB.setEntry(i-1, -(xx*xx+yy*yy));
					}
					DecompositionSolver solver = new SingularValueDecomposition(AA).getSolver();
					RealVector CC = solver.solve(BB);
					double a1 = CC.getEntry(0);
					double a2 = CC.getEntry(1);
					double a3 = CC.getEntry(2);
					xx = -a1/2.;
					yy = -a2/2.;
					rad= Math.sqrt((a1*a1+a2*a2)/4 - a3);
					break;
				}
				msk.setCenterX(xx);
				msk.setCenterY(yy);
				msk.setRadius(rad);
			}
			getChildren().get(0).setVisible(!flg);
			for(int i=1; i<getChildren().size(); i++){
				getChildren().get(i).setVisible(flg);
			}
		}
		public double[] getInfo(){
			final double[] inf = {0., 0., 0.,};
			inf[0] = msk.getCenterX();
			inf[1] = msk.getCenterY();
			inf[2] = msk.getRadius();
			return inf;
		}
	};
	protected CircMask cmask = new CircMask();
	
	private void addFringe(){
		//one group is one fringe
		//Only first node is polynomial-line, the others are dot~~~		
		Polyline py = new Polyline();
		py.setStroke(Color.FORESTGREEN);
		py.autosize();
		py.setStrokeWidth(1);
		
		Group grp = new Group();
		grp.getChildren().add(py);
		
		getOverlayList().add(grp);
	}
	
	private void addFringeDot(double locaX, double locaY){
		//the first shape in overlay is 'circle mask'
		int idxFng = getOverlayList().size() - 1;
		if(idxFng<=0){
			return;
		}
		
		Circle dot = new Circle(locaX,locaY,3);
		dot.setStroke(Color.FORESTGREEN);
		dot.setFill(Color.TRANSPARENT);
		dot.setStrokeWidth(1);
		
		ObservableList<Node> lst = ((Group)getOverlayList().get(idxFng)).getChildren();
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
	
	/*private void subLastFringe(){
		int idxFng = getOverlay().size() - 1;
		if(idxFng<0){
			return;
		}
		getOverlay().remove(idxFng);
	}*/
	
	private void subLastFringeDot(){
		//the first shape in overlay is 'circle mask'
		int idxFng = getOverlayList().size() - 1;
		if(idxFng<=0){
			return;
		}
		
		Group gp = (Group)getOverlayList().get(idxFng);
		
		Polyline py = (Polyline)gp.getChildren().get(0);
		
		int idxDot = gp.getChildren().size() - 1;
		if(idxDot<=1){
			//remove the last fringe~~~~
			getOverlayList().remove(idxFng);
			return;
		}
		gp.getChildren().remove(idxDot);
		
		py.getPoints().remove(idxDot*2-2, idxDot*2);	
	}
	//----------------------------------//
		
	private void init(){

		getOverlayList().add(cmask);
		
		setOnMouseClicked(e->{
			
		});
		setOnKeyPressed(event->{
			final KeyCode cc = event.getCode();

			if(cc==KeyCode.Q && event.isControlDown()==false){
				//mark a new circle-mask edge
				cmask.addMaskEdge(
					getCursorX(), 
					getCursorY()
				);
				
			}else if(cc==KeyCode.W && event.isControlDown()==false){
				//remove one circle-mask edge
				cmask.delMaskEdge();
				
			}else if(cc==KeyCode.Q && event.isControlDown()==true){
				//draw circle-mark
				cmask.showMask();
				
			}else if(cc==KeyCode.A && event.isControlDown()==true){
				//create a new fringe
				addFringe();
				
			}else if(cc==KeyCode.A && event.isControlDown()==false){
				//add dot to the last fringe
				addFringeDot(
					getCursorX(),
					getCursorY()
				);
	
			}else if(cc==KeyCode.S && event.isControlDown()==false){
				//delete a dot from last fringe,
				//if all dots are gone, the fringe is disappear automatically
				subLastFringeDot();
				
			}
		});
	}
	
	//private static final KeyCombination hotkeyUndo= new KeyCodeCombination(KeyCode.Z, KeyCombination.);

	@Override
	public void eventChangeScale(int scale) {
		
	}
}
