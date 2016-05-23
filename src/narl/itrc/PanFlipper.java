package narl.itrc;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import eu.hansolo.enzo.flippanel.FlipPanel;

/**
 * This panel is regards as a wrap of FlipPanel(Enzo)
 * @author qq
 *
 */
abstract class PanFlipper extends FlipPanel {

	public PanFlipper(){
		super(Orientation.VERTICAL);
		getFront().getChildren().add(wrapFront());
		getBack().getChildren().add(wrapBack());
	}
	
	/**
	 * user layout widget here
	 * @return - the front panel with a setting icon
	 */
	abstract Node initFront();
	
	/**
	 * user layout widget here
	 * @return - the back panel with a icon to flip again~~
	 */
	abstract Node initBack();
	
	private Pane wrapFront() {
		Region icon = new Region();
		icon.getStyleClass().add("flipper-icon1");
		icon.addEventHandler(MouseEvent.MOUSE_CLICKED,EVENT->{
			PanFlipper.this.flipToBack();
		});
		Node body = initFront();
		VBox pane = new VBox();
		pane.getStyleClass().add("flipper-front");		
		if(body!=null){
			VBox.setVgrow(body,Priority.ALWAYS);
			pane.getChildren().addAll(icon,body);
		}else{
			pane.getChildren().addAll(icon);
		}
        return pane;
	}
	
	private Pane wrapBack() {
		Region icon = new Region();
		icon.getStyleClass().add("flipper-icon2");
		icon.addEventHandler(MouseEvent.MOUSE_CLICKED,EVENT->{
			PanFlipper.this.flipToFront();
		});
		Node body = initBack();
		VBox pane = new VBox();
		pane.getStyleClass().add("flipper-back");		
		if(body!=null){
			VBox.setVgrow(body,Priority.ALWAYS);
			pane.getChildren().addAll(icon,body);
		}else{
			pane.getChildren().addAll(icon);
		}
        return pane;
	}
}
