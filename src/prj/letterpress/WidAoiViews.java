package prj.letterpress;

import java.io.File;
import java.util.ArrayList;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;

import javafx.event.ActionEvent;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;
import narl.itrc.vision.CamBundle;
import narl.itrc.vision.ImgFilter;
import narl.itrc.vision.ImgPreview;
import narl.itrc.vision.ImgRender;

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
			CamBundle bnd0 = list.get(0).bundle;
			CamBundle bnd1 = list.get(1).bundle;
			for(int i=0; i<10; i++){
				implTrainGrnd(bnd0,bnd1);
				refreshData(list);//next frame~~~
				Misc.logv("收集影像(%d)",i+1);
			}
			implTrainDone(backName0,backName1,10);
			scoreCross[0] = implFindCross(bnd0,0,paramLF,locaCross[0]);
			scoreCross[1] = implFindCross(bnd1,1,paramRH,locaCross[1]);
		}
		@Override
		public boolean showData(ArrayList<ImgPreview> list) {
			Misc.logv("校正結束");
			txtLocaCross();
			return true;
		}
	};
	private FilterCalib filterCalibrate = new FilterCalib();
	//----------------------------------//
	
	class FilterMarkRect extends ImgFilter {
		@Override
		public void cookData(ArrayList<ImgPreview> list) {
			CamBundle bnd0 = list.get(0).bundle;
			CamBundle bnd1 = list.get(1).bundle;
			scoreRect[0] = implFindRect(bnd0,0,paramLF,locaRect[0],locaCross[0]);
			scoreRect[1] = implFindRect(bnd1,1,paramRH,locaRect[1],locaCross[1]);
		}
		@Override
		public boolean showData(ArrayList<ImgPreview> list) {
			txtLocaRect();
			get_vector();
			return true;
		}
	}
	private FilterMarkRect filterMarkRect = new FilterMarkRect();
	//----------------------------------//
	
	class FilterAlign extends ImgFilter {
		public FilterAlign() {
		}
		//private final double biasDivStep = 2.;//effect~~~
		private final double biasTheta= 50.;//This is experiment value~~~
		private int[][] vec = null;
		@Override
		public void cookData(ArrayList<ImgPreview> list) {
			//update all location~~~
			filterMarkRect.cookData(list);
			vec = get_vector();
			refreshData(list);
			//kick motion stage~~~
			int th = vec[2][0] - vec[2][1];
			if(Math.abs(th)>=20){
				//if(th>0){
				//	asyncDone = Entry.stg0.asyncMoveTo('a',biasTheta);
				//}else{
				//	asyncDone = Entry.stg0.asyncMoveTo('a',-biasTheta);
				//}
				double thea = biasTheta * (th/Math.abs(th));
				asyncDone = Entry.stg0.asyncMoveTo('a',thea);
			}else{
				int dx = Math.min(vec[0][0],vec[1][0]);
				int dy = Math.min(vec[0][1],vec[1][1]);
				if(Math.abs(dx)>10 || Math.abs(dy)>10){
					//double stp_x = dx / biasDivStep;
					//double stp_y =-dy / biasDivStep;
					//Entry.stg0.moveTo(stp_x,stp_y);
					double stp_x = dx;
					double stp_y =-dy;
					asyncDone = Entry.stg0.asyncMoveTo(stp_x,stp_y);
				}
			}
			//refreshData(list);
		}
		@Override
		public boolean showData(ArrayList<ImgPreview> list) {
			if(vec==null){
				System.out.println("something is wrong!!!");
				return false;//???why???
			}
			txtLocaRect();
			int hypth = Math.min(vec[2][0],vec[2][1]);
			if(hypth<=20){
				Misc.logv("--------Aligment--------");
				Entry.stg0.exec("DE 0,0,0,0;DP 0,0,0,0;\r\n"); 
				Entry.stg0.exec_TP();
				return true;//we success!!!!!
			}		
			return false;
		}
		@Override
		public void handle(ActionEvent event) {
			if(locaCross[0][0]<0 || locaCross[1][0]<0){
				PanBase.notifyWarning("注意","必須先決定十字標靶位置");
				return;
			}
			Entry.rndr.attach(filterAlign);
		}
	};
	public FilterAlign filterAlign = new FilterAlign();

	private int[][] get_vector(){
		int[][] vec = {{0,0},{0,0},{-1,-1}};
		String txt = "向量：", tmp=null;
		//left camera
		if(locaCross[0][0]>=0){
			vec[0][0] = locaRect[0][0] - locaCross[0][0];
			vec[0][1] = locaRect[0][1] - locaCross[0][1];
			vec[2][0] = (int)Math.sqrt(vec[0][0]*vec[0][0] + vec[0][1]*vec[0][1]);
			tmp = String.format("(%d,%d)@%d",vec[0][0],vec[0][1],vec[2][0]);
		}else{
			tmp = "???";
		}
		txt = txt + tmp + "，";
		//right camera
		if(locaCross[1][0]>=0){
			vec[1][0] = locaRect[1][0] - locaCross[1][0];
			vec[1][1] = locaRect[1][1] - locaCross[1][1];
			vec[2][1] = (int)Math.sqrt(vec[1][0]*vec[1][0] + vec[1][1]*vec[1][1]);
			tmp = String.format("(%d,%d)@%d",vec[1][0],vec[1][1],vec[2][1]);
		}else{
			tmp = "???";
		}
		txt = txt + tmp;
		Misc.logv(txt);
		return vec;
	}
	//-------------------------------//

	/**
	 * 0 - debug mode
	 * 1 - Binary-Threshold for Cross-T.right
	 * 2 - Morphology-kernel for Cross-T
	 * 3 - Epsilon for Cross-T
	 * 
	 * 9 - Binary-Threshold for Rectangle
	 * 10- Morphology-kernel for Rectangle
	 */
	private int[] paramLF = {
		0,
		200,5,7,0,
		0,0,0,0,
		150,5,0,0,
		0,0,0,0
	};
	private int[] paramRH = {
		0,
		130,5,7,0,
		0,0,0,0,
		110,5,0,0,
		0,0,0,0
	};
	
	private float[] scoreCross = {0,0};//left, right
	private int[][] locaCross = {{313,599},{595,609}};

	private double[] scoreRect = {0,0};//left, right
	private int[][] locaRect = {{-1,-1},{-1,-1}};
	
	private native void implInitEnviroment(String name0,String name1);
	private native void implTrainGrnd(CamBundle bnd0,CamBundle bnd1);
	private native void implTrainDone(String name0,String name1,int count);
	
	private native float implFindCross(CamBundle bnd,int idx,int[] param,int[] loca);
	private native float implFindRect(CamBundle bnd,int idx,int[] param,int[] loca,int[] locaCross);
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
		final GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium");
		lay.add(layoutParam(), 0, 0, 2, 1);
		lay.add(new WidLight(), 1, 1);
		lay.add(new WidTheta(), 1, 2);
		lay.add(new WidMovement(), 0, 1, 1, 2);
		return lay;
	}
	
	private Node layoutParam(){
		//----information----
		GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-small");
		
		lay.add(new Label("[左視角]"),1,0,1,1);
		lay.add(new Label("[右視角]"),2,0,1,1);
		lay.addRow(1,new Label("十字位置："),txtTarget[0],txtTarget[1]);		
		lay.addRow(2,new Label("口型位置："),txtTarget[4],txtTarget[5]);
		lay.addRow(3,new Label("相似度.1："),txtTarget[2],txtTarget[3]);
		lay.addRow(4,new Label("相似度.2："),txtTarget[6],txtTarget[7]);

		//----actions----
		Button btnMarkCros = PanBase.genButton0("訓練模式",null);
		btnMarkCros.setOnAction(event->{
			resetLocaCross();
			resetLocaRect();
			Entry.rndr.attach(filterCalibrate);
		});
				
		Button btnMarkRect = PanBase.genButton0("標定口型",null);
		btnMarkRect.setOnAction(event->{
			resetLocaRect();			
			Entry.rndr.attach(filterMarkRect);
		});
		
		Button btnMarkAlign = PanBase.genButton0("標靶對位",null);
		btnMarkAlign.setOnAction(filterAlign);
		
		JFXComboBox<Integer> cmbDebug = new JFXComboBox<Integer>();		
		cmbDebug.getItems().addAll(0,1,2,3);
		cmbDebug.getSelectionModel().select(paramRH[0]);
		cmbDebug.setOnAction(event->{ 
			paramRH[0] = cmbDebug.getValue();
			paramLF[0] = cmbDebug.getValue(); 
		});
		cmbDebug.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(cmbDebug,true);
		
		Label txt1 = new Label("Action");
		GridPane.setHalignment(txt1, HPos.CENTER);
		
		Label txt2 = new Label("Debug：");
		
		lay.add(txt1,4,0, 2,1);
		lay.add(btnMarkCros ,4,1, 2,1);
		lay.add(btnMarkRect ,4,2, 2,1);
		lay.add(btnMarkAlign,4,3, 2,1);
		lay.add(txt2        ,4,4, 1,1);
		lay.add(cmbDebug    ,5,4, 1,1);

		//----parameter----
		lay.add(new Label("＋Thres  ："), 0,6 );
		lay.add(new Label("＋Struct ："), 0,7 );
		lay.add(new Label("＋Epsilon："), 0,8 );		
		lay.add(new Label("□ Thres  ："), 0,9 );
		lay.add(new Label("□ Struct ："), 0,10);
		
		lay.add(genBoxValue(1 ,paramLF), 1,6 ,2,1);
		lay.add(genCmbRange(2 ,paramLF), 1,7 ,2,1);
		lay.add(genCmbRange(3 ,paramLF), 1,8 ,2,1);		
		lay.add(genBoxValue(9 ,paramLF), 1,9 ,2,1);
		lay.add(genCmbRange(10,paramLF), 1,10,2,1);

		lay.add(genBoxValue(1 ,paramRH), 4,6 ,2,1);
		lay.add(genCmbRange(2 ,paramRH), 4,7 ,2,1);
		lay.add(genCmbRange(3 ,paramRH), 4,8 ,2,1);		
		lay.add(genBoxValue(9 ,paramRH), 4,9 ,2,1);
		lay.add(genCmbRange(10,paramRH), 4,10,2,1);
		
		lay.add(new Separator(Orientation.VERTICAL  ), 3,0, 1,5);		
		lay.add(new Separator(Orientation.HORIZONTAL), 0,5, 7,1);
		lay.add(new Separator(Orientation.VERTICAL  ), 3,6, 1,5);
		return PanDecorate.group("MVS Status",lay);
	}

	private Node genBoxValue(final int idx,final int[] param){
		final JFXTextField box = new JFXTextField();
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
	
	private Node genCmbRange(final int idx,final int[] param){
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
			"(%3d,%3d)",locaCross[0][0],locaCross[0][1]
		));
		txtTarget[1].setText(String.format(
			"(%3d,%3d)",locaCross[1][0],locaCross[1][1]
		));
		txtTarget[2].setText(String.format(
			"%.3f%%",scoreCross[0]
		));
		txtTarget[3].setText(String.format(
			"%.3f%%",scoreCross[1]
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
	private void resetLocaCross(){
		locaCross[0][0] = locaCross[0][1] = -1;
		locaCross[1][0] = locaCross[1][1] = -1;
		scoreCross[0] = scoreCross[1] = -1.f;
	}
	private void resetLocaRect(){		
		locaRect[0][0] = locaRect[0][1] = -1;
		locaRect[1][0] = locaRect[1][1] = -1;
		scoreRect[0] = scoreRect[1] = -1.f;
	}
}
