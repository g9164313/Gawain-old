package prj.refuge;

import java.io.File;
import java.util.Optional;

import com.jfoenix.controls.JFXTabPane;

import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.WidTextSheet;

public class PanEntry2 extends PanBase {

	public PanEntry2(){
	}

	private SingleSelectionModel<Tab> tabs = null;
	
	private final WidMarkView lstMark[] = {
		new WidMarkView(),
		new WidMarkView(),
		new WidMarkView(),
	};
	
	private final WidTextSheet lstSheet[] = {
		lstMark[0].sheet,
		lstMark[0].sheet,
		lstMark[0].sheet,
	};
	
	private Node gen_mark_table(){
		final TabPane lay0 = new TabPane();
		lay0.setSide(Side.BOTTOM);
		tabs = lay0.getSelectionModel();
		Tab tt[] = { new Tab(), new Tab(), new Tab(),};
		tt[0].setText("3Ci");
		tt[0].setClosable(false);
		tt[0].setContent(lstMark[0]);
		tt[1].setText("0.5Ci");
		tt[1].setClosable(false);
		tt[1].setContent(lstMark[1]);
		tt[2].setText("0.05Ci");
		tt[2].setClosable(false);
		tt[2].setContent(lstMark[2]);		
		lay0.getTabs().addAll(tt);
		return lay0;
	}
	
	private Node gen_control_panel(){
		
		final Button btnLoadMark = PanBase.genButton2("匯入Excel","briefcase-upload.png");
		btnLoadMark.setOnAction(event->{
			//ask where is file.
			FileChooser diaFile = new FileChooser();
			diaFile.setTitle("匯入 Excel");
			diaFile.setInitialDirectory(Gawain.dirHome);
			File fs = diaFile.showOpenDialog(Misc.getParent(event));
			if(fs==null){
				return;
			}
			//ask which row data is read.
			Alert diaLoca = new Alert(AlertType.CONFIRMATION,"使用新距離？");
			Optional<ButtonType> optLoca = diaLoca.showAndWait();
			
			TaskLoadExcel tsk = new TaskLoadExcel();
			tsk.file = fs;
			tsk.sheet= lstSheet;
			if(optLoca.get()==ButtonType.OK){
				tsk.rowIndex = 7;
			}else{
				tsk.rowIndex = 6;
			}
			spinner.kick('m',tsk);
		});
		
		final Button btnAutoMark = PanBase.genButton2("產生標定","toc.png");
		
		final Button btnMeasure  = PanBase.genButton3("開始量測","toc.png");
		btnMeasure.setOnAction(event->{
			
		});
		
		final Button btnSaveMark = PanBase.genButton2("匯出Excel","briefcase-download.png");
		
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.getChildren().addAll(
			btnLoadMark,
			btnAutoMark,
			btnMeasure,
			btnSaveMark
		);
		return lay0;
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		BorderPane lay0 = new BorderPane();
		lay0.getStyleClass().add("vbox-small");		
		lay0.setCenter(gen_mark_table());
		lay0.setRight(gen_control_panel());
		return lay0;
	}
	
	@Override
	public void eventShown(PanBase self) {
		//demo code
		/*Task<Integer> tsk = new Task<Integer>(){
			@Override
			protected Integer call() throws Exception {
				long t1 = System.currentTimeMillis();
				while(isCancelled()==false){
					long t2 = System.currentTimeMillis();
					long curr = (t2 - t1)/1000L;
					updateProgress(curr, 10L);
					updateMessage(String.format("prog=%d",(int)curr));
					if(curr>=10L){
						break;
					}										
				}				
				return 3;
			}
		};		
		spinner.kick('m',tsk);*/
	}
}
