package narl.itrc;

import java.util.ArrayList;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

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
	
	private void create_board(String title,int width,int height){
		
		screen = new ImageView();		
		overlay1 = new Canvas();
		overlay2 = new Canvas();
		
		StackPane grp = new StackPane();
		grp.getChildren().addAll(screen,overlay1);
		
		ScrollPane pan = new ScrollPane();
		pan.setMinSize(width+13,height+13);
		pan.setContent(grp);
		pan.setFitToWidth(true);
		pan.setFitToHeight(true);
		//pan.setBorder(value);
		
		board = new VBox();//Do we need some control items??
		if(title.length()==0){
			board.getChildren().add(pan);
		}else{
			board.getChildren().addAll(new Label(title),pan);
		}
	}
	
	private Pane board = null;
	
	public Pane getBoard(String title,int width,int height){
		if(board==null){
			create_board(title,width,height);
		}
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
};

