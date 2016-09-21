package prj.letterpress;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.RequiredFieldValidator;

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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.ImgRender;
import narl.itrc.PanBase;
import narl.itrc.PanDecorate;

public class WidAoiViews extends BorderPane {

	private ImgRender rndr;
	
	private int filterSize = 3;	
	private int[] filterThres = {10,20,50,200};
	
	public WidAoiViews(ImgRender rndr){		
		this.rndr = rndr;
		setCenter(layoutViews());
		setBottom(layoutOption());
	}
	
	private Node layoutViews(){
		HBox lay = new HBox();		
		for(int idx=0; idx<rndr.getSize(); idx++){
			Pane pan = rndr.getPreview(idx).genBoard(
				String.format("攝影機 %d",idx+1),
				512,512
			);
			lay.getChildren().add(pan);
		}
		return lay;		
	}
	
	private Node layoutOption(){
		
		final ToggleGroup grpStep = new ToggleGroup();
		final JFXRadioButton[] radStep = new JFXRadioButton[3];
		
		//----parameters for step.1----
		radStep[0] = new JFXRadioButton("StdDev Filter");
		radStep[0].setToggleGroup(grpStep);

		JFXComboBox<String> cmbRange = new JFXComboBox<String>();
		cmbRange.getItems().addAll("3x3","5x5","7x7");
		cmbRange.setOnAction(event->{
			int idx = cmbRange.getSelectionModel().getSelectedIndex();
			filterSize = idx*2 + 3;
		});
		cmbRange.getSelectionModel().select((filterSize/2)-1);
		cmbRange.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(cmbRange,true);
		
		Button btnReset = new Button("關閉分析");		
		btnReset.setOnAction(event->{
			radStep[0].setSelected(false);
			radStep[1].setSelected(false);
			radStep[2].setSelected(false);
		});
		btnReset.setMaxWidth(Double.MAX_VALUE);
		GridPane.setFillWidth(btnReset,true);
		
		GridPane lay2 = new GridPane();
		lay2.getStyleClass().add("grid-small");
		lay2.addColumn(0,radStep[0],cmbRange,btnReset);
		
		//----parameters for step.2----
		radStep[1] = new JFXRadioButton("Range Thres.");
		radStep[1].setToggleGroup(grpStep);
		
		GridPane lay3 = new GridPane();
		lay3.getStyleClass().add("grid-small");
		lay3.add(radStep[1], 0, 0, 3, 1);
		lay3.addRow(1, new Label("上邊界"), genBoxThres(2), genBoxThres(3));
		lay3.addRow(2, new Label("下邊界"), genBoxThres(0), genBoxThres(1));
		
		//----parameters for step.3----
		radStep[2] = new JFXRadioButton("Show Target");
		radStep[2].setToggleGroup(grpStep);
		
		GridPane lay4 = new GridPane();
		lay4.getStyleClass().add("grid-small");
		lay4.addRow(0, radStep[2]);

		//----other actions----
		
		 Button btnGoHome = PanBase.genJButton1("定位標靶","selection.png");
		
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
			btnGoHome
		);
		return PanDecorate.group("設定",lay1);
	}
	
	private Node genBoxThres(final int idx){
		JFXTextField box = new JFXTextField();                    
		box.setPromptText("灰階值");
		box.setPrefWidth(60);
		box.setText(""+filterThres[idx]);
		box.setOnAction(event->{
			String txt = box.getText().trim();
			try{
				int val = Integer.valueOf(txt);
				if(0<=val && val<=255){
					filterThres[idx] = val;
				}else{
					box.setText(""+filterThres[idx]);//reset again!!!!!
				}
			}catch(NumberFormatException e){
				box.setText(""+filterThres[idx]);//reset again!!!!!
			}
		});
		return box;
	}
}
