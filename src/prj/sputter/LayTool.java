package prj.sputter;

import java.util.Optional;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import narl.itrc.Misc;
import narl.itrc.PadTouch;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TextSize;

public class LayTool {

	public interface Translate{
		float func(final float src);
	};
	
	public static ReadOnlyFloatProperty transform(
		final ReadOnlyFloatProperty observe,		
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
	
	public static Tile create_prefix_gauge(
		final String name, 
		final String unit,
		final ReadOnlyFloatProperty prop
	) {
		final Tile obj = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.textSize(TextSize.BIGGER)
			.title(name)
			.autoScale(false)
			.minValue(0.)
			.maxValue(1000.)
			.decimals(2)					
			.unit(" "+unit)
			.build();
		prop.addListener((obv,oldVal,newVal)->{
			float val = newVal.floatValue();
			Misc.logv("%s: %.2E --> %.2E ", obv.toString(), oldVal.floatValue(), newVal.floatValue());			
			String pfx= "";
			if(val<1e-6f) {
				pfx="n"; val*=1e+9f;
			}else if(1e-6f<=val && val<1e-3f) {
				pfx="μ"; val*=1e+6f;
			}else if(1e-3f<=val && val<1e+0f) {
				pfx="m"; val*=1e+3f;
			}else if(1e+0f<=val && val<1e+3f) {
				pfx=" "; val*=1e+0f;
			}else if(1e+3f<=val && val<1e+6f) {
				pfx="K"; val*=1e-3f;
			}else if(1e+6f<=val && val<1e+9f) {
				pfx="M"; val*=1e-6f;
			}else if(1e+9f<=val) {
				pfx="G"; val*=1e-9f;
			}			
			obj.setValue(val);
			obj.setUnit(pfx+unit);
		});
		return obj; 
	}
}
