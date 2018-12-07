package narl.itrc;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.util.Duration;

public class ControlWheel extends AnchorPane {

	private static final Color cca = Color.grayRgb(0xA0);
	private static final Color ccb = Color.grayRgb(0xDE);
	
	private static final double sqrt2_2 = Math.sqrt(2) / 2.;
	
	private class CButton extends Path {
		
		ImageView icon = new ImageView();
		
		Paint paintA, paintB;
			
		public CButton(
			int quad, 
			int cx, int cy, 
			int rad1, int rad2
		){
			double off1 = rad1 * sqrt2_2;
			double off2 = rad2 * sqrt2_2;
			
			final MoveTo P0 = new MoveTo();
			final LineTo L1 = new LineTo();
			final ArcTo  A1 = new ArcTo(rad2, rad2, 90, 0, 0, false, false);
			final LineTo L2 = new LineTo();
			final ArcTo  A2 = new ArcTo(rad1, rad1, 90, 0, 0, false, true);
			
			//from quad-1 to quad-4
			switch(quad){			
			default:
				return;
			case 00:
				paintA = new RadialGradient(
					0, 0,
					0.5, 0.5,
					0.8,
					true, CycleMethod.NO_CYCLE,
					new Stop(0, ccb),
					new Stop(1, cca)
				);
				paintB = new RadialGradient(
					0, 0,
					0.5, 0.5,
					0.8,
					true, CycleMethod.NO_CYCLE,
					new Stop(0, cca),
					new Stop(1, ccb)
				);
				P0.setX(cx); P0.setY(cy-rad1);
				A1.setX(cx); A1.setY(cy+rad1);
				A1.setRadiusX(rad1);
				A1.setRadiusY(rad1);
				A2.setX(cx); A2.setY(cy-rad1);				
				A2.setSweepFlag(false);
				getElements().addAll(P0,A1,A2);
				break;
				
			case 41:
			case 14:
				paintA = new LinearGradient(
					1, 0, 0, 0, 
					true, CycleMethod.NO_CYCLE, 
					new Stop(0, cca),
					new Stop(1, ccb)
				);
				paintB = new LinearGradient(
					1, 0, 0, 0, 
					true, CycleMethod.REFLECT, 
					new Stop(0, ccb),
					new Stop(1, cca)
				);				
				P0.setX(cx+off1); P0.setY(cy+off1);
				L1.setX(cx+off2); L1.setY(cy+off2);
				A1.setX(cx+off2); A1.setY(cy-off2);
				L2.setX(cx+off1); L2.setY(cy-off1);
				A2.setX(cx+off1); A2.setY(cy+off1);
				getElements().addAll(P0,L1,A1,L2,A2);
				break;
			case 23:
			case 32:
				paintA = new LinearGradient(
					0, 0, 1, 0, 
					true, CycleMethod.NO_CYCLE, 
					new Stop(0, cca),
					new Stop(1, ccb)
				);
				paintB = new LinearGradient(
					0, 0, 1, 0, 
					true, CycleMethod.REFLECT, 
					new Stop(0, ccb),
					new Stop(1, cca)
				);				
				P0.setX(cx-off1); P0.setY(cy+off1);
				L1.setX(cx-off2); L1.setY(cy+off2);
				A1.setX(cx-off2); A1.setY(cy-off2);
				A1.setSweepFlag(true);
				L2.setX(cx-off1); L2.setY(cy-off1);
				A2.setX(cx-off1); A2.setY(cy+off1);			
				A2.setSweepFlag(false);
				getElements().addAll(P0,L1,A1,L2,A2);
				break;
			case 12:
			case 21:
				paintA = new LinearGradient(
					0, 0, 0, 1, 
					true, CycleMethod.NO_CYCLE, 
					new Stop(0, cca),
					new Stop(1, ccb)
				);
				paintB = new LinearGradient(
					0, 0, 0, 1, 
					true, CycleMethod.REFLECT, 
					new Stop(0, ccb),
					new Stop(1, cca)
				);
				P0.setX(cx+off1); P0.setY(cy-off1);
				L1.setX(cx+off2); L1.setY(cy-off2);
				A1.setX(cx-off2); A1.setY(cy-off2);
				L2.setX(cx-off1); L2.setY(cy-off1);
				A2.setX(cx+off1); A2.setY(cy-off1);
				getElements().addAll(P0,L1,A1,L2,A2);
				break;
			case 34:
			case 43:
				paintA = new LinearGradient(
					0, 1, 0, 0, 
					true, CycleMethod.NO_CYCLE, 
					new Stop(0, cca),
					new Stop(1, ccb)
				);
				paintB = new LinearGradient(
					0, 1, 0, 0, 
					true, CycleMethod.REFLECT, 
					new Stop(0, ccb),
					new Stop(1, cca)
				);
				P0.setX(cx+off1); P0.setY(cy+off1);
				L1.setX(cx+off2); L1.setY(cy+off2);
				A1.setX(cx-off2); A1.setY(cy+off2);
				A1.setSweepFlag(true);
				L2.setX(cx-off1); L2.setY(cy+off1);
				A2.setX(cx+off1); A2.setY(cy+off1);			
				A2.setSweepFlag(false);
				getElements().addAll(P0,L1,A1,L2,A2);
				break;
			}
			
			setFill(paintA);
			
			icon.setMouseTransparent(true);
		}
		
		void resetEvent(){
			setOnMouseClicked(null);
			setOnMousePressed(null);
			setOnMouseReleased(null);
		}
		
		Image imgA = null; 
		Image imgB = null;
		boolean flagAB = true;
		
