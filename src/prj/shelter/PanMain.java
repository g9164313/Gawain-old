package prj.shelter;

import java.math.BigDecimal;

import com.jfoenix.controls.JFXTabPane;

import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PanMain extends PanBase {

	final DevHustIO hustio= new DevHustIO();
	final DevAT5350 at5350= new DevAT5350();
	final DevCDR06  cdr06 = new DevCDR06();
	
	final LayLadder ladder = new LayLadder(hustio,at5350,cdr06);
	
	public final DataRadiation radpro = new DataRadiation();
	
	public PanMain(final Stage stg){
		super(stg);
		
		BigDecimal val = new BigDecimal("1.2345");
		int pp = val.precision();
		int ss = val.scale();
		if((pp-ss)>=2) {
			val = val.movePointLeft(1);
		}
		String txt = val.toString();
		stage().setOnShown(e->on_shown());		
	}
	
	private void on_shown(){
		String arg;
		arg = Gawain.prop().getProperty("HUSTIO", "");
		if(arg.length()!=0) { hustio.open(arg); }
		arg = Gawain.prop().getProperty("AT5350", "");
		if(arg.length()!=0) { at5350.open(arg);	}
		arg = Gawain.prop().getProperty("CDR06", "");
		if(arg.length()!=0) { cdr06.open(arg); }
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
			new Tab("調閱",new LayQuery()),
			new Tab("校正",ladder),
			new Tab("設備",lay2)
		);
		lay1.getSelectionModel().select(2);

		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay1);
		lay0.setRight(gen_info_panel());
		return lay0;
	}
	
	private Pane gen_info_panel(){
		
		final String f_size="font-size2";
		
		final Label[] inf = {
			new Label("大氣溫度："),new Label(),
			new Label("大氣濕度："),new Label(),
			new Label("大氣壓力："),new Label(),
			new Label("輻射源種："),new Label(),
			new Label("輻射劑量："),new Label(),
			new Label("照射位置："),new Label(),
			new Label("照射時間："),new Label(),
		};
		for(int i=0 ;i<inf.length; i++){
			inf[i].getStyleClass().addAll(f_size);
			if(i%2==1) {
				inf[i].setMinWidth(90);
			}
		}
		
		inf[1].textProperty().bind(cdr06.getPropTemperature());
		inf[3].textProperty().bind(cdr06.getPropHumidity());
		inf[5].textProperty().bind(cdr06.getPropPression());
		
		//inf[ 7].textProperty().bind(hustio.isotope);
		//inf[ 9].textProperty().bind();
		inf[11].textProperty().bind(hustio.locationText);
		inf[13].textProperty().bind(hustio.leftTime);
		
		GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad");
		lay1.addRow(0, inf[ 0], inf[ 1]);
		lay1.addRow(1, inf[ 2], inf[ 3]);
		lay1.addRow(2, inf[ 4], inf[ 5]);
		lay1.add(new Separator(), 0, 3, 2, 1);
		lay1.addRow(4, inf[ 6], inf[ 7]);
		lay1.addRow(5, inf[ 8], inf[ 9]);
		lay1.addRow(6, inf[10], inf[11]);
		lay1.addRow(7, inf[12], inf[13]);
		lay1.add(new Separator(), 0, 8, 2, 1);
		lay1.add(new Separator(), 0, 9, 2, 1);
		return lay1;
	}
}
