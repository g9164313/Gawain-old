package narl.itrc;

import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

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
			if(isRender()==false){
				return;
			}
			int idx = 0;
			for(int i=0; i<CamBundle.PR_SIZE; i++){
				int[] pos = {0,0};
				render.bund.getPinPos(i,pos);
				if(pos[0]<0 && pos[1]<0){
					idx = i;
					break;
				}
			}
			render.bund.setPinPos(idx, e.getX(), e.getY());
		}
	};

	private EventHandler<MouseEvent> eventPrepareROI = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent e) {
			if(isRender()==false){
				return;
			}
			EventType<?> typ = e.getEventType();
			if(typ==MouseEvent.DRAG_DETECTED){
				render.bund.setROI(true,e.getX(),e.getY());
			}else if(typ==MouseEvent.MOUSE_DRAGGED){
				render.bund.setROI(false,e.getX(),e.getY());
			}else if(typ==MouseEvent.MOUSE_RELEASED){
				int idx = 0;
				for(int i=0; i<CamBundle.PR_SIZE; i++){
					
				}
				render.bund.fixROI(idx,CamBundle.ROI_TYPE_RECT);
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
				render.bund.setPinPos(pIdx,-1.,-1.);
				break;
			case 2://ROI mode
				render.bund.delROI(pIdx);
				break;
			}
		}
	};
	//---------------------//
	
	private ImgControl ctrl = null;
	public void attachControl(ImgControl control){
		if(ctrl!=null){
			return;
		}
		ctrl = control;
		ctrl.attachScreen(this);
	}
	
	public Runnable eventUpdate = new Runnable(){
		@Override
		public void run() {
			if(isRender()==false){
				return;
			}
			int mIdx = (int)menu.getUserData();
			switch(mIdx){
			case 1://PIN mode
				for(int i=0; i<CamBundle.PR_SIZE; i++){
					msgData[i].textProperty().set(render.bund.getPinVal(i));
				}
				break;
			case 2://ROI mode	
				break;
			case 3://Snap a picture
				String name = Misc.imWriteX(
					Misc.pathTemp+"snap.png",
					render.bund.getMatSrc()
				);
				name = Misc.trimPath(name);
				menu.setUserData(0);//go to default mode~~~
				PanBase.msgBox.notifyInfo("Snap","儲存成"+name);
				break;
			case 4://record start
				break;
			case 5://record stop
				break;
			}
			screen.setImage(render.getBuffer());
		}
	};
	
	public Runnable eventFinal = new Runnable(){
		@Override
		public void run() {
			//When we cancel thread, it will drop from the execution pool.			
			render.bund.close();
			msgLast.textProperty().unbind();
		}
	};
	
	public ImgRender render;
	public void bindCamera(CamBundle cam){
		if(isRender()==true){
			return;
		}
		render = new ImgRender(cam,this,ctrl);
		render.setOnScheduled(new EventHandler<WorkerStateEvent>(){
			@Override
			public void handle(WorkerStateEvent event) {
				//This is invoked by GUI thread...
				msgLast.textProperty().bind(render.bund.msgLast);
			}	
		});
		render.setOnCancelled(new EventHandler<WorkerStateEvent>(){
			@Override
			public void handle(WorkerStateEvent event) {
				eventFinal.run();
			}
		});		
		new Thread(render,"imgRender").start();
	}

	public void unbindCamera(){
		if(render==null){
			return;
		}		
		render.cancel();
		while(render.isDone()==false);		
	}
	
	public boolean isRender(){
		if(render==null){
			return false;
		}
		return !render.isDone();
	}
}

