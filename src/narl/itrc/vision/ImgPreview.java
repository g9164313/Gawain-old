package narl.itrc.vision;

import com.sun.glass.ui.Application;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import narl.itrc.Misc;

public class ImgPreview extends BorderPane {
	
	public CamBundle bundle = null;
	public ImgRender render = null;
	
	/**
	 * This image data came from camera-bundle.<p>
	 * It means data is created and acquired by camera.<p>
	 * The first is original image, and the second is augment data.<p>
	 */
	private Image[] imdata = { null, null };
	
	/**
	 * Viewer object for 'imdata'.<p>
	 * The first is grabbed image.<p>
	 * The second is augment image(edge, point, and mask etc...).<p>
	 */
	private ImageView[] screen = { 
		new ImageView(), 
		new ImageView()
	};
	
	/**
	 * It is still a viewer object, but specially for GUI-thread.<p>
	 */
	private Canvas board = new Canvas();//show ROI
	
	public ImgPreview(CamBundle bnd,ImgRender rnd){
		bundle = bnd;
		render = rnd;		
		init_layout();
	}
	
	public void fetchBuff(){
		imdata[0] = null;
		if(bundle!=null){
			bundle.fetch();
			imdata[0] = bundle.getImgBuff();
		}
	}
	
	public void fetchInfo(){
		imdata[1] = null;
		if(bundle!=null){
			//information is gave by filter 
			imdata[1] = bundle.getImgInfo();
		}
	}
	
	public void rendering(){
		//called by GUI-thread
		if(Application.isEventThread()==false){
			return;
		}
		screen[0].setImage(imdata[0]);
		screen[1].setImage(imdata[1]);		
		if(imdata[0]!=null){
			//check overlay dimension,always~~~
			int sw = (int)imdata[0].getWidth();
			int sh = (int)imdata[0].getHeight();
			int dw = (int)board.getWidth();
			int dh = (int)board.getHeight();
			if(sw!=dw||sh!=dh){			
				board.setWidth(sw); 
				board.setHeight(sh);
			}
		}
		//show some marks when mouse is dragging!!!!		
	}
	//--------------------------//

	private void init_layout(){
		board.setOnMouseMoved(event->{
			if(markIndx<0){
				return;
			}
			GraphicsContext gc = board.getGraphicsContext2D();
			double mx = event.getX();
			double my = event.getY();			
			clearAll(gc);
			drawCross(gc,mx,my);
		});
		board.setOnMousePressed(event->{
			if(event.getButton()!=MouseButton.PRIMARY){
				return;
			}
			if(markIndx<0){
				return;
			}
			GraphicsContext gc = board.getGraphicsContext2D();
			double mx = event.getX();
			double my = event.getY();						
			drawNailPoint1(gc,mx,my);
			//keep the first nail point!!!
			markList[markIndx].pts1[0] = markList[markIndx].pts2[0] = (int)mx;
			markList[markIndx].pts1[1] = markList[markIndx].pts2[1] = (int)my;
		});
		board.setOnMouseDragged(event->{
			if(event.getButton()!=MouseButton.PRIMARY){
				return;
			}
			if(markIndx<0){
				return;
			}
			drawNailPoint2(
				board.getGraphicsContext2D(),
				event.getX(),
				event.getY()
			);
		});
		board.setOnMouseReleased(event->{
			if(event.getButton()!=MouseButton.PRIMARY){
				return;
			}
			if(markIndx<0){
				return;
			}
			if(markList[markIndx].type!=MARK_PONT){			
				markList[markIndx].pts2[0] = (int)event.getX();
				markList[markIndx].pts2[1] = (int)event.getY();
			} 			
			markIndx = MARK_NONE;//reset state for next turn~~~
			drawAllMark();
		});
		
		StackPane lay0 = new StackPane();
		lay0.getChildren().addAll(
			screen[0],
			screen[1],
			board
		);
		ScrollPane lay1 = new ScrollPane();
		lay1.setMinSize(640,480);		
		lay1.setContent(lay0);
		lay1.setContextMenu(prepare_menu());
		setCenter(lay1);
	}
	
