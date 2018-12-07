package prj.puppet;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import narl.itrc.ButtonEx;
import narl.itrc.Gawain;
import narl.itrc.PanBase;
import narl.itrc.UtilTaskTool;

public class WidRecorder extends PanBase {
	
	private WidMonitor monitor;
	
	public WidRecorder(WidMonitor monitor){
		this.monitor = monitor;
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
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
			
		final ButtonEx btnMarkSet= new ButtonEx("定位標記").setStyleBy("btn-raised-4");		
		btnMarkSet.setOnAction(e->monitor.markLocate(boxROI));
		btnMarkSet.setMaxWidth(Double.MAX_VALUE);
			
		final ButtonEx btnRecTest= new ButtonEx("OCR 測試").setStyleBy("btn-raised-4");
		btnRecTest.setOnTask(e1->{
			int[] roi = UtilTaskTool.getTextRoi(boxROI);
			((ButtonEx)e1.getSource()).arg = monitor.recognizeDigital(roi);
		}, e2->{
			String txt = (String)((ButtonEx)e2.getSource()).arg;
			txtRecog.setText(txt);
		});
				
		btnRecTest.setMaxWidth(Double.MAX_VALUE);
			
		final ButtonEx btnMarkDel= new ButtonEx("清除標記").setStyleBy("btn-raised-4");		
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
		layROI.add(new Label("辨識結果："), 0, 2, 1, 1); layROI.add(txtRecog, 1, 2, 3, 1);		
		layROI.add(btnMarkSet, 0, 3, 4, 1);
		layROI.add(btnRecTest, 0, 4, 4, 1);
		layROI.add(btnMarkDel, 0, 5, 4, 1);
					
		final TextArea boxScript = new TextArea();
		//boxScript.setMaxWidth(Double.MAX_VALUE);
		//boxScript.setMaxHeight(Double.MAX_VALUE);
		//boxScript.setPrefSize(143, 143);

		final ButtonEx btnMouseTrace = new ButtonEx(
			"追蹤右鍵",null,
			"取消追蹤",null
		);
		btnMouseTrace.setMaxWidth(Double.MAX_VALUE);
		btnMouseTrace.setOnToggle(eventOn->{
			monitor.setHookEvent(event2->{
				boxScript.appendText(String.format(
					"mo.click(%d,%d);\n",
					monitor.getCursorX(),
					monitor.getCursorY()
				));
				boxScript.setScrollTop(Double.MAX_VALUE);
			});
		},eventOff->{
			monitor.setHookEvent(null);
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
			
		final ButtonEx btnScriptExec = new ButtonEx(
			"執行","play.png",
			"取消","pause.png"
		).setStyleBy("btn-raised-3");
		btnScriptExec.setMaxWidth(Double.MAX_VALUE);
		btnScriptExec.setOnTask(event1->{			
			try {
				String txt = UtilTaskTool.getText(boxScript);
				ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
				nashorn.put("mo", monitor);
				nashorn.eval(txt);
				UtilTaskTool.clear();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		},null);
				
		final ButtonEx btnScriptSave = new ButtonEx(
			"儲存","content-save.png"
		).setStyleBy("btn-raised-3");	
		btnScriptSave.setMaxWidth(Double.MAX_VALUE);
		btnScriptSave.setOnAction(event->{	
			FileChooser dia = new FileChooser();
			dia.setTitle("另存...");
			dia.setInitialDirectory(Gawain.dirSock);
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
			
		final ButtonEx btnScriptLoad = new ButtonEx(
			"載入","upload.png"
		).setStyleBy("btn-raised-3");	
		btnScriptLoad.setMaxWidth(Double.MAX_VALUE);
		btnScriptLoad.setOnAction(event->{
			FileChooser dia = new FileChooser();
			dia.setTitle("讀取...");
			dia.setInitialDirectory(Gawain.dirSock);
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
			btnMouseTrace,
			btnAppendMark,
			btnScriptSave,
			btnScriptLoad,
			btnScriptExec,
			new Label("ROI"),
			layROI
		);
		
		final VBox lay1 = new VBox();
		lay1.getStyleClass().add("vbox-medium");
		lay1.getChildren().addAll(
			new Label("腳本程序"),
			boxScript
		);
		VBox.setVgrow(boxScript, Priority.ALWAYS);
		
		//---- main frame----//
		final BorderPane layRoot = new BorderPane();
		layRoot.getStyleClass().add("layout-small");
		layRoot.setLeft(lay0);
		layRoot.setCenter(lay1);
		//layRoot.setRight(lay0);
		return layRoot;
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

	@Override
	public void eventShown(PanBase self) {
		// TODO Auto-generated method stub
		
	}
}
