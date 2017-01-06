package narl.itrc.vision;

import com.jfoenix.controls.JFXProgressBar;
import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

/**
 * 'Blk' is the abbreviation of "Bulk".<p>
 * This object collects many frames and bundles it into a huge file.<p>
 * This object should also support to 'view' huge image.<p>
 * Attention!! 
 * P.S: Which method should we use? memory-mapping IO or RAM-disk file.<p>  
 * @author qq
 *
 */
public class BlkRender extends BorderPane implements Gawain.EventHook {

	public CamBundle bundle;
	
	public BlkRender(CamBundle bnd){
		Gawain.hook(this);
		bundle = bnd;
		init_layout();
	}

	@Override
	public void release() {
		if(looper!=null){
			if(looper.isDone()==false){
				looper.cancel();
			}
		}
	}

	@Override
	public void shutdown() {
		bundle.close();
	}
	//--------------------------------------------//
	
	private final String TXT_START = "開始";
	private final String TXT_CANCEL= "取消";
	
	private Button actLaunch;
	
	private ProgressBar actProgress;
	
	private PanBase.FlatIcon actSetting;
	
	private void init_layout(){
		HBox lay = new HBox();
		lay.getStyleClass().add("hbox-medium");
		lay.setAlignment(Pos.CENTER_LEFT);
		lay.setPrefWidth(300);
		
		actLaunch = PanBase.genButton1(TXT_START,null);
		actLaunch.setPrefWidth(60);
		actLaunch.setOnAction(event->action());
		
		actProgress = new ProgressBar();
		actProgress.setProgress(0.);
		actProgress.setPrefHeight(27.);
		actProgress.prefWidthProperty().bind(lay.widthProperty().subtract(100));

		actSetting = new PanBase.FlatIcon("");
		
		lay.getChildren().addAll(actLaunch,actProgress);
		setTop(lay);
	}
	//--------------------------------------------//
	
	private long blkPoint;
	
	private int blkCount = 100;
	
	private Task<Integer> looper = null;

	private Runnable eventShowData = new Runnable(){
		@Override
		public void run() {
			actLaunch.setText(TXT_START);//for next turn~~~
			//do something~~~~
		}
	};
	
	public void action(){
		if(looper!=null){
			if(looper.isDone()==false){
				if(looper.cancel()==true){
					actLaunch.setText(TXT_START);//for next turn~~~
					return;
				}
				PanBase.notifyError("內部錯誤","無法取消工作");//show message~~~~
				return;
			}			
		}
		actLaunch.setText(TXT_CANCEL);
		
		looper = new Task<Integer>(){
			private int blkIndex = 0;
			@Override
			protected Integer call() throws Exception {
				for(int i=0; i<blkCount; i++){
					updateProgress(++blkIndex, blkCount);
					Thread.sleep(100);
				}
				Application.invokeAndWait(eventShowData);
				return 0;
			}
		};
		actProgress.progressProperty().bind(looper.progressProperty());
		new Thread(looper,"BlkRender").start();
	}
}
