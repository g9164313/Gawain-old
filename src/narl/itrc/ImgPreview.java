package narl.itrc;

import java.util.concurrent.atomic.AtomicInteger;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

public class ImgPreview extends BorderPane {
	
	private static final int DEF_WIDTH =640;
	private static final int DEF_HEIGHT=480;

	public ImgPreview(){
		this(null,DEF_WIDTH,DEF_HEIGHT);
	}
	
	public ImgPreview(CamBundle bnd,int width,int height){
		bundle = bnd;
		initBoard(width,height);
	}

	public static final int ACT_NONE = 0;
	public static final int ACT_SNAP = 1;
	public static final int ACT_RECD = 2;
	public AtomicInteger action = new AtomicInteger(ACT_NONE);

	public ImgRender render = null;
	public CamBundle bundle = null;
	
	public ImageView screen = new ImageView();
	public Label msgLast = new Label();
	
	private Button btnSnap = new Button("Snap");
	private JFXRadioButton btnReco = new JFXRadioButton("錄製");
	private JFXRadioButton btnPick = new JFXRadioButton("選取");
	private JFXComboBox<String> lstPickType = new JFXComboBox<String>();
	private JFXComboBox<Integer> lstPickIndx = new JFXComboBox<Integer>();
	private Button btnPickCancel = new Button("取消");	
	private ContextMenu menu = new ContextMenu();
	
	public void initScreen(ImgRender rnd){
		render = rnd;
		msgLast.textProperty().unbind();
		msgLast.textProperty().bind(bundle.msgLast);
	} 
		
	private void initBoard(int width,int height){		
		getStyleClass().add("board-center");
		
		btnSnap.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(render.isWorking()==false){
					return;
				}
				//TODO: how to pass message???
				if(action.get()==ACT_NONE){
					action.set(ACT_SNAP);
				}
			}	
		});
		
		btnReco.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(render.isWorking()==false){
					return;
				}
			}	
		});
		
		lstPickType.getItems().addAll("Pin","矩形(實)","圓形(實)");
		lstPickType.getSelectionModel().select(0);
		//lstPickType.setOnAction(eventPrepareHook);
		lstPickType.setDisable(true);
		lstPickIndx.getItems().addAll(1,2,3,4);
		lstPickIndx.getSelectionModel().select(0);
		lstPickIndx.setDisable(true);
		btnPick.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(btnPick.isSelected()==true){					
					lstPickType.setDisable(false);
					lstPickIndx.setDisable(false);
					btnPickCancel.setDisable(false);
				}else{					
					lstPickType.setDisable(true);
					lstPickIndx.setDisable(true);
					btnPickCancel.setDisable(true);					
				}
			}	
		});
		
		btnPickCancel.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(render.isWorking()==false){
					return;
				}
				int index = lstPickIndx.getSelectionModel().getSelectedItem()-1;
				bundle.delMark(index);
			}
		});
		
		FlowPane pan0 = new FlowPane();		
		pan0.getStyleClass().add("flow-small");
		pan0.getChildren().addAll(
			btnSnap,
			btnReco,
			btnPick,lstPickType,lstPickIndx,
			btnPickCancel,
			msgLast
		);

		ScrollPane pan1 = new ScrollPane();
		pan1.setPrefSize(width, height);
		pan1.setContent(screen);
		pan1.setContextMenu(menu);

		screen.setOnMousePressed(eventPrepareROI);
		screen.setOnMouseDragged(eventPrepareROI);
		screen.setOnMouseReleased(eventPrepareROI);
		
		setTop(pan0);
		setCenter(pan1);
	}
	
	private EventHandler<MouseEvent> eventPrepareROI = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent e) {
			if(render.isWorking()==false){
				return;
			}
			EventType<?> typ = e.getEventType();
			if(lstPickType.getSelectionModel().getSelectedIndex()==0){
				//put a pin in screen
				if(typ==MouseEvent.MOUSE_PRESSED){
					bundle.fixPin(
						lstPickIndx.getSelectionModel().getSelectedItem()-1,
						e.getX(),
						e.getY()
					);
				}
			}else{
				//drag mouse and select a interesting region
				if(typ==MouseEvent.MOUSE_PRESSED){
					bundle.stickPin(
						true, 
						e.getX(),
						e.getY()
					);
				}else if(typ==MouseEvent.MOUSE_DRAGGED){
					bundle.stickPin(
						false,
						e.getX(),
						e.getY()
					);
				}else if(typ==MouseEvent.MOUSE_RELEASED){					
					int shape = lstPickType.getSelectionModel().getSelectedIndex();
					int index = lstPickIndx.getSelectionModel().getSelectedItem()-1;
					switch(shape){
					case 1: shape = CamBundle.ROI_TYPE_RECT; break;
					case 2: shape = CamBundle.ROI_TYPE_CIRC; break;
					}				
					bundle.fixROI(index,shape);
				}
			}
		}
	};
	//---------------------//

	/*public Runnable eventUpdate = new Runnable(){
		@Override
		public void run() {
			if(snapAction==true){
					
				PanBase.msgBox.notifyInfo("Snap","儲存成"+name);
			}
		}
	};*/
	
	public Runnable eventFinal = new Runnable(){
		@Override
		public void run() {
			//When we cancel thread, it will drop from the execution pool.			
			bundle.close();			
		}
	};
}

