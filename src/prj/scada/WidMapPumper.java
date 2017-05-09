package prj.scada;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import com.sun.glass.ui.Application;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import narl.itrc.Misc;


public class WidMapPumper extends StackPane {
	
	private Canvas effect = new Canvas();//draw effect or animation
	private Canvas cursor = new Canvas();//draw cursor

	private final GraphicsContext g_effect = effect.getGraphicsContext2D();
	private final GraphicsContext g_cursor = cursor.getGraphicsContext2D();
	
	public WidMapPumper(){
		
		g_effect.setFill(Color.TRANSPARENT);
		g_effect.setStroke(Color.YELLOWGREEN);
		g_effect.setLineWidth(2);
		
		g_cursor.setFill(Color.TRANSPARENT);
		g_cursor.setStroke(Color.YELLOWGREEN);
		g_cursor.setLineWidth(2);
		
		cursor.setOnMouseMoved(event->{
			
			g_cursor.clearRect(
				0, 0, 
				cursor.getWidth(), cursor.getHeight()
			);
			Iterator<Item> itor = lst.values().iterator();
			while(itor.hasNext()==true){
				Item itm = itor.next();
				if(itm.contains(event)==true){
					g_cursor.strokeRoundRect(
						itm.geom[0], itm.geom[1], 
						itm.geom[2], itm.geom[3], 
						10, 10
					);
					break;
				}
			}
		});
		cursor.setOnMouseClicked(event->{
			
			Iterator<Item> itor = lst.values().iterator();
			while(itor.hasNext()==true){
				Item itm = itor.next();
				if(itm.contains(event)==true){
					doItemTask(itm);
					break;
				}
			}
		});
		
		Timeline watcher = new Timeline(new KeyFrame(
			Duration.millis(200),
			event->{
				Iterator<Item> itor = lst.values().iterator();
				while(itor.hasNext()==true){
					Item itm = itor.next();
					g_effect.clearRect(
						itm.geom[0], itm.geom[1], 
						itm.geom[2], itm.geom[3]
					);
					if(itm.lifeFlag==true){						
						switch(itm.type){
						case TYP_VALVE:
							g_effect.drawImage(
								ANI_VALVE[0], 
								itm.geom[0], itm.geom[1]
							);
							break;
						case TYP_MOTOR:
							int idx = itm.lifeTick%ANI_MOTOR.length;
							g_effect.drawImage(
								ANI_MOTOR[idx], 
								itm.geom[0], itm.geom[1]
							);
							break;
						}						
						itm.lifeTick++;
					}
				}
			}
		));
		watcher.setCycleCount(Timeline.INDEFINITE);
		watcher.play();
		
		getStyleClass().add("pad-small");
	}
	
	public WidMapPumper(String name){
		this();
		build(Misc.pathSock,name);
	}

