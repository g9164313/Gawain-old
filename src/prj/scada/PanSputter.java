package prj.scada;

import com.jfoenix.controls.JFXDecorator;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import narl.itrc.PanBase;

public class PanSputter extends PanBase {

	public PanSputter(){
		customStyle = "-fx-background-color: #FFFFFF;";
	}
	
	private DevSQM160 devSQM160 = new DevSQM160();
		
	@Override
	protected void eventShown(WindowEvent e){
		//devSQM160.open("/dev/ttyS0,19200,8n1");
		//devSQM160.exec("@");
	}
	
	@Override
	public Node eventLayout() {
		
		Button btn = new Button("test");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(event->{
			PanBase.notifyError("ggyy", "text1");
		});
		
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("vbox-medium");
		lay0.getChildren().addAll(
			devSQM160.build("SQM160"),
			btn
		);
		
		BorderPane root = new BorderPane();
		root.setRight(lay0);
		return root;
	}
}
