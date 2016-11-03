package prj.letterpress;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import narl.itrc.CamBundle;
import narl.itrc.ImgFilter;
import narl.itrc.ImgPreview;
import narl.itrc.ImgRender;
import narl.itrc.Misc;
import narl.itrc.PanBase;

/**
 * AXIS-A : 10pps <==> 50um
 * AXIS-B : 10pps <==> 50um
 * @author qq
 *
 */
public class WidAoiViews extends BorderPane {
	
	private final int MARK_CROS= 1;
	private final int MARK_RECT = 2;
		
	class FilterCalib extends ImgFilter {
		
		public FilterCalib(){
		}
		@Override
		public void cookData(ArrayList<ImgPreview> list) {
			implInitParam();
			for(int i=0; i<10; i++){
				implTrainGrnd(
					list.get(0).bundle,
					list.get(1).bundle
				);
				refreshData(list);//next frame~~~
			}
			implTrainDone(
				backName0,
				backName1,
				10
			);
			
			//get_loca_cross(list);
		}
		@Override
		public boolean showData(ArrayList<ImgPreview> list) {
			Misc.logv("校正結束");
			txtLocaCross();
			return true;
		}
	};
	private FilterCalib filterCalib = new FilterCalib();
	
	private final double biasAxis = 100.;//This is experiment value~~~
	private final double biasTheta = 100.;//This is experiment value~~~
	class FilterBias extends ImgFilter implements 
		EventHandler<ActionEvent>
	{
		public FilterBias() {
		}
		@Override
		public void cookData(ArrayList<ImgPreview> list) {
			
			int loca_prev[][] = {{-1,-1},{-1,-1}};
			
			get_loca_rect(list,locaRect);			
			refreshData(list);
			Entry.stg0.moveTo(biasAxis,'x');			
			get_loca_rect(list,loca_prev);
			refreshData(list);
			Entry.stg0.moveTo(-biasAxis,'x');//go to original point
			get_vector(0,loca_prev);
				
			get_loca_rect(list,locaRect);
			refreshData(list);
			Entry.stg0.moveTo(biasAxis,'y');
			get_loca_rect(list,loca_prev);
			refreshData(list);
			Entry.stg0.moveTo(-biasAxis,'y');//go to original point
			get_vector(1,loca_prev);

			solve_trans();
			
			//check the origin of Theta value
			get_loca_rect(list,locaRect);
			Entry.stg0.moveTo(biasTheta,'a');
			Misc.logv("start to check Theta");
				
			get_loca_rect(list,loca_prev);
			Entry.stg0.moveTo(-biasTheta,'a');//back to origin~~~~
		}
		@Override
		public boolean showData(ArrayList<ImgPreview> list) {			
			return true;
		}

		@Override
		public void handle(ActionEvent event) {
			if(locaCros[0][0]<0 || locaCros[1][0]<0){
				PanBase.msgBox.notifyWarning("注意","必須先決定十字標靶位置");
				return;
			}
			Entry.rndr.attach(filterBias);
		}
	};
	public FilterBias filterBias = new FilterBias();

	private void get_loca_cross(ArrayList<ImgPreview> list){
		scoreCros[0] = implFindCros(list.get(0).bundle,locaCros[0]);
		scoreCros[1] = implFindCros(list.get(1).bundle,locaCros[1]);
	}
	private void get_loca_rect(ArrayList<ImgPreview> list,int[][] loca){
		scoreRect[0] = implFindRect(list.get(0).bundle,locaCros[0],loca[0]);
		scoreRect[1] = implFindRect(list.get(1).bundle,locaCros[1],loca[1]);
	}
	private void get_vector(int i,int[][] locaVetx){
		vectScale[i][0] = vectScale[i][1] = 0;//reset one~~~
		int[] v1=null,v2=null;//just pickup one side
		if(locaRect[0][0]>=0 && locaVetx[0][0]>=0){
			v1 = locaRect[0];
			v2 = locaVetx[0];
		}else if(locaRect[1][0]>=0 && locaVetx[1][0]>=0){
			v1 = locaRect[1];
			v2 = locaVetx[1];
		}else{
			Misc.loge("No valid vector");
			return;
		}
		vectScale[i][0] = v2[0] - v1[0];
		vectScale[i][1] = v2[1] - v1[1];
		double hypt = Math.hypot(vectScale[i][0],vectScale[i][1]);
		vectScale[i][0] = vectScale[i][0] / hypt;
		vectScale[i][1] = vectScale[i][1] / hypt;
		Misc.logv(
			"vect-%d = (%.3f,%.3f)",
			i,vectScale[i][0],vectScale[i][1]
		);
	}
	
