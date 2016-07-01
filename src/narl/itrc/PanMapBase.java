package narl.itrc;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import com.sun.glass.ui.Application;

import javafx.geometry.Side;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.stage.FileChooser;

public abstract class PanMapBase extends PanDecorate {

	private static final SnapshotParameters parm = new SnapshotParameters();
	
	private final ContextMenu menu = new ContextMenu();
	
	protected static final Color clrGround = Color.web("#b0bec5");
	protected static final Color clrSelect = Color.web("#ffeb3b");
	protected static final Color clrWalking= Color.web("#64b5f6");
	
	public PanMapBase(){
		init();
	}
	
	public PanMapBase(String title){
		super(title);
		init();
	}
	
	public PanMapBase(String title, DevMotion stage){
		super(title);
		init();
	}

	private void init(){
		parm.setFill(Color.TRANSPARENT);

		MenuItem m1 = new MenuItem("匯出layout");
		m1.setOnAction(EVENT->{
			FileChooser dia = new FileChooser();
			dia.setInitialFileName("layout.txt");
			dia.setTitle("匯出layout");
			File fs = dia.showSaveDialog(PanMapBase.this.getScene().getWindow());
			if(fs==null){
				return;
			}
			eventExport(fs);
		});
		menu.getItems().add(m1);
	}

	protected void eventExport(File fs){ }
	
	/**
	 * put cell in map, this is the basic unit!!!
	 * @author qq
	 *
	 */
	public class Die {
		public Color clrState = clrGround;
		
		/**
		 * this variable indicates the scan sequence.<p>
		 * Key is one-based number!!!.<p>
		 * 0 - none.<p>
		 * 1~n - the scan priority, "1" mean the first position.<p>   
		 */
		public int key = 0;
		
		/**
		 * This is the center position of die. unit is 'mm'.<p>
		 * Remember, it is 'Absolute Location'.<p>
		 */
		private double[] pos = {0.,0.};
		
		/**
		 * This is vertex of die mapping to canvas. unit is 'pixel'.<p>
		 * They are center, top-left, top-right, bottom-left and bottom-right.<p>
		 */
		private int[] vtx[]={{0,0}, {0,0}, {0,0}, {0,0}, {0,0}};
		
		public Die(){
		}

		public Die setCenter(double cx,double cy){
			pos[0] = cx;
			pos[1] = cy;			
			//center
			vtx[0][0] = Math.round((float)cx*scale); 
			vtx[0][1] = Math.round((float)cy*scale);
			int ww = dieGrid[0]/2;
			int hh = dieGrid[1]/2;
			//left-top
			vtx[1][0] = vtx[0][0] - ww; 
			vtx[1][1] = vtx[0][1] + hh;
			//right-top
			vtx[2][0] = vtx[0][0] + ww; 
			vtx[2][1] = vtx[0][1] + hh;
			//left-bottom
			vtx[3][0] = vtx[0][0] - ww; 
			vtx[3][1] = vtx[0][1] - hh;
			//right-bottom
			vtx[4][0] = vtx[0][0] + ww; 
			vtx[4][1] = vtx[0][1] - hh;
			return this;
		}
		
		public Die setLfTp(double left,double top){
			return setCenter(
				left+dieSize[0]/2.,
				top -dieSize[1]/2.
			);
		}
		
		public Die setRhTp(double right,double top){
			return setCenter(
				right-dieSize[0]/2.,
				top  -dieSize[1]/2.
			);
		}
		
		public Die setLfBm(double left,double bottom){
			return setCenter(
				left  +dieSize[0]/2.,
				bottom+dieSize[1]/2.
			);
		}
	
		public Die setRhBm(double right,double bottom){
			return setCenter(
				right -dieSize[0]/2.,
				bottom+dieSize[1]/2.
			);
		}
		
