package prj.refuge;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.virtualbox_4_3.IMachine;
import org.virtualbox_4_3.VirtualBoxManager;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import narl.itrc.DiaChoice;
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
		lstMark[1].sheet,
		lstMark[2].sheet,
	};
	
	private Node gen_mark_table(){
		final TabPane lay0 = new TabPane();
		lay0.setSide(Side.BOTTOM);
		tabs = lay0.getSelectionModel();
		Tab tt[] = {new Tab(), new Tab(), new Tab()};
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
		btnLoadMark.setMaxWidth(Double.MAX_VALUE);
		btnLoadMark.setOnAction(event->{
			//ask where is file.
			FileChooser diaFile = new FileChooser();
			diaFile.setTitle("匯入 Excel");
			diaFile.setInitialDirectory(Gawain.dirHome);
			File fs = diaFile.showOpenDialog(Misc.getParent(event));
			if(fs==null){
				return;
			}
			//dispatch task to load Excel file.
			TaskLoadExcel tsk = new TaskLoadExcel();
			tsk.file = fs;
			tsk.sheet= lstSheet;
			tsk.nmark= new Alert(AlertType.CONFIRMATION,"是否使用預測值？")
				.showAndWait()
				.get();
			spinner.kick('p',tsk);
		});
		
		final Button btnAutoMark = PanBase.genButton3("產生標定","toc.png");
		btnAutoMark.setMaxWidth(Double.MAX_VALUE);
		
		final Button btnMeasure  = PanBase.genButton3("開始量測","toc.png");
		btnMeasure.setMaxWidth(Double.MAX_VALUE);
		btnMeasure.setOnAction(event->{
			spinner.kick('p',new TaskMeasure(lstSheet));
		});
		
		final Button btnSaveMark = PanBase.genButton2("匯出Excel","briefcase-download.png");
		btnSaveMark.setMaxWidth(Double.MAX_VALUE);
		
		final Button btnStartVM = PanBase.genButton2("虛擬機器","developer_board.png");
		btnStartVM.setMaxWidth(Double.MAX_VALUE);
		btnStartVM.setOnAction(e->{
			monitor = TaskSandbox.factory(monitor);
		});
		
		final Button btnTest = PanBase.genButton2("測試用","developer_board.png");
		btnTest.setMaxWidth(Double.MAX_VALUE);
		btnTest.setOnAction(e->{
			sandbox.appear(monitor);
		});
		
		VBox lay0 = new VBox();
		lay0.getStyleClass().add("vbox-small");
		lay0.getChildren().addAll(
			btnLoadMark,
			btnAutoMark,
			btnMeasure,
			btnSaveMark,
			btnStartVM,
			btnTest
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

	private TaskSandbox monitor = null;
	
	private PanSandbox sandbox = new PanSandbox();
		
	@Override
	public void eventShown(PanBase self) {
		monitor = TaskSandbox.factory(monitor);
	}
	
	@Override
	public void eventClose(PanBase self) {
	}
	
	public static double formularNextYearDose(double val){
		return val * 0.97716;
	}
	
	public static double formularNextYearLoca(double val){
		return ((val+90.) * 0.988) - 90.;
	}
}
