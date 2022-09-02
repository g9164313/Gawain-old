package prj.sputter;

import java.util.Optional;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.layout.StackPane;
import narl.itrc.Misc;
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
			.maxValue(1.)						
			.unit(" "+unit)
			//.decimals((int)range[i][3])
			.build();
		prop.addListener((obv,oldVal,newVal)->{
			float val = newVal.floatValue();
			Misc.logv("%s: %.2E --> %.2E ", obv.toString(), oldVal.floatValue(), newVal.floatValue());			
			String pfx= "";
			int dec = 0;
			double min=0., max=1.;
			if(val<1e-6f) {
				pfx="n"; val*=1e+9f; min=0.; max=1000.; dec=0;
			}else if(1e-6<=val && val<1e-3f) {
				pfx="μ"; val*=1e+6f; min=0.; max=1000.; dec=0;
			}else if(1e-3<=val && val<1) {
				pfx="m"; val*=1e+3f; min=0.; max=1000.; dec=0;
			}else if(1   <=val && val<1e+3f) {
				pfx=" "; val*=1f   ; min=0.; max=1000.; dec=1;
			}else if(1e+3<=val && val<1e+6f) {
				pfx="K"; val*=1e-3f; min=0.; max=1000.; dec=0;
			}else if(1e+6<=val && val<1e+9f) {
				pfx="M"; val*=1e-6f; min=0.; max=1000.; dec=0;
			}else if(1e+9<=val) {
				pfx="G"; val*=1e-9f; min=0.; max=1000.; dec=0;
			}			
			obj.setMinValue(min);
			obj.setMaxValue(max);
			obj.setValue(val);
			obj.setUnit(pfx+unit);
			obj.setDecimals(dec);
		});
		return obj; 
	}
}
