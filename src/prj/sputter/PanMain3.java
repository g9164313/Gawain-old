package prj.sputter;

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.Tile.SkinType;
import javafx.beans.property.ReadOnlyFloatProperty;
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
	private DevAdam4024 a4024 = new DevAdam4024("01",DevAdam.z5V,DevAdam.z5V,DevAdam.z5V,DevAdam.z5V);
	private DevAdam4117 a4117 = new DevAdam4117("11");

	private float MFC_MAX_SCCM = 100f;
	
	public PanMain3(Stage stg) {
		super(stg);		
		stg.setOnShown(e->on_shown());
	}
	
	void on_shown() {
		//a4117.open();
		//a4024.open(a4117);
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		final ReadOnlyFloatProperty v_ch7 = LayTool.transform(a4117.ain[7].val, src->{
			float dst = (float)Math.pow(10f, src-3f);//Pa
			dst = dst * 0.0075006168f;//Pa-->Torr
			return dst;
		});		
		final ReadOnlyFloatProperty v_ch6 = LayTool.transform(a4117.ain[6].val, src->{
			float dst = (float)Math.pow(10f, (src-7.25f)/0.75f-0.125f);
			return dst;//Torr
		});
				
		final ReadOnlyFloatProperty v_ch0 = LayTool.transform(a4117.ain[0].val, src->{
			return (src*MFC_MAX_SCCM)/5f; //0~5V --> 0~100sccm
		});
		
		final Node ch7 = LayTool.create_prefix_gauge("前級真空計","Torr",v_ch7);// fore-line pump		
		GridPane.setHgrow(ch7, Priority.ALWAYS);
		GridPane.setVgrow(ch7, Priority.ALWAYS);
		final Node ch6 = LayTool.create_prefix_gauge("腔體真空計","Torr",v_ch6);
		GridPane.setHgrow(ch6, Priority.ALWAYS);
		GridPane.setVgrow(ch6, Priority.ALWAYS);

		final Tile mfc = LayTool.create_MFC_gauge(
			"MFC","sccm",100.,
			v_ch0, src->{
				//clap data~~~
				if(src>=MFC_MAX_SCCM) {
					src = MFC_MAX_SCCM;
				}else if(src<=0f) {
					src = 0f;
				}
				//0~100sccm --> 0~5V
				final float dst = (src * 5f) / MFC_MAX_SCCM;
				a4024.asyncDirectOuput(a4024.aout[0], dst);
				return 0f;
			});
		
		final Label[] txt_info = {
			new Label("前級真空計:"), new Label(),
			new Label("腔體真空計:"), new Label(),
		};
		txt_info[1].setMinWidth(200);
		txt_info[1].textProperty().bind(v_ch7.asString("%1.2E"));//前級真空計
		txt_info[3].setMinWidth(200);
		txt_info[3].textProperty().bind(v_ch6.asString("%1.2E"));//腔體真空計
		
		final VBox lay2 = new VBox(txt_info);
		lay2.getStyleClass().addAll("box-pad","font-size20");
		
		final GridPane lay1 = new GridPane();
		lay1.getStyleClass().addAll("box-pad");
		lay1.addRow(0, ch7, ch6);
		lay1.addRow(1, mfc);
		
		final BorderPane lay0 = new BorderPane();
		lay0.setCenter(lay1);
		lay0.setRight(lay2);
		//lay0.setCenter(DevAdam4117.genPanel(a4117));
		//lay0.setBottom(DevAdam4024.genPanel(a4024));
		return lay0;
	}

}
