package prj.LPS_8S;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXToggleButton;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TextSize;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PadTouch;
import narl.itrc.PanBase;

public class PanMain extends PanBase {
	
	private static final String TXT_AXIS_MAJOR = "主軸";
	private static final String TXT_AXIS_PRESS = "加壓軸";
	private static final String TXT_AXIS_SWING = "擺動軸";
	private static final String TXT_CYLI_FORCE = "氣缸加壓";
	private static final String TXT_CYLI_DELTA = "氣缸壓差";
	
	private ModCoupler coup = new ModCoupler();
	private ModInsider ibus = new ModInsider(coup);	

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
	
	public PanMain(final Stage stg) {
		super(stg);
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
			coup.emerged_press  = ()->toggle_btn(coup.tglDoneAlarm,true);
			coup.emerged_release= ()->toggle_btn(coup.tglDoneAlarm,false);
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
	
	//private static final String TXT_LOCK = "主軸鎖定";
	//private static final String TXT_UNLOCK = "主軸解鎖";
	//private static final ImageView ICON_LOCK = Misc.getIconView("lock-outline.png");
	//private static final ImageView ICON_UNLOCK = Misc.getIconView("lock-open-outline.png");
	//private static final ImageView ICON_SETTINGS = Misc.getIconView("settings.png");
	
	private Tile tileAlarm;
	
	private JFXToggleButton tglMotorMajor = new JFXToggleButton();
	private JFXToggleButton tglMotorOther = new JFXToggleButton();
	//private JFXButton btnMajorLock;
	
	//計時, 抬升，汽缸壓, 電導, 流量, 溫度,
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
			toggle_btn(coup.tglDoneAlarm,false);
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
			toggle_btn(coup.tglDoneAlarm,true);
		},
	};
	
	@Override
	public Pane eventLayout(PanBase self) {		
		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(gen_panel_gauge());
		lay0.setRight(new HBox(
			new VBox(
				gen_arm_setting(),
				gen_motorB_setting("加壓軸",ModInsider.ID_PRESS),
				gen_motorB_setting("擺動軸",ModInsider.ID_SWING)
			),
			gen_panel_ctrl()
		));
		return lay0;
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
		lay.add(new Label("馬達轉速設定"), 0, 0, 2, 1);
		lay.addRow(1, new Label("主軸:"),txt[0]);
		lay.addRow(2, new Label("加壓:"),txt[1]);
		lay.addRow(3, new Label("擺動:"),txt[2]);
		return lay;
	}
		
	private Pane gen_motorB_setting(
		final String title,
		final int dev_id
	) {
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
		
		final JFXButton btn = new JFXButton("運轉");
		btn.getStyleClass().add("btn-raised-1");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setMinHeight(72.);	
		btn.setOnMousePressed (e->coup.servoMove(dev_id, true));
		btn.setOnMouseReleased(e->coup.servoMove(dev_id, false));

		final GridPane lay = new GridPane();		
		lay.getStyleClass().addAll("box-pad","box-border","font-size7");
		lay.disableProperty().bind(tglMotorOther.selectedProperty());
		lay.add(new Label(title+"寸進"), 0, 0, 2, 1);
		lay.add(rad[0], 0, 1, 2, 1);
		lay.add(rad[1], 0, 2, 2, 1);
		lay.add(btn   , 0, 3, 2, 1);
		return lay;
	}
	
	private Pane gen_arm_setting() {
		
		final ToggleGroup grp = new ToggleGroup();
		final JFXRadioButton[] rad = {
			new JFXRadioButton("臂長-短"),
			new JFXRadioButton("臂長-中"),
			new JFXRadioButton("臂長-長"),
		};
		rad[0].setOnAction(e->{
			coup.ArmUpSP2  = 0.3f; 
			coup.ArmUpSP3  = 0.1f;
			coup.ArmDwSP2_1= 0.9f;
			coup.ArmDwSP2_2= 1.1f;
		});
		rad[1].setOnAction(e->{
			coup.ArmUpSP2  = 0.9f; 
			coup.ArmUpSP3  = 0.1f;
			coup.ArmDwSP2_1= 1.7f;
			coup.ArmDwSP2_2= 2.2f;
		});
		rad[2].setOnAction(e->{
			coup.ArmUpSP2  = 1.3f; 
			coup.ArmUpSP3  = 0.1f;
			coup.ArmDwSP2_1= 1.9f;
			coup.ArmDwSP2_2= 2.5f;
		});		
		rad[2].setSelected(true);
		for(JFXRadioButton obj:rad) {
			obj.setToggleGroup(grp);
			if(obj.isSelected()==true) {
				obj.getOnAction().handle(null);
			}
		}
		
		final Label txt_dw = new Label();
		txt_dw.textProperty().bind(coup.armForceDw.asString("下壓 %4.1f V"));
		txt_dw.setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('f',"下壓閥門電壓");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			float val = Float.valueOf(opt.get());
			coup.armForceDw.set(val);
		});
		final Label txt_up = new Label();
		txt_up.textProperty().bind(coup.armForceUp.asString("上推 %4.1f V"));
		txt_up.setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('f',"上推閥門電壓");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			float val = Float.valueOf(opt.get());
			coup.armForceUp.set(val);
		});
		
		final JFXButton btn_valve_in = new JFXButton("加壓");
		btn_valve_in.getStyleClass().add("btn-raised-1");
		btn_valve_in.setMaxWidth(Double.MAX_VALUE);
		btn_valve_in.setMinHeight(72.);	
		btn_valve_in.setOnAction(e->coup.armPression());
		
		final JFXButton btn_valve_out= new JFXButton("洩壓");
		btn_valve_out.getStyleClass().add("btn-raised-0");
		btn_valve_out.setMaxWidth(Double.MAX_VALUE);
		btn_valve_out.setMinHeight(72.);
		btn_valve_out.setOnAction(e->coup.armPression(0f, 0f));
		
		final GridPane lay = new GridPane();		
		lay.getStyleClass().addAll("box-pad","box-border","font-size7");
		lay.add(new Label("手臂設定"), 0, 0, 2, 1);
		lay.disableProperty().bind(tglMotorMajor.selectedProperty().or(tglMotorOther.selectedProperty()));
		lay.add(rad[0], 0, 1, 2, 1);
		lay.add(rad[1], 0, 2, 2, 1);
		lay.add(rad[2], 0, 3, 2, 1);
		lay.add(txt_dw, 0, 4, 2, 1);
		lay.add(txt_up, 0, 5, 2, 1);
		lay.add(btn_valve_in , 0, 6, 2, 1);
		lay.add(btn_valve_out, 0, 7, 2, 1);
		return lay;
	}
	
	private Pane gen_start_work_setting() {		
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
		
		final VBox lay = new VBox(new Label("加工步驟設定"));		
		lay.getStyleClass().addAll("box-pad","box-border","font-size7");
		lay.getChildren().addAll(chk[1],chk[2],chk[3]);
		return lay; 
	}
		
	private Pane gen_panel_ctrl() {
		
		final JFXToggleButton tgl_alarm  = new JFXToggleButton();
		final JFXToggleButton tgl_heater = new JFXToggleButton();
		final JFXToggleButton tgl_pumper = new JFXToggleButton();

		
		tgl_alarm .setText("警報");
		tgl_heater.setText("加熱器");
		tgl_pumper.setText("研磨液");
		tglMotorMajor.setText("旋轉軸");
		tglMotorOther.setText("加工軸");
		
		coup.tglDoneAlarm = tgl_alarm;
		tgl_alarm.setOnAction(e->coup.giveAlarm());

		coup.tglSlurryHeat = tgl_heater;
		tgl_heater.setOnAction(e->coup.heatSlurry());
		
		coup.tglSlurryPump = tgl_pumper;
		tgl_pumper.setOnAction(e->coup.pumpSlurry());
		
		tglMotorMajor.disableProperty().bind(coup.majorUnlock.not());
		tglMotorMajor.setOnAction(e->{
			if(tglMotorMajor.isDisable()==true) { 
				return; 
			}
			ibus.majorKickoff(tglMotorMajor);
		});

		tglMotorOther.setOnAction(e->{
			if(tglMotorOther.isDisable()==true) { 
				return; 
			}
			coup.kickoff_other(tglMotorOther);
		});
		
		final JFXButton btn_lock = new JFXButton();
		btn_lock.getStyleClass().add("btn-raised-0");
		btn_lock.setMaxWidth(Double.MAX_VALUE);
		btn_lock.setMinHeight(72.);		
		btn_lock.disableProperty().bind(coup.majorUnlock.not());
		btn_lock.setText("Lock");
		btn_lock.setOnAction(e->ibus.lockMajorMotor());
		
		final JFXButton btn_unlock = new JFXButton();
		btn_unlock.getStyleClass().add("btn-raised-1");
		btn_unlock.setMaxWidth(Double.MAX_VALUE);
		btn_unlock.setMinHeight(72.);		
		btn_unlock.disableProperty().bind(coup.majorUnlock);
		btn_unlock.setText("Unlock");
		btn_unlock.setOnAction(e->coup.LockMasterMotor(false));
		
		final VBox lay1 = new VBox(
			tgl_alarm,
			tgl_heater,
			tgl_pumper,
			tglMotorOther,
			tglMotorMajor
		);
		AnchorPane.setTopAnchor  (lay1, 7.0);
		AnchorPane.setLeftAnchor (lay1, 7.0);
		AnchorPane.setRightAnchor(lay1, 7.0);
		
		final VBox lay2 = new VBox(btn_lock,btn_unlock); 
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
			.title(TXT_CYLI_DELTA)			
			.unit("kgf/cm2")
			.maxValue(5)
			.build();
		tile[4].setDecimals(2);
		tile[4].valueProperty().bind(coup.ARM_PRESS_UP.multiply(10.2f));
		tile[4].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('i',"%");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			int val = Integer.valueOf(opt.get());
			if(val>=100) {
				val = 100;
			}
		});
		
		tile[5] = TileBuilder.create()
			.skinType(SkinType.SLIDER)
			.title(TXT_CYLI_FORCE)
			.minValue(1)
			.value(1.)
			.maxValue(9)
			.textSize(TextSize.BIGGER)
			.build();
		tile[5].setDecimals(0);
		tile[5].valueProperty().addListener((obv,oldVal,newVal)->{
			int val = tile[5].valueProperty().getValue().intValue();
			switch(val) {
			case 1: coup.armPressProp(1.0f,0.7f);break;//0.3 
			case 2: coup.armPressProp(2.0f,1.4f);break;//0.5 
			case 3: coup.armPressProp(3.0f,2.3f);break;//0.7
			case 4: coup.armPressProp(4.0f,3.1f);break;//0.9 
			case 5: coup.armPressProp(5.0f,3.9f);break;//1.1
			case 6: coup.armPressProp(6.0f,4.7f);break;//1.3 
			case 7: coup.armPressProp(7.0f,5.5f);break;//1.5
			case 8: coup.armPressProp(8.0f,7.3f);break;//1.7 
			case 9: coup.armPressProp(9.0f,7.0f);break;//1.9
			}
		});

		final GridPane lay = new GridPane();
		lay.getStyleClass().addAll("box-pad");
		lay.addColumn(0,tile[0],tile[5],tile[4]);
		lay.addColumn(1,moto[0],moto[1],moto[2]);
		lay.addColumn(2,tile[1],tile[2],tile[3]);		
		return lay;
	}
}
