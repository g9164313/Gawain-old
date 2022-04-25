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

public class PanMain1 extends PanBase {
	
	final DevDCG100 dcg1 = new DevDCG100();	
	final DevCESAR  csar = new DevCESAR();
	final DevSQM160 sqm1 = new DevSQM160();
	
	//final LayGauges gauges = LayGauges.getInstance();
	final LayLogger  logger = new LayLogger();		
	final LayLadder  ladder = new LayLadder();
	
	public PanMain1(final Stage stg) {
		super(stg);
		stg.setTitle("一號濺鍍機");
		stg.setOnShown(e->on_shown());
	}
	
	private void on_shown(){
		String arg;
		arg = Gawain.prop().getProperty("DCG100", "");
		if(arg.length()>0) {			
			dcg1.open(arg);
			logger.bindProperty(dcg1);
		}
		arg = Gawain.prop().getProperty("CESAR", "");
		if(arg.length()>0) {
			csar.open(arg);
		}
		arg = Gawain.prop().getProperty("SQM160", "");
		if(arg.length()>0) {			
			sqm1.open(arg);
			logger.bindProperty(sqm1);
		}
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final HBox lay3 = new HBox();
		lay3.getStyleClass().addAll("box-pad");
		lay3.getChildren().addAll(
			DevDCG100.genPanel(dcg1),
			DevCESAR.genCtrlPanel(csar)
		);
		final ScrollPane lay2 = new ScrollPane(lay3);
		lay2.setPrefViewportWidth(800);
		lay2.setMinViewportHeight(500);
		
		final JFXTabPane lay1 = new JFXTabPane();
		lay1.getTabs().addAll(
			new Tab("監測",logger),
			new Tab("製程",ladder),
			new Tab("裝置",lay3)
		);
		lay1.getSelectionModel().select(0);

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
		lay0.getChildren().add(Misc.addBorder(DevDCG100.genCtrlPanel(dcg1)));				
		return lay0;
	}
}





