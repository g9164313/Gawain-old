package narl.itrc;

import java.util.HashMap;

import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class Pan4AxisPad extends FlowPane {
	
	private DevMotion dev;
	
	private final int TKN_X_P = 0x11;
	private final int TKN_X_N = 0x12;
	private final int TKN_Y_P = 0x21;
	private final int TKN_Y_N = 0x22;
	private final int TKN_Z_P = 0x31;
	private final int TKN_Z_N = 0x32;
	private final int TKN_A_P = 0x41;
	private final int TKN_A_N = 0x42;
	private final int TKN_INF = 0x00;
	
	public Pan4AxisPad(DevMotion dev,int size){
		this.dev = dev;
		initIcons();
		initLayout(size);
	}

	private void motionStart(int tkn){
		switch(tkn){
		case TKN_X_P: dev.asyncMoveTo( 5000.); break;
		case TKN_X_N: dev.asyncMoveTo(-5000.); break;
		}
	}
	
	private void motionStop(int tkn){
		
	}
	
	private EventHandler<MouseEvent> eventCtrl = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			Button btn = (Button)(event.getSource());
			int tkn = (int)btn.getUserData();
			if(event.getEventType()==MouseEvent.MOUSE_PRESSED){
				motionStart(tkn);
				btn.setGraphic(lstPadPress.get(tkn));
			}else if(event.getEventType()==MouseEvent.MOUSE_RELEASED){
				motionStop(tkn);
				btn.setGraphic(lstPadRelex.get(tkn));
			}
		}
	};
	
	private Button createPad(int tkn){
		Button btn = new Button("",lstPadRelex.get(tkn));		
		btn.setUserData(tkn);
		btn.addEventFilter(MouseEvent.MOUSE_PRESSED,eventCtrl);
		btn.addEventFilter(MouseEvent.MOUSE_RELEASED,eventCtrl);
		GridPane.setHgrow(btn,Priority.ALWAYS);
		GridPane.setFillWidth(btn, true);
		GridPane.setVgrow(btn,Priority.ALWAYS);
		GridPane.setFillHeight(btn, true);
		return btn;
	}
	
	void initLayout(int boardSize){
		//int cellSize = boardSize/3;
		//control-block
		GridPane pan0 = new GridPane();
		pan0.getStyleClass().add("grid-small");
		pan0.setPrefSize(boardSize, boardSize);
		
		pan0.add(createPad(TKN_Z_N), 0, 0, 1, 1);		
		pan0.add(createPad(TKN_Y_P), 1, 0, 1, 1);		
		pan0.add(createPad(TKN_Z_P), 2, 0, 1, 1);
		
		pan0.add(createPad(TKN_X_N), 0, 1, 1, 1);
		pan0.add(createPad(TKN_INF), 1, 1, 1, 1);		
		pan0.add(createPad(TKN_X_P), 2, 1, 1, 1);
		
		pan0.add(createPad(TKN_A_N), 0, 2, 1, 1);
		pan0.add(createPad(TKN_Y_N), 1, 2, 1, 1);
		pan0.add(createPad(TKN_A_P), 2, 2, 1, 1);
		
		//information-block
		GridPane pan1 = new GridPane();

		//pan1.getChildren().add();
		
		//put together
		getChildren().addAll(pan0,pan1);
	}
		
	private HashMap<Integer,Node> lstPadPress = new HashMap<Integer,Node>();
	private HashMap<Integer,Node> lstPadRelex = new HashMap<Integer,Node>();
	
	private void initIcons(){

		lstPadRelex.put(TKN_X_P,Misc.getIcon("chevron-right.png"));
		lstPadRelex.put(TKN_X_N,Misc.getIcon("chevron-left.png"));
		lstPadRelex.put(TKN_Y_P,Misc.getIcon("chevron-up.png"));
		lstPadRelex.put(TKN_Y_N,Misc.getIcon("chevron-down.png"));
		lstPadRelex.put(TKN_Z_P,Misc.getIcon("arrow-up.png"));
		lstPadRelex.put(TKN_Z_N,Misc.getIcon("arrow-down.png"));
		lstPadRelex.put(TKN_A_P,Misc.getIcon("replay-flop.png"));
		lstPadRelex.put(TKN_A_N,Misc.getIcon("replay.png"));
		lstPadRelex.put(TKN_INF,Misc.getIcon("image-filter-center-focus-weak.png"));
		
		lstPadPress.put(TKN_X_P,Misc.getIcon("chevron-double-right.png"));
		lstPadPress.put(TKN_X_N,Misc.getIcon("chevron-double-left.png"));
		lstPadPress.put(TKN_Y_P,Misc.getIcon("chevron-double-up.png"));
		lstPadPress.put(TKN_Y_N,Misc.getIcon("chevron-double-down.png"));
		lstPadPress.put(TKN_Z_P,Misc.getIcon("format-vertical-align-top.png"));
		lstPadPress.put(TKN_Z_N,Misc.getIcon("format-vertical-align-bottom.png"));
		lstPadPress.put(TKN_A_P,Misc.getIcon("rotate-right.png"));
		lstPadPress.put(TKN_A_N,Misc.getIcon("rotate-left.png"));
		lstPadPress.put(TKN_INF,lstPadRelex.get(TKN_INF));
	}		
}
