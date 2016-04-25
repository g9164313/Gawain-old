package prj.reheating;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXDialogLayout;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXToggleButton;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;

public class PanControl extends VBox {

	class DlgHeatSV extends JFXDialog {
		public JFXSlider sldHeatA = new JFXSlider();
		public DlgHeatSV(){
			super();
			/*VBox root = new VBox();
			root.getStyleClass().add("vbox-small");
			sldHeatA.setMinWidth(400);
			sldHeatA.setIndicatorPosition(IndicatorPosition.RIGHT);
		    root.getChildren().add(sldHeatA);*/
			JFXDialogLayout root = new JFXDialogLayout();
			JFXButton tt = new JFXButton("test");			
			root.setBody(tt);
			setContent(PanControl.this);
		    setDialogContainer(root);			
		}		
	};
	
	private DlgHeatSV dlgHeatSv = new DlgHeatSV();
	
	//private JFXToggleButton tglAll= new JFXToggleButton();
	private JFXToggleButton tglVaccum= new JFXToggleButton();
	private JFXToggleButton tglCooler= new JFXToggleButton();
	private JFXToggleButton tglHeatT = new JFXToggleButton();//爐管
	private JFXToggleButton tglHeatB = new JFXToggleButton();//油桶	
	private JFXButton btnMotionUp = new JFXButton("掛勾上升");
	private JFXButton btnMotionDw = new JFXButton("掛勾下降");
	private JFXButton btnMotionPos= new JFXButton("移動至...");
	
	private EventHandler<MouseEvent> eventMouse = new EventHandler<MouseEvent>(){
		@Override
		public void handle(MouseEvent event) {
			int bitPos = 0;
			Object btn = event.getSource();
			if(btn==btnMotionUp){
				bitPos = 6;//JOG+, move up=4
			}else if(btn==btnMotionDw){
				bitPos = 7;//JOG-, move dw=5
			}else{
				return;
			}
			if(event.getEventType()==MouseEvent.MOUSE_PRESSED){
				e.padIO.writeOBit(bitPos,true);
			}else if(event.getEventType()==MouseEvent.MOUSE_RELEASED){
				e.padIO.writeOBit(bitPos,false);
			}
		}
	};
	
	private Entry e = null;
	public PanControl(Entry parent){
		e = parent;
	    getStyleClass().add("vbox-small");
	    
	    /*tglAll.setText("總開關");
	    tglAll.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				boolean flag = tglAll.selectedProperty().getValue();
				tglVaccum.selectedProperty().set(flag);
				tglVaccum.fireEvent(event);
				tglHeatA.selectedProperty().set(flag);
				tglHeatA.fireEvent(event);
				tglHeatB.selectedProperty().set(flag);
				tglHeatB.fireEvent(event);
			}
	    });*/
	    
	    tglHeatT.setText("爐管加熱器");
	    tglHeatT.setToggleColor(Paint.valueOf("red"));
	    tglHeatT.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				e.padIO.writeOBit(0,tglHeatT.selectedProperty().getValue());
			}
	    });
	    
	    tglHeatB.setText("油桶加熱器");
	    //tglHeatB.getStyleClass().add("toggle-small");// what is gap????
	    tglHeatB.setToggleColor(Paint.valueOf("red"));
	    tglHeatB.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				e.padIO.writeOBit(1,tglHeatT.selectedProperty().getValue());
			}
	    });
	    
	    tglVaccum.setText("真空閥");
	    tglVaccum.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				e.padIO.writeOBit(2,tglVaccum.selectedProperty().getValue());
			}
	    });
	    
	    tglCooler.setText("氮氣閥");
	    tglCooler.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				e.padIO.writeOBit(3,tglVaccum.selectedProperty().getValue());
			}
	    });
	    
	    JFXButton btnHeatTSv = new JFXButton();
	    btnHeatTSv.getStyleClass().add("button-raised");
	    btnHeatTSv.setText("設定爐管加熱器");
	    btnHeatTSv.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				dlgHeatSv.show();
			}
	    });
	    
	    JFXButton btnHeatBSv = new JFXButton();
	    btnHeatBSv.getStyleClass().add("button-raised");
	    btnHeatBSv.setText("設定油桶加熱器");
	    btnHeatBSv.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				dlgHeatSv.show();
			}
	    });
	    
	    btnMotionUp.getStyleClass().add("button-raised");
	    btnMotionUp.addEventFilter(MouseEvent.MOUSE_PRESSED,eventMouse);
	    btnMotionUp.addEventFilter(MouseEvent.MOUSE_RELEASED,eventMouse);
	    
	    btnMotionDw.getStyleClass().add("button-raised");
	    btnMotionDw.addEventFilter(MouseEvent.MOUSE_PRESSED,eventMouse);
	    btnMotionDw.addEventFilter(MouseEvent.MOUSE_RELEASED,eventMouse);
	    
	    btnMotionPos.getStyleClass().add("button-raised");
	    
	    getChildren().addAll(
	    	tglVaccum,
		    tglCooler,
	    	tglHeatT,
	    	tglHeatB,	    	
	    	btnHeatTSv,
	    	btnHeatBSv,
	    	btnMotionUp,
	    	btnMotionDw,
	    	btnMotionPos
	    );
	}	
}


