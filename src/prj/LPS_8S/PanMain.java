package prj.LPS_8S;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXToggleButton;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TextSize;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PadTouch;
import narl.itrc.PanBase;

public class PanMain extends PanBase {
	
	private static final String TXT_AXIS_MAJOR = "主軸";
	private static final String TXT_AXIS_PRESS = "加壓軸";
	private static final String TXT_AXIS_SWING = "擺動軸";
	private static final String TXT_CYLI_IN1 = "氣壓缸（進）";
	private static final String TXT_CYLI_IN2 = "氣壓缸（出）";
	
	private ModInnerBus infobus = new ModInnerBus();
	private ModCoupler coupler = new ModCoupler();
	private LogHistory history = new LogHistory(infobus,coupler);
	
	private SignMotor[] sgm = {
		new SignMotor(TXT_AXIS_MAJOR),
		new SignMotor(TXT_AXIS_PRESS),
		new SignMotor(TXT_AXIS_SWING),
	};
	
	public PanMain() {
		init_gauge();
		stage().setOnShown(e->on_shown());
	}
	private void on_shown() {		
		String arg;
		arg = Gawain.prop().getProperty("INFOBUS", "");
		if(arg.length()!=0) {
			infobus.open(arg);
		}		
		arg = Gawain.prop().getProperty("COUPLER", "");
		if(arg.length()!=0) {
			coupler.open(arg);
		}
		sgm[0].attach(infobus, ModInnerBus.ID_MAJOR);
		sgm[1].attach(infobus, ModInnerBus.ID_PRESS);
		sgm[2].attach(infobus, ModInnerBus.ID_SWING);
	}
	
	private JFXToggleButton tglPumpSlurry;
	private JFXToggleButton tglMotorMajor;
	private JFXToggleButton tglMotorAuxil;
	
	private void check_toggle(final ToggleButton tgl,final boolean val) {
		if(tgl.isSelected()==val) {
			return;
		}
		tgl.setSelected(val);
		tgl.getOnAction().handle(null);
	}
	
	private final NotifyEvent[] e_working = {
		(lad,dlg)->{
			//prepare 
			//history.kick();			
			Misc.logv("ggyy-1");
		},
		(lad,dlg)->{
			//dlg.setText("開始澆灌");
			check_toggle(tglPumpSlurry,true);
			Misc.logv("ggyy-2");
		},
		(lad,dlg)->{
			//waiting for signal
			//lad.playFromStart();
			//lad.jumpTo(Duration.seconds(1.));//back to self			
			Misc.logv("ggyy-3");
			ladderJump(lad,3);
		},
		(lad,dlg)->{
			//dlg.setText("開始旋轉");
			check_toggle(tglMotorMajor,true);
			check_toggle(tglMotorAuxil,true);
		},
	};
	private final NotifyEvent[] e_halting = {
		(lad,dlg)->{
			//msg[0].setText("停止旋轉");
			check_toggle(tglMotorMajor,false);
			check_toggle(tglMotorAuxil,false);
		},
		(lad,dlg)->{
			//msg[0].setText("停止澆灌");
			check_toggle(tglPumpSlurry,false);
			//history.stop();
		},
	};
	
	/*private void on_timer_alarm(final AlarmEvent EVENT) {
		gag[0].setTitle("計時");//restore title~~~
	}
	private void on_working() {
		if(gag[0].getTimePeriod().getSeconds()>0L) {
			gag[0].setRunning(true);
		}
	}
	private void on_stopping() {
		gag[0].setRunning(false);
		//infobus.kickoff_all(false);
	}*/
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		final JFXToggleButton[] tgl = {
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton(),
		};
		tgl[0].setText("研磨液");			
		tgl[1].setText("旋轉軸");
		tgl[2].setText("研磨軸");

		tglPumpSlurry = tgl[0];
		tglPumpSlurry.setOnAction(e->{
			Misc.logv("pump-slurry!!");
		});
		tglMotorMajor = tgl[1];
		tglMotorMajor.setOnAction(e->{
			Misc.logv("motor-major");
		});
		tglMotorAuxil= tgl[2];
		tglMotorAuxil.setOnAction(e->{
			Misc.logv("motor-auxiliary");
		});
		
