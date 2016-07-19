package narl.itrc;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

public class ImgPreview {
		
	public CamBundle bundle = null;
	public Image imdata = null;
	
	private ImageView screen = null;
	private Canvas overlay1 = null;//show information	
	private Canvas overlay2 = null;//show ROI
	
	public ImgPreview(CamBundle bnd){
		bundle = bnd;
	}
	
	public void fetch(){
		imdata = null;
		if(bundle!=null){
			bundle.fetch();
			imdata = bundle.getImage();
		}
	}
	
	public void refresh(){
		//called by GUI-thread
		if(screen==null || imdata==null){
			return;
		}
		screen.setImage(imdata);
		
		//always check dimension~~~
		int sw = (int)imdata.getWidth();
		int sh = (int)imdata.getHeight();
		int dw = (int)overlay1.getWidth();
		int dh = (int)overlay1.getHeight();
		if(sw!=dw||sh!=dh){			
			overlay1.setWidth(sw); 
			overlay1.setHeight(sh);
			overlay2.setWidth(sw); 
			overlay2.setHeight(sh);
		}
	}
	
	private Pane board = null;
	private Label txtName = new Label();
	private Label txtInf1 = new Label();
	private Label txtInf2 = new Label();
	
	public Pane genBoard(String title,int width,int height){
		
		if(board!=null){
			return board;//if we have already create board, just pass it~~~
		}
		
		txtName.setText(title);
		screen = new ImageView();		
		overlay1 = new Canvas();
		overlay2 = new Canvas();
		overlay2.setOnMousePressed(event->{
			Mark mk = getCurrentMark();
			if(mk==null){ return; }
			if(event.getButton()!=MouseButton.PRIMARY){
				return;
			}
			pinVal[0] = pinVal[2] = (int)event.getX();
			pinVal[1] = pinVal[3] = (int)event.getY();
		});
		overlay2.setOnMouseDragged(event->{
			Mark mk = getCurrentMark();
			if(mk==null){ return; }//Do we need this??
			int mx = (int)event.getX();
			int my = (int)event.getY();
			switch(mk.type){
			case MARK_PONT:
				pinVal[0] = pinVal[2] = mx;
				pinVal[1] = pinVal[3] = my;
				break;
			case MARK_RECT:
				pinVal[2] = mx;
				pinVal[3] = my;				
				break;
			}
			drawPinPoint();
		});
		overlay2.setOnMouseReleased(event->{
			Mark mk = getCurrentMark();
			if(mk==null){ return; }//Do we need this??
			if(event.getButton()!=MouseButton.PRIMARY){
				return;
			}
			int mx = (int)event.getX();
			int my = (int)event.getY();
			switch(mk.type){
			case MARK_PONT:
				pinVal[0] = pinVal[2] = mx;
				pinVal[1] = pinVal[3] = my;
				break;
			case MARK_RECT:
				pinVal[2] = mx;
				pinVal[3] = my;				
				break;
			}
			mk.nail(pinVal);
			refreshMark();
		});	
		
		StackPane grp = new StackPane();
		grp.getChildren().addAll(
			screen,
			overlay1,
			overlay2
		);
		
		ScrollPane pan = new ScrollPane();
		pan.setMinSize(width+13,height+13);
		pan.setContent(grp);
		pan.setFitToWidth(true);
		pan.setFitToHeight(true);
		pan.setContextMenu(create_menu());

		HBox lay0 = new HBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.getChildren().addAll(txtName,txtInf1,txtInf2);
		
		board = new VBox();		
		board.getChildren().addAll(lay0,pan);
		
		return board;		
	}
		
	public static final String[] markTypeName = {
		"單點",
		"矩形"
	};
	public static final int MARK_NONE =-1;
	public static final int MARK_PONT = 0;//it is according to name sequence
	public static final int MARK_RECT = 1;//it is according to name sequence!!!
	
	public static class Mark {
		private Color clr = Color.YELLOW;
		public int type = MARK_NONE;
		public int[] loca = {0,0};
		public int[] size = {0,0};//width, height or radius		
		public Mark(Color clr){
			this.clr = clr;
		}
		private int[] roi = {0,0,0,0};
		public int[] getROI(){
			if(type==MARK_NONE||type==MARK_PONT){
				return null;
			}
			roi[0] = loca[0];//location-x
			roi[1] = loca[1];//location-y
			roi[2] = size[0];//width
			roi[3] = size[1];//height
			return roi;
		}
		private void nail(int[] val){
			if(val[0]<=val[2]){
				loca[0] = val[0];
				size[0] = val[2] - val[0];
			}else{
				loca[0] = val[2];
				size[0] = val[0] - val[2];
			}
			if(val[1]<=val[3]){
				loca[1] = val[1];
				size[1] = val[3] - val[1];
			}else{
				loca[1] = val[3];
				size[1] = val[1] - val[3];
			}
		}
	};
	
