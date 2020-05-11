package prj.shelter;

import com.jfoenix.controls.JFXTabPane;

import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import narl.itrc.DevModbus;
import narl.itrc.Gawain;
import narl.itrc.PanBase;

public class PanMain extends PanBase {

	final DevModbus coup = new DevModbus();
	
	final LayDataBridge brdg = new LayDataBridge();
	
	public PanMain(){
		stage().setOnShown(e->on_shown());
	}
	
	private void on_shown(){
		String arg;
		arg = Gawain.prop().getProperty("modbus", "");
		if(arg.length()!=0) {			
			coup.open(arg);
			coup.mapRegister16("h30000","r30000");
		}
		brdg.connect();
	}
	
	@Override
	public Pane eventLayout(PanBase self) {
		
		final JFXTabPane lay1 = new JFXTabPane();
		lay1.getTabs().addAll(
			new Tab("調閱-F1",brdg),
			new Tab("照射-F2"),
			new Tab("校正-F3")
		);
		lay1.getSelectionModel().select(0);
		
		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay1);
		lay0.setRight(gen_ctrl_panel());
		return lay0;
	}
	
	private Pane gen_ctrl_panel(){
		
		Label t_tempo = new Label("20.1");
		Label t_humid = new Label("50%");
		Label t_press = new Label("psi");
		
		GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad-inner","big-font");
		lay1.addRow(0, new Label("溫度："), t_tempo);
		lay1.addRow(1, new Label("濕度："), t_humid);
		lay1.addRow(2, new Label("壓力："), t_press);
		lay1.add(new Separator(), 0, 3, 2, 1);
		lay1.addRow(4, new Label("源種："));
		lay1.addRow(5, new Label("位置："));
		lay1.addRow(6, new Label("時間："));
		lay1.add(new Separator(), 0, 7, 2, 1);
		
		VBox lay0 = new VBox(lay1);
		lay0.getStyleClass().addAll("box-pad","border");
		return lay0;
	}
}