		public double[] getPosition(){ 
			return pos;
		}
		public int[] getOrig(){ return vtx[0].clone(); }//It is just the center~~~
		public int[] getLfTp(){ return vtx[1].clone(); }
		public int[] getRhTp(){ return vtx[2].clone(); }
		public int[] getLfBm(){ return vtx[3].clone(); }
		public int[] getRhBm(){ return vtx[4].clone(); }
		
		public boolean isHold(int xx,int yy){			
			if(vtx[1][0]<=xx && xx<=vtx[2][0]){
				if(vtx[3][1]<=yy && yy<=vtx[1][1]){
					/*Misc.logv(
						"[%03d,%03d] -[%03d,%03d] - (%d,%d)",
						vtx[1][0],vtx[2][0],
						vtx[3][1],vtx[1][1],
						xx,yy
					);*/
					return true;
				}
			}
			return false;
		}
	}
		
	/**
	 * this variable keeps all dies information.
	 */
	protected ArrayList<Die> lstDie = new ArrayList<Die>();
	
	/**
	 * this variable shows path, it means how to scan mapping data.<p>
	 * Remember, path must be made by child class.<p>
	 * 'key' value presents the path sequence.<p>
	 */
	protected HashMap<Integer,Die> lstPath = new HashMap<Integer,Die>();
	
	/**
	 * Keep the index of sequence path.<p>
	 * When this variable is less than 0, it means none of sequence.<p>
	 */
	private int curSeq = 0;
	/**
	 * Get absolute location along the sequence path.<p>
	 * Just keep calling this function.<p>
	 * When we reach the end of sequence path, it returned null.<p> 
	 * @param unit - length unit, default unit is 'mm'.
	 * @return location value (x,y).<p> null - the end of sequence path.<p>
	 */
	public Double[] getSequencePath(String unit){
		if(curSeq<=0){
			curSeq = 1;
		}		
		Die dd = lstPath.get(curSeq);
		if(dd==null){
			curSeq = 0;
			clearGround();
			return null;
		}		
		double[] val = dd.getPosition();
		Double[] res = new Double[2];
		res[0] = Misc.phyConvert(val[0], "mm", unit);
		res[1] = Misc.phyConvert(val[1], "mm", unit);
		drawSeqDie();
		curSeq++;//for the next location~~
		return res;
	}
	
	public Double[] getSequencePath(){
		return getSequencePath("mm");
	}	
	
	/**
	 * Reset the sequence path.(Just reset index).<p>
	 */
	public void resetSequence(){
		curSeq = 0;
	}
	
	private final int MIN_SCALE = 1;
	private final int MAX_SCALE = 10;
	/**
	 * indicate how to convert rate between pixel and 'mm'.<p>
	 * unit is 'pix/mm'. It also has range (1~10)
	 */
	private int scale = 4;
	
	protected void incScale(){
		if(scale==MAX_SCALE){
			return;
		}
		scale++;
		resetScale();
		generate();
	}
	
	protected void decScale(){
		if(scale==MIN_SCALE){
			return;
		}
		scale--;
		resetScale();
		generate();
	}
	
	private void resetScale(){
		dieGrid[0] = oddval(dieSize[0]);
		dieGrid[1] = oddval(dieSize[1]);
		mapGrid[0] = oddval(mapSize[0]);
		mapGrid[1] = oddval(mapSize[1]);
	}
	
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
		arrowSize = Math.min(dieGrid[0],dieGrid[1]);
		arrowSize = (67*arrowSize)/200;
		if(arrowSize<=2){
			arrowSize = -1;//let the minimum arrow be 4 pixel~~
		}
		feathOff1 = arrowSize*0.8f;
		feathOff2 = arrowSize*0.2f;
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
	 * When we need to re-draw, use this image to refresh canvas.<p>
	 * This method is like buffered image.<p>
	 */
	private WritableImage mapGround = null;
	
	/**
	 * Generate canvas to draw map, it also hook the event handler.<p>
	 * Attention, This canvas is Cartesian coordinate system.<p> 
	 */

