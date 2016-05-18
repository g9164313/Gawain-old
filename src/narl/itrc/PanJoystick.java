package narl.itrc;

import com.jfoenix.controls.JFXRadioButton;

import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

public class PanJoystick extends AnchorPane {

	public PanJoystick(int size){
		initLayout(size);
	}
	
	private Canvas can = new Canvas();
	
	private ToggleGroup grpMode = new ToggleGroup();

	private void initLayout(double size){
		/*VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
				
		JFXRadioButton rad;
		rad= new JFXRadioButton("連續");
		rad.setToggleGroup(grpMode);
		lay1.getChildren().add(rad);
		
		rad = new JFXRadioButton("步進");
		rad.setToggleGroup(grpMode);
		lay1.getChildren().add(rad);
		
		rad = new JFXRadioButton("定點");
		rad.setToggleGroup(grpMode);
		lay1.getChildren().add(rad);*/
		
		can.setWidth(size);
		can.setHeight(size);
		
		can.setOnMousePressed(eventDirXYA);
		can.setOnMouseMoved(eventDirXYA);
		can.setOnMouseReleased(eventDirXYA);
		
		//clock-center
		vtx[0][0] = size/2.;  
		vtx[0][1] = size/2.;
		rad= size/2. - PPAD;
		qad = (rad*2.)/3.;
		double qad1_2= qad/2.;
		double qad2_3= qad + qad1_2;
		//clock-12
		vtx[1][0] = vtx[0][0]-qad1_2; 
		vtx[1][1] = vtx[0][1]-qad2_3;
		//clock-3
		vtx[2][0] = vtx[0][0]+qad1_2; 
		vtx[2][1] = vtx[0][1]-qad1_2;
		//clock-6
		vtx[3][0] = vtx[0][0]-qad1_2; 
		vtx[3][1] = vtx[0][1]+qad1_2;
		//clock-9
		vtx[4][0] = vtx[0][0]-qad2_3; 
		vtx[4][1] = vtx[0][1]-qad1_2;
		
		draw();

		AnchorPane.setRightAnchor(can,0.);
		//AnchorPane.setLeftAnchor(lay1,0.);
		getChildren().add(can);
	}

	private Image[][] imgArrow = {
		{
			null,
			Misc.getImage("nav0-0.png"),
			Misc.getImage("nav0-1.png"),
			Misc.getImage("nav0-2.png"),
			Misc.getImage("nav0-3.png"),
			null,null
		},
		{
			null,
			Misc.getImage("nav1-0.png"),
			Misc.getImage("nav1-1.png"),
			Misc.getImage("nav1-2.png"),
			Misc.getImage("nav1-3.png"),
			null,null
		}
	};
	
	private final int PPAD = 3; 
	private double rad=1.;//the inside and outside of clock radius
	private double qad=rad*2./3.;
	private double[][] vtx = {
		{0,0},
		{0,0},{0,0},{0,0},{0,0},
		{0,0},{0,0}
	};//center,clock-wise, and two center part
	
	
	private void draw(){
		GraphicsContext gc = can.getGraphicsContext2D();

		gc.drawImage(imgArrow[0][1],vtx[1][0],vtx[1][1],qad,qad);
		gc.drawImage(imgArrow[0][2],vtx[2][0],vtx[2][1],qad,qad);
		gc.drawImage(imgArrow[0][3],vtx[3][0],vtx[3][1],qad,qad);
		gc.drawImage(imgArrow[0][4],vtx[4][0],vtx[4][1],qad,qad);
		
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(1.);
		gc.strokeArc(
			vtx[0][0]-rad, vtx[0][1]-rad, 
			2*rad, 2*rad, 
			0, 360, 
			ArcType.OPEN
		);
	}
	
	private EventHandler<MouseEvent> eventDirXYA = new EventHandler<MouseEvent>(){
		private int getRegin(double mx,double my){
			double dx = mx - vtx[0][0];
			double dy = my - vtx[0][1];
			double dist = dx*dx + dy*dy;
			if(dist<=(qad*qad)/4.){
				return 0;//inside center~~~
			}
			if(dx==0.){				
				if(dy<0.){
					Misc.logv("clock-6");
					return 2;//clock-6
				}else{
					Misc.logv("clock-12");
					return 2;//clock-12
				}
			}else{
				double ang = ((Math.atan(dy/dx))*180.)/Math.PI;
				Misc.logv("degree=%2.3f",ang);
			}			
			return 0;
		}
		@Override
		public void handle(MouseEvent e) {
			EventType<?> typ = e.getEventType();
			
			if(typ==MouseEvent.MOUSE_PRESSED){
				
			}else if(typ==MouseEvent.MOUSE_MOVED){
				getRegin(e.getX(),e.getY());
			}else if(typ==MouseEvent.MOUSE_RELEASED){
				
			}
		}
	};
}
