package prj.scada;

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
	
	private DevSPIK2000 devSPIK2K = new DevSPIK2000();
	
	@Override
	protected void eventShown(WindowEvent e){
		//devSQM160.open("/dev/ttyS0,19200,8n1");
		//devSQM160.exec("@");
		spinning(true);
	}
	
	@Override
	public Node eventLayout() {
		
		Button btn = new Button("test");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(event->{
			spinning(true);
		});
		
		VBox lay_setting = new VBox();
		lay_setting.getStyleClass().add("vbox-medium");
		lay_setting.getChildren().addAll(
			devSQM160.build("SQM160"),
			devSPIK2K.build("SPKI2000"),
			btn
		);

		BorderPane root = new BorderPane();
		root.setRight(lay_setting);
		return root;
	}
}
