package prj.daemon;

import java.util.ArrayList;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import narl.itrc.CamBundle;
import narl.itrc.ImgFilter;
import narl.itrc.ImgPreview;
import narl.itrc.ImgRender;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;

public class FltrSlangEdge extends ImgFilter {

	public FltrSlangEdge(ImgRender render){
		super(render);
	}
		
	private native void implSfrProc(CamBundle bnd);
	
	/**
	 * Pixel Per Millimeter
	 */
	private double pix_mm = 200.;
	
	//this is provided by native code
	private double[] frq = null;
	private double[] sfr = null;
	//this is provided by native code
	private int idxFrqOver = 0;
	private int idxSfrLess = 0;
	//this is provided by native code
	private double slopeDegree;
	private int numOfCycles;
	private int numOfLeftSide,numOfRightSide;
	private double fitRatio;

	@Override
	public void cookData(ArrayList<ImgPreview> list) {
		frq = sfr = null;//clear these variables~~~
		implSfrProc(list.get(0).bundle);
	}

	@Override
	public boolean showData(ArrayList<ImgPreview> list) {
		if(frq!=null && sfr!=null){
			info.setSeries(frq,sfr);
		}else{
			PanBase.notifyError("FltrSlangEdge", "內部錯誤");
		}
		return true;
	}
	
	public class PanInfo extends PanBase {
		
		private LineChart<Number,Number> chart;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void setSeries(double[] xx,double[] yy){
			
			chart.getData().clear();			
			
			XYChart.Series ss1 = new XYChart.Series();
			ss1.setName("Freq<");
			
			XYChart.Series ss2 = new XYChart.Series();
			ss2.setName("<Freq");

			for(int i=0; i<xx.length; i++){
				XYChart.Data dat = new XYChart.Data(xx[i],yy[i]);
				if(idxFrqOver<i){
					ss1.getData().add(dat);					
				}else if(i==idxFrqOver){
					ss1.getData().add(dat);
					ss2.getData().add(dat);		
				}else{
					ss2.getData().add(dat);
				}
			}
			
			XYChart.Series ss3 = new XYChart.Series();
			ss3.setName("SFR-50%");
			ss3.getData().add(new XYChart.Data(xx[idxSfrLess],0));
			ss3.getData().add(new XYChart.Data(xx[idxSfrLess],1));
			
			chart.getData().addAll(ss1,ss2,ss3);
			
			show_result();
		}
		
		private Label[] txtInfo = {
			new Label(),new Label(),new Label(),
			new Label(),new Label(),new Label()
		};
		
		private void show_result(){
			txtInfo[0].setText(String.format("%.3f°",slopeDegree));
			txtInfo[1].setText(String.format("%d",numOfCycles));
			txtInfo[2].setText(String.format("%d pix",numOfLeftSide));
			txtInfo[3].setText(String.format("%d pix",numOfRightSide));
			txtInfo[4].setText(String.format("%.3f",fitRatio));
			double _v = frq[idxSfrLess];
			txtInfo[5].setText(String.format("%.1f cy/mm",_v));
		}

		private Node layoutChart(){
			final NumberAxis x_axis = new NumberAxis();
	        final NumberAxis y_axis = new NumberAxis(0.,1.,0.1);
			x_axis.setLabel("cy/mm");
			y_axis.setLabel("SFR");
			chart = new LineChart<Number,Number>(x_axis,y_axis);
			chart.setCreateSymbols(false);
			chart.setAnimated(true);
			return chart;
		}
		
		private Node layoutCtrl(){
			GridPane lay = new GridPane();
			lay.getStyleClass().add("grid-medium");
			
			final Button btnCalculate = PanBase.genButton1("演算",null);
			btnCalculate.setOnAction(event->{
				rndr.attach(FltrSlangEdge.this);
			});
			btnCalculate.setPrefWidth(100.);
			
			final Button btnExport = PanBase.genButton1("匯出",null);
			btnExport.setOnAction(event->{
				
			});
			
			lay.add(btnCalculate, 0,0, 2, 1);
			lay.add(btnExport, 0,1, 2, 1);
			lay.addRow(2,new Label("Slope ："),txtInfo[0]);
			lay.addRow(3,new Label("Cycles："),txtInfo[1]);
			lay.addRow(4,new Label("Left  ："),txtInfo[2]);
			lay.addRow(5,new Label("Right："),txtInfo[3]);
			lay.addRow(6,new Label("R² fit："),txtInfo[4]);
			lay.addRow(7,new Label("half-freq："),txtInfo[5]);
			return PanDecorate.group(lay);
		}
				
		@Override
		public Parent layout() {
			BorderPane lay = new BorderPane();
			lay.setCenter(layoutChart());
			lay.setRight(layoutCtrl());
			return lay;
		}
	};
	
	public PanInfo info = new PanInfo();
}
