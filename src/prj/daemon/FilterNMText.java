package prj.daemon;

import java.util.ArrayList;

import javafx.scene.control.Control;

import com.jfoenix.controls.JFXButton;

import narl.itrc.CamRender;
import narl.itrc.ImgFilter;
import narl.itrc.ImgPreview;
import narl.itrc.ImgPreview.Rect;
import narl.itrc.Misc;

public class FilterNMText extends ImgFilter {

	private String fileNM1 = Misc.pathTemp+"trained_classifierNM1.xml";
	private String fileNM2 = Misc.pathTemp+"trained_classifierNM2.xml";
	private String fileGRP = Misc.pathTemp+"trained_classifier_erGrouping.xml";
	
	private CamRender rndr;
	
	public FilterNMText(){
	}

	private ArrayList<ImgPreview.Rect> lstBox = new ArrayList<ImgPreview.Rect>();
	@Override
	public void cookData(ArrayList<ImgPreview> list) {
		lstBox.clear();
		implCookData(list.get(0).bundle.getMatx());
	}
	private void updateBox(int x, int y, int width, int height){
		lstBox.add(new Rect(x,y,width,height));
	}
	
	@Override
	public boolean showData(ArrayList<ImgPreview> list) {
		ImgPreview prv = list.get(0);
		prv.clearAll();
		prv.drawRect(lstBox);
		return true;//we done~~~~
	}

	public Control getControl(final CamRender rndr){		
		JFXButton btn = new JFXButton("辨識字元");
		btn.getStyleClass().add("btn-raised1");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(event->{
			rndr.addFilter(FilterNMText.this);
		});
		return btn;
	}
	
	private native void implCookData(long ptrMatx);
}
