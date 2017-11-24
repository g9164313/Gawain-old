package prj.daemon;

import com.sun.javafx.scene.traversal.Direction;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import narl.itrc.BoxLogger;
import narl.itrc.ButtonBounce;
import narl.itrc.DevTTY;
import narl.itrc.PanBase;

/**
 * Wrapping panel for testing motion device, like motor.
 * @author qq
 *
 */
public class PanMotionDriving extends PanBase {

	public PanMotionDriving(){		
	}

	private class Axis extends GridPane {
		
		private ButtonBounce[] btn = {
			new ButtonBounce(),
			new ButtonBounce()
		};
		
		public Direction dir = Direction.LEFT;

		public Axis(){
			getStyleClass().add("grid-medium");
		
			final Label txtStep = new Label("");
			final Label txtLoca = new Label("");
			
			btn[0].setIcon("dir-left.png");
			btn[1].setIcon("dir-right.png");
			
			final HBox lay0 = new HBox();
			lay0.getStyleClass().add("hbox-spacing");
			lay0.getChildren().addAll(btn[0],btn[1]);
			GridPane.setHgrow(lay0, Priority.ALWAYS);
			
			addRow(0, new Label("Step："), txtStep);
			addRow(1, new Label("Loca："), txtLoca);
			addRow(2, lay0);
		}
		
		public void setOnAction(
			EventHandler<ActionEvent> eventStart,
			EventHandler<ActionEvent> eventFinal,
			EventHandler<ActionEvent> eventChange
		){
			final ActionEvent event = new ActionEvent(this,null);
			
			btn[0].setOnAction(event0->{
				if(dir!=Direction.LEFT){
					dir = Direction.LEFT;
					eventChange.handle(event);
				}
				eventStart.handle(event);
			},eventFinal);
			
			btn[1].setOnAction(event1->{
				if(dir!=Direction.RIGHT){
					dir = Direction.RIGHT;
					eventChange.handle(event);
				}
				eventStart.handle(event);
			},eventFinal);
		}		
	};
	
	private DevTTY dev = new DevTTY();
	
	@Override
	protected void eventShown(WindowEvent e){
		dev.open("/dev/ttyACM0,115200,8n1");
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final BoxLogger box = new BoxLogger();
		box.setPrefHeight(120);
		
		final Axis axs1 = new Axis();
		axs1.setOnAction(event->{
			dev.writeTxt('i');//start motion			
		}, event->{
			dev.writeTxt('i');//stop motion
		}, event->{
			dev.writeTxt('d');//change direction~~~
		});
		
		final VBox lay1 = new VBox();
		lay1.getChildren().add(axs1);
		
		final BorderPane lay0 = new BorderPane();
		//lay0.setRight(layout_action());
		//lay0.setCenter(lay1);
		lay0.setLeft(lay1);		
		lay0.setBottom(box);
		return lay0;
	}
}
