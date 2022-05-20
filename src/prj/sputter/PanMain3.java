package prj.sputter;

import eu.hansolo.tilesfx.TileBuilder;

import java.util.ArrayList;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.Tile.SkinType;
import eu.hansolo.tilesfx.Tile.TextSize;
import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import narl.itrc.PanBase;

public class PanMain3 extends PanBase {

	private DevAdam4024 a4024 = new DevAdam4024("01");	
	private DevAdam4117 a4117 = new DevAdam4117("11");
	
	public PanMain3(Stage stg) {
		super(stg);
		stg.setOnShown(e->on_shown());
	}

	void on_shown() {
		a4024.open();
		//a4117.open(a4024);
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final BorderPane lay0 = new BorderPane();		
		//lay0.setCenter(DevAdam4117.genPanel(a4117));
		lay0.setCenter(DevAdam4024.genPanel(a4024));
		return lay0;
	}

}
