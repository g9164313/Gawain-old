package prj.scada;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXToggleButton;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.PanBase;

public class PanLayout {

	public static Node gen_information(final DevSPIK2000 dev){
		
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
		
		final Button btnTest1 = PanBase.genButton2("Update", null);
		btnTest1.setMaxWidth(Double.MAX_VALUE);
		btnTest1.setOnAction(event1->{
			dev.update();
		});
		
		//final JFXToggleButton btn2 = new JFXToggleButton();
		final Button btn2 = PanBase.genButton2("Update", null);
		btn2.setText("DC-1");
		btn2.setMaxWidth(Double.MAX_VALUE);
		btn2.setOnAction(event1->{
			if(dev.State_DC1_On.get()==true){
				dev.setState(DevSPIK2000.STA_DC1_OFF);
			}else{
				dev.setState(DevSPIK2000.STA_DC1_ON);
			}
		});
		
		final Button btnTest3 = PanBase.genButton2("Update", null);
		btnTest3.setMaxWidth(Double.MAX_VALUE);
		btnTest3.setOnAction(event1->{
			dev.update();
		});
		
		final VBox lay0 = new VBox();
		lay0.setStyle("-fx-spacing: 13;");
		lay0.getChildren().addAll(
			lay1,lay2,
			btnTest1,
			btn2,
			btnTest3
		);
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
		return gen_checkbox(txt,prop,"-jfx-checked-color: RED;");
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
	
	
}
