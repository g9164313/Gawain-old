package narl.itrc;

import com.sun.glass.ui.Application;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
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
		private int getWheelQ(MouseEvent e){
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
		private int getBoxKey(MouseEvent event){
			double pos;
			if(ori==Orientation.HORIZONTAL){
				pos = event.getY();				
			}else{
				pos = event.getX();
			}
			if(pos<acc_bound){
				return 7;//direction-up
			}
			return 8;//direction-down
		}
		private int prevQuad = 0;
		@Override
		public void handle(MouseEvent event) {
			//TODO: map all directions event to 'real' device
			EventType<?> typ = event.getEventType();
			if(typ==MouseEvent.MOUSE_PRESSED){
				Canvas can = (Canvas)(event.getSource());
				if(can.equals(canWheel)==true){
					prevQuad = getWheelQ(event);
				}else{
					prevQuad = getBoxKey(event);
				}				
				drawStick(prevQuad,1);
				tskTrigger.arg1.set(1);
				tskTrigger.start();
			}else if(typ==MouseEvent.MOUSE_RELEASED){
				tskTrigger.arg1.set(0);//it mean we stop jogging
				drawStick(prevQuad,0);//this will change axis attribute, so do it finally~~
			}
		}
	};

	private EventHandler<ActionEvent> eventSetStep = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			//TODO: map this event to 'real' device
			Button btn = (Button)event.getSource();
			char tkn = (char)(btn.getUserData());
			Misc.logv("reset axis-%c value to zero",tkn);
		}
	};
	
	private final int ADDR_JOGGING = 0;	
	private final int ADDR_RELATIVE = 1;
	private final int ADDR_ABSOLUTE = 2;
	private int addr = ADDR_JOGGING;
	
	private final char AXS_NONE= '.';
	private final char AXS_X = 'x';
	private final char AXS_Y = 'y';
	private final char AXS_Z = 'z';
	private final char AXS_A = 'a';	
	private final char AXS_POS = '+';
	private final char AXS_NEG = '-';
	private char[] axis = {AXS_NONE,AXS_NONE};
	
	private TskBase tskTrigger = new TskBase(){
		private int valStep = 0;
		protected boolean eventInit(){
			valStep = Integer.valueOf(boxStep.getText());
			if(axis[1]=='+'){
				return true;
			}else if(axis[1]=='-'){
				valStep = -1 * valStep;
				return true;
			}
			return false;
		}
		private Runnable updateInfo = new Runnable(){
			@Override
			public void run() {
			}
		};
		@Override
		public int looper(Task<Integer> task) {
			switch(addr){
			case ADDR_JOGGING:
				//TODO: start jogging~~~
				while(arg1.get()==1){
					Application.invokeAndWait(updateInfo);
				}
				//TODO: stop jogging~~~
				break;
			case ADDR_RELATIVE:
				//TODO: this must be asynchronous 
				break;
			case ADDR_ABSOLUTE:
				//TODO: this must be asynchronous
				break;
			}
			Application.invokeAndWait(updateInfo);//finally, update the step value
			return 0;
		}
	};
	//-------------------//
	
	private Canvas canWheel = new Canvas();//stick(4-direction & clockwise)
	private Canvas canZBox = new Canvas();//accessory(Up & Down)
	private final String DEF_STEP_VAL = "00000000";
	private Label[] txtAxis = {
		new Label(DEF_STEP_VAL)/* X-axis: step or distance */,
		new Label(DEF_STEP_VAL)/* Y-axis: step or distance */,
		new Label(DEF_STEP_VAL)/* Z-axis: step or distance */,
		new Label(DEF_STEP_VAL)/* A-axis: step or distance */,
	};
	private TextField boxStep = new TextField("10000");

	private void initLayout(double size){
		
		initWheel(size);
		initZBox(size);
		
		drawStick(0,0);
		drawStick(1,0);
		drawStick(2,0);
		drawStick(3,0);
		drawStick(4,0);				
		drawStick(7,0);
		drawStick(8,0);

		PanFlipper panOption = new PanFlipper(){
			@Override
			Node initFront() {
				GridPane lay0 = new GridPane();
				lay0.getStyleClass().add("grid-small");
				final String title = "重設";
				Button btn;				
				btn = new Button(title);
				btn.setUserData(AXS_X);
				btn.setOnAction(eventSetStep);				
				lay0.addRow(0,new Label("X："),txtAxis[0],btn);
				
				btn = new Button(title);
				btn.setUserData(AXS_Y);
				btn.setOnAction(eventSetStep);
				lay0.addRow(1,new Label("Y："),txtAxis[1],btn);
				
				btn = new Button(title);
				btn.setUserData(AXS_Z);
				btn.setOnAction(eventSetStep);
				lay0.addRow(2,new Label("Z："),txtAxis[2],btn);
				
				btn = new Button(title);
				btn.setUserData(AXS_A);
				btn.setOnAction(eventSetStep);
				lay0.addRow(3,new Label("A："),txtAxis[3],btn);
				return lay0;
			}
			@Override
			Node initBack() {
				GridPane lay0 = new GridPane();
				lay0.getStyleClass().add("grid-small");
				ToggleGroup grp = new ToggleGroup();
				
				RadioButton rad0= new RadioButton("連續(jogging)");
				rad0.setOnAction(EVENT->{ addr = ADDR_JOGGING; });
				rad0.setToggleGroup(grp);
				rad0.setSelected(true);//default~~~
				
				RadioButton rad1 = new RadioButton("相對(relative)");
				rad1.setOnAction(EVENT->{ addr = ADDR_RELATIVE; });
				rad1.setToggleGroup(grp);
				
				RadioButton rad2 = new RadioButton("絕對(absolute)");
				rad2.setOnAction(EVENT->{ addr = ADDR_ABSOLUTE; });
				rad2.setToggleGroup(grp);
				
				boxStep.disableProperty().bind(rad0.selectedProperty());
				lay0.add(rad0, 0, 0, 2, 1);
				lay0.add(rad1, 0, 1, 2, 1);
				lay0.add(rad2, 0, 2, 2, 1);
				lay0.add(boxStep, 0, 3);
				return lay0;
			}
		};
		if(ori==Orientation.HORIZONTAL){			
			panOption.setPrefHeight(size);
		}else{
			panOption.setPrefWidth(size);
		}
		
		setVgap(3);
	    setHgap(3);
	    setPrefWrapLength(size);
		getChildren().addAll(canWheel,canZBox,panOption);		
	}

	private Orientation ori;
	private final int SPACE = 3; 
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
	
	private void initWheel(double size){
		canWheel.setWidth(size);
		canWheel.setHeight(size);
		canWheel.setOnMousePressed(eventSticK);		
		canWheel.setOnMouseReleased(eventSticK);
		
		imgArrow[1][0] = imgArrow[0][0];
		imgArrow[0][5] = imgArrow[0][0];
		imgArrow[0][6] = imgArrow[0][0];
		
		ctx[0] = size/2.;
		ctx[1] = size/2.;
		rad  = size/2. - SPACE;
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
	
	private void initZBox(double size){
		//they use the same image~~~
		imgArrow[0][7] = imgArrow[0][1];
		imgArrow[0][8] = imgArrow[0][3];
		imgArrow[1][7] = imgArrow[1][1];
		imgArrow[1][8] = imgArrow[1][3];
				
		if(ori==Orientation.HORIZONTAL){			
			canZBox.setWidth(48);
			canZBox.setHeight(size);
		}else{
			canZBox.setWidth(size);
			canZBox.setHeight(48);
		}
		canZBox.setOnMousePressed(eventSticK);		
		canZBox.setOnMouseReleased(eventSticK);
		
		//Accessory-up & down
		double icw = imgArrow[0][1].getWidth();
		double ich = imgArrow[0][1].getHeight();
		if(ori==Orientation.HORIZONTAL){
			acc_bound = canZBox.getHeight()/2.;
			vtx[7][0] = canZBox.getWidth()/2. -icw/2.; 
			vtx[7][1] = canZBox.getHeight()/4.-ich/2.;
			vtx[8][0] = vtx[7][0];
			vtx[8][1] = vtx[7][1] + canZBox.getHeight()/2.;		
		}else{
			acc_bound = canZBox.getWidth()/2.;
			vtx[7][0] = canZBox.getWidth()/4. -icw/2.; 
			vtx[7][1] = canZBox.getHeight()/2.-ich/2.;
			vtx[8][0] = vtx[7][0] + canZBox.getWidth()/2.;
			vtx[8][1] = vtx[7][1];
		}
		
		//draw board~~~
		GraphicsContext gc = canZBox.getGraphicsContext2D();
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(2.);
		gc.strokeRect(
			0, 0,
			canZBox.getWidth(),
			canZBox.getHeight()
		);
	}
	
	private void drawStick(int quad,int typ){
		switch(quad){
		case 0://center
			axis[0] = axis[1] = AXS_NONE; 
			patchWheel(quad,typ);
			return;
		case 5://clockwise
			axis[0]=AXS_A; axis[1]=AXS_POS; 
			patchWheel(quad,typ);
			return;
		case 6://anti-clockwise
			axis[0]=AXS_A; axis[1]=AXS_NEG;
			patchWheel(quad,typ);
			return;
		case 1://quadrant-1, clock-12 
			axis[0]=AXS_Y; axis[1]=AXS_POS; 
			patchWheel(quad,typ, 45.);
			break;
		case 2://quadrant-2, clock-3 
			axis[0]=AXS_X; axis[1]=AXS_POS; 
			patchWheel(quad,typ,315.);
			break;
		case 3://quadrant-3, clock-6
			axis[0]=AXS_Y; axis[1]=AXS_NEG; 
			patchWheel(quad,typ,225.);
			break;
		case 4://quadrant-4, clock-9
			axis[0]=AXS_X; axis[1]=AXS_NEG; 
			patchWheel(quad,typ,135.);
			break;
		case 7://axis-z up
			axis[0]=AXS_Z; axis[1]=AXS_POS; 
			patchZBox(quad,typ);
			break;
		case 8://axis-z down
			axis[0]=AXS_Z; axis[1]=AXS_NEG; 
			patchZBox(quad,typ);
			break;
		}
	}
	
	private void patchWheel(int quad,int typ){
		GraphicsContext gc = canWheel.getGraphicsContext2D();
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
	}
	
	private void patchWheel(int quad,int typ,double start){
		GraphicsContext gc = canWheel.getGraphicsContext2D();
		patchWheel(quad,typ);
		gc.strokeArc(
			ctx[0]-rad_f, ctx[1]-rad_f, 
			rad_f*2., rad_f*2., 
			start, 90., 
			ArcType.OPEN
		);//outside-circle
	}

	private void patchZBox(int quad,int typ){
		GraphicsContext gc = canZBox.getGraphicsContext2D();
		double icw = imgArrow[typ][quad].getWidth();
		double ich = imgArrow[typ][quad].getHeight();
		gc.clearRect(
			vtx[quad][0],vtx[quad][1],
			icw,ich
		);
		gc.drawImage(
			imgArrow[typ][quad],
			vtx[quad][0],vtx[quad][1],
			icw,ich
		);
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(2.);
		gc.strokeRect(
			0, 0,
			canZBox.getWidth(),
			canZBox.getHeight()
		);
	}
}
