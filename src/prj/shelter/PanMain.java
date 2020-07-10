package prj.shelter;

import com.jfoenix.controls.JFXTabPane;

import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PanMain extends PanBase {

	final DevHustIO hustio= new DevHustIO();
	final DevAT5350 at5350= new DevAT5350();
	final DevCDR06  cdr06 = new DevCDR06();
	
	final LayLadder ladder = new LayLadder(hustio,at5350,cdr06);
	
	public PanMain(){
		stage().setOnShown(e->on_shown());		
	}
	
	private void on_shown(){
		String arg;
		arg = Gawain.prop().getProperty("hustio", "");
		if(arg.length()!=0) {			
			hustio.open(arg);
		}
		arg = Gawain.prop().getProperty("at5350", "");
		if(arg.length()!=0) {			
			at5350.open(arg);
		}
		arg = Gawain.prop().getProperty("cdr06", "");
		if(arg.length()!=0) {			
			cdr06.open(arg);
		}
		//DataBridge.getInstance();
	}
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		final HBox lay2 = new HBox();
		lay2.getChildren().addAll(
			PanBase.border("HustIO", DevHustIO.genPanelD(hustio)),
			PanBase.border("AT5350", DevAT5350.genPanel(at5350))
		);
		
		final JFXTabPane lay1 = new JFXTabPane();
		lay1.getTabs().addAll(			
			new Tab("調閱", new LayQuery()),
			new Tab("資料庫"),
			new Tab("執行",ladder),
			new Tab("設備",lay2)
		);
		lay1.getSelectionModel().select(3);

		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay1);
		lay0.setRight(gen_info_panel());
		return lay0;
	}
	
	private Pane gen_info_panel(){
		
		Label[] inf = new Label[8];
		for(int i=0; i<inf.length; i++){
			inf[i] = new Label();
			inf[i].setMinWidth(80);
		}		
		inf[0].textProperty().bind(cdr06.getChannelText(1));
		inf[1].textProperty().bind(cdr06.getChannelText(2));
		inf[2].textProperty().bind(cdr06.getChannelText(3));
		
		inf[3].textProperty().bind(hustio.isotope);
		inf[4].textProperty().bind(hustio.location);
		inf[5].textProperty().bind(hustio.remain_t);
		
		GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad-inner","font-size5");
		lay1.addRow(0, new Label("壓力："), inf[0]);
		lay1.addRow(1, new Label("濕度："), inf[1]);
		lay1.addRow(2, new Label("溫度："), inf[2]);
		lay1.add(new Separator(), 0, 3, 2, 1);
		lay1.addRow(4, new Label("源種："), inf[3]);
		lay1.addRow(5, new Label("位置："), inf[4]);
		lay1.addRow(6, new Label("時間："), inf[5]);
		lay1.add(new Separator(), 0, 7, 2, 1);
		lay1.add(new Separator(), 0, 9, 2, 1);
		
		VBox lay0 = new VBox(lay1);
		lay0.getStyleClass().addAll("box-pad","border");
		return lay0;
	}
}