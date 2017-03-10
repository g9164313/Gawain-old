package prj.scada;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import narl.itrc.PanBase;

public class PanSputter extends PanBase {

	public PanSputter(){
		customStyle = "-fx-background-color: #FEFEFE;";
	}
	
	private DevSQM160 devSQM160 = new DevSQM160();
		
	@Override
	protected void eventShown(WindowEvent e){
		devSQM160.open("/dev/ttyS0,19200,8n1");
		devSQM160.exec("@");
	}
	
	@Override
	public Node eventLayout() {
		
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("vbox-medium");
		lay0.getChildren().addAll(
			devSQM160.build("SQM160")
		);
		
		BorderPane root = new BorderPane();
		root.setRight(lay0);
		return root;
	}
}
