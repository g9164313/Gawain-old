package prj.scada;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import com.sun.glass.ui.Application;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.Gauge.SkinType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import narl.itrc.Misc;


public class WidMapPumper1 extends StackPane {
	
	private AnchorPane pegged = new AnchorPane();
	private Canvas effect = new Canvas();//draw effect or animation	
	private Canvas cursor = new Canvas();//draw cursor

	private final GraphicsContext g_effect = effect.getGraphicsContext2D();
	private final GraphicsContext g_cursor = cursor.getGraphicsContext2D();
	
	public WidMapPumper1(){
		
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
			Iterator<Item> itor = mapItem.values().iterator();
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
			
			Iterator<Item> itor = mapItem.values().iterator();
			while(itor.hasNext()==true){
				Item itm = itor.next();
				if(itm.contains(event)==true){
					doItemTask(itm);
					break;
				}
			}
		});
		
		final Timeline watcher = new Timeline(new KeyFrame(
			Duration.millis(200),
			event->{
				Iterator<Item> itor = mapItem.values().iterator();
				while(itor.hasNext()==true){
					Item itm = itor.next();
					g_effect.clearRect(
						itm.geom[0], itm.geom[1], 
						itm.geom[2], itm.geom[3]
					);
					if(itm.lifeFlag==true){
						int idx = itm.lifeTick%ANI_MOTOR.length;						
						switch(itm.type){
						case TYP_VALVE:
							g_effect.drawImage(
								ANI_VALVE[0], 
								itm.geom[0], itm.geom[1]
							);
							break;
						case TYP_MOTOR:
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
		
		//pegged.getStyleClass().add("decorate2-border");
		getStyleClass().add("pad-small");
	}
	
	public WidMapPumper1(String name){
		this();
		build(Misc.pathSock,name);
	}

	private void build(String path,String name){
		
		mapItem.clear();		
		lstGauge.clear();
		getChildren().clear();
		
		//First, load the background...	
		try {
			ImageView ground = new ImageView();
			FileInputStream fs = new FileInputStream(path+name+".png");			
			Image img = new Image(fs);
			ground.setImage(img);
			getChildren().add(ground);
			double iw = img.getWidth();
			double ih = img.getHeight();
			pegged.setMaxSize(iw, ih);
			effect.setWidth(iw);
			effect.setHeight(ih);
			cursor.setWidth(iw);
			cursor.setHeight(ih);
			getChildren().add(effect);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return;
		}

		//Second, load configure property, it contains item and location information...
		try {			
			FileInputStream stm = new FileInputStream(path+name+".inf");
			Properties prop = new Properties();
			prop.load(stm);			
			check_key("valve",prop,null);
			check_key("motor",prop,null);
			check_key("gauge",prop,lstGauge);
			for(Item itm:lstGauge){				
				Label txt = new Label();
				txt.textProperty().bind(itm.propInf);
				txt.setStyle("-fx-font-size: 23px;");
				AnchorPane.setLeftAnchor(txt, (double)itm.geom[0]);
				AnchorPane.setTopAnchor (txt, (double)itm.geom[1]-30);
				pegged.getChildren().add(txt);
			}
			stm.close();			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		getChildren().addAll(pegged,cursor);
	}
	
	private void check_key(String type,Properties prop,ArrayList<Item> lst){
		
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
			mapItem.put(itm.name,itm);
			if(lst!=null){
				lst.add(itm);
			}
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
		private int[]  vtex = {0,0,0,0};//left,right,top,bottom
		
		private int lifeTick = 0;//always increase it for GUI animation
		private boolean lifeFlag = false;
		
		private ItemHook hook = null;
		
		private ImageView surface = null;

		public double value = 0.f;
		public String unit = "";
		
		private DoubleProperty propVal = new SimpleDoubleProperty();
		private StringProperty propInf = new SimpleStringProperty();
		
		public Item(String key,String[] arg){
			//format: type-index = name, location, [addition]
			name = arg[0];
			set_loca(arg[1]);
			geom[2] = geom[3] = 40;
			
			if(key.equalsIgnoreCase("valve")==true){				
				type = TYP_VALVE;
				if(arg.length>=3){
					Image img = Misc.getFileImage(Misc.pathSock+arg[2]);
					surface = new ImageView(img);
					surface.setVisible(lifeFlag);
				}
			}else if(key.equalsIgnoreCase("motor")==true){		
				type = TYP_MOTOR;
			}else if(key.equalsIgnoreCase("gauge")==true){
				type = TYP_GAUGE;				
				unit = arg[2];
				propInf.setValue(String.format("%.3f %s", value, unit));
			}else{
				return;
			}
			vtex[0] = geom[0];//left
			vtex[1] = geom[0]+geom[2];//right
			vtex[2] = geom[1];//top
			vtex[3] = geom[1]+geom[3];//bottom
		}
		
		private boolean set_loca(String arg){
			String[] loca = arg.split("_");			
			geom[0] = Misc.txt2int(loca[0]);
			geom[1] = Misc.txt2int(loca[1]);
			return true;
		}
		
		private boolean contains(MouseEvent e){
			int px = (int)e.getX();
			int py = (int)e.getY();
			return contains(px,py);
		}
		
		private boolean contains(int px, int py){
			if( vtex[0]<=px && px<=vtex[1] ){
				if( vtex[2]<=py && py<=vtex[3] ){
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
			switch(type){
			case TYP_GAUGE:
				propVal.setValue(value);
				propInf.setValue(String.format("%.3f %s", value, unit));
				break;			
			}
			//draw  screen or show state!!!
		}
	};
	
	public static interface ItemHook {
		void handle(Item itm);
	};
	
	private ArrayList<Item> lstGauge = new ArrayList<Item>();
	
	private Hashtable<String,Item> mapItem = new Hashtable<String,Item>();
	
	public void doItemTask(Item itm){
		
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
		Item itm = mapItem.get(name);
		if(itm==null){
			Misc.loge("no item - (%s)",name);
			return;
		}
		doItemTask(itm);
	}

	public void hookWith(String name,ItemHook item){		
		Item itm = mapItem.get(name);
		if(itm==null){
			Misc.loge("no item - (%s)",name);
			return;
		}
		itm.hook = item;
	}
	
	/**
	 * refresh all gauges' data value.
	 */
	public void refresh(){
		
		if(Application.isEventThread()==true){
			for(Item itm:lstGauge){
				if(itm.hook==null){
					continue;
				}
				itm.hook.handle(itm);
				itm.eventUpdate();
			}			
		}else{
			for(Item itm:lstGauge){				
				if(itm.hook==null){
					continue;
				}				
				itm.hook.handle(itm);
				final Runnable node = new Runnable(){
					@Override
					public void run() {
						itm.eventUpdate();						
					}					
				};
				Application.invokeAndWait(node);
			}
		}
	}
	
	public Gauge[] createGauge(){
		int cnt = lstGauge.size();
		if(cnt==0){
			return null;
		}
		Gauge[] lst = new Gauge[cnt];
		for(int i=0; i<cnt; i++){
			Item itm = lstGauge.get(i);
			lst[i] = GaugeBuilder.create()
				.skinType(SkinType.DASHBOARD)
				.animated(true)
				.title(itm.name)
				.unit(itm.unit)
				.minValue(1)
				.maxValue(100)
				.build();
			lst[i].valueProperty().bind(itm.propVal);
		}
		return lst;
	}
	
	private static final String IMG_DIR = "/narl/itrc/res/tile/";
	
	private static final Image[] ANI_VALVE = {
		new Image(WidMapPumper1.class.getResourceAsStream(IMG_DIR+"ani-valve-1.png")),
	};
	
	private static final Image[] ANI_MOTOR = {
		new Image(WidMapPumper1.class.getResourceAsStream(IMG_DIR+"ani-motor-1.png")),
		new Image(WidMapPumper1.class.getResourceAsStream(IMG_DIR+"ani-motor-2.png")),
		new Image(WidMapPumper1.class.getResourceAsStream(IMG_DIR+"ani-motor-3.png")),
		new Image(WidMapPumper1.class.getResourceAsStream(IMG_DIR+"ani-motor-4.png")),
		new Image(WidMapPumper1.class.getResourceAsStream(IMG_DIR+"ani-motor-5.png")),
		new Image(WidMapPumper1.class.getResourceAsStream(IMG_DIR+"ani-motor-6.png")),
		new Image(WidMapPumper1.class.getResourceAsStream(IMG_DIR+"ani-motor-7.png")),
		new Image(WidMapPumper1.class.getResourceAsStream(IMG_DIR+"ani-motor-8.png")),
	};
}
