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
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
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
	
	protected static final int FIRST_NONE = 0;
	protected static final int FIRST_FULLSCREEN = 1;
	protected static final int FIRST_MAXIMIZED = 2;
	
	protected int firstAction = FIRST_NONE;
	
	protected java.net.URL customCSS = null;
	
	protected String customStyle = null;
	
	protected String title;
		
	private Node root;
	
	private Scene scene;
	
	private Stage stage;
	
	public PanBase(){
		this("",null);
	}

	public PanBase(String title){
		this(title,null);
	}
	
	public PanBase(Parent root){
		this("",root);
	}
	
	public PanBase(String title, Parent root){
		this.title = title;
		this.root = root;
	}
	
	public Node getRootNode(){ 
		return root;
	}
	
	public Scene getScene(){
		return scene;
	}
	
	public Stage getStage(){ 
		return stage;
	}
	//------------------------//
	
	/**
	 * present a new panel, but no-blocking
	 * @return self
	 */
	public PanBase appear(){
		return appear(create_stage(null));
	}
	
	/**
	 * present a new panel, but no-blocking
	 * @param stg - parent stage
	 * @return self
	 */
	public PanBase appear(Stage stg){		
		init_panel();		
		init_stage(stg).show();
		return this;
	}
	
	/**
	 * present a new panel, and blocking for dismissing 
	 */
	public void standby(){
		standby(create_stage(null));
	}
	
	/**
	 * present a new panel, and blocking for dismissing 
	 * @param stg - parent stage
	 */
	public void standby(Stage stg){		
		init_panel();		
		init_stage(stg).showAndWait();
	}

	/**
	 * present a new 'dialog' with buttons, and blocking for dismissing.<p>
	 * If user want create buttons, just give a lambda function.<p>
	 * @param parent - it can be null
	 * @param eventCancel - when user press cancel button
	 * @param eventConfirm - when user press confirm button 
	 */
	public void popup(
		Window parent,
		final EventHandler<ActionEvent> eventCancel,
		final EventHandler<ActionEvent> eventConfirm
	){
		init_dialog(eventCancel,eventConfirm);		
		init_stage(create_dialog(parent)).showAndWait();
	}
	
	public void dismiss(){		
		if(stage==null){
			return;
		}		
		stage.close();
		stage = null;
	}
	//------------------------//
		
	public abstract Node eventLayout();
	
	private void init_scene(Parent root){
		
		scene = new Scene(root);
		
		//load a default style...
		scene.getStylesheets().add(Gawain.class.getResource("res/styles.css").toExternalForm());		
		
		//if user give us a URL, try to load a custom style file....
		if(customCSS!=null){			
			scene.getStylesheets().add(customCSS.toExternalForm());
		}
		scene.setUserData(PanBase.this);//do we need this???
	}
	
	private void init_panel(){
		//first initialization...
		//require children generate GUI-layout
		if(root!=null){
			return;
		}
		root = eventLayout();

		spin.setVisible(false);
		spin.setRadius(64);
		spin.setOnMouseClicked(event->spinning(false));
		
		StackPane _root = new StackPane(root,spin);
		if(customStyle!=null){
			_root.setStyle(customStyle);
		}
		
		init_scene(_root);
	}
	
	private void init_dialog(
		final EventHandler<ActionEvent> eventCancel,
		final EventHandler<ActionEvent> eventConfirm
	){		
		if(root!=null){
			return;
		}
		root = eventLayout();
		
		BorderPane _root = new BorderPane();		
		_root.setCenter(root);
		
		Button[] btn = {
			genButton1("取消",""),
			genButton2("確認","")
		};
		for(Button b:btn){
			HBox.setHgrow(b, Priority.ALWAYS);
			b.setVisible(false);
			b.setPrefHeight(32);
			b.setMaxWidth(Double.MAX_VALUE);
		}
		if(eventCancel!=null){
			btn[0].setVisible(true);
			btn[0].setOnAction(eventCancel);
		}
		if(eventCancel!=null){
			btn[1].setVisible(true);
			btn[1].setOnAction(eventCancel);
		}

		Button btn1 = genButton2("確認","");
		HBox.setHgrow(btn1, Priority.ALWAYS);
		btn1.setPrefHeight(32);
		btn1.setMaxWidth(Double.MAX_VALUE);
		
		HBox lay0 = new HBox();
		lay0.getStyleClass().add("hobx-medium");		
		lay0.getChildren().addAll(btn[0],btn[1]);

		_root.setBottom(lay0);

		init_scene(_root);
	}
	
	private Stage create_stage(Window parent){
		Stage stg = new Stage(StageStyle.UNIFIED);		
		stg.initModality(Modality.NONE); 
		stg.initOwner(parent);
		stg.centerOnScreen();
		return stg;
	}
	
	private Stage create_dialog(Window parent){
		Stage stg = new Stage(StageStyle.UNIFIED);		
		stg.initModality(Modality.WINDOW_MODAL); 
		stg.initOwner(parent);
		stg.setResizable(false);
		stg.centerOnScreen();
		return stg;
	}
	
	private Stage init_stage(Stage stg){	
		
		stage = stg;//override global variable, keep it for 'dismiss' command.
		if(stg.isShowing()==true){
			return stage;
		}

		//check whether we need to hook event~~~
		if(stg.getOnShowing()==null){
			stg.setOnShowing(eventWindow);
		}
		if(stg.getOnShown()==null){
			stg.setOnShown(eventWindow);
		}		
		if(stg.getOnHiding()==null){
			stg.setOnHiding(eventWindow);
		}
		
		//set title and some properties~~~
		stg.setTitle(title);
		stg.setScene(scene);
		stg.sizeToScene();
		stg.setUserData(PanBase.this);
		
		switch(firstAction){
		case FIRST_FULLSCREEN:
			stage.setFullScreen(true);
			break;
		case FIRST_MAXIMIZED:
			stage.setMaximized(true);
			break;
		}
		return stage;
	}
	//------------------------//
	
	private JFXSpinner spin = new JFXSpinner();	

	public void spinning(
		final boolean flag
	){
		spinning(flag,null);
	}
	
	public void spinning(
		final boolean flag,
		final TskAction task
	){
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
		final TskAction spinTask
	){
		if(Application.GetApplication()==null){
			return;
		}
		Application.invokeAndWait(()->spinning(flag,spinTask));
	}
	//------------------------//
	
	//this message may have some bug, workaround is stopping it periodically
	private static final Notification.Notifier msgBox =
		NotifierBuilder.create()
		.popupLocation(Pos.CENTER)
		.popupLifeTime(Duration.millis(1500))
		.build();
	
	private static Timeline msgEvent = null;
	
	private static void event_stop_msgBox(){
		msgEvent = new Timeline(new KeyFrame(
			Duration.millis(2000),
			event->msgBox.stop()
		));
		msgEvent.play();
	}
	
	public static void notifyInfo(String title,String message){
		if(msgEvent!=null){
			msgEvent.pause();
		}
		msgBox.notifyInfo(title, message);
		event_stop_msgBox();
	}
	
	public static void notifyWarning(String title,String message){
		if(msgEvent!=null){
			msgEvent.pause();
		}
		msgBox.notifyWarning(title, message);
		event_stop_msgBox();
	}
	
	public static void notifyError(String title,String message){
		if(msgEvent!=null){
			msgEvent.pause();
		}
		msgBox.notifyError(title, message);
		event_stop_msgBox();
	}
	//------------------------//
	
	public interface EventHook {
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
	
	private boolean flagPresent = false;
	
	public boolean isPresent(){
		return  flagPresent;
	}
	
	private EventHandler<WindowEvent> eventWindow = new EventHandler<WindowEvent>(){
		@Override
		public void handle(WindowEvent event) {
			//if stage have no handle, direct event to here!!!
			if(WindowEvent.WINDOW_SHOWING==event.getEventType()){
				flagPresent = false;
				eventShowing(event);
			}else if(WindowEvent.WINDOW_SHOWN==event.getEventType()){
				flagPresent = true;
				eventShown(event);
			}else if(WindowEvent.WINDOW_HIDING==event.getEventType()){
				flagPresent = false;
				watchStop();				
				eventClose(event);
				//TODO: BoxLogger.pruneList(root);//??? how to refresh message
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

	/**
	 * Decorate root-panel with lines.<p>
	 * It will generate a new panel 
	 * @param txt - just show title
	 * @param cntxt - context-panel or root-panel
	 * @return a new panel
	 */
	public static Pane decorate(String txt,Node cntxt){
		
		Label title = new Label("[ "+txt+" ]");
		title.getStyleClass().add("decorate0-title");
		cntxt.getStyleClass().add("decorate0-content");
		StackPane.setAlignment(title,Pos.TOP_LEFT);
		StackPane.setAlignment(cntxt,Pos.BOTTOM_LEFT);
		
		StackPane grp = new StackPane();
		grp.getStyleClass().add("decorate0-border");
		grp.getChildren().addAll(title,cntxt);
		return grp;
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
	//------------------------//
	
	public static Button genButtonFlat(
		final String title,
		final String iconName
	){		
		final double def_size = 25.;
		Button btn = new Button();
		btn.getStyleClass().add("btn-flat");
		btn.setMaxWidth(def_size);
		btn.setPrefHeight(def_size);
		if(title.length()!=0){
			btn.setText(title);
		}
		if(iconName.length()!=0){
			ImageView img = Misc.getIcon(iconName);
			img.setFitWidth(def_size);
			img.setFitHeight(def_size);
			btn.setGraphic(img);
		}		
		return btn;
	}
	
	private static Button gen_def_button(
		final String title,
		final String iconName,
		final String styleName
	){
		Button btn = new Button();
		if(title.length()!=0){
			btn.setText(title);
		}
		if(styleName.length()!=0){
			btn.getStyleClass().add(styleName);
		}
		if(iconName!=null){
			if(iconName.length()!=0){
				btn.setGraphic(Misc.getIcon(iconName));
			}
		}		
		btn.setMaxWidth(Double.MAX_VALUE);
		return btn;
	}
		
	public static Button genButton0(
		final String title,
		final String iconName
	){
		return gen_def_button(title,iconName,"");
	}
	
	private static Button gen_fx_button(
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
		return btn;
	}
	
	public static Button genButton1(
		final String title,
		final String iconName
	){
		return gen_fx_button(title,iconName,"btn-raised-1");
	}
	
	public static Button genButton2(
		final String title,
		final String iconName
	){
		return gen_fx_button(title,iconName,"btn-raised-2");
	}
	
	public static Button genButton3(
		final String title,
		final String iconName
	){
		return gen_fx_button(title,iconName,"btn-raised-3");
	}
	
	public static Button genButton4(
		final String title,
		final String iconName
	){
		return gen_fx_button(title,iconName,"btn-raised-4");
	}
}