	private void solve_trans(){
		locaTran[0][0] = (vectScale[0][0]/biasAxis); locaTran[0][1] = (vectScale[1][0]/biasAxis);
		locaTran[1][0] = (vectScale[0][1]/biasAxis); locaTran[1][1] = (vectScale[1][1]/biasAxis);
		RealMatrix val = new Array2DRowRealMatrix(locaTran,false);
		RealMatrix mat = new LUDecomposition(val).getSolver().getInverse();
		locaTran[0][0] = mat.getEntry(0,0); locaTran[0][1] = mat.getEntry(0,1);
		locaTran[1][0] = mat.getEntry(1,0); locaTran[1][1] = mat.getEntry(1,1);
	}

	private double[] get_axis_bias(){
		double[] bias = {0.,0.};
		double[] vect = {0.,0.};
		int[] v1 = null,v2 = null;
		if(locaCros[0][0]>=0 && locaRect[0][0]>=0){
			v2 = locaCros[0];
			v1 = locaRect[0];
		}else if(locaCros[1][0]>=0 && locaRect[1][0]>=0){
			v2 = locaCros[1];
			v1 = locaRect[1];
		}else{
			Misc.loge("No valid location!!!");
			return bias;
		}
		vect[0] = v2[0] - v1[0];
		vect[1] = v2[1] - v1[1];
		bias[0] = locaTran[0][0] * vect[0] +  locaTran[0][1] * vect[1];
		bias[1] = locaTran[1][0] * vect[0] +  locaTran[1][1] * vect[1];
		return bias;
	}
	
	//-------------------------------//
	
	private int debugMode = 0;
	
	/**
	 * Parameter for AOI. Their meanings are : <p>
	 * 0: Binary Threshold.<p>
	 * 1: Canny Threshold.<p>
	 * 2: Canny Threshold, but only offset value.<p>
	 * 3: Canny Aperture.<p>
	 * 4: Dilate Size.<p>
	 * 5: Approximates Epsilon.<p>
	 * 6: minimum score for Cross-T.<p>
	 * 7: minimum score for Rectangle.<p>
	 */
	private int param[] = {125,190,10,5,5,7,70,70};

	private double[] scoreCros = {0,0};
	//private int[][] locaCros = {{-1,-1},{-1,-1}};
	private int[][] locaCros = {{348,472},{482,455}};
	
	private double[] scoreRect = {0,0};	
	private int[][] locaRect = {{-1,-1},{-1,-1}};

	private double[][] vectScale = {{0.,0.},{0.,0.}};//X-bias,Y-bias
	
	private double[][] locaTran = {{0.,0.},{0.,0.}};
	
	private native void implInitEnviroment(String name0,String name1);
	private native void implInitParam();
	private native void implTrainGrnd(CamBundle bnd0,CamBundle bnd1);
	private native void implTrainDone(String name0,String name1,int count);
	private native float implFindCros(CamBundle bnd,int[] loca);
	private native float implFindRect(CamBundle bnd,int[] mask,int[] loca);
	//-------------------------------//

	private final String backName0 = Misc.pathTemp+"back0.png";
	private final String backName1 = Misc.pathTemp+"back1.png";
	
