package prj.daemon;

import com.jfoenix.controls.JFXButton;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Control;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import narl.itrc.CamBundle;
import narl.itrc.ImgRender;
import narl.itrc.PanBase;

public class FltrSlangEdge extends PanBase implements
	ImgRender.Filter
{
	public FltrSlangEdge(){
	}
	//---------------------//
	
	private ImgRender render = null;
	private float pixel_per_mm = 0.005f;
	private float[] result = null;//frequency-value,frequency-value...
	
	private native float[] procSFR(CamBundle bnd, long ptrMat0, long patMat1);
	
	@Override
	public boolean initData(ImgRender rnd) {
		render = rnd;//always update this variable~~
		appear();
		chrMTF.getData().clear();
		return false;
	}

	@Override
	public boolean cookData(CamBundle bnd, long ptrMat0, long patMat1) {
		result = procSFR(bnd,ptrMat0,patMat1);
		return false;
	}

	@Override
	public boolean showData(CamBundle bnd) {
		Series<Number,Number> serial = new XYChart.Series<Number,Number>();
		for(float idx=0; idx<1.; idx=idx+0.1f){			
			serial.getData().add(new XYChart.Data<Number,Number>(
				idx, Math.random()
			));
		}
		chrMTF.getData().add(serial);
		return true;
	}
	//---------------------//
	
	private JFXButton btnAction = new JFXButton("重新計算");
	
	private LineChart<Number,Number> chrMTF = new LineChart<Number,Number>(
		new NumberAxis(),
		new NumberAxis()
	);
	
	@Override
	public Parent layout() {
		
		btnAction.getStyleClass().add("btn-raised");
		btnAction.setMaxWidth(Double.MAX_VALUE);
		btnAction.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if(render==null){
					return;
				}
				render.hookFilter(FltrSlangEdge.this);
			}
		});
		
		chrMTF.getXAxis().setLabel("cycle/mm");
		chrMTF.getYAxis().setLabel("MTF");
		chrMTF.setLegendVisible(false);
		
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.getChildren().addAll(chrMTF);
		
		HBox lay1 = new HBox();
		lay1.getStyleClass().add("hbox-small");
		lay1.getChildren().addAll(btnAction);
		
		BorderPane root = new BorderPane();
		root.setCenter(lay0);
		root.setBottom(lay1);
		return root;
	}
}
