package narl.itrc;

import com.sun.glass.ui.Application;

import eu.hansolo.enzo.notification.Notification;
import eu.hansolo.enzo.notification.NotifierBuilder;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.util.Duration;

public class ImgPreview extends BorderPane {
	
	private static final int DEF_WIDTH =800;
	private static final int DEF_HEIGHT=600;

	public ImgPreview(){
		this(DEF_WIDTH,DEF_HEIGHT);
	}
	
	public ImgPreview(ImgControl control){		
		this(DEF_WIDTH,DEF_HEIGHT);
		ctrl = control;
	}

	public ImgPreview(int width,int height){
		initMenu();
		initBoard(width,height);
	}

	public void release(){
		if(renderTask!=null){
			if(renderTask.isRunning()==true){
				renderTask.cancel();
			}
			while(renderTask.isRunning()==true);
		}
	}

	public ImageView screen = new ImageView();
	public Label msgLast = new Label();
	public Label msgInfo = new Label();
	public Label[] msgData;
	private ContextMenu menu = new ContextMenu();
	
	private void initBoard(int width,int height){
		
		FlowPane pan0 = new FlowPane();
		
		pan0.getStyleClass().add("flow-small");
		
		ObservableList<Node> lst0 = pan0.getChildren();
		lst0.addAll(msgLast,msgInfo);
		msgData = new Label[CamBundle.PR_SIZE];
		for(int i=0; i<CamBundle.PR_SIZE; i++){
			msgData[i] = new Label();
			msgData[i].setOnMouseClicked(eventCheckData);
			msgData[i].setUserData(i);
			lst0.add(msgData[i]);			
		}
		
		ScrollPane pan1 = new ScrollPane();
		pan1.setPrefSize(width, height);
		pan1.setContent(screen);
		pan1.setContextMenu(menu);
		
		setTop(pan0);
		setCenter(pan1);
	}
	
