package prj.LPS_8S;

import java.time.Duration;
import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXToggleButton;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TextSize;
import eu.hansolo.tilesfx.events.AlarmEvent;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PadTouch;
import narl.itrc.PanBase;

public class PanMain extends PanBase {
	
	private static final String TXT_AXIS_MAIN = "主軸";
	private static final String TXT_AXIS_PRESS = "加壓軸";
	private static final String TXT_AXIS_SWING = "擺動軸";
	private static final String TXT_CYLI_1 = "氣壓缸（進）";
	private static final String TXT_CYLI_2 = "氣壓缸（出）";
	
	private ModInfoBus infobus = new ModInfoBus();
	private ModCoupler coupler = new ModCoupler();
	
	public PanMain() {
		init_gauge();
		stage().setOnShown(e->on_shown());
	}
	private void on_shown() {		
		String arg;
		arg = Gawain.prop().getProperty("INFOBUS", "");
		if(arg.length()!=0) {
			//TODO:binding data~~~~~
			//gag[6].titleProperty().set("ggyy");
			infobus.open(arg);	
		}
		arg = Gawain.prop().getProperty("COUPLER", "");
		if(arg.length()!=0) {
			coupler.open(arg);
		}
	}

	//計時, 
	//（研磨液）電導, 流量, 溫度,
	//主軸轉速-1, 加壓轉速-2, 推動轉速-3,
	//（加壓氣缸）電控比例閥-1, 電控比例閥-2,
	private final Tile[] gag = new Tile[9];
	
	private void on_timer_alarm(final AlarmEvent EVENT) {
		gag[0].setTitle("計時");//restore title~~~
	}
	
	private void on_working() {
		if(gag[0].getTimePeriod().getSeconds()>0L) {
			gag[0].setRunning(true);
		}
	}
	
	private void on_stopping() {
		gag[0].setRunning(false);
	}
	
	JFXToggleButton tglPumpSlurry;

	@Override
	public Pane eventLayout(PanBase self) {
		
		JFXToggleButton[] tgl = {
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton(),
			new JFXToggleButton(),
		};
		tgl[0].setText("氣壓缸");
		tgl[1].setText("研磨液");
		tgl[2].setText("主軸");
		tgl[3].setText("加壓軸");
		tgl[4].setText("推動軸");
		tglPumpSlurry = tgl[0];
		
		JFXButton[] btn = {
			new	JFXButton("裝模"),			
			new	JFXButton("拆模"),
			new	JFXButton("加工"),
			new	JFXButton("停止"),			
		};
		for(JFXButton obj:btn) {
			obj.setMinSize(100., 57.);
			obj.setMaxWidth(Double.MAX_VALUE);
		}
		btn[0].getStyleClass().add("btn-raised-2");
		//btn[0].setOnAction(e->on_working());
		btn[1].getStyleClass().add("btn-raised-2");
		btn[1].setOnAction(e->on_working());
		btn[2].getStyleClass().add("btn-raised-2");
		btn[2].setOnAction(e->on_stopping());
		btn[3].getStyleClass().add("btn-raised-0");
		//btn[3].setOnAction(e->on_stopping());
		
		final VBox lay_tgl = new VBox(tgl);
		lay_tgl.getStyleClass().add("box-pad");
		final VBox lay_btn = new VBox(
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
		lay_gagu.addColumn(0, gag[1], gag[2], gag[3]);
		lay_gagu.addColumn(1, gag[4], gag[5], gag[6]);
		lay_gagu.addColumn(2, gag[0], gag[7], gag[8]);
		
		return new BorderPane(
			lay_gagu,
			null,lay_ctrl,
			null,null				
		);
	}
 
	void init_gauge() {
		gag[0] = TileBuilder.create()
			.skinType(SkinType.COUNTDOWN_TIMER)
			.title("計時")
			.textSize(TextSize.BIGGER)
			.timePeriod(Duration.ofSeconds(-1L))
			.onAlarm(e->on_timer_alarm(e))
			.build();
		gag[0].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('T',"時：分：秒");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			String txt = opt.get();
			long val = PadTouch.toSecondValue(txt);
			gag[0].setTitle("設定 "+txt);
			gag[0].setTimePeriod(Duration.ofSeconds(val));
		});
		gag[1] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("電導")
			.textSize(TextSize.BIGGER)
			.unit("PH")
			.maxValue(1000)			
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
			.skinType(SkinType.GAUGE)
			.title("主軸轉速")
			.textSize(TextSize.BIGGER)
			.unit("RPM")
			.maxValue(3000)	
			.decimals(0)
			.build();
		gag[4].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('N',"RPM");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			int rpm = Integer.valueOf(opt.get());
			//infobus.SDA_speed(rpm);
		});
		gag[5] = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.title("加壓軸轉速")
			.textSize(TextSize.BIGGER)
			.unit("RPM")
			.maxValue(3000)
			.decimals(0)
			.build();
		gag[5].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('N',"RPM");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			int rpm = Integer.valueOf(opt.get());
			//infobus.SDA_speed(rpm);
		});
		gag[6] = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.title("推動軸轉速")
			.textSize(TextSize.BIGGER)
			.unit("RPM")
			.maxValue(3000)	
			.decimals(0)
			.build();		
		gag[6].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('N',"RPM");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			int rpm = Integer.valueOf(opt.get());
			//servo.SDA_speed(rpm);
		});
		gag[7] = TileBuilder.create()
			.skinType(SkinType.GAUGE2)
			.title("加壓（進）")
			.textSize(TextSize.BIGGER)
			.unit("Torr")
			.maxValue(100)			
			.build();
		gag[8] = TileBuilder.create()
			.skinType(SkinType.GAUGE2)
			.title("加壓（出）")
			.textSize(TextSize.BIGGER)
			.unit("Torr")
			.maxValue(100)			
			.build();
	}
}
