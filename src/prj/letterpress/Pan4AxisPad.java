package prj.letterpress;

import java.util.HashMap;

import eu.hansolo.enzo.lcd.Lcd;
import eu.hansolo.enzo.lcd.Lcd.LcdDesign;
import eu.hansolo.enzo.lcd.LcdBuilder;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import narl.itrc.DevMotion;
import narl.itrc.Misc;


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
	private final int TKN_ZRO = 0x00;
	
	public Pan4AxisPad(DevMotion dev,int size){
		this.dev = dev;
		initIcons();
		initLayout(size);
	}

	private void makeMotion(int tkn){
		if(tkn==TKN_ZRO){
			//this is a special case
			dev.archTo(0.,0.,0.);
			return;
		}
		_jogging(tkn,true,10000.);
	}
	
	private void stopMotion(int tkn){
		if(tkn==TKN_ZRO){
			return;
		}
		_jogging(tkn,false,0.);
	}
	
	private void _jogging(int tkn,final boolean go, final double val){
		switch(tkn){
		case TKN_X_P: dev.Jogging(go, val); break;
		case TKN_X_N: dev.Jogging(go,-val); break;
		
		case TKN_Y_P: dev.Jogging(go,null, val); break;
		case TKN_Y_N: dev.Jogging(go,null,-val); break;
		
		case TKN_Z_P: dev.Jogging(go,null,null, val); break;
		case TKN_Z_N: dev.Jogging(go,null,null,-val); break;
		
		case TKN_A_P: dev.Jogging(go,null,null,null, val); break;
		case TKN_A_N: dev.Jogging(go,null,null,null,-val); break;
		}
	}
	
	
	private EventHandler<MouseEvent> eventCtrl = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			Button btn = (Button)(event.getSource());
			int tkn = (int)btn.getUserData();
			if(event.getEventType()==MouseEvent.MOUSE_PRESSED){
				makeMotion(tkn);
				btn.setGraphic(lstPadPress.get(tkn));
			}else if(event.getEventType()==MouseEvent.MOUSE_RELEASED){
				stopMotion(tkn);
				btn.setGraphic(lstPadRelex.get(tkn));
			}
		}
	};
	
	private Button createPadArrow(int tkn){
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
	
	private Button createPadRest(final int tkn){
		String txt="";
		switch(tkn){
		case TKN_X_P:
		case TKN_X_N: txt="X"; break;
		case TKN_Y_P:
		case TKN_Y_N: txt="Y"; break;
		case TKN_Z_P: 
		case TKN_Z_N: txt="Z"; break;
		case TKN_A_P: 
		case TKN_A_N: txt="A"; break;
		}
		Button btn = new Button(txt);
		btn.setOnAction(event->{			
			switch(tkn){
			case TKN_X_P:
			case TKN_X_N: dev.setPosition(0); break;
			case TKN_Y_P:
			case TKN_Y_N: dev.setPosition(null,0); break;
			case TKN_Z_P: 
			case TKN_Z_N: dev.setPosition(null,null,0); break;
			case TKN_A_P: 
			case TKN_A_N: dev.setPosition(null,null,null,0); break;
			}
		});
		GridPane.setHgrow(btn,Priority.ALWAYS);
		GridPane.setFillWidth(btn, true);
		GridPane.setVgrow(btn,Priority.ALWAYS);
		GridPane.setFillHeight(btn, true);
		return btn;
	}
	
	private Lcd createLCD(int tkn,int width){
		Lcd node = LcdBuilder.create()
			.prefWidth(width)
			.keepAspect(true)
			.lcdDesign(LcdDesign.STANDARD)
			.foregroundShadowVisible(true)
			.crystalOverlayVisible(false)
			.valueFont(Lcd.LcdFont.ELEKTRA)
			.animated(true)
			.build();
		int idx = 0;
		switch(tkn){
		case TKN_X_P:
		case TKN_X_N: idx=0; break;
		case TKN_Y_P:
		case TKN_Y_N: idx=1; break;
		case TKN_Z_P: 
		case TKN_Z_N: idx=2; break;
		case TKN_A_P: 
		case TKN_A_N: idx=3; break;
		}
		node.valueProperty().bind(dev.pulse[idx]);
		return node;
	}
	
	void initLayout(int boardSize){
		//int cellSize = boardSize/3;
		
		//information-block
		GridPane panInfo = new GridPane();
		panInfo.getStyleClass().add("grid-small");
		
		panInfo.add(createPadRest(TKN_X_P), 0, 0);
		panInfo.add(createPadRest(TKN_Y_P), 0, 1);
		panInfo.add(createPadRest(TKN_Z_P), 0, 2);
		panInfo.add(createPadRest(TKN_A_P), 0, 3);
		panInfo.add(createLCD(TKN_X_P,boardSize), 1, 0);
		panInfo.add(createLCD(TKN_Y_P,boardSize), 1, 1);
		panInfo.add(createLCD(TKN_Z_P,boardSize), 1, 2);
		panInfo.add(createLCD(TKN_A_P,boardSize), 1, 3);
		
		//control-block
		GridPane panCtrl = new GridPane();
		panCtrl.getStyleClass().add("grid-small");
		
		panCtrl.setPrefSize(boardSize, boardSize);
		
		panCtrl.add(createPadArrow(TKN_Z_N), 0, 0, 1, 1);		
		panCtrl.add(createPadArrow(TKN_Y_P), 1, 0, 1, 1);		
		panCtrl.add(createPadArrow(TKN_Z_P), 2, 0, 1, 1);
		
		panCtrl.add(createPadArrow(TKN_X_N), 0, 1, 1, 1);
		panCtrl.add(createPadArrow(TKN_ZRO), 1, 1, 1, 1);		
		panCtrl.add(createPadArrow(TKN_X_P), 2, 1, 1, 1);
		
		panCtrl.add(createPadArrow(TKN_A_N), 0, 2, 1, 1);
		panCtrl.add(createPadArrow(TKN_Y_N), 1, 2, 1, 1);
		panCtrl.add(createPadArrow(TKN_A_P), 2, 2, 1, 1);
		
		//put together
		getChildren().addAll(panInfo,panCtrl);
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
		lstPadRelex.put(TKN_ZRO,Misc.getIcon("image-filter-center-focus-weak.png"));
		
		lstPadPress.put(TKN_X_P,Misc.getIcon("chevron-double-right.png"));
		lstPadPress.put(TKN_X_N,Misc.getIcon("chevron-double-left.png"));
		lstPadPress.put(TKN_Y_P,Misc.getIcon("chevron-double-up.png"));
		lstPadPress.put(TKN_Y_N,Misc.getIcon("chevron-double-down.png"));
		lstPadPress.put(TKN_Z_P,Misc.getIcon("format-vertical-align-top.png"));
		lstPadPress.put(TKN_Z_N,Misc.getIcon("format-vertical-align-bottom.png"));
		lstPadPress.put(TKN_A_P,Misc.getIcon("rotate-right.png"));
		lstPadPress.put(TKN_A_N,Misc.getIcon("rotate-left.png"));
		lstPadPress.put(TKN_ZRO,lstPadRelex.get(TKN_ZRO));
	}		
}
