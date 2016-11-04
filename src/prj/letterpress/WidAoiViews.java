package prj.letterpress;

import java.io.File;
import java.util.ArrayList;

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
			CamBundle bnd0 = list.get(0).bundle;
			CamBundle bnd1 = list.get(1).bundle;
			for(int i=0; i<10; i++){
				implTrainGrnd(bnd0,bnd1);
				refreshData(list);//next frame~~~
				Misc.logv("收集影像(%d)",i+1);
			}
			implTrainDone(backName0,backName1,10);
			scoreCross[0] = implFindCross(bnd0,0,param,locaCross[0]);
			scoreCross[1] = implFindCross(bnd1,1,param,locaCross[1]);
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
			scoreRect[0] = implFindRect(bnd0,0,param,locaRect[0],locaCross[0]);
			scoreRect[1] = implFindRect(bnd1,1,param,locaRect[1],locaCross[1]);
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
	
	class FilterBias extends ImgFilter implements 
		EventHandler<ActionEvent>
	{
		public FilterBias() {
		}
		private final double biasStep = 5.;//This is experiment value~~~
		private final double biasTheta= 25.;//This is experiment value~~~
		private int[][] vec;
		@Override
		public void cookData(ArrayList<ImgPreview> list) {			
			filterMarkRect.cookData(list);//update all location~~~
			vec = get_vector();
			
			int th = vec[2][0]-vec[2][1];
			if(Math.abs(th)>10){
				if(th>0){
					Entry.stg0.moveTo(biasTheta,'a');
				}else{
					Entry.stg0.moveTo(-biasTheta,'a');
				}
				refreshData(list);
				return;
			}

			int dx = Math.min(vec[0][0], vec[1][0]);
			int dy = Math.min(vec[0][1], vec[1][1]);
			if(Math.abs(dx)>10 || Math.abs(dy)>10){
				//double stp_x = dx * biasStep;
				//Entry.stg0.moveTo(stp_x,'x');
				double stp_y = dy * biasStep;				
				Entry.stg0.moveTo(stp_y,'y');
				//Entry.stg0.moveTo(stp_x,stp_y);
				refreshData(list);
				return;
			}
		}
		@Override
		public boolean showData(ArrayList<ImgPreview> list) {
			txtLocaRect();
			int th = vec[2][0]-vec[2][1];
			if(Math.abs(th)<10){
				return true;//we success!!!!!
			}			
			return true;
		}
		@Override
		public void handle(ActionEvent event) {
			if(locaCross[0][0]<0 || locaCross[1][0]<0){
				PanBase.msgBox.notifyWarning("注意","必須先決定十字標靶位置");
				return;
			}
			Entry.rndr.attach(filterBias);
		}
	};
	public FilterBias filterBias = new FilterBias();

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
	 * 1 - Binary-Threshold for Cross-T
	 * 2 - Morphology-kernel for Cross-T
	 * 3 - Epsilon for Cross-T
	 * 
	 * 9 - Binary-Threshold for Rectangle
	 * 10- Morphology-kernel for Rectangle
	 */
	private int[] param = {0,
		150,5,7,0,
		0,0,0,0,
		128,5,0,0,
		0,0,0,0
	};
	
	private float[] scoreCross = {0,0};//left, right
	private int[][] locaCross = {{271,560},{527,565}};

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
				
		//----parameter----
		final JFXComboBox<Integer> dbg_mode = new JFXComboBox<Integer>();		
		dbg_mode.getItems().addAll(0,1,2,3);
		dbg_mode.getSelectionModel().select(param[0]);
		dbg_mode.setOnAction(event->{ param[0] = dbg_mode.getValue(); });
		dbg_mode.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(dbg_mode,true);
		
		GridPane lay2 = new GridPane();
		lay2.getStyleClass().add("grid-small");
		lay2.addRow(0,new Label("Debug Mode："),dbg_mode);
		
		lay2.addRow(1,new Label("Cross - Thres："),genBoxValue(1));
		lay2.addRow(2,new Label("Cross - Struct：") ,genCmbRange(2));
		lay2.addRow(3,new Label("Cross - Epsilon："),genCmbRange(3));
	
		lay2.addRow(4,new Label("Rect - Thres："),genBoxValue(9));
		lay2.addRow(5,new Label("Rect - Struct：") ,genCmbRange(10));

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
		Button btnMarkCros = PanBase.genButton1("標定十字",null);
		btnMarkCros.setOnAction(event->{
			resetLocaCross();
			resetLocaRect();
			Entry.rndr.attach(filterCalibrate);
		});
		
		Button btnMarkRect = PanBase.genButton1("標定口型",null);
		btnMarkRect.setOnAction(event->{
			resetLocaRect();			
			Entry.rndr.attach(filterMarkRect);
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
