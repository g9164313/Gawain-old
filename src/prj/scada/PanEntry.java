package prj.scada;

import com.jfoenix.controls.JFXTabPane;

import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import narl.itrc.PanBase;

public class PanEntry extends PanBase {

	private DevSPIK2000 dev = new DevSPIK2000();
	
	public PanEntry(){
	}

	@Override
	public Node eventLayout(PanBase self) {
		
		final Tab[] tabs = {
			new Tab("管路控制", new PID_Widget()),
			new Tab("資訊面板", Layout_1.gen_gauge_scope(dev)),
			new Tab("腳本編輯"),
			new Tab("其他"),	
		};
		final JFXTabPane lay1 = new JFXTabPane();
		lay1.getTabs().addAll(tabs);
		lay1.getSelectionModel().select(0);
		
		final TitledPane tps[] ={
			new TitledPane("SPIK2000", Layout_1.gen_information(dev)),
			new TitledPane("test1", new Button("test1")),
			new TitledPane("test2", new Button("test1"))
		};
		final Accordion lay2 = new Accordion();
		lay2.getPanes().addAll(tps);
		lay2.setExpandedPane(tps[0]);
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		lay0.setLeft(lay2);
		lay0.setCenter(lay1);
		lay0.setRight(Layout_1.gen_action_button());
		return lay0;
	}

	@Override
	public void eventShown(PanBase self) {
		//dev.link("\\\\.\\COM2,19200,8n1");
	}

	@Override
	public void eventClose(PanBase self) {
		//dev.unlink();
	}
}
