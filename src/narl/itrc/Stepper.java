package narl.itrc;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;

import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class Stepper extends HBox {

	private static final Image v_empty = new WritableImage(24,24);
	private static final Image v_arrow = Misc.getIconImage("arrow-right.png");
	private static final Image v_pen = Misc.getIconImage("pen.png");
	private static final Image v_tash_can = Misc.getIconImage("trash-can.png");
	
	private ImageView imgSign = new ImageView();

	private JFXButton btnEdit = new JFXButton();
	private JFXButton btnDrop = new JFXButton();
	
	public Optional<Runnable[]> works = Optional.empty();
	public Optional<ObservableList<Stepper>> items = Optional.empty();
	
	/**
	 * decide that work should repeat or stop self.<p>
	 * >0 : repeat
	 * =0 : next 
	 * <0 : abort
	 */
	public int result = 0;
	
	public Stepper(){
	}
	
	public void indicate(boolean flag){
		if(flag){
			imgSign.setImage(v_arrow);
		}else{
			imgSign.setImage(v_empty);
		}
	}
	
	public Stepper doLayout(){
		
		indicate(false);
		
		Node cntxt = getContent();
		HBox.setHgrow(cntxt, Priority.ALWAYS);
		
		btnEdit.setGraphic(new ImageView(v_pen));
		btnEdit.setOnAction(e->eventEdit());
		
		btnDrop.setGraphic(new ImageView(v_tash_can));
		btnDrop.setOnAction(e->dropping());
				
		setAlignment(Pos.BASELINE_LEFT);
		getChildren().addAll(imgSign,cntxt,btnEdit,btnDrop);		
		return this;
	}
		
	private void dropping(){
		if(items.isPresent()==false){
			return;
		}
		items.get().remove(this);
	}
	
	protected Node getContent(){
		Label txt = new Label("-DEFAULT-");
		txt.setMaxWidth(Double.MAX_VALUE);
		txt.getStyleClass().addAll("border");
		return txt;
	}
	
	protected void eventEdit(){
		//let user pop a panel to edit parameter~~~
	}
}