	private void initMenu(){
		MenuItem itm0 = new MenuItem("None");//reset all hook~~~
		itm0.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				//display all pin value~~~
				menu.setUserData(0);
				screen.setOnMouseClicked(null);
				screen.setOnDragDetected(null);
				screen.setOnMouseDragged(null);
				screen.setOnMouseReleased(null);
				for(Label txt:msgData){
					txt.textProperty().set("");
				}
			}
		});
		
		MenuItem itm1 = new MenuItem("Pin Mode");
		itm1.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				menu.setUserData(1);
				screen.setOnMouseClicked(eventPreparePin);				
			}
		});
		
		MenuItem itm2 = new MenuItem("ROI Mode");
		itm2.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				//display all ROI values~~~
				menu.setUserData(2);
				screen.setOnDragDetected(eventPrepareROI);
				screen.setOnMouseDragged(eventPrepareROI);
				screen.setOnMouseReleased(eventPrepareROI);
			}
		});
		
		MenuItem itm3 = new MenuItem("Snap");
		itm3.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				menu.setUserData(3);
			}
		});
		
		final String TXT_RECORD_START = "Record";
		final String TXT_RECORD_PAUSE = "[Pause]";
		MenuItem itm4 = new MenuItem(TXT_RECORD_START);
		itm4.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				//display all ROI values~~~
				menu.setUserData(4);
				String txt = itm4.getText();
				if(txt.equalsIgnoreCase(TXT_RECORD_START)==true){
					//start to record~~~~
					itm4.setText(TXT_RECORD_PAUSE);					
				}else{
					itm4.setText(TXT_RECORD_START);
				}				
			}
		});
		
		menu.getItems().addAll(itm0,itm1,itm2,itm3,itm4);
		menu.setUserData(0);//default, remember to keep this~~~
	}
	
	private EventHandler<MouseEvent> eventPreparePin = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent e) {
			if(renderTask==null){ return; }
			int idx = 0;
			for(int i=0; i<CamBundle.PR_SIZE; i++){
				int[] pos = {0,0};
				renderPlug.getPinPos(i,pos);
				if(pos[0]<0 && pos[1]<0){
					idx = i;
					break;
				}
			}
			renderPlug.setPinPos(idx, e.getX(), e.getY());
		}
	};

	private EventHandler<MouseEvent> eventPrepareROI = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent e) {
			if(renderTask==null){ return; }
			EventType<?> typ = e.getEventType();
			if(typ==MouseEvent.DRAG_DETECTED){
				renderPlug.setROI(true,e.getX(),e.getY());
			}else if(typ==MouseEvent.MOUSE_DRAGGED){
				renderPlug.setROI(false,e.getX(),e.getY());
			}else if(typ==MouseEvent.MOUSE_RELEASED){
				int idx = 0;
				for(int i=0; i<CamBundle.PR_SIZE; i++){
					
				}
				renderPlug.fixROI(idx,CamBundle.ROI_TYPE_RECT);
			}
		}
	};
	
	private EventHandler<MouseEvent> eventCheckData = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			int mIdx = (int)menu.getUserData();
			int pIdx = (int)((Label)event.getSource()).getUserData();
			switch(mIdx){
			case 1://PIN mode
				renderPlug.setPinPos(pIdx,-1.,-1.);
				break;
			case 2://ROI mode
				renderPlug.delROI(pIdx);
				break;
			}
		}
	};
	//---------------------//
	
	private EventHandler<WorkerStateEvent> eventStart = 
		new EventHandler<WorkerStateEvent>()
	{
		@Override
		public void handle(WorkerStateEvent event) {
			//bind every information!!!!			
			msgLast.textProperty().bind(renderPlug.msgLast);
		}
	};
	
	private Image renderBuff = null;
	private Runnable eventUpdate = new Runnable(){
		@Override
		public void run() {
			if(renderBuff==null){
				return;
			}			
			int mIdx = (int)menu.getUserData();
			switch(mIdx){
			case 1://PIN mode
				for(int i=0; i<CamBundle.PR_SIZE; i++){
					msgData[i].textProperty().set(renderPlug.getPinVal(i));
				}
				break;
			case 2://ROI mode	
				break;
			case 3://Snap a picture
				String name = Misc.imWriteX(Misc.pathTemp+"snap.png",renderPlug.getMatSrc());
				name = Misc.trimPath(name);
				menu.setUserData(0);//go to default mode~~~
				PanBase.msgBox.notifyInfo("Snap","儲存成"+name);
				break;
			case 4://record start
				break;
			case 5://record stop
				break;
			}
			screen.setImage(renderBuff);
		}
	};
	
	private EventHandler<WorkerStateEvent> eventFinal = 
		new EventHandler<WorkerStateEvent>()
	{
		@Override
		public void handle(WorkerStateEvent event) {
			//When we cancel thread, it will drop from the execution pool.			
			renderPlug.close();
			msgLast.textProperty().unbind();
			if(ctrl!=null){
				ctrl.swtEnable.selectedProperty().unbind();
				ctrl.swtEnable.selectedProperty().set(false);
			}
		}
	};
	
	public int camIndx = 0;
	public String camConf = null;

	private CamBundle renderPlug;
	private Task<Integer> renderTask;

	public CamBundle getCamera(){ return renderPlug; }
	
	public void bindCamera(CamBundle cam){
		if(renderTask!=null){
			if(renderTask.isRunning()==true){
				return;
			}
		}
		renderTask = new Task<Integer>(){			
			@Override
			protected Integer call() throws Exception {
				//stage.1 - try to open camera~~~
				renderPlug.setup(camIndx, camConf);
				if(ctrl!=null){
					Application.invokeAndWait(new Runnable(){
						@Override
						public void run() {
							ctrl.swtEnable.selectedProperty().bind(renderPlug.optEnbl);
						}
					});					
				}
				
				//stage.2 - continue to grab image from camera			
				while(isCancelled()==false){
					if(Application.GetApplication()==null){						
						return 1;//Platform is shutdown
					}
					if(renderPlug.optEnbl.get()==false){
						return -2;//always check property
					}
					if(ctrl!=null){
						if(ctrl.swtPlayer.get()==false){
							Thread.sleep(50);
							continue;
						}
					}
					renderPlug.fetch();
					renderPlug.markData();
					//TODO: hook something~~~~
					//update some information
					renderBuff = renderPlug.getImage(1);//show overlay~~
					Application.invokeAndWait(eventUpdate);
				}
				return 0;
			}
		};
		
		renderPlug = cam;
		renderTask.setOnScheduled(eventStart);
		renderTask.setOnCancelled(eventFinal);
		new Thread(renderTask,"imgRender").start();
	}

	public void unbindCamera(){
		if(renderTask==null){
			return;
		}		
		renderTask.cancel();
		while(renderTask.isRunning()==true);
		renderPlug = null;//reset it~~~
	}
	
	private ImgControl ctrl = null;
	public void attachControl(ImgControl control){
		if(ctrl!=null){
			return;
		}
		ctrl = control;
		ctrl.attachScreen(this);
	}
}

