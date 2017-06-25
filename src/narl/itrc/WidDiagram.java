package narl.itrc;

import java.util.ArrayList;
import java.util.HashMap;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class WidDiagram extends StackPane {
	
	private static final int TILE_SIZE = 40;
	private static final int GRID_SIZE = TILE_SIZE/2;
	
	private double width=640., height=640.; 
	
	private Canvas blue_p = new Canvas();//show blueprint paper
	private Canvas ground = new Canvas();//draw background
	private Canvas effect = new Canvas();//draw animation	
	private Canvas cursor = new Canvas();//draw cursor
	
	private AnchorPane pegged = new AnchorPane();
	private HBox       toolbox= new HBox();
	
	private final GraphicsContext gc_effect = effect.getGraphicsContext2D();
	private final GraphicsContext gc_cursor = cursor.getGraphicsContext2D();
	
	public final BooleanProperty editMode = new SimpleBooleanProperty();
	
	public WidDiagram(){
		init();		
	}
	
	public WidDiagram(int width,int height){
		this.width = width;
		this.height= height;
		init();
	}
	
	private void init(){
		
		gc_effect.setFill(Color.TRANSPARENT);
		gc_effect.setStroke(Color.YELLOWGREEN);
		gc_effect.setLineWidth(2);
		
		gc_cursor.setFill(Color.TRANSPARENT);
		gc_cursor.setStroke(Color.YELLOWGREEN);
		gc_cursor.setLineWidth(2);
	
		watcher.setCycleCount(Timeline.INDEFINITE);
		
		blue_p.setWidth(width);
		blue_p.setHeight(height);
		ground.setWidth(width);
		ground.setHeight(height);
		effect.setWidth(width);
		effect.setHeight(height);
		cursor.setWidth(width);
		cursor.setHeight(height);
		
		pegged.setMaxSize(width, height);
		
		draw_blue_print();
		init_tool_box();
		
		//pegged.getStyleClass().add("decorate2-border");
		getStyleClass().add("pad-small");
		getChildren().addAll(blue_p,ground,effect,cursor,pegged);
		
		editMode(true);
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
			pegged.getChildren().add(toolbox);
			//setOnMouseMoved(eventEditMove);
			//setOnMouseClicked(eventEditClick);
		}else{
			watcher.play();
			pegged.getChildren().remove(toolbox);
			//setOnMouseMoved(null);			
			//setOnMouseClicked(eventUserClick);
		}
		blue_p.setVisible(flag);
	}
	//----------------------------------------//
	
	private EventHandler<MouseEvent> eventEditMove = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
		}
	};
	
	private EventHandler<MouseEvent> eventEditClick = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
		}
	};
	
	private EventHandler<MouseEvent> eventUserClick = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
		}
	};
	//----------------------------------------//
	
	public class Item {
		public String  name = "";
		public Image[] face = null;		
		public int[]   geom = {0,0,0,0};//x,y,width,height
		
		public int     makeupFlag = -1;//negative value means 'disable'
		
		private void makeup(){
			/*makeupFlag = makeupFlag % makeupPics.length;
			gc_effect.clearRect(
				geom[0], geom[1], 
				geom[2], geom[3]
			);
			gc_effect.drawImage(
				makeupPics[makeupFlag],
				geom[0], geom[1]
			);				
			makeupFlag++;*/
		}
	};

	/**
	 * This array keep 'all' items
	 */
	protected HashMap<String,Item> mapItem = new HashMap<String,Item>();
	
	/**
	 * This array keep items with 'makeup' attribute.<p>
	 */
	private ArrayList<Item> lstMakeup = new ArrayList<Item>();
	
	private final Timeline watcher = new Timeline(new KeyFrame(
		Duration.millis(200),
		event->{
			for(Item itm:lstMakeup){				
				if(itm.makeupFlag<0){
					continue;
				}
				itm.makeup();
			}
		}
	));
	//----------------------------------------//
	
	protected HashMap<String,Image[]> mapFace  = new HashMap<String,Image[]>();
	protected HashMap<String,Image[]> mapMakeup= new HashMap<String,Image[]>();

	protected static final String KEY_BLUEPRINT= "blueprint";
	protected static final String KEY_VALVE    = "valve";
	protected static final String KEY_MOTOR1   = "motor1";
	protected static final String KEY_MOTOR2   = "motor2";
	protected static final String KEY_WALL     = "wall";
	protected static final String KEY_VESSEL   = "vessel";
	
	private static final String path = "/narl/itrc/res/tile/";
	
	protected void loadPumpTile(){
		
		mapFace.clear();
		mapMakeup.clear();

		final String[][] name = {
			{KEY_BLUEPRINT, "blueprint.png"},
			{KEY_VALVE    ,  },
			{KEY_MOTOR1   ,  },
			{KEY_MOTOR2   ,  },
			{KEY_WALL     ,  },
			{KEY_VESSEL   ,  }
		};
			
		/*for(int i=0; i<name.length; i++){
			//Default picture format is "PNG"
			InputStream stm = WidDiagram.class.getResourceAsStream(path+name+".png");
			Image image = new Image(stm);
			mapFace.put(name[i], image);
		}
		
		final String[] ani_motor = {
			"ani-motor-1.png",
			"ani-motor-2.png",
			"ani-motor-3.png",
			"ani-motor-4.png",
			"ani-motor-5.png",
			"ani-motor-6.png",
			"ani-motor-7.png",
			"ani-motor-8.png",
		};
		Image[] img = load_image(ani_motor);*/
	}

	private Image[] load_image(String[] name){
		int cnt = name.length;
		Image[] img = new Image[cnt];
		for(int i=0; i<cnt; i++){
			img[i] = new Image(WidDiagram.class.getResourceAsStream(path+name));
		}
		return img;
	}	
}
