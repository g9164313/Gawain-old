package narl.itrc;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class PanTTY extends PanBase {
	
	private static final String MSG_CONNECT = "連線"; 
	private static final String MSG_DISCONNECT = "離線";
	
	public PanTTY(){
		
	}

	private static class Attr{
		public String key = "";
		public int val = 0;
		public Attr(String k, int v){
			key = k;
			val = v;
		}
	}
		
	@Override
	public Node eventLayout(PanBase self) {
		
		final JFXCheckBox chkUseHEX = new JFXCheckBox("16 進制");

		final TextField boxPrompt = new TextField();
		boxPrompt.setOnAction(e->{
			boxPrompt.setText("");//clear for next command....
		});
		HBox.setHgrow(boxPrompt, Priority.ALWAYS);		
		
		final HBox layPrompt = new HBox();
		layPrompt.setAlignment(Pos.CENTER);
		layPrompt.getChildren().addAll(
			new Label(">>"),
			boxPrompt,
			chkUseHEX
		);

		final JFXComboBox<Attr> cmbBaud = new JFXComboBox<Attr>();
		cmbBaud.getItems().addAll(
			new Attr("   300",  300),   
			new Attr("   600",  600), 
			new Attr("  1200", 1200),   
			new Attr("  1800", 1800),  
			new Attr("  2400", 2400),  
			new Attr("  4800", 4800),  
			new Attr("  9600", 9600),  
			new Attr(" 19200", 19200),
			new Attr(" 38400", 38400), 
			new Attr(" 57600", 57600),
			new Attr("115200",115200),
			new Attr("230400",230400),
			new Attr("460800",460800),
			new Attr("500000",500000)
		);
		cmbBaud.getSelectionModel().select(6);
		
		final JFXComboBox<Attr> cmbDataBit = new JFXComboBox<Attr>();
		
		final JFXComboBox<Attr> cmbPartityBit = new JFXComboBox<Attr>();
		
		final JFXComboBox<Attr> cmbStopBit = new JFXComboBox<Attr>();
		
		
		
		final Button btnLinking = PanBase.genButton2(MSG_CONNECT,null);
		btnLinking.setOnAction(e->{
			
		});
		
		final Label txtDummy = new Label(" ");
		txtDummy.setMaxHeight(Double.MAX_VALUE);
		VBox.setVgrow(txtDummy, Priority.ALWAYS);
		
		final VBox lay2 = new VBox();
		lay2.getChildren().addAll(
			cmbBaud,
			txtDummy,
			btnLinking
		);
		
		final BorderPane lay1 = new BorderPane();
		lay1.setBottom(layPrompt);		
		
		final BorderPane lay0 = new BorderPane();
		lay0.setStyle("-fx-padding: 13px;");
		lay0.setCenter(lay1);
		lay0.setRight(lay2);
		return lay0;
	}

	@Override
	public void eventShown(PanBase self) {

	}
}
