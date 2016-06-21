package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public abstract class TskDialog extends TskBase
	implements EventHandler<ActionEvent>
{
	protected String title = "TskDialog";
	
	protected PanBase parent = null;
	
	/**
	 * User decide whether the dialog may be closed.<p>
	 * True  - this dialog will be alive after work done.<p>
	 * False - this dialog will be closed after work done.<p> 
	 */
	protected boolean afterwards = false; 
	
	public TskDialog(){
		initScene();
	}
	
	public TskDialog(PanBase root){
		parent = root;
		initScene();
	}

	private Stage workStage = null;
	private Scene workScene = null;
	private ProgressBar workBar;
	private TextArea workLog;

	private void initScene(){
		workScene = new Scene(layoutBody());
		workScene.getStylesheets().add(Gawain.class.getResource("res/styles.css").toExternalForm());
	}
	
	private Parent layoutBody(){
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("vbox-small");
		HBox lay1 = new HBox();
		lay1.getStyleClass().add("hbox-small");
		lay1.setAlignment(Pos.CENTER);
		
		workBar = new ProgressBar();
		workBar.setPrefWidth(270);

		Button btn = new Button("取消");
		btn.setPrefWidth(64);
		btn.setOnAction(event->stop());
		
		workLog = new TextArea();
		workLog.prefWidthProperty().bind(workBar.prefWidthProperty().add(btn.prefWidthProperty()));
		workLog.setPrefHeight(270);
		
		lay1.getChildren().addAll(workBar,btn);		
		lay0.getChildren().addAll(lay1,workLog);
		return lay0;
	}
	
	private void initStage(){
		workStage = new Stage(StageStyle.UNIFIED);		
		workStage.initModality(Modality.WINDOW_MODAL);
		if(parent!=null){
			workStage.initOwner(parent.getStage());
		}
		//workStage.setOnCloseRequest(event->stop());
		workStage.setOnHidden(event->stop());
		workStage.setOnShown(event->eventShown());
		workStage.setResizable(false);		
		workStage.setScene(workScene);
		workStage.centerOnScreen();
		workStage.setTitle(title);
	}
	
	public void appear(){		
		initStage();
		start(title);
		workStage.show();	
	}
	
	public void stabdby(){		
		initStage();
		start(title);
		workStage.showAndWait();		
	}
	
	public void setProgress(double val){
		if(Application.isEventThread()==true){
			workBar.setProgress(val);
		}else{
			Application.invokeAndWait(new Runnable(){
				@Override
				public void run() {
					workBar.setProgress(val);
				}				
			});
		}
	}	
	public void setProgress(int cur,int max){
		setProgress((double)(cur)/(double)(max));
	}
	
	private String checkTail(String txt){
		if(txt.length()==0){
			return txt;
		}
		if(txt.charAt(txt.length()-1)!='\n'){
			txt = txt + "\n";
		}
		return txt;
	}
	
	private int logCnt = 1;
	public void logv(String txt){
		setMessage(String.format("[INFO]%04d：%s",logCnt,checkTail(txt)));
		logCnt++;
	}
	public void logw(String txt){
		setMessage(String.format("[WARN]%04d：%s",logCnt,checkTail(txt)));
		logCnt++;
	}
	public void loge(String txt){
		setMessage(String.format("[ERRO]%04d：%s",logCnt,checkTail(txt)));
		logCnt++;
	}
	public void log(String txt){
		setMessage(checkTail(txt));
	}
	public void setMessage(final String txt){
		final Runnable work = new Runnable(){
			@Override
			public void run() {
				if(workLog.textProperty().get().length()>=500){
					workLog.textProperty().set("");//clear!!!
				}
				workLog.appendText(txt);
				workLog.setScrollTop(Double.MAX_VALUE);
			}
		};
		if(Application.isEventThread()==true){
			work.run();
		}else{
			Application.invokeAndWait(work);
		}
	}
	
	protected void eventShown(){
		//Here, we reset variables 
		logCnt = 1;
		workBar.setProgress(-1.);
	}
	
	protected void eventFinish(){
		if(afterwards==false){
			workStage.close();
		}
		if(parent!=null){
			parent.getParent().setDisable(false);
		}
	}
		
	@Override
	public void handle(ActionEvent event) {
		if(afterwards==true){
			//how to check whether the previous dialog is alive? 
		}
		if(eventBegin()==false){
			return;
		}
		if(parent!=null){
			parent.getParent().setDisable(true);
		}
		appear();
		start(title);
	}	
}
