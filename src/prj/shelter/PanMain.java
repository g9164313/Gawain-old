package prj.shelter;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;

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
	final LayLadder ladder = new LayLadder(hustio,at5350,cdr06,abacus);	
		
	public PanMain(final Stage stg){
		super(stg);
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
		
		abacus.reloadLast();
	}
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		final HBox lay_dev = new HBox();
		lay_dev.getStyleClass().addAll("box-pad");
		lay_dev.getChildren().addAll(
			PanBase.border("HustIO", DevHustIO.genPanel(hustio)),
			PanBase.border("AT5350", DevAT5350.genPanel(at5350))
		);
		
		final JFXTabPane lay_tabs = new JFXTabPane();
		lay_tabs.getTabs().addAll(			
			new Tab("調閱",pogsql),
			new Tab("模型",abacus),
			new Tab("校正",ladder),			
			new Tab("設備",lay_dev)
		);
		lay_tabs.getSelectionModel().select(3);//預設顯示的頁籤

		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay_tabs);
		lay0.setRight(gen_info_panel());
		return lay0;
	}
	
	private Pane gen_info_panel(){
		
		final String f_size="font-size3";
		
		final Label[] inf = {
			new Label("衰退："),new Label(),
			new Label("溫度："),new Label(),
			new Label("濕度："),new Label(),
			new Label("壓力："),new Label(),
			new Label("強度："),new Label(),
			new Label("位置："),new Label(),
			new Label("照射："),new Label(),
		};
		for(int i=0 ;i<inf.length; i++){
			if(inf[i]==null) { continue; }
			inf[i].getStyleClass().addAll(f_size);
		}
		
		inf[ 1].textProperty().bind(abacus.endofday.asString());//衰退
		inf[ 3].textProperty().bind(cdr06.getPropCelsius());//溫度
		inf[ 5].textProperty().bind(cdr06.getPropHumidity());//濕度
		inf[ 7].textProperty().bind(cdr06.getPropPression());//壓力		
		inf[ 9].textProperty().bind(hustio.activity.asString());//強度
		inf[11].textProperty().bind(hustio.locationText);//位置
		inf[13].textProperty().bind(hustio.leftTimeText);//照射
		
		inf[ 9].getStyleClass().add("box-border");
		inf[ 9].setOnMouseClicked(e->{
			
		});
		
		final JFXTextField box_dose = new JFXTextField();
		final JFXTextField box_loca = new JFXTextField();
		final JFXTextField box_time = new JFXTextField();
		
		box_time.setPromptText("照射時間(mm:ss)");
		box_time.setText("03:00");		
		box_dose.setPromptText("輸入劑量("+LayAbacus.Model.DOSE_UNIT+")");
		box_dose.setOnAction(e->{
			//parse parameter~~~~
			final String loca = abacus.predict_loca(
				Double.valueOf(box_dose.getText()),
				hustio.activity
			);
			box_loca.setText(loca);
		});
		box_loca.setPromptText("輸入距離("+LayAbacus.Model.LOCA_UNIT+")");
		box_loca.setOnAction(e->{
			final String dose = abacus.predict_dose(
				Double.valueOf(box_loca.getText()),
				hustio.activity
			);
			box_dose.setText(dose);
		});
		
		final double btn_height = 64.;
		final JFXButton btn_make_radiation = new JFXButton("照射");
		btn_make_radiation.getStyleClass().add("btn-raised-1");
		btn_make_radiation.setPrefHeight(btn_height);
		btn_make_radiation.disableProperty().bind(hustio.isMoving.or(hustio.isRadiant));
		btn_make_radiation.setOnAction(e->{			
			final String time = box_time.getText();
			long left_time = Misc.text2tick(time);
			if(left_time==0L) {
				PanBase.notifyInfo("", "請設定照射時間!!");
				return;
			}
			final String loca = box_loca.getText();
			if(loca.length()==0) {
				//依據現在位置立即照射
				hustio.asyncRadite(left_time);
				return;
			}else if(loca.matches("[\\d]+[.]?(\\d+)?")==false) {
				PanBase.notifyError("", "不合法的數字表示  - "+loca);
				return;
			}
			//先移動至目標位置，再進行照射
			hustio.asyncWorking(loca+" cm",left_time);
		});
		AnchorPane.setRightAnchor(btn_make_radiation, 7.);
		AnchorPane.setLeftAnchor (btn_make_radiation, 7.);
		AnchorPane.setBottomAnchor(btn_make_radiation, btn_height+7.+7.);
		
		final JFXButton btn_stop_radiation = new JFXButton("停止");
		btn_stop_radiation.getStyleClass().add("btn-raised-0");
		btn_stop_radiation.setPrefHeight(btn_height);
		btn_stop_radiation.setOnAction(e->hustio.asyncHaltOn());
		AnchorPane.setRightAnchor(btn_stop_radiation, 7.);
		AnchorPane.setLeftAnchor (btn_stop_radiation, 7.);
		AnchorPane.setBottomAnchor(btn_stop_radiation, 7.);
		
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
		lay1.add(box_dose, 0, 11, 2, 1);
		lay1.add(box_loca, 0, 12, 2, 1);
		lay1.add(box_time, 0, 13, 2, 1);


		final AnchorPane lay2 = new AnchorPane();
		lay2.getChildren().addAll(
			lay1,
			btn_make_radiation,
			btn_stop_radiation
		);
		
		return lay2;
	}
}