	public WidAoiViews(ImgRender rndr){
		File fs0 = new File(backName0);
		File fs1 = new File(backName1);
		if(fs0.exists()==true && fs1.exists()==true){
			implInitEnviroment(backName0,backName1);
		}else{
			implInitEnviroment(null,null);
		}

		setCenter(layoutViews());
		setRight(layoutOption());		
		txtLocaCross();
		txtLocaRect();
	}

	private Node layoutViews(){
		HBox lay0 = new HBox();
		lay0.getStyleClass().add("hbox-small");
		for(int idx=0; idx<Entry.rndr.getSize(); idx++){
			Pane pan = Entry.rndr.getPreview(idx);
			lay0.getChildren().add(pan);
		}
		return lay0;		
	}
	
	private Node layoutOption(){
				
		//----parameter----
		final JFXComboBox<Integer> dbg_mode = new JFXComboBox<Integer>();		
		dbg_mode.getItems().addAll(0,1,2,3);
		dbg_mode.getSelectionModel().select(debugMode);
		dbg_mode.setOnAction(event->{
			debugMode = dbg_mode.getValue();
		});
		dbg_mode.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(dbg_mode,true);
		
		GridPane lay2 = new GridPane();
		lay2.getStyleClass().add("grid-small");
		lay2.addRow(0,new Label("Debug Mode："),dbg_mode);
		lay2.addRow(1,new Label("Binary-Thres："),genBoxValue(0));
		lay2.addRow(2,new Label("Canny-Value：") ,genBoxValue(1));
		lay2.addRow(3,new Label("Canny-Offset："),genBoxValue(2));
		lay2.addRow(4,new Label("Canny-Apture："),genCmbRange(3));
		lay2.addRow(5,new Label("Dilate Size：") ,genCmbRange(4));
		lay2.addRow(6,new Label("Appx-Epsilon："),genCmbRange(5));
		lay2.addRow(7,new Label("Score.1："),genBoxValue(6));
		lay2.addRow(8,new Label("Score.2："),genBoxValue(7));
		
		//----information----
		final String SPACE="  ";
		GridPane lay3 = new GridPane();
		lay3.getStyleClass().add("grid-small");
		lay3.add(new Label("[左視角]"),1,0,1,1);
		lay3.add(new Label("[右視角]"),3,0,1,1);
		lay3.addRow(1,new Label("十字位置："),
			txtTarget[0],new Label(SPACE),txtTarget[1]
		);		
		lay3.addRow(2,new Label("口型位置："),
			txtTarget[4],new Label(SPACE),txtTarget[5]
		);
		lay3.addRow(3,new Label("相似度.1："),
			txtTarget[2],new Label(SPACE),txtTarget[3]
		);
		lay3.addRow(4,new Label("相似度.2："),
			txtTarget[6],new Label(SPACE),txtTarget[7]
		);
		//----actions----
		Button btnMarkCros = PanBase.genButton1("校正平台",null);
		btnMarkCros.setOnAction(event->{
			resetLocaCross();
			Entry.rndr.attach(filterCalib);
		});
		
		Button btnMarkRect = PanBase.genButton1("標定口型",null);
		btnMarkRect.setOnAction(event->{
			//resetLocaRect();
			//filterMark.step = MARK_RECT;//reset~~~
			//Entry.rndr.attach(filterMark);
		});

		Button btnMarkAlign = PanBase.genButton1("?????",null);
		//btnMarkAlign.setOnAction(filterBias);
		
		//----combine them all----
		VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		lay1.getChildren().addAll(
			btnMarkCros,
			btnMarkRect,
			btnMarkAlign,
			new Separator(),
			lay3,
			new Separator(),
			lay2			
		);
		ScrollPane root = new ScrollPane();
		root.setContent(lay1);
		return root;
	}

