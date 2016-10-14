package prj.letterpress;

import java.util.ArrayList;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import narl.itrc.CamBundle;
import narl.itrc.ImgFilter;
import narl.itrc.ImgPreview;
import narl.itrc.ImgRender;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;

public class WidAoiViews extends BorderPane {
	
	private final int MARK_CROS= 1;
	private final int MARK_RECT = 2;
		
	class FilterMark extends ImgFilter {
		public int step = MARK_CROS;
		private WidAoiViews inst;
		public FilterMark(WidAoiViews instance){
			inst = instance;
		}
		@Override
		public void cookData(ArrayList<ImgPreview> list) {
			inst.implInitParam();
			switch(step){
			case MARK_CROS:
				/*inst.scoreCros[0] = inst.implFindCros(
					list.get(0).bundle,
					inst.locaCros[0]
				);*/
				inst.scoreCros[1] = inst.implFindCros(
					list.get(1).bundle,
					inst.locaCros[1]
				);
				break;
			case MARK_RECT:
				inst.scoreRect[0] = inst.implFindRect(
					list.get(0).bundle,
					inst.locaCros[0],
					inst.locaRect[0]
				);
				inst.scoreRect[1] = inst.implFindRect(
					list.get(1).bundle,
					inst.locaCros[1],
					inst.locaRect[1]
				);				
				break;
			}
		}
		@Override
		public boolean showData(ArrayList<ImgPreview> list) {
			switch(step){
			case MARK_CROS:
				txtPosCros();
				return true;
			case MARK_RECT://run just once~~~
				txtPosRect();
				return true;//run just once~~~
			}
			return true;
		}
	};
	
	private native void implInitShape();
	private native void implInitParam();
	private native float implFindCros(CamBundle bnd,int[] loca);
	private native float implFindRect(CamBundle bnd,int[] mask,int[] loca);
	
	private FilterMark filterMark = new FilterMark(this);
	
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
	private int param[] = {100,300,50,5,5,7,70,70};

	private double[] scoreCros = {0,0};
	private int[][] locaCros = {{-1,-1},{-1,-1}};
	
	private double[] scoreRect = {0,0};	
	private int[][] locaRect = {{-1,-1},{-1,-1}};
	
	public WidAoiViews(ImgRender rndr){
		setCenter(layoutViews());
		setRight(layoutOption());
		implInitShape();
		txtPosCros();
		txtPosRect();
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
		GridPane lay3 = new GridPane();
		lay3.getStyleClass().add("grid-small");
		lay3.add(new Label("----左視角----"),0,0,3,1);
		lay3.addRow(1,new Label("十字位置："),txtTarget[0]);		
		lay3.addRow(2,new Label("口型位置："),txtTarget[4]);
		lay3.addRow(3,new Label("相似度.1："),txtTarget[2]);
		lay3.addRow(4,new Label("相似度.2："),txtTarget[6]);
		lay3.add(new Label("    "),0,5,3,1);
		lay3.add(new Label("----右視角----"),0,6,3,1);
		lay3.addRow(7,new Label("十字位置："),txtTarget[1]);		
		lay3.addRow(8,new Label("口型位置："),txtTarget[5]);
		lay3.addRow(9,new Label("相似度.1："),txtTarget[3]);
		lay3.addRow(10,new Label("相似度.2："),txtTarget[7]);
		
		//----actions----
		Button btnMarkCros = PanBase.genButton1("標定十字",null);
		btnMarkCros.setOnAction(event->{
			resetPosCros();
			filterMark.step = MARK_CROS;//reset~~~
			Entry.rndr.attach(filterMark);
		});
		
		Button btnMarkRect = PanBase.genButton1("標定口型",null);
		btnMarkRect.setOnAction(event->{
			resetPosRect();
			filterMark.step = MARK_RECT;//reset~~~
			Entry.rndr.attach(filterMark);
		});

		//----combine them all----
		VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		lay1.getChildren().addAll(
			lay3,
			new Separator(),
			lay2,
			new Separator(),
			btnMarkCros,
			btnMarkRect
		);
		return PanDecorate.group("設定",lay1);
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
	private void txtPosCros(){
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
	private void txtPosRect(){
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
	private void resetPosCros(){
		locaCros[0][0] = locaCros[0][1] = -1;
		locaCros[1][0] = locaCros[1][1] = -1;
		scoreCros[0] = scoreCros[1] = -1.;
	}
	private void resetPosRect(){		
		locaRect[0][0] = locaRect[0][1] = -1;
		locaRect[1][0] = locaRect[1][1] = -1;
		scoreRect[0] = scoreRect[1] = -1.;
	}
}
