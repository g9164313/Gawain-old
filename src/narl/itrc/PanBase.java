package narl.itrc;

import java.math.BigDecimal;
import java.util.HashMap;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.binding.Bindings;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;


public abstract class PanBase {	
	
	protected static final int FIRST_NONE = 0;
	protected static final int FIRST_FULLSCREEN = 1;
	protected static final int FIRST_MAXIMIZED = 2;
	
	protected int firstAction = FIRST_NONE;
	
	protected java.net.URL customCSS = null;
	
	protected String customStyle = null;
	
	protected StringProperty propTitle = new SimpleStringProperty();
	
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
		propTitle.set(title);
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
		stage = stg;
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
		stage = stg;
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
		final Window parent,
		final EventHandler<ActionEvent> eventCancel,
		final EventHandler<ActionEvent> eventConfirm
	){
		stage = create_dialog(parent);
		init_dialog(eventCancel,eventConfirm);		
		init_stage(stage).showAndWait();
	}
	
	/**
	 * present a new 'dialog'.<p>
	 * @param parent - root panel
	 */
	public void popup(final Window parent){
		popup(parent,null,null);
	}
	
	/**
	 * present a new 'dialog'.<p>
	 */
	public void popup(){
		popup(null,null,null);
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
	}
	//------------------------//
		
	public abstract Node eventLayout(PanBase self);
	
	private void init_scene(Parent root){		
		scene = new Scene(root);
		//load a default style...
		scene.getStylesheets().add(Gawain.class.getResource("res/styles.css").toExternalForm());				
		//if user give us a URL, try to load a custom style file....
		if(customCSS!=null){			
			scene.getStylesheets().add(customCSS.toExternalForm());
		}
		//capture some short-key
		scene.setOnKeyPressed(eventHookPress);
	}

	private void init_panel(){
		//first initialization...
		//require children generate GUI-layout
		if(root!=null){
			return;
		}
		root = eventLayout(this);
		//At this time, we should have stage~~~~
		StackPane _root = new StackPane(root,panTask);
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
		root = eventLayout(this);
		
		BorderPane _root = new BorderPane();		
		_root.setCenter(root);
		if(eventCancel!=null || eventConfirm!=null){			
			HBox lay0 = new HBox();
			lay0.setAlignment(Pos.BASELINE_RIGHT);
			lay0.getStyleClass().add("hbox-small");
			if(eventCancel!=null){
				Button btn = genButton3("取消","close.png");
				btn.setOnAction(event->{
					eventCancel.handle(event);
					dismiss();
				});
				lay0.getChildren().add(btn);
			}
			if(eventConfirm!=null){
				Button btn = genButton2("確認","check.png");				
				btn.setOnAction(event->{					
					eventConfirm.handle(event);
					dismiss();
				});
				lay0.getChildren().add(btn);
			}
			_root.setBottom(lay0);
		}
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
		if(stg.isShowing()==true){
			return stg;
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
		stg.titleProperty().bind(propTitle);
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

	private EventHandler<WindowEvent> eventWindow = new EventHandler<WindowEvent>(){
		@Override
		public void handle(WindowEvent event) {
			//if stage have no handle, direct event to here!!!
			if(WindowEvent.WINDOW_SHOWING==event.getEventType()){
				eventShowing(event);
			}else if(WindowEvent.WINDOW_SHOWN==event.getEventType()){
				eventShown(event);
			}else if(WindowEvent.WINDOW_HIDING==event.getEventType()){			
				eventClose(event);
				//TODO:??? BoxLogger.pruneList(root);//??? how to refresh message
			}
		}
	};
	
	private HashMap<KeyCode,EventHandler<ActionEvent>> tableHotKey = new HashMap<KeyCode,EventHandler<ActionEvent>>();
	
	private HashMap<KeyCombination,EventHandler<ActionEvent>> tableShortcut = new HashMap<KeyCombination,EventHandler<ActionEvent>>();
	
	/**
	 * Hook key in current panel, then launch a event~~~
	 * @param key - just use key-code
	 * @param event
	 */
	public void hookPress(KeyCode key,EventHandler<ActionEvent> event){		
		tableHotKey.put(key, event);
	}
	
	/**
	 * Hook shortcut in current panel, then launch a event~~~
	 * @param key - the string which represents the requested key combination
	 * @param event
	 */
	public void hookPress(String key,EventHandler<ActionEvent> event){		
		tableShortcut.put(KeyCombination.keyCombination(key), event);
	}
	
	private EventHandler<KeyEvent> eventHookPress = new EventHandler<KeyEvent>(){
		@Override
		public void handle(KeyEvent event) {
			KeyCode cc = event.getCode();
			if(cc==KeyCode.ESCAPE){
				dismiss();
				return;
			}
			//check hot-key~~~
			EventHandler<ActionEvent> event2;
			if(tableShortcut.containsKey(cc)==true){
				tableHotKey.get(cc).handle(null);
				return;
			}
			//check all combination~~~
			for(KeyCombination com:tableShortcut.keySet()){
				if(com.match(event)==true){
					event2 = tableShortcut.get(com);
					event2.handle(null);
					return;
				}
			}			
		}
	};
	//------------------------//
	
	private class PanTask extends HBox {		
		public JFXProgressBar bar = new JFXProgressBar();
		public TextArea box = new TextArea();
		public JFXSpinner spn = new JFXSpinner();
		
		public PanTask(){
			setStyle("-fx-spacing: 7;-fx-padding: 10;");
			final double ww = 300.;
			final double hh = 30.;
			spn.setRadius((hh/2.)+7.);
			spn.setOnMouseClicked(event->{
				root.setDisable(false);
				this.setVisible(false);
			});
			bar.setPrefWidth(ww);
			box.setPrefSize(ww, hh);

			VBox lay0 = new VBox();
			lay0.setStyle("-fx-spacing: 7;");
			lay0.getChildren().addAll(bar,box);
			getChildren().addAll(lay0,spn);
			
			setMaxSize(ww+hh, hh);
			setVisible(false);//the initial state is invisible~~ 
		}
	}	
	private PanTask panTask = new PanTask();
	
	public void spinning(
		final boolean flag
	){
		spinning(flag,null);
	}
	
	public void spinning(
		final boolean flag,
		final TskAction task
	){
		root.setDisable(flag);
		panTask.setVisible(true);
	}
	//------------------------//
	
	/**
	 * macro-expansion for information alter dialog
	 * @param title
	 * @param message
	 */
	public static void notifyInfo(
		final String title,
		final String message
	){
		popup_alter(
			AlertType.INFORMATION,
			title,message,
			null
		);
	}
	
	/**
	 * macro-expansion for warning alter dialog
	 * @param title
	 * @param message
	 */
	public static void notifyWarning(
		final String title,
		final String message
	){
		popup_alter(
			AlertType.WARNING,
			title,message,
			null
		);
	}
	
	/**
	 * macro-expansion for error alter dialog
	 * @param title 
	 * @param message
	 */
	public static void notifyError(
		final String title,
		final String message
	){
		popup_alter(
			AlertType.ERROR,
			title,message,
			null
		);
	}
	
	private static void popup_alter(
		final AlertType type,
		final String title,
		final String message,
		final Node expand
	){
		Alert dia = new Alert(type);
		dia.setTitle(title);
		dia.setContentText(message);
		if(expand!=null){
			dia.getDialogPane().setExpandableContent(expand);
		}
		dia.showAndWait();
	}
	//------------------------//

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
		btn.setMinWidth(110);
		btn.setMaxWidth(Double.MAX_VALUE);
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
	//----------------------------------------------//
		
	public static Spinner<Float> genSpinnerFloat(
		final float min,
		final float max,
		final float tick,
		FloatProperty prop
	){
		Spinner<Float> spn = new Spinner<Float>();
		SpinnerValueFactory<Float> fac = new SpinnerValueFactory<Float>(){
			
			private BigDecimal valMin,valMax,valTick;
			private FloatProperty propVal;
			private String fmt = "%.03f";
			
			private StringConverter<Float> conv = new StringConverter<Float>(){
				@Override
				public String toString(Float object) {
					return String.format(fmt,object);
				}
				@Override
				public Float fromString(String string) {
					return Float.valueOf(string);
				}
			};
			{
				String txt = String.valueOf(tick);
				int pos = txt.indexOf(".");
				pos = txt.length() - pos - 1;
				fmt = "%.0"+pos+"f";
				valMin = new BigDecimal(String.format(fmt, min));
				valMax = new BigDecimal(String.format(fmt, max));			
				valTick = new BigDecimal(txt);
				propVal = prop;
				setConverter(conv);
				setValue(prop.getValue());
			}
			@Override
			public void decrement(int steps) {
				final BigDecimal valOld = new BigDecimal(String.format(fmt, propVal.get()));
	            final BigDecimal valNew = valOld.subtract(valTick.multiply(BigDecimal.valueOf(steps)));
	            int cmp = valNew.compareTo(valMin);
	            if(cmp>=0){
	            	float v = valNew.floatValue();            	
	            	setValue(v);
	            	propVal.setValue(v);
	            }
			}

			@Override
			public void increment(int steps) {
				final BigDecimal valOld = new BigDecimal(String.format(fmt, propVal.get()));
	            final BigDecimal valNew = valOld.add(valTick.multiply(BigDecimal.valueOf(steps)));
	            int cmp = valNew.compareTo(valMax);
	            Misc.logv("inc-cmp = %d, %s, %s", cmp, valNew.toString(), valMax.toString());
	            if(cmp<=0){
	            	float v = valNew.floatValue();            	
	            	setValue(v);
	            	propVal.setValue(v);
	            }
			}
		};
		spn.setValueFactory(fac);
		return spn;
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

