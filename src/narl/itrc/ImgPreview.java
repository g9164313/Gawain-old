package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class ImgPreview extends BorderPane {
		
	public CamBundle bundle = null;
	public ImgRender render = null;
	
	private Image[] imdata = {null, null};
	
	private ImageView[] screen ={
		new ImageView(),//show grabbed image
		new ImageView(),//show augment image(edge, point, and mask etc...)
	};
	private Canvas overlay = new Canvas();//show ROI
	
	public ImgPreview(ImgRender rnd,CamBundle bnd){
		render = rnd;
		bundle = bnd;
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
		//check overlay dimension,always~~~
		if(imdata[0]!=null){
			int sw = (int)imdata[0].getWidth();
			int sh = (int)imdata[0].getHeight();
			int dw = (int)overlay.getWidth();
			int dh = (int)overlay.getHeight();
			if(sw!=dw||sh!=dh){			
				overlay.setWidth(sw); 
				overlay.setHeight(sh);
			}
		}
	}
	//--------------------------//

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
		/*GraphicsContext gc = overlay2.getGraphicsContext2D();
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
		);*/
	}

	private void refreshMark(){
		/*GraphicsContext gc = overlay2.getGraphicsContext2D();
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
		}*/
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
		
		MenuItem itm;
		itm = new MenuItem("執行 ImageJ");
		itm.setOnAction(event->{
			render.execIJ(this);
		});
		root.getItems().add(itm);
		
		itm = new MenuItem("取消標記");
		itm.setOnAction(event->{
			clearMark(-1);//clear all mark~~~~
			roiType.selectToggle(null);
		});
		root.getItems().add(itm);
		return root;
	}

	//protected static final Color clrGround = Color.web("#b0bec5");
	
	/*public void clearAll(){
		overlay1.getGraphicsContext2D().clearRect(
			0, 0, 
			overlay1.getWidth(), overlay1.getHeight()
		);
	}*/
	
	/*public void drawRect(int[] rect){
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
	}*/	
	//--------------------------//

	private void init_layout(){
		StackPane lay0 = new StackPane();
		lay0.getChildren().addAll(
			screen[0],
			screen[1],
			overlay
		);
		ScrollPane lay1 = new ScrollPane();
		lay1.setMinSize(640,480);		
		lay1.setContent(lay0);
		lay1.setContextMenu(create_menu());
		setCenter(lay1);
	}
	
	/*public Pane genBoard(String title,int width,int height){		
	if(board!=null){
		//if we have already create board, just pass it~~~
		return board;
	}
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
		screen[0]
	);
	
	ScrollPane pan = new ScrollPane();
	pan.setMinSize(width+13,height+13);		
	pan.setContent(grp);
	pan.setContextMenu(create_menu());
	//pan.setFitToWidth(true);
	//pan.setFitToHeight(true);
	//HBox.setHgrow(lay2,Priority.ALWAYS);
	
	board = new BorderPane();
	board.setCenter(pan);		
	return board;		
	}*/
};



