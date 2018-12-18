package prj.daemon;

import java.io.IOException;

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
	
	final String name = "C:\\labor\\zip32.csv";//for test~~~
	
	public PanZipcodeTW() throws IOException{
		
		ZipcodeTW.buid(name);
		
		//ZipcodeTW.notation("14號含附號");
		//String code = ZipcodeTW.mark2vcode("1374巷256弄90之2號21樓");
		//int[] mark = ZipcodeTW.vcode2mark(code);
	}
	
	@Override
	public Node eventLayout(PanBase pan) {
		
		GridPane root = new GridPane();//show all sensor
		root.getStyleClass().add("grid-large");
		
		Label txtCode = new Label(TXT_UNKNOWN);
		
		JFXTextField boxAddr = new JFXTextField("新竹市東區研發六路20號");
		boxAddr.setPromptText("輸入地址後，按 Enter 鍵會產生 3+2郵遞區號");
		boxAddr.setLabelFloat(true);
		boxAddr.setPrefWidth(230);
		boxAddr.setOnAction(event->{
			String txt = ZipcodeTW.parse(boxAddr.getText());
			txtCode.setText(txt);
		});
	
		Button btnBuild = PanBase.genButton1("訓練","");
		btnBuild.setOnAction(event->{
			ZipcodeTW.buid(name);
			//ZipcodeTW.flatten("C:\\labor\\ggyy.java");
		});
		
		Button btnReset = PanBase.genButton1("分析","");
		btnReset.setOnAction(event->{
			//How to put data in clipboard ??
			String txt = ZipcodeTW.parse(boxAddr.getText());
			txtCode.setText(txt);
		});

		HBox layCommand = new HBox(btnBuild,btnReset);
		layCommand.setAlignment(Pos.BASELINE_RIGHT);
		layCommand.getStyleClass().add("hbox-small");
		
		root.addRow(0, new Label("郵遞區號:"), txtCode);
		root.addRow(2, new Label("道路地址:"), boxAddr);
		root.add(layCommand,0,3,2,1);
		
		return root;
	}
	@Override
	public void eventShown(Object[] args) {
	}
}
