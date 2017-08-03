package narl.itrc;

import java.util.HashMap;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class WidDiagram1 extends StackPane {
	
	protected static final int TILE_SIZE = 40;

	private double width=20*TILE_SIZE, height=20*TILE_SIZE; 
	
	private Canvas blue_p = new Canvas();//show blueprint
	private Canvas ground = new Canvas();//draw background	
	private Canvas cursor = new Canvas();//draw cursor
	
	private AnchorPane pegged = new AnchorPane();//speical layout for hooking!!!
	
	private final GraphicsContext gc_ground = ground.getGraphicsContext2D();
	private final GraphicsContext gc_cursor = cursor.getGraphicsContext2D();
	
	public WidDiagram1(){
		init();
	}
	
	private void init(){
		
		gc_cursor.setFill(Color.TRANSPARENT);
		gc_cursor.setStroke(Color.YELLOWGREEN);
		gc_cursor.setLineWidth(2);
	
		watcher.setCycleCount(Timeline.INDEFINITE);
		
		blue_p.setWidth(width);
		blue_p.setHeight(height);
		ground.setWidth(width);
		ground.setHeight(height);
		cursor.setWidth(width);
		cursor.setHeight(height);

		//pegged.getStyleClass().add("decorate2-border");
		pegged.setPickOnBounds(true);
		pegged.setPrefSize(width, height);
		//pegged.getChildren().add(toolbox);
		
		loadPumpTile();
		
		draw_blue_print();
		init_tool_box();
		
		getStyleClass().add("pad-small");
		getChildren().addAll(blue_p,ground,pegged,cursor);
				
		editMode(false);
	}
		
	protected Label addLabel(String title,int gx,int gy){
		Label txt = new Label(title);
		txt.setStyle("-fx-font-size: 23px;");
		AnchorPane.setLeftAnchor(txt, (double)(gx*TILE_SIZE + TILE_SIZE));//why???
		AnchorPane.setTopAnchor (txt, (double)(gy*TILE_SIZE));		
		pegged.getChildren().add(txt);
		return txt;
	}
	
	private void draw_blue_print(){

		GraphicsContext gc = blue_p.getGraphicsContext2D();
		
		Image img = new Image(WidDiagram.class.getResourceAsStream("/narl/itrc/res/tile/blueprint.png"));
		
		int step_w = (int)img.getWidth();
		int step_h = (int)img.getHeight();
		
		for(int sh=0; sh<height; sh+=step_h){
			for(int sw=0; sw<width; sw+=step_w){
				gc.drawImage(img, sw, sh);
			}
		}
	}
	
	private HBox toolbox= new HBox();
	
	private void init_tool_box(){
		toolbox.setStyle(
			"-fx-background-color: palegreen; "+
			"-fx-padding: 13;"+
			"-fx-spacing: 7; "+
			"-fx-background-radius: 10; "+		
			"-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"		
		);
		
		Button btnSave = new Button("Save");
		btnSave.setOnAction(event->{
			Misc.logv("debug!!!");
		});
		//btnSave.setPrefSize(32., 32.);
		
		Button btnLoad = new Button("Load");
		//btnLoad.setPrefSize(32., 32.);
		
		toolbox.getChildren().addAll(btnSave,btnLoad);
		
		AnchorPane.setLeftAnchor(toolbox, width/4.);
		AnchorPane.setBottomAnchor(toolbox, 17.);
	}

	public void editMode(boolean flag){
		if(flag==true){
			watcher.pause();
			toolbox.setVisible(true);
			cursor.setOnMouseMoved(null);
			cursor.setOnMouseClicked(null);
		}else{
			watcher.play();
			toolbox.setVisible(false);
			//cursor.setOnMouseMoved(eventUserMove);			
			cursor.setOnMouseClicked(eventUserClick);
		}
		blue_p.setVisible(flag);
	}
	//----------------------------------------//
	
	/*private EventHandler<MouseEvent> eventEditMove = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
		}
	};
	
	private EventHandler<MouseEvent> eventEditClick = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
		}
	};
	
	private EventHandler<MouseEvent> eventUserMove = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			final int cx = (int)(event.getX());
			final int cy = (int)(event.getY());
		}
	};*/
	
	private EventHandler<MouseEvent> eventUserClick = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			final int cx = (int)(event.getX());
			final int cy = (int)(event.getY());
			mapPart.forEach((name,itm)->{
				if(itm.clickable==false){
					//some symbol is special, they don't need click event~~
					return;
				}
				if(itm.contains(cx, cy)==true){
					if(itm.flag!=0){
						itm.flag = 0;
					}else{
						if(itm.face.length<=1){
							itm.flag = 0;
						}else{
							itm.flag = 1;
						}
					}
					itm.redraw();
					itm.handle(event);
					return;
				}
			});
		}
	};
	//----------------------------------------//
	
	protected abstract class ItmPart implements EventHandler<MouseEvent> {
		
		public Image[]  face = null;		
		public double[] loca = {0.,0.};//x,y coordinate...
		public int      flag = 0;
		
		public boolean clickable = true;
		
		public ItmPart(){
			eventInit(null);
		}

		public ItmPart(String category){
			this(category,0,0);
		}
		
		public ItmPart(String category, int gx, int gy){
			this(category,(double)gx,(double)gy);
		}
		
		public ItmPart(String category, double gx, double gy){
			locate(category,gx,gy);
			eventInit(null);
		}
		
		public void eventInit(DevBase device){			
		}
		
		protected void locate(String category,double gx,double gy){
			face = faces.get(category);			
			if(
				face.length==1 ||
				category.contains("pipe")==true ||
				category.contains("wall")==true
			){
				clickable = false;
			}
			loca[0] = gx * TILE_SIZE;
			loca[1] = gy * TILE_SIZE;
			eventInit(null);
		}
		
		private void makeup(){
			if(face.length<=2){
				return;
			}			
			if(flag==0){
				return;
			}
			redraw();
			flag++;
			if(flag>=face.length){
				flag = 1;//reset~~~~
			}
		}
		
		public boolean contains(int x, int y){
			int width = (int)(face[0].getWidth());
			int height= (int)(face[0].getHeight());
			if( 
				loca[0]<=x && x<=(loca[0]+width ) &&
				loca[1]<=y && y<=(loca[1]+height)
			){
				return true;
			}
			return false;
		}
		
		public void clear(){
			gc_ground.clearRect(
				loca[0]+1., 
				loca[1]+1., 
				face[0].getWidth()-1.,
				face[0].getHeight()-1.
			);
		}
		
		public void draw(int idx){
			gc_ground.drawImage(
				face[idx],
				loca[0], loca[1]
			);
		}
		
		public void redraw(){
			clear();
			draw(flag);
		}
	};
	protected HashMap<String,ItmPart> mapPart = new HashMap<String,ItmPart>();
		
	protected void redraw(){
		mapPart.forEach((name,itm)->itm.redraw());
	}
	
	private final Timeline watcher = new Timeline(new KeyFrame(
		Duration.millis(200),
		event->{
			mapPart.forEach((name,itm)->itm.makeup());
		}
	));
	//----------------------------------------//
	
	protected HashMap<String,Image[]> faces  = new HashMap<String,Image[]>();

	protected static final String CATE_WALL_A = "wall_1";
	protected static final String CATE_WALL_B = "wall_2";
	protected static final String CATE_WALL_C = "wall_3";
	protected static final String CATE_WALL_D = "wall_4";
	protected static final String CATE_WALL_E = "wall_5";
	protected static final String CATE_WALL_F = "wall_6";
	protected static final String CATE_WALL_G = "wall_7";
	protected static final String CATE_WALL_H = "wall_8";
	
	protected static final String CATE_CHUCKA = "chuck_a";
	protected static final String CATE_CHUCKB = "chuck_b";
	protected static final String CATE_TOWER  = "tower-lamp";
	protected static final String CATE_BATTLE = "battle";
	
	protected static final String CATE_PIPE_A = "pipe2_1";
	protected static final String CATE_PIPE_B = "pipe2_2";
	protected static final String CATE_PIPE_C = "pipe2_3";
	protected static final String CATE_PIPE_D = "pipe2_4";
	protected static final String CATE_PIPE_E = "pipe2_5";
	protected static final String CATE_PIPE_F = "pipe2_6";
	protected static final String CATE_PIPE_G = "pipe3_1";
	protected static final String CATE_PIPE_H = "pipe3_2";
	protected static final String CATE_PIPE_I = "pipe3_3";
	protected static final String CATE_PIPE_J = "pipe3_4";
	protected static final String CATE_PIPE_K = "pipe4";
	
	protected static final String CATE_VALVE   = "valve_manual";
	protected static final String CATE_VALVE_BL= "valve_ball";
	
	protected static final String CATE_GAUGE   = "gauge";
	
	protected static final String CATE_M_PUMP  = "mp";
	protected static final String CATE_C_PUMP  = "cp";
	
	private static final String path = "/narl/itrc/res/tile/";
	
	protected void loadPumpTile(){
		
		faces.clear();
		
		final String[][] cate = {
			{CATE_TOWER  , "tower-1", "tower-2", "tower-3", "tower-4"},
			//--------------//
			{CATE_CHUCKA , "chuck-a1", "chuck-a2", "chuck-a3", "chuck-a4", "chuck-a5", "chuck-a6" },
			{CATE_CHUCKB , "chuck-b1", "chuck-b2", "chuck-b3", "chuck-b4", "chuck-b5", "chuck-b6" },
			//--------------//
			{CATE_WALL_A , "wall-a"},
			{CATE_WALL_B , "wall-b"},
			{CATE_WALL_C , "wall-c"},
			{CATE_WALL_D , "wall-d"},
			{CATE_WALL_E , "wall-e"},
			{CATE_WALL_F , "wall-f"},
			{CATE_WALL_G , "wall-g"},
			{CATE_WALL_H , "wall-h"},
			//--------------//
			{CATE_BATTLE , "battle"},
			//--------------//
			{CATE_PIPE_A, "pipe-a0", "pipe-a1"},
			{CATE_PIPE_B, "pipe-b0", "pipe-b1"},
			{CATE_PIPE_C, "pipe-c0", "pipe-c1"},
			{CATE_PIPE_D, "pipe-d0", "pipe-d1"},
			{CATE_PIPE_E, "pipe-e0", "pipe-e1"},
			{CATE_PIPE_F, "pipe-f0", "pipe-f1"},
			//--------------//
			{CATE_PIPE_G, "pipe-g0", "pipe-g1"},
			{CATE_PIPE_H, "pipe-h0", "pipe-h1"},
			{CATE_PIPE_I, "pipe-i0", "pipe-i1"},
			{CATE_PIPE_J, "pipe-j0", "pipe-j1"},
			{CATE_PIPE_K, "pipe-k0", "pipe-k1"},
			//--------------//
			{CATE_VALVE   , "valve-0", "valve-1"},
			{CATE_VALVE_BL, "valve-bl-0", "valve-bl-1"},
			{CATE_GAUGE   , "gauge" },
			//--------------//
			{CATE_M_PUMP , "MP-0", 
				           "MP-1", "MP-2", "MP-3", "MP-4",
				           "MP-5", "MP-6", "MP-7", "MP-8", 
			},
			{CATE_C_PUMP , "CP-0", 
		                   "CP-1", "CP-2", "CP-3", "CP-4",
		                   "CP-5", "CP-6", "CP-7", "CP-8", 
			},
		};
			
		for(int i=0; i<cate.length; i++){
			int cnt = cate[i].length - 1;
			Image[] imgs = new Image[cnt];
			for(int j=1; j<=cnt; j++){
				imgs[j-1] = new Image(WidDiagram.class.getResourceAsStream(path+cate[i][j]+".png"));
			}
			faces.put(cate[i][0],imgs);
		}		
	}
	//----------------------------------------//
}
