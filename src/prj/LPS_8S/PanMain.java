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
import javafx.scene.layout.HBox;
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
	private static final String TXT_CYLI_UP = "氣壓缸（上提）";
	private static final String TXT_CYLI_DW = "氣壓缸（下壓）";
	
	private ModInsider ibus = new ModInsider();
	private ModCoupler coup = new ModCoupler();
	//private LogHistory hist = new LogHistory(ibus,coup);
	
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
		arg = Gawain.prop().getProperty("INSIDER", "");
		if(arg.length()!=0) {
			ibus.open(arg);
			ibus.playLoop();
		}		
		arg = Gawain.prop().getProperty("COUPLER", "");
		if(arg.length()!=0) {
			coup.working_press = ()->{
				Misc.logv("test");
				notifyEvent(e_working);
			};
			coup.working_float = ()->{
				notifyEvent(e_halting);
			};
			coup.emerged_press = ()->{
				toggle_btn(tglAlarm,true);
				notifyEvent(e_halting);
			};
			coup.emerged_float = ()->{
				toggle_btn(tglAlarm,false);
			};
			coup.open(arg);
		}
		sgm[0].attach(ibus, ModInsider.ID_MAJOR);
		sgm[1].attach(ibus, ModInsider.ID_PRESS);
		sgm[2].attach(ibus, ModInsider.ID_SWING);
	}

	private void toggle_btn(
		final ToggleButton tgl,
		final boolean val
	) {
		tgl.setUserData(val);
		tgl.setSelected(val);
		tgl.getOnAction().handle(null);
	}
	private void freeze_tgl(final ToggleButton tgl) {
		if(tgl.isSelected()==false) { return; }
		tgl.setSelected(false);
		tgl.getOnAction().handle(null);
	}
	private void recover_tgl(final ToggleButton tgl) {
		boolean val = (boolean) tgl.getUserData();
		if(val==false) { return; }
		tgl.setSelected(val);
		tgl.getOnAction().handle(null);
	}
	
	private JFXToggleButton tglAlarm;
	private JFXToggleButton tglSlurryHeat;
	private JFXToggleButton tglSlurryPump;
	private JFXToggleButton tglMotorMajor;
	private JFXToggleButton tglMotorOther;
	
	//計時, 
	//（研磨液）電導, 流量, 溫度,
	//（加壓氣缸）電控比例閥-1, 電控比例閥-2,
	private final Tile[] gag = new Tile[6];
	
	private NotifyEvent[] e_working = {
		(lad,dlg)->{
			dlg.setText("確認[拆模磁簧]");//1:沒卡，0:卡住
			ladderJump(lad,3);
		},
		(lad,dlg)->{
			dlg.setText("確認[主軸近接]");
			//ladderJump(lad,3);
		},
		(lad,dlg)->{
			toggle_btn(tglSlurryPump,true);
			dlg.setText("開始澆灌");
		},
		(lad,dlg)->{
			dlg.setText("開始旋轉");
			toggle_btn(tglMotorMajor,true);
		},
		(lad,dlg)->{
		},
		(lad,dlg)->{
			dlg.setText("開始旋轉");
			toggle_btn(tglMotorOther,true);
			if(gag[0].getTimePeriod().getSeconds()>0L) {
				gag[0].setRunning(true);
			}
		},
	};
	private final NotifyEvent[] e_halting = {
		(lad,dlg)->{
			dlg.setText("停止旋轉");
			toggle_btn(tglMotorMajor,false);
		},
		(lad,dlg)->{
			dlg.setText("停止旋轉");
			toggle_btn(tglMotorOther,false);
		},
		(lad,dlg)->{
			dlg.setText("停止澆灌");
			toggle_btn(tglSlurryPump,false);
		},
		(lad,dlg)->{
			toggle_btn(tglAlarm,false);
		},
	};
	private final NotifyEvent[] e_halting_alarm = {
		(lad,dlg)->{
			dlg.setText("停止旋轉");
			toggle_btn(tglMotorMajor,false);
		},
		(lad,dlg)->{
			dlg.setText("停止旋轉");
			toggle_btn(tglMotorOther,false);
		},
		(lad,dlg)->{
			dlg.setText("停止澆灌");
			toggle_btn(tglSlurryPump,false);
		},
		(lad,dlg)->{
			toggle_btn(tglAlarm,true);
		},
	};
	
	
	private final NotifyEvent[] e_unlock_master = {
		(ldd,dlg)->{
			dlg.setText("退出卡榫");
			ladderJump(ldd,1);
		}	
	};
	private final NotifyEvent[] e_lock_master = {
		(ldd,dlg)->{
			if(coup.flgMasterLock.get()==true) {
				ladderJump(ldd,-1);
				return;
			}
			if(coup.flgMasterUnLock.get()==true) {
				//TODO:汽缸準備
				ladderJump(ldd,-1);
				return;
			}
			dlg.setText("準備拆模");
			ibus.setSpeed(ModInsider.ID_MAJOR, 50);
			
		},
		(ldd,dlg)->{
			dlg.setText("等待卡榫位置");
			if(coup.flgMasterLock.get()==true) {
				return;
			}
			ladderJump(ldd,2);
		},
		(ldd,dlg)->{
			dlg.setText("推入卡榫");
			coup.toggleLatch(true);
		}
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
			new JFXToggleButton(),
			new JFXToggleButton(),
		};
		for(JFXToggleButton obj:tgl) {
			obj.setUserData(false);
		}
		tgl[0].setText("警報");
		tgl[1].setText("加熱");
		tgl[2].setText("研磨液");			
		tgl[3].setText("旋轉軸");
		tgl[4].setText("加工軸");
		
		tglAlarm = tgl[0];
		tglAlarm.setOnAction(e->{
			boolean flg = tglAlarm.isSelected();
			coup.toggleAlarm(flg);
		});
		tglSlurryHeat = tgl[1];
		tglSlurryHeat.setOnAction(e->{
			boolean flg = tglSlurryHeat.isSelected();
			coup.toggleHeater(flg);
		});
		tglSlurryPump = tgl[2];
		tglSlurryPump.setOnAction(e->{
			boolean flg = tglSlurryPump.isSelected();
			coup.toggleSlurry(flg);
		});
		tglMotorMajor = tgl[3];
		tglMotorMajor.disableProperty().bind(coup.flgMasterUnLock.not());
		tglMotorMajor.setOnAction(e->{
			if(tglMotorMajor.isDisable()==true) {
				return;
			}
			boolean flg = tglMotorMajor.isSelected();
			ibus.kickoff(ModInsider.ID_MAJOR,flg);
		});
		tglMotorOther = tgl[4];
		tglMotorOther.setOnAction(e->{
			boolean flg = tglMotorOther.isSelected();
			ibus.kickoff(ModInsider.ID_OTHER,flg);
		});
		
		final JFXButton[] btn = {
			new	JFXButton("裝模"),			
			new	JFXButton("拆模"),			
		};
		for(JFXButton obj:btn) {
			obj.setMinSize(100., 57.);
			obj.setMaxWidth(Double.MAX_VALUE);
		}
		btn[0].getStyleClass().add("btn-raised-1");
		btn[0].setOnAction(e->notifyEvent(e_unlock_master));
		btn[1].getStyleClass().add("btn-raised-1");
		btn[1].setOnAction(e->notifyEvent(e_lock_master));		
		
		final VBox lay_tgl = new VBox(tgl);
		lay_tgl.getStyleClass().add("box-pad");
		final VBox lay_btn = new VBox(
			new Separator(),
			btn[0],btn[1]
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
		lay_gagu.addColumn(3, 
			sgm[0].get_TOR_Tile(), 
			sgm[1].get_TOR_Tile(), 
			sgm[2].get_TOR_Tile()
		);
		
		final HBox lay_test = new HBox(
			coup.gen_console(),
			ibus.gen_console()
		);
		lay_test.getStyleClass().add("box-pad");
		
		final JFXTabPane lay_tabs = new JFXTabPane();
		lay_tabs.getTabs().addAll(
			new Tab("儀表",lay_gagu),
			//new Tab("監控",hist),
			new Tab("TEST-1",lay_test)			
		);
		lay_tabs.getSelectionModel().select(1);//select TABs
		
		return new BorderPane(
			lay_tabs,
			null,lay_ctrl,
			null,null				
		);
	}


	void init_gauge() {
		gag[0] = TileBuilder.create()
			.skinType(SkinType.COUNTDOWN_TIMER)
			.title("計時")
			.textSize(TextSize.BIGGER)
			.timePeriod(java.time.Duration.ofSeconds(-1L))
			.onAlarm(e->{
				notifyEvent(e_halting_alarm);
			})
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
			.unit("us/cm")
			.maxValue(7)			
			.build();
		gag[1].setDecimals(3);
		gag[1].valueProperty().bind(ibus.PV_COND);
		
		gag[2] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("流量")
			.textSize(TextSize.BIGGER)
			.unit("Volt")
			.maxValue(10)			
			.build();
		gag[2].setDecimals(2);
		gag[2].valueProperty().bind(coup.FLUX_VAL.divide(1000.f));
		
		gag[3] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("溫度")
			.textSize(TextSize.BIGGER)
			.unit("C")
			.maxValue(70)			
			.build();
		gag[3].setDecimals(1);
		gag[3].valueProperty().bind(
			ibus.PV_FA231.divide(10.)
		);
		gag[3].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('f',"Degree");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			ibus.set_FA231(Float.valueOf(opt.get()));
		});
		
		gag[4] = TileBuilder.create()
			.skinType(SkinType.GAUGE2)
			.title(TXT_CYLI_UP)
			.textSize(TextSize.BIGGER)
			.unit("Volt")
			.maxValue(5)			
			.build();
		gag[4].setDecimals(3);
		gag[4].valueProperty().bind(
			coup.PRESS_UP.divide(1000.f)
		);
		gag[4].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('f',"Volt");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			coup.armPressUp(Float.valueOf(opt.get()));
		});
		
		gag[5] = TileBuilder.create()
			.skinType(SkinType.GAUGE2)
			.title(TXT_CYLI_DW)
			.textSize(TextSize.BIGGER)
			.unit("Volt")
			.maxValue(5)		
			.build();
		gag[5].setDecimals(3);
		gag[5].valueProperty().bind(
			coup.PRESS_DW.divide(1000.f)
		);
		gag[5].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('f',"Volt");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			coup.armPressDw(Float.valueOf(opt.get()));
		});
	}
}
