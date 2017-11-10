package prj.daemon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
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

	protected class MaskData {
		int locaX, locaY;//the top-lest is (0,0)
		double normX, normY;
		int color;//alpha, red, green blue
		double model; 
	};
	protected class MaskCirc extends Group {
		public MaskCirc(){
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
				update_data();
			}
			toggle_edge(flg);
		}
		public double[] getInfo(){
			final double[] inf = {0., 0., 0.,};
			inf[0] = msk.getCenterX();
			inf[1] = msk.getCenterY();
			inf[2] = msk.getRadius();
			return inf;
		}
		public void setInfo(double[] info){
			msk.setCenterX(info[0]);
			msk.setCenterY(info[0]);
			msk.setRadius(info[0]);
			cmask.toggle_edge(false);
		}
		public int[] getInfoInt(){
			final int[] inf = {0, 0, 0,};
			inf[0] = (int)msk.getCenterX();
			inf[1] = (int)msk.getCenterY();
			inf[2] = (int)msk.getRadius();
			return inf;
		}
		public void setInfoInt(int[] info){
			msk.setCenterX(info[0]);
			msk.setCenterY(info[1]);
			msk.setRadius(info[2]);			
			cmask.toggle_edge(false);
		}
		
		public ArrayList<MaskData> lstData = new ArrayList<MaskData>();
		
		private void update_data(){
			lstData.clear();
			int[] info = getInfoInt();
			int xa = info[0]-info[2];
			int xb = info[0]+info[2];
			int ya = info[1]-info[2];
			int yb = info[1]+info[2];
			for(int yy=ya; yy<=yb; yy++){
				for(int xx=xa; xx<=xb; xx++){
					if(yy<0 || xx<0){
						continue;
					}
					int aa = xx - info[0];
					int bb = yy - info[1];
					double cc = Math.sqrt(aa*aa+bb*bb);
					if(cc>info[2]){
						continue;
					}
					MaskData dat = new MaskData();
					dat.locaX = xx;
					dat.locaY = yy;
					dat.normX = (double)( xx-info[0]) / info[2];
					dat.normY = (double)(-yy+info[1]) / info[2];
					lstData.add(dat);
				}
			}
		}
		
		private void toggle_edge(boolean flag){
			getChildren().get(0).setVisible(!flag);
			for(int i=1; i<getChildren().size(); i++){
				getChildren().get(i).setVisible(flag);
			}
		}
	};
	protected MaskCirc cmask = new MaskCirc();
	
	protected void setCircleMask(double cx, double cy, double rad){	
		cmask.msk.setCenterX(cx);
		cmask.msk.setCenterY(cy);
		cmask.msk.setRadius(rad);
		cmask.toggle_edge(false);
	}
		
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
	
	private void delFringe(int idx){
		int cntFng = getOverlayList().size();
		if(cntFng==1){
			return;
		}else if(idx<0){
			//remove all fringe!!!			
			getOverlayList().remove(1, cntFng-1);			
		}else if(idx<cntFng){
			getOverlayList().remove(idx);
		}
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
	
	/**
	 * flatten location information(normalized) from all fringes
	 * @return 2-D array, (x,y) values pair
	 */
	public double[][] getFringeDots(){
		//the first node is circle-mask
		//Remember, the fringe index starts with '1'		
		int cntFng = getOverlayList().size() - 1;
		if(cntFng==0){
			return null;
		}
		double[]   circ = cmask.getInfo();
		double[][] data = new double[cntFng][];
		for(int i=0; i<cntFng; i++){
			Group fng = (Group)getOverlayList().get(i+1);
			int cntDot = fng.getChildren().size() - 1;
			Misc.logv("flatten fringe(%d), dots number is %d", i, cntDot);
			data[i] = new double[cntDot*2];
			for(int j=0; j<cntDot; j++){
				Circle dot = (Circle)fng.getChildren().get(j+1);
				data[i][j*2+0] = ( dot.getCenterX()-circ[0])/circ[2];
				data[i][j*2+1] = (-dot.getCenterY()+circ[1])/circ[2];
			}
		}
		return data;
	}
	//----------------------------------//
	
	public void save(String name){
		save(new File(name));
	}
	
	private static final String K_MASK_CX = "M_CX";
	private static final String K_MASK_CY = "M_CY";
	private static final String K_MASK_RAD= "M_RAD";
	private static final String K_F_COUNT = "F_COUNT";
	private static final String K_FRINGE  = "FRINGE_";

	public void save(File fs){
		//TODO: The scale should be 1:1
		try {
			Properties prop = new Properties();
			int[] info = cmask.getInfoInt();
			prop.setProperty(K_MASK_CX , Integer.toString(info[0]));
			prop.setProperty(K_MASK_CY , Integer.toString(info[1]));
			prop.setProperty(K_MASK_RAD, Integer.toString(info[2]));
			int cntFng = getOverlayList().size();
			prop.setProperty(K_F_COUNT, Integer.toString(cntFng-1));
			for(int i=1; i<cntFng; i++){
				//the first node is polynomial-line, the others are dot~~~
				Group grp = (Group)(getOverlayList().get(i));
				int cntDot = grp.getChildren().size();
				String txtDots = "";
				for(int j=1; j<cntDot; j++){
					Circle dot = (Circle)grp.getChildren().get(j);
					txtDots = txtDots + String.format(
						"%d,%d,", 
						(int)dot.getCenterX(), (int)dot.getCenterY()
					);
				}
				prop.setProperty(K_FRINGE+i, txtDots);
			}
			prop.storeToXML(new FileOutputStream(fs), "Fringe data");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void load(String name){
		File fs = new File(name);
		if(fs.exists()==false){
			return;
		}
		load(fs);
	}
	
	public void load(File fs){
		delFringe(-1);
		try {
			Properties prop = new Properties();
			prop.loadFromXML(new FileInputStream(fs));
			int[] info = {
				Integer.valueOf(prop.getProperty(K_MASK_CX)),
				Integer.valueOf(prop.getProperty(K_MASK_CY)),
				Integer.valueOf(prop.getProperty(K_MASK_RAD)),
			};
			cmask.setInfoInt(info);
			int cntFng = Integer.valueOf(prop.getProperty(K_F_COUNT));
			for(int i=1; i<=cntFng; i++){
				addFringe();
				String txtDots = prop.getProperty(K_FRINGE+i);
				String[] txtLoca = txtDots.split(",");				
				for(int j=0; j<txtLoca.length; j+=2){
					int dx = Integer.valueOf(txtLoca[j+0]);
					int dy = Integer.valueOf(txtLoca[j+1]);					
					addFringeDot(dx,dy);
				}
			}
			cmask.update_data();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		
		final Label txtCenter = new Label(Misc.TXT_UNKNOW);
		final Label txtRadius = new Label(Misc.TXT_UNKNOW);
		txtCenter.textProperty().bind(Bindings.format(
			"(%.1f,%.1f)", 
			cmask.msk.centerXProperty(), 
			cmask.msk.centerYProperty()
		));
		txtRadius.textProperty().bind(cmask.msk.radiusProperty().asString("%.1f"));
		layCtrl.add(new Label("圓心："), 0, 7);layCtrl.add(txtCenter , 1, 7);
		layCtrl.add(new Label("半徑："), 0, 8);layCtrl.add(txtRadius , 1, 8);
	}
	
	//private static final KeyCombination hotkeyUndo= new KeyCodeCombination(KeyCode.Z, KeyCombination.);

	@Override
	public void eventChangeScale(int scale) {
		
	}
}
