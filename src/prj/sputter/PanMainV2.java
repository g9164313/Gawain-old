package prj.sputter;

import com.jfoenix.controls.JFXTabPane;

import javafx.scene.Node;
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
import prj.sputter.action.Bumper;

public class PanMainV2 extends PanBase {
	
	final DevCouple coup = new DevCouple();
	final DevDCG100 dcg1 = new DevDCG100();	
	final DevSPIK2k spik = new DevSPIK2k();
	final DevSQM160 sqm1 = new DevSQM160();
	final DevSQM2Usb sqm2 = new DevSQM2Usb();
	
	final LayLogger  logger = new LayLogger();	
	final DrawVaccum digram = new DrawVaccum(coup);	
	final LayLadder  ladder = new LayLadder();
	
	public PanMainV2(final Stage stg) {
		super(stg);
		stg.setTitle("二號濺鍍機");
		stg.setOnShown(e->on_shown());
		ladder.logger = logger;
		ladder.dcg1 = dcg1;
		Bumper.coup = coup;		
		Bumper.dcg1 = dcg1;
		Bumper.spik = spik;
		Bumper.sqm1 = sqm1;
		Bumper.logg = logger;
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
		sqm1.open();		
		sqm2.open();
		logger.bindProperty(sqm1);
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		//rad[0].setDisable(true);
		//rad[0].setStyle("-fx-opacity: 1.0;");
		
		final HBox lay3 = new HBox();
		lay3.getStyleClass().addAll("box-pad");
		lay3.getChildren().addAll(
			DevDCG100.genPanel(dcg1),
			DevSPIK2k.genPanel(spik)
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
		lay0.setLeft(lay_info());
		lay0.setCenter(lay1);		
		lay0.setRight(lay_ctrl());
		return lay0;
	}
	
	private Pane lay_info() {
		final VBox lay = new VBox();
		lay.getStyleClass().addAll("box-pad");
		lay.getChildren().addAll(
			Misc.addBorder(DevSQM2Usb.genCtrlPanel(sqm2)),
			Misc.addBorder(DevSQM160.genCtrlPanel(sqm1))	
		);
		return lay;
	}	
	private Pane lay_ctrl() {
		final VBox lay = new VBox();
		lay.getStyleClass().addAll("box-pad");
		lay.getChildren().addAll(
			//Misc.addBorder(StepGunsHub.genCtrlPanel()),
			//Misc.addBorder(StepFlowCtrl.genCtrlPanel()),		
			Misc.addBorder(DevDCG100.genCtrlPanel(dcg1))
		);				
		return lay;
	}
}





