package prj.puppet;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import narl.itrc.ButtonExtra;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.TaskTool;

public class PanPuppeteer extends PanBase {

	public PanPuppeteer(){		
	}
	

	@Override
	public Node eventLayout(PanBase self) {
		
		final WidMonitor monitor = new WidMonitor(1024,768);
		
		final VBox layDeft = new VBox();
		layDeft.getStyleClass().add("vbox-small");
				
		final VBox layLink = new VBox();
		//lay2.setStyle("-fx-padding: 10;");
		layLink.getStyleClass().add("vbox-one-dir");
		
		final VBox layFunc = new VBox();
		layFunc.getStyleClass().add("vbox-one-dir");
		layFunc.disableProperty().bind(layLink.disabledProperty().not());
		
		final Pane layAdvn = create_advance_panel(monitor);
		layAdvn.disableProperty().bind(layLink.disabledProperty().not());
		
		final JFXTextField boxAddress = new JFXTextField("172.16.2.144");
		
		final ButtonExtra btnConnect = new ButtonExtra(
			"連線","lan-disconnect.png",
			"離線","lan-connect.png"
		).setOnToggle(eventOn->{
			monitor.loopStart(boxAddress.getText());
			layLink.setDisable(true);
		},eventOff->{
			monitor.loopStop();
			layLink.setDisable(false);
		});
		btnConnect.setMaxWidth(Double.MAX_VALUE);

		
		
		final ButtonExtra btnAutopump = new ButtonExtra("自動抽氣");
		btnAutopump.setMaxWidth(Double.MAX_VALUE);
		
		final ButtonExtra btnLeaveVaccum = new ButtonExtra("破真空");
		btnLeaveVaccum.setMaxWidth(Double.MAX_VALUE);
		
		//---- the block of speed button----//
		layLink.getChildren().addAll(
			new Label("主機 IP 位置"),
			boxAddress
		);
		layFunc.getChildren().addAll(
			btnAutopump,
			btnLeaveVaccum
		);
		layDeft.getChildren().addAll(
			layLink,
			btnConnect,
			layFunc
		);
		
		//---- main frame----//
		final BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("layout-small");
		lay0.setLeft(layAdvn);
		lay0.setCenter(monitor);
		lay0.setRight(layDeft);
		return lay0;
	}
	
