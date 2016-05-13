package prj.daemon;

import com.jfoenix.controls.JFXButton;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import narl.itrc.BoxPhyValue;
import narl.itrc.CamBundle;
import narl.itrc.ImgRender;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class FltrSlangEdge extends PanBase implements
	ImgRender.Filter
{
	public FltrSlangEdge(){
	}
	//---------------------//
	
	private ImgRender render = null;
	private int[] zone={-1,-1,-1,-1};
	private float pixel_per_mm = 0.0055f;
	private float[] result = null;//[x,y] [x,y] [x,y]...
	private native float[] procSFR(CamBundle bnd, long ptrMat0, long patMat1);
	
	@Override
	public boolean initData(ImgRender rnd) {
		pixel_per_mm = boxPPMM.getFloat();
		Misc.logv("pixel-per-mm=%f",pixel_per_mm);
		appear();
		render = rnd;//always update this variable~~
		render.getBundle().getROI(0,zone);
		chrMTF.getData().clear();			
		return false;
	}

	@Override
	public boolean cookData(CamBundle bnd) {
		result = procSFR(
			bnd,
			bnd.getMatSrc(),
			bnd.getMatOva()
		);
		return false;
	}

	@Override
	public boolean showData(CamBundle bnd) {
		if(result==null){
			PanBase.msgBox.notifyError("SFR","不明原因的錯誤");
			return true;
		}
		Series<Number,Number> serial = new XYChart.Series<Number,Number>();
		for(int i=0; i<result.length; i+=2){			
			serial.getData().add(new XYChart.Data<Number,Number>(
				result[i], result[i+1]
			));
		}
		chrMTF.getData().add(serial);
		return true;
	}
	//---------------------//
	
	private LineChart<Number,Number> chrMTF = new LineChart<Number,Number>(
		new NumberAxis(),
		new NumberAxis()
	);
	
	private BoxPhyValue boxPPMM = new BoxPhyValue("像素大小","5um").setType("mm");
	
	@Override
	public Parent layout() {
		
		chrMTF.getXAxis().setLabel("cycle/mm");
		chrMTF.getYAxis().setLabel("MTF");
		chrMTF.setLegendVisible(false);
		
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.getChildren().addAll(chrMTF);
		//------------------------//
		
		JFXButton btnExport = new JFXButton("匯出");
		btnExport.getStyleClass().add("btn-raised");
		btnExport.setMaxWidth(Double.MAX_VALUE);
		
		JFXButton btnAction = new JFXButton("更新");
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
		
		HBox lay1 = new HBox();
		lay1.getStyleClass().add("hbox-small");
		lay1.getChildren().addAll(
			boxPPMM.decorateTitle(),
			btnExport,
			btnAction
		);
		lay1.setAlignment(Pos.CENTER);
		//------------------------//
		
		BorderPane root = new BorderPane();
		root.setCenter(lay0);
		root.setBottom(lay1);
		return root;
	}
}
