package narl.itrc;

import java.io.File;
import java.util.List;

import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.events.JFXDialogEvent;

import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public abstract class PanBase {
	
	private Scene scene;
	private Stage stage;
	
	public PanBase(){
		stage = new Stage();
	}
	
	public PanBase(Stage init_stage){
		stage = init_stage;
	}	
	//------------------------//
	
	public Parent root() {
		return scene.getRoot();
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
		
		final Pane root = new StackPane(eventLayout(this));			
		if(Gawain.propFlag("JFX_DECORATE")==true) {
			scene = new Scene(new JFXDecorator(stage,root));
		}else {
			scene = new Scene(root);
		}
		
		scene.setFill(Color.WHITE);
		scene.getStylesheets().add(Gawain.sheet);//load a default style...
		root.getStyleClass().addAll("background");
		scene.setOnKeyPressed(event->{
			KeyCode cc = event.getCode();
			if(cc==KeyCode.F1){
				if(Gawain.mainPanel.equals(PanBase.this)==true){
					if(notifyConfirm("！！注意！！","確認離開主程式？")==ButtonType.CANCEL){
						return;
					}			
				}
				stage.close();
			}else if(cc==KeyCode.F2){				
				stage.setFullScreen(stage.fullScreenProperty().not().get());
			}else if(cc==KeyCode.F3){
				//show console ???
			}else if(hotkey_console.match(event)==true){
			}
		});//capture some short-key
	}
	
	private void prepare(){
		if(scene==null) {
			initLayout();
		}
		stage.setScene(scene);
		stage.sizeToScene();
		stage.centerOnScreen();
		
		if(Gawain.mainPanel==this) {
			if(Gawain.propFlag("FULL_SCREEN")==true) {
				stage.setFullScreen(true);
			}
		}
	}
		
	/**
	 * present a new panel, but non-blocking
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

	/**
	 * Create all objects on panel, this is run by other thread.<p>
	 * So, don't bind any property in this method, or make anything doing by GUI-thread.<p>
	 * @param self - super class,
	 * @return
	 */
	public abstract Pane eventLayout(PanBase self);
	//----------------------------------------------//
	
	/**
	 * special short-key for popping stdio panel.
	 */
	private static KeyCombination hotkey_console = KeyCombination.keyCombination("Ctrl+Alt+C");
	//----------------------------------------------//
	
	protected static class Spinner extends JFXDialog {
		JFXSpinner icon;
		Label mesg;
		public Spinner(){
			icon = new JFXSpinner();
			mesg = new Label();
			mesg.setMinWidth(100);
			mesg.setFont(Font.font("Arial", 26));
			mesg.setAlignment(Pos.CENTER);
			final HBox lay = new HBox(icon,mesg);
			lay.setAlignment(Pos.BASELINE_LEFT);
			lay.getStyleClass().addAll("box-pad");
			lay.setAlignment(Pos.BASELINE_LEFT);	
			setContent(lay);
		}
		public void text(final String text) {
			mesg.setText(text);
		}
	};
	/**
	 * show a spinner, let user know we are working.<p>
	 * @param event1 - After the dialog show...
	 * @param event2 - After closing the dialog...
	 */
	public void notifyTask(
		final EventHandler<JFXDialogEvent> event1,
		final EventHandler<JFXDialogEvent> event2
	) {
		final JFXDialog dlg = new Spinner();
		dlg.setOnDialogOpened(event1);
		dlg.setOnDialogClosed(event2);
		dlg.show((StackPane)root());
	}
	/**
	 * show a spinner, let user know we are working.<p>
	 * @param task - a working thread.<p>
	 */
	public void notifyTask(
		final Task<?> task,
		final Runnable hook
	) {
		final Spinner dlg = new Spinner();
		dlg.mesg.textProperty().bind(task.messageProperty());
		task.setOnSucceeded(e->{
			if(hook!=null) {
				hook.run();
			}
			dlg.close();
		});
		task.setOnCancelled(e->dlg.close());
		dlg.setOnDialogOpened(e->new Thread(task).start());
		dlg.setOnDialogClosed(e->task.cancel());
		dlg.show((StackPane)root());
	}
	public void notifyTask(final Task<?> tsk) {
		notifyTask(tsk,null);
	}

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
	//----------------------------------------------//
	
	protected List<File> chooseFiles(final String title){
		final FileChooser dia = new FileChooser();
		dia.setTitle(title);
		dia.setInitialDirectory(Gawain.dirHome);
		return dia.showOpenMultipleDialog(stage);
	}
	
	protected File chooseFile(final String title){
		final FileChooser dia = new FileChooser();
		dia.setTitle(title);
		dia.setInitialDirectory(Gawain.dirHome);
		return dia.showOpenDialog(stage);
	}
	
	public String saveAsFile(final String default_name){
		final FileChooser dia = new FileChooser();
		dia.setTitle("儲存成為...");
		dia.setInitialFileName(default_name);
		dia.setInitialDirectory(Gawain.dirHome);
		File fs = dia.showSaveDialog(stage);
		if(fs==null) {
			return "";
		}
		return fs.getAbsolutePath();
	}	
}

