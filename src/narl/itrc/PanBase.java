package narl.itrc;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.binding.Bindings;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.converter.NumberStringConverter;

public abstract class PanBase {	
	
	protected static final int FIRST_NONE = 0;
	protected static final int FIRST_FULLSCREEN = 1;
	protected static final int FIRST_MAXIMIZED = 2;
	
	protected int firstAction = FIRST_NONE;
	
	protected java.net.URL customCSS = null;
	protected String customStyle = null;

	private Node  panel;
	private Stage stage;
	private Scene scene;
	
	public PanBase(){
		this("",null);
	}
	public PanBase(String title){
		this(title,null);
	}
	public PanBase(Parent root){
		this("",root);
	}
	public PanBase(String title, Parent pan){
		this.panel = pan;
	}
	
	public Node  getPanel(){ return panel; }
	public Stage getStage(){ return stage; }
	public Scene getScene(){ return scene; }	
	//------------------------//
	
	/**
	 * prepare a stage or panel. no showing!!!
	 * @return self
	 */
	public PanBase prepare(){
		return prepare(create_stage(null));
	}
	/**
	 * prepare a stage or panel. no showing!!!
	 * @return self
	 */
	public PanBase prepare(Stage stg){
		init_panel();		
		init_stage(stg);
		return this;
	}
	
	/**
	 * present a new panel, but no-blocking
	 * @return self
	 */
	public PanBase appear(){
		return appear(null);
	}
	/**
	 * present a new panel, but no-blocking
	 * @param stg - parent stage
	 * @return self
	 */
	public PanBase appear(Stage owner){
		stage = create_stage(owner);
		prepare(stage);
		stage.show();	
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
		stage = stg;
		prepare(stg);
		stg.showAndWait();
	}
	
	/**
	 * close panel
	 */
	public void dismiss(){		
		if(stage==null){
			return;
		}
		stage.close();
		stage = null;
		scene = null;
		panel = null;
	}
	//------------------------//
		
	public abstract Node eventLayout(PanBase self);
	protected       void eventShowing(PanBase self){ }//let user override this function
	public abstract void eventShown(PanBase self);
	protected       void eventClose(PanBase self){ }//let user override this function
	
	private Stage create_stage(Window parent){
		Stage stg = new Stage(StageStyle.UNIFIED);		
		stg.initModality(Modality.WINDOW_MODAL); 
		stg.initOwner(parent);
		stg.centerOnScreen();		
		return stg;
	}
	private Stage init_stage(Stage stg){		
		if(stg.isShowing()==true){
			return stg;
		}		
		//hook all events
		stg.setOnShowing(e->{
			eventShowing(PanBase.this);
			//Platform.runLater(() -> panel.requestFocus());//it may cause screen not fresh~~~
		});
		stg.setOnShown  (e->{ eventShown(PanBase.this); });
		stg.setOnHidden (e->{ eventClose(PanBase.this); });		
		//set title and some properties~~~
		stg.setScene(scene);
		stg.sizeToScene();
		stg.setUserData(PanBase.this);
		//user may need a special action~~~
		switch(firstAction){
		case FIRST_FULLSCREEN:
			stg.setFullScreen(true);
			break;
		case FIRST_MAXIMIZED:
			stg.setMaximized(true);
			break;
		}
		return stg;
	}
	private void init_panel(){
		//first initialization...
		//require children generate GUI-layout
		if(panel!=null){
			return;
		}
		panel = eventLayout(this);
		if(panel==null){
			//We don't have panel,so create a default node....
			Label txt = new Label("?");
			txt.setFont(Font.font("Arial", 60));
			txt.setPrefSize(200, 200);
			txt.setAlignment(Pos.CENTER);
			panel = txt;
		}
		Parent node = new StackPane(panel,spinner);
		if(customStyle!=null){
			node.setStyle(customStyle);
		}
		init_scene(node);
	}
	private void init_scene(Parent root){
		
		scene = new Scene(root);		
		//load a default style...
		scene.getStylesheets().add(Gawain.class.getResource("res/styles.css").toExternalForm());				
		//if user give us a URL, try to load a custom style file....		
		//scene.getStylesheets().add(customCSS.toExternalForm());
		//capture some short-key
		scene.setOnKeyPressed(eventHookPress);
	}
	//------------------------//
	
	/**
	 * special short-key for popping logger panel.
	 */
	private static KeyCombination hotkey_console = KeyCombination.keyCombination("Ctrl+Alt+C");
	
