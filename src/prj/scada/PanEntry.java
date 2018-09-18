package prj.scada;

import com.jfoenix.controls.JFXTabPane;

import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PanEntry extends PanBase {

	private DevSPIK2000 spik = new DevSPIK2000();
	
	private DevSQM160 sqm = new DevSQM160("\\\\.\\COM2,19600,8n1");
	
	private DevPCI9113 pci0 = new DevPCI9113(0);
	
	private PID_Root pid = new PID_Root(Gawain.pathSock+"ggyy.xml");
	
	private DevHS2000 hs2k = new DevHS2000();
	
	public PanEntry(){
	}

	@Override
	public Node eventLayout(PanBase self) {
		//layout-1: device information and setting
		
		//pid.createLeaf(PID_Const.Pipe1UP, 5, 5);
		
		final TitledPane tps[] ={
			new TitledPane("SPIK2000", Layout_1.gen_information(spik)),
			new TitledPane("SQM160  ", DevSQM160.gen_panel(sqm)),
			new TitledPane("PCI9113 ", DevPCI9113.gen_panel(pci0)),			
		};
		final Accordion lay1 = new Accordion();
		lay1.getPanes().addAll(tps);
		lay1.setExpandedPane(tps[0]);
				
		//layout-2: diagram, gauge console and script editor
		final Tab[] tabs = {
			new Tab("管路控制", pid),			
			new Tab("資訊面板", Layout_1.gen_gauge_scope(spik)),
			new Tab("時間-頻譜", DevHS2000.gen_panel(hs2k)),
			new Tab("腳本編輯",new PID_Leaf().generate_brick()),			
			new Tab("其他"),	
		};
		final JFXTabPane lay2 = new JFXTabPane();
		lay2.getTabs().addAll(tabs);
		lay2.getSelectionModel().select(2);
		
		//layout-3: action button
		final Button[] btn = {
			PanBase.genButton3("edit-Mode", null),
			PanBase.genButton3("test-1", null),
			PanBase.genButton3("test-2", null),
		};
		for(int i=0; i<btn.length; i++){
			btn[i].setMaxWidth(Double.MAX_VALUE);
			btn[i].setMinWidth(100);
		}
		//something or event for test
		btn[0].setOnAction(event->{
			pid.editMode(true);
		});
		btn[1].setOnAction(event->{
		});
		btn[2].setOnAction(event->{
		});
		final VBox lay3 = new VBox();
		lay3.setStyle("-fx-padding: 7; -fx-spacing: 7;");
		lay3.getChildren().addAll(btn);
		
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		lay0.setLeft(lay1);
		lay0.setCenter(lay2);
		lay0.setRight(lay3);
		return lay0;
	}

	@Override
	public void eventShown(PanBase self) {
		//dev.link("\\\\.\\COM2,19200,8n1");
		//sqm.link();
		//pci0.link();
		hs2k.link();
	}

	@Override
	public void eventClose(PanBase self) {		
		//dev.unlink();
		//sqm.unlink();
		//pci0.unlink();
		hs2k.unlink();
	}
}
