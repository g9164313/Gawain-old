package prj.scada;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.Gauge.SkinType;
import eu.hansolo.medusa.GaugeBuilder;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import narl.itrc.Misc;
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
	}
	
	@Override
	public Node eventLayout(PanBase pan) {
		
		Button btn = new Button("test");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(event->{
			//spinning(true);
			Misc.logv("---check---");
		});
		
		VBox lay_setting = new VBox();
		lay_setting.getStyleClass().add("vbox-medium");
		lay_setting.getChildren().addAll(
			devSQM160.build("SQM160"),
			devSPIK2K.build("SPKI2000"),
			btn
		);

		WidMapPiping map = new WidMapPiping(Misc.pathSock+"PID.xml");
		
		BorderPane root = new BorderPane();
		root.setRight(lay_setting);
		root.setCenter(map);
		root.setLeft(lay_gauge());
		return root;
	}
	
	private Node lay_gauge(){
		GridPane lay = new GridPane();//show all sensor
		lay.getStyleClass().add("grid-medium-vertical");
		
		Gauge gg1 = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.animated(true)
			.title("溫度-1")
			.unit("Å/s")
			.minValue(10)
			.maxValue(250)
			.build();

		Gauge gg2 = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.animated(true)
			.title("溫度-2")
			.unit("AAA")
			.minValue(10)
			.maxValue(40)
			.build();
		
		Gauge gg3 = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.animated(true)
			.title("溫度-3")
			.unit("AAA")
			.minValue(10)
			.maxValue(40)
			.build();

		Gauge gg4 = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.animated(true)
			.title("溫度-4")
			.unit("AAA")
			.minValue(10)
			.maxValue(40)
			.build();
		
		Gauge gg5 = GaugeBuilder.create()
			.skinType(SkinType.DASHBOARD)
			.animated(true)
			.title("溫度-4")
			.unit("AAA")
			.minValue(10)
			.maxValue(40)
			.build();
		
		lay.add(gg1, 0, 0);
		lay.add(gg2, 0, 1);
		lay.add(gg3, 0, 2);
		lay.add(gg4, 0, 3);
		lay.add(gg5, 0, 4);
		return lay;
	}
}
