package prj.sputter;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import narl.itrc.Misc;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TextSize;

public class LayTool {

	public interface Func{
		float run(final float src);
	};
	
	public static Tile create_vaccum_gauge(
		final String name,
		final ReadOnlyFloatProperty prop,
		//final float v_minium,
		final float v_under,
		final float v_over,
		//final float v_maxium,
		final String unit, final Func func
	) {
		//final float min = func.run(v_minium);
		final float under = func.run(v_under);
		final float over = func.run(v_over);
		//final float max = func.run(v_maxium);

		final SimpleFloatProperty v_prop = new SimpleFloatProperty();
		final SimpleIntegerProperty p_deci = new SimpleIntegerProperty(1);
		final SimpleStringProperty f_name = new SimpleStringProperty("");//prefix+unit
				
		final Tile tile = TileBuilder.create()
			.skinType(SkinType.SPARK_LINE)
			.textSize(TextSize.BIGGER)
			.title(name)
			.autoScale(false)
			.minValue(under)
			.maxValue(over)
			.unit(unit)
			.decimals(3)
			.build();		
		tile.valueProperty().bind(v_prop);
		//tile.decimalsProperty().bind(p_deci);
		//tile.unitProperty().bind(f_name);
		
		prop.addListener((obv,oldVal,newVal)->{
			final float pure = func.run(newVal.floatValue());
			if(pure<under) {
				tile.setTitle(name+"(過低)");
			}else if(over<pure) {
				tile.setTitle(name+"(過高)");
			}else {
				tile.setTitle(name);
			}
			v_prop.set(pure);
			
			/*final Object[] arg = Misc.calculate_prefix(pure);
			final float  val = (float)arg[0];
			final String prx = (String)arg[1];
			if(prx.length()==0) {
				p_deci.set(3);
			}else {
				p_deci.set(1);
			}
			v_prop.set(val);
			f_name.set(prx+unit);*/
		});		
		return tile;
	}
}
