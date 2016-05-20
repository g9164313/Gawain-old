package narl.itrc;

import com.jfoenix.controls.JFXRadioButton;

import eu.hansolo.enzo.flippanel.FlipPanel;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Orientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

public class PanJoystick extends FlowPane {

	public PanJoystick(Orientation ori,int size){
		this.ori = ori;
		initLayout(size);
	}
	
	private EventHandler<MouseEvent> eventSticK = new EventHandler<MouseEvent>(){
		private double cos45 = Math.cos(45.*Math.PI/180.);
		private double sin45 = Math.sin(45.*Math.PI/180.);
		private int getQuadrant(MouseEvent e){
			double ox = e.getX() - ctx[0];
			double oy = ctx[1] - e.getY();
			double dist = Math.sqrt(ox*ox + oy*oy);
			if(dist<=radIn){
				//inside center~~~
				if(ox<0){
					return 5;
				}
				return 6;
			}
			double xx = ox * cos45 + oy * sin45;
			double yy = ox *-sin45 + oy * cos45;
			if(xx>=0 && yy>=0){
				return 1;
			}else if(xx>=0 && yy<0){
				return 2;
			}else if(xx<0 && yy<0){
				return 3;
			}
			return 4;
		}
		private int prevQuad = 0;
		@Override
		public void handle(MouseEvent e) {
			EventType<?> typ = e.getEventType();
			//TODO: drive controller!!!
			if(typ==MouseEvent.MOUSE_PRESSED){
				prevQuad = getQuadrant(e);
				drawStick(prevQuad,1);
			}else if(typ==MouseEvent.MOUSE_RELEASED){
				drawStick(prevQuad,0);
			}
		}
	};
	
	private EventHandler<MouseEvent> eventAccss = new EventHandler<MouseEvent>(){
		private boolean isUpKey(MouseEvent e){
			double pos;
			if(ori==Orientation.HORIZONTAL){
				pos = e.getY();				
			}else{
				pos = e.getX();
			}
			if(pos<acc_bound){
				return true;
			}
			return false;
		}
		private boolean prevFlag;
		@Override
		public void handle(MouseEvent e) {
			EventType<?> typ = e.getEventType();
			//TODO: drive controller!!!
			if(typ==MouseEvent.MOUSE_PRESSED){
				prevFlag = isUpKey(e);
				drawAccss(prevFlag,1);				
			}else if(typ==MouseEvent.MOUSE_RELEASED){
				drawAccss(prevFlag,0);
			}
		}
	};
	
	private Canvas canStick = new Canvas();//stick(4-direction & clockwise)
	private Canvas canAccss = new Canvas();//accessory(Up & Down)
	private FlipPanel flpOption = new FlipPanel(Orientation.VERTICAL);
	private Label[] txtAxis = {
		new Label("00000000")/* X-axis: step or distance */,
		new Label()/* Y-axis: step or distance */,
		new Label()/* Z-axis: step or distance */,
		new Label()/* A-axis: step or distance */,
	};
	private ToggleGroup grpMode = new ToggleGroup();
	
	private void initLayout(double size){
		
		initStick(size);
		drawStick(0,0);
		drawStick(1,0);
		drawStick(2,0);
		drawStick(3,0);
		drawStick(4,0);
		
		initAccss(size);		
		drawAccss(true,0);
		drawAccss(false,0);
		
		initOption(size);
		
		setVgap(7);
	    setHgap(7);
		getChildren().addAll(canStick,canAccss);
	}

	private Orientation ori;
	private final int PPAD = 3; 
	private double rad=1.;//the inside and outside of clock radius
	private double radIn = rad*0.4;
	private double qad=rad*2./3.;
	private double rad_f = 1.;
	private double qad_f = qad*2.;
	private double acc_bound;
	private double[] ctx = {0,0};
	private double[][] vtx = {
		{0,0},
		{0,0},{0,0},{0,0},{0,0},
		{0,0},{0,0},
		{0,0},{0,0},
	};//Center,Quadrant,Clockwise,Accessory(Up-Down)

	private Image[][] imgArrow = {
		{
			Misc.getImage("pause-circle-outline.png"),
			Misc.getImage("chevron-up.png"),
			Misc.getImage("chevron-right.png"),
			Misc.getImage("chevron-down.png"),
			Misc.getImage("chevron-left.png"),
			null,null,
			null,null/*it is same as the up&down icon*/
		},
		{
			null,
			Misc.getImage("chevron-double-up.png"),
			Misc.getImage("chevron-double-right.png"),
			Misc.getImage("chevron-double-down.png"),
			Misc.getImage("chevron-double-left.png"),
			Misc.getImage("rotate-right.png"),Misc.getImage("rotate-left.png"),
			null,null
		}
	};
	
	private void initStick(double size){
		canStick.setWidth(size);
		canStick.setHeight(size);
		canStick.setOnMousePressed(eventSticK);		
		canStick.setOnMouseReleased(eventSticK);
		
		imgArrow[1][0] = imgArrow[0][0];
		imgArrow[0][5] = imgArrow[0][0];
		imgArrow[0][6] = imgArrow[0][0];
		
		ctx[0] = size/2.;
		ctx[1] = size/2.;
		rad  = size/2. - PPAD;
		radIn= rad*0.4;
		qad  = rad/3.;
		rad_f= size/2.;
		qad_f= qad*2.;
		
		//clock-center
		vtx[0][0] = ctx[0] - qad;  
		vtx[0][1] = ctx[1] - qad;
		//quadrant-1, clock-12 
		vtx[1][0] = ctx[0] - qad; 
		vtx[1][1] = ctx[1] - qad*3.;
		//quadrant-2, clock-3,
		vtx[2][0] = ctx[0] + qad; 
		vtx[2][1] = ctx[1] - qad;
		//quadrant-3, clock-6,
		vtx[3][0] = ctx[0] - qad; 
		vtx[3][1] = ctx[1] + qad;
		//quadrant-4, clock-9,
		vtx[4][0] = ctx[0] - qad*3.; 
		vtx[4][1] = ctx[1] - qad;
		//Clockwise		
		vtx[5][0] = vtx[0][0]; 
		vtx[5][1] = vtx[0][1];
		vtx[6][0] = vtx[0][0]; 
		vtx[6][1] = vtx[0][1];
	}
	