	/**
	 * special short-key for debug something.
	 */
	private static KeyCombination hotkey_debug = KeyCombination.keyCombination("Ctrl+Alt+D");
	
	/**
	 * event for capture all key-input, this happened when user click the title of panel.
	 */
	private EventHandler<KeyEvent> eventHookPress = new EventHandler<KeyEvent>(){
		@Override
		public void handle(KeyEvent event) {
			KeyCode cc = event.getCode();
			if(cc==KeyCode.ESCAPE){
				if(Gawain.isMainWindow(PanBase.this)==true){
					if(notifyConfirm("！！注意！！","確認離開主程式？")==ButtonType.CANCEL){
						return;
					}			
				}
				dismiss();
			}else if(hotkey_console.match(event)==true){
				Gawain.showLogger();
			}else if(hotkey_debug.match(event)==true){
				spinner.kick();
			}			
		}
	};
	//------------------------//
	
	protected class Spinner extends VBox {
		
		private JFXSpinner icon = new JFXSpinner();//show that we are waiting
		private Label      text = new Label();//show progress value
		private Task<?>    task = null;
		
		public Spinner(){
			icon.setRadius(48);
			icon.setOnMouseClicked(e->{
				if(notifyConfirm("！！注意！！","確認離開主程式？")==ButtonType.CANCEL){
					return;
				}
				if(task!=null){
					task.cancel(true);
				}
				done();			
			});		
			text.setFont(Font.font("Arial", 27));
			text.setAlignment(Pos.CENTER);
			
			setStyle("-fx-spacing: 13;-fx-padding: 7;");
			setAlignment(Pos.CENTER);			
			getChildren().addAll(icon,text);
			setVisible(false);
		}
		private void prepare(){
			text.textProperty().unbind();
			text.setText("工作中");
			panel.setDisable(true);
			setVisible(true);
		}
		private void done(){
			task = null;//reset it for next turn~~~
			panel.setDisable(false);
			setVisible(false);	
		}
		
		private void kick(){
			prepare();		
		};
		
		public void kick(final Task<?> tsk){
			kick("panel-task", '?', tsk);
		}
		public void kick(String name, final Task<?> tsk){
			kick(name, '?', tsk);
		}
		
		/**
		 * kick a task to do heavy working.
		 * @param type - 'p' mean bar, 'm' mean label
		 * @param tsk - the instance of task class.
		 */
		public void kick(char type, final Task<?> tsk){
			kick("panel-task", type, tsk);
		}
		/**
		 * kick a task to do heavy working.
		 * @param name - the name of task.
		 * @param type - the bottom label will show what kind of information.
		 * @param tsk - the instance of task class.
		 */
		public void kick(
			String name, 
			char type, 
			final Task<?> tsk
		){
			prepare();
			switch(type){
			case 'p':
				text.textProperty().bind(
					tsk.progressProperty()
					.multiply(100)
					.asString("%.0f%%")
				);
				break;
			case 'm':
				text.textProperty().bind(tsk.messageProperty());
				break;
			}
			//replace original handle
			final EventHandler<WorkerStateEvent> old = tsk.getOnSucceeded();
			tsk.setOnSucceeded(event->{
				if(old!=null){
					old.handle(event);
				}				
				done();
			});
			task = tsk;//keep this variable~~~
			new Thread(tsk,name).start();
		}
		/**
		 * lamda-style task function
		 */
		public void kick(			
			final EventHandler<Event> task,
			final EventHandler<WorkerStateEvent> event1 
		){
			prepare();
			final Task<Integer> tsk = new Task<Integer>(){
				@Override
				protected Integer call() throws Exception {
					task.handle(null);
					return 0;
				}
			};
			tsk.setOnSucceeded(event->{
				if(event1!=null){
					event1.handle(event);
				}
				done();
			});
			tsk.setOnCancelled(event->{
				done();
			});
			new Thread(tsk,"").start();
		}
	};
	protected Spinner spinner = new Spinner();
	//------------------------//
	
	/**
	 * macro-expansion for information alter dialog
	 * @param title
	 * @param message
	 */
	public static ButtonType notifyInfo(
		final String title,
		final String message
	){
		return popup_alter(
			AlertType.INFORMATION,
			title, message,
			null
		);
	}
	
