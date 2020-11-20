package prj.sputter;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;

import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import narl.itrc.Gawain;
import narl.itrc.Ladder;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.Stepper;
import narl.itrc.init.LogStream;

public class PanMain extends PanBase {
	
	final ModCouple coup = new ModCouple();
	final DevDCG100 dcg1 = new DevDCG100();	
	final DevSPIK2k spik = new DevSPIK2k();
	final DevSQM160 sqm1 = new DevSQM160();
	
	final LayFlower flower = new LayFlower(coup);
	final LayGauges gauges = LayGauges.getInstance();	
	final Ladder    ladder = new Ladder();

	public PanMain() {
		super();
		init();
		stage().setOnShown(e->on_shown());
	}
	
	private void on_shown(){		
		//notifyTask(StepAnalysis.task_dump("0FAE64"));//debug		
		String arg;
		arg = Gawain.prop().getProperty("modbus", "");
		if(arg.length()!=0) {			
			coup.open(arg);
		}
		arg = Gawain.prop().getProperty("DCG100", "");
		if(arg.length()>0) {			
			dcg1.open(arg);
			gauges.bindProperty(dcg1);
		}
		arg = Gawain.prop().getProperty("SPIK2k", "");
		if(arg.length()>0) {
			spik.open(arg);
		}
		arg = Gawain.prop().getProperty("SQM160", "");
		if(arg.length()>0) {			
			sqm1.open(arg);
			gauges.bindProperty(sqm1);
		}
	}
	
	private void init(){
		//initial step-box for recipe
		ladder.addStep("分隔線", Stepper.Sticker.class);
		ladder.addStep("薄膜設定",StepSetFilm.class , sqm1);
		ladder.addStep("電極切換",StepGunsHub.class , coup);
		ladder.addStep("脈衝設定",StepSetPulse.class, spik);
		ladder.addStep("高壓設定",StepKindler.class , sqm1, dcg1, spik);
		ladder.addStep("流量控制",StepFlowCtrl.class, coup);
		ladder.addStep("厚度監控",StepWatcher.class , sqm1, dcg1);		
		ladder.addSack(
			"<單層鍍膜.3>", 
			Stepper.Sticker.class,
			StepSetFilm.class,
			StepKindler.class,
			StepWatcher.class
		);
		ladder.addSack(
			"<單層鍍膜.4>", 
			Stepper.Sticker.class,
			StepSetFilm.class,
			StepGunsHub.class,
			StepKindler.class,
			StepWatcher.class
		);
		ladder.addSack(
			"<單層鍍膜.5>", 
			Stepper.Sticker.class,
			StepSetFilm.class,
			StepSetPulse.class,
			StepGunsHub.class,			
			StepKindler.class,
			StepWatcher.class
		);
		ladder.prelogue = ()->{
			LogStream.getInstance().usePool(true);
		};
		ladder.epilogue = ()->{
			LogStream.getInstance().usePool(false);
			notifyTask(StepAnalysis.task_dump(ladder.uuid()));
		};
	}
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		final VBox lay4 = new VBox();
		lay4.getStyleClass().addAll("box-pad");
		lay4.getChildren().addAll(
			Misc.addBorder(flower.genPanel_5850E("O2",1,10)), //O₂
			Misc.addBorder(flower.genPanel_5850E("Ar",2,100))
			//Misc.addBorder(flower.genMassFlowCtrl("N₂",2)),			
			//Misc.addBorder(flower.genMassFlowCtrl("He",4))
		);
		
		final HBox lay3 = new HBox();
		lay3.getStyleClass().addAll("box-pad");
		lay3.getChildren().addAll(
			DevDCG100.genPanel(dcg1),
			DevSPIK2k.genPanel(spik),
			DevSQM160.genPanel(sqm1),
			lay4
		);

		final ScrollPane lay2 = new ScrollPane(lay3);
		lay2.setPrefViewportWidth(800);
		lay2.setMinViewportHeight(500);
		
