package narl.itrc.vision;

import java.io.File;
import java.util.ArrayList;

import narl.itrc.Gawain;
import narl.itrc.Misc;
import narl.itrc.PanBase;

public class FilterExecIJ extends ImgFilter {
	
	@Override
	public void cookData(ArrayList<ImgPreview> list) {
		String name = Misc.fsPathTemp+File.separator+"temp.png";
		ImgPreview prv = list.get(prvIndex);
		CamBundle bnd = prv.bundle;
		bnd.saveImage(name);
		execIJ(name);
	}
	
	@Override
	public boolean showData(ArrayList<ImgPreview> list) {
		return true;
	}
	
	private static Process procIJ = null;
	
	private static void execIJ(String img_name){
		if(procIJ!=null){
			//just only execute one instance~~~
			if(procIJ.isAlive()==true){
				procIJ.destroy();
				procIJ = null;//for next turn~~~~
				return;
			}			
		}
		String ij_path = Gawain.prop.getProperty("IJ_PATH","");		
		if(ij_path.length()==0){
			PanBase.notifyError(
				"內部資訊",
				"請在 conf.properties 設定 ImageJ 執行路徑"
			);
			return;
		}
		try {			
			//How to find 'java' from M$ Windows OS ? 
			ProcessBuilder pb = null;
			if(ij_path.contains(".jar")){
				//execute ImageJ from jar file
				pb = new ProcessBuilder(
					"/usr/bin/java","-Xmx1024m","-jar",
					ij_path, img_name
				);
			}else{
				//it is a executed file
				pb = new ProcessBuilder(ij_path,img_name);
			}
			pb.directory(Misc.fsPathTemp);
			procIJ = pb.start();
		} catch (Exception e) {
			PanBase.notifyError(
				"內部資訊",
				e.getMessage()
			);
		}
	}
}
