package prj.sputter.perspect;

import javafx.beans.property.StringProperty;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Mass flow control used this symbol.<p>
 * @author qq
 *
 */
public class ValveLift extends PElement {

	private static final String s_info =
		"-fx-font-size: 1.5em;"+
		"-fx-spacing: 7px;"+
		"-fx-padding: 7px;"+
		"-fx-hgap: 7px;"+
		"-fx-vgap: 7px;"+
		"-fx-background-color: WHITE;"+
		"-fx-border-color: BLACK;"+
		"-fx-border-style: SOLID;"+
		"-fx-border-radius: 14px;"+
		"-fx-border-width: 2.1px;";
	
	public GridPane info = new GridPane();
	private int info_row = 0;
	
	public ValveLift() {
		super(PDir.HORI);
		info.setStyle(s_info);
		info.setMinSize(60., 60.);
		rh.setMinWidth(30.);
		
		add(info, 1, 1);
		
		getChildren().remove(lf);
		
		//rh.setMinWidth(57.);
		//rh.setMinHeight(PIPE_SIZE);
		
		GridPane.setVgrow(info, Priority.ALWAYS);
		GridPane.setHgrow(info, Priority.ALWAYS);

		addInfo("PV","000.0 psi");
		addInfo("SV","000.0 psi");

		//setGridLinesVisible(true);
	}
	
	public ValveLift addInfo(
		final String name,
		final Label text
	) {
		info.addRow(
			info_row++, 
			new Label(name), new Label("："), 
			text
		);
		return this;
	}
	public ValveLift addInfo(
		final String name,
		final StringProperty text
	) {
		Label _txt =new Label();
		_txt.textProperty().bind(text);
		return addInfo(name,_txt);
	}
	public ValveLift addInfo(
		final String name,
		final String text
	) {
		return addInfo(name,new Label(text));
	}
}
