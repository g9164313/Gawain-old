package narl.itrc;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.binding.Bindings;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
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
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.NumberStringConverter;

public abstract class PanBase {	
	
	private Stage stage;
	
	public PanBase(){
		stage = new Stage(StageStyle.DECORATED);
	}
	
	public PanBase(Stage owner){
		stage = owner;
	}
	//------------------------//
	
	public Stage stage(){ 
		return stage; 
	}

	private boolean use_decor = false;
	
	/**
	 * use JFXDecorator, Attention call this before standby() or appear().<p> 
	 * @return
	 */
	public PanBase useDecorator(boolean flag){
		use_decor = flag;
		return this;
	}
	
	/**
	 * present a new panel, user can provide a owner
	 * @return self
	 */
	public PanBase appear(Stage owner){
		stage = owner;
		init();
		stage.show();
		return this;
	}
	
	/**
	 * present a new panel, but no-blocking
	 * @return self
	 */
	public PanBase appear(){
		init();
		stage.show();
		return this;
	}

	/**
	 * present a new panel, and blocking for dismissing.<p>
	 * Never return to caller.<p>
	 */
	public void standby(){
		init();
		stage.showAndWait();
	}
	
	private void init(){
		
		spin.visibleProperty().set(false);
		
		Node r_face = eventLayout(this);
		if(r_face==null){
			//default panel node
			Label txt = new Label("===\n X \n===");
			txt.setFont(Font.font("Arial", 60));
			txt.setPrefSize(200, 200);
			txt.setAlignment(Pos.CENTER);
			r_face = txt;
		}
		r_face.disableProperty().bind(spin.visibleProperty());
		
		final Node pan1 = new StackPane(r_face,spin);
		
		Parent root;		
		if(use_decor==true){
			JFXDecorator dec = new JFXDecorator(stage, pan1);
			dec.setCustomMaximize(true);
			root = dec;
		}else{
			root = (Parent)pan1;
		}
		
		final Scene se = new Scene(root);		
		//load a default style...
		se.getStylesheets().add(Gawain.class.getResource("res/styles.css").toExternalForm());				
		//if user give us a URL, try to load a custom style file....		
		//scene.getStylesheets().add(customCSS.toExternalForm());
		//capture some short-key
		se.setOnKeyPressed(eventHookPress);
		
		stage.setScene(se);
		stage.sizeToScene();
		stage.centerOnScreen();
		//stage.setUserData(this);
	}	
	//------------------------//
	
	public abstract Node eventLayout(PanBase self);

	private class DutyFace extends VBox {
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
	private DutyFace spin = new DutyFace();
	
	protected class Duty extends Task<Integer> {
		int p_idx = 0;
		int p_stp = 1;
		int p_end = 100;
		Runnable task;
		Duty(final Runnable hook){
			task = hook;
		}
		public void setMessage(String msg){
			updateMessage(msg);
		}
		public void setProgressIndex(int idx){
			p_idx = idx;
			int val = (p_idx * 100) / p_end;
			updateMessage(String.format("%d.%%", val));
		}
		public void incProgress(){
			p_idx = p_idx + p_stp;
			int val = (p_idx * 100) / p_end;
			updateMessage(String.format("%d.%%", val));
		}
		public void setProgressTotal(int beg, int stp, int end){
			p_idx = beg;
			p_stp = stp;
			p_end = end;
			updateMessage("0.%%");
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
		duty.setOnSucceeded(e->spin.event_complete(hookOffDuty));
		duty.setOnCancelled(e->spin.event_complete(hookOffDuty));
		new Thread(duty,"Panel-Task").start();
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
				stage.close();
			}else if(hotkey_console.match(event)==true){
				Gawain.showLogger();
			}else if(hotkey_debug.match(event)==true){
				
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

