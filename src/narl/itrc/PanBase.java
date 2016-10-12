package narl.itrc;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;
import com.sun.glass.ui.Application;

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
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public abstract class PanBase {

	protected String title = "::panel::";
	
	/**
	 * User may use some control widget to pop a panel.<p>
	 * When this happened,this control widget should be disabled.<p>
	 * Because the panel should not be created twice.<p>
	 * After closing this panel, the widget will be enabled again~~~.<p>
	 */
	private Control trigger = null;
	
	protected static final int FIRST_NONE = 0;
	protected static final int FIRST_FULLSCREEN = 1;
	protected static final int FIRST_MAXIMIZED = 2;
	protected int firstAction = FIRST_NONE;

	private Scene scene=null;
	private Stage stage=null;
	
	public abstract Parent layout();
	
	public PanBase(){
		this("",null);
	}
	
	public PanBase(String title){
		this(title,null);
	}
	
	public PanBase(Control trigger){
		this("",trigger);
	}
	
	public PanBase(String title,Control trigger){
		this.title = title;
		this.trigger = trigger;
	}
	
	public Node getParent(){ 
		return root;
	}
	
	public Scene getScene(){ 
		return scene;
	}
	
	public Stage getStage(){ 
		return stage;
	}
	
	public void makeDialog(Window parent){
		stage = new Stage(StageStyle.UNIFIED);		
		stage.initModality(Modality.WINDOW_MODAL); 
		stage.initOwner(parent);
		stage.setResizable(false);
		stage.centerOnScreen();
		init_stage(stage);
	}
	
	public void makeStage(Window parent){
		stage = new Stage(StageStyle.UNIFIED);		
		stage.initModality(Modality.NONE); 
		stage.initOwner(parent);
		stage.centerOnScreen();
		init_stage(stage);
	}
	
	public void appear(Stage stg){
		stage = stg;
		init_stage(stg);		
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
		init_stage(stg);
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
	
	private void init_stage(Stage stg){		
		init_scene();
		//check whether we need to hook event~~~
		if(stg.getOnShowing()==null){
			stg.setOnShowing(eventWindow);
		}
		if(stg.getOnShown()==null){
			stg.setOnShown(eventWindow);
		}		
		if(stg.getOnHiding()==null){
			//stg.setOnCloseRequest(eventWindow);
			stg.setOnHiding(eventWindow);
		}
		
		//set title and some properties~~~
		stg.setTitle(title);
		stg.setScene(scene);
		stg.sizeToScene();
		stg.setUserData(PanBase.this);
	}
	
	private TskBase task = null;
	private JFXSpinner spin=new JFXSpinner();	
	private Parent root = null;
	
	public void spinning(
		final boolean flag
	){
		spinning(flag,null);
	}
	
	public void spinning(
		final boolean flag,
		final TskBase spinTask
	){
		task = spinTask;
		if(flag==false && task!=null){
			task.stop();
		}
		root.setDisable(flag);
		spin.setVisible(flag);
	}
	
	public void invokeSpinning(
		final boolean flag
	){
		invokeSpinning(flag,null);
	}
	
	public void invokeSpinning(
		final boolean flag,
		final TskBase spinTask
	){
		if(Application.GetApplication()==null){
			return;
		}
		Application.invokeAndWait(()->spinning(flag,spinTask));
	}
	
	private void init_scene(){
		if(scene!=null){
			return;
		}
		//first initialization...
		spin.setVisible(false);
		spin.setRadius(64);
		spin.setOnMouseClicked(EVENT->{
			spinning(false,task);
		});
		root = layout();		
		scene = new Scene(new StackPane(root,spin));
		scene.getStylesheets().add(Gawain.class.getResource("res/styles.css").toExternalForm());
		scene.setUserData(PanBase.this);
	}

	public static final Notification.Notifier msgBox = 
		NotifierBuilder.create()
		.popupLocation(Pos.CENTER)
		.popupLifeTime(Duration.millis(1500))
		.build();
	//------------------------//
	
	public interface EventHook{
		void eventShowing(WindowEvent e);
		void eventShown(WindowEvent e);
		void eventWatch(int cnt);
		void eventClose(WindowEvent e);
	};
	
	public EventHook hook = null;
	
	protected void eventShowing(WindowEvent e){
		if(hook!=null){ 
			hook.eventShowing(e);
		}
	}
	protected void eventShown(WindowEvent e){
		if(hook!=null){ 
			hook.eventShown(e);
		}
	}
	protected void eventWatch(int cnt){
		if(hook!=null){
			hook.eventWatch(cnt);
		}		
	}
	protected void eventClose(WindowEvent e){		
		if(hook!=null){ 
			hook.eventClose(e);
		}
	}
	
	private EventHandler<WindowEvent> eventWindow = new EventHandler<WindowEvent>(){
		@Override
		public void handle(WindowEvent event) {
			//if stage have no handle, direct event to here!!!
			if(WindowEvent.WINDOW_SHOWING==event.getEventType()){				
				eventShowing(event);
			}else if(WindowEvent.WINDOW_SHOWN==event.getEventType()){
				eventShown(event);
			}else if(WindowEvent.WINDOW_HIDING==event.getEventType()){
				if(trigger!=null){
					trigger.setDisable(false);
				}
				watchStop();				
				eventClose(event);
				BoxLogger.pruneList(root.getChildrenUnmodifiable());
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

	public static Pane decorate(String txt,Node cntxt){
		
		Label title = new Label(" "+txt);
		title.getStyleClass().add("group-title");
		cntxt.getStyleClass().add("group-content");
		StackPane.setAlignment(title,Pos.TOP_LEFT);
		StackPane.setAlignment(cntxt,Pos.BOTTOM_LEFT);
		
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
	
	public static HBox fillHBox(Object... args){
		HBox lay = new HBox();
		lay.getStyleClass().add("hbox-small");
		for(int i=0; i<args.length; i++){
			Control ctl = (Control)(args[i]);
			//why do we need this to stretch widget??
			ctl.setMaxWidth(Double.MAX_VALUE);
			HBox.setHgrow(ctl,Priority.ALWAYS);
			lay.getChildren().add(ctl);
		}
		return lay;
	}
	
	public static VBox fillVBox(Object... args){
		VBox lay = new VBox();
		lay.getStyleClass().add("vbox-small");		
		for(int i=0; i<args.length; i++){
			Control ctl = (Control)(args[i]);
			ctl.setMaxWidth(Double.MAX_VALUE);
			//VBox.setVgrow(ctl,Priority.ALWAYS);
			lay.getChildren().add(ctl);
		}
		return lay;
	}
	
	public static Button genButton1(
		final String title,
		final String iconName
	){
		return genButton(title,iconName,"btn-raised1");
	}
	
	public static Button genButton2(
		final String title,
		final String iconName
	){
		return genButton(title,iconName,"btn-raised2");
	}
	
	public static Button genButton3(
		final String title,
		final String iconName
	){
		return genButton(title,iconName,"btn-raised3");
	}
	
	public static Button genButton4(
		final String title,
		final String iconName
	){
		return genButton(title,iconName,"btn-raised4");
	}
	
	private static Button genButton(
		final String title,
		final String iconName,
		final String styleName
	){
		JFXButton btn = new JFXButton(title);
		btn.getStyleClass().add(styleName);
		if(iconName!=null){
			if(iconName.length()!=0){
				btn.setGraphic(Misc.getIcon(iconName));
			}
		}		
		btn.setMaxWidth(Double.MAX_VALUE);
		return btn;
	}
	
	/**
	 * Decorate control item with a table grid.<p>
	 * User must pay attention to argument sequence.Column symbol will be added automatically.<p> 
	 * @param arg - the sequence must be Label and Control, etc.
	 * @return
	 */
	public static GridPane decorateGrid(Object... arg){
		GridPane pan = new GridPane();
		pan.getStyleClass().add("grid-small");
		int cnt = arg.length/2;
		for(int i=0; i<cnt; i++){
			Label txt = new Label((String)arg[i*2+0]);
			Node obj = (Node)(arg[i*2+1]);
			pan.addRow(i,txt,new Label("："),obj);
		}		
		return pan;
	}
}

