package prj.daemon;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.jfoenix.controls.JFXTextField;

import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;
import narl.itrc.vision.CamBundle;
import narl.itrc.vision.ImgFilter;
import narl.itrc.vision.ImgPreview;
import narl.itrc.vision.ImgRender;

public class FltrSlangEdge extends ImgFilter {

	public FltrSlangEdge(ImgRender render){
		super(render);
	}
		
	private native void implSfrProc(CamBundle bnd,int[] roi);
	
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
		ImgPreview prv = list.get(0);
		int[] roi = prv.getMark(0);
		roi[2] = roi[2] - roi[2]%2;//even width
		roi[3] = roi[3] - roi[3]%2;//even height
		implSfrProc(prv.bundle,roi);
	}

	@Override
	public boolean showData(ArrayList<ImgPreview> list) {
		if(frq!=null && sfr!=null){
			ctrl.setSeries(frq,sfr);
		}else{
			PanBase.notifyError("FltrSlangEdge", "內部錯誤");
		}
		return true;
	}
	
	public class PanCtrl extends PanBase {
		
		private LineChart<Number,Number> chart;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void setSeries(double[] xx,double[] yy){
			
			chart.getData().clear();			
			
			XYChart.Series ss1 = new XYChart.Series();
			ss1.setName("Freq.1");
			XYChart.Series ss2 = new XYChart.Series();
			ss2.setName("Freq.2");

			for(int i=0; i<xx.length; i++){
				XYChart.Data dat = new XYChart.Data(xx[i],yy[i]);
				if(idxFrqOver<i){
					ss2.getData().add(dat);					
				}else if(i==idxFrqOver){
					ss1.getData().add(dat);
					ss2.getData().add(dat);		
				}else{
					ss1.getData().add(dat);
				}
			}
			
			XYChart.Series ss3 = new XYChart.Series();
			ss3.setName("SFR-50%");
			ss3.getData().add(new XYChart.Data(xx[idxSfrLess],0));
			ss3.getData().add(new XYChart.Data(xx[idxSfrLess],1));
			
			chart.getData().addAll(ss1,ss2,ss3);
			show_result();
		}

		private JFXTextField txtPixSize;
		
		private Label[] txtInfo = {
			new Label(),new Label(),new Label(),
			new Label(),new Label(),new Label(),
			new Label()
		};
		
		private void show_result(){
			txtInfo[0].setText(String.format("%.3f°",slopeDegree));
			txtInfo[1].setText(String.format("%d",numOfCycles));
			txtInfo[2].setText(String.format("%d pix",numOfLeftSide));
			txtInfo[3].setText(String.format("%d pix",numOfRightSide));
			txtInfo[4].setText(String.format("%.3f",fitRatio));
			double _v = frq[idxSfrLess];
			txtInfo[5].setText(String.format("%.1f cy/mm",_v));
			_v = 1e-3/_v;//it is already millimeter
			txtInfo[6].setText(String.format("%sm",Misc.num2prefix(_v,1)));
		}

		private void export_sheet(){
			try {
				FileWriter fs = new FileWriter(Misc.pathRoot+"SFR.txt");
				fs.write("cy/mm \tSFR \t edge\n");
				for(int i=0; i<frq.length; i++){
					fs.write(String.format("%3.3f\t%.3f \t",frq[i],sfr[i]));
					if(i>=idxFrqOver){
						fs.write("#\n");
					}else{
						fs.write("\n");
					}
				}
				fs.close();
				PanBase.notifyInfo("Slang Edge","輸出至 SFR.txt");
			} catch (IOException e) {
				e.printStackTrace();
			}
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
				String txt = txtPixSize.getText();
				if(Misc.isValidPhy(txt)==true){
					double pix = Misc.phyConvert(txt, "mm");
					pix_mm = 1./pix;
				}else{
					//something wrong, just reset it~~
					txtPixSize.setText("5 um");
					pix_mm = 200.;
				}
				rndr.attach(FltrSlangEdge.this);
			});
			btnCalculate.setPrefWidth(100.);
			
			final Button btnExport = PanBase.genButton1("匯出",null);
			btnExport.setOnAction(event->{
				export_sheet();
			});
			
			txtPixSize = new JFXTextField("5 um");                    
			txtPixSize.setPromptText("pixel size");
			
			lay.add(btnCalculate,0, 0, 2, 1);
			lay.add(btnExport,   0, 1, 2, 1);
			lay.add(txtPixSize,  0, 2, 2, 1);
			lay.addRow(3,new Label("Slope ："),txtInfo[0]);
			lay.addRow(4,new Label("Cycles："),txtInfo[1]);
			lay.addRow(5,new Label("Left  ："),txtInfo[2]);
			lay.addRow(6,new Label("Right："),txtInfo[3]);
			lay.addRow(7,new Label("R² fit："),txtInfo[4]);
			lay.addRow(8,new Label("half-SRF："),txtInfo[5]);
			lay.addRow(9,new Label("half-Res："),txtInfo[6]);
			return PanDecorate.group(lay);
		}
				
		@Override
		public Node eventLayout() {
			BorderPane lay = new BorderPane();
			lay.setCenter(layoutChart());
			lay.setRight(layoutCtrl());
			return lay;
		}
	};
	
	public PanCtrl ctrl = new PanCtrl();
}
