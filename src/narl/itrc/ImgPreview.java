package narl.itrc;

import javax.swing.ButtonGroup;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
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
		initBoard(width,height);
	}

	
	private boolean snapAction = false;
	
	public ImageView screen = new ImageView();
	private Button btnSnap = new Button("Snap");
	private JFXRadioButton btnReco = new JFXRadioButton("錄製");
	private JFXRadioButton btnPick = new JFXRadioButton("選取");
	private JFXComboBox<String> lstPickType = new JFXComboBox<String>();
	private JFXComboBox<Integer> lstPickIndx = new JFXComboBox<Integer>();
	private Label msgLast = new Label();
	private ContextMenu menu = new ContextMenu();
	
	private void initBoard(int width,int height){		
		getStyleClass().add("board-center");
		
		btnSnap.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(isRender()==false){
					return;
				}
				snapAction = true;
			}	
		});
		
		lstPickType.getItems().addAll("Pin","矩形(實)","圓形(實)");
		lstPickType.getSelectionModel().select(0);
		lstPickType.setOnAction(eventPrepareHook);
		lstPickIndx.getItems().addAll(1,2,3,4);
		lstPickIndx.getSelectionModel().select(0);
		btnPick.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(btnPick.isSelected()==true){
					eventPrepareHook.handle(null);
					lstPickType.setDisable(false);
					lstPickIndx.setDisable(false);
				}else{					
					lstPickType.setDisable(true);
					lstPickIndx.setDisable(true);
					screen.setOnMouseClicked(null);
					screen.setOnDragDetected(null);
					screen.setOnMouseDragged(null);
					screen.setOnMouseReleased(null);
				}
			}	
		});
		
		FlowPane pan0 = new FlowPane();		
		pan0.getStyleClass().add("flow-small");
		pan0.getChildren().addAll(btnSnap,btnReco,btnPick,lstPickType,lstPickIndx,msgLast);

		ScrollPane pan1 = new ScrollPane();
		pan1.setPrefSize(width, height);
		pan1.setContent(screen);
		pan1.setContextMenu(menu);

		setTop(pan0);
		setCenter(pan1);
	}
	
	private EventHandler<ActionEvent> eventPrepareHook = new EventHandler<ActionEvent>(){
		@Override
		public void handle(ActionEvent event) {
			if(lstPickType.getSelectionModel().getSelectedIndex()==0){
				screen.setOnMouseClicked(eventPreparePin);
				screen.setOnDragDetected(null);
				screen.setOnMouseDragged(null);
				screen.setOnMouseReleased(null);
			}else{
				screen.setOnMouseClicked(null);
				screen.setOnDragDetected(eventPrepareROI);
				screen.setOnMouseDragged(eventPrepareROI);
				screen.setOnMouseReleased(eventPrepareROI);
			}
		}
	};
	
	private EventHandler<MouseEvent> eventPreparePin = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent e) {
			if(isRender()==false){
				return;
			}
			int index = lstPickIndx.getSelectionModel().getSelectedItem()-1;
			render.bund.fixPin(index,e.getX(),e.getY());
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
				render.bund.setROI(true, e.getX(),e.getY());
			}else if(typ==MouseEvent.MOUSE_DRAGGED){
				render.bund.setROI(false,e.getX(),e.getY());
			}else if(typ==MouseEvent.MOUSE_RELEASED){
				int shape = lstPickType.getSelectionModel().getSelectedIndex();
				int index = lstPickIndx.getSelectionModel().getSelectedItem()-1;
				switch(shape){
				case 1: shape = CamBundle.ROI_TYPE_RECT; break;
				case 2: shape = CamBundle.ROI_TYPE_CIRC; break;
				}				
				render.bund.fixROI(index,shape);
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
			if(snapAction==true){
				String name = Misc.imWriteX(
					Misc.pathTemp+"snap.png",
					render.bund.getMatSrc()
				);
				name = Misc.trimPath(name);				
				PanBase.msgBox.notifyInfo("Snap","儲存成"+name);
				snapAction = false;//for next turn~~~
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

