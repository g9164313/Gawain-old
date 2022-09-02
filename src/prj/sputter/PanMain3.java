package prj.sputter;

import java.util.Optional;

import eu.hansolo.tilesfx.Tile;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import narl.itrc.PanBase;

public class PanMain3 extends PanBase {
	
	private DevAdam4024 a4024 = new DevAdam4024(1);
	private DevAdam4x17 a4117 = new DevAdam4x17(11);

	private float MFC_MAX_SCCM = 100f;
	
	public PanMain3(Stage stg) {
		super(stg);
		stg.setOnShown(e->on_shown());
	}
	
	void on_shown() {
		a4117.open();
		a4024.open(a4117);
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		
		//final SimpleFloatProperty test = new SimpleFloatProperty(0.1f);
		
		final ReadOnlyFloatProperty v_ch7 = LayTool.transform(a4117.ain[7].val, src->{
			float dst = (float)Math.pow(10f, src-3f);//Pa
			dst = dst * 0.0075006168f;//Pa-->Torr
			return dst;
		});
		//final ReadOnlyFloatProperty v_ch6 = test;
		final ReadOnlyFloatProperty v_ch6 = LayTool.transform(a4117.ain[6].val, src->{
			float dst = (float)Math.pow(10f, (src-7.25f)/0.75f-0.125f);
			return dst;//Torr
		});
		
		final SimpleFloatProperty v_mfc_sv = new SimpleFloatProperty(0);
		final ReadOnlyFloatProperty v_mfc_pv = LayTool.transform(a4117.ain[0].val, src->{
			return (src*MFC_MAX_SCCM)/5f; //0~5V --> 0~100sccm
		});
		
		final Node ch7 = LayTool.create_prefix_gauge("前級真空計","Torr",v_ch7);// fore-line pump		
		GridPane.setHgrow(ch7, Priority.ALWAYS);
		GridPane.setVgrow(ch7, Priority.ALWAYS);
		final Node ch6 = LayTool.create_prefix_gauge("腔體真空計","Torr",v_ch6);
		GridPane.setHgrow(ch6, Priority.ALWAYS);
		GridPane.setVgrow(ch6, Priority.ALWAYS);

		/*ch6.setOnMouseClicked(e->{
			final TextInputDialog dia = new TextInputDialog(String.format("%.9f", test.get()));
			dia.setTitle("test");
			final Optional<String> opt = dia.showAndWait();
			if(opt.isPresent()==true) {
				test.set(Float.parseFloat(opt.get()));
			}			
		});*/
		
		final Tile mfc = LayTool.create_MFC_gauge(
			"MFC - 100 sccm","sccm",100.,
			v_mfc_pv, src->{
				//clap data~~~
				if(src>=MFC_MAX_SCCM) {
					src = MFC_MAX_SCCM;
				}else if(src<=0f) {
					src = 0f;
				}
				//0~100sccm --> 0~5V
				final float dst = (src * 5f) / MFC_MAX_SCCM;
				v_mfc_sv.set(dst);
				a4024.asyncDirectOuput(a4024.aout[0], dst);
				return 0f;
			});
		mfc.setMinHeight(200);
		
		final Label[] txt_info = {
			new Label("前級真空計:"), new Label(),
			new Label("腔體真空計:"), new Label(),
			new Label("MFC-1 PV:"), new Label(),
			new Label("MFC-1 SV:"), new Label(),
		};
		for(Label txt:txt_info) {
			txt.getStyleClass().add("font-size25");
		}
		txt_info[1].setMinWidth(200);
		txt_info[1].textProperty().bind(v_ch7.asString("%1.2E"));//前級真空計
		txt_info[3].setMinWidth(200);
		txt_info[3].textProperty().bind(v_ch6.asString("%1.2E"));//腔體真空計
		
		txt_info[5].setMinWidth(200);
		txt_info[5].textProperty().bind(v_mfc_pv.asString("%6.2f"));//MFC-1, property value, unit is sccm
		txt_info[7].setMinWidth(200);
		txt_info[7].textProperty().bind(v_mfc_sv.asString("%6.2f"));//MFC-1, setting value, unit is sccm
		
		final VBox lay2 = new VBox(txt_info);
		lay2.getStyleClass().addAll("box-pad");
		
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