	private ContextMenu prepare_menu(){
		
		final ContextMenu root = new ContextMenu();
		
		Menu men;
		MenuItem itm;
		
		men = new Menu("Mark");
		prepare_mark_menu(0,men);
		prepare_mark_menu(1,men);
		prepare_mark_menu(2,men);
		prepare_mark_menu(3,men);
		root.getItems().add(men);
		
		itm = new MenuItem("Save");
		itm.setOnAction(event->{
			render.snap("snap.png");
		});
		root.getItems().add(itm);
		
		itm = new MenuItem("ImageJ");
		itm.setOnAction(event->{
			render.execIJ(this);
		});
		root.getItems().add(itm);
		
		itm = new MenuItem("Clear");
		itm.setOnAction(event->{
			bundle.clearImgInfo();
		});
		root.getItems().add(itm);
		
		itm = new MenuItem("Bulk");
		itm.setOnAction(event->{			
			
		});
		root.getItems().add(itm);
				
		itm = new MenuItem("Setting");
		itm.setOnAction(event->{			
			bundle.showSetting(this);
		});
		root.getItems().add(itm);
		
		return root;
	}

	private void prepare_mark_menu(final int idx,Menu root){
		Menu sub = new Menu(String.format("%d",idx+1));
		ToggleGroup grp = new ToggleGroup();
		sub.getItems().addAll(
			prepare_mark_radio(grp,idx,MARK_PONT),
			prepare_mark_radio(grp,idx,MARK_LINE),
			prepare_mark_radio(grp,idx,MARK_RECT),
			prepare_mark_radio(grp,idx,MARK_CIRC),
			prepare_mark_radio(grp,idx,MARK_NONE)
		);
		root.getItems().add(sub);
	}
	
	private MenuItem prepare_mark_radio(ToggleGroup grp,final int idx,final int type){
		final RadioMenuItem itm = new RadioMenuItem();
		itm.setOnAction(event->{
			if(type==MARK_NONE){				
				markIndx = -1;				
			}else{
				markIndx = idx;
			}			
			markList[idx].type = type;
			Misc.logv("set ROI(%d), type(%d)",idx,type);
			drawAllMark();
		});
		itm.setToggleGroup(grp);
		switch(type){
		case MARK_PONT:
			itm.setText("Point");
			break;
		case MARK_LINE:
			itm.setText("Line");
			break;
		case MARK_RECT:
			itm.setText("Rectangle");
			break;
		case MARK_CIRC:
			itm.setText("Circle");
			break;
		case MARK_NONE:
			itm.setText("取消");
			break;
		}
		if(markList[idx].type==type){
			itm.setSelected(true);
		}
		return itm;
	}
	//--------------------------//
	
	public static final int MARK_NONE =-1;
	public static final int MARK_PONT = 0;
	public static final int MARK_LINE = 1;
	public static final int MARK_RECT = 2;
	public static final int MARK_CIRC = 3;
	
	private static class Mark {
		
		public Color clr = Color.YELLOW;
		
		public int type = MARK_NONE;
		
		/**
		 * The first nail point when user press button.<p>
		 * This location is relative to viewer.The Scale is 1:1<p>
		 */
		public int[] pts1 = {-1,-1};//start point
		
		/**
		 * The final nail point when user.<p>
		 * This location is relative to viewer.The Scale is 1:1<p>
		 */
		public int[] pts2 = {-1,-1};//end point
		
		public Mark(Color clr){
			this.clr = clr;
		}		
		
		public int[] getROI(){
			final int[] roi = {0,0,0,0};//X-value, Y-value, width,height
			if(type==MARK_NONE){ 
				return null;
			}
			switch(type){
			case MARK_PONT:
				roi[0] = pts1[0];
				roi[1] = pts1[1];
				roi[2] = 1;
				roi[3] = 1;
				break;
			case MARK_LINE:
			case MARK_RECT:
				roi[0] = Math.min(pts1[0], pts2[0]);
				roi[1] = Math.min(pts1[1], pts2[1]);
				roi[2] = Math.abs(pts1[0] - pts2[0]) + 1;
				roi[3] = Math.abs(pts1[1] - pts2[1]) + 1;
				break;
			case MARK_CIRC:				
				double hx = Math.abs(pts1[0] - pts2[0]) + 1;
				double hy = Math.abs(pts1[1] - pts2[1]) + 1;
				double hypt = Math.sqrt(hx*hx + hy*hy);
				roi[0] = pts1[0] - (int)hypt;
				roi[1] = pts1[1] - (int)hypt;
				roi[2] = roi[3] = (int)(2.*hypt);
				break;
			}
			return roi;
		}
	};
		
	/**
	 * Storage mark information, we support 4 mark to indicate data.<p>
	 */
	private Mark markList[] = {
		new Mark(Color.TOMATO),
		new Mark(Color.CHOCOLATE),
		new Mark(Color.AQUA),
		new Mark(Color.DARKVIOLET)
	};

	/**
	 * Which mark structure is assigned.<p>
	 * The negative index means no mark assigned.<p>  
	 */
	private int markIndx = MARK_NONE;

