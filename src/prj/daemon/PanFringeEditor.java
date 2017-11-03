package prj.daemon;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.WindowEvent;
import narl.itrc.BoxLogger;
import narl.itrc.PanBase;

public class PanFringeEditor extends PanBase{

	public PanFringeEditor(){		
	}
	
	private WidFringeMap map = new WidFringeMap();
	
	@Override
	protected void eventShown(WindowEvent e){	
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final BoxLogger box = new BoxLogger();
		box.setPrefHeight(120);
			
		final BorderPane lay1 = new BorderPane();		
		lay1.setBottom(box);
		lay1.setCenter(map);
		
		final BorderPane lay2 = new BorderPane();
		lay2.setRight(layout_action());
		lay2.setCenter(lay1);
		return lay2;
	}
	
	private Node layout_action(){
		
		final Button btn1 = PanBase.genButton2("pre-load",null);
		btn1.setOnAction(e->{
			map.loadImageFile("../bang/roi-01.png");
		});
		
		final Button btn2 = PanBase.genButton2("drawing",null);
		btn1.setOnAction(e->{
			map.drawMap();
		});
		
		final Button btn3 = PanBase.genButton2("test-3",null);
		btn1.setOnAction(e->{
		});
		
		final Button btn4 = PanBase.genButton2("test-4",null);
		btn1.setOnAction(e->{
		});

		
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium-vertical");
		lay.addColumn(0,btn1,btn2,btn3,btn4);
		return lay;
	}
}
