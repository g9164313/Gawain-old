package prj.scada;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import narl.itrc.DevModbus;

public class LayDCG100 {

	public final static FloatProperty[] prop = {
		new SimpleFloatProperty(0.f),//channel A - voltage
		new SimpleFloatProperty(0.f),//channel A - amp
		new SimpleFloatProperty(0.f),//channel A - power		
		new SimpleFloatProperty(0.f),//channel B - voltage
		new SimpleFloatProperty(0.f),//channel B - amp
		new SimpleFloatProperty(0.f),//channel B - power
	};
	
	public static void bindProperty(final DevModbus dev) {
		//channel A
		prop[0].bind(dev.register(8001).multiply(0.20f));
		prop[1].bind(prop[2].divide(prop[0].add(Float.MIN_VALUE)));
		prop[2].bind(dev.register(8002).multiply(1.06f));
		//channel B
		prop[3].bind(dev.register(8003).multiply(0.20f));
		prop[4].bind(prop[5].divide(prop[3].add(Float.MIN_VALUE)));
		prop[5].bind(dev.register(8004).multiply(1.06f));
	}
	
	private static void set_power(
		final DevModbus dev,
		final int addr,
		final TextField box
	) {
		try {				
			int val = Integer.valueOf(box.getText());
			dev.asyncWriteVal(addr,val);
		}catch(NumberFormatException exp) {
			box.setText("50");
		}
	}
	
	public static GridPane genPanel(final DevModbus dev) {
		
		final Label[] txt = new Label[6];
		for(int i=0; i<txt.length; i++) {
			Label obj = new Label();
			obj.setMaxWidth(Double.MAX_VALUE);
			StringBinding str = null;
			switch(i%3) {
			case 0: str = prop[i].asString("%.1f V"); break;
			case 1: str = prop[i].asString("%.1f A"); break;
			case 2: str = prop[i].asString("%.0f W"); break;
			}
			obj.textProperty().bind(str);
			GridPane.setHgrow(obj, Priority.ALWAYS);
			txt[i] = obj;
		}
		
		final JFXTextField[] box = new JFXTextField[2];
		for(int i=0; i<box.length; i++) {
			JFXTextField obj = new JFXTextField();
			obj.setMaxWidth(Double.MAX_VALUE);
			obj.setLabelFloat(true);
			obj.setPromptText("輸出功率(Watt)");
			obj.setText("50");
			GridPane.setHgrow(obj, Priority.ALWAYS);
			box[i] = obj;
		}
		box[0].setOnAction(e->set_power(dev,8006,(TextField)e.getSource()));
		box[1].setOnAction(e->set_power(dev,8007,(TextField)e.getSource()));

		final JFXButton[] btn = new JFXButton[3];
		for(int i=0; i<btn.length; i++) {
			btn[i] = new JFXButton(); 			
			btn[i].getStyleClass().add("btn-raised-1");
			btn[i].setMaxWidth(Double.MAX_VALUE);
			GridPane.setHgrow(btn[i], Priority.ALWAYS);
		}
		btn[0].setText("點火");
		btn[0].setOnAction(e->{
			int power = Integer.valueOf(box[0].getText());
			dev.breakIn(()->{		
			dev.writeVal(8006,power);
			dev.write_OR(8005,0x01);
		});});
		
		btn[1].setText("熄火");
		btn[1].setOnAction(e->dev.asyncWriteAND(8005,0xFE));
		
		
		final GridPane lay0 = new GridPane();
		lay0.getStyleClass().addAll("box-pad");
		
		lay0.add(new Label("A通道"), 0, 0, 2, 1);
		lay0.addRow(1, new Label("電壓:"),txt[0]);
		lay0.addRow(2, new Label("電流:"),txt[1]);
		lay0.addRow(3, new Label("功率:"),txt[2]);
		lay0.addRow(4, new Label());//dummy for floating box title
		lay0.add(box[0], 0, 5, 2, 1);		
		
		lay0.add(new Label("B通道"), 0, 6, 2, 1);
		lay0.addRow(7, new Label("電壓:"),txt[3]);
		lay0.addRow(8, new Label("電流:"),txt[4]);
		lay0.addRow(9, new Label("功率:"),txt[5]);
		lay0.addRow(10, new Label());//dummy for floating box title
		lay0.add(box[1], 0, 11, 2, 1);
		
		lay0.add(new Separator(), 0, 12, 2, 1);
		lay0.add(btn[0], 0, 13, 2, 1);
		lay0.add(btn[1], 0, 14, 2, 1);
		return lay0;
	}
}
