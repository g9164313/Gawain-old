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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public abstract class PanBase {

	protected String title = "::panel::";
	
	protected static final int FIRST_NONE = 0;
	protected static final int FIRST_FULLSCREEN = 1;
	protected static final int FIRST_MAXIMIZED = 2;
	protected int firstAction = FIRST_NONE;
	
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
		title = txt;
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
		if(stage.isShowing()==true){
			return;
		}
		doFirstAction();
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
		doFirstAction();
		stage.showAndWait();
	}
	
	private void doFirstAction(){
		switch(firstAction){
		case FIRST_FULLSCREEN:
			stage.setFullScreen(true);
			break;
		case FIRST_MAXIMIZED:
			stage.setMaximized(true);
			break;
		}
	}
	
	/*protected void maximize(){
		Screen scr = Screen.getPrimary();
		Rectangle2D bnd = scr.getVisualBounds();
		stage.setX(bnd.getMinX());
		stage.setY(bnd.getMinY());
		stage.setWidth(bnd.getWidth());
		stage.setHeight(bnd.getHeight());
	}*/
	
	public void dismiss(){
		if(stage==null){
			return;
		}
		stage.close();
		stage = null;
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
		stg.setTitle(title);
		stg.setScene(scene);
		stg.sizeToScene();
	}
		
	private void init_scene(){
		if(scene!=null){
			return;
		}
		//first initialization...
		scene = new Scene(layout());
		scene.getStylesheets().add(Gawain.class.getResource("res/styles.css").toExternalForm());
	}

	protected void eventShowing(WindowEvent e){
	}
	protected void eventShown(WindowEvent e){
	}
	protected void eventWatch(int cnt){
	}
	protected void eventClose(WindowEvent e){
		msgBox.stop();//user must close this message box
	}
	
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
			.popupLifeTime(Duration.millis(1500))
			.build();
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

	public static Pane decorate(String txt,Node cntxt){
		
		Label title = new Label(" "+txt);
		title.getStyleClass().add("group-title");
		cntxt.getStyleClass().add("group-content");
		StackPane.setAlignment(title,Pos.TOP_LEFT);
		StackPane.setAlignment(cntxt,Pos.BOTTOM_LEFT);
				
		//StackPane body = new StackPane();
		//body.getChildren().add(cntxt);
		
		StackPane grp = new StackPane();
		grp.getStyleClass().add("group-border");
		grp.getChildren().addAll(title,cntxt);
		return grp;
	}
	
	public static HBox decorateHBox(Object... args){
		HBox lay = new HBox();
		for(int i=0; i<args.length; i+=2){
			String title = (String)(args[i+0]);
			Node cntxt = (Node)(args[i+1]);
			Pane panel = decorate(title,cntxt);
			HBox.setHgrow(panel,Priority.ALWAYS);
			lay.getChildren().add(panel);
		}
		return lay;
	}
}