	/**
	 * macro-expansion for warning alter dialog
	 * @param title
	 * @param message
	 */
	public static ButtonType notifyWarning(
		final String title,
		final String message
	){
		return popup_alter(
			AlertType.WARNING,
			title, message,
			null
		);
	}
	
	/**
	 * macro-expansion for error alter dialog
	 * @param title 
	 * @param message
	 */
	public static ButtonType notifyError(
		final String title,
		final String message
	){
		return popup_alter(
			AlertType.ERROR,
			title,message,
			null
		);
	}
	
	public static ButtonType notifyConfirm(
		final String title,
		final String message
	){
		return popup_alter(
			AlertType.CONFIRMATION,
			title,message,
			null
		);
	}
	
	private static ButtonType popup_alter(
		final AlertType type,
		final String title,
		final String message,
		final Node expand
	){
		Alert dia = new Alert(type);
		dia.setTitle(title);
		dia.setHeaderText(message);
		dia.setContentText(null);
		if(expand!=null){
			dia.getDialogPane().setExpandableContent(expand);
		}
		return dia.showAndWait().get();
	}
	//------------------------//
	
	protected List<File> chooseFiles(String title){
		final FileChooser dia = new FileChooser();
		dia.setTitle(title);
		dia.setInitialDirectory(Gawain.dirRoot);
		return dia.showOpenMultipleDialog(stage);
	}
	
	protected File chooseFile(String title){
		final FileChooser dia = new FileChooser();
		dia.setTitle(title);
		dia.setInitialDirectory(Gawain.dirRoot);
		return dia.showOpenDialog(stage);
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
			ImageView img = Misc.getIconView(iconName);
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
				btn.setGraphic(Misc.getIconView(iconName));
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
	
	private static Button gen_fx_button(
		final String title,
		final String iconName,
		final String styleName
	){
		JFXButton btn = new JFXButton(title);
		btn.getStyleClass().add(styleName);
		if(iconName!=null){
			if(iconName.length()!=0){
				btn.setGraphic(Misc.getIconView(iconName));
			}
		}
		return btn;
	}	
	//----------------------------------------------//
	
	private static void validInteger(
		final int min,
		final int max,
		final TextField box,
		final IntegerProperty val
	){
		int curVal = 0;
		try{
			curVal = Integer.valueOf(box.getText());
			if(curVal<min){
				box.setText(String.valueOf(min));
			}else if(max<curVal){
				box.setText(String.valueOf(max));			
			}else{
				box.setText(String.valueOf(curVal));
				val.setValue(curVal);//assign new value~~~
			}
		}catch(NumberFormatException e){
			box.setText(String.valueOf(val.get()));//restore old value~~~~
		}
		box.positionCaret(box.getText().length());
	}
		
	private static void validFloat(
		final BigDecimal min,
		final BigDecimal max,
		final TextField box,
		final FloatProperty val
	){		
		try{
			BigDecimal _val = new BigDecimal(val.get());
			if(_val.compareTo(min)<0){
				box.setText(min.toString());
			}else if(_val.compareTo(max)>0){
				box.setText(max.toString());
			}else{
				box.setText(_val.toString());
				val.setValue(_val.floatValue());//assign new value~~~
			}
		}catch(NumberFormatException e){
			box.setText(String.valueOf(val.get()));//restore old value~~~~
		}
		box.positionCaret(box.getText().length());
	}
		
	public static TextField genBoxInteger(
		final int min, 
		final int max,
		final IntegerProperty val
	){
		final JFXTextField box = new JFXTextField();
		box.setOnAction(event->{
			validInteger(min,max,box,val);
		});
		box.focusedProperty().addListener((arg1,newVal,oldVal)->{
			validInteger(min,max,box,val);
		});
		Bindings.bindBidirectional(
			box.textProperty(), 
			val, 
			new NumberStringConverter("#")
		);
		return box;
	} 

	public static TextField genBoxFloat(
		final String min, 
		final String max,
		final FloatProperty val
	){
		final JFXTextField box = new JFXTextField();
		final BigDecimal _min = new BigDecimal(min);
		final BigDecimal _max = new BigDecimal(max);
		box.setOnAction(event->{
			validFloat(_min,_max,box,val);
		});
		box.focusedProperty().addListener((arg1,newVal,oldVal)->{
			validFloat(_min,_max,box,val);
		});
		Bindings.bindBidirectional(
			box.textProperty(), 
			val, 
			new NumberStringConverter("#.####")
		);
		return box;
	}
}

