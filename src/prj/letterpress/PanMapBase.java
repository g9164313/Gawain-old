package prj.letterpress;

import java.util.ArrayList;


import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import narl.itrc.Misc;
import narl.itrc.PanDecorate;

abstract class PanMapBase extends PanDecorate {
	
	private static final SnapshotParameters parm = new SnapshotParameters();
	
	protected static final Color clrCellGround = Color.web("#b0bec5");
	protected static final Color clrCellSelect = Color.web("#ffeb3b");
	protected static final Color clrCellWorking= Color.web("#3f51b5");
	
	public PanMapBase(String title){
		super(title);
		init();
	}
	
	public PanMapBase(){
		init();
	}
	
	private void init(){
		parm.setFill(Color.TRANSPARENT);
	}
	
	/**
	 * put cell in map, this is the basic unit!!!
	 * @author qq
	 *
	 */
	public class Die{
		public Color clrState = clrCellGround;
		/**
		 * this is the center position of die. unit is 'mm'
		 */
		private double[] pos = {0.,0.};
		
		/**
		 * This is vertex of die mapping to canvas. unit is 'pixel'.<p>
		 * They are center, top-left, top-right, bottom-left and bottom-right.<p>
		 */
		private int[] vtx[]={{0,0}, {0,0}, {0,0}, {0,0}, {0,0}};
		
		public Die(double cx,double cy){
			setPosition(cx,cy);
		}
		
		public void setPosition(double cx,double cy){
			pos[0] = cx;
			pos[1] = cy;
			//center
			vtx[0][0] = Math.round((float)cx*scale); 
			vtx[0][1] = Math.round((float)cy*scale);
			//top-left
			vtx[1][0] = vtx[1][0] - dieGrid[0]; 
			vtx[1][1] = vtx[1][1] + dieGrid[1];
			//top-right
			vtx[1][0] = vtx[1][0] - dieGrid[0]; 
			vtx[1][1] = vtx[1][1] + dieGrid[1];
			//bottom-left
			vtx[1][0] = vtx[1][0] - dieGrid[0]; 
			vtx[1][1] = vtx[1][1] + dieGrid[1];
			//bottom-right
			vtx[1][0] = vtx[1][0] - dieGrid[0]; 
			vtx[1][1] = vtx[1][1] + dieGrid[1];
		}
		public double[] getPosition(){ 
			return pos;
		}
		public int[] getOrig(){ return vtx[0]; }//It is just the center~~~
		public int[] getTpLf(){ return vtx[1]; }
		public int[] getTpRh(){ return vtx[2]; }
		public int[] getBmLf(){ return vtx[3]; }
		public int[] getBmRh(){ return vtx[4]; }		
	}
	
	private final int MIN_SCALE = 1;
	private final int MAX_SCALE = 10;
	/**
	 * indicate how to convert rate between pixel and 'mm'.<p>
	 * unit is 'pix/mm'. It also has range (1~10)
	 */
	private int scale = 4;
		
	/**
	 * Indicate this size of die or granule, unit is 'mm'.<p>
	 * "Die" is the basic zone to compose "Map".
	 */
	protected double[] dieSize={0.,0.};
	/**
	 * Indicate die size with canvas dimension, unit is 'pixel'.<p> 
	 */
	protected int[] dieGrid = {0,0};  
	
	protected void setDieSize(double val){		
		setDieSize(val,val);
	}
	protected void setDieSize(double width,double height){		
		dieSize[0] = width;
		dieSize[1] = height;
		dieGrid[0] = oddval(width);
		dieGrid[1] = oddval(height);
	}
	
	/**
	 * indicate the size of map, unit is 'mm'.
	 */
	protected double[] mapSize ={0.,0.};
	/**
	 * Indicate map size with canvas dimension, unit is 'pixel'.<p> 
	 */
	protected int[] mapGrid = {0,0};  
			
	protected void setMapSize(double val){		
		setMapSize(val,val);
	}
	protected void setMapSize(double width,double height){		
		mapSize[0] = width;
		mapSize[1] = height;
		mapGrid[0] = oddval(width);
		mapGrid[1] = oddval(height);
	}

	/**
	 * convert physical value to pixel.<p>
	 * Attention that it will take odd value.<p>
	 * @param val - unit is 'mm'
	 * @return
	 */
	private int oddval(double val){
		val = val * scale;
		int vv = (int)Math.round(val);
		if(vv%2==1){
			return vv;
		}
		return vv+1;
	}
	
	/**
	 * Generate canvas to draw map, it also hook the event handler.<p>
	 */
	protected void generate(){
		mapScreen.setWidth(mapGrid[0]);
		mapScreen.setHeight(mapGrid[1]);
		mapScreen.setScaleX( 1.);
		mapScreen.setScaleY(-1.);
		GraphicsContext gc = mapScreen.getGraphicsContext2D();
		gc.translate(mapGrid[0]/2,mapGrid[1]/2);
		
		//drawing!!!
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(1.);
		
		drawCross(gc);
		drawShape(gc);
		
		setHandler();
	}
	
	abstract void drawShape(GraphicsContext gc);
	
	private void drawCross(GraphicsContext gc){
		gc.save();
		//gc.setFill(Color.valueOf(value));
		gc.strokeLine(0, 0,30, 0);
		gc.setStroke(Color.RED);
		gc.strokeLine(0,30,30,30);
		gc.restore();
	}
	
	private void setHandler(){
		//mapScreen.setOnMouseClicked(event);
		mapScreen.setOnMouseMoved(EVENT->{			
			
		});
		mapScreen.setOnMouseMoved(EVENT->{
			EventType<?> typ = EVENT.getEventType();
			int mx = (int)EVENT.getX() - mapGrid[0]/2;
			int my = (int)EVENT.getY() - mapGrid[1]/2;
			Misc.logv("mouse=(%03d,%03d)",mx,my);
		});
		mapScreen.setOnMouseEntered(EVENT->{
			getScene().setCursor(Cursor.CROSSHAIR);
		});
		mapScreen.setOnMouseExited(EVENT->{
			getScene().setCursor(Cursor.DEFAULT);
		});
	}
	
	private WritableImage mapGround = null;
	private Canvas mapScreen;//let parent create this object!!!	
	@Override
	public Node layoutBody() {
		mapScreen = new Canvas();//this is dummy~~~		
		ScrollPane root = new ScrollPane();
		root.setContent(mapScreen);
		return root;
	}	
}
