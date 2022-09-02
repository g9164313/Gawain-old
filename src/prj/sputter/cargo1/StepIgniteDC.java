package prj.sputter.cargo1;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;


public class StepIgniteDC extends StepCommon {
	
	public static final String action_name = "DC 電漿";
	
	final TextField vol = new TextField();
	final TextField amp = new TextField();
	final TextField pow = new TextField();
	
	final TextField t_on_p = new TextField();
	final TextField t_on_n = new TextField();
	final TextField t_of_p = new TextField();
	final TextField t_of_n = new TextField();
	
	public StepIgniteDC() {
		vol.setPrefWidth(100.);
		amp.setPrefWidth(100.);
		pow.setPrefWidth(100.);
		
		t_on_p.setPrefWidth(87.);
		t_on_n.setPrefWidth(87.);
		t_of_p.setPrefWidth(87.);
		t_of_n.setPrefWidth(87.);
	}

	@Override
	public Node getContent() {
		return gen_grid_pane(
			action_name,"5:00",false,
			new HBox(new Label("電壓:"), vol), new HBox(new Label("Ton +:"), t_on_p),
			new HBox(new Label("電流:"), amp), new HBox(new Label("Toff+:"), t_of_p),
			new HBox(new Label("功率:"), pow), new HBox(new Label("Ton -:"), t_on_n),
			new Label(), new HBox(new Label("Toff-:"), t_of_n)
		);
	}
	@Override
	public void eventEdit() {
	}
	@Override
	public String flatten() {
		return null;
	}
	@Override
	public void expand(String txt) {
	}
}
