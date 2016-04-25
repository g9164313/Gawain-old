package prj.seesaw;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;

import eu.hansolo.enzo.lcd.Lcd;
import eu.hansolo.enzo.lcd.Lcd.LcdDesign;
import eu.hansolo.enzo.lcd.LcdBuilder;
import narl.itrc.Misc;
import narl.itrc.StgBundle;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;

public class PanMotion extends GridPane implements EventHandler<MouseEvent> {

	private final int OPT_CC = 0;//counter-clockwise
	private final int OPT_UP = 1;
	private final int OPT_CW = 2;//clockwise
	private final int OPT_LF = 3;
	//private final int OPT_XX = 4;//pop-up a setting window
	private final int OPT_RH = 5;
	private final int OPT_ZU = 6;
	private final int OPT_DW = 7;
	private final int OPT_ZD = 8;
	
	private StgBundle stg = null;
	
	private Lcd[] axsValue = new Lcd[4];
	
	private JFXButton[] btnOperate = new JFXButton[9];
	
	private JFXComboBox<String> chkMode = new JFXComboBox<String>();	
	
	private int DEF_PULSE=100;
	@Override
	public void handle(MouseEvent event) {
		int opt = (int)(((JFXButton)event.getSource()).getUserData());
		EventType<?> typ = event.getEventType();
		if(chkMode.getSelectionModel().getSelectedIndex()==0){
			//continue mode
			if(typ==MouseEvent.MOUSE_PRESSED){
				switch(opt){
				case OPT_CC: stg.setJogger('-',3); break;
				case OPT_UP: stg.setJogger('+',1); break;
				case OPT_CW: stg.setJogger('+',3); break;
				case OPT_LF: stg.setJogger('-',0); break;
				case OPT_RH: stg.setJogger('+',0); break;
				case OPT_ZU: stg.setJogger('+',2); break;
				case OPT_DW: stg.setJogger('-',1); break;
				case OPT_ZD: stg.setJogger('-',2); break;
				}
			}else if(typ==MouseEvent.MOUSE_RELEASED){
				stg.setJogger('*',-1);
			}
		}else{
			//one-shoot mode			
			if(typ==MouseEvent.MOUSE_PRESSED){				
				switch(opt){
				case OPT_CC: stg.setPosition('r',3,-DEF_PULSE); break;
				case OPT_UP: stg.setPosition('r',1, DEF_PULSE); break;
				case OPT_CW: stg.setPosition('r',3, DEF_PULSE); break;
				case OPT_LF: stg.setPosition('r',0,-DEF_PULSE); break;
				case OPT_RH: stg.setPosition('r',0, DEF_PULSE); break;
				case OPT_ZU: stg.setPosition('r',2, DEF_PULSE); break;
				case OPT_DW: stg.setPosition('r',1,-DEF_PULSE); break;
				case OPT_ZD: stg.setPosition('r',2,-DEF_PULSE); break;
				}
			}
		}		
	}
	
	public PanMotion(StgBundle stage){
		stg = stage;
		init_layout();
	}

	private void init_layout(){
		getStyleClass().add("grid-small");

		final int DEF_PAD = 32;
		for(int i=0; i<axsValue.length; i++){
			axsValue[i] = LcdBuilder.create()
				.minSize(200,DEF_PAD)
				.keepAspect(true)				
                .lcdDesign(LcdDesign.STANDARD)
                .foregroundShadowVisible(true)
                .crystalOverlayVisible(false)
                .minValue(Integer.MIN_VALUE)
                .maxValue(Integer.MAX_VALUE)
                .title("AXIS-"+i)
				.build();
			axsValue[i].valueProperty().bind(stg.getAxisProperty(i));
			//axsValue[0].setValue(100);
		}
		
		final String[] icon_name = {
			"ic_undo_black_24dp_1x.png",
			"ic_keyboard_arrow_up_black_24dp_1x.png",
			"ic_redo_black_24dp_1x.png",
			"ic_keyboard_arrow_left_black_24dp_1x.png",
			"ic_build_black_24dp_1x.png",
			"ic_keyboard_arrow_right_black_24dp_1x.png",
			"ic_publish_black_24dp_1x.png",
			"ic_keyboard_arrow_down_black_24dp_1x.png",
			"ic_get_app_black_24dp_1x.png"
		};//sequence is important
		for(int i=0; i<btnOperate.length; i++){
			btnOperate[i] = new JFXButton();
			btnOperate[i].setUserData(i);
			btnOperate[i].setPrefSize(DEF_PAD,DEF_PAD);
			btnOperate[i].getStyleClass().add("btn-raised");					
			btnOperate[i].addEventFilter(MouseEvent.MOUSE_PRESSED ,this);
			btnOperate[i].addEventFilter(MouseEvent.MOUSE_RELEASED,this);
			btnOperate[i].setGraphic(Misc.getIcon(icon_name[i]));
		}
		
		chkMode.getItems().addAll("連續","單擊");
		chkMode.setEditable(false);
		chkMode.getSelectionModel().select(0);
		
		for(int i=0; i<axsValue.length; i++){
			add(axsValue[i], 0, i);
		}
		
		for(int i=0; i<btnOperate.length; i++){
			add(btnOperate[i], i%3+1, i/3);
		}

		add(new Label("模式"),1,3);
		add(chkMode,2,3,2,1);
	}	
}
