package prj.letterpress;

import javafx.scene.Node;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import narl.itrc.Misc;
import narl.itrc.PanDecorate;

public class WidLight extends PanDecorate {

	public WidLight(){
		super("Aligment Light");
	}
	
	@Override
	public Node eventLayout() {
		
		final GridPane lay = new GridPane();
		lay.getStyleClass().add("grid-medium");
				
		ToggleButton tb0 = new ToggleButton("OFF");
		tb0.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		tb0.setOnAction(event->{
			//Entry.stg2.writeTxt('R');
			//Entry.stg1.writeTxt("1,0\r\n",20);
			//Entry.stg1.writeTxt("2,0\r\n",20);
			PanOption.enableAOI(false);
		});
		
		ToggleButton tb1 = new ToggleButton("ON");
		tb1.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		tb1.setSelected(true);
		tb1.setOnAction(event->{
			//Entry.stg2.writeTxt('F');
			//Entry.stg1.writeTxt("1,45\r\n",20);
			//Entry.stg1.writeTxt("2,45\r\n",20);
			PanOption.enableAOI(true);
		});
		
		final ImageView img0 = Misc.getIcon("lightbulb.png");		
		img0.visibleProperty().bind(tb0.selectedProperty());
		
		final ImageView img1 = Misc.getIcon("lightbulb-on-outline.png");		
		img1.visibleProperty().bind(tb1.selectedProperty());
		
		ToggleGroup grp = new ToggleGroup();
		tb0.setToggleGroup(grp);
		tb1.setToggleGroup(grp);
		Misc.getIcon("");
		
		GridPane.setHgrow(tb0, Priority.ALWAYS);
		GridPane.setHgrow(tb1, Priority.ALWAYS);
		
		lay.add(tb0 , 0, 0);
		lay.add(img0, 1, 0);
		lay.add(img1, 1, 0);
		lay.add(tb1 , 2, 0);
		return lay;
	}
}
