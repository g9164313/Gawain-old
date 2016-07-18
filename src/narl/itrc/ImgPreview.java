package narl.itrc;

import java.util.ArrayList;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class ImgPreview {
	public CamBundle bundle = null;
	public Image     imdata = null;
	public ImageView screen = null;
	public Canvas    canvas = null;		
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
		if(screen!=null && imdata!=null){
			screen.setImage(imdata);
		}			
	}
	
	public Pane board = null;	
	public void create_board(int[] size){
		screen = new ImageView();
		screen.setFitWidth(size[0]);
		screen.setFitHeight(size[1]);
		
		canvas = new Canvas();
		canvas.widthProperty().bind(screen.fitWidthProperty());
		canvas.heightProperty().bind(screen.fitHeightProperty());
		
		board = new StackPane();
		board.getChildren().addAll(screen,canvas);
		/*new ScrollPane();
		setMinSize(320,240);
		setContent(screen);
		setFitToWidth(true);
		setFitToHeight(true);*/
	}
	
	/**
	 * This is same as OpenCV structure - 'Rect'.<p>
	 * @author qq
	 *
	 */
	public static class Rect {
		int x, y, width, height;
		public Rect(){			
		}
		public Rect(int x,int y,int width,int height){
			this.x = x;
			this.y = y;
			this.width = width;
			this.height= height;
		}
	}
	
	public void clearAll(){
		canvas.getGraphicsContext2D().clearRect(
			0, 0, 
			canvas.getWidth(), canvas.getHeight()
		);
	}
	
	public void drawRect(ArrayList<Rect> list){		
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setStroke(Color.YELLOW);
		gc.setLineWidth(2);
		for(Rect rr:list){
			gc.strokeRect(
				rr.x,rr.y,
				rr.width,rr.height
			);
		}
	}
};

