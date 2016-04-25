package narl.itrc;

import java.io.ByteArrayInputStream;

import com.sun.glass.ui.Application;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public abstract class CamBundle implements Gawain.EventHook {

	public CamBundle(){
		Gawain.hook(this);		
	}

	@Override
	public void shutdown() {
		close();
	}
	
	public void showPanel(Window owner){
		PanBase pan = getPanelSetting();
		if(pan==null){
			return;
		}
		Stage stg = new Stage(StageStyle.UNIFIED);
		stg.initModality(Modality.NONE); 
		stg.setResizable(false);
		stg.centerOnScreen();
		stg.setTitle("設定相機參數");
		stg.initOwner(owner);
		pan.appear(stg);
	}
	public abstract PanBase getPanelSetting();

	private Thread thrSetup;
	public void asynSetup(int idx, String configName){
		if(thrSetup!=null){
			if(thrSetup.isAlive()==true){
				return;
			}
		}		
		thrSetup = new Thread(new Runnable(){
			@Override
			public void run() {
				setup(idx,configName);
			}
		},"cam-setup");
	}
	
	private long ptrCntx = 0;//point to a container for whatever devices~~~
	private long[] ptrMatx = new long[16];//point to Mat, the first is source layer, the second is  

	public SimpleBooleanProperty optEnbl = new SimpleBooleanProperty(false);
	public SimpleStringProperty msgLast = new SimpleStringProperty("");

	public abstract void setup(int idx,String configName);
	public abstract void fetch();
	public abstract void close();
	
	public void updateOptEnbl(boolean val){
		//callback by native instance~~		
		Application.invokeAndWait(new Runnable(){
			@Override
			public void run() {
				optEnbl.set(val);
			}
		});
	}
	
	public void updateMsgLast(String txt){
		//callback by native instance~~
		Application.invokeAndWait(new Runnable(){
			@Override
			public void run() {
				msgLast.set(txt);
			}
		});
	}
	
	public Image getImage(){
		return getImage(0);
	}
	
	public Image getImage(int idx){
		if(idx>=ptrMatx.length){
			return null;
		}
		byte[] dat = getData(ptrMatx[idx]);
		if(dat==null){
			return null;
		}
		return new Image(new ByteArrayInputStream(dat));
	}
	
	private native byte[] getData(long ptr);
}