	protected void generate(){
		GraphicsContext gc = mapScreen.getGraphicsContext2D();
		double ww = mapScreen.getWidth();
		double hh = mapScreen.getHeight();
		gc.clearRect(-ww/2, -hh/2,ww, hh);//clear 'old screen'

		mapScreen.setWidth(mapGrid[0]);
		mapScreen.setHeight(mapGrid[1]);
		
		Affine aff = gc.getTransform();
		aff.setTx(mapGrid[0]/2);
		aff.setTy(mapGrid[1]/2);
		//gc.translate(
		//	mapGrid[0]/2,
		//	mapGrid[1]/2
		//);//bug!!, this is not 'set', this function just 'accumulate' value
		gc.setTransform(aff);
		
		lstPath.clear();
		lstDie.clear();
		layoutDie(lstDie);
		
		//drawing!!!
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(1.);
		
		drawAllDie(gc);
		drawShape(gc);		
		drawCross(gc);
	
		mapGround = mapScreen.snapshot(parm,null);		
		setHandler();
	}
	
	public abstract void layoutDie(ArrayList<Die> lst);
	
	public abstract void drawShape(GraphicsContext gc);
	
	private void drawCross(GraphicsContext gc){
		gc.save();
		//gc.setFill(Color.valueOf(value));
		gc.setStroke(Color.RED);
		gc.strokeLine(-30,0,30,0);		
		gc.strokeLine(0,-30,0,30);
		gc.restore();
	}
	
	private int arrowSize = -1;
	private float feathOff1 = 1.f;
	private float feathOff2 = 1.f;
	
	private void drawArrow(GraphicsContext gc,Die die){
		int key = die.key;
		if(key==0){
			return;
		}
		if(key==lstPath.size()){
			return;//we don't need draw the last die~~~
		}
		int[] vaa = die.getOrig();
		int[] vbb = lstPath.get(key+1).getOrig();//key value is one-base!!!
		int len = Misc.hypotInt(vbb, vaa);
		float[] dir = {
			((float)(vbb[0] - vaa[0]))/len,
			((float)(vbb[1] - vaa[1]))/len
		};
		//give offset~~~
		vaa[0] = vaa[0] - (int)((dir[0]*arrowSize)/2.f);
		vaa[1] = vaa[1] - (int)((dir[1]*arrowSize)/2.f);
		//take vector~~~
		vbb[0] = vaa[0] + (int)(arrowSize*dir[0]);
		vbb[1] = vaa[1] + (int)(arrowSize*dir[1]);
		gc.strokeLine(
			vaa[0],vaa[1],
			vbb[0],vbb[1]
		);
		//draw feather by using normal vector
		float[] fth1 = { -dir[1],  dir[0]};
		float[] fth2 = {-fth1[0],-fth1[1]};
		
		vaa[0] = vaa[0] + (int)(feathOff1*dir[0]);
		vaa[1] = vaa[1] + (int)(feathOff1*dir[1]);
		
		gc.setLineWidth(2);
		gc.strokeLine(
			vaa[0]+(int)(feathOff2*fth1[0]),
			vaa[1]+(int)(feathOff2*fth1[1]),
			vbb[0],vbb[1]
		);
		gc.strokeLine(
			vaa[0]+(int)(feathOff2*fth2[0]),
			vaa[1]+(int)(feathOff2*fth2[1]),
			vbb[0],vbb[1]
		);
	}
	
	private void drawAllDie(GraphicsContext gc){
		gc.save();	
		for(Die die:lstDie){
			drawDie(gc,die,die.clrState);
			if(
				lstPath.isEmpty()==true||
				arrowSize<=0
			){
				continue;
			}			
			drawArrow(gc,die);			
		}
		gc.restore();
	}
	
	private void drawDie(GraphicsContext gc,Die die,Color state){
		int[] pos = die.getLfBm();
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(0.5);
		gc.setFill(state);		
		gc.fillRect(
			pos[0], pos[1],
			dieGrid[0], dieGrid[1]
		);
		gc.strokeRect(
			pos[0]+1, pos[1]+1, 
			dieGrid[0]-2, dieGrid[1]-2
		);		
		/*int[] org = die.getOrig();
		gc.setStroke(Color.RED);
		gc.strokeArc(
			org[0]-5,org[1]-5, 
			10, 10, 
			0, 360, 
			ArcType.OPEN
		);*/
	}

