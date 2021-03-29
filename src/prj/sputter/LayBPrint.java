package prj.sputter;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class LayBPrint extends BorderPane {

	private final ModCouple coup;
	
	public LayBPrint(final ModCouple dev) {
		
		coup = dev;
		
		final JFXButton btn1 = new JFXButton("馬達(L)");
		final JFXButton btn2 = new JFXButton("馬達(R)");
		final JFXButton btn3 = new JFXButton("馬達(-)");
		btn1.getStyleClass().add("btn-raised-1");
		btn2.getStyleClass().add("btn-raised-1");
		btn3.getStyleClass().add("btn-raised-1");
		btn1.setOnAction(e->coup.asyncMotorPump(-1));
		btn2.setOnAction(e->coup.asyncMotorPump( 1));
		btn3.setOnAction(e->coup.asyncMotorPump( 0));
		
		final VBox lay0 = new VBox(btn1,btn2,btn3);
		lay0.getStyleClass().addAll("box-pad","border");
		
		setLeft(lay0);
	}
}
