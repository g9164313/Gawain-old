package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class PanTask extends PanBase implements 
	EventHandler<ActionEvent> 
{
	private Task<?> propTask;
	
	private ProgressBar bar;
	private Button btn;
	private TextArea log;
	
	public PanTask(String title,Task<?> task){
		propTask = task;
		this.title = title;
	}
	
	public static String checkMsgTail(String txt){
		if(txt.length()==0){
			return txt;
		}
		if(txt.charAt(txt.length()-1)!='\n'){
			txt = txt + "\n";
		}
		return txt;
	}
	
	public void txt2box(String _msg){
		log.appendText(_msg);
		log.setScrollTop(Double.MAX_VALUE);
	}
	
	private String _msg;
	public void logEvent(String msg){
		_msg = checkMsgTail(msg);
		final Runnable _event = new Runnable(){
			@Override
			public void run() {
				txt2box(_msg);
			}
		};
		Application.invokeAndWait(_event);
	}
	protected void logUpdate(String msg){
		txt2box(checkMsgTail(msg));
	}

	@Override
	public Parent layout() {

		final int BAR_WIDTH = 200;
		final int BTN_WIDTH = 60;
		
		HBox lay0 = new HBox();
		lay0.getStyleClass().add("hbox-small");
		lay0.setAlignment(Pos.CENTER);
		
		bar = new ProgressBar();
		bar.setPrefWidth(BAR_WIDTH);
		bar.setPrefHeight(27);
		
		btn = new Button("取消");
		btn.setPrefWidth(80);
		btn.setPrefHeight(30);
		btn.setOnAction(this);
		lay0.getChildren().addAll(bar,btn);
		
		VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");		
		
		log = new TextArea();
		log.setPrefWidth(BAR_WIDTH+BTN_WIDTH);
		log.setPrefHeight(130);
		lay1.getChildren().addAll(lay0,log);
		
		if(propTask!=null){
			bar.progressProperty().bind(propTask.progressProperty());
			btn.disableProperty().bind(propTask.runningProperty().not());
		}
		return lay1;
	}

	@Override
	public void handle(ActionEvent event) {
		if(propTask==null){
			return;
		}
		propTask.cancel();
	}
}
