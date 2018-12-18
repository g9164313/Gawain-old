package prj.refuge;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import narl.itrc.Gawain;
import narl.itrc.PanBase;
import narl.itrc.UtilRandom;
import narl.itrc.vision.ImgPreviewX;

/**
 * This panel is designed for control virtual machine.<br>
 * It is real a user interface component.
 * @author qq
 *
 */
public class PanSandbox extends PanBase{

	private TaskSandbox model = null;

	public void appear(TaskSandbox m){
		model = m;
		this.appear();
	}
		
	@Override
	public Node eventLayout(PanBase self) {
		
		final Tab tabView = new Tab("螢幕畫面");
		tabView.setClosable(false);
		tabView.setId("");
		tabView.setContent(new ImgPreviewX());
		
		final TabPane lay2 = new TabPane();
		lay2.getTabs().add(tabView);

		final TextField boxPrompt = new TextField();
		boxPrompt.setOnAction(e1->{
			if(model==null){
				return;
			}
			model.sendScript(boxPrompt.getText(),null);
			boxPrompt.setText("");//clear for next command....
		});
		HBox.setHgrow(boxPrompt, Priority.ALWAYS);
		
		HBox layPrompt = new HBox();		
		VBox layCommand = new VBox();
		
		final Button btnAddScript = PanBase.genButton1("新增腳本",null);
		btnAddScript.setMaxWidth(Double.MAX_VALUE);
		btnAddScript.setOnAction(e->create_tab_for_script(lay2,""));
		
		final Button btnLoadScript = PanBase.genButton1("讀取腳本",null);
		btnLoadScript.setMaxWidth(Double.MAX_VALUE);
		btnLoadScript.setOnAction(e->{
			FileChooser dia = new FileChooser();
			dia.setTitle("讀取...");
			dia.setInitialDirectory(Gawain.dirSock);
			File fs = dia.showOpenDialog(getScene().getWindow());
			if(fs==null){
				return;
			}
			try {
				char[] buf = new char[8000];
				FileReader fr = new FileReader(fs);
				fr.read(buf);
				fr.close();
				create_tab_for_script(lay2,String.valueOf(buf));		
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		
		final Button btnSaveScript = PanBase.genButton1("儲存腳本",null);
		btnSaveScript.setMaxWidth(Double.MAX_VALUE);
		btnSaveScript.setOnAction(e->{
			String txt = get_current_script(lay2);
			if(txt.length()==0){
				return;
			}
			FileChooser dia = new FileChooser();
			dia.setTitle("另存...");
			dia.setInitialDirectory(Gawain.dirSock);
			File fs = dia.showSaveDialog(getScene().getWindow());
			if(fs==null){
				return;
			}
			try {
				FileWriter fw = new FileWriter(fs);
				fw.write(txt);
				fw.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
		
		final Button btnExecScript = PanBase.genButton2("執行腳本",null);
		btnExecScript.setMaxWidth(Double.MAX_VALUE);
		btnExecScript.setOnAction(e1->{
			if(model==null){
				return;
			}
			String txt = get_current_script(lay2);
			if(txt.length()==0){
				return;
			}
			layCommand.setDisable(true);
			layPrompt.setDisable(true);
			model.sendScript(txt, e2->{
				layCommand.setDisable(false);
				layPrompt.setDisable(false);
			});
		});
		
		final Button btnCheckPoint = PanBase.genButton2("紀錄畫面",null);
		btnCheckPoint.setMaxWidth(Double.MAX_VALUE);
		btnCheckPoint.setOnAction(e->{
			String file_name = UtilRandom.uuid(8) + ".png";
			String full_name = Gawain.pathSock+file_name;
			append_current_script(lay2,"m.checkPoint('"+file_name+"');\n");
			if(model!=null){
				model.recvSnapshot(
					full_name,
					tabView.getContent()					
				);
			}
		});
		
		layPrompt.setAlignment(Pos.CENTER);
		layPrompt.getChildren().addAll(
			new Label(">>"),
			boxPrompt
		);
		
		layCommand.getStyleClass().add("vbox-small");
		layCommand.getChildren().addAll(
			btnAddScript,
			btnLoadScript,
			btnSaveScript,
			btnExecScript,
			btnCheckPoint
		);
		
		BorderPane lay1 = new BorderPane();
		lay1.getStyleClass().add("vbox-small");
		lay1.setCenter(lay2);
		lay1.setBottom(layPrompt);		
		
		BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("vbox-small");		
		lay0.setCenter(lay1);
		lay0.setRight(layCommand);
		return lay0;
	}

	private String get_current_script(TabPane lay){
		Tab tab = lay.getSelectionModel().getSelectedItem();
		if(tab.getId().length()==0){
			return "";//this is view tab, no script in this component
		}
		return ((TextArea)tab.getContent()).getText();
	}
	
	private void append_current_script(TabPane lay,String txt){
		Tab tab = lay.getSelectionModel().getSelectedItem();
		if(tab.getId().length()==0){
			return;//this is view tab, no script in this component
		}
		((TextArea)tab.getContent()).appendText(txt);
	}
	
	private void create_tab_for_script(TabPane lay, String txt){
		
		final TextArea boxScript = new TextArea();
		boxScript.setMaxWidth(Double.MAX_VALUE);
		boxScript.setMaxHeight(Double.MAX_VALUE);
		boxScript.setText(txt);

		final String name = String.format(
			"腳本-%d", 
			lay.getTabs().size()
		);		
		final Tab tabScript = new Tab(name);
		tabScript.setId(name);
		tabScript.setContent(boxScript);
		tabScript.setClosable(true);
		//tabScript.setOnClosed(e->{	
		//});
		lay.getTabs().add(tabScript);
	}
	
	@Override
	public void eventShown(Object[] args) {
	}
}
