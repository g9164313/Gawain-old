package prj.sputter;

import java.util.Optional;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.layout.StackPane;
import narl.itrc.PadTouch;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TextSize;

public class LayTool {

	public interface Translate{
		float func(final float src);
	};
	
	public static ReadOnlyFloatProperty transform(
		final FloatProperty observe,		
		final Translate hook
	) {
		final SimpleFloatProperty prop = new SimpleFloatProperty();
		if(observe==null) {
			return prop;
		}
		observe.addListener((obv,oldVal,newVal)->{
			prop.set(hook.func(newVal.floatValue()));
		});		
		return prop;
	}
	
	public static Tile create_MFC_gauge(
		final String name, 
		final String unit,
		final double max_flow,		
		final ReadOnlyFloatProperty prop,
		final Translate hook
	) {
		final Tile obj = TileBuilder.create()
			.skinType(SkinType.BAR_GAUGE)
			.textSize(TextSize.BIGGER)
			.title(name)
			.autoScale(false)
			.minValue(0.)
			.maxValue(max_flow)			
			.unit(unit)
			.decimals(1)
			.build();
		obj.valueProperty().bind(prop);
		obj.setOnMouseClicked(event->{
			PadTouch pad = new PadTouch('f',name,"0");
			Optional<String> res = pad.showAndWait();
			if(res.isPresent()==true) {
				if(hook!=null) {
					hook.func(Float.parseFloat(res.get()));
				}
			}
		});
		return obj;
	}
	
	public static StackPane create_prefix_gauge(
		final String name, 
		final String unit,
		final ReadOnlyFloatProperty prop
	) {
		final float[][] range = {
			{1e-12f,1e-9f , 1e+9f, 2f },//n
			{1e-9f, 1e-6f , 1e+6f, 2f },//μ
			{1e-6f, 1e-3f , 1e+3f, 2f },//m
			{1e-3f, 1f    , 1f,    3f },
			{1f   , 1e+3f , 1f,    0f },
			{1e+3f, 1e+6f , 1e-3f, 0f },//k
			{1e+6f, 1e+9f , 1e-6f, 0f },//M
			{1e+9f, 1e+12f, 1e-9f, 0f },//G
		};
		final String[] pref = {
			"n", "μ", "m", 
			"", "", 
			"k", "M", "G"
		};

		final Tile[] lst = new Tile[range.length];
		for(int i=0; i<lst.length; i++) {
			final Tile obj = TileBuilder.create()
				.skinType(SkinType.SPARK_LINE)
				.textSize(TextSize.BIGGER)
				.title(name)
				.autoScale(false)
				.minValue(range[i][0] * range[i][2])
				.maxValue(range[i][1] * range[i][2])						
				.unit(pref[i]+unit)
				.decimals((int)range[i][3])
				.build();
			obj.valueProperty().bind(prop.multiply(range[i][2]));
			lst[i] = obj;
		}
		
		lst[0].visibleProperty().bind(prop.lessThanOrEqualTo(range[0][1]));
		lst[1].visibleProperty().bind(prop.greaterThan(range[1][0]).and(prop.lessThanOrEqualTo(range[1][1])));
		lst[2].visibleProperty().bind(prop.greaterThan(range[2][0]).and(prop.lessThanOrEqualTo(range[2][1])));
		lst[3].visibleProperty().bind(prop.greaterThan(range[3][0]).and(prop.lessThan(range[3][1])));
		
		lst[4].visibleProperty().bind(prop.greaterThanOrEqualTo(range[4][0]).and(prop.lessThan(range[4][1])));
		lst[5].visibleProperty().bind(prop.greaterThanOrEqualTo(range[5][0]).and(prop.lessThan(range[5][1])));
		lst[6].visibleProperty().bind(prop.greaterThanOrEqualTo(range[6][0]).and(prop.lessThan(range[6][1])));
		lst[7].visibleProperty().bind(prop.greaterThanOrEqualTo(range[7][0]));
		
		return new StackPane(lst);
	}
	
}
