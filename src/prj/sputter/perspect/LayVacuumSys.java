package prj.sputter.perspect;

import java.util.Optional;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import narl.itrc.Misc;

public class LayVacuumSys extends GridPane {

	private static final String s_txt = 
		"-fx-padding: 27px";
	
	public LayVacuumSys LayoutBody() {
		
		setStyle(s_txt);
		
		ValveLift p1 = new ValveLift();
		ValveLift p2 = new ValveLift();
		ValveLift p3 = new ValveLift();
		
		Valve p4 = new Valve(PDir.HORI)
			.addInfo("PV", "000.00 psi")
			.addInfo("SV", "000.00 psi");
		
		Valve p5 = new Valve(PDir.VERT);
		
		Pipe p6 = new Pipe(PDir.TP_LF);
		
		Oblong b1 = new Oblong();
		OblongSink b2 = new OblongSink();
		OblongSink b3 = new OblongSink();
		
		add(p1, 0, 0);
		add(p2, 0, 1);
		add(p3, 0, 2);
		
		add(b1, 1, 0, 1, 3);
		add(b2, 2, 2);
		add(b3, 3, 0);
		
		add(p4, 2, 0);
		add(p5, 3, 1);
		add(p6, 3, 2);

		//setGridLinesVisible(true);
		return this;
	}	
}
