package prj.LPS_8S;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXToggleButton;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TextSize;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
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
	private static final String TXT_CYLI_FORCE = "氣壓缸";
	
	private ModInsider ibus = new ModInsider();
	private ModCoupler coup = new ModCoupler();
	//private LogHistory hist = new LogHistory(ibus,coup);
	
	private SignMotor[] moto = {
		new SignMotor(TXT_AXIS_MAJOR).binding(
			ibus.MAJOR_RPM.negate().divide(40), 
			ibus.MAJOR_TOR.negate().multiply(1), 
			ibus.MAJOR_ALM.multiply(1)
		).attach(ibus, ModInsider.ID_MAJOR),
		new SignMotor(TXT_AXIS_PRESS).binding(
			ibus.PRESS_RPM.divide(40), 
			ibus.PRESS_TOR.multiply(1), 
			ibus.PRESS_ALM.multiply(1)
		).attach(ibus, ModInsider.ID_PRESS),
		new SignMotor(TXT_AXIS_SWING,"CPM").binding(
			ibus.SWING_RPM.divide(40), 
			ibus.SWING_TOR.multiply(1), 
			ibus.SWING_ALM.multiply(1)
		).attach(ibus, ModInsider.ID_SWING),
	};
	
	public PanMain() {
		moto[0].SV_RPM = ibus.MAJOR_RPM_SV;
		moto[1].SV_RPM = ibus.PRESS_RPM_SV;
		moto[2].SV_RPM = ibus.SWING_RPM_SV;
		stage().setOnShown(e->on_shown());
	}
	private void on_shown() {
		coup.ibus = ibus;
		String arg;
		arg = Gawain.prop().getProperty("INSIDER", "");
		if(arg.length()!=0) {
			ibus.open(arg);
			ibus.playLoop();
		}		
		arg = Gawain.prop().getProperty("COUPLER", "");
		if(arg.length()!=0) {
			coup.working_press  = ()->notifyEvent(e_working);
			coup.working_release= ()->notifyEvent(e_halting);
			coup.emerged_press  = ()->toggle_btn(coup.tglAlarm,true);
			coup.emerged_release= ()->toggle_btn(coup.tglAlarm,false);
			coup.open(arg);
		}
	}

	private void toggle_btn(
		final ToggleButton tgl,
		final boolean val
	) {
		tgl.setSelected(val);
		tgl.getOnAction().handle(null);
	}
	
	private static final String TXT_LOCK = "主軸鎖定";
	private static final String TXT_UNLOCK = "主軸解鎖";
	private static final ImageView ICON_LOCK = Misc.getIconView("lock-outline.png");
	private static final ImageView ICON_UNLOCK = Misc.getIconView("lock-open-outline.png");
	private static final ImageView ICON_SETTINGS = Misc.getIconView("settings.png");
	
	private Tile tileAlarm;
	private JFXToggleButton tglMotorMajor;
	private JFXToggleButton tglMotorOther;
	//private JFXButton btnMajorLock;
	
	//計時, （研磨液）電導, 流量, 溫度,
	private final Tile[] tile = new Tile[6];
	
	private BooleanProperty startSlurryHeat = new SimpleBooleanProperty(true);
	private BooleanProperty startSlurryPump = new SimpleBooleanProperty(true);
	private BooleanProperty startMotorMajor = new SimpleBooleanProperty(true);
	private BooleanProperty startMotorOther = new SimpleBooleanProperty(true);
	
	private NotifyEvent[] e_working = {
		/*(lad,dlg)->{
			dlg.setText("確認[拆模磁簧]");//1:沒卡，0:卡住
			//ladderJump(lad,3);
		},
		(lad,dlg)->{
			dlg.setText("確認[主軸近接]");
			//ladderJump(lad,3);
		},*/
		(lad,dlg)->{
			if(startSlurryPump.get()==false) { return; }
			dlg.setText("開始澆灌");
			toggle_btn(coup.tglSlurryPump,true);
		},
		(lad,dlg)->{
			if(startMotorMajor.get()==false) { return; }
			dlg.setText("主軸旋轉");
			toggle_btn(tglMotorMajor,true);
		},
		(lad,dlg)->{
			if(startMotorOther.get()==false) { return; }
			dlg.setText("加工旋轉");
			toggle_btn(tglMotorOther,true);
			if(tileAlarm.getTimePeriod().getSeconds()>0L) {
				tileAlarm.setRunning(true);
			}
		},
	};
	private final NotifyEvent[] e_halting = {
		(lad,dlg)->{
			if(startMotorMajor.get()==false) { return; }
			dlg.setText("停止主軸");
			toggle_btn(tglMotorMajor,false);
		},
		(lad,dlg)->{
			if(startMotorOther.get()==false) { return; }
			dlg.setText("停止加工");
			toggle_btn(tglMotorOther,false);
		},
		(lad,dlg)->{
			if(startSlurryPump.get()==false) { return; }
			dlg.setText("停止澆灌");
			toggle_btn(coup.tglSlurryPump,false);
		},
		(lad,dlg)->{
			toggle_btn(coup.tglAlarm,false);
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
			toggle_btn(coup.tglSlurryPump,false);
		},
		(lad,dlg)->{
			toggle_btn(coup.tglAlarm,true);
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
			if(coup.flgMasterLocate.get()==true) {
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
			if(coup.flgMasterLocate.get()==true) {
				return;
			}
			ladderJump(ldd,2);
		},
		(ldd,dlg)->{
			dlg.setText("推入卡榫");
		}
	};
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		//global toggle must be instanced first!!
		final Pane lay_ctrl = gen_panel_ctrl();
		
		final FlowPane lay_info = new FlowPane(Orientation.VERTICAL);
		lay_info.getStyleClass().addAll("box-pad");
		lay_info.getChildren().addAll(
			gen_speed_setting(),
			gen_stretch_option(),
			gen_locate_setting(TXT_AXIS_PRESS,ModInsider.ID_PRESS),
			gen_locate_setting(TXT_AXIS_SWING,ModInsider.ID_SWING),
			//gen_locate_setting(TXT_AXIS_MAJOR,ModInsider.ID_MAJOR)
			gen_starter_setting()
		);
		
		final HBox lay1 = new HBox(lay_info,lay_ctrl);
		lay1.getStyleClass().add("box-pad");
		
		final BorderPane lay = new BorderPane();
		lay.setCenter(gen_panel_gauge());
		lay.setRight(lay1);
		return lay;
	}
	
	private Pane gen_speed_setting() {		
		final Label[] txt = { 
			new Label(),
			new Label(),
			new Label(),
		};
		for(int i=0; i<txt.length; i++) {
			final SignMotor obj = moto[i];
			txt[i].textProperty().bind(obj.SV_RPM.asString("%2d RPM"));
			txt[i].setOnMouseClicked(e->obj.event_set_speed());
			txt[i].setMinWidth(87.);
			txt[i].setAlignment(Pos.BASELINE_RIGHT);
		}
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad","box-border");
		lay.add(new Label("轉速設定"), 0, 0, 2, 1);
		lay.addRow(1, new Label("主軸:"),txt[0]);
		lay.addRow(2, new Label("加壓:"),txt[1]);
		lay.addRow(3, new Label("擺動:"),txt[2]);
		return lay;
	}
	
	private Pane gen_stretch_option() {
		
		final ToggleGroup grp = new ToggleGroup();
		final JFXRadioButton[] rad = {
			new JFXRadioButton("高度-1"),
			new JFXRadioButton("高度-2"),
			new JFXRadioButton("高度-3"),
		};
		rad[0].setSelected(true);
		rad[0].setToggleGroup(grp);
		rad[1].setToggleGroup(grp);
		rad[2].setToggleGroup(grp);
		rad[0].setOnAction(e->coup.ArmDownDelay.set(500));
		rad[1].setOnAction(e->coup.ArmDownDelay.set(650));
		rad[2].setOnAction(e->coup.ArmDownDelay.set(750));
		
		final VBox lay = new VBox(new Label("汽缸設定"));
		lay.getChildren().addAll(rad);
		lay.getStyleClass().addAll("box-pad","box-border");
		return lay;
	}
	
	private Pane gen_locate_setting(
		final String title,
		final int dev_id
	) {
		final JFXButton btn = new JFXButton("");
		btn.setPrefSize(64., 64.);
		btn.getStyleClass().add("btn-raised-1");
		btn.setOnMousePressed (e->{
			if(dev_id==ModInsider.ID_MAJOR) {
				ibus.majorMove(true);
			}else {
				coup.servoMove(dev_id, true);
			}
		});
		btn.setOnMouseReleased(e->{
			if(dev_id==ModInsider.ID_MAJOR) {
				ibus.majorMove(false);
			}else {
				coup.servoMove(dev_id, false);
			}
		});

		final ToggleGroup grp = new ToggleGroup();
		final JFXRadioButton[] rad = {
			new JFXRadioButton("順時針"),
			new JFXRadioButton("逆時針"),
		};
		rad[0].setSelected(true);
		rad[0].setToggleGroup(grp);
		rad[1].setToggleGroup(grp);
		//rad[0].setOnAction(e->ibus.setLocatePulse(dev_id,-5000));
		//rad[1].setOnAction(e->ibus.setLocatePulse(dev_id, 5000));
		
		final GridPane lay = new GridPane();
		lay.disableProperty().bind(tglMotorOther.selectedProperty());
		lay.getStyleClass().addAll("box-pad","box-border");
		lay.add(new Label(title+"寸進"), 0, 0, 2, 1);
		lay.addColumn(0, rad);
		lay.add(btn, 1, 1, 2, 2);
		return lay;
	}
	
	private Pane gen_starter_setting() {		
		final JFXCheckBox[] chk = {
			new JFXCheckBox("加熱器"),
			new JFXCheckBox("研磨液"),
			new JFXCheckBox("旋轉軸"),
			new JFXCheckBox("加工軸")
		};
		startSlurryHeat.bind(chk[0].selectedProperty());
		startSlurryPump.bind(chk[1].selectedProperty());
		startMotorMajor.bind(chk[2].selectedProperty());
		startMotorOther.bind(chk[3].selectedProperty());
		chk[3].setSelected(true);
		
		final VBox lay = new VBox(new Label("啟動設定"));
		lay.getChildren().addAll(chk[1],chk[2],chk[3]);
		lay.getStyleClass().addAll("box-pad","box-border");
		return lay; 
	}
	
	private Pane gen_panel_ctrl() {
		
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
		
		coup.tglAlarm = tgl[0];
		tgl[0].setOnAction(e->coup.giveAlarm());

		coup.tglSlurryHeat = tgl[1];
		tgl[1].setOnAction(e->coup.heatSlurry());
		
		coup.tglSlurryPump = tgl[2];
		tgl[2].setOnAction(e->coup.pumpSlurry());
		
		tglMotorMajor = tgl[3];
		//tglMotorMajor.disableProperty().bind(coup.flgMasterUnLock.not());
		tglMotorMajor.setOnAction(e->{
			if(tglMotorMajor.isDisable()==true) { return; }
			ibus.majorKickoff(tgl[3]);
		});

		tglMotorOther = tgl[4];
		tgl[4].setOnAction(e->coup.kickoff_other(tgl[4]));
		
		final JFXButton btn2 = new JFXButton();
		btn2.setMaxWidth(Double.MAX_VALUE);
		btn2.setMinHeight(72.);
		btn2.getStyleClass().add("btn-raised-1");
		btn2.setText("test");
				
		final VBox lay1 = new VBox(tgl);
		AnchorPane.setTopAnchor  (lay1, 7.0);
		AnchorPane.setLeftAnchor (lay1, 7.0);
		AnchorPane.setRightAnchor(lay1, 7.0);
		
		final VBox lay2 = new VBox(btn2); 
		lay2.getStyleClass().add("box-pad");
		AnchorPane.setBottomAnchor(lay2, 7.0);
		AnchorPane.setLeftAnchor  (lay2, 7.0);
		AnchorPane.setRightAnchor (lay2, 7.0);
		return new AnchorPane(lay1,lay2);
	}
	
	private Pane gen_panel_gauge() {
		
		tile[0] = TileBuilder.create()
			.skinType(SkinType.COUNTDOWN_TIMER)
			.title("計時")
			.textSize(TextSize.BIGGER)
			.timePeriod(java.time.Duration.ofSeconds(-1L))
			.onAlarm(e->notifyEvent(e_halting_alarm))
			.build();
		tile[0].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('T',"時：分：秒");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			String txt = opt.get();
			long val = PadTouch.toSecondValue(txt);
			tile[0].setTitle("計時"+Misc.tick2text(val*1000,true,3));
			tile[0].setTimePeriod(java.time.Duration.ofSeconds(val));
		});
		tileAlarm = tile[0];
			
		tile[1] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.textSize(TextSize.BIGGER)
			.title("電導")			
			.unit("uS/cm")
			.maxValue(1)			
			.build();
		tile[1].setDecimals(3);
		tile[1].valueProperty().bind(ibus.PV_COND);
			
		tile[2] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.textSize(TextSize.BIGGER)
			.title("流量")
			.unit("L/min")
			.maxValue(10)			
			.build();
		tile[2].setDecimals(2);
		tile[2].valueProperty().bind(coup.FD_Q20C_AOUT.divide(1000f).multiply(6f));
			
		tile[3] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.textSize(TextSize.BIGGER)
			.title("溫度")
			.unit("度C")
			.maxValue(70)			
			.build();
		tile[3].setDecimals(1);
		tile[3].valueProperty().bind(
			ibus.PV_FA231.divide(10f)
		);
		tile[3].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('f',"度C");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			ibus.set_FA231(Float.valueOf(opt.get()));
		});
			
		tile[4] = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.textSize(TextSize.BIGGER)
			.title(TXT_CYLI_FORCE)			
			.unit("kgf/cm2")
			.maxValue(5)
			.build();
		tile[4].setDecimals(2);
		tile[4].valueProperty().bind(coup.ARM_PRESS_UP.multiply(10.2f));
		tile[4].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('f',"Volt");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			coup.armForce(Float.valueOf(opt.get()));
		});
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,tile[0],tile[4]);
		lay.addColumn(1,moto[0],moto[1],moto[2]);
		lay.addColumn(2,tile[1],tile[2],tile[3]);		
		return lay;
	}
}
