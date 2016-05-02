package narl.itrc;

import eu.hansolo.enzo.notification.Notification;
import eu.hansolo.enzo.notification.NotifierBuilder;
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

	protected String panTitle = "::panel::";

	private Scene scene=null;
	private Stage stage=null;
	
	public abstract Parent layout();
	
	public PanBase(){
		initMsgBox();
	}
	
	public PanBase(String txt){
		setTitle(txt);
		initMsgBox();
	}
	
	public void setTitle(String txt){
		panTitle = txt;
	}
	
	public Scene getScene(){ 
		return scene;
	}
	
	public Stage getOwner(){ 
		return stage;
	}
	
	public void makeDialog(Window parent){
		stage = new Stage(StageStyle.UNIFIED);		
		stage.initModality(Modality.WINDOW_MODAL); 
		stage.initOwner(parent);
		stage.setResizable(false);
		stage.centerOnScreen();
		init(stage);
	}
	
	public void makeStage(Window parent){
		stage = new Stage(StageStyle.UNIFIED);		
		stage.initModality(Modality.NONE); 
		stage.initOwner(parent);
		stage.centerOnScreen();
		init(stage);
	}
	
	public void appear(Stage stg){
		stage = stg;
		init(stg);		
		appear();
	}
	public void appear(){
		if(stage==null){
			makeStage(null);
		}
		stage.show();
	}
	
	public void standby(Stage stg){
		stage = stg;
		init(stg);
		standby();
	}
	public void standby(){
		if(stage==null){
			makeStage(null);
		}
		stage.showAndWait();
	}
	
	public void dismiss(){
		if(stage==null){
			return;
		}
		stage.close();
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
	
	public static Notification.Notifier msgBox = null;
	
	private void initMsgBox(){
		if(msgBox!=null){
			//we already had message box~~~
			return;
		}
		msgBox = NotifierBuilder.create()
			.popupLocation(Pos.CENTER)
			.popupLifeTime(Duration.millis(1000))
			.build();
		final Gawain.EventHook event = new Gawain.EventHook(){
			@Override
			public void shutdown() {
				msgBox.stop();//this widget must be stop.
			}		
		};
		Gawain.hook(event);
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

