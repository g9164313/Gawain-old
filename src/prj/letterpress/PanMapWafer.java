package prj.letterpress;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import narl.itrc.BoxPhyValue;
import narl.itrc.PanDecorate;

import com.jfoenix.controls.JFXComboBox;

public class PanMapWafer extends PanMapBase {

	public PanMapWafer(){
		super("配置圖");
		setMapSize(8*25.4);//default is 8' wafer
		generate();
	}

	private int diameter2index(){
		int idx = Math.round((float)(mapSize[0]/25.4));
		idx = idx - 4;
		if(idx<0){ return 0; }
		if(idx>8){ return 8; }
		return idx;
	}
	
	private void index2diameter(int idx){
		double dia = (idx+4)*25.4;//unit is millimeter
		setMapSize(dia);
	}
	
	private Label txtInfo = new Label("--------");
	private Label txtScale = new Label("??? mm/px");
	private BoxPhyValue boxDieW= new BoxPhyValue("寬").setType("mm").setValue("10mm");	
	private BoxPhyValue boxDieH= new BoxPhyValue("高").setType("mm").setValue("10mm");
	
	private GridPane con = null;
	public Pane getConsole(){
		if(con!=null){			
			return null;//we just create one console
		}
		final double CHK_SIZE = 110.;

		JFXComboBox<String> chkWType = new JFXComboBox<String>();
		chkWType.setPrefWidth(CHK_SIZE);
		chkWType.getItems().addAll(
			"4''晶圓",
			"5''晶圓",
			"6''晶圓",
			"7''晶圓",
			"8''晶圓",
			"9''晶圓",
			"10''晶圓",
			"11''晶圓",
			"12''晶圓"
		);
		chkWType.getSelectionModel().select(diameter2index());
		chkWType.setOnAction(EVENT->{
			index2diameter(chkWType.getSelectionModel().getSelectedIndex());
			//TODO: refresh canvas
		});

		JFXComboBox<String> chkMethod = new JFXComboBox<String>();
		chkMethod.setPrefWidth(CHK_SIZE);
		chkMethod.getItems().addAll(
			"method-1",
			"method-2"
		);
		chkMethod.getSelectionModel().select(0);
		chkMethod.setOnAction(EVENT->{
			//TODO: refresh canvas
		});
		
		boxDieW.setOnAction(EVENT->{
			
		});
		boxDieH.setOnAction(EVENT->{
			
		});
		
		con = new GridPane();
		con.getStyleClass().add("grid-small");
		con.addRow(0,new Label("掃描方式"),new Label("："),chkMethod);
		con.addRow(1,new Label("晶圓大小"),new Label("："),chkWType);		
		con.addRow(2,new Label("顆粒寬")  ,new Label("："),boxDieW);
		con.addRow(3,new Label("顆粒高")  ,new Label("："),boxDieH);
		con.addRow(4,new Label("比例尺")  ,new Label("："),txtScale);
		con.add(txtInfo, 0, 5, 4, 1);
		
		return PanDecorate.group("配置圖設定",con);
	}

	@Override
	void drawShape(GraphicsContext gc) {
		gc.save();
		gc.strokeArc(
			-mapGrid[0]/2, -mapGrid[1]/2, 
			mapGrid[0],mapGrid[1],
			0, 360, 
			ArcType.CHORD
		);
		gc.restore();
	}
}
