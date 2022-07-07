package prj.sputter;

import eu.hansolo.tilesfx.Tile;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class PanMain3 extends PanBase {

	//private DevAdam4024 a4024 = new DevAdam4024("01");	
	private DevAdam4117 a4117 = new DevAdam4117("11");
	
	public PanMain3(Stage stg) {
		super(stg);		
		stg.setOnShown(e->on_shown());
	}

	void on_shown() {
		a4117.open();
		//a4024.open(a4117);
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final Tile ch7 = LayTool.create_vaccum_gauge(
			"前級真空計",
			a4117.aout[7].val, 
			1.7f, 8.f, 
			"Torr",src->{
				float dst = (float)Math.pow(10f, src-3f);//Pa
				dst = dst * 0.0075006168f;
				return dst;
			});//measure fore-line pump 
		ch7.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		GridPane.setHgrow(ch7, Priority.ALWAYS);
		GridPane.setVgrow(ch7, Priority.ALWAYS);
				
		final Tile ch6 = LayTool.create_vaccum_gauge(
			"腔體真空計",
			a4117.aout[6].val, 
			0.1f, 9.9f, 
			"Torr",src->{
			float dst = (float)Math.pow(10f, (src-7.25f)/0.75f-0.125f);
			return dst;
		});
		ch6.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		GridPane.setHgrow(ch6, Priority.ALWAYS);
		GridPane.setVgrow(ch6, Priority.ALWAYS);
		
		
		//Misc.MetricPrefix prop_ch7 = new Misc.MetricPrefix(ch7.valueProperty(),"Torr");
		//Misc.MetricPrefix prop_ch6 = new Misc.MetricPrefix(ch6.valueProperty(),"Torr");
		
		final Label[] txt_info = {
			new Label("前級真空計:"), new Label(),
			new Label("腔體真空計:"), new Label(),
		};
		txt_info[0].getStyleClass().addAll("font-size20");
		txt_info[1].getStyleClass().addAll("font-size40");
		txt_info[2].getStyleClass().addAll("font-size20");
		txt_info[3].getStyleClass().addAll("font-size40");
		txt_info[1].setMinWidth(200);
		txt_info[1].textProperty().bind(ch7.valueProperty().asString("%1.3E"));//前級真空計
		txt_info[3].setMinWidth(200);
		txt_info[3].textProperty().bind(ch6.valueProperty().asString("%1.3E"));//腔體真空計
		
		
		final VBox lay2 = new VBox(txt_info);
		lay2.getStyleClass().addAll("box-pad");
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad");
		lay1.addRow(0, ch7, ch6);
		
		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay1);
		lay0.setRight(lay2);
		//lay0.setCenter(DevAdam4117.genPanel(a4117));
		//lay0.setBottom(DevAdam4024.genPanel(a4024));
		return lay0;
	}

}