		void eventHook(
			final Image image1,
			final Image image2,
			final EventHandler<ActionEvent> eventA,
			final EventHandler<ActionEvent> eventB
		){
			//toggle behavior
			imgA = image1;
			imgB = image2;
			icon.setImage(imgA);
			flagAB = true;//TRUE is image-A, false is image-B.
			setOnMouseClicked(event->{
				if(flagAB==true){
					setFill(paintB);
					icon.setImage(imgB);
					if(eventB!=null){
						eventB.handle(null);
					}
				}else{
					setFill(paintA);
					icon.setImage(imgA);
					if(eventA!=null){
						eventA.handle(null);
					}
				}
				flagAB = !flagAB;
			});
		}
				
		void eventHook(
			final Image image,
			final EventHandler<ActionEvent> event1,
			final EventHandler<ActionEvent> event2,
			final EventHandler<ActionEvent> event3
		){
			//analog stick behavior
			//or
			//click behavior
			imgA = image;
			icon.setImage(imgA);
			final Duration durat = Duration.millis(50);
			final Timeline timer = new Timeline(
				new KeyFrame(durat, event3)
			);
			timer.setCycleCount(Timeline.INDEFINITE);
			if(event1!=null){
				timer.setDelay(durat);
			}
			setOnMousePressed(event->{
				if(event.getButton()==MouseButton.PRIMARY){
					setFill(paintB);
					if(event1!=null){
						event1.handle(null);
					}
					timer.play();
				}
			});
			setOnMouseReleased(event->{
				if(event.getButton()==MouseButton.PRIMARY){
					setFill(paintA);
					timer.pause();
					if(event2!=null){
						event2.handle(null);
					}					
				}
			});
		}
	};
		
	private final CButton[] area = new CButton[5]; 

	public ControlWheel(int iconSize){

		int bnd = (iconSize*15)/10;
		int pad = (bnd - iconSize)/2;
		
		final int r1 = (bnd  )/2;
		final int r2 = (bnd*3)/2;
		final int oo = r2;
		
		//from quad-1 to quad-4
		//center of circle
		area[0] = new CButton(00, oo, oo, r1, 0);
		
		//around of circle
		area[1] = new CButton(14, oo, oo, r1, r2);//right
		area[2] = new CButton(12, oo, oo, r1, r2);//top, up
		area[3] = new CButton(23, oo, oo, r1, r2);//left
		area[4] = new CButton(34, oo, oo, r1, r2);//bottom, down

		getChildren().addAll(area);
		
		AnchorPane.setLeftAnchor(area[0].icon,(double)(bnd+pad));
		AnchorPane.setTopAnchor (area[0].icon,(double)(bnd+pad));
		getChildren().add(area[0].icon);//center part
		
		AnchorPane.setLeftAnchor(area[1].icon,(double)(2*bnd+pad));
		AnchorPane.setTopAnchor (area[1].icon,(double)(bnd+pad));
		getChildren().add(area[1].icon);//right part
		
		AnchorPane.setLeftAnchor(area[2].icon,(double)(bnd+pad));
		AnchorPane.setTopAnchor (area[2].icon,(double)(0+pad));
		getChildren().add(area[2].icon);//top part
		
		AnchorPane.setLeftAnchor(area[3].icon,(double)(0+pad));
		AnchorPane.setTopAnchor (area[3].icon,(double)(bnd+pad));
		getChildren().add(area[3].icon);//left part
		
		AnchorPane.setLeftAnchor(area[4].icon,(double)(bnd+pad));
		AnchorPane.setTopAnchor (area[4].icon,(double)(2*bnd+pad));
		getChildren().add(area[4].icon);//bottom part
				
		//getStyleClass().add("group-border-0");//debug~~~
	}

	public ControlWheel setOnFocusView(final EventHandler<ActionEvent> eventFocus){
		area[0].resetEvent();
		area[0].eventHook(
			Misc.getIconImage("image-filter-center-focus-weak.png"), 
			eventFocus, null, null
		);
		return this;
	}
	
	public ControlWheel setOnPlayPause(
		final EventHandler<ActionEvent> eventPlay,
		final EventHandler<ActionEvent> eventPause
	){ 
		area[0].resetEvent();
		area[0].eventHook(
			Misc.getIconImage("play.png"), 
			Misc.getIconImage("pause.png"),
			eventPlay, 
			eventPause
		);
		return this;
	}
	
	public ControlWheel setOnArrowUp(
		final EventHandler<ActionEvent> event1,
		final EventHandler<ActionEvent> event2,
		final EventHandler<ActionEvent> event3
	){
		area[2].resetEvent();
		area[2].eventHook(
			Misc.getIconImage("dir-up.png"), 
			event1, event2, event3
		);
		return this;
	}
	
	public ControlWheel setOnArrowDown(
		final EventHandler<ActionEvent> event1,
		final EventHandler<ActionEvent> event2,
		final EventHandler<ActionEvent> event3
	){
		area[4].resetEvent();
		area[4].eventHook(
			Misc.getIconImage("dir-down.png"), 
			event1, event2, event3
		);
		return this;
	}
	
	public ControlWheel setOnArrowLeft(
		final EventHandler<ActionEvent> event1,
		final EventHandler<ActionEvent> event2,
		final EventHandler<ActionEvent> event3
	){
		area[3].resetEvent();
		area[3].eventHook(
			Misc.getIconImage("dir-left.png"), 
			event1, event2, event3
		);
		return this;
	}
	
	public ControlWheel setOnArrowRight(
		final EventHandler<ActionEvent> event1,
		final EventHandler<ActionEvent> event2,
		final EventHandler<ActionEvent> event3
	){
		area[1].resetEvent();
		area[1].eventHook(
			Misc.getIconImage("dir-right.png"), 
			event1, event2, event3
		);
		return this;
	}
}
