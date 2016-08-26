package prj.daemon;

import java.util.ArrayList;

import com.jfoenix.controls.JFXButton;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import narl.itrc.ImgRender;
import narl.itrc.ImgFilter;
import narl.itrc.ImgPreview;

public class FilterIsBlur extends ImgFilter {

	public FilterIsBlur(){
	}
	
	private float parmBlur,parmExtend;
	
	private native void implCookData(long ptrMatx);
	
	@Override
	public void cookData(ArrayList<ImgPreview> list) {
		//TODO:implCookData(list.get(0).getMatx());
	}

	@Override
	public boolean showData(ArrayList<ImgPreview> list) {
		txtParm[0].setText(String.format("%2.1f%%", parmBlur*100.f));
		txtParm[1].setText(String.format("%.4f", parmExtend));
		return true;//we done
	}
	
	private Label txtParm[]={
		new Label(),
		new Label()
	};
	
	public Pane getPanel(final ImgRender rndr){		
		GridPane lay0 = new GridPane();
		lay0.getStyleClass().add("grid-small");
		
		JFXButton btn = new JFXButton("分析清晰度");
		btn.getStyleClass().add("btn-raised1");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(event->{
			rndr.addFilter(FilterIsBlur.this);
		});
		
		txtParm[0].setPrefWidth(80);
		txtParm[1].setPrefWidth(80);
		
		lay0.add(btn, 0, 0, 2, 1);
		lay0.addRow(1,new Label("清晰度"),txtParm[0]);
		lay0.addRow(2,new Label("擴充係數"),txtParm[1]);		
		return lay0;
	}
}
