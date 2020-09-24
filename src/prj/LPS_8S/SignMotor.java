package prj.LPS_8S;

import java.util.Optional;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TextSize;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import narl.itrc.PadTouch;

public class SignMotor extends StackPane {

	private final Tile rpm,alm;
	private final Tile tor;

	private final BooleanProperty[] flg = {
		new SimpleBooleanProperty(true),
		new SimpleBooleanProperty(true),
	};
	
	public final BooleanProperty showRPM = flg[0];
	
	public SignMotor(final String name) {
		
		rpm = TileBuilder.create()
			.skinType(SkinType.GAUGE)
			.title(name)
			.textSize(TextSize.BIGGER)
			.unit("RPM")
			.maxValue(3000)	
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
	
	public void attach(final ModInfoBus bus,final int ID) {
		
		rpm.setOnMouseClicked(e->{
			PadTouch pad = new PadTouch('N',"RPM");
			Optional<String> opt = pad.showAndWait();			
			if(opt.isPresent()==false) {
				return;
			}
		});
		
		rpm.valueProperty().bind(bus.getSpeed(ID));
		tor.valueProperty().bind(bus.getTorr(ID));
		
		flg[1].bind(bus.getAlarm(ID).isEqualTo(0));
		alm.textProperty().bind(bus.getAlarmText(ID));
	}
}
