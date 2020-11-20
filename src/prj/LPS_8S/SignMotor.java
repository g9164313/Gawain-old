package prj.LPS_8S;

import java.util.Optional;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TextSize;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import narl.itrc.PadTouch;

public class SignMotor extends StackPane {

	private final Tile rpm,alm;
	private final Tile tor;

	private final String title;
	
	private final StringProperty txt_alm = new SimpleStringProperty("");
	
	private final BooleanProperty[] flg = {
		new SimpleBooleanProperty(true),
		new SimpleBooleanProperty(true),
	};
	
	public final BooleanProperty showRPM = flg[0];
	
	public IntegerProperty SV_RPM = null;
	
	public SignMotor(final String name) {
		this(name,"RPM");
	}
	
	public SignMotor(final String name,final String unit) {
		
		title = name;
		
		rpm = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.title(name)
			.textSize(TextSize.BIGGER)
			.unit(unit)
			.maxValue(40)	
			.decimals(0)
			.build();
		
		tor = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.title(name)
			.textSize(TextSize.BIGGER)
			.unit("％")
			.maxValue(100)	
			.decimals(0)
			.build();
		
		alm = TileBuilder.create()
			.skinType(SkinType.LED)
			.title(name+"異常")
			.titleColor(Color.RED)
			.text("ggyy2")
			.textColor(Color.RED)
			.textSize(TextSize.BIGGER)
			.build();
		alm.setActive(true);
		alm.setActiveColor(Color.RED);
		
		rpm.visibleProperty().bind(flg[0].and(flg[1]));
		tor.visibleProperty().bind(flg[0].not().and(flg[1]));
		alm.visibleProperty().bind(flg[1].not());

		getChildren().addAll(rpm,tor,alm);
		//getChildren().addAll(rpm,alm);
	}
	
	public SignMotor binding(
		final IntegerBinding v_rpm,
		final IntegerBinding v_tor,
		final IntegerBinding v_alm
	) {
		rpm.valueProperty().bind(v_rpm);
		tor.valueProperty().bind(v_tor);
		alm.textProperty().bind(txt_alm);
		v_alm.addListener((obv,oldVal,newVal)->alarm_text(newVal.intValue()));
		flg[1].bind(v_alm.isEqualTo(0).or(v_alm.isEqualTo(255)));
		return this;
	}

	private ModInsider bus = null;
	private int did = -1;
	
	public void event_set_speed() {
		if(bus==null || did<0) {
			return;
		}
		PadTouch pad = new PadTouch('N',title+"(RPM)");
		Optional<String> opt = pad.showAndWait();		
		if(opt.isPresent()==false) {
			return;
		}
		int val = Integer.valueOf(opt.get());
		if(val>=41) {
			return;
		}
		SV_RPM.set(val);
		bus.setSpeed(did,val);
	}
	
	public SignMotor attach(final ModInsider bus,final int did) {
		this.bus = bus;
		this.did = did;
		rpm.setOnMouseClicked(e->event_set_speed());
		return this;
	}
	
	private final void alarm_text(final int val) {
		switch(val) {
		case  1: txt_alm.set("過電壓"); break;
		case  2: txt_alm.set("低電壓"); break;
		case  3: txt_alm.set("過電流"); break;
		case  4: txt_alm.set("回生異常"); break;
		case  5: txt_alm.set("過負載"); break;
		case  6: txt_alm.set("過速度"); break;
		case  7: txt_alm.set("異常脈波控制命令"); break;
		case  8: txt_alm.set("位置控制誤差過大"); break;
		case  9: txt_alm.set("串列通訊異常"); break;
		case 10: txt_alm.set("串列通訊逾時"); break;
		case 11: txt_alm.set("位置檢出器異常 1"); break;
		case 12: txt_alm.set("位置檢出器異常 2"); break;
		case 13: txt_alm.set("風扇異常"); break;
		case 14: txt_alm.set("IGBT 過溫"); break;
		case 15: txt_alm.set("記憶體異常"); break;
		
		case 16: txt_alm.set("過負載 2"); break;
		case 17: txt_alm.set("馬達匹配異常");  break;
		case 18: txt_alm.set("緊急停止"); break;
		case 19: txt_alm.set("正反轉極限異常"); break;
		
		case 0x20: txt_alm.set("馬達碰撞錯誤"); break;
		case 0x21: txt_alm.set("馬達 UVW 斷線"); break;
		case 0x22: txt_alm.set("編碼器通訊異常"); break;
		case 0x24: txt_alm.set("馬達編碼器種類錯誤"); break;
		case 0x26: txt_alm.set("位置檢出器異常 3"); break;
		case 0x27: txt_alm.set("位置檢出器異常 4"); break;
		case 0x28: txt_alm.set("位置檢出器過熱"); break;
		case 0x29: txt_alm.set("位置檢出器溢位 5"); break;
		case 0x2A: txt_alm.set("絕對型編碼器異常 1"); break;
		case 0x2B: txt_alm.set("絕對型編碼器異常 2"); break;
		case 0x2E: txt_alm.set("控制迴路異常"); break;
		case 0x2F: txt_alm.set("回生能量異常"); break;
		
		case 0x30: txt_alm.set("脈波輸出檢出器頻率過高"); break;
		case 0x31: txt_alm.set("過電流 2"); break;
		case 0x32: txt_alm.set("控制迴路異常 2"); break;
		case 0x33: txt_alm.set("記憶體異常 2"); break;
		case 0x34: txt_alm.set("過負載 4"); break;
		}
	}	
}