	private Node genBoxValue(final int idx){
		final JFXTextField box = new JFXTextField();                    
		box.setPromptText("canny threshold");
		box.setPrefWidth(100);
		box.setText(""+param[idx]);
		box.setOnAction(event->{
			String txt = box.getText().trim();
			try{
				param[idx] = Integer.valueOf(txt);
			}catch(NumberFormatException e){
				box.setText(""+param[idx]);//reset again!!!!!
			}
		});
		return box;
	}
	
	private Node genCmbRange(final int idx){
		final JFXComboBox<String> cmb = new JFXComboBox<String>();
		cmb.getItems().addAll(
			"1","3","5","7","9",
			"11","13","15","17"
		);
		cmb.setOnAction(event->{
			int val = cmb.getSelectionModel().getSelectedIndex();
			param[idx] = val*2+1;
		});
		cmb.getSelectionModel().select((param[idx]-1)/2);
		cmb.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(cmb,true);
		return cmb;
	}
	
	private Label[] txtTarget = {
		new Label()/* left cross-location */, 
		new Label()/* right cross-location */,
		new Label()/* left cross-score */, 
		new Label()/* right cross-score */,
		new Label()/* left rectangle-location */, 
		new Label()/* right rectangle-location */,
		new Label()/* left rectangle-score */, 
		new Label()/* right rectangle-score */
	};
	private void txtLocaCross(){
		txtTarget[0].setText(String.format(
			"(%3d,%3d)",locaCros[0][0],locaCros[0][1]
		));
		txtTarget[1].setText(String.format(
			"(%3d,%3d)",locaCros[1][0],locaCros[1][1]
		));
		txtTarget[2].setText(String.format(
			"%.3f%%",scoreCros[0]
		));
		txtTarget[3].setText(String.format(
			"%.3f%%",scoreCros[1]
		));
	}
	private void txtLocaRect(){
		txtTarget[4].setText(String.format(
			"(%3d,%3d)",locaRect[0][0],locaRect[0][1]
		));
		txtTarget[5].setText(String.format(
			"(%3d,%3d)",locaRect[1][0],locaRect[1][1]
		));
		txtTarget[6].setText(String.format(
			"%.3f%%",scoreRect[0]
		));
		txtTarget[7].setText(String.format(
			"%.3f%%",scoreRect[1]
		));
	}	
	private int[][] txtShowVector(){
		int[][] vec = {{0,0},{0,0},{-1,-1}};
		String txt = "向量：", tmp=null;
		//left camera
		if(locaCros[0][0]>=0){
			vec[0][0] = locaRect[0][0] - locaCros[0][0];
			vec[0][1] = locaRect[0][1] - locaCros[0][1];
			vec[2][0] = (int)Math.sqrt(vec[0][0]*vec[0][0] + vec[0][1]*vec[0][1]);
			tmp = String.format("(%d,%d)@%d",vec[0][0],vec[0][1],vec[2][0]);
		}else{
			tmp = "--------";
		}
		txt = txt + tmp + "，";
		//right camera
		if(locaCros[1][0]>=0){
			vec[1][0] = locaRect[1][0] - locaCros[1][0];
			vec[1][1] = locaRect[1][1] - locaCros[1][1];
			vec[2][1] = (int)Math.sqrt(vec[1][0]*vec[1][0] + vec[1][1]*vec[1][1]);
			tmp = String.format("(%d,%d)@%d",vec[1][0],vec[1][1],vec[2][1]);
		}else{
			tmp = "--------";
		}
		txt = txt + tmp;
		Misc.logv(txt);
		return vec;
	}
	private void resetLocaCross(){
		locaCros[0][0] = locaCros[0][1] = -1;
		locaCros[1][0] = locaCros[1][1] = -1;
		scoreCros[0] = scoreCros[1] = -1.;
	}
	private void resetLocaRect(){		
		locaRect[0][0] = locaRect[0][1] = -1;
		locaRect[1][0] = locaRect[1][1] = -1;
		scoreRect[0] = scoreRect[1] = -1.;
	}
}
