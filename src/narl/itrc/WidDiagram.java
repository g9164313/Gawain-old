package narl.itrc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class WidDiagram extends StackPane {
	
	private static final int TILE_SIZE = 40;
	private static final int GRID_SIZE = TILE_SIZE/2;
	
	private double width=640., height=640.; 
			
	private AnchorPane pegged = new AnchorPane();
	private Canvas ground = new Canvas();//draw background
	private Canvas effect = new Canvas();//draw animation	
	private Canvas cursor = new Canvas();//draw cursor
	
	private final GraphicsContext gc_effect = effect.getGraphicsContext2D();
	private final GraphicsContext gc_cursor = cursor.getGraphicsContext2D();
	
	public final BooleanProperty editMode = new SimpleBooleanProperty();
	
	public WidDiagram(){

		gc_effect.setFill(Color.TRANSPARENT);
		gc_effect.setStroke(Color.YELLOWGREEN);
		gc_effect.setLineWidth(2);
		gc_cursor.setFill(Color.TRANSPARENT);
		gc_cursor.setStroke(Color.YELLOWGREEN);
		gc_cursor.setLineWidth(2);
	
		watcher.setCycleCount(Timeline.INDEFINITE);
		
		//ground.setWidth(width);
		//ground.setHeight(height);
		//pegged.setMaxSize(maxWidth, maxHeight);
		//effect.widthProperty().bind(ground.widthProperty());
		//effect.heightProperty().bind(ground.heightProperty());
		//cursor.widthProperty().bind(ground.widthProperty());
		//cursor.heightProperty().bind(ground.heightProperty());
				
		//pegged.getStyleClass().add("decorate2-border");
		getStyleClass().add("pad-small");
		getChildren().addAll(ground,effect,pegged,cursor);
		
		editMode(false);
	}
	
	public void editMode(boolean flag){
		if(flag==true){
			watcher.pause();
			setOnMouseMoved(eventEditMove);
			setOnMouseClicked(eventEditClick);
		}else{
			watcher.play();
			setOnMouseMoved(null);			
			setOnMouseClicked(eventItemClick);
		}
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
	
	private EventHandler<MouseEvent> eventItemClick = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
		}
	};
	//----------------------------------------//
	
	public class Item {
		public String name = "";
		public Image  face = null;		
		public int[]  geom = {0,0,0,0};//x,y,width,height
		
		public Image[] makeupPics = null;//for animation
		public int     makeupFlag = -1;//negative value means 'disable'
		
		private void makeup(){
			makeupFlag = makeupFlag % makeupPics.length;
			gc_effect.clearRect(
				geom[0], geom[1], 
				geom[2], geom[3]
			);
			gc_effect.drawImage(
				makeupPics[makeupFlag],
				geom[0], geom[1]
			);				
			makeupFlag++;
		}
	};

	protected HashMap<String,Item> mapItem = new HashMap<String,Item>();
	
	
	
	/**
	 * Array keep items with 'makeup' attribute.<p>
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
	
	protected HashMap<String,Image>   mapFace  = new HashMap<String,Image>();
	protected HashMap<String,Image[]> mapMakeup= new HashMap<String,Image[]>();

	protected static final String IMG_BLUEPRINT= "blueprint";
	protected static final String IMG_VALVE    = "valve";
	protected static final String IMG_MOTOR_1  = "motor-1";
	protected static final String IMG_MOTOR_2  = "motor-2";
	protected static final String IMG_WALL     = "wall";
	protected static final String IMG_VESSEL   = "vessel";
	
	private static final String path = "/narl/itrc/res/tile/";
	
	protected void loadPumpTile(){
		
		final String[] name = {
			IMG_BLUEPRINT,
			IMG_VALVE,
			IMG_MOTOR_1,
			IMG_MOTOR_2,
			IMG_WALL,
			IMG_VESSEL,
		};		
		load_tile(name);
	}
	
	private void load_tile(String[] name){
		
		mapFace.clear();
				
		for(int i=0; i<name.length; i++){
			//Default picture format is "PNG"
			InputStream stm = WidDiagram.class.getResourceAsStream(path+name+".png");
			Image image = new Image(stm);
			mapFace.put(name[i], image);
		}
	}
}
