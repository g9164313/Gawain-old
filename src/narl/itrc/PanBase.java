package narl.itrc;

import java.io.File;
import java.util.List;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.controls.JFXSpinner;

import javafx.concurrent.Task;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public abstract class PanBase {
	
	protected DutyFace spin = new DutyFace();
	
	private final StackPane root = new StackPane();
	
	private final Scene scene = new Scene(root);
	private final Stage stage = new Stage();
	
	public PanBase(){
	}
	//------------------------//
	
	public Parent root() {
		return root;
	}
	public Scene scene() {
		return scene;
	}
	public Stage stage(){ 
		return stage; 
	}
	
	//default panel node
	//Label txt = new Label("===\n X \n===");
	//txt.setFont(Font.font("Arial", 60));
	//txt.setPrefSize(200, 200);
	//txt.setAlignment(Pos.CENTER);
	//face1 = txt;
	
	public void initLayout() {
		
		Node face1 = eventLayout(this);
		face1.disableProperty().bind(spin.visibleProperty());
		
		root.getChildren().addAll(face1, spin);
		
		//load a default style...
		scene.setRoot(
			(Gawain.propFlag("JFX_DECORATE")==true)?
			(new JFXDecorator(stage, root)):
			(root)
		);
		scene.setFill(Color.WHITE);
		scene.getStylesheets().add(Gawain.sheet);				
		scene.setOnKeyPressed(eventHotKey);//capture some short-key
	}
	
	private void prepare(){
		Pane pan = (Pane)root();
		if(pan.getChildren().size()==0) {
			initLayout();
		}
		stage.setScene(scene);
		String opt1 = Gawain.prop().getProperty("FULL_SCREEN","");
		if(opt1.equalsIgnoreCase("true")==true) {
			stage.setFullScreen(true);
		}else {
			stage.sizeToScene();
			stage.centerOnScreen();
		}
	}
		
	/**
	 * present a new panel, but no-blocking
	 * @return self
	 */
	public PanBase appear(){
		prepare();
		stage.show();
		return this;
	}
	/**
	 * present a new panel, and blocking for dismissing.<p>
	 * Never return to caller.<p>
	 */
	public void standby(){
		prepare();
		stage.showAndWait();		
	}
	//------------------------//
	
	/**
	 * Create all objects on panel, this is run by other thread.<p>
	 * So, don't bind any property in this method, or make anything doing by GUI-thread.<p>
	 * @param self - super class,
	 * @return
	 */
	public abstract Node eventLayout(PanBase self);

	protected class DutyFace extends VBox {
		JFXSpinner icon = new JFXSpinner();//show that we are waiting
		Label      mesg = new Label();//show progress value
		DutyFace(){
			icon.setRadius(48);
			icon.setOnMouseClicked(e->{
				if(notifyConfirm("！！注意！！","確認停止？")==ButtonType.CANCEL){
					return;
				}
				if(duty==null){
					return;
				}
				duty.cancel(true);
			});		
			mesg.setFont(Font.font("Arial", 27));
			mesg.setAlignment(Pos.CENTER);
			setStyle("-fx-spacing: 13;-fx-padding: 7;");
			setAlignment(Pos.CENTER);			
			getChildren().addAll(icon,mesg);
			setVisible(false);
		}
		void event_take_on(final Runnable hook){
			mesg.textProperty().bind(duty.messageProperty());
			setVisible(true);			
			if(hook==null){
				return;
			}
			hook.run();
		}
		void event_complete(final Runnable hook){
			mesg.textProperty().unbind();
			setVisible(false);
			if(hook==null){
				return;
			}
			hook.run();
		}
	};

	protected class Duty extends Task<Integer> {
		long p_idx = 0;
		long p_stp = 1;
		long p_end = 100;
		Runnable task;
		Duty(final Runnable hook){
			task = hook;
		}
		public void setMessage(String msg){
			updateMessage(msg);
		}
		public void setProgress(long idx){
			p_idx = idx;
			long val = (p_idx * 100L) / p_end;
			updateMessage(String.format("%2d%%", val));
		}
		public void incProgress(){
			p_idx = p_idx + p_stp;
			long val = (p_idx * 100L) / p_end;
			updateMessage(String.format("%2d%%", val));
		}
		public void initProgress(long beg, long stp, long end){
			p_idx = beg;
			p_stp = stp;
			p_end = end;
			updateMessage(" 0%%");
		}
		@Override
		protected Integer call() throws Exception {
			task.run();
			return 0;
		}		
	}
	protected Duty duty;
	
	protected void doDuty(
		final Runnable hookWorking
	){
		doDuty(null,hookWorking,null);
	}
	protected void doDuty(
		final Runnable hookWorking,
		final Runnable hookOffDuty
	){
		doDuty(null,hookWorking,hookOffDuty);
	}
	protected void doDuty(
		final Runnable hookOnDuty,
		final Runnable hookWorking,
		final Runnable hookOffDuty		
	){
		if(duty!=null){
			return;
		}
		duty = new Duty(hookWorking);
		duty.setOnScheduled(e->spin.event_take_on(hookOnDuty));
		duty.setOnSucceeded(e->{
			spin.event_complete(hookOffDuty);
			duty = null;
		});
		duty.setOnCancelled(e->{
			spin.event_complete(hookOffDuty);
			duty = null;
		});
		new Thread(duty,"Panel-Task").start();
	}
	//------------------------//
	
	/**
	 * special short-key for popping stdio panel.
	 */
	private static KeyCombination hotkey_console = KeyCombination.keyCombination("Ctrl+Alt+C");
	
	/**
	 * event for capture all key-input, this happened when user click the title of panel.
	 */
	private EventHandler<KeyEvent> eventHotKey = new EventHandler<KeyEvent>(){
		@Override
		public void handle(KeyEvent event) {
			KeyCode cc = event.getCode();
			if(cc==KeyCode.F1){
				if(Gawain.mainPanel.equals(PanBase.this)==true){
					if(notifyConfirm("！！注意！！","確認離開主程式？")==ButtonType.CANCEL){
						return;
					}			
				}
				stage.close();
			}else if(hotkey_console.match(event)==true){
			}			
		}
	};
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
		dia.setInitialDirectory(Gawain.dirHome);
		return dia.showOpenMultipleDialog(stage);
	}
	
	protected File chooseFile(String title){
		final FileChooser dia = new FileChooser();
		dia.setTitle(title);
		dia.setInitialDirectory(Gawain.dirHome);
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
	
	/*private static void validInteger(
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
	}*/
}

