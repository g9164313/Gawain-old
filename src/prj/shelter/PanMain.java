package prj.shelter;

import java.time.LocalDate;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;

import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;
import narl.itrc.UtilPhysical;

public class PanMain extends PanBase {

	final DevHustIO hustio= new DevHustIO();
	final DevAT5350 at5350= new DevAT5350();
	final DevCDR06  cdr06 = new DevCDR06();
	
	final LayAbacus abacus = new LayAbacus();	
	final LayPogsql pogsql = new LayPogsql();//調閱機器紀錄
	final LayLadder ladder = new LayLadder(abacus);	
		
	public PanMain(final Stage stg){
		super(stg);
		
		RadiateStep.hustio= hustio;
		RadiateStep.at5350= at5350;
		RadiateStep.cdr06 = cdr06;
		RadiateStep.abacus= abacus;
		
		stage().setOnShown(e->on_shown());		
	}
	
	private void on_shown(){
		abacus.reloadLast();
		
		//hustio.open();
		//at5350.open();
		//cdr06.open();
		//DataBridge.getInstance();		
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
			new Tab("調閱",pogsql),
			new Tab("照射"),
			new Tab("標定",abacus),
			new Tab("程序",ladder),			
			new Tab("設備",lay_dev)
		);
		lay_tabs.getSelectionModel().select(2);//預設顯示的頁籤

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
		final JFXButton btn_kick = new JFXButton("開始");
		btn_kick.getStyleClass().add("btn-raised-1");
		btn_kick.setPrefHeight(btn_height);
		btn_kick.disableProperty().bind(hustio.isMoving.or(hustio.isRadiant));
		btn_kick.setOnAction(e->{			
		});
		AnchorPane.setRightAnchor(btn_kick, 7.);
		AnchorPane.setLeftAnchor (btn_kick, 7.);
		AnchorPane.setBottomAnchor(btn_kick, btn_height+7.+7.);
		
		final JFXButton btn_stop = new JFXButton("停止");
		btn_stop.getStyleClass().add("btn-raised-0");
		btn_stop.setPrefHeight(btn_height);
		btn_stop.setOnAction(e->{
			hustio.asyncHaltOn();
		});
		AnchorPane.setRightAnchor(btn_stop, 7.);
		AnchorPane.setLeftAnchor (btn_stop, 7.);
		AnchorPane.setBottomAnchor(btn_stop, 7.);
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad","font-size3","font-console");
		lay1.addRow(0, inf[ 0], inf[ 1]);
		lay1.add(new Separator(), 0, 2, 2, 1);
		lay1.addRow(3, inf[ 2], inf[ 3]);
		lay1.addRow(4, inf[ 4], inf[ 5]);
		lay1.addRow(5, inf[ 6], inf[ 7]);
		lay1.add(new Separator(), 0, 6, 2, 1);
		lay1.addRow(7, inf[ 8], inf[ 9]);
		lay1.addRow(8, inf[10], inf[11]);
		lay1.addRow(9, inf[12], inf[13]);
		lay1.add(new Separator(), 0, 10, 2, 1);

		final AnchorPane lay2 = new AnchorPane();
		lay2.getChildren().addAll(
			lay1,
			btn_kick,
			btn_stop
		);
		return lay2;
	}
}
