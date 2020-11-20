package prj.LPS_8S;

import java.util.Optional;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSlider;

import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import narl.itrc.PadTouch;

class XSlider extends VBox {
	
	Label txt = new Label();
	Label val = new Label();
	JFXSlider bar = new JFXSlider();
	JFXButton btn = new JFXButton();
	
	XSlider(final String name){	
		txt.setText(name);
		txt.setMinWidth(83.);
		txt.setOnMouseClicked(e->change_value());
		
		//val.textProperty().bind(bar.valueProperty().asString("%.1f"));
		val.setText("－－－－－");
		val.setMinWidth(83.);
		val.setOnMouseClicked(e->change_value());
		
		bar.setOrientation(Orientation.VERTICAL);
		bar.setMaxHeight(Double.MAX_VALUE);
		VBox.setVgrow(bar, Priority.ALWAYS);
		
		btn.setText("設定");
		btn.getStyleClass().add("btn-raised-1");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setMinHeight(32.);		
		btn.setOnAction(e->change_value());
		
		setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		setAlignment(Pos.CENTER);
		getChildren().addAll(txt,val,bar,btn);			
	}
	void reset_value() {
		bar.setValue(0.);
	}
	void change_value() {
		PadTouch pad = new PadTouch('N',"VALUE:");
		Optional<String> opt = pad.showAndWait();			
		if(opt.isPresent()==false) {
			return;
		}
		bar.setValue(Integer.valueOf(opt.get()));
	}
}