package prj.scada;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import javafx.animation.Animation.Status;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;

public class PID_Root extends AnchorPane {
	
	public PID_Root(){
		this("");		
	}
	
	public PID_Root(String name){
		PID_Leaf.initialize();
		if(name.length()!=0){
			load(name);//make diagram from XML file
		}
		looper.setCycleCount(Timeline.INDEFINITE);
		looper.play();
		
		//let user know we got focus~~~~
		/*setOnMouseEntered(event->{
			Scene sc = getScene();
			old_cursor = sc.getCursor();
			sc.setCursor(Cursor.HAND);
		});
		setOnMouseExited(event->{
			getScene().setCursor(old_cursor);;
		});*/
	}
	
	private Hashtable<Integer,PID_Leaf> map = new Hashtable<>();
	private ArrayList<PID_Leaf> lst = new ArrayList<>();
	
	private Timeline looper = new Timeline(new KeyFrame(
		Duration.millis(100),
		event->{
			//update all skin in the diagram~~~
			for(PID_Leaf itm:lst){
				itm.castSkin();
			}
		}
	));

	public PID_Leaf createLeaf(int type, int grid_x, int grid_y){
		return createLeaf(new PID_Leaf(type, grid_x, grid_y));
	}
	public PID_Leaf createLeaf(PID_Leaf itm){
		//check location is empty...
		int[] size = itm.getSize();
		for(int yy=size[1]; yy<=size[3]; yy++){
			for(int xx=size[0]; xx<=size[2]; xx++){
				if(map.get(gen_key(xx,yy))!=null){
					return null;
				}
			}
		}
		for(int yy=size[1]; yy<=size[3]; yy++){
			for(int xx=size[0]; xx<=size[2]; xx++){
				map.put(gen_key(xx,yy), itm);
			}
		}
		lst.add(itm);
		itm.setRoot(this);		
		return itm;
	}
	
	public void deleteLeaf(int grid_x, int grid_y){
		PID_Leaf itm = map.get(gen_key(grid_x,grid_y));
		if(itm==null){
			return;
		}
		deleteLeaf(itm);
	}
	public void deleteLeaf(PID_Leaf itm){
		int[] size = itm.getSize();
		for(int yy=size[1]; yy<=size[3]; yy++){
			for(int xx=size[0]; xx<=size[2]; xx++){
				map.remove(gen_key(xx,yy));
			}
		}
		lst.remove(itm);
		getChildren().remove(itm);
	}	
	
	public PID_Leaf selectLeaf(int mx, int my){
		return null;
	}
	public PID_Leaf selectLeaf(String name){
		return null;
	}
	public PID_Leaf selectLeaf(int idx){		
		return lst.get(idx);
	}
	public int leafSize(){
		return lst.size();
	}
	
	private int gen_key(int gx, int gy){
		return gx + gy * 1000;
	}
	
	private final String K_LEAF_CNT = "Count";
	private final String K_LEAF_FMT = "Leaf-%03d";
	
	public void save(String name){
		save(new File(name));
	}
	public void save(File fs){
		if(fs==null){
			return;
		}
		try {
			int cnt = lst.size();
			Properties prop = new Properties();
			prop.put(K_LEAF_CNT, String.valueOf(cnt));
			for(int i=0; i<cnt; i++){
				prop.put(
					String.format(K_LEAF_FMT, i), 
					PID_Leaf.flatten(lst.get(i))
				);
			}
			FileOutputStream stm = new FileOutputStream(fs);
			prop.storeToXML(stm, "_PID");
			stm.close();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	
	public void load(String name){
		load(new File(name));
	}
	public void load(File fs){
		if(fs==null){
			return;
		}
		FileInputStream stm;
		try {
			stm = new FileInputStream(fs);			
			Properties prop = new Properties();			
			prop.loadFromXML(stm);
			stm.close();
			int cnt = Integer.valueOf(prop.getProperty(K_LEAF_CNT,"0"));
			for(int i=0; i<cnt; i++){
				String key = String.format(K_LEAF_FMT, i);
				String txt = prop.getProperty(key,"");
				if(txt.length()==0){
					break;
				}
				createLeaf(PID_Leaf.unflatten(txt));
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	
	private PID_Pallet pallet = new PID_Pallet(this);
	
	public boolean isEditMode(){
		if(looper.getStatus()==Status.RUNNING){
			return false;
		}
		return true;
	}
	
	public void editMode(boolean flag){
		if(flag==false){
			pallet.stage().close();
			looper.play();
		}else{			
			pallet.appear();
			looper.stop();
		}
	}
}
