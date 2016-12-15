package narl.itrc.vision;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import narl.itrc.CamBundle;
import narl.itrc.ImgFilter;
import narl.itrc.ImgPreview;
import narl.itrc.PanBase;

public class FilterSnap extends ImgFilter {
	/**
	 * This is file name for filter 'snap'.<p> 
	 * It means "path", "prefix" and "postfix"(.jpg, .png, .gif, etc).<p>
	 */
	public String[] snapName = {"","",""};
	public AtomicInteger snapIndx = new AtomicInteger(0);
	@Override
	public void cookData(ArrayList<ImgPreview> list) {
		int idx = snapIndx.incrementAndGet();
		for(int i=0; i<list.size(); i++){
			//first save the entire image
			ImgPreview prv = list.get(i);
			CamBundle bnd = prv.bundle;
			bnd.saveImage(String.format(
				"%s%s%d_%03d%s",
				snapName[0],snapName[1],(i+1),
				idx,
				snapName[2]
			));
			//second save all ROI inside image
			//how to check ROI???
			/*for(int j=0; j<prv.mark.length; j++){
				int[] roi = prv.mark[j].getROI();
				if(roi==null){
					continue;
				}
				bnd.saveImageROI(String.format(
					"%sroi_%s%d_%03d%s",
					snapName[0],snapName[1],(i+1),
					idx,
					snapName[2]
				), roi);
			}*/
		}	
	}
	@Override
	public boolean showData(ArrayList<ImgPreview> list) {
		PanBase.notifyInfo("Render",
		String.format(
			"儲存影像(%d) %s",
			snapIndx.get(),
			snapName[1]
		));
		return true;
	}
}
