package prj.daemon;

import java.io.File;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;
import narl.itrc.BoxLogger;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanFringeEditor extends PanBase{

	public PanFringeEditor(){
	}
	
	private WidFringeMap map = new WidFringeMap();

	private FileChooser dia = new FileChooser();
	
	private String reportName = Misc.pathHome+"fringe.xml";
	
	@Override
	protected void eventShown(WindowEvent e){
		dia.setInitialDirectory(Misc.dirHome);
	}
	
	@Override
	public Node eventLayout(PanBase self) {
				
		final BoxLogger boxMesg = new BoxLogger();
		boxMesg.setPrefHeight(170);
		
		final Tab tab1 = new Tab();
		tab1.setText("Zernike");
		tab1.setContent(map.genPaneZernikePoly());
		
		final Tab tab2 = new Tab();
		tab2.setText("訊息欄");
		tab2.setContent(boxMesg);
		
		TabPane tabInfo = new TabPane();
		tabInfo.setSide(Side.LEFT);
		tabInfo.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		tabInfo.getTabs().addAll(tab1,tab2);

		final Button btnTest = PanBase.genButton2("test-image",null);
		btnTest.setMaxWidth(Double.MAX_VALUE);
		btnTest.setOnAction(e->{
			File fs = dia.showOpenDialog(Misc.getParent(e));
			if(fs!=null){
				map.loadImageFile(fs.toString());
			}
			//map.loadImageFile("../bang/target.png");
		});

		final Button btnLoad = PanBase.genButton2("load",null);
		btnLoad.setMaxWidth(Double.MAX_VALUE);
		btnLoad.setOnAction(e->{
			map.load(reportName);
		});
		final Button btnSave = PanBase.genButton2("save",null);
		btnSave.setMaxWidth(Double.MAX_VALUE);
		btnSave.setOnAction(e->{
			map.save(reportName);
		});
		
		final Button btnCalculate = PanBase.genButton2("Calculate",null);
		btnCalculate.setMaxWidth(Double.MAX_VALUE);
		btnCalculate.setOnAction(e->{
			tabInfo.getSelectionModel().select(tab2);
			map.calculate();
		});
		
		final Button btnUpdate = PanBase.genButton2("Update",null);
		btnUpdate.setMaxWidth(Double.MAX_VALUE);
		btnUpdate.setOnAction(e->{
			map.update();
		});
		
		/*final Button btn4 = PanBase.genButton2("save as...",null);
		btn5.setOnAction(e->{
			dia.setTitle("save as...");
			dia.setInitialFileName(reportName);
			File fs = dia.showSaveDialog(Misc.getParent(e));
			if(fs!=null){
				map.save(fs);
			}
		});
		final Button btn5 = PanBase.genButton2("load from...",null);
		btn6.setOnAction(e->{
			dia.setTitle("load");
			File fs = dia.showOpenDialog(Misc.getParent(e));
			if(fs!=null){
				reportName = fs.getAbsolutePath();
				map.load(fs);
			}
		});*/
				
		final VBox layAct = new VBox();
		layAct.getStyleClass().add("vbox-small");
		layAct.getChildren().addAll(
			btnTest,
			btnLoad,
			btnSave,
			btnCalculate,
			btnUpdate
		);		
				
		final BorderPane lay1 = new BorderPane();
		lay1.setCenter(map);
		lay1.setBottom(tabInfo);
		
		final BorderPane lay2 = new BorderPane();
		lay2.setRight(layAct);
		lay2.setCenter(lay1);
		
		return lay2;
	}
}