	private void build(String path,String name){
		
		getChildren().clear();
		
		//First, load the background...	
		try {
			ImageView ground = new ImageView();
			FileInputStream fs = new FileInputStream(path+name+".png");			
			Image img = new Image(fs);
			ground.setImage(img);
			getChildren().add(ground);
			
			effect.setWidth(img.getWidth());
			effect.setHeight(img.getHeight());
			cursor.setWidth(img.getWidth());
			cursor.setHeight(img.getHeight());
			
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		
		
		//Second, load configure property, it contains item and location information...
		try {			
			FileInputStream stm = new FileInputStream(path+name+".inf");
			Properties prop = new Properties();
			prop.load(stm);			
			check_key("valve",prop);
			check_key("motor",prop);
			check_key("gauge",prop);
			stm.close();			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		getChildren().addAll(effect,cursor);
	}
	
	private void check_key(String type,Properties prop){
		
		for(int idx=1; ;idx++){
			String key = String.format("%s%d", type, idx);
			String val = prop.getProperty(key);
			if(val==null){
				return;
			}
			val = val.replaceAll("\\s", "");
			
			Item itm = new Item(type, val.split(","));
			if(itm.type==TYP_NONE){
				Misc.logw("Unknown item?? - %s",val);
				continue;
			}
			if(itm.surface!=null){
				getChildren().add(itm.surface);
			}
			lst.put(key,itm);
		}
	}

	private static final int TYP_NONE  = -1;//it means this item is invalid!!!
	private static final int TYP_VALVE = 0x10;
	private static final int TYP_MOTOR = 0x20;
	private static final int TYP_GAUGE = 0x30;
	private static final int TYP_OTHER = 0xF0;
	
	public static class Item {
		
		public  String name = "";
		public  int    type = TYP_NONE;
		private int[]  geom = {0,0,0,0};//location and size
		private int[]  vtex = {0,0,0,0};//left,top,right,bottom
		
		private int lifeTick = 0;//always increase it for GUI animation
		private boolean lifeFlag = false;
		
		private ItemHook hook = null;
		
		private ImageView surface = null;

		public Item(String key,String[] arg){
			//format: type-index = name, location, [addition]
			name = arg[0];
			if(key.equalsIgnoreCase("valve")==true){
				set_loca(arg[1],40,40);
				type = TYP_VALVE;
				if(arg.length>=3){
					Image img = Misc.getFileImage(Misc.pathSock+arg[2]);
					surface = new ImageView(img);
					surface.setVisible(lifeFlag);
				}
			}else if(key.equalsIgnoreCase("motor")==true){
				set_loca(arg[1],40,40);				
				type = TYP_MOTOR;
				
			}else if(key.equalsIgnoreCase("gauge")==true){	
				type = TYP_GAUGE;
			}else{
				return;
			}			
			vtex[0] = geom[0];//left
			vtex[1] = geom[1];//top
			vtex[2] = geom[0]+geom[2];//right
			vtex[3] = geom[1]+geom[3];//bottom
		}
		
		private boolean set_loca(String arg, int width, int height){
			String[] loca = arg.split("_");			
			geom[0] = Misc.txt2int(loca[0]);
			geom[1] = Misc.txt2int(loca[1]);
			geom[2] = width; 
			geom[3] = height;
			return true;
		}
		
		private boolean contains(MouseEvent e){
			int px = (int)e.getX();
			int py = (int)e.getY();
			return contains(px,py);
		}
		
		private boolean contains(int px, int py){
			if( vtex[0]<=px && px<=vtex[2] ){
				if( vtex[1]<=py && py<=vtex[3] ){
					return true;
				}
			}
			return false;
		}
		
		private void eventUpdate(){
			lifeFlag = !lifeFlag;
			if(surface!=null){
				surface.setVisible(lifeFlag);
			}
			
			/*switch(type){
			case TYP_VALVE:
				if(surface!=null){
					boolean flag = surface.visibleProperty().get();
					surface.setVisible(!flag);
				}
				break;
			case TYP_MOTOR:
				break;
			case TYP_GAUGE:
				break;
			}*/
			//draw  screen or show state!!!
		}
	};
	
	public static interface ItemHook {
		void handle(Item itm);
	};
	
	private Hashtable<String,Item> lst = new Hashtable<String,Item>();
	
	private void doItemTask(Item itm){
		
		if(itm.hook!=null){
			itm.hook.handle(itm);
		}		
		if(Application.isEventThread()==true){
			itm.eventUpdate();			
		}else{
			final Runnable node = new Runnable(){
				@Override
				public void run() {
					itm.eventUpdate();
				}
			};
			Application.invokeAndWait(node);
		}
	}
	
	public void doTask(String name){
		
		Item itm = lst.get(name);
		if(itm==null){
			return;
		}
		doItemTask(itm);
	}
	
	public void hookWith(String name,ItemHook item){
		
		Item itm = lst.get(name);
		if(itm==null){
			return;
		}
		itm.hook = item;
	}

	private static final String IMG_DIR = "/narl/itrc/res/pumper/";
	
	private static final Image[] ANI_VALVE = {
		new Image(WidMapPumper.class.getResourceAsStream(IMG_DIR+"valve-1.png")),
	};
	
	private static final Image[] ANI_MOTOR = {
		new Image(WidMapPumper.class.getResourceAsStream(IMG_DIR+"motor-1.png")),
		new Image(WidMapPumper.class.getResourceAsStream(IMG_DIR+"motor-2.png")),
		new Image(WidMapPumper.class.getResourceAsStream(IMG_DIR+"motor-3.png")),
		new Image(WidMapPumper.class.getResourceAsStream(IMG_DIR+"motor-4.png")),
		new Image(WidMapPumper.class.getResourceAsStream(IMG_DIR+"motor-5.png")),
		new Image(WidMapPumper.class.getResourceAsStream(IMG_DIR+"motor-6.png")),
		new Image(WidMapPumper.class.getResourceAsStream(IMG_DIR+"motor-7.png")),
		new Image(WidMapPumper.class.getResourceAsStream(IMG_DIR+"motor-8.png")),
	};
}
