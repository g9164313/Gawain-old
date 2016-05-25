package prj.letterpress;

import com.jfoenix.controls.JFXComboBox;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.BoxPhyValue;
import narl.itrc.PanDecorate;

public class PanMapWafer extends PanDecorate {
	
	private Canvas canMap = new Canvas();
	
	/**
	 * Draw a map to show the location of cursor or field.<p>
	 * It also decides how to operate motor(?).<p>
	 */
	public PanMapWafer(){
		super("配置圖");
	}

	@Override
	public Node layoutBody() {
		ScrollPane root = new ScrollPane();
		root.setContent(canMap);
		canMap.getGraphicsContext2D();
		return root;
	}
	
	private double wafDiameter = 8*25.4;//unit is millimeter
	
	private WritableImage imgGround = new WritableImage(1024,1024);

	private int diameter2index(){
		int idx = Math.round((float)(wafDiameter/25.4));
		idx = idx - 4;
		if(idx<0){ return 0; }
		if(idx>8){ return 8; }
		return idx;
	}
	
	private void index2diameter(int idx){
		wafDiameter = (idx+4)*25.4;//unit is millimeter
	}
	
	private GridPane console = null;	
	private Label txtScale = new Label("??? mil/px");
	private BoxPhyValue boxDieW = new BoxPhyValue("寬").setType("mm").setValue("5mm");	
	private BoxPhyValue boxDieH= new BoxPhyValue("高").setType("mm").setValue("5mm");
	public Pane getConsole(){
		if(console!=null){			
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
		
		console = new GridPane();
		console.getStyleClass().add("grid-small");
		console.addRow(0,new Label("掃描方式"),new Label("："),chkMethod);
		console.addRow(1,new Label("晶圓大小"),new Label("："),chkWType);		
		console.addRow(2,new Label("顆粒寬")  ,new Label("："),boxDieW);
		console.addRow(3,new Label("顆粒高")  ,new Label("："),boxDieH);
		console.addRow(4,new Label("比例尺")  ,new Label("："),txtScale);
				
		return PanDecorate.group("配置圖設定",console);
	}	
}
