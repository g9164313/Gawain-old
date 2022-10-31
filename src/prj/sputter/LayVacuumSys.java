package prj.sputter;

import java.util.Optional;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class LayVacuumSys extends GridPane {
	
	
	private final static double PIPE_SIZE = 32.;
	private final static double PIPE_STROKE = 3.3; 
	
	private Rectangle create_node() {
		Rectangle node = new Rectangle(
			32.,32.,
			PIPE_SIZE/2., PIPE_SIZE
		);
		node.setStroke(Color.TRANSPARENT);
		node.setFill(Color.BLACK);
		return node; 
	}
	
	public class Pipe extends HBox {
		
		private Rectangle nodeLF = create_node();
		private Rectangle nodeRH = create_node();
		
		public Pipe() {
			getStyleClass().addAll("dbg-border");
			getChildren().addAll(nodeLF,nodeRH);
			//GridPane.setHgrow(child, value);
			GridPane.setVgrow(this, Priority.ALWAYS);
		}
	};
	
	public class Valve extends StackPane {
		
	};

	public class Pump {
		
	};
	
	public class Chamber extends StackPane {
		
		public Chamber() {
			setStyle(
				"-fx-padding: 32px;"+
				"-fx-border-color: BLACK;"+
				"-fx-border-style: SOLID;"+
				"-fx-border-radius: 30px;"+
				"-fx-border-width : 7.3px"
			);
			setMinSize(300,300);
			getChildren().add(lay);
			
			lay.getStyleClass().addAll("box-pad","dbg-border");
		}
		
		private GridPane lay = new GridPane();
		
		public Label postSticker(final String title) {
			Label name = new Label(title);
			name.getStyleClass().add("font-size7");
			Label ctxt = new Label("999.999 psi");
			ctxt.getStyleClass().add("font-size7");
			lay.addColumn(0, name);
			lay.addColumn(1, ctxt);
			return ctxt;
		}
	};
	
	public Optional<Chamber> node = Optional.empty();
	
	public Node[] layout_basic() {
		
		Chamber chamber = new Chamber();
		chamber.postSticker("xx1:");
		chamber.postSticker("xx2:");
		chamber.postSticker("xx3:");
		add(chamber, 0, 0, 1, 4);
		add(new Pipe(), 2, 2);
		add(new Pipe(), 2, 3);
		return new Node[] {chamber };
	}	
}
