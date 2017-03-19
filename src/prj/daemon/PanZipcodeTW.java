package prj.daemon;

import com.jfoenix.controls.JFXTextField;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import narl.itrc.PanBase;

/**
 * GUI for training and generating Tawain 3+2 Zipcode
 * @author qq
 *
 */
public class PanZipcodeTW extends PanBase {
	
	private final String TXT_UNKNOWN = "???";
	
	public PanZipcodeTW(){
	}
	
	@Override
	public Node eventLayout() {
		
		GridPane root = new GridPane();//show all sensor
		root.getStyleClass().add("grid-medium");
		
		Label txtCode = new Label(TXT_UNKNOWN);
		
		JFXTextField boxAddr = new JFXTextField("新竹市東區研發六路20號");
		boxAddr.setPromptText("輸入地址後，按 Enter 鍵會產生 3+2郵遞區號");
		boxAddr.setPrefWidth(230);
		boxAddr.setOnAction(event->{
			String txt = ZipcodeTW.parse(boxAddr.getText());
			txtCode.setText(txt);
		});
	
		Button btnBuild = PanBase.genButton1("訓練","");
		btnBuild.setOnAction(event->{
			String name = "C:\\labor\\zip32.csv";//for test~~~
			ZipcodeTW.buid(name);
			ZipcodeTW.flatten("C:\\labor\\ggyy.java");
		});
		
		Button btnReset = PanBase.genButton1("清除","");
		btnReset.setOnAction(event->{
			//How to put data in clipboard ??
			txtCode.setText(TXT_UNKNOWN);
			boxAddr.setText("");
		});

		HBox layCommand = new HBox(btnBuild,btnReset);
		layCommand.setAlignment(Pos.BASELINE_RIGHT);
		layCommand.getStyleClass().add("hbox-small");
		
		root.addRow(0, new Label("郵遞區號:"), txtCode);
		root.addRow(1, new Label("道路地址:"), boxAddr);
		root.add(layCommand,0,2,2,1);
		
		return root;
	}

}