	public Mark mark[] = {
		new Mark(Color.TOMATO),
		new Mark(Color.CHOCOLATE),
		new Mark(Color.AQUA),
		new Mark(Color.DARKVIOLET)
	};

	private Mark getCurrentMark(){
		Toggle itm = roiType.getSelectedToggle();
		if(itm==null){
			return null;
		}
		int key = (int)itm.getUserData();
		int idx = (key&0xFF00)>>8;
		return mark[idx];
	}
		
	private void clearMark(int idx){
		if(idx<0 || idx>=mark.length){
			for(int i=0; i<mark.length; i++){
				mark[i].type = MARK_NONE;
			}
		}else{
			mark[idx].type = MARK_NONE;
		}
	}
	
	private int pinVal[] = {0,0,0,0};//the first and second pin point
	
	private void drawPinPoint(){
		GraphicsContext gc = overlay2.getGraphicsContext2D();
		int ww = (int)overlay2.getWidth();
		int hh = (int)overlay2.getHeight();
		gc.clearRect(0,0,ww,hh);		
		gc.setLineWidth(1);	
		gc.setStroke(Color.YELLOW);
		gc.strokeLine(
			pinVal[0], pinVal[1], 
			pinVal[2], pinVal[3]
		);
		gc.strokeArc(
			pinVal[2]-10, pinVal[3]-10, 
			20, 20, 
			0., 360.,
			ArcType.OPEN
		);
	}

	private void refreshMark(){
		GraphicsContext gc = overlay2.getGraphicsContext2D();
		int ww = (int)overlay2.getWidth();
		int hh = (int)overlay2.getHeight();
		gc.clearRect(0,0,ww,hh);
		gc.setLineWidth(1);		
		for(int i=0; i<mark.length; i++){
			Mark m = mark[i];
			gc.setStroke(m.clr);
			switch(m.type){
			case MARK_PONT:
				gc.strokeArc(
					m.loca[0]-2,m.loca[1]-2, 
					5, 5, 
					0., 360., 
					ArcType.OPEN
				);
				break;
			case MARK_RECT:
				gc.strokeRect(
					m.loca[0],m.loca[1],
					m.size[0],m.size[1]
				);
				break;
			}
		}
	}
	
	private final ToggleGroup roiType = new ToggleGroup();
	
	private ContextMenu create_menu(){		
		final 
		ContextMenu root = new ContextMenu();
		for(int i=0; i<mark.length; i++){			
			Menu subs = new Menu("標記 "+i);
			for(int j=0; j<markTypeName.length; j++){
				RadioMenuItem chk = new RadioMenuItem(markTypeName[j]);
				chk.setToggleGroup(roiType);
				chk.setUserData((int)((i<<8)+j));
				chk.setOnAction(event->{
					MenuItem itm = (MenuItem)event.getSource();
					int key = (int)itm.getUserData();
					int idx = (key&0xFF00)>>8;
					int typ = (key&0x00FF);
					mark[idx].type = typ;
				});
				subs.getItems().add(chk);
			}
			root.getItems().add(subs);
		}		
		MenuItem itm0 = new MenuItem("取消");
		itm0.setOnAction(event->{
			clearMark(-1);//clear all mark~~~~
			roiType.selectToggle(null);
		});
		root.getItems().add(itm0);
		return root;
	}

	public Pane getBoard(){
		return board;
	}
	
	public long getMatx(){
		return bundle.getMatx();
	}
	
	/**
	 * This is same as OpenCV structure - 'Rect'.<p>
	 * @author qq
	 *
	 */
	/*public static class Rect {
		int x, y, width, height;
		public Rect(){			
		}
		public Rect(int x,int y,int width,int height){
			this.x = x;
			this.y = y;
			this.width = width;
			this.height= height;
		}
	}*/
	
	//protected static final Color clrGround = Color.web("#b0bec5");
	
	public void clearAll(){
		overlay1.getGraphicsContext2D().clearRect(
			0, 0, 
			overlay1.getWidth(), overlay1.getHeight()
		);
	}
	
	/**
	 * Draw rectangle, the array size must be 4 times.<p>
	 * Each elements present 'x', 'y', 'width', and 'height'
	 * @param rect - [x,y,width,height], [x,y,width,height]...
	 */
	public void drawRect(int[] rect){
		if(rect==null){
			return;
		}
		GraphicsContext gc = overlay1.getGraphicsContext2D();
		gc.setStroke(Color.GREENYELLOW);
		gc.setLineWidth(2);
		for(int i=0; i<rect.length; i+=4){
			gc.strokeRect(
				rect[i+0],rect[i+1],
				rect[i+2],rect[i+3]
			);
		}
	}
	
	public void drawContour(int[] pts){
		if(pts==null){
			return;
		}
		GraphicsContext gc = overlay1.getGraphicsContext2D();
		gc.setStroke(Color.GREENYELLOW);
		for(int i=0; i<pts.length; i+=2){
			//gc.strokePolygon(xPoints, yPoints, nPoints);
		}
	}
};

