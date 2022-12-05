package prj.sputter.diagram;

import javafx.beans.property.StringProperty;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class PanInfo extends GridPane {

	private static final String s_info =
		"-fx-font-size: 1.5em;"+
		"-fx-spacing: 7px;"+
		"-fx-padding: 7px;"+
		"-fx-hgap: 7px;"+
		"-fx-vgap: 7px;"+
		"-fx-background-color: transparent;";
		
	public PanInfo() {
		setStyle(s_info);
		setMinSize(60., 60.);
		//GridPane.setHalignment(this, HPos.CENTER);
		//GridPane.setValignment(this, VPos.CENTER);
		//GridPane.setVgrow(this, Priority.ALWAYS);
		//GridPane.setHgrow(this, Priority.ALWAYS);
	}
	
	private int info_row = 0;
	
	public PanInfo insert(
		final String name,
		final Label text
	) {
		addRow(
			info_row++, 
			new Label(name), new Label("："), 
			text
		);
		return this;
	}
	public PanInfo insert(
		final String name,
		final StringProperty text
	) {
		Label _txt =new Label();
		_txt.textProperty().bind(text);
		return insert(name,_txt);
	}
	public PanInfo insert(
		final String name,
		final String text
	) {
		return insert(name,new Label(text));
	}
}
