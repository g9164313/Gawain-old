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

public class LayHelper {

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
	
	public static class MetrixPrefix {
		final float value;
		final String prefix;		
		public MetrixPrefix(final float val){
			if(clip_inc(null,val,1e-6f)) {
				value = val * 1e+9f; prefix="n"; 
			}else if(clip_inc(1e-6f,val,1e-3f)) {
				value = val * 1e+6f; prefix="μ"; 
			}else if(clip_inc(1e-3f,val,1e+0f)) {
				value = val * 1e+3f; prefix="m"; 
			}else if(clip_inc(1e+0f,val,1e+3f)) {
				value = val * 1e+0f; prefix=" "; 
			}else if(clip_inc(1e+3f,val,1e+6f)) {
				value = val * 1e-3f; prefix="K"; 
			}else if(clip_inc(1e+6f,val,1e+9f)) {
				value = val * 1e-6f; prefix="M"; 
			}else if(clip_inc(1e+9f,val,null)) {
				value = val * 1e-9f; prefix="G"; 
			}else {
				value = val; prefix= "";
			}
		}
		
		private boolean clip_inc(
			final Float lower, 
			float value, 
			final Float upper
		) {
			value = Math.abs(value);
			if(upper!=null) {
				if(upper<=value) {
					return false;
				}
			}
			if(lower!=null) {
				if(value<lower) {
					return false;
				}
			}
			return true;
		}		
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
			final float val = newVal.floatValue();
			final MetrixPrefix mp = new MetrixPrefix(val);
			obj.setValue(mp.value);
			obj.setUnit(mp.prefix+unit);
		});
		return obj; 
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
}
