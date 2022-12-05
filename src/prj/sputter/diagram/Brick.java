package prj.sputter.diagram;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

public class Brick extends StackPane {
	
	private final String s_txt = 
		"-fx-background-color: WHITE;"+
		"-fx-border-color: BLACK;"+
		"-fx-border-style: SOLID;"+
		"-fx-border-radius: 27px;"+
		"-fx-border-width: 6px;";
	
	public Brick() {
		GridPane.setVgrow(this, Priority.ALWAYS);
		GridPane.setHgrow(this, Priority.ALWAYS);
		setMinSize(100., 100.);
		setStyle(s_txt);
		getChildren().add(info);
	}
	
	public final PanInfo info = new PanInfo();	
}