	private void initAccss(double size){
		//they use the same image~~~
		imgArrow[0][7] = imgArrow[0][1];
		imgArrow[0][8] = imgArrow[0][3];
		imgArrow[1][7] = imgArrow[1][1];
		imgArrow[1][8] = imgArrow[1][3];
				
		if(ori==Orientation.HORIZONTAL){			
			canAccss.setWidth(48);
			canAccss.setHeight(size);
		}else{
			canAccss.setWidth(size);
			canAccss.setHeight(48);
		}
		canAccss.setOnMousePressed(eventAccss);		
		canAccss.setOnMouseReleased(eventAccss);
		
		//Accessory-up & down
		double icw = imgArrow[0][1].getWidth();
		double ich = imgArrow[0][1].getHeight();
		if(ori==Orientation.HORIZONTAL){
			acc_bound = canAccss.getHeight()/2.;
			vtx[7][0] = canAccss.getWidth()/2. -icw/2.; 
			vtx[7][1] = canAccss.getHeight()/4.-ich/2.;
			vtx[8][0] = vtx[7][0];
			vtx[8][1] = vtx[7][1] + canAccss.getHeight()/2.;		
		}else{
			acc_bound = canAccss.getWidth()/2.;
			vtx[7][0] = canAccss.getWidth()/4. -icw/2.; 
			vtx[7][1] = canAccss.getHeight()/2.-ich/2.;
			vtx[8][0] = vtx[7][0] + canAccss.getWidth()/2.;
			vtx[8][1] = vtx[7][1];
		}
		
		//draw board~~~
		GraphicsContext gc = canAccss.getGraphicsContext2D();
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(2.);
		gc.strokeRect(
			0, 0,
			canAccss.getWidth(),
			canAccss.getHeight()
		);
	} 
	
	private void drawStick(int quad,int typ){
		GraphicsContext gc = canStick.getGraphicsContext2D();
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(1.);
		gc.clearRect(
			vtx[quad][0],vtx[quad][1],
			qad_f,qad_f
		);
		gc.drawImage(
			imgArrow[typ][quad],
			vtx[quad][0],vtx[quad][1],
			qad_f,qad_f
		);
		double start=0.,extra=90.;
		switch(quad){
		case 0://center
		case 5://Clockwise - left
		case 6://Clockwise - right
			return;
		case 1://quadrant-1
			start= 45.;
			break;
		case 2://quadrant-2
			start=315.;
			break;
		case 3://quadrant-3
			start=225.;
			break;
		case 4://quadrant-4
			start=135.;
			break;
		}
		gc.strokeArc(
			ctx[0]-rad_f, ctx[1]-rad_f, 
			rad_f*2., rad_f*2., 
			start, extra, 
			ArcType.OPEN
		);//outside-circle
		/*gc.strokeArc(
			ctx[0]-radIn, ctx[1]-radIn, 
			radIn*2., radIn*2.,
			start, 90., 
			ArcType.OPEN
		);*///inside-circle
	}
	
	private void drawAccss(boolean isUp,int typ){
		GraphicsContext gc = canAccss.getGraphicsContext2D();
		int idx;
		if(isUp==true){
			idx = 7;
		}else{
			idx = 8;
		}
		double icw = imgArrow[typ][idx].getWidth();
		double ich = imgArrow[typ][idx].getHeight();
		gc.clearRect(
			vtx[idx][0],vtx[idx][1],
			icw,ich
		);
		gc.drawImage(
			imgArrow[typ][idx],
			vtx[idx][0],vtx[idx][1],
			icw,ich
		);
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(2.);
		gc.strokeRect(
			0, 0,
			canAccss.getWidth(),
			canAccss.getHeight()
		);
	}
	
	private void initOption(double size){
		StackPane pane;
		
		/*GridPane lay0 = new GridPane();
		lay0.getStyleClass().add("grid-small");
		lay0.addRow(0,new Label("X："),txtAxis[0]);
		lay0.addRow(1,new Label("Y："),txtAxis[1]);
		lay0.addRow(2,new Label("Z："),txtAxis[2]);
		lay0.addRow(3,new Label("A："),txtAxis[3]);
		pane = new StackPane();
        pane.getStyleClass().add("panel");
        pane.getChildren().addAll(lay0);
		flpOption.getFront().getChildren().add(pane);
		
		VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		JFXRadioButton rad0= new JFXRadioButton("連續");
		rad0.setToggleGroup(grpMode);
		JFXRadioButton rad1 = new JFXRadioButton("步進");
		rad1.setToggleGroup(grpMode);		
		lay1.getChildren().addAll(rad0,rad1);
		pane = new StackPane();
        pane.getStyleClass().add("panel");
        pane.getChildren().addAll(lay1);
		flpOption.getBack().getChildren().add(pane);*/
		
		/*if(ori==Orientation.HORIZONTAL){			
			
		}else{
			flpOption.prefWidth(size);
			flpOption.prefHeight(size);
		}*/
	}
}
