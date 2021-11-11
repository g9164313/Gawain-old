package prj.sputter;

import com.jfoenix.controls.JFXTabPane;

import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanMain extends PanBase {
	
	final DevCouple coup = new DevCouple();
	final DevDCG100 dcg1 = new DevDCG100();	
	final DevSPIK2k spik = new DevSPIK2k();
	final DevSQM160 sqm1 = new DevSQM160();
	
	//final LayGauges gauges = LayGauges.getInstance();
	final LayLogger  logger = new LayLogger();	
	final DrawVaccum digram = new DrawVaccum(coup);	
	final LayLadder  ladder = new LayLadder(logger,coup,dcg1,spik,sqm1);
	
	public PanMain(final Stage stg) {
		super(stg);
		stg.setTitle("濺鍍機");
		stg.setOnShown(e->on_shown());
	}
	
	private void on_shown(){
		String arg;
		arg = Gawain.prop().getProperty("modbus", "");
		if(arg.length()!=0) {			
			coup.open(arg);
			logger.bindProperty(coup);
		}
		arg = Gawain.prop().getProperty("DCG100", "");
		if(arg.length()>0) {			
			dcg1.open(arg);
			logger.bindProperty(dcg1);
		}
		arg = Gawain.prop().getProperty("SPIK2k", "");
		if(arg.length()>0) {
			spik.open(arg);
		}
		arg = Gawain.prop().getProperty("SQM160", "");
		if(arg.length()>0) {			
			sqm1.open(arg);
			logger.bindProperty(sqm1);
		}
	}
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		final HBox lay3 = new HBox();
		lay3.getStyleClass().addAll("box-pad");
		lay3.getChildren().addAll(
			DevDCG100.genPanel(dcg1),
			DevSPIK2k.genPanel(spik),
			DevSQM160.genPanel(sqm1)
		);
		final ScrollPane lay2 = new ScrollPane(lay3);
		lay2.setPrefViewportWidth(800);
		lay2.setMinViewportHeight(500);
		
		final JFXTabPane lay1 = new JFXTabPane();
		lay1.getTabs().addAll(
			new Tab("管路",digram),
			new Tab("監測",logger),
			new Tab("製程",ladder),
			new Tab("裝置",lay3)
		);
		lay1.getSelectionModel().select(2);

		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay1);
		lay0.setRight(lay_ctrl());
		return lay0;
	}
	
	private Pane lay_ctrl() {
		//rad[0].setDisable(true);
		//rad[0].setStyle("-fx-opacity: 1.0;");		
		final VBox lay0 = new VBox();
		lay0.getStyleClass().addAll("box-pad");
		lay0.getChildren().add(Misc.addBorder(DevSQM160.genCtrlPanel(sqm1)));
		lay0.getChildren().add(Misc.addBorder(StepGunsHub.genCtrlPanel()));
		lay0.getChildren().add(Misc.addBorder(StepFlowCtrl.genCtrlPanel()));		
		lay0.getChildren().add(Misc.addBorder(DevDCG100.genCtrlPanel(dcg1)));				
		return lay0;
	}
}





