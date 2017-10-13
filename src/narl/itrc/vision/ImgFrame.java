package narl.itrc.vision;

import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

public class ImgFrame extends AnchorPane {

	private ImageView screen = new ImageView();
	
	private CamBundle bundle;
	
	public ImgFrame(){
		//this();
	}
	
	public ImgFrame(CamBundle cam){
		bundle = cam;
		setOnMouseMoved(event->{
			
		});
		setOnMouseClicked(event->{
			MouseButton btn = event.getButton();
			if(btn==MouseButton.PRIMARY){
				
			}else if(btn==MouseButton.SECONDARY){
				
			}
		});
	}
	
	
	/*	HBox lay = new HBox();
	lay.setStyle(
		"-fx-background-color: palegreen; "+
		"-fx-padding: 13;"+
		"-fx-spacing: 7; "+
		"-fx-background-radius: 10; "+		
		"-fx-effect: dropshadow(three-pass-box, black, 10, 0, 0, 0);"		
	);
	 */
}