		final JFXTabPane lay1 = new JFXTabPane();
		lay1.getTabs().addAll(
			new Tab("管路"),
			new Tab("監測",gauges),
			new Tab("製程",ladder),			
			new Tab("裝置",lay2)
		);
		lay1.getSelectionModel().select(2);

		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay1);
		lay0.setRight(lay_ctrl());
		return lay0;
	}
	
	private Pane lay_ctrl() {
		
		final JFXTextField[] box = new JFXTextField[5];
		for(int i=0; i<box.length; i++) {
			JFXTextField obj = new JFXTextField();
			obj.setMaxWidth(Double.MAX_VALUE);
			obj.setLabelFloat(true);
			box[i] = obj;
		}
		box[0].setPromptText("輸出功率(Watt)");
		box[0].setText("100");
		box[0].setOnAction(e->{
			try {				
				int val = Integer.valueOf(box[0].getText());
				//coup.asyncWriteVal(8006,val);
				dcg1.asyncExec("SPW="+val);
			}catch(NumberFormatException exp) {
			}
		});
		
		final JFXButton[] btn = new JFXButton[5];
		for(int i=0; i<btn.length; i++) {
			JFXButton obj = new JFXButton();
			obj.setMaxWidth(Double.MAX_VALUE);
			btn[i] = obj;
		}

		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setText("擋片-ON");
		btn[0].setOnAction(e->sqm1.shutter_on_zeros());
		
		btn[1].getStyleClass().add("btn-raised-2");
		btn[1].setText("擋片-OFF");
		btn[1].setOnAction(e->sqm1.shutter(false));
		
		btn[2].getStyleClass().add("btn-raised-1");
		btn[2].setText("電漿-ON");
		btn[2].setOnAction(e->{
			try {				
				int val = Integer.valueOf(box[0].getText());
				sqm1.zeros();
				dcg1.asyncBreakIn(()->{
					dcg1.exec("SPW="+val);
					dcg1.exec("TRG");
				});
			}catch(NumberFormatException exp) {
			}
		});
		btn[3].getStyleClass().add("btn-raised-2");
		btn[3].setText("電漿-OFF");
		btn[3].setOnAction(e->{
			//Misc.logw("關閉高壓電");
			dcg1.asyncExec("OFF");
		});
		
		final Label[] txt = new Label[6];
		for(int i=0; i<txt.length; i++) {
			Label obj = new Label();
			obj.setMaxWidth(Double.MAX_VALUE);
			txt[i] = obj;
		}
		txt[0].textProperty().bind(sqm1.filmData[0]);
		txt[1].textProperty().bind(sqm1.filmData[1]);
		txt[2].textProperty().bind(sqm1.filmData[2]);
		txt[3].textProperty().bind(sqm1.filmData[3]);
		txt[4].textProperty().bind(sqm1.filmData[7]);
		
		final GridPane lay2 = new GridPane();
		lay2.getStyleClass().addAll("box-pad-inner");
		lay2.addRow(0, new Label("薄膜名稱 ：" ), txt[0]);
		lay2.addRow(1, new Label("Density："), txt[1]);
		lay2.addRow(2, new Label("Tooling："), txt[2]);
		lay2.addRow(3, new Label("Z-Ratio："), txt[3]);		
		lay2.addRow(4, new Label("感測器編號："), txt[4]);
		lay2.setOnMouseClicked(e->sqm1.updateFilm());
		
		final JFXCheckBox[] rad = {
			new JFXCheckBox("Shutter"),
			new JFXCheckBox("Bipolar"),
			new JFXCheckBox("Unipolar"),
			new JFXCheckBox("Gun-1"),
			new JFXCheckBox("Gun-2"),
		};
		rad[0].setDisable(true);
		rad[0].selectedProperty().bind(sqm1.shutter);
		rad[0].setStyle("-fx-opacity: 1.0;");
		
		final ToggleGroup grp = new ToggleGroup();
		
		rad[1].setOnAction(e->{
			//a mutex for checking I/O
			if(rad[1].isSelected()){
				rad[2].setSelected(false);
				coup.asyncBreakIn(()->coup.writeVals(8007, 1));
			}else{
				coup.asyncBreakIn(()->coup.writeVals(8007, 0));
			}
		});
		rad[2].setOnAction(e->{
			//a mutex for checking I/O
			if(rad[2].isSelected()){
				rad[1].setSelected(false);
				coup.asyncBreakIn(()->coup.writeVals(8007, 2));
			}else{
				coup.asyncBreakIn(()->coup.writeVals(8007, 0));
			}
		});
		rad[3].setOnAction(e->coup.select_gun1(e));
		rad[4].setOnAction(e->coup.select_gun2(e));
		rad[3].visibleProperty().bind(rad[2].selectedProperty());
		rad[4].visibleProperty().bind(rad[2].selectedProperty());
		
		final GridPane lay3 = new GridPane();
		lay3.getStyleClass().addAll("box-pad-inner");
		lay3.addColumn(0, rad);
		
		//final Label _test_ = new Label();
		//_test_.textProperty().bind(dcg1.onTime);
		
		final VBox lay0 = new VBox();
		lay0.getStyleClass().addAll("box-pad","border");
		//lay0.getChildren().addAll(_test_,new Label(),box[0]);
		lay0.getChildren().addAll(new Label(),box[0]);
		lay0.getChildren().add(lay2);
		lay0.getChildren().add(new Separator());
		lay0.getChildren().add(lay3);
		lay0.getChildren().add(new Separator());
		lay0.getChildren().addAll(btn);
		return lay0;
	}
}





