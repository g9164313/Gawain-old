package prj.LPS_8S;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import narl.itrc.Misc;

class XMark extends StackPane {
	final Region[] img = {
		new Region(),new Region(),new Region()	
	};
	final Label[] txt = {
		new Label("－－－"),new Label("－－－"),new Label("ERROR"),
	};
	final HBox[] lay = {
		new HBox(img[0],txt[0]),
		new HBox(img[1],txt[1]),
		new HBox(img[2],txt[2])
	};
	final IntegerProperty state = new SimpleIntegerProperty(0);
	public XMark(){
		img[0].getStyleClass().add("mark-cycle");
		img[1].getStyleClass().add("mark-check");
		img[2].getStyleClass().add("mark-cross");
		for(Label obj:txt) {
			obj.setMinWidth(67.);
		}
		txt[2].getStyleClass().add("font-red");
		
		for(HBox obj:lay) {
			obj.getStyleClass().add("box-pad");
			obj.setAlignment(Pos.CENTER);
		}
		lay[0].visibleProperty().bind(state.isEqualTo(0));
		lay[1].visibleProperty().bind(state.isEqualTo(1));
		lay[2].visibleProperty().bind(state.isEqualTo(2));
		StackPane.setAlignment(lay[0],Pos.CENTER);
		StackPane.setAlignment(lay[1],Pos.CENTER);
		StackPane.setAlignment(lay[2],Pos.CENTER);		
		
		getChildren().addAll(lay);
		setOnMouseClicked(e->next_state());
	}
	public XMark(final String S1,final String S2){
		this();
		txt[0].setText(S1);
		txt[1].setText(S2);
	}
	/**
	 * test function~~~
	 */
	public void next_state() {
		int ss = state.get();
		ss+=1;
		if(ss>=lay.length) {
			ss=0;
		}
		state.set(ss);
	}
}