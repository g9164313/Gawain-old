package prj.letterpress;

import javafx.scene.Parent;
import narl.itrc.CamBundle;
import narl.itrc.CamVidcap;
import narl.itrc.ImgPreview;
import narl.itrc.ImgRender;
import narl.itrc.PanBase;

public class Entry extends PanBase {

	public Entry(){
	}
	
	private CamBundle cam0 = new CamVidcap(0,"");
	private CamBundle cam1 = new CamVidcap(1,"");
	
	private ImgPreview prv0 = new ImgPreview(cam0);
	private ImgPreview prv1 = new ImgPreview(cam1);
	private ImgRender rnd = new ImgRender(prv0,prv1);
		
	@Override
	public Parent layout() {
		
		return null;
	}
}
