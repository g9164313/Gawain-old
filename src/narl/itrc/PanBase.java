package narl.itrc;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public abstract class PanBase {

	protected String panTitle = "::scene::";

	private Scene scene=null;
	private Stage owner=null;
	
	public abstract Parent layout();
	
	public PanBase(){	   
	}
	
	public PanBase(String txt){
		setTitle(txt);
	}
	
	public void setTitle(String txt){
		panTitle = txt;
	}
	
	public Scene getScene(){ 
		return scene;
	}
	
	public Stage getOwner(){ 
		return owner;
	}
	
	public void genDlgOwner(Window parent){
		owner = new Stage(StageStyle.UNIFIED);		
		owner.initModality(Modality.WINDOW_MODAL); 
		owner.initOwner(parent);
		owner.setResizable(false);		
		init(owner);
	}
	
	public void appear(Stage stg){
		owner = stg;
		init(stg);		
		appear();
	}
	public void appear(){
		if(owner==null){
			return;
		}
		owner.show();
	}
	
	public void standby(Stage stg){
		owner = stg;
		init(stg);
		standby();
	}
	public void standby(){
		if(owner==null){
			return;
		}
		owner.showAndWait();
	}
	
	public void dismiss(){
		if(owner==null){
			return;
		}
		owner.close();
	}
	
	private void init(Stage stg){		
		init_scene();
		//check whether we need to hook event~~~
		if(stg.getOnShowing()==null){
			stg.setOnShowing(eventWinHandle);
		}
		if(stg.getOnShown()==null){
			stg.setOnShown(eventWinHandle);
		}
		if(stg.getOnCloseRequest()==null){
			stg.setOnCloseRequest(eventWinHandle);
		}
		//set title and some properties~~~
		stg.setTitle(panTitle);
		stg.setScene(scene);
		stg.sizeToScene();
	}
		
	private void init_scene(){
		if(scene!=null){
			return;
		}
		//first initialization...
		scene = new Scene(layout());
		scene.getStylesheets().add(Gawain.class.getResource("res/style.css").toExternalForm());
	}
	
	protected void eventShowing(WindowEvent event){ }
	protected void eventShown(WindowEvent event){ }
	protected void eventWatch(int cnt){ }
	protected void eventClose(WindowEvent event){ }
	
	private EventHandler<WindowEvent> eventWinHandle = new EventHandler<WindowEvent>(){
		@Override
		public void handle(WindowEvent event) {
			//if stage have no handle, direct event to here!!!
			if(WindowEvent.WINDOW_SHOWING==event.getEventType()){
				eventShowing(event);
			}else if(WindowEvent.WINDOW_SHOWN==event.getEventType()){
				eventShown(event);
			}else if(WindowEvent.WINDOW_CLOSE_REQUEST==event.getEventType()){
				watchStop();
				eventClose(event);
			}
		}
	};
	//------------------------//
	private int watchCount = 0;
	
	/*private ScheduledExecutorService watch = null;
	protected int getWatchCount(){
		return watchCount;
	}
	protected void watchStart(int ms){
		if(watch==null){
			watch = Executors.newScheduledThreadPool(1);
		}
		watchCount = 0;//reset it~~~
		final Runnable tsk = new Runnable(){
			@Override
			public void run() {
				watchCount++;
				eventWatch(watchCount);
			}
		};
		watch.scheduleAtFixedRate(tsk,0,ms,TimeUnit.MILLISECONDS);
	}
	protected void watchStop(){		
		if(watch!=null){
			if(watch.isShutdown()==false){
				watch.shutdown();
				watch = null;
			}
		}
		watchCount = 0;
	}*/
	
	private Timeline watch = null;	
	private EventHandler<ActionEvent> eventWatchHandle = 
		new EventHandler<ActionEvent>()
	{
		@Override
		public void handle(ActionEvent event) {
			eventWatch(watchCount);
			watchCount++;
		}
	};	
	protected void watchStart(int ms){
		watchCount = 0;//reset this counter~~~
		watch = new Timeline(new KeyFrame(
			Duration.millis(ms),
			eventWatchHandle
		));
		watch.setCycleCount(Timeline.INDEFINITE);
		watch.play();
	}	
	protected void watchStop(){
		if(watch!=null){
			watch.stop();
			watch = null;
		}		
	}
	//------------------------//

	public GridPane genGridPack(int stride,Pane root,Node... lstND){
		
		GridPane pan = new GridPane();
		pan.getStyleClass().add("grid-small");
		
		if(root!=null){
			pan.prefWidthProperty().bind(root.widthProperty().divide(stride));
		}
		
		int col=0, row=0;
		for(int i=0; i<lstND.length; i++){			
			pan.add(lstND[i],col,row);
			pan.setAlignment(Pos.TOP_LEFT);
			if(i%stride==(stride-1)){
				row++;
				col=0;
			}else{
				col++;
			}
		}
		return pan;
	}
	//------------------------//
	
	public static Pane decorate(String txt,Node content){
		
		Label title = new Label(" "+txt+" ");
		
		title.getStyleClass().add("group-title");
		StackPane.setAlignment(title,Pos.TOP_LEFT);

		StackPane body = new StackPane();
		content.getStyleClass().add("group-content");
		body.getChildren().add(content);
		
		StackPane grp = new StackPane();
		grp.getStyleClass().add("group-border");
		grp.getChildren().addAll(title,body);
		
		return grp;
	}
}