	private Pane create_advance_panel(final WidMonitor monitor){
		
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
		
		final ButtonExtra btnMarkSet= new ButtonExtra("定位標記").setStyleBy("btn-raised-4");		
		btnMarkSet.setOnAction(e->monitor.markLocate(boxROI));
		btnMarkSet.setMaxWidth(Double.MAX_VALUE);
		
		final ButtonExtra btnRecTest = new ButtonExtra("OCR 測試").setStyleBy("btn-raised-4");		
		btnRecTest.setOnAction(event->{
			//monitor.recognize(get_geom(boxROI),txtRecog)
		});
		btnRecTest.setMaxWidth(Double.MAX_VALUE);
		
		final ButtonExtra btnMarkDel = new ButtonExtra("清除標記").setStyleBy("btn-raised-4");		
		btnMarkDel.setOnAction(e->monitor.markClear());
		btnMarkDel.setMaxWidth(Double.MAX_VALUE);
		
		GridPane.setHgrow(txtRecog, Priority.ALWAYS);
		GridPane.setHgrow(btnMarkSet, Priority.ALWAYS);
		GridPane.setHgrow(btnRecTest, Priority.ALWAYS);
		GridPane.setHgrow(btnMarkDel, Priority.ALWAYS);				
		
		final GridPane layROI = new GridPane();
		layROI.getStyleClass().add("grid-small");
		layROI.addRow(0, new Label("X"), boxROI[0], new Label("Y"), boxROI[1]); 
		layROI.addRow(1, new Label("寬"),boxROI[2], new Label("高"), boxROI[3]);
		layROI.add(new Label("結果："), 0, 2, 1, 1); layROI.add(txtRecog, 1, 2, 3, 1);		
		layROI.add(btnMarkSet, 0, 3, 4, 1);
		layROI.add(btnRecTest, 0, 4, 4, 1);
		layROI.add(btnMarkDel, 0, 5, 4, 1);
				
		final TextArea boxScript = new TextArea();
		boxScript.setMaxWidth(Double.MAX_VALUE);
		boxScript.setPrefSize(143, 143);

		final JFXCheckBox chkTraceMouse = new JFXCheckBox("追蹤右鍵");
		chkTraceMouse.setMaxWidth(Double.MAX_VALUE);
		chkTraceMouse.setOnAction(event1->{
			if(chkTraceMouse.isSelected()==true){
				monitor.setHookEvent(event2->{
					boxScript.appendText(String.format(
						"mo.click(%d,%d);\n",
						monitor.getCursorX(),
						monitor.getCursorY()
					));
					boxScript.setScrollTop(Double.MAX_VALUE);
				});
			}else{
				monitor.setHookEvent(null);
			}
		});
		
		final JFXButton btnAppendMark = new JFXButton("OCR標記");		
		btnAppendMark.getStyleClass().add("btn-raised-3");
		btnAppendMark.setMaxWidth(Double.MAX_VALUE);
		btnAppendMark.setOnAction(event->{			
			boxScript.appendText(String.format(
				"mo.recognize(%s,%s,%s,%s);\n",
				boxROI[0].getText(),boxROI[1].getText(),
				boxROI[2].getText(),boxROI[3].getText()
			));
			boxScript.setScrollTop(Double.MAX_VALUE);
		});
		
		final ButtonExtra btnScriptExec = new ButtonExtra(
			"執行","play.png",
			"取消","pause.png"
		).setStyleBy("btn-raised-3");
		btnScriptExec.setMaxWidth(Double.MAX_VALUE);
		btnScriptExec.setOnTask(event1->{			
			try {
				String txt = TaskTool.getText(boxScript);
				ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
				nashorn.put("mo", monitor);
				nashorn.eval(txt);
				TaskTool.clear();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		},null);
			
		final ButtonExtra btnScriptSave = new ButtonExtra(
			"儲存","content-save.png"
		).setStyleBy("btn-raised-3");	
		btnScriptSave.setMaxWidth(Double.MAX_VALUE);
		btnScriptSave.setOnAction(event->{	
			FileChooser dia = new FileChooser();
			dia.setTitle("另存...");
			dia.setInitialDirectory(Misc.dirSock);
			File fs = dia.showSaveDialog(getScene().getWindow());
			if(fs==null){
				return;
			}
			try {
				FileWriter fw = new FileWriter(fs);
				fw.write(boxScript.getText());
				fw.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		
		final ButtonExtra btnScriptLoad = new ButtonExtra(
			"載入","upload.png"
		).setStyleBy("btn-raised-3");	
		btnScriptLoad.setMaxWidth(Double.MAX_VALUE);
		btnScriptLoad.setOnAction(event->{
			FileChooser dia = new FileChooser();
			dia.setTitle("讀取...");
			dia.setInitialDirectory(Misc.dirSock);
			File fs = dia.showOpenDialog(getScene().getWindow());
			if(fs==null){
				return;
			}
			try {
				char[] buf = new char[4000];
				FileReader fr = new FileReader(fs);				
				fr.read(buf);
				fr.close();
				boxScript.setText(String.valueOf(buf));				
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
				
		final VBox lay0 = new VBox();
		//lay2.setStyle("-fx-padding: 10;");
		lay0.getStyleClass().add("vbox-one-dir");
		lay0.getChildren().addAll(			
			new Label("腳本程序"),
			boxScript,
			btnScriptExec,
			chkTraceMouse,
			btnAppendMark,
			btnScriptSave,
			btnScriptLoad,			
			new Label("ROI"),
			layROI
		);		
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
