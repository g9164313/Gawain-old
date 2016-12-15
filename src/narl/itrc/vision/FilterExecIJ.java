package narl.itrc.vision;

import java.io.File;
import java.util.ArrayList;

import narl.itrc.CamBundle;
import narl.itrc.ImgFilter;
import narl.itrc.ImgPreview;
import narl.itrc.Misc;

public class FilterExecIJ extends ImgFilter {
	
	@Override
	public void cookData(ArrayList<ImgPreview> list) {
		String name = Misc.fsPathTemp+File.separator+"temp.png";
		ImgPreview prv = list.get(prvIndex);
		CamBundle bnd = prv.bundle;
		bnd.saveImage(name);
		Misc.execIJ(name);
	}
	
	@Override
	public boolean showData(ArrayList<ImgPreview> list) {
		return true;
	}
}
