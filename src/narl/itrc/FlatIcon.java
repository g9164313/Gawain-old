package narl.itrc;

import com.sun.javafx.scene.control.skin.ButtonSkin;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Skin;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;

public class FlatIcon extends ButtonBase {

	private static final double DEF_SIZE = 25.;
	

	public FlatIcon(String name){
		super("", Misc.getIcon(name));
		/*super(DEF_SIZE,DEF_SIZE);
		
		img = Misc.getImage(name);
		
		getGraphicsContext2D().drawImage(
			img, 
			0, 0, img.getWidth(), img.getHeight(), 
			0, 0, DEF_SIZE, DEF_SIZE
		);
		
		setOnMousePressed(eventPress);		
		setOnMouseReleased(eventRelease);
		Button btn;*/
	}

	@Override
	public void fire() {
		if(!isDisabled()){
			fireEvent(new ActionEvent());
		}
	}
}
