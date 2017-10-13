package prj.daemon;

import java.util.ArrayList;

import javafx.scene.control.Control;

import com.jfoenix.controls.JFXButton;

import narl.itrc.Misc;
import narl.itrc.vision.ImgFilter;
import narl.itrc.vision.ImgPreview1;
import narl.itrc.vision.ImgRender1;

public class FilterNMText extends ImgFilter {

	private String fileNM1 = Misc.pathSock+"trained_classifierNM1.xml";
	private String fileNM2 = Misc.pathSock+"trained_classifierNM2.xml";
	private String fileGRP = Misc.pathSock+"trained_classifier_erGrouping.xml";
	
	private ImgRender1 rndr;
	
	public FilterNMText(){
	}

	private native int[] implCookData(long ptrMatx);//this implements in 'FilterMisc.cpp'
	private int[] rect = null;
	@Override
	public void cookData(ArrayList<ImgPreview1> list) {
		//TODO:rect = implCookData(list.get(0).bundle.getMatx());
	}
		
	@Override
	public boolean showData(ArrayList<ImgPreview1> list) {
		//ImgPreview prv = list.get(0);
		//prv.clearAll();
		//prv.drawRect(rect);
		return true;//we done~~~~
	}

	public Control getControl(final ImgRender1 rndr){		
		JFXButton btn = new JFXButton("偵測字元");
		btn.getStyleClass().add("btn-raised-2");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(event->{
			rndr.attach(FilterNMText.this);
		});
		return btn;
	}
}
