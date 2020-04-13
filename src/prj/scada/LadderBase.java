package prj.scada;

import java.util.LinkedList;
import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import narl.itrc.Misc;

public class LadderBase extends BorderPane {

	public LadderBase(){
		
		final JFXButton[] btn = new JFXButton[5];
		for(int i=0; i<btn.length; i++) {
			JFXButton obj = new JFXButton();
			obj.setMaxWidth(Double.MAX_VALUE);
			btn[i] = obj;
		}
		//Import procedure
		btn[0].setText("匯入");
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setGraphic(Misc.getIconView("database-import.png"));
		btn[0].disableProperty().bind(is_cooking);
		//Export procedure
		btn[1].setText("匯出");
		btn[1].getStyleClass().add("btn-raised-1");
		btn[1].setGraphic(Misc.getIconView("database-export.png"));
		btn[1].disableProperty().bind(is_cooking);
		//Run all steps
		btn[2].setText("執行");
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setGraphic(Misc.getIconView("run.png"));
		btn[2].disableProperty().bind(is_cooking);
		btn[2].setOnAction(e->start_cooking());
		//Pause the current step~~
		btn[3].setText("暫停");
		btn[3].getStyleClass().add("btn-raised-2");
		btn[3].setGraphic(Misc.getIconView("pause.png"));
		btn[3].disableProperty().bind(is_cooking.not());
		btn[3].setOnAction(e->pause_cooking(false));
		//Stop immediately
		btn[4].setText("停止");
		btn[4].getStyleClass().add("btn-raised-2");
		btn[4].setGraphic(Misc.getIconView("pan_tool.png"));
		btn[4].disableProperty().bind(is_cooking.not());
		btn[4].setOnAction(e->pause_cooking(true));
		
		ctrl.getStyleClass().addAll("box-pad-inner");
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().addAll("box-pad");
		lay1.getChildren().addAll(
			btn[0],btn[1],
			btn[2],btn[3],btn[4],
			ctrl
		);
		
		final VBox lay2 = new VBox();
		lay2.getStyleClass().addAll("box-pad");
		
		setLeft(lay1);
		setCenter(lay2);
		
		addFunction("test-1");
		addFunction("test-2");
		addFunction("test-3");
	}
	//--------------------------------//
	
	private VBox ctrl = new VBox();
		
	public LadderBase addFunction(
		final String title
	){
		final JFXButton btn = new JFXButton(title);
		btn.getStyleClass().add("btn-raised-3");
		btn.setOnAction(e->{
			
		});
		ctrl.getChildren().add(btn);
		return this;
	}	
	//--------------------------------//
	
	private final JFXListView<Node> recipe = new JFXListView<Node>();
	
	private final BooleanProperty is_cooking = new SimpleBooleanProperty(false);
	
	private Optional<Timeline> time = Optional.empty();
	
	private void start_cooking(){
		is_cooking.set(true);
		if(time.isPresent()==false) {
			time = Optional.of(prepare_cooking());
			time.get().playFromStart();
		}else{
			time.get().play();
		}
	}
	
	private void pause_cooking(boolean abort){
		if(time.isPresent()==false) {
			return;
		}
		time.get().pause();
		if(abort){
			time = Optional.empty();
		}
		is_cooking.set(false);
	}
	
	private LinkedList<Runnable> step = new LinkedList<Runnable>();
	
	private boolean undo = false;
	
	private Timeline prepare_cooking(){
		step.clear();
		//list all runnable task~~~
		for(Node nn:recipe.getItems()){
			Runnable[] lst = (Runnable[])nn.getUserData();
			for(Runnable rr:lst){
				step.addLast(rr);
			}
		}
		//setup timer to repeat all steps~~~
		KeyFrame key = new KeyFrame(
			Duration.millis(500),
			e->cooking()
		);
		Timeline obj = new Timeline(key);
		obj.setCycleCount(Animation.INDEFINITE);
		obj.setOnFinished(e->is_cooking.set(false));
		return obj;
	}
	
	private void cooking(){
		if(step.isEmpty()==true){
			pause_cooking(true);
			return;
		}
		Runnable rr = step.pollFirst();
		rr.run();
		if(undo==true){
			step.addFirst(rr);
		}
	}

	public void waiting(){
		if(time.isPresent()==false){
			return;
		}
		Timeline obj = time.get();
		obj.jumpTo(obj.getCurrentTime());
	}
}
