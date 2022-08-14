package prj.shelter;

import java.time.LocalDate;
import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTabPane;

import javafx.scene.Node;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.PanBase;

public class PanMain extends PanBase {

	final DevHustIO hustio= new DevHustIO();
	final DevAT5350 at5350= new DevAT5350();
	final DevCDR06  cdr06 = new DevCDR06();
	
	final ManBooker booker = new ManBooker();
	final LayPogsql pogsql = new LayPogsql();//調閱機器紀錄
	final LayLadder ladder = new LayLadder();	
		
	public PanMain(final Stage stg){
		super(stg);
		
		RadiateStep.hustio= hustio;
		RadiateStep.at5350= at5350;
		RadiateStep.cdr06 = cdr06;
		RadiateStep.booker= booker;
		
		stage().setOnShown(e->on_shown());
		//stage().setOnCloseRequest(e->on_close());
	}
	
	private void on_shown(){
		booker.reload();
		//hustio.open();
		//at5350.open();
		//cdr06.open();
		//DataBridge.getInstance();		
	}
	
	private void on_close(){
		booker.restore();		
		//hustio.asyncHaltOn();
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final HBox lay_dev = new HBox();
		lay_dev.getStyleClass().addAll("box-pad");
		lay_dev.getChildren().addAll(
			PanBase.border("HustIO", DevHustIO.genPanel(hustio)),
			PanBase.border("AT5350", DevAT5350.genPanel(at5350))
		);
		
		final JFXTabPane lay_tabs = new JFXTabPane();
		lay_tabs.getTabs().addAll(
			new Tab("照射"),
			new Tab("標定紀錄",booker),
			new Tab("校正流程",ladder),			
			new Tab("操作設備",lay_dev)
		);
		//預設顯示的頁籤
		//lay_tabs.getSelectionModel().select(0);
		//lay_tabs.getSelectionModel().select(1);
		lay_tabs.getSelectionModel().select(2);
		//lay_tabs.getSelectionModel().select(3);
		//lay_tabs.getSelectionModel().select(4);

		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay_tabs);
		lay0.setRight(gen_info_panel());
		return lay0;
	}
	
	private Pane gen_info_panel(){
		
		final Label[] inf = {
			new Label("日期："),new Label(LocalDate.now().toString()),
			new Label("溫度："),new Label(),
			new Label("濕度："),new Label(),
			new Label("壓力："),new Label(),
			new Label("強度："),new Label(),
			new Label("位置："),new Label(),
			new Label("照射："),new Label(),
		};
		for(int i=0 ;i<inf.length; i++){
			if(inf[i]==null) { continue; }
			inf[i].getStyleClass().add("font-size6");
		}
		
		inf[ 3].textProperty().bind(cdr06.getPropCelsius());//溫度
		inf[ 5].textProperty().bind(cdr06.getPropHumidity());//濕度
		inf[ 7].textProperty().bind(cdr06.getPropPression());//壓力		
		inf[ 9].textProperty().bind(hustio.activity.asString());//強度
		inf[11].textProperty().bind(hustio.locationText);//位置
		inf[13].textProperty().bind(hustio.leftTimeText);//照射
		
		final double btn_height = 64.;
		
		final JFXButton btn_meas = new JFXButton("量測");
		btn_meas.getStyleClass().add("btn-raised-1");
		btn_meas.setPrefHeight(btn_height);
		btn_meas.disableProperty().bind(at5350.isIdle.not());
		btn_meas.setOnAction(e->{
			final TextInputDialog dia = new TextInputDialog("1:00");
			dia.setTitle("照射時間");
			dia.setHeaderText("");
			dia.setContentText("");
			final Optional<String> opt = dia.showAndWait();
			if(opt.isPresent()==false) {
				return;
			}
			at5350.asyncPopMeasure(
				opt.get(),
				cdr06.getTxtTemperature(),
				cdr06.getTxtPression()
			);
		});
		AnchorPane.setRightAnchor(btn_meas, 7.);
		AnchorPane.setLeftAnchor (btn_meas, 7.);
		AnchorPane.setBottomAnchor(btn_meas, btn_height*2.+21.);
		
		final JFXButton btn_kick = new JFXButton("開始");
		btn_kick.getStyleClass().add("btn-raised-1");
		btn_kick.setPrefHeight(btn_height);
		btn_kick.disableProperty().bind(hustio.isMoving.or(hustio.isRadiant));
		btn_kick.setOnAction(e->{			
		});
		AnchorPane.setRightAnchor(btn_kick, 7.);
		AnchorPane.setLeftAnchor (btn_kick, 7.);
		AnchorPane.setBottomAnchor(btn_kick, btn_height*1.+14.);
		
		final JFXButton btn_stop = new JFXButton("停止");
		btn_stop.getStyleClass().add("btn-raised-0");
		btn_stop.setPrefHeight(btn_height);
		btn_stop.setOnAction(e->{
			hustio.asyncHaltOn();
			//at5350.syncAbort();
		});
		AnchorPane.setRightAnchor(btn_stop, 7.);
		AnchorPane.setLeftAnchor (btn_stop, 7.);
		AnchorPane.setBottomAnchor(btn_stop, 7.);
	
		
		final GridPane lay2 = new GridPane();
		lay2.getStyleClass().addAll("box-pad","font-size3","font-console");
		lay2.addRow(0, inf[ 0], inf[ 1]);
		lay2.add(new Separator(), 0, 2, 2, 1);
		lay2.addRow(3, inf[ 2], inf[ 3]);
		lay2.addRow(4, inf[ 4], inf[ 5]);
		lay2.addRow(5, inf[ 6], inf[ 7]);
		lay2.add(new Separator(), 0, 6, 2, 1);
		lay2.addRow(7, inf[ 8], inf[ 9]);
		lay2.addRow(8, inf[10], inf[11]);
		lay2.addRow(9, inf[12], inf[13]);
		lay2.add(new Separator(), 0, 10, 2, 1);

		
		final AnchorPane lay1 = new AnchorPane();
		lay1.getChildren().addAll(lay2,btn_meas,btn_kick,btn_stop);
		return lay1;
	}
}
