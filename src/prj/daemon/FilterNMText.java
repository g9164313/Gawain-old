package prj.daemon;

import java.util.ArrayList;

import javafx.scene.control.Control;

import com.jfoenix.controls.JFXButton;

import narl.itrc.ImgRender;
import narl.itrc.ImgFilter;
import narl.itrc.ImgPreview;
import narl.itrc.Misc;

public class FilterNMText extends ImgFilter {

	private String fileNM1 = Misc.pathTemp+"trained_classifierNM1.xml";
	private String fileNM2 = Misc.pathTemp+"trained_classifierNM2.xml";
	private String fileGRP = Misc.pathTemp+"trained_classifier_erGrouping.xml";
	
	private ImgRender rndr;
	
	public FilterNMText(){
	}

	private native int[] implCookData(long ptrMatx);//this implements in 'FilterMisc.cpp'
	private int[] rect = null;
	@Override
	public void cookData(ArrayList<ImgPreview> list) {
		//TODO:rect = implCookData(list.get(0).bundle.getMatx());
	}
		
	@Override
	public boolean showData(ArrayList<ImgPreview> list) {
		ImgPreview prv = list.get(0);
		prv.clearAll();
		prv.drawRect(rect);
		return true;//we done~~~~
	}

	public Control getControl(final ImgRender rndr){		
		JFXButton btn = new JFXButton("偵測字元");
		btn.getStyleClass().add("btn-raised1");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(event->{
			rndr.addFilter(FilterNMText.this);
		});
		return btn;
	}
}