	public void setROI(int x, int y, int w, int h){
		markList[0].pts1[0] = x;
		markList[0].pts1[1] = y;
		markList[0].pts2[0] = x + w;
		markList[0].pts2[1] = y + h;
		markList[0].type = MARK_RECT;
		drawAllMark();
	}
	
	public int[] getMark(int i){
		if(i<0 || markList.length<=i){
			return null;
		}
		if(markList[i].type==MARK_NONE){
			return null;
		}
		return markList[i].getROI();
	}
		
	private void clearAll(GraphicsContext gc){
		gc.clearRect(
			0., 0., 
			board.getWidth(), board.getHeight()
		);
	}
	
	private final double nailHalfSize = 5.;
	
	private void drawCross(GraphicsContext gc,double mx,double my){
		gc.setStroke(Color.RED);
		gc.strokeLine(
			mx-nailHalfSize, my, 
			mx+nailHalfSize, my
		);
		gc.strokeLine(
			mx, my-nailHalfSize, 
			mx, my+nailHalfSize
		);
	}

	private void drawNailPoint1(GraphicsContext gc,double mx,double my){
		gc.clearRect(
			0., 0., 
			board.getWidth(), board.getHeight()
		);
		gc.setStroke(Color.YELLOW);
		gc.strokeRect(
			mx-nailHalfSize, my-nailHalfSize, 
			2*nailHalfSize , 2*nailHalfSize
		);
	}
	
	private void drawNailPoint2(GraphicsContext gc,double mx,double my){
		double _mx = (double)markList[markIndx].pts1[0];
		double _my = (double)markList[markIndx].pts1[1];
		gc.clearRect(
			0., 0., 
			board.getWidth(), board.getHeight()
		);
		gc.setStroke(Color.YELLOW);
		switch(markList[markIndx].type){
		case MARK_PONT:
		case MARK_LINE:
			gc.strokeRect(
				_mx-nailHalfSize, _my-nailHalfSize, 
				2*nailHalfSize , 2*nailHalfSize
			);
			gc.strokeLine(_mx, _my,	mx, my);
			gc.strokeArc(
				mx-nailHalfSize, my-nailHalfSize, 
				2*nailHalfSize , 2*nailHalfSize,
				0., 360.,
				ArcType.OPEN
			);
			break;
		case MARK_RECT:
			double xx = Math.min(_mx,mx);
			double yy = Math.min(_my,my);
			double ww = Math.abs(_mx-mx) + 1;
			double hh = Math.abs(_my-my) + 1;
			gc.strokeRect(xx, yy, ww, hh);
			break;
		case MARK_CIRC:			
			gc.setLineDashes(25d, 10d);
			gc.strokeRect(
				_mx-nailHalfSize, _my-nailHalfSize, 
				2*nailHalfSize , 2*nailHalfSize
			);
			double hx = Math.abs(mx - _mx) + 1;
			double hy = Math.abs(my - _my) + 1;
			double hypt = Math.sqrt(hx*hx+hy*hy);
			gc.strokeArc(
				_mx - hypt, _my - hypt, 
				2*hypt , 2*hypt,
				0., 360.,
				ArcType.OPEN
			);
			gc.setLineDashes(null);
			break;
		}
	}
	
	private void drawAllMark(){
		GraphicsContext gc = board.getGraphicsContext2D();
		clearAll(gc);
		for(Mark mm:markList){
			drawMarkShape(gc,mm);
		}
	}
	
	private void drawMarkShape(GraphicsContext gc,Mark mm){
		final int[] roi = mm.getROI();
		gc.setStroke(mm.clr);
		switch(mm.type){
		case MARK_PONT:
			gc.strokeLine(
				mm.pts1[0]-nailHalfSize, mm.pts1[1]-nailHalfSize, 
				mm.pts1[0]+nailHalfSize, mm.pts1[1]+nailHalfSize
			);
			gc.strokeLine(
				mm.pts1[0]-nailHalfSize, mm.pts1[1]+nailHalfSize, 
				mm.pts1[0]+nailHalfSize, mm.pts1[1]-nailHalfSize
			);
			break;
		case MARK_LINE:
			gc.strokeLine(
				mm.pts1[0], mm.pts1[1], 
				mm.pts2[0], mm.pts2[1]
			);
			break;
		case MARK_RECT:
			gc.strokeRect(
				roi[0], roi[1], 
				roi[2], roi[3]
			);
			break;
		case MARK_CIRC:
			gc.strokeArc(
				roi[0], roi[1],
				roi[2], roi[3],
				0., 360., 
				ArcType.OPEN
			);
			break;
		}
	}
};