		final JFXButton[] btn = {
			new	JFXButton("裝模"),			
			new	JFXButton("拆模"),
			new	JFXButton("加工"),
			new	JFXButton("停止"),			
		};
		for(JFXButton obj:btn) {
			obj.setMinSize(100., 57.);
			obj.setMaxWidth(Double.MAX_VALUE);
		}
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setOnAction(e->{			
		});
		btn[1].getStyleClass().add("btn-raised-1");
		btn[1].setOnAction(e->{
		});
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setOnAction(e->notifyEvent(e_working));
		btn[3].getStyleClass().add("btn-raised-0");
		btn[3].setOnAction(e->notifyEvent(e_halting));
		
		final VBox lay_tgl = new VBox(tgl);
		lay_tgl.getStyleClass().add("box-pad");
		final VBox lay_btn = new VBox(
			new Separator(),
			btn[0],btn[1],
			new Separator(),
			btn[2],btn[3]
		);
		lay_btn.getStyleClass().add("box-pad");
		final BorderPane lay_ctrl = new BorderPane(
			lay_tgl,
			null,null,
			lay_btn, null
		);

		final GridPane lay_gagu = new GridPane();
		lay_gagu.getStyleClass().add("box-pad");
		lay_gagu.addColumn(0, gag[0], gag[4], gag[5]);
		lay_gagu.addColumn(1, gag[1], gag[2], gag[3]);
		lay_gagu.addColumn(2, sgm[0], sgm[1], sgm[2]);
		lay_gagu.addColumn(3, sgm[0].get_TOR_Tile(), sgm[1].get_TOR_Tile(), sgm[2].get_TOR_Tile());

		final JFXTabPane lay_tabs = new JFXTabPane();
		lay_tabs.getTabs().addAll(
			new Tab("儀表",lay_gagu),
			new Tab("監控",history)
		);
		lay_tabs.getSelectionModel().select(0);//select TABs
		
		return new BorderPane(
			lay_tabs,
			null,lay_ctrl,
			null,null				
		);
	}

	//計時, 
	//（研磨液）電導, 流量, 溫度,
	//（加壓氣缸）電控比例閥-1, 電控比例閥-2,
	private final Tile[] gag = new Tile[6];
	
	void init_gauge() {
		gag[0] = TileBuilder.create()
			.skinType(SkinType.COUNTDOWN_TIMER)
			.title("計時")
			.textSize(TextSize.BIGGER)
			.timePeriod(java.time.Duration.ofSeconds(-1L))
			.onAlarm(e->notifyEvent(e_halting))
			.build();
		gag[0].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('T',"時：分：秒");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			String txt = opt.get();
			long val = PadTouch.toSecondValue(txt);
			gag[0].setTitle("計時"+Misc.tick2text(val*1000,true,3));
			gag[0].setTimePeriod(java.time.Duration.ofSeconds(val));
		});
		gag[1] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("電導")
			.textSize(TextSize.BIGGER)
			.unit("PH")
			.maxValue(7)			
			.build();
		gag[2] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("流量")
			.textSize(TextSize.BIGGER)
			.unit("？？？")
			.maxValue(1000)			
			.build();
		gag[3] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("溫度")
			.textSize(TextSize.BIGGER)
			.unit("C")
			.maxValue(1000)			
			.build();
		gag[4] = TileBuilder.create()
			.skinType(SkinType.GAUGE2)
			.title("氣缸（進）")
			.textSize(TextSize.BIGGER)
			.unit("Torr")
			.maxValue(100)			
			.build();
		gag[4].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('n',"Torr");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			//String txt = opt.get();
		});
		gag[5] = TileBuilder.create()
			.skinType(SkinType.GAUGE2)
			.title("氣缸（出）")
			.textSize(TextSize.BIGGER)
			.unit("Torr")
			.maxValue(100)			
			.build();
		gag[5].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('n',"Torr");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			//String txt = opt.get();
		});
	}
}
