package prj.puppet;

import com.jfoenix.controls.JFXTextField;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.ButtonExtra;
import narl.itrc.PanBase;

public class PanPuppeteer2 extends PanBase {

	public PanPuppeteer2(){		
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-small");
		
		final VBox lay2 = new VBox();
		//lay2.setStyle("-fx-padding: 10;");
		lay2.getStyleClass().add("vbox-one-dir");
		lay2.setDisable(false);
		
		final VBox lay3 = new VBox();
		lay3.getStyleClass().add("vbox-one-dir");
		lay3.disableProperty().bind(lay2.disabledProperty().not());
		
		final WidMonitor monitor = new WidMonitor(1024,768);
		
		final JFXTextField boxAddress = new JFXTextField("172.16.2.144");
		
		final ButtonExtra btnConnect = new ButtonExtra(
			"連線","lan-disconnect.png",
			"離線","lan-connect.png"
		).setOnToggle(eventOn->{
			monitor.loopStart(boxAddress.getText());
			lay2.setDisable(true);
		},eventOff->{
			monitor.loopStop();
			lay2.setDisable(false);
		});
		btnConnect.setMaxWidth(Double.MAX_VALUE);
		
		final GridPane lay4 = new GridPane();
		
		final JFXTextField[] boxROI ={
			new JFXTextField("0"), new JFXTextField("0"),
			new JFXTextField("100"), new JFXTextField("100")
		};
		for(JFXTextField box:boxROI){
			box.setPrefWidth(64);
			box.setOnAction(e->monitor.markSet(get_geom(boxROI)));
		}
		final Label txtRecog = new Label();
		txtRecog.setMaxWidth(Double.MAX_VALUE);
		
		final ButtonExtra btnRecog = new ButtonExtra("辨識數字").setStyleBy("btn-raised-3");		
		btnRecog.setOnAction(e->monitor.recognize(get_geom(boxROI),txtRecog));
		btnRecog.setMaxWidth(Double.MAX_VALUE);
		
		final ButtonExtra btnMarkMake= new ButtonExtra("定位標記").setStyleBy("btn-raised-3");		
		btnMarkMake.setOnAction(e->monitor.markLocate(boxROI));
		btnMarkMake.setMaxWidth(Double.MAX_VALUE);
		
		final ButtonExtra btnMarkWipe = new ButtonExtra("清除標記").setStyleBy("btn-raised-3");		
		btnMarkWipe.setOnAction(e->monitor.markClear());
		btnMarkWipe.setMaxWidth(Double.MAX_VALUE);
		
		GridPane.setHgrow(txtRecog, Priority.ALWAYS);
		GridPane.setHgrow(btnMarkMake, Priority.ALWAYS);
		GridPane.setHgrow(btnMarkWipe, Priority.ALWAYS);
		GridPane.setHgrow(btnRecog, Priority.ALWAYS);
		
		lay4.getStyleClass().add("grid-small");
		lay4.addRow(0, new Label("X"), boxROI[0], new Label("Y"), boxROI[1]); 
		lay4.addRow(1, new Label("寬"),boxROI[2], new Label("長"), boxROI[3]);
		lay4.add(new Label("結果："), 0, 2, 1, 1);
		lay4.add(txtRecog   , 1, 2, 3, 1);
		lay4.add(btnRecog   , 0, 3, 4, 1);
		lay4.add(btnMarkMake, 0, 4, 4, 1);
		lay4.add(btnMarkWipe, 0, 5, 4, 1);
				
		final ButtonExtra btnAutopump = new ButtonExtra("自動抽氣");
		btnAutopump.setMaxWidth(Double.MAX_VALUE);
		
		final ButtonExtra btnLeaveVaccum = new ButtonExtra("破真空");
		btnLeaveVaccum.setMaxWidth(Double.MAX_VALUE);
		
		//---- the block of speed button----//
		lay2.getChildren().addAll(
			new Label("主機 IP 位置"),
			boxAddress
		);
		lay3.getChildren().addAll(
			btnAutopump,
			btnLeaveVaccum,
			lay4
		);
		lay1.getChildren().addAll(
			lay2,
			btnConnect,
			lay3
		);
		
		//---- main frame----//
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		lay0.setCenter(monitor);
		lay0.setRight(lay1);
		return lay0;
	}
	
	private int[] get_geom(JFXTextField[] box){
		int[] geom ={
			Integer.valueOf(box[0].getText()),
			Integer.valueOf(box[1].getText()),
			Integer.valueOf(box[2].getText()),
			Integer.valueOf(box[3].getText()),
		};
		return geom;
	}
}
