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

	private ModServo servo = new ModServo();
	
	public PanMain() {
		init_gauge();
		stage().setOnShown(e->on_shown());
	}
	private void on_shown() {
		String arg;
		arg = Gawain.prop().getProperty("SERVO", "");
		if(arg.length()!=0) {
			servo.open(arg);		
		}
	}

	private final XMark[] mrk = {
		new XMark("待機中","運轉中"),
		new XMark("待機中","運轉中"),
		new XMark("待機中","運轉中"),
		new XMark("夾模","拆模"),
		new XMark("止水","噴水"),
		new XMark("失壓","加壓中"),
		new XMark("失壓","加壓中"),
	};
	
	//計時, 電導度, 研磨液流量, 溫度, 
	//轉速-1, 轉速-2, 轉速-3,
	private final Tile[] gag = new Tile[7];
	
	//比例閥-1, 比例閥-2,
	//轉速-1, 轉速-2, 轉速-3,
	/*private final XSlider[] bar = {
		new XSlider("比例閥-1"),
		new XSlider("比例閥-2"),
		new XSlider("轉速-1"),
		new XSlider("轉速-2"),
		new XSlider("轉速-3"),
	};*/
	
	private void on_counting_done(final AlarmEvent EVENT) {
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
			new JFXToggleButton()
		};
		tgl[0].setText("噴水");
		tgl[1].setText("拆模");
		tgl[2].setText("馬達-1");
		tgl[2].setOnAction(e->{
			JFXToggleButton obj = (JFXToggleButton)e.getSource();
			boolean flg = obj.isSelected();
			Misc.logv("moto start="+flg);
			servo.SDA_motor(flg);
		});
		tgl[3].setText("馬達-2");
		tgl[4].setText("馬達-3");
		tglPumpSlurry = tgl[0];
		
		JFXButton[] btn = {
			new	JFXButton("加工"),
			new	JFXButton("停止"),
		};
		for(JFXButton obj:btn) {
			obj.setMinSize(100., 57.);
			obj.setMaxWidth(Double.MAX_VALUE);
		}
		btn[0].getStyleClass().add("btn-raised-2");
		btn[0].setOnAction(e->on_working());
		btn[1].getStyleClass().add("btn-raised-0");
		btn[1].setOnAction(e->on_stopping());
				
		final VBox lay_info = new VBox(
			new Label("馬達"),
			mrk[0], mrk[1], mrk[2],
			new Separator(),
			new Label("燈號"),
			mrk[3], mrk[4], mrk[5], mrk[6],
			new Separator()
		);
		lay_info.getStyleClass().add("box-pad");
		
		/*final GridPane lay_bars = new GridPane();
		//lay2.setGridLinesVisible(true);
		lay_bars.getStyleClass().addAll("box-pad");		
		lay_bars.addRow(0,bar[2],bar[3],bar[4]);
		lay_bars.addRow(1,bar[0],bar[1]);
		for(XSlider obj:bar) {
			GridPane.setHgrow(obj,Priority.ALWAYS);
			GridPane.setVgrow(obj,Priority.ALWAYS);
		}*/
		
		final VBox lay_tgl = new VBox(tgl);
		lay_tgl.getStyleClass().add("box-pad");
		
		final VBox lay_btn = new VBox(btn);
		lay_btn.getStyleClass().add("box-pad");
		
		final BorderPane lay_ctrl = new BorderPane(
			lay_tgl,
			null,null,
			lay_btn, null
		);
								
		final GridPane lay_gagu = new GridPane();
		lay_gagu.getStyleClass().add("box-pad");
		lay_gagu.addColumn(0, gag[4], gag[5], gag[6]);
		lay_gagu.addColumn(1, gag[1], gag[2], gag[3]);
		
		return new BorderPane(
			null,
			null,lay_ctrl,
			null,lay_gagu				
		);
	}

	private 
	
	void init_gauge() {
		gag[0] = TileBuilder.create()
			.skinType(SkinType.COUNTDOWN_TIMER)
			.title("計時")
			.textSize(TextSize.BIGGER)
			.timePeriod(Duration.ofSeconds(-1L))
			.onAlarm(e->on_counting_done(e))
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
			.title("電導度")
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
			.skinType(SkinType.SPARK_LINE)
			.title("馬達-1")
			.textSize(TextSize.BIGGER)
			.unit("RPM")
			.maxValue(3000)			
			.build();
		gag[4].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('N',"RPM");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			int rpm = Integer.valueOf(opt.get());
			servo.SDA_speed(rpm);
		});
		gag[5] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("馬達-2")
			.textSize(TextSize.BIGGER)
			.unit("RPM")
			.maxValue(3000)			
			.build();
		gag[5].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('N',"RPM");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			int rpm = Integer.valueOf(opt.get());
			servo.SDA_speed(rpm);
		});
		gag[6] = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.title("馬達-3")
			.textSize(TextSize.BIGGER)
			.unit("RPM")
			.maxValue(3000)			
			.build();
		gag[6].setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('N',"RPM");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
			int rpm = Integer.valueOf(opt.get());
			servo.SDA_speed(rpm);
		});
	}
}
