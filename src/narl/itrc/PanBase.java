package narl.itrc;

import java.io.File;
import java.util.List;

import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXSpinner;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import narl.itrc.init.LogStream;

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
	
	public static PanBase self(final Node obj){
		return (PanBase)(obj.getScene().getUserData());
	}
	
	//default panel node
	//Label txt = new Label("===\n X \n===");
	//txt.setFont(Font.font("Arial", 60));
	//txt.setPrefSize(200, 200);
	//txt.setAlignment(Pos.CENTER);
	//face1 = txt;
	private static final KeyCombination kb_quit = KeyCombination.keyCombination("Ctrl+ESC");
	private static final KeyCombination kb_console = KeyCombination.keyCombination("Ctrl+F1");
	
	public void initLayout() {
		final Pane face = eventLayout(this);
		StackPane.setAlignment(face, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(face, Pos.TOP_RIGHT);
        final Pane root = new StackPane(face);
		//final Pane root = eventLayout(this);
		root.getStyleClass().addAll("background");
		if(Gawain.propFlag("JFX_DECORATE")==true) {
			scene = new Scene(new JFXDecorator(stage,root));
		}else {
			scene = new Scene(root);
		}
		scene.setUserData(this);//keep self-pointer		
		scene.getStylesheets().add(Gawain.sheet);//load a default style...
		scene.setFill(Color.WHITE);
		scene.setOnKeyPressed(event->{
			if(kb_console.match(event)==true) {
				LogStream.getInstance().showConsole();
			}else if(
				kb_quit.match(event)==true &&
				Gawain.mainPanel.equals(PanBase.this)==true
			) {
				if(notifyConfirm("！！注意！！","確認離開主程式？")==ButtonType.OK){
					stage.close();
				}
			}
		});//capture some short-key
	}
	
	private void prepare(){
		if(scene==null) {
			initLayout();
		}		
		stage.setScene(scene);
		if(Gawain.mainPanel==this) {
			if(Gawain.propFlag("PANEL_FULL")==true) {
				stage.setFullScreen(true);
			}else if(Gawain.propFlag("PANEL_MAX")==true) {
				stage.setMaximized(true);
			}else {
				stage.sizeToScene();
				stage.centerOnScreen();
			}
		}else {
			stage.sizeToScene();
			stage.centerOnScreen();
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
		
	private static class Spinner extends JFXDialog {		
		JFXSpinner icon;
		Label[] mesg = {new Label(), new Label()};		
		public Spinner(){			
			icon = new JFXSpinner();

			mesg[0].setMinWidth(100);
			mesg[0].setFont(Font.font("Arial", 26));
			mesg[0].setAlignment(Pos.BASELINE_LEFT);
			
			mesg[1].setMinWidth(50);
			mesg[1].setFont(Font.font("Arial", 26));
			mesg[1].setAlignment(Pos.BASELINE_RIGHT);
			
			final HBox lay = new HBox(icon,mesg[0],mesg[1]);
			lay.getStyleClass().addAll("box-pad");
			lay.setAlignment(Pos.CENTER_LEFT);	
			setContent(lay);
		}
	};
	
	protected static interface NotifyEvent {
		void action(Timeline ladder,Label[] message);
	};
	
	/**
	 * use 'Timeline' as stepper-ladder.<p>
	 * For skipping backward visit keyframe, combine 'playFromStart' and 'jumpTo'.<p>
	 * @param event - extend KeyFrame Interface.
	 */
	public void notifyEvent(NotifyEvent... event) {
		final Spinner dlg = new Spinner();
		final Timeline ladder = new Timeline();		
		ladder.setCycleCount(0);
		ladder.setOnFinished(e->dlg.close());
		for(int i=0; i<event.length; i++) {
			final int idx = i;
			ladder.getKeyFrames().add(new KeyFrame(
				Duration.millis(idx*100.),
				e->event[idx].action(ladder, dlg.mesg)
			));
		}
		//ladder.playFromStart();
		//ladder.jumpTo(Duration.seconds(1.));
		dlg.setOnDialogOpened(e->ladder.playFromStart());
		dlg.setOnDialogClosed(e->ladder.stop());
		dlg.show((StackPane)root());
	}

	/**
	 * show a spinner, let user know we are working.<p>
	 * @param task - a working thread.<p>
	 */
	public Task<?> notifyTask(
		final String name,
		final Task<?> task
	) {
		final Spinner dlg = new Spinner();
		dlg.mesg[0].textProperty().bind(task.messageProperty());
		dlg.mesg[1].visibleProperty().bind(task.progressProperty().greaterThan(0.f));
		dlg.mesg[1].textProperty().bind(task.progressProperty().multiply(100.f).asString("%.0f％"));
		//override old handler~~~
		task.setOnSucceeded(e->{
			if(task.getOnSucceeded()!=null){
				task.getOnSucceeded().handle(e);
			}
			dlg.close();
		});
		task.setOnCancelled(e->dlg.close());
		dlg.setOnDialogOpened(e->new Thread(task,name).start());
		dlg.setOnDialogClosed(e->task.cancel());
		dlg.show((StackPane)root());
		return task;
	}
	public Task<?> notifyTask(final Task<?> task) {
		return notifyTask("--task--",task);
	}
	//----------------------------------------------//
	
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

	public File loadFrom(){
		final FileChooser dia = new FileChooser();
		dia.setTitle("讀取檔案...");
		dia.setInitialDirectory(Gawain.dirHome);
		return dia.showOpenDialog(stage);
	}
	
	public File saveAs(final String default_name){
		final FileChooser dia = new FileChooser();
		dia.setTitle("儲存成為...");
		dia.setInitialFileName(default_name);
		dia.setInitialDirectory(Gawain.dirHome);
		return dia.showSaveDialog(stage);
	}
	//----------------------------------------------//
	
	public static Node border(
		final String title,
		final Node obj
	){
		Label txt = new Label(title);
		txt.getStyleClass().add("font-size7");
		
		obj.getStyleClass().add("box-border");
		
		VBox lay = new VBox(txt,obj);
		lay.getStyleClass().add("box-pad-group");		
		return lay;
	}
}