	private void _drawCursor(Die die){
		int[] pos = die.getLfBm();
		pos[0] = pos[0] + 7;
		pos[1] = pos[1] + dieGrid[1]/4;
		GraphicsContext gc = mapScreen.getGraphicsContext2D();
		gc.save();
		drawDie(gc,die,clrSelect);
		gc.setFill(Color.BLACK);	
		gc.fillText(
			String.format("%d",die.key),
			pos[0],pos[1]
		);
		gc.restore();
	}
	
	private void _drawSeqDie(){		
		GraphicsContext gc = mapScreen.getGraphicsContext2D();
		gc.save();
		Die die = lstPath.get(curSeq);
		if(die!=null){
			drawDie(gc,die,clrWalking);
		}		
		gc.restore();
	}
	
	public void drawSeqDie(){
		if(Application.isEventThread()==true){
			_drawSeqDie();
		}else{
			Application.invokeAndWait(new Runnable(){
				@Override
				public void run() {
					_clearGround();
					_drawSeqDie();
				}
			});
		}
	}

	private void _clearGround(){
		mapScreen.getGraphicsContext2D().drawImage(
			mapGround,			
			-mapGrid[0]/2,-mapGrid[0]/2
		);		
	}
	
	public void clearGround(){
		if(Application.isEventThread()==true){
			_clearGround();
		}else{
			Application.invokeAndWait(new Runnable(){
				@Override
				public void run() {
					_clearGround();
				}
			});
		}
	}
	
	private void redrawDie(int mx,int my){
		_clearGround();
		//find where cursor is,or which die hold this cursor~~~
		for(Die die:lstDie){
			if(die.isHold(mx,my)==true){			
				_drawCursor(die);
				break;
			}
		}
		if(curSeq>0){
			_drawSeqDie();
		}
	}

	private void setHandler(){
		//mapScreen.setOnMouseClicked(event);
		mapScreen.setOnMouseMoved(EVENT->{
			int mx = (int)EVENT.getX() - mapGrid[0]/2;
			int my = (int)EVENT.getY() - mapGrid[1]/2;
			redrawDie(mx,my);
		});
		mapScreen.setOnMouseClicked(EVENT->{
			MouseButton mb = EVENT.getButton();
			int mx = (int)(EVENT.getX() - mapScreen.getWidth() +5);
			int my = (int)(mapScreen.getHeight() - EVENT.getY()+5);
			if(mb==MouseButton.PRIMARY){
				if(menu.isShowing()==true){
					menu.hide();
					return;
				}
			}else if(mb==MouseButton.SECONDARY){
				menu.show(mapScreen,Side.RIGHT,mx,my);				
			}
		});
		mapScreen.setOnMouseEntered(EVENT->{
			//cursor = null;
			getScene().setCursor(Cursor.CROSSHAIR);
		});
		mapScreen.setOnMouseExited(EVENT->{
			//cursor = null;
			_clearGround();
			getScene().setCursor(Cursor.DEFAULT);
		});		
	}

	private Canvas mapScreen;//let parent create this object!!!	
	@Override
	public Node layoutBody() {
		mapScreen = new Canvas();//this is dummy~~~		

		ScrollPane root = new ScrollPane();
		root.setContent(mapScreen);
		/*root.setContextMenu(menu);
		root.setOnMouseClicked(EVENT->{
			Window win = PanMapBase.this.getScene().getWindow();
			MouseButton mb = EVENT.getButton();
			int mx = (int)EVENT.getX();
			int my = (int)EVENT.getY();
			if(mb==MouseButton.SECONDARY){
				menu.show(mapScreen,Side.RIGHT, 0, 0);
				Misc.logv("right-click, (%03d,%03d)",mx,my);
			}
		});*/
		return root;
	}
}
