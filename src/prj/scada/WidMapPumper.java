package prj.scada;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import narl.itrc.Misc;

public class WidMapPumper extends StackPane {
	
	private Image[] middle = null;
	private Canvas  screen = new Canvas();//draw cursor
			
	public WidMapPumper(){
		
		final GraphicsContext gc = screen.getGraphicsContext2D();
		
		gc.setFill(Color.TRANSPARENT);
		gc.setStroke(Color.YELLOWGREEN);
		gc.setLineWidth(2);
		
		screen.setOnMouseMoved(event->{	
			gc.clearRect(
				0, 0, 
				screen.getWidth(), screen.getHeight()
			);//clear??
			for(Item itm:lst){
				if(itm.contains(event)==true){
					gc.strokeRoundRect(
						itm.geom[0], itm.geom[1], 
						itm.geom[2], itm.geom[3], 
						10, 10
					);
					return;
				}
			}
		});
		screen.setOnMouseClicked(event->{
			for(Item itm:lst){
				if(itm.contains(event)==true){
					itm.handle(null);
					return;
				}
			}
		});
		
		getStyleClass().add("pad-small");
	}
	
	public WidMapPumper(String name){
		this();
		build(Misc.pathSock,name);
	}
	
	private void build(String path,String name){
		
		getChildren().clear();
		
		//First, load the background...	
		ImageView ground = new ImageView();

		try {
			
			FileInputStream fs = new FileInputStream(path+name+".png");
			
			Image img = new Image(fs);
			
			ground.setImage(img);
			
			screen.setWidth(img.getWidth());
			screen.setHeight(img.getHeight());

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		
		//Second, load configure property, it contains item and location information...
		try {			
			
			FileInputStream stm = new FileInputStream(path+name+".inf");
			
			Properties prop = new Properties();
			
			prop.load(stm);
			
			int idx = 1;
			for(;;idx++){
				String txt = prop.getProperty(String.format("item%d",idx));
				if(txt==null){
					break;
				}
				txt = txt.replaceAll("\\s", "");
				lst.add(new Item(txt));
			}
			
			stm.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		getChildren().addAll(ground,screen);		
	}
	
	private static final int ITM_TYPE_VALVE = 1;
	private static final int ITM_TYPE_MOTOR = 2;
	private static final int ITM_TYPE_GAUGE = 3;
	
	private class Item implements EventHandler<ActionEvent> {
		
		public  int    type = 0;
		public  String name = "";
		public  int[]  geom = {0,0,0,0};//location and size
		private int[]  vtex = {0,0,0,0};//left,top,right,bottom
		
		public Item(String txt){
			//format: index = name, type, location, [addition]
			String[] arg = txt.split(",");
			name = arg[0];
			if(arg[1].equalsIgnoreCase("valve")==true){
				type = ITM_TYPE_VALVE;
			}else if(arg[1].equalsIgnoreCase("motor")==true){
				type = ITM_TYPE_MOTOR;
			}else if(arg[1].equalsIgnoreCase("gauge")==true){
				type = ITM_TYPE_GAUGE;
			}
			Misc.parseGeomInfo(arg[2], geom);
			vtex[0] = geom[0];//left
			vtex[1] = geom[1];//top
			vtex[2] = geom[0]+geom[2];//right
			vtex[3] = geom[1]+geom[3];//bottom
		}
		
		public boolean contains(MouseEvent e){
			int px = (int)e.getX();
			int py = (int)e.getY();
			return contains(px,py);
		}
		
		public boolean contains(int px, int py){
			if( vtex[0]<=px && px<=vtex[2] ){
				if( vtex[1]<=py && py<=vtex[3] ){
					return true;
				}
			}
			return false;
		}

		@Override
		public void handle(ActionEvent event) {
			
		}		
	};
	
	private ArrayList<Item> lst = new ArrayList<Item>(); 
}
