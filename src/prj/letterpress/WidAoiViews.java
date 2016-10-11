package prj.letterpress;

import java.util.ArrayList;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import narl.itrc.CamBundle;
import narl.itrc.ImgFilter;
import narl.itrc.ImgPreview;
import narl.itrc.ImgRender;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;

public class WidAoiViews extends BorderPane {

	class FilterAligment extends ImgFilter {
		public int step = 1;
		private WidAoiViews inst;
		public FilterAligment(WidAoiViews instance){
			inst = instance;
		}
		@Override
		public void cookData(ArrayList<ImgPreview> list) {
			inst.implFindTarget(list.get(0).bundle,step);
			//inst.implFindTarget(list.get(1).bundle,step);
			//implements, stage go~~~~
		}
		@Override
		public boolean showData(ArrayList<ImgPreview> list) {
			return false;
		}
	};
	
	private native void implInitShape();	
	private native void implFindTarget(CamBundle bnd,int step);
	
	private FilterAligment filter = new FilterAligment(this);
	
	private int param[] = {2000,5000,7,3};

	public WidAoiViews(ImgRender rndr){
		setCenter(layoutViews());
		setBottom(layoutOption());
		implInitShape();
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
		
		final ToggleGroup grpStep = new ToggleGroup();
		final JFXRadioButton[] radStep = new JFXRadioButton[3];
		
		//----parameters for step.1----//
		radStep[0] = new JFXRadioButton("Segment");
		radStep[0].setToggleGroup(grpStep);
		radStep[0].setOnAction(event->{
			filter.step = 1;
			Entry.rndr.attach(filter);
		});
		
		GridPane lay2 = new GridPane();
		lay2.getStyleClass().add("grid-small");
		lay2.add(radStep[0],0,0,4,1);
		lay2.addRow(1, 
			new Label("Thres.1" ),genBoxThres(0),
			new Label("Aperture"),genCmbSize(2)
		);
		lay2.addRow(2, 
			new Label("Thres.2"),genBoxThres(1),
			new Label("Dilate" ),genCmbSize(3)
		);

		//----parameters for step.2----//
		radStep[1] = new JFXRadioButton("Contours");
		radStep[1].setToggleGroup(grpStep);
		radStep[1].setOnAction(event->{
			filter.step = 2;
			Entry.rndr.attach(filter);
		});
		
		GridPane lay3 = new GridPane();
		lay3.getStyleClass().add("grid-small");
		lay3.add(radStep[1], 0, 0, 3, 1);
		
		//----parameters for step.3----//
		radStep[2] = new JFXRadioButton("Matching");
		radStep[2].setToggleGroup(grpStep);
		radStep[2].setOnAction(event->{
			filter.step = 3;
			Entry.rndr.attach(filter);
		});
		
		GridPane lay4 = new GridPane();
		lay4.getStyleClass().add("grid-small");
		lay4.addRow(0, radStep[2]);

		//----other actions----
		Button btnReset = new Button("取消分析");
		btnReset.getStyleClass().add("btn-raised1");
		btnReset.setOnAction(event->{
			radStep[0].setSelected(false);
			radStep[1].setSelected(false);
			radStep[2].setSelected(false);
			filter.step = 0;//reset~~~
			Entry.rndr.detach(filter);
		});
		btnReset.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(btnReset,true);
		
		//----combine them all----
		HBox lay1 = new HBox();
		lay1.getStyleClass().add("hbox-small");
		lay1.getChildren().addAll(
			lay2,
			new Separator(Orientation.VERTICAL),
			lay3,
			new Separator(Orientation.VERTICAL),
			lay4,
			new Separator(Orientation.VERTICAL),
			btnReset
		);
		return PanDecorate.group("設定",lay1);
	}

	private Node genBoxThres(final int idx){
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
	
	private Node genCmbSize(final int idx){
		final JFXComboBox<String> cmb = new JFXComboBox<String>();
		cmb.getItems().addAll("3x3","5x5","7x7","9x9","11x11");
		cmb.setOnAction(event->{
			int val = cmb.getSelectionModel().getSelectedIndex();
			param[idx] = val*2+3;
		});
		cmb.getSelectionModel().select((param[idx]-3)/2);
		cmb.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(cmb,true);
		return cmb;
	}
}
