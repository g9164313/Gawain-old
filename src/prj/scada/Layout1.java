package prj.scada;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTabPane;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.controls.JFXToggleButton;

import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.medusa.Gauge.SkinType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class Layout1 {

	public static Node gen_information(final DevSPIK2k dev){
		
		final int MIN_COL_WIDTH = 53;
		
		final  Label[] lstMeas = new Label[7];
		for(int i=0; i<lstMeas.length; i++){
			Label txt = new Label();
			txt.setMinWidth(MIN_COL_WIDTH);
			txt.setAlignment(Pos.BASELINE_RIGHT);
			lstMeas[i] = txt;			
		}
		lstMeas[0].textProperty().bind(dev.ARC_Count.asString());
		lstMeas[1].textProperty().bind(dev.DC1_V_Act.asString());
		lstMeas[2].textProperty().bind(dev.DC1_I_Act.asString());
		lstMeas[3].textProperty().bind(dev.DC1_P_Act.asString());
		lstMeas[4].textProperty().bind(dev.DC2_V_Act.asString());
		lstMeas[5].textProperty().bind(dev.DC2_I_Act.asString());
		lstMeas[6].textProperty().bind(dev.DC2_P_Act.asString());
		
		Label t1 = new Label("DC-1");
		t1.setMinWidth(MIN_COL_WIDTH);
		t1.setAlignment(Pos.BASELINE_RIGHT);
		
		Label t2 = new Label("DC-1");
		t2.setMinWidth(MIN_COL_WIDTH);
		t2.setAlignment(Pos.BASELINE_RIGHT);
		GridPane.setHgrow(t2, Priority.ALWAYS);
		
		final GridPane lay1 = new GridPane();
		lay1.setStyle("-fx-vgap: 7px;");
		lay1.addRow(0, new Label("    "), t1, t2); 
		lay1.addRow(1, new Label("電壓："), lstMeas[1], lstMeas[4]); 
		lay1.addRow(2, new Label("電流："), lstMeas[2], lstMeas[5]); 
		lay1.addRow(3, new Label("功率："), lstMeas[3], lstMeas[6]);
		lay1.add(new Label("電弧："), 0, 4);
		lay1.add(lstMeas[0], 1, 4, 2, 1);
		
		final String[] lstStateTitle = {
			"Error",
			"Multiplex",			
			"ARC Delay",
			"CFG Saved",
			"DC-1",
			"DC-2",
			"Ready",
			"Running",
		};
		final BooleanProperty[] lstStateProp = {
			dev.State_Error,
			dev.Mode_Multiplex,
			dev.State_Arc_Delay,
			dev.State_CFG_Saved,
			dev.State_DC1_On,
			dev.State_DC2_On,
			dev.State_Ready,
			dev.State_Running,	
		};
		
		final JFXCheckBox[] chk = gen_readonly_checkbox(
			lstStateTitle, 
			lstStateProp, 
			""
		);
		chk[0].setStyle("-fx-opacity: 1; -jfx-checked-color: RED;");//the box has special color
		
		final GridPane lay2 = new GridPane();
		lay2.setStyle("-fx-hgap: 7px; -fx-vgap: 7px;");
		lay2.addRow(0, chk[0], chk[4]); 
		lay2.addRow(1, chk[1], chk[5]);
		lay2.addRow(2, chk[2], chk[6]); 
		lay2.addRow(3, chk[3], chk[7]); 
		
		final JFXToggleButton tglRun = new JFXToggleButton();
		tglRun.setText("Running");
		tglRun.selectedProperty().unbindBidirectional(dev.State_Running);
		tglRun.setOnAction(event1->{
			dev.Running(tglRun.isSelected());
		});
		
		final JFXToggleButton tglDC_1 = new JFXToggleButton();
		tglDC_1.setText("DC-1");
		tglDC_1.selectedProperty().unbindBidirectional(dev.State_DC1_On);
		tglDC_1.setOnAction(event1->{
			dev.setDC1(tglDC_1.isSelected());
		});
		
		final JFXToggleButton tglDC_2 = new JFXToggleButton();
		tglDC_2.setText("DC-2");
		tglDC_2.selectedProperty().unbindBidirectional(dev.State_DC2_On);
		tglDC_2.setOnAction(event1->{
			dev.setDC1(tglDC_2.isSelected());
		});
		
		final Button btnSetting = PanBase.genButton2("設定參數", "wrench.png");
		btnSetting.setMaxWidth(Double.MAX_VALUE);
		btnSetting.setOnAction(event1->{
			new PanBase(){
				@Override
				public Node eventLayout(PanBase self) {
					stage().setOnShown(e->dev.getRegister());
					return gen_setting(dev);
				}
			}.appear();
		});
		
		final Button btnSaveCFG = PanBase.genButton2("儲存設定", "content-save.png");
		btnSaveCFG.setMaxWidth(Double.MAX_VALUE);
		btnSaveCFG.setOnAction(event1->{
			dev.Save_CFG();
		});

		final VBox lay0 = new VBox();
		lay0.setStyle("-fx-padding: 7; -fx-spacing: 7;");
		lay0.getChildren().addAll(
			lay1, lay2,
			tglRun,
			tglDC_1, 
			tglDC_2,			
			btnSetting,
			btnSaveCFG
		);
		return lay0;
	}
	
	private static class ItemParam{
		public IntegerProperty prop;
		public Label name, value;
		public JFXTextField box;
		public final int BOX_WIDTH = 53;
		public ItemParam(String arg1, IntegerProperty arg2){
			prop = arg2;
			name = new Label(arg1);
			value= new Label();
			value.setPrefWidth(BOX_WIDTH);
			value.setAlignment(Pos.BASELINE_RIGHT);
			value.textProperty().bind(prop.asString());
			box = new JFXTextField();
			box.setPrefWidth(BOX_WIDTH);
			box.setOnAction(e->{
				try{					
					int val = Integer.valueOf(box.getText());
					prop.setValue(val);
				}catch(NumberFormatException exp){
				}
			});
		}
	};
	
	private static Node gen_setting(DevSPIK2k dev){
		
		final ItemParam[] parm = {
			new ItemParam("Pulse_Pos", dev.Reg_Puls_Pos),
			new ItemParam("Pause_Pos", dev.Reg_Paus_Pos),
			new ItemParam("Pulse_Neg", dev.Reg_Puls_Neg),
			new ItemParam("Pause_Neg", dev.Reg_Paus_Neg),
			new ItemParam("ARC_Pos"  , dev.Reg_ARC_Pos),
			new ItemParam("ARC_Neg"  , dev.Reg_ARC_Neg),
			new ItemParam("ARC_Delay", dev.Reg_ARC_Delay),
			new ItemParam("ARC_Overflow", dev.Reg_ARC_Overflow),
			new ItemParam("ARC_Interval", dev.Reg_ARC_Interval),
			new ItemParam("DC1_Voltage" , dev.Reg_DC1_Volt),
			new ItemParam("DC1_Ampere", dev.Reg_DC1_Amp),
			new ItemParam("DC1_Power", dev.Reg_DC1_Pow),
			new ItemParam("DC2_Voltage", dev.Reg_DC2_Volt),
			new ItemParam("DC2_Ampere", dev.Reg_DC2_Amp),
			new ItemParam("DC2_Power", dev.Reg_DC2_Pow),
		};

		final JFXComboBox<String> cmbMode = new JFXComboBox<String>();
		cmbMode.getItems().addAll(DevSPIK2000.ModeText);
		cmbMode.valueProperty().bindBidirectional(dev.Mode_Operation);
		
		final Button btnFetch = PanBase.genButton2("更新", "refresh.png");
		btnFetch.setMaxWidth(Double.MAX_VALUE);
		btnFetch.setOnAction(event1->{
			dev.getRegister();
		});
		
		final Button btnApply = PanBase.genButton2("套用", "check.png");
		btnApply.setMaxWidth(Double.MAX_VALUE);
		btnApply.setOnAction(event1->{
			dev.setRegister();
		});
		
		final GridPane lay31 = new GridPane();
		lay31.setStyle("-fx-hgap: 7px; -fx-vgap: 7px;");
		lay31.add(new Label("Mode"), 0, 0, 1, 1);
		lay31.add(cmbMode, 1, 0, 2, 1);
		lay31.addRow(1, parm[0].name, parm[0].value, parm[0].box);
		lay31.addRow(2, parm[1].name, parm[1].value, parm[1].box);
		lay31.addRow(3, parm[2].name, parm[2].value, parm[2].box);
		lay31.addRow(4, parm[3].name, parm[3].value, parm[3].box);
		
		final GridPane lay32 = new GridPane();
		lay32.setStyle("-fx-hgap: 7px; -fx-vgap: 7px;");
		lay32.addRow(0, parm[4].name, parm[4].value, parm[4].box);
		lay32.addRow(1, parm[5].name, parm[5].value, parm[5].box);
		lay32.addRow(2, parm[6].name, parm[6].value, parm[6].box);
		lay32.addRow(3, parm[7].name, parm[7].value, parm[7].box);
		lay32.addRow(4, parm[8].name, parm[8].value, parm[8].box);
		
		final GridPane lay33 = new GridPane();
		lay33.setStyle("-fx-hgap: 7px; -fx-vgap: 7px;");
		lay33.addRow(0, parm[ 9].name, parm[ 9].value, parm[ 9].box);
		lay33.addRow(1, parm[10].name, parm[10].value, parm[10].box);
		lay33.addRow(2, parm[11].name, parm[11].value, parm[11].box);
		lay33.addRow(3, parm[12].name, parm[12].value, parm[12].box);
		lay33.addRow(4, parm[13].name, parm[13].value, parm[13].box);
		lay33.addRow(5, parm[14].name, parm[14].value, parm[14].box);
		
		final Tab[] tabs = {
			new Tab("脈衝",lay31),
			new Tab("電弧",lay32),
			new Tab("直流",lay33),	
		};
		final JFXTabPane lay2 = new JFXTabPane();
		lay2.getTabs().addAll(tabs);
		
		final HBox lay1 = new HBox();
		HBox.setHgrow(btnFetch, Priority.ALWAYS);
		HBox.setHgrow(btnApply, Priority.ALWAYS);
		lay1.setStyle("-fx-spacing: 13;");		
		lay1.getChildren().addAll(btnFetch, btnApply);
		
		final VBox lay0 = new VBox();
		lay0.setStyle("-fx-padding: 7; -fx-spacing: 7;");
		lay0.getChildren().addAll(lay2, lay1);
		return lay0;
	}
	
	public static VBox gen_info_error(DevSPIK2000 dev){
		
		final String[] txt = {
			"Driver-1L",
			"Driver-1R",
			"Driver-2L",
			"Driver-2R",
			"ARC＋",
			"ARC－",
			"ARC Overflow",
			"Rack Temp",
			"Heat Sink-1",
			"Heat Sink-2",
			"Interlock",
			"DC-1",
			"DC-2",
			"Configure",
			"Address",
			"Watchdog"
		};
		final BooleanProperty[] prop = {
			dev.Error_Driver1L,
			dev.Error_Driver1R,
			dev.Error_Driver2L,
			dev.Error_Driver2R,
			dev.Error_Arc_Pos,
			dev.Error_Arc_Neg,
			dev.Error_Arc_Over,
			dev.Error_Rack_Temp,
			dev.Error_HeatSink1,
			dev.Error_HeatSink2,
			dev.Error_Interlock,
			dev.Error_DC1,
			dev.Error_DC2,
			dev.Error_Config,
			dev.Error_Address,
			dev.Error_Watchdog,
		};
		return gen_checkbox(txt, prop, "-jfx-checked-color: RED;");
	}

	private static VBox gen_checkbox(
		final String[] title, 
		final BooleanProperty[] property,
		final String appendStyle
	){
		final VBox lay0 = new VBox();
		lay0.setStyle("-fx-spacing: 7;");
		lay0.getChildren().addAll(gen_readonly_checkbox(title, property, appendStyle));		
		return lay0;
	}
	
	private static JFXCheckBox[] gen_readonly_checkbox(
		final String[] title, 
		final BooleanProperty[] property,
		final String appendStyle
	){
		int cnt = title.length;		
		final JFXCheckBox[] lst = new JFXCheckBox[cnt];
		for(int i=0; i<cnt; i++){
			JFXCheckBox box = new JFXCheckBox(title[i]);			
			box.setStyle("-fx-opacity: 1;"+appendStyle);
			box.selectedProperty().bind(property[i]);
			box.setDisable(true);			
			lst[i] = box;
		}
		return lst;
	}
	
	public static Node gen_gauge_scope(DevSPIK2000 dev){

		final Gauge gag[] = {
			gen_gage("DC-1 電壓","V","",dev.DC1_V_Act),
			gen_gage("DC-1 電流","A","",dev.DC1_I_Act),
			gen_gage("DC-1 功率","W","",dev.DC1_P_Act),
			gen_gage("DC-2 電壓","V","",dev.DC2_V_Act),
			gen_gage("DC-2 電流","A","",dev.DC2_I_Act),
			gen_gage("DC-2 功率","W","",dev.DC2_P_Act),
		};
		
		final GridPane lay0 = new GridPane();
		lay0.setStyle("-fx-spacing: 13; -fx-hgap: 7px; -fx-vgap: 7px;");
		lay0.addRow(0, gag[0], gag[3]);
		lay0.addRow(1, gag[1], gag[4]);
		lay0.addRow(2, gag[2], gag[5]);
		return lay0;
	}
	
	private static Gauge gen_gage(
		final String name,
		final String unit,
		final String subTxt,
		final IntegerProperty prop
	){
		final Gauge gag = GaugeBuilder.create()
				.skinType(SkinType.TILE_SPARK_LINE)
				.title(name)
				.unit(unit)
				.subTitle(subTxt)
				.autoScale(true)
				.build();
		gag.setOnMouseClicked(event->{
			Misc.logv("ggyy");
		});
		gag.valueProperty().bind(prop.multiply(1.));
		return gag;
	}
}
